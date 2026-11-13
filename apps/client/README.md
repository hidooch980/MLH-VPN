# Molla VPN client shell

Flutter UI shell for Android, iOS, Windows and macOS. Platform runner folders and native VPN adapters are intentionally not checked in yet; generate runners with the Flutter SDK on the corresponding build hosts.

## Local setup

```bash
cd apps/client
flutter pub get
flutter test
```

Create native runner projects after installing Flutter:

```bash
flutter create --platforms=android,ios,windows,macos .
```

iOS and macOS builds require macOS with Xcode. Windows builds require Windows and Visual Studio build tools. This shell deliberately does not simulate a VPN connection; the connect action remains unavailable until a real, tested tunnel adapter is integrated.
