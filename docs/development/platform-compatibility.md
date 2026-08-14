# Platform compatibility contracts

POWERS automatically binds Fabric Permission API v1, which is the supported LuckPerms integration boundary. It keeps vanilla operator levels as the fallback when the permission module, a provider, or a provider decision is absent. Servers may independently grant `powers.command.diagnose`, `powers.command.testing`, `powers.command.travel`, `powers.command.assign`, `powers.command.recover`, and `powers.command.boss`; each command family routes to exactly one of those nodes.

Rank prefixes decorate the existing display-name component, so team, nickname, hover, click, and formatting-mod data remains intact. POWERS does not rewrite signed chat content. `rankPrefixesEnabled: false` returns the original display component unchanged; the same display-name path covers ordinary chat presentation, tab names, nameplates, and death messages.

Every one of the 64 registered magic actions carries an entity-target contract in the central catalogue. Player-only mechanics request an explicit participant capability. Ordinary living targets receive the typed `UNSUPPORTED_TARGET` failure or the mechanic's documented mob fallback; they are never cast to players. Foreign projectile, damage, and effect inputs default to unchanged behavior, while POWERS-owned markers remain explicit.

Optional client art resolves through a visible fallback when a resource is absent or lookup fails. The energy HUD draws procedural ten-symbol icons with the same 9×9/ten-slot contract if its atlas is omitted; optional item models select the baked vanilla barrier model. Missing optional pack content therefore remains conspicuous without turning model or HUD extraction into a crash.

The pinned Minecraft 26.2 runtime matrix for Sodium, Lithium, Simple Voice Chat, ClaimMod, and Inventory Extended is published in [`../verification/compatibility-matrix.md`](../verification/compatibility-matrix.md). Read its `LIMITED` boundaries literally: ClaimMod has no published adapter API, unknown/nested inventory slots have no artifact-ownership adapter, voice networking was proven without recording audio, and the enhanced Light Realm sky remains future VFX-009 work.
