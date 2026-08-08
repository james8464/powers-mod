# Telekinesis Release Design

## Goal

Make Telekinesis mechanically match its outward shockwave fantasy, preserve server-authoritative counterplay, and avoid charging players for casts that cannot affect anything.

## Contracts

- A valid target receives a normalized horizontal impulse from the caster toward the target plus the existing upward lift. Coincident horizontal positions are skipped because no outward direction exists.
- Amethyst-dampened, protected, dead, and self targets remain excluded. Projectile reflection retains its ownership-depth guard and hard cap of 16.
- The cast succeeds only when at least one living target moves or one projectile reflects. Otherwise it returns `false`, so the existing transaction refunds energy and starts neither cooldown nor magic residue.
- Allowed collision reactions are previewed without side effects and emitted only when gameplay succeeds. A blocked attempt immediately emits only the reactions that actually reject it, preventing targetless or rejected casts from applying free reveal, cleanse, pressure, ward damage, projectile consumption, or unrelated neighbouring reactions.
- Empty casts receive a restrained collapsed-rune cue and lore message. Successful casts show violet/cyan counter-rotating rings, directional tethers, impact sigils, and reflection sparks before the universal rank-responsive ceremony commits.
- Vector arithmetic rejects non-finite or non-positive strengths and never creates NaN velocity.

## Verification

Pure tests prove outward signs, exact normalized magnitude, overlap handling, invalid inputs, and success-resolution rules. Full verification includes source/assets audits, clean checks, resources, jar build, and dedicated-server startup/shutdown.
