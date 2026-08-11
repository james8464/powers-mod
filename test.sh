#!/usr/bin/env bash
# Reproducible launcher for development, live tests, and release verification.
set -euo pipefail

POWERS_PROJECT_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$POWERS_PROJECT_ROOT"

java_major() {
  "$1/bin/java" -version 2>&1 | sed -nE '1s/.*version "([0-9]+).*/\1/p'
}

resolve_java_home() {
  local candidates=()
  [[ -n "${POWERS_JAVA_HOME:-}" ]] && candidates+=("$POWERS_JAVA_HOME")
  [[ -n "${JAVA_HOME:-}" ]] && candidates+=("$JAVA_HOME")
  if [[ -n "${USER:-}" ]]; then
    candidates+=("/Users/${USER}/Library/Application Support/minecraft/runtime/java-runtime-epsilon/mac-os-arm64/java-runtime-epsilon/jre.bundle/Contents/Home")
  fi
  candidates+=("/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home")

  local candidate
  for candidate in "${candidates[@]}"; do
    if [[ -x "$candidate/bin/java" ]] && [[ "$(java_major "$candidate")" == "25" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  local java_command
  java_command="$(command -v java 2>/dev/null || true)"
  if [[ -n "$java_command" ]]; then
    candidate="$(cd -- "$(dirname -- "$java_command")/.." && pwd)"
    if [[ "$(java_major "$candidate")" == "25" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  fi

  printf 'POWERS requires Java 25. Set POWERS_JAVA_HOME to a Java 25 installation.\n' >&2
  return 1
}

export JAVA_HOME="$(resolve_java_home)"

case "${1:-client}" in
  doctor)
    printf 'POWERS project: %s\n' "$POWERS_PROJECT_ROOT"
    printf 'Java 25 home: %s\n' "$JAVA_HOME"
    "$JAVA_HOME/bin/java" -version
    ;;
  client)
    exec ./gradlew runClient --no-daemon
    ;;
  server)
    mkdir -p run
    [[ -f run/eula.txt ]] || printf 'eula=true\n' > run/eula.txt
    exec ./gradlew runServer --no-daemon
    ;;
  check)
    exec ./gradlew clean check --no-daemon
    ;;
  gametest)
    exec ./gradlew runGameTest --no-daemon
    ;;
  soak)
    exec ./gradlew test --tests com.powers.performance.SyntheticMultiplayerSoakTest --no-daemon
    ;;
	restart-soak)
		exec python3 scripts/restart_soak.py "${@:2}"
		;;
  *)
    printf 'Usage: %s {client|server|check|gametest|soak|restart-soak|doctor}\n' "$0" >&2
    exit 2
    ;;
esac
