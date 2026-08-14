# Item catalogue live acceptance — `f076572`

This bundle records one real Fabric 26.2 client connected to a dedicated Fabric
server rendering every one of the 260 item IDs in the generated QA-005 register.
The client was an operator only so the scenario could prepare the visual catalogue;
normal gameplay permissions and acquisition contracts remain covered by their
existing production-entrypoint tests.

`scenario.tsv` places nine registered items into the real player hotbar, waits for
the server response, and captures one contact sheet per batch. `client.log` contains
exactly 260 accepted item-replace commands and 260 translated success responses,
with no unknown-command, invalid-item, disconnect, or spam failure. The 29 batch
screenshots show all inventory icons and the selected first-person model. The first
screenshot is the automatic connection capture; the last is the empty-inventory
zero-residue diagnostic after the catalogue is cleared.

Batch `N` contains lines `((N - 1) * 9 + 1)` through `min(N * 9, 260)` of
`item-ids.txt`. Screenshots are chronological: the automatic capture is first,
then batches 1–29, then the final diagnostic. This deterministic mapping is also
recorded per item in the generated manual-acceptance results ledger.
