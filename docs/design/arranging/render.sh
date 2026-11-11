#!/usr/bin/env bash
# Renders each state of mockup.html to a 1920x1080 PNG with headless Edge.
set -euo pipefail
cd "$(dirname "$0")"
EDGE="${EDGE:-/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe}"
SRC="file:///$(cygpath -m "$(pwd)/mockup.html")"
for s in confirm arrange menu moving after-hide hidden-menu no-hidden last-app; do
  "$EDGE" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 \
    --window-size=1920,1080 --screenshot="$(cygpath -w "$(pwd)/$s.png")" "$SRC#$s" >/dev/null 2>&1
  echo "rendered $s.png"
done
