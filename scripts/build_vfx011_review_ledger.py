#!/usr/bin/env python3
"""Join explicit digest-bound VFX-011 decisions into the exhaustive review ledger."""

from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "docs/quality/vfx-011-asset-audit.json"
EVIDENCE = ROOT / "docs/verification/evidence/2026-08-21-vfx-011"
DECISIONS = EVIDENCE / "review-decisions.tsv"
LEDGER = EVIDENCE / "review-ledger.tsv"
VERDICTS = {"PASS", "REPAIRED", "LIMITED"}


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def inputs() -> tuple[dict, list[dict], dict]:
    manifest = json.loads(MANIFEST.read_text())
    client_rows = list(csv.DictReader(
        (EVIDENCE / "client-capture-index.tsv").read_text().splitlines(), delimiter="\t"))
    receipt = json.loads((EVIDENCE / "two-client/receipt.json").read_text())
    if len(manifest["assets"]) != 970 or len(manifest["pageDigests"]) != 90:
        raise ValueError("asset inventory/page count drift")
    if len(manifest["pageTiles"]) != 16_887:
        raise ValueError("asset page-tile coverage drift")
    if len(client_rows) != 9_034 or len({row["screenshot"] for row in client_rows}) != 971:
        raise ValueError("client screenshot/capture-ID coverage drift")
    if len({row["page"] for row in client_rows}) != 49:
        raise ValueError("client page ownership drift")
    if not receipt.get("passed") or len(receipt.get("screenshots", [])) != 2:
        raise ValueError("two-client proof is not accepted")
    return manifest, client_rows, receipt


def expected_decision_digests() -> dict[tuple[str, str], str]:
    manifest, client_rows, receipt = inputs()
    expected: dict[tuple[str, str], str] = {}
    for asset in manifest["assets"]:
        expected[("asset_source", asset["path"])] = asset["sha256"]
    for page, page_digest in manifest["pageDigests"].items():
        expected[("asset_page", page)] = page_digest
    for row in client_rows:
        key = ("client_screenshot", row["screenshot"])
        prior = expected.setdefault(key, row["sha256"])
        if prior != row["sha256"]:
            raise ValueError(f"inconsistent screenshot digest: {row['screenshot']}")
    for page in {row["page"] for row in client_rows}:
        expected[("client_page", page)] = digest(EVIDENCE / "client-contact-sheets" / page)
    for screenshot in receipt["screenshots"]:
        expected[("two_client_capture", screenshot["file"])] = screenshot["sha256"]
    return expected


def expected_decision_keys() -> set[tuple[str, str]]:
    return set(expected_decision_digests())


def load_decisions(path: Path = DECISIONS) -> dict[tuple[str, str], dict]:
    if not path.is_file():
        raise ValueError(f"missing explicit review decisions: {path}")
    result: dict[tuple[str, str], dict] = {}
    for row in csv.DictReader(path.read_text().splitlines(), delimiter="\t"):
        key = (row["kind"], row["id"])
        if key in result:
            raise ValueError(f"duplicate review decision: {key}")
        if row["verdict"] not in VERDICTS or not row["notes"].strip():
            raise ValueError(f"blank/invalid explicit decision: {key}")
        result[key] = row
    return result


def validate_decisions(decisions: dict[tuple[str, str], dict],
                       expected_keys: set[tuple[str, str]] | None = None) -> None:
    expected = expected_decision_digests()
    keys = set(expected) if expected_keys is None else expected_keys
    missing = keys - set(decisions)
    extra = set(decisions) - keys
    if missing or extra:
        raise ValueError(f"explicit decision coverage missing={sorted(missing)[:3]} extra={sorted(extra)[:3]}")
    for key in keys:
        if decisions[key]["sha256"] != expected[key]:
            raise ValueError(f"stale explicit decision digest: {key}")


def render() -> str:
    manifest, client_rows, receipt = inputs()
    decisions = load_decisions()
    validate_decisions(decisions)
    output = io.StringIO()
    writer = csv.writer(output, delimiter="\t", lineterminator="\n")
    writer.writerow(("kind", "source", "page", "tile_or_slot", "bounds", "verdict", "notes"))
    for asset in manifest["assets"]:
        decision = decisions[("asset_source", asset["path"])]
        writer.writerow(("asset_source", asset["path"], ",".join(asset["sheetPageIds"]), "", "",
                         decision["verdict"], decision["notes"]))
    for tile in manifest["pageTiles"]:
        decision = decisions[("asset_page", tile["page"])]
        bounds = f'{tile["x"]},{tile["y"]},{tile["width"]},{tile["height"]}'
        source = f'{tile["path"]}#frame={tile["physicalFrame"]};mip={tile["mipLevel"]};bg={tile["background"]}'
        writer.writerow(("asset_tile", source, tile["page"], tile["tileId"], bounds,
                         decision["verdict"], decision["notes"]))
    for page in sorted(manifest["pageDigests"]):
        decision = decisions[("asset_page", page)]
        writer.writerow(("asset_page", "", page, "", "", decision["verdict"], decision["notes"]))
    for row in client_rows:
        decision = decisions[("client_screenshot", row["screenshot"])]
        bounds = f'{row["x"]},{row["y"]},{row["width"]},{row["height"]}'
        writer.writerow(("client_capture", row["capture_id"], row["page"], row["slot"], bounds,
                         decision["verdict"], f'{decision["notes"]}; screenshot={row["screenshot"]}'))
    for page in sorted({row["page"] for row in client_rows}):
        decision = decisions[("client_page", page)]
        writer.writerow(("client_page", "", page, "", "", decision["verdict"], decision["notes"]))
    for screenshot in receipt["screenshots"]:
        decision = decisions[("two_client_capture", screenshot["file"])]
        writer.writerow(("two_client_capture", screenshot["file"], "full_resolution", "", "",
                         decision["verdict"], decision["notes"]))
    return output.getvalue()


def main() -> None:
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--update", action="store_true")
    group.add_argument("--check", action="store_true")
    args = parser.parse_args()
    expected = render()
    if args.update:
        LEDGER.write_text(expected)
    elif not LEDGER.is_file() or LEDGER.read_text() != expected:
        raise SystemExit("VFX-011 review ledger is missing or stale; run with --update")


if __name__ == "__main__":
    main()
