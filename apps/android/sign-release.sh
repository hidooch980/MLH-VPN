#!/usr/bin/env bash
# Initial release only. Future releases MUST reuse the recovered signing key.
set -euo pipefail
umask 077
android_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
checkout="${1:?prepared upstream checkout required}"
signing_dir="${RUNNER_TEMP:?RUNNER_TEMP required}/mlh-signing"
mkdir -p "$signing_dir" dist backup
trap 'rm -rf "$signing_dir"' EXIT
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:4096 -out "$signing_dir/key.pem" 2>/dev/null
openssl req -new -x509 -key "$signing_dir/key.pem" -out "$signing_dir/signing-cert.pem" \
  -days 10000 -sha256 -subj '/CN=MLH VPN/O=MLH VPN'
openssl pkcs8 -topk8 -nocrypt -in "$signing_dir/key.pem" -outform DER -out "$signing_dir/signing-key.pk8"
rm "$signing_dir/key.pem"
# Back up before signing. Only the local holder of backup-private.pem can decrypt.
tar -C "$signing_dir" -czf "$signing_dir/backup.tar.gz" signing-key.pk8 signing-cert.pem
openssl cms -encrypt -binary -aes-256-cbc -in "$signing_dir/backup.tar.gz" \
  -outform DER -out backup/MLHVPN-signing-backup.cms "$android_dir/signing-backup-public.pem"
cp "$signing_dir/signing-cert.pem" dist/MLHVPN-signing-cert.pem
sdk_tools="$ANDROID_HOME/build-tools/37.0.0"
for apk in "$checkout"/V2rayNG/app/build/outputs/apk/fdroid/release/*.apk; do
  name="$(basename "$apk")"
  "$sdk_tools/zipalign" -f -p 4 "$apk" "$signing_dir/aligned.apk"
  "$sdk_tools/apksigner" sign --key "$signing_dir/signing-key.pk8" --cert "$signing_dir/signing-cert.pem" \
    --v4-signing-enabled false --out "dist/$name" "$signing_dir/aligned.apk"
  "$sdk_tools/apksigner" verify --verbose --print-certs "dist/$name" > "dist/$name.signature.txt"
  "$sdk_tools/aapt2" dump badging "dist/$name" > "$signing_dir/badging.txt"
  grep -F "package: name='io.github.hidooch980.mlhvpn.fdroid'" "$signing_dir/badging.txt"
  if grep -Fq 'application-debuggable' "$signing_dir/badging.txt"; then echo 'Release is debuggable' >&2; exit 1; fi
done
mkdir -p test-apk
for apk in "$checkout"/V2rayNG/app/build/outputs/apk/androidTest/fdroid/debug/*.apk; do
  "$sdk_tools/apksigner" sign --key "$signing_dir/signing-key.pk8" --cert "$signing_dir/signing-cert.pem" \
    --v4-signing-enabled false --out test-apk/MLHVPN-androidTest.apk "$apk"
done
python3 "$android_dir/verify_apks.py" dist
cp "$android_dir/INSTALL.fa.md" "$android_dir/RELEASE-NOTES.fa.md" "$android_dir/upstream.lock.json" dist/
tar --exclude=.git --exclude=.gradle --exclude=build --exclude=libs --exclude=local.properties \
  -czf dist/MLHVPN-android-source.tar.gz apps/android work/android
(cd dist && sha256sum *.apk *.pem *.tar.gz > SHA256SUMS)
