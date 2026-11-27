#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
if command -v mvn >/dev/null 2>&1; then
  coffee_maven=mvn
else
  coffee_maven=./mvnw
fi
exec "$coffee_maven" spring-boot:run -Dspring-boot.run.profiles=local "$@"
