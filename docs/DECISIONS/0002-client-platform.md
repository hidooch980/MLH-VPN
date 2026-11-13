# ADR 0002: Shared cross-platform client UI

- Status: Proposed
- Date: 2026-11-13

## Context
The product targets Android, iOS, Windows and macOS and should share product UX and account logic.

## Proposal
Evaluate Flutter for the shared UI, with native platform-specific VPN extension/service adapters. Keep tunnel engines behind a narrow interface so the UI is not coupled to a particular protocol.

## Validation required
Confirm current native APIs, signing requirements, app-store restrictions, desktop packaging, supported architectures and licensing before selecting final dependencies.
