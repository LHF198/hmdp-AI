#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

export SERVER_PORT="${SERVER_PORT:-8083}"
exec mvn spring-boot:run
