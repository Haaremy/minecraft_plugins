# Network Notes

Last checked: 2026-05-03.

## VM Map

| VM | Name | IP | Minecraft role |
| --- | --- | --- | --- |
| 111 | `hmy-minecraft` | `10.0.3.16` | Main/public Minecraft stack. Runs Velocity on `25565` and local Paper backends on `30066-30068`. |
| 107 | `hmy-minecraft-ai` | `10.0.3.62` | AI-side Minecraft stack. Runs Velocity on `25565` and Paper backends on `30066-30068`. |

## Velocity Routing

The active VM111 Velocity config routes:

```toml
[servers]
lobby = "127.0.0.1:30066"
ai = "10.0.3.62:30066"
vanilla = "127.0.0.1:30067"
kitsune1 = "127.0.0.1:30068"
try = ["lobby","vanilla"]
```

For `Lobby -> AI`, the relevant TCP path is:

```text
VM111 Velocity -> 10.0.3.62:30066 -> VM107 Paper backend
```

## Fixed Network Error

Symptom: switching from Lobby to AI produced client-side network errors.

Cause: VM107 Proxmox firewall allowed public `25565`, SSH, monitoring, and HTTP, but did not allow backend ports `30066-30068` from VM111. VM111 already had the reverse allow rule for VM107.

Fix applied in `/etc/pve/firewall/107.fw`:

```text
IN ACCEPT -source 10.0.3.16 -p tcp -dport 30066:30068
IN ACCEPT -source 10.0.3.16 -p udp -dport 30066:30068
```

Then Proxmox firewall was reloaded with:

```bash
pve-firewall restart
```

Verification from VM111:

```bash
timeout 5 bash -lc '</dev/tcp/10.0.3.62/30066'
# RC=0
```

The Proxmox host remains unable to connect to `10.0.3.62:30066`, which is expected because the rule is intentionally scoped to source `10.0.3.16`.

## Follow-Up Checks

- If another backend server is moved to a different VM, add narrow Proxmox firewall rules for only the required source VM and backend ports.
- Keep backend Paper servers in `online-mode=false` when they are behind Velocity with modern forwarding.
- Ensure the Velocity `forwarding.secret` matches the Paper backend proxy secret when adding new backend nodes.
