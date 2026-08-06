#!/bin/bash
# Test launcher for the POWERS mod.
# Usage: ./test.sh client | server
set -e
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@25}"

case "${1:-client}" in
  client)
    ./gradlew runClient
    ;;
  server)
    mkdir -p run/server
    printf 'eula=true\n' > run/eula.txt
    ./gradlew runServer
    ;;
  *)
    echo "Usage: $0 client | server"
    exit 1
    ;;
esac
