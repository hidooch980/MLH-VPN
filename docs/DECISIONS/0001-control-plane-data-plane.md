# ADR 0001: Separate control plane and VPN data plane

- Status: Accepted
- Date: 2026-11-13

## Context
Cloudflare provides valuable DNS, edge security and application delivery services, but a generic VPN data plane requires protocol-aware packet forwarding and dedicated operational control.

## Decision
Use Cloudflare for DNS, API protection, and selected management-plane connectivity. Run end-user VPN protocol endpoints on dedicated VPN nodes. Do not treat Workers or ordinary Cloudflare Tunnel publishing as a universal raw VPN relay.

## Consequences
The system needs a separately operated node fleet, monitoring, key management and routing. Cloudflare Tunnel can be used for management services where its protocol and connection semantics fit.
