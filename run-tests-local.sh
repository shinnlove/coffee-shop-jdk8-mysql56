#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
# Dedicated test database only. Never substitute the application's DB_URL here.
export TEST_DB_URL="${TEST_DB_URL:-jdbc:mysql://127.0.0.1:3306/coffee_shop_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8}"
export TEST_DB_USERNAME="${TEST_DB_USERNAME:-coffee_test}"
export TEST_DB_PASSWORD="${TEST_DB_PASSWORD:-coffee-test-only}"
if command -v mvn >/dev/null 2>&1; then
  coffee_maven=mvn
else
  coffee_maven=./mvnw
fi
exec "$coffee_maven" verify "$@"
