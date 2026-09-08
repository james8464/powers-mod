# INT-012 verification status

Status: **implementation staged; focused verification green; independent review pending.**

Dimensional Anchor can now bind a loaded vulnerable body proxy to the body's
current dimension through a finite identity-only registry. The registry holds
no entity or world references, has a 256-entry hard cap, purges expired leases
before capacity refusal, and is cleared when bodies or the server are cleared.

The same explicit interaction can extend one target owner's already-existing
artifact gate. It performs a constant-time owner lookup and requires both
endpoints to be in the caster's current loaded dimension, within eight blocks
of the anchored target, and legal under the portal protection hook. The
extension is capped at 600 ticks per interaction and 1,200 ticks beyond the
interaction time; it never creates a gate, destination, realm exit, search, or
chunk ticket.

## Test-driven record

- RED: the policy tests failed to compile before the stabilization contract
  existed.
- GREEN: local-body eligibility, route existence/loading/dimension/range/
  protection refusals, finite deadline extension, and no-shortening behavior
  pass.
- RED: the body registry tests failed to compile before its bounded state
  owner existed.
- GREEN: expiry, renewal, invalid identifiers, capacity refusal, and stale
  expiry reclamation pass.
- GREEN (live): the exact filtered production GameTest extended one existing
  loaded legal gate, refused a missing gate, anchored a live body proxy, and
  proved that body cleanup removed the lease under Java 25.

No unfiltered GameTest, full aggregate, stage checkbox, backlog removal,
integration, merge, push, or final acceptance is claimed here.
