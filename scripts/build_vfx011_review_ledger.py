#!/usr/bin/env python3
"""Join explicit digest-bound VFX-011 decisions into the exhaustive review ledger."""

from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "docs/quality/vfx-011-asset-audit.json"
EVIDENCE = ROOT / "docs/verification/evidence/2026-08-21-vfx-011"
DECISIONS = EVIDENCE / "review-decisions.tsv"
LEDGER = EVIDENCE / "review-ledger.tsv"
VERDICTS = {"PASS", "REPAIRED", "LIMITED", "PENDING_RAW_RECAPTURE"}
GUI_SCALE_CAPTURE = re.compile(r"(?:^|/)scale([1-4])(?:/|$)")


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def validate_runtime_scale_bindings(rows: list[dict]) -> None:
    for row in rows:
        options = row.get("runtimeOptions", {})
        for capture_id in row.get("captureIds", []):
            match = GUI_SCALE_CAPTURE.search(capture_id)
            if match is None:
                continue
            nominal = int(match.group(1))
            requested = options.get("requestedGuiScale")
            effective = options.get("effectiveGuiScale")
            if requested != nominal or effective != nominal:
                raise ValueError(
                    f"nominal GUI scale {nominal} does not match requested/effective "
                    f"runtime scale {requested}/{effective}: {capture_id}")


def validate_client_command_receipt(evidence: Path, run_receipt: dict) -> None:
    path = evidence / "client-command-receipt.json"
    if not path.is_file():
        raise ValueError("fresh client command terminal receipt is missing")
    receipt = json.loads(path.read_text())
    if receipt.get("result") != "PASS" or receipt.get("exitCode") != 0:
        raise ValueError("fresh client command did not record terminal success")
    expected_command = [
        "./gradlew", "runClientGameTest", "-Pvfx011ClientOnly", "--rerun-tasks",
        "--no-daemon", "--console=plain",
    ]
    if receipt.get("command") != expected_command:
        raise ValueError("fresh client command contract drift")
    if receipt.get("implementationCommit") != run_receipt.get("implementationCommit"):
        raise ValueError("fresh client command implementation binding drift")
    if receipt.get("jar", {}).get("sha256") != run_receipt.get("jar", {}).get("sha256"):
        raise ValueError("fresh client command JAR binding drift")
    emitted = receipt.get("clientEmittedMetadata", {})
    emitted_path = evidence / run_receipt["clientEmittedMetadata"]["file"]
    if (emitted.get("rows") != 971
            or emitted.get("sha256") != digest(emitted_path)
            or emitted.get("sha256") != run_receipt["clientEmittedMetadata"]["sha256"]):
        raise ValueError("fresh client command metadata binding drift")
    raw = receipt.get("rawScreenshots", {})
    capture_ids = receipt.get("captureIds", {})
    if (raw.get("rows") != 971 or raw.get("uniqueScreenshots") != 971
            or raw.get("verifiedDigests") != 971
            or capture_ids.get("rows") != 9_034 or capture_ids.get("unique") != 9_034):
        raise ValueError("fresh client command coverage drift")
    transcript_info = receipt.get("transcript", {})
    transcript = evidence / transcript_info.get("file", "")
    if not transcript.is_file() or transcript_info.get("sha256") != digest(transcript):
        raise ValueError("fresh client command transcript binding drift")
    terminal = transcript.read_text(errors="replace")
    if "BUILD SUCCESSFUL" not in terminal or "VFX011_CLIENT_COMMAND_EXIT=0" not in terminal:
        raise ValueError("fresh client command transcript lacks terminal success")


def inputs(evidence: Path = EVIDENCE) -> tuple[dict, list[dict], dict | None]:
    manifest = json.loads(MANIFEST.read_text())
    client_rows = list(csv.DictReader(
        (evidence / "client-capture-index.tsv").read_text().splitlines(), delimiter="\t"))
    if len(manifest["assets"]) != 970 or len(manifest["pageDigests"]) != 90:
        raise ValueError("asset inventory/page count drift")
    if len(manifest["pageTiles"]) != 16_887:
        raise ValueError("asset page-tile coverage drift")
    if len(client_rows) != 9_034 or len({row["screenshot"] for row in client_rows}) != 971:
        raise ValueError("client screenshot/capture-ID coverage drift")
    if len({row["page"] for row in client_rows}) != 49:
        raise ValueError("client page ownership drift")
    fresh_receipt_path = evidence / "client-run-receipt.json"
    receipt = None
    if fresh_receipt_path.is_file():
        if (evidence / "two-client").exists():
            raise ValueError("fresh exact-build bundle must exclude separately built two-client proof")
        fresh = json.loads(fresh_receipt_path.read_text())
        emitted_rows = [json.loads(line) for line in
                        (evidence / fresh["clientEmittedMetadata"]["file"]).read_text().splitlines()
                        if line.strip()]
        validate_runtime_scale_bindings(emitted_rows)
        if fresh.get("clientEmittedMetadata", {}).get("rows") != 971:
            raise ValueError("fresh client receipt row count drift")
        raw = fresh.get("rawScreenshots", {})
        if raw.get("rows") != 971 or raw.get("uniqueContentFiles") != 971:
            raise ValueError("fresh raw screenshot receipt coverage drift")
        validate_client_command_receipt(evidence, fresh)
    else:
        receipt = json.loads((evidence / "two-client/receipt.json").read_text())
        if not receipt.get("passed") or len(receipt.get("screenshots", [])) != 2:
            raise ValueError("two-client proof is not accepted")
    return manifest, client_rows, receipt


def expected_decision_digests(evidence: Path = EVIDENCE) -> dict[tuple[str, str], str]:
    manifest, client_rows, receipt = inputs(evidence)
    expected: dict[tuple[str, str], str] = {}
    for asset in manifest["assets"]:
        expected[("asset_source", asset["path"])] = asset["sha256"]
    for page, page_digest in manifest["pageDigests"].items():
        expected[("asset_page", page)] = page_digest
    fresh = (evidence / "client-run-receipt.json").is_file()
    raw_index: dict[str, dict] = {}
    if fresh:
        raw_index = {row["screenshot"]: row for row in csv.DictReader(
            (evidence / "client-raw-index.tsv").read_text().splitlines(), delimiter="\t")}
        emitted = [json.loads(line) for line in
                   (evidence / "client-emitted-captures.jsonl").read_text().splitlines()]
        emitted_by_name = {row["screenshot"]: row for row in emitted}
        if len(raw_index) != 971 or len(emitted_by_name) != 971:
            raise ValueError("fresh raw/emitted metadata coverage drift")
    client_kind = "client_raw" if fresh else "historical_client_digest"
    for row in client_rows:
        key = (client_kind, row["screenshot"])
        prior = expected.setdefault(key, row["sha256"])
        if prior != row["sha256"]:
            raise ValueError(f"inconsistent screenshot digest: {row['screenshot']}")
        if fresh:
            indexed = raw_index.get(row["screenshot"])
            emitted_row = emitted_by_name.get(row["screenshot"])
            if indexed is None or emitted_row is None:
                raise ValueError(f"fresh screenshot is not retained: {row['screenshot']}")
            content = evidence / indexed["content_path"]
            if (indexed["sha256"] != row["sha256"]
                    or emitted_row.get("screenshotSha256") != row["sha256"]
                    or not content.is_file() or digest(content) != row["sha256"]):
                raise ValueError(f"fresh screenshot digest binding drift: {row['screenshot']}")
    for page in {row["page"] for row in client_rows}:
        expected[("client_page", page)] = digest(evidence / "client-contact-sheets" / page)
    if receipt is not None:
        for screenshot in receipt["screenshots"]:
            expected[("two_client_capture", screenshot["file"])] = screenshot["sha256"]
    return expected


def expected_decision_keys(evidence: Path = EVIDENCE) -> set[tuple[str, str]]:
    return set(expected_decision_digests(evidence))


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
                       expected_keys: set[tuple[str, str]] | None = None,
                       evidence: Path = EVIDENCE) -> None:
    expected = expected_decision_digests(evidence)
    keys = set(expected) if expected_keys is None else expected_keys
    missing = keys - set(decisions)
    extra = set(decisions) - keys
    if missing or extra:
        raise ValueError(f"explicit decision coverage missing={sorted(missing)[:3]} extra={sorted(extra)[:3]}")
    for key in keys:
        if decisions[key]["sha256"] != expected[key]:
            raise ValueError(f"stale explicit decision digest: {key}")
        if key[0] == "historical_client_digest" and decisions[key]["verdict"] != "PENDING_RAW_RECAPTURE":
            raise ValueError(f"historical client digest cannot claim visual acceptance: {key}")
        if key[0] == "client_raw" and decisions[key]["verdict"] == "PENDING_RAW_RECAPTURE":
            raise ValueError(f"retained fresh client raw requires a reviewed verdict: {key}")
        if key[0] == "client_page" and decisions[key]["verdict"] != "LIMITED":
            raise ValueError(f"client contact page is navigation-only: {key}")


def render(evidence: Path = EVIDENCE) -> str:
    manifest, client_rows, receipt = inputs(evidence)
    decisions = load_decisions(evidence / "review-decisions.tsv")
    validate_decisions(decisions, evidence=evidence)
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
    fresh = (evidence / "client-run-receipt.json").is_file()
    client_kind = "client_raw" if fresh else "historical_client_digest"
    output_kind = "client_capture" if fresh else "client_capture_pending_raw"
    for row in client_rows:
        decision = decisions[(client_kind, row["screenshot"])]
        bounds = f'{row["x"]},{row["y"]},{row["width"]},{row["height"]}'
        writer.writerow((output_kind, row["capture_id"], row["page"], row["slot"], bounds,
                         decision["verdict"], f'{decision["notes"]}; screenshot={row["screenshot"]}'))
    for page in sorted({row["page"] for row in client_rows}):
        decision = decisions[("client_page", page)]
        writer.writerow(("client_page", "", page, "", "", decision["verdict"], decision["notes"]))
    if receipt is not None:
        for screenshot in receipt["screenshots"]:
            decision = decisions[("two_client_capture", screenshot["file"])]
            writer.writerow(("two_client_capture", screenshot["file"], "full_resolution", "", "",
                             decision["verdict"], decision["notes"]))
    return output.getvalue()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--evidence", type=Path, default=EVIDENCE)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--update", action="store_true")
    group.add_argument("--check", action="store_true")
    args = parser.parse_args()
    evidence = args.evidence.resolve()
    ledger = evidence / "review-ledger.tsv"
    expected = render(evidence)
    if args.update:
        ledger.write_text(expected)
    elif not ledger.is_file() or ledger.read_text() != expected:
        raise SystemExit("VFX-011 review ledger is missing or stale; run with --update")


if __name__ == "__main__":
    main()
