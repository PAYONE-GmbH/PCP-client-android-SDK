#!/usr/bin/env bash
set -euo pipefail

tools="${ANDROID_HOME:?}/cmdline-tools"
# setup-android adds its installed tools to PATH. Its directory can be a build
# number (16111833), not the package revision (23.0), so do not guess the name.
if ! sdkmanager="$(command -v sdkmanager)"; then
  echo "::error::setup-android did not provide sdkmanager on PATH."
  exit 1
fi
selected="$(dirname "$(dirname "$sdkmanager")")"
if [[ ! -x "$selected/bin/avdmanager" || ! -f "$selected/source.properties" ]]; then
  echo "::error::Incomplete Android command-line tools at $selected."
  exit 1
fi

revision=0
while IFS= read -r line; do
  if [[ "$line" =~ ^Pkg\.Revision[[:space:]]*=[[:space:]]*([0-9]+) ]]; then
    revision="${BASH_REMATCH[1]}"
  fi
done < "$selected/source.properties"
if (( revision < 23 )); then
  echo "::error::Android command-line tools 23 or newer required; found $revision at $selected."
  exit 1
fi
echo "Using Android command-line tools from $selected"

# android-emulator-runner prepends cmdline-tools/latest/bin to PATH, ignoring
# setup-android's versioned path. The runner image still provides version 12.0
# there, which can create target=android-0 for dotted API levels such as 37.0.
if [[ ! "$tools/latest" -ef "$selected" ]]; then
  if [[ -e "$tools/latest" || -L "$tools/latest" ]]; then
    backup="$(mktemp -d "${RUNNER_TEMP:?}/android-cmdline-tools.XXXXXX")"
    mv "$tools/latest" "$backup/latest"
  fi
  ln -s "$selected" "$tools/latest"
fi

cat "$tools/latest/source.properties"
