#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Use isolated databases and a unique Compose project; never reset the developer's database.
command -v docker >/dev/null || { echo 'Docker CLI is required.' >&2; exit 1; }
command -v python3 >/dev/null || { echo 'Python 3 is required.' >&2; exit 1; }
command -v curl >/dev/null || { echo 'curl is required.' >&2; exit 1; }
docker info >/dev/null
docker compose version
run_id="$(date +%Y%m%d-%H%M%S)-${RANDOM}"
project="coffee-acceptance-$run_id"
result_dir="$PWD/docker/results/$run_id"
export MYSQL_DATA_DIR="$PWD/docker/data/acceptance-$run_id"
export MYSQL_TEST_DATA_DIR="$PWD/docker/data/tests-$run_id"
export TEST_RESULTS_DIR="$result_dir/tests"
export APP_PORT="${DOCKER_ACCEPTANCE_APP_PORT:-18080}"
export MYSQL_PORT="${DOCKER_ACCEPTANCE_MYSQL_PORT:-13307}"
# These are isolated local demo accounts, unrelated to a developer's .env credentials.
export DB_PASSWORD=coffee-local-only MYSQL_ROOT_PASSWORD=coffee-root-only DEFAULT_SHOP_ID=1
mkdir -p "$MYSQL_DATA_DIR" "$MYSQL_TEST_DATA_DIR" "$TEST_RESULTS_DIR"
compose=(docker compose -p "$project" -f compose.yaml -f compose.test.yaml --profile test)

finish() {
  local result=$?
  trap - EXIT
  "${compose[@]}" ps -a > "$result_dir/containers.txt" 2>&1 || true
  "${compose[@]}" logs --no-color > "$result_dir/containers.log" 2>&1 || true
  "${compose[@]}" down --remove-orphans > "$result_dir/shutdown.log" 2>&1 || result=1
  printf '%s\n' "$result" > "$result_dir/exit-code.txt"
  printf '\nEvidence: %s\nPersistent data: %s\n' "$result_dir" "$MYSQL_DATA_DIR"
  if [ "$result" = 0 ]; then
    printf 'PASS: image build, full tests, startup, stop/start, recreation and persisted data.\n'
  else
    printf 'FAIL: inspect evidence; data directories were retained.\n' >&2
  fi
  exit "$result"
}
trap finish EXIT

docker version > "$result_dir/docker-version.txt"
docker compose version > "$result_dir/compose-version.txt"
"${compose[@]}" config --quiet
"${compose[@]}" build --pull app db tests 2>&1 | tee "$result_dir/build.log"
"${compose[@]}" run --rm tests 2>&1 | tee "$result_dir/maven-verify.log"

wait_db() {
  for ((attempt=0; attempt<120; attempt++)); do
    if "${compose[@]}" exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql --protocol=tcp -h127.0.0.1 -u"$MYSQL_USER" -D"$MYSQL_DATABASE" -N -e "SELECT 1"' >/dev/null 2>&1; then
      return
    fi
    sleep 2
  done
  echo 'Database readiness timed out.' >&2
  return 1
}
wait_app() {
  for ((attempt=0; attempt<120; attempt++)); do
    if curl -fsS "http://127.0.0.1:$APP_PORT/actuator/health" > "$result_dir/health.json"; then
      return
    fi
    sleep 2
  done
  echo 'Application readiness timed out.' >&2
  return 1
}
"${compose[@]}" up -d db app
wait_db
wait_app
"${compose[@]}" exec -T app java -version > "$result_dir/java-version.txt" 2>&1
grep -q 'version "1.8.0' "$result_dir/java-version.txt"
"${compose[@]}" exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql --protocol=tcp -h127.0.0.1 -u"$MYSQL_USER" -N -e "SELECT VERSION(), @@version_comment"' > "$result_dir/mysql-version.txt"
grep -q $'5.6.30\tMySQL Community Server (GPL)' "$result_dir/mysql-version.txt"
grep -q 'Apache Maven 3.6.3' "$result_dir/maven-verify.log"
http_check=(python3 scripts/acceptance_http.py --base-url "http://127.0.0.1:$APP_PORT" --state "$result_dir/http-state.json")
"${http_check[@]}" create | tee "$result_dir/http-create.txt"

# Stop/start the same containers; explicitly wait for MySQL before starting the application.
"${compose[@]}" stop app db
"${compose[@]}" start db
wait_db
"${compose[@]}" start app
wait_app
"${http_check[@]}" verify | tee "$result_dir/http-after-start.txt"

# Removing containers must not remove the host's mounted data directories.
"${compose[@]}" down --remove-orphans
"${compose[@]}" up -d db app
wait_db
wait_app
"${http_check[@]}" verify | tee "$result_dir/http-after-recreate.txt"
