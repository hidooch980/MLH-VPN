# Security Policy

## Reporting
Do not publish vulnerabilities, credentials, private keys, access tokens, or exploitable server details in public issues. Contact the repository owner through a private channel and include reproduction steps and impact.

## Repository rules
- Never commit .env files, private keys, certificates, billing secrets, Cloudflare tokens, or production configuration.
- Use least-privilege credentials and short-lived tokens.
- Require review for changes to authentication, cryptography, tunnel lifecycle, billing and deployment.
- Pin third-party dependencies and review upstream security advisories.
- Rotate any credential that may have been exposed; deleting it from the latest commit is not sufficient.

## Production readiness
A security review, dependency scan, secret scan, platform-specific tunnel tests, and documented incident/revocation procedure are required before public production release.
