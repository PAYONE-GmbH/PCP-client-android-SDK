#!/usr/bin/env bash
set -euo pipefail

# Check the AVD created by the action before starting an incorrectly configured
# guest. Old avdmanager versions silently turn dotted API levels into android-0.
avd_ini="${ANDROID_AVD_HOME:?}/${AVD_NAME:?}.ini"
if ! grep -Fxq "target=android-${AVD_API_LEVEL:?}" "$avd_ini"; then
  echo "::error::AVD target does not match android-$AVD_API_LEVEL; refusing to boot."
  cat "$avd_ini"
  exit 1
fi

adb start-server
