# Standalone Android privacy notes

Molla VPN's standalone Android build uses configurations and subscriptions you import. It does not require a Molla account or send profiles to the optional FastAPI backend in this repository. Imported profiles may contain passwords and private keys: treat local data, exported files and QR codes as sensitive.

The app connects to your configured VPN/proxy endpoints, subscription providers and DNS resolvers. Those providers have their own logging and privacy practices. Upstream features can also contact GitHub for updates/assets, the configured connectivity-test hosts, and the IP-information service when used. The overlay replaces the upstream promotional destination with the Molla repository; it does not claim to make all upstream features network-free. No claim of provider anonymity, no-logging, or protection from all DNS/IPv6 leaks is made.

Android controls VPN permission and system-level always-on/block-without-VPN settings. Removing a profile or uninstalling the app does not delete copies you exported or records held by your provider. Do not send configuration secrets in issue reports.
