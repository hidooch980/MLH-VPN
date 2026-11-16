# MolidoVPN AI Android release and update operations

## Release target

The feature branch currently targets MolidoVPN AI 1.2.0 (versionCode 11), with the existing Android package identity and the established 1.1.0 signing certificate. Never change the application ID or rotate the signing certificate for an in-place upgrade.

## Required GitHub Actions secrets

The private signing key is intentionally not stored in Git. Before dispatching `.github/workflows/molidovpn-signed-release.yml`, configure these repository Actions secrets:

- `MOLIDO_ANDROID_SIGNING_KEY_PK8_B64`: base64-encoded DER PKCS#8 private key corresponding to the existing release certificate.
- `MOLIDO_ANDROID_SIGNING_CERT_PEM_B64`: base64-encoded PEM certificate whose SHA-256 fingerprint is `3C8849FC59084133E7B09402EE1665563F5329CE3602380A7E05FAEE8D4E7D7D`.

The workflow checks the certificate fingerprint, signs all three release APKs, verifies the signatures and package/version metadata, then performs an Android emulator upgrade smoke test from the published 1.1.0 Universal APK before publishing v1.2.0. It fails closed if either secret is missing or the certificate does not match. Do not place either secret in source control, logs, artifacts, or issue comments.

The private key must be recovered from the owner's protected signing-key backup. The public certificate in this repository is not sufficient to sign APKs. If the private key is lost, an APK signed with a newly generated key cannot update the already-installed app; users would need an uninstall/reinstall migration and would lose app-private data.

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
