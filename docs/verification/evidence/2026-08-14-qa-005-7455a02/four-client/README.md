# QA-005 four-client acceptance evidence

This directory records a real four-client Fabric session against dedicated-server commit `7455a02236245e6dfef6ebcfeda8541577cc4bcb` on 14 August 2026. The clients used the production networking entrypoints; the development harness only scheduled ordinary commands, chat, hotbar activation packets, screenshots, and vanilla respawn packets.

## Roles and scenario

- `Caster`: cast Forcefield and Energy Beam, respawned through the vanilla packet, revealed Shadow, asked a mechanics question, and requested Lightning Strike.
- `Ally`: stood inside the shared Forcefield radius, observed the beam interaction, died during the hostile pressure-wave scenario, and respawned through the vanilla packet.
- `Enemy`: cast Thunderclap and Void Beam towards Caster.
- `Observer`: spectator camera used only for independent visual evidence.

The exact scripts are retained under `scripts/`. The server had testing energy/cooldown bypass enabled for the three actors so the run exercised effects rather than resource waiting. It was stopped cleanly after removing the nine explicit arena force-loads.

## Proven observations

- All four clients joined concurrently and received the same registry/datapack state.
- Caster and Enemy sent real activation packets for Forcefield, Energy Beam, Thunderclap, and Void Beam.
- Thunderclap produced a hostile pressure-wave presentation and a genuine Ally death/respawn lifecycle.
- Opposing beam activity produced the visible `Counter` interaction, event geometry, fire, and terrain scars without crashing the server.
- Revealed Shadow dialogue was delivered to all four clients, answered an amethyst mechanics question from live context, accepted a Lightning Strike request, completed it, and remained a healthy server entity.
- Final diagnostics reported four players online, one revealed Shadow body, zero leaked forced chunks, and no active travel/celestial tickets.

These observations do not close QA-005 by themselves. Rows without direct evidence remain pending in the generated acceptance checklist.

## Evidence map

- `screenshots/caster-forcefield.png`: Caster immediately after Forcefield activation.
- `screenshots/observer-forcefield.png`: independent view of the shared-field presentation.
- `screenshots/enemy-thunderclap.png`: hostile cast aftermath and Ally death message.
- `screenshots/caster-beam-counter.png`: Caster-side beam-counter presentation.
- `screenshots/observer-beam-counter.png`: independent beam/collision and terrain presentation.
- `screenshots/ally-shadow-dialogue.png`: global Shadow mechanics/action dialogue after Ally respawn.
- `screenshots/enemy-shadow-dialogue.png`: independent global visibility and revealed Shadow scene.
- `logs/*.log`: complete client/server logs, including connected ticks and final diagnostics.

The launcher’s Mojang profile-key HTTP 401 messages are expected for named development clients in offline dedicated-server mode and are unrelated to the mod. Client processes were terminated after evidence capture; the server then performed a normal save and clean stop.
