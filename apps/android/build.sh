#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-only
set -euo pipefail
android_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
checkout="${1:?Usage: build.sh /absolute/path/to/prepared-upstream [debug|release]}"
variant="${2:-debug}"
case "$variant" in debug) task=Debug ;; release) task=Release ;; *) echo 'Expected debug or release' >&2; exit 2 ;; esac
: "${ANDROID_HOME:?Set ANDROID_HOME to an installed Android SDK}"
ndk_version="$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["ndk"])' "$android_dir/upstream.lock.json")"
export NDK_HOME="$ANDROID_HOME/ndk/$ndk_version"
python3 "$android_dir/prepare.py" "$checkout" --download-core
(cd "$checkout" && bash compile-hevtun.sh)
cp -R "$checkout/libs/." "$checkout/V2rayNG/app/libs/"
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$checkout/V2rayNG/local.properties"
(cd "$checkout/V2rayNG" && ./gradlew --no-daemon licenseFdroidReleaseReport)
python3 "$android_dir/prepare.py" "$checkout" --notices
(cd "$checkout/V2rayNG" && ./gradlew --no-daemon :app:testFdroidDebugUnitTest ":app:assembleFdroid$task")
