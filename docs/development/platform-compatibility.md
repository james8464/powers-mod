# Platform compatibility contracts

POWERS keeps vanilla operator levels as the fallback when no permission adapter is installed. An adapter may independently decide `powers.command.diagnose`, `powers.command.testing`, `powers.command.travel`, `powers.command.assign`, `powers.command.recover`, and `powers.command.boss`. Adapter absence, failure, or an empty decision safely returns to the configured vanilla operator level.

Rank prefixes decorate the existing display-name component, so team, nickname, hover, click, and formatting-mod data remains intact. POWERS does not rewrite signed chat content. `rankPrefixesEnabled: false` returns the original display component unchanged; the same display-name path covers ordinary chat presentation, tab names, nameplates, and death messages.

Player-only mechanics request an explicit participant capability. Ordinary living targets receive the typed `UNSUPPORTED_TARGET` failure or the mechanic's documented mob fallback; they are never cast to players. Foreign projectile, damage, and effect inputs default to unchanged behavior, while POWERS-owned markers remain explicit.

Optional client art resolves through a visible vanilla fallback when a resource is absent or lookup fails. Missing optional pack content therefore remains conspicuous without turning model or HUD extraction into a crash.
