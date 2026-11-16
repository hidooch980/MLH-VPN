# MolidoVPN AI Android release and update operations

## Release target

The feature branch currently targets MolidoVPN AI 1.2.0 (versionCode 11), with the existing Android package identity and the established 1.1.0 signing certificate. Never change the application ID or rotate the signing certificate for an in-place upgrade.

## Existing GitHub Actions signing secrets

Earlier project setup recorded these four repository Actions secrets as already present:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

The release workflow now reuses this established keystore and credentials. It imports the selected key into a temporary PKCS#12 container, extracts a temporary PKCS#8 key and certificate in the isolated runner, checks the certificate SHA-256 fingerprint against the installed 1.1.0 release identity (`3C8849FC59084133E7B09402EE1665563F5329CE3602380A7E05FAEE8D4E7D7D`), signs all three release APKs, verifies signatures and package/version metadata, and runs an Android emulator upgrade smoke test from the published 1.1.0 Universal APK before publishing v1.2.0. The workflow fails closed if any secret is absent, the keystore cannot be opened, or its certificate does not match. Secret values are never read into chat, committed, or printed to logs.

The existing keystore is the source of the private signing key. The public certificate in this repository is not sufficient by itself to sign APKs. If the keystore is missing, corrupt, or does not match the installed certificate, an APK signed with a newly generated key cannot update the already-installed app; users would need an uninstall/reinstall migration and would lose app-private data.

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
