# MolidoVPN AI Android release and update operations

## Release target

The feature branch currently targets MolidoVPN AI 1.2.0 (versionCode 11), with the existing Android package identity and the established 1.1.0 signing certificate. Never change the application ID or rotate the signing certificate for an in-place upgrade.

## Recovering the original VPN signing identity

The original signed Android release workflow (run `43440434815`, 2026-11-13) produced the encrypted artifact `MLHVPN-encrypted-signing-backup`. The artifact is still retained by GitHub Actions and contains `MLHVPN-signing-backup.cms`. It was downloaded and inspected; it contains an encrypted CMS backup, not a plaintext private key.

The release workflow downloads that exact artifact and decrypts it using the original recovery private key. Configure one repository Actions secret:

- `MLHVPN_BACKUP_PRIVATE_KEY_PEM_B64`: base64-encoded contents of the original `backup-private.pem` that corresponds to `apps/android/signing-backup-public.pem`.

Do not use signing secrets from another repository or generate a replacement key. In particular, the four `ANDROID_*` secrets documented in the separate `molido-robo` project belong to a different Android application and are not the MLH VPN recovery key.

If the owner has the recovery file on a trusted Linux machine, encode it locally without printing its contents into chat:

```sh
base64 -w0 /secure/path/backup-private.pem > /secure/path/backup-private.pem.b64
```

Copy the encoded value directly into the GitHub Actions secret `MLHVPN_BACKUP_PRIVATE_KEY_PEM_B64`; delete the temporary `.b64` file securely afterward. Do not commit either file.

After decrypting the CMS backup, the workflow extracts `signing-key.pk8` and `signing-cert.pem` into an isolated runner directory, checks the recovered certificate SHA-256 fingerprint against the published 1.1.0 identity (`3C8849FC59084133E7B09402EE1665563F5329CE3602380A7E05FAEE8D4E7D7D`), signs all three APKs, verifies signatures and package/version metadata, then runs an Android emulator upgrade smoke test from the published 1.1.0 Universal APK before publishing v1.2.0. Private material is removed when the signing step exits and is never uploaded or committed.

If the original `backup-private.pem` is unavailable, the encrypted CMS artifact cannot be decrypted. The public certificate and already-published APKs do not contain the private key; a new key cannot update existing installations.

## Dispatch

1. Open GitHub Actions for this repository.
2. Select **MolidoVPN AI signed production release**.
3. Run it on `feature/molidovpn-ai-redesign`.
4. Enter exactly `RELEASE` in the confirmation field.
5. Confirm the build, signature verification, upgrade smoke test, and release publication jobs all succeed.

Do not merge the branch or announce the release before all jobs pass. CI emulator smoke tests do not replace physical-device, VPN endpoint, DNS/IPv6 leak, network handover, and battery/background acceptance tests.

## In-app updates

The app checks for stable updates at most once per day, selects the matching ABI APK, and downloads only from the repository's GitHub Releases path. The Android package installer validates package signing continuity and performs the installation. Android requires the user to approve the installation; silent background installation is unavailable to an ordinary non-device-owner app. If Android asks for the per-app unknown-source permission, grant it only to MolidoVPN AI and return to the app to continue.

## Current limitations

- No signed 1.2.0 artifact is committed or published by this change.
- No production signing key is available to the build session.
- No physical Android device or live VPN endpoint has been used for acceptance testing.
- A successful build or emulator launch is not proof of working VPN traffic or leak protection.
