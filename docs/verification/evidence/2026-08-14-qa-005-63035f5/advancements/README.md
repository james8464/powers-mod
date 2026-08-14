# Advancement alignment acceptance

Exact build: `63035f5` on `main`, POWERS 1.0.2, Minecraft 26.2, Fabric Loader
0.19.3, Fabric API 0.156.0+26.2, and Java 25 on macOS arm64.

A real Fabric client connected to a fresh dedicated server. The server synchronised the Light
advancement path after removing the `darkness` tag, then the Darkness path after adding it. The
client opened Minecraft's ordinary Advancements screen through its bound key on both states.

- `screenshots/advancement-light.png` shows only **Awaken Your Skill** on the pale radiant
  background.
- `screenshots/advancement-darkness.png` shows only **Darkness Initiation** on the dark violet
  background.
- `screenshots/advancement-zero-residue.png` follows `/powers diagnose`; no proxy, travel load,
  forced chunk, field, cast, companion, or Celestial Ruin state remained.
- `logs/client.log`, `logs/server.log`, and `scenarios/advancements.tsv` preserve the exact live
  sequence, alignment revocations, screen captures, diagnostics, and clean server save/stop.

Both images were inspected at their original 1708×960 resolution. No missing texture, wrong
background, opposite-alignment tab, clipping, or unreadable title was observed.
