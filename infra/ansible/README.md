# Molla VPN infrastructure automation

This Ansible role prepares a Linux host for the first WireGuard pilot. It is a deployment template only: it has **not** been run against a server.

## Safety and scope

- Targets only hosts explicitly placed in the `vpn_nodes` inventory group.
- Installs WireGuard tools and iptables, enables IPv4 forwarding, creates a root-only server key, and configures `wg0`.
- The server private key is generated on the target host, stored as mode `0600`, and marked `no_log` in Ansible tasks.
- Does not create client peers, issue client profiles, change SSH, change provider firewalls, configure DNS, or provision cloud resources.
- The playbook starts the WireGuard interface. Do not run it until the host, provider firewall, egress interface, and rollback plan have been reviewed.
- A running interface with no peers is not a working VPN service.

## Prerequisites

- Ubuntu 22.04+ or a compatible Debian-family Linux host.
- Ansible Core 2.16+ on the operator machine.
- SSH key-based access with sudo privileges.
- A provider firewall rule allowing the selected UDP port (default 51820).
- A known public IPv4 and verified outbound interface.

## Prepare

1. Copy `inventory.example.ini` to `inventory.ini` and set the real host and SSH user. Keep the real inventory out of Git.
2. Review `group_vars/vpn_nodes.yml`, especially `molla_vpn_uplink_interface`.
3. Confirm the server is dedicated for the pilot and that no existing WireGuard configuration will be overwritten.
4. Run a syntax check:
   `ansible-playbook -i inventory.ini playbooks/wireguard-server.yml --syntax-check`
5. Review the exact target and changes:
   `ansible-playbook -i inventory.ini playbooks/wireguard-server.yml --list-hosts`
6. Only after explicit operational approval, run the playbook:
   `ansible-playbook -i inventory.ini playbooks/wireguard-server.yml`

## Important limitations

The initial role assumes the selected host's default IPv4 interface is the correct egress interface. It uses iptables rules in WireGuard PostUp/PostDown hooks and does not manage UFW/nftables policy. Validate compatibility with the host firewall before activation. It configures IPv4 only; IPv6 routing and leak prevention are separate acceptance gates.

Never put private keys, real server addresses, SSH credentials, or client configurations in this repository.
