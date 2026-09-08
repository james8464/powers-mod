# INT-014 verification status

Status: **implementation staged; focused verification green; independent review pending.**

Light and Dark Heralds now accept a finite opposition-stagger reaction only
from the opposite mythic artifact or its corresponding Concord. The neutral
First Vessel accepts either alignment, so coordinated setup is rewarded but no
party composition or player faction is mandatory. Reactions are readable
through a fracture rune, outline, weakness, slowing, sound, and encounter
overlay; each boss owns a transient 60-tick cooldown.

Artifact activation inspects at most sixteen nearby living candidates and only
reacts after the authoritative artifact action commits. Concords use their
existing twenty-four-target bound. Both paths preserve damage and movement
protection, and matching Herald artifacts remain ordinary legal play rather
than a required counter.

## Test-driven record

- RED: the focused policy test failed to compile before the boss/stimulus
  matrix existed.
- GREEN: the matrix covers both Herald alignments, all four First Vessel
  stimuli, cooldown behavior, and finite durations.
- RED: GameTest compilation failed before the runtime reaction owner existed.
- GREEN (live): the exact filtered production GameTest activated a dark
  artifact through `ArtifactWeaponManager`, exposed only the Light Herald,
  breached the First Vessel, then formed a Radiant Concord through
  `ConcordCastManager` and proved the second First Vessel accepted it.
- GREEN: focused Java 25 unit tests, GameTest compilation, resource validation,
  generated interaction-document verification, Java/non-item asset audits,
  all 240 Python fixture tests, and `git diff --check` pass.

No unfiltered GameTest, full aggregate, stage checkbox, backlog removal,
integration, merge, push, or final acceptance is claimed here.
