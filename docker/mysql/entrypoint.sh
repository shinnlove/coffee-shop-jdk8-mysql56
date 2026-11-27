#!/usr/bin/env bash
set -euo pipefail

# Build from Oracle's official archive: the old Docker Hub tag uses obsolete Schema 1.
mysql_base=/opt/mysql
mysql_data=/var/lib/mysql
mysql_socket=/run/mysqld/mysqld.sock

# Bind mounts start owned by root. Drop privileges before initialization and normal serving.
if [ "$(id -u)" = 0 ]; then
  mkdir -p "$mysql_data" /run/mysqld
  chown mysql:mysql "$mysql_data" /run/mysqld
  exec gosu mysql "$0" "$@"
fi

if [ ! -d "$mysql_data/mysql" ]; then
  : "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is required for initial setup}"
  : "${MYSQL_DATABASE:?MYSQL_DATABASE is required for initial setup}"
  : "${MYSQL_USER:?MYSQL_USER is required for initial setup}"
  : "${MYSQL_PASSWORD:?MYSQL_PASSWORD is required for initial setup}"
  [[ "$MYSQL_DATABASE" =~ ^[A-Za-z0-9_]+$ ]] || { echo 'Invalid database name' >&2; exit 1; }
  [[ "$MYSQL_USER" =~ ^[A-Za-z0-9_]+$ ]] || { echo 'Invalid database user' >&2; exit 1; }
  [ "$MYSQL_USER" != root ] || { echo 'Use a non-root application user' >&2; exit 1; }

  # MySQL 5.6 uses mysql_install_db, not the later --initialize-insecure option.
  "$mysql_base/scripts/mysql_install_db" --no-defaults --basedir="$mysql_base" --datadir="$mysql_data" --keep-my-cnf
  mysqld --no-defaults --basedir="$mysql_base" --datadir="$mysql_data" \
    --socket="$mysql_socket" --pid-file=/run/mysqld/mysqld.pid --skip-networking &
  mysql_init_pid=$!
  trap 'kill "$mysql_init_pid" 2>/dev/null || true; wait "$mysql_init_pid" 2>/dev/null || true' EXIT
  mysql_ready=false
  for ((attempt=0; attempt<90; attempt++)); do
    if mysql --no-defaults --socket="$mysql_socket" -uroot -e 'SELECT 1' >/dev/null 2>&1; then
      mysql_ready=true
      break
    fi
    kill -0 "$mysql_init_pid" 2>/dev/null || { echo 'MySQL initialization exited' >&2; exit 1; }
    sleep 1
  done
  [ "$mysql_ready" = true ] || { echo 'MySQL initialization timed out' >&2; exit 1; }

  # SQL literal escaping is needed even for local passwords containing quotes or backslashes.
  sql_escape() {
    local value="$1"
    value="${value//\\/\\\\}"
    value="${value//\'/\'\'}"
    printf '%s' "$value"
  }
  mysql_root_literal="$(sql_escape "$MYSQL_ROOT_PASSWORD")"
  mysql_app_literal="$(sql_escape "$MYSQL_PASSWORD")"
  # Escape underscores in database-level grants: MySQL treats them as wildcard characters.
  mysql_grant_database="${MYSQL_DATABASE//_/\\_}"
  mysql --no-defaults --socket="$mysql_socket" -uroot <<SQL
DELETE FROM mysql.user WHERE User = '' OR (User = 'root' AND Host <> 'localhost');
DROP DATABASE IF EXISTS test;
DELETE FROM mysql.db WHERE Db = 'test' OR Db = 'test\\_%';
CREATE DATABASE \`$MYSQL_DATABASE\` CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
CREATE USER '$MYSQL_USER'@'%' IDENTIFIED BY '$mysql_app_literal';
GRANT ALL PRIVILEGES ON \`$mysql_grant_database\`.* TO '$MYSQL_USER'@'%';
SET PASSWORD FOR 'root'@'localhost' = PASSWORD('$mysql_root_literal');
FLUSH PRIVILEGES;
SQL
  MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqladmin --no-defaults --socket="$mysql_socket" -uroot shutdown
  wait "$mysql_init_pid"
  trap - EXIT
  touch "$mysql_data/.coffee-initialized"
fi

[ -f "$mysql_data/.coffee-initialized" ] || {
  echo "Incomplete or foreign data directory. Inspect it and use a new empty directory; data was not deleted." >&2
  exit 1
}

# Existing data is never reseeded on restart; changed environment passwords are not reapplied.
exec mysqld --no-defaults --basedir="$mysql_base" --datadir="$mysql_data" \
  --socket="$mysql_socket" --pid-file=/run/mysqld/mysqld.pid "$@"
