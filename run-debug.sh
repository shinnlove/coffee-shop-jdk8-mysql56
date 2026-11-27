#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
exec ./run-local.sh '-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:5005' "$@"
