# Four-client acceptance harness

The QA harness is disabled in production and ordinary development launches. It activates only
when Fabric reports a development environment and both `powers.qa.role` and `powers.qa.server`
are explicitly supplied. An optional `powers.qa.script` points to a UTF-8 scenario file.

Each non-comment row is `connectedTick<TAB>operation<TAB>argument`. Supported operations are:

| Operation | Argument | Behaviour |
| --- | --- | --- |
| `command` | command without `/` | Sends the command through the real client connection. |
| `chat` | message | Sends signed-chat-compatible player dialogue. |
| `activate` | slot `0`–`2` | Sends the normal serverbound innate activation packet. |
| `select` | `slot option` | Sends the normal serverbound ability-option packet. |
| `screenshot` | safe evidence label | Captures the rendered client and records the label in its log. |

Rows must be ordered by tick. Invalid operations, slot ranges, control characters, oversized
arguments, and unsafe screenshot labels reject the script. All gameplay remains subject to the
ordinary server packet limits, permission checks, energy, cooldown, amethyst, confinement, and
protection policy. Four separate working directories keep the caster, ally, enemy, and observer
logs and screenshots isolated.
