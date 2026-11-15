#!/usr/bin/env bash
# Sign an already-built MLH VPN update without copying the private key into the repository.
set -euo pipefail
umask 077

android_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
unsigned_dir="${1:?Usage: offline-sign-update.sh UNSIGNED_DIR SIGNING_KEY_PK8 SIGNING_CERT_PEM OUTPUT_DIR}"
signing_key="${2:?Signing key path is required}"
signing_cert="${3:?Signing certificate path is required}"
output_dir="${4:?Output directory is required}"

: "${ANDROID_HOME:?ANDROID_HOME must point to an Android SDK}"
tools_dir="$ANDROID_HOME/build-tools/37.0.0"
for tool in apksigner zipalign; do
  test -x "$tools_dir/$tool" || { echo "Missing $tools_dir/$tool" >&2; exit 2; }
done
for path in "$unsigned_dir" "$signing_key" "$signing_cert"; do
  test -e "$path" || { echo "Missing input: $path" >&2; exit 2; }
done

expected_fingerprint='3C8849FC59084133E7B09402EE1665563F5329CE3602380A7E05FAEE8D4E7D7D'
actual_fingerprint="$(openssl x509 -in "$signing_cert" -noout -fingerprint -sha256 | cut -d= -f2 | tr -d ':')"
test "$actual_fingerprint" = "$expected_fingerprint" || {
  echo 'Certificate does not match the established MLH VPN release identity.' >&2
  exit 3
}

temporary="$(mktemp -d)"
trap 'rm -rf "$temporary"' EXIT
mkdir -p "$output_dir/signed" "$output_dir/patches"

expected=(
  MLHVPN_1.1.0-fdroid_universal.apk
  MLHVPN_1.1.0-fdroid_arm64-v8a.apk
  MLHVPN_1.1.0-fdroid_armeabi-v7a.apk
  MLHVPN-androidTest.apk
)

for name in "${expected[@]}"; do
  unsigned="$unsigned_dir/$name"
  signed="$output_dir/signed/$name"
  patch="$output_dir/patches/$name.json"
  test -f "$unsigned" || { echo "Missing unsigned APK: $name" >&2; exit 4; }
  "$tools_dir/zipalign" -c -P 16 4 "$unsigned"
  "$tools_dir/apksigner" sign \
    --key "$signing_key" --cert "$signing_cert" \
    --v1-signing-enabled false --v4-signing-enabled false \
    --out "$signed" "$unsigned"
  "$tools_dir/apksigner" verify --verbose --print-certs "$signed" > "$temporary/$name.verify.txt"
  grep -Fqi 'certificate SHA-256 digest: 3c8849fc59084133e7b09402ee1665563f5329ce3602380a7e05faee8d4e7d7d' "$temporary/$name.verify.txt"
  python3 "$android_dir/apk_signature_patch.py" extract "$unsigned" "$signed" "$patch"
  python3 "$android_dir/apk_signature_patch.py" apply "$unsigned" "$patch" "$temporary/$name"
  cmp "$signed" "$temporary/$name"
done

(cd "$output_dir/signed" && sha256sum "${expected[@]}" > SHA256SUMS)
cp "$signing_cert" "$output_dir/signed/MLHVPN-signing-cert.pem"
echo "Signed APKs: $output_dir/signed"
echo "Public signature patches safe to commit: $output_dir/patches"
