# Release signing and recovery

The first signed release is v0.4.0. The bootstrap workflow only runs for the exact commit message `release: MLH VPN 0.4.0` on the project branch and refuses to replace that release. It creates an RSA-4096 Android signing key in the temporary runner directory, signs non-debuggable release APKs, verifies signatures and native libraries, and encrypts a recovery archive to `signing-backup-public.pem` using OpenSSL CMS. The private recovery key is held locally outside this repository. Plain signing material is deleted when the signing step exits and never uploaded or included in the source archive.

The encrypted backup artifact is retained for 90 days; recover it promptly and back up the recovered signing key in secure durable storage. The release includes only the public Android signing certificate, APK signatures, checksums and source. Installation is tested on an emulator before the publishing job can run. Publication uses the job-scoped GitHub Actions token with contents:write; no personal access token is stored.

Do not reuse the bootstrap workflow to generate keys for subsequent versions. Recover `signing-key.pk8` and `signing-cert.pem`, then use that same pair with `apksigner sign --key ... --cert ...`. Never commit either the private recovery key or the private Android signing key. Future automated signing must use a protected signing environment or repository secrets provisioned by the owner.

Recovery example (paths are supplied by the owner; private files must stay outside the repo):

```sh
umask 077
openssl cms -decrypt -binary -inform DER -in MLHVPN-signing-backup.cms \
  -inkey backup-private.pem -out signing-backup.tar.gz
```

Verify the recovered certificate matches the APK's published certificate fingerprint before using it for an update. Debug APKs use another certificate and cannot update a signed release. Never silently uninstall an existing app: export its user data first.
