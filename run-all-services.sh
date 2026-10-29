#!/usr/bin/env bash
# Forwarder for backward-compatibility with start-all-services.sh
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$SCRIPT_DIR/start-all-services.sh" "$@"
