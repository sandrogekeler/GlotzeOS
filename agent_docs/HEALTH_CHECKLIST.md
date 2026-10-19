# GlotzeOS: health checklist

What "healthy" means for this repo, in four parts. `python3 .claude/suite-check.py`
runs the mechanical half from `.claude/suite.json`; this file is the half that
needs judgement. Add an item when a real problem teaches you one, with the
issue or PR that taught it.

## 1. Clean

- [ ] Every health command in `.claude/suite.json` passes, none skipped.
- [ ] No dead code, commented-out blocks or TODOs without an issue number.
- [ ] Generated files are regenerated, never hand-edited.

## 2. Stable

- [ ] Errors are handled or deliberately propagated, never silently dropped.
- [ ] Tests cover the behaviour a user relies on, not only the happy path.
- [ ] Every screen is usable with the remote alone: focus starts somewhere,
      moves predictably, and Back never leaves the home screen.

## 3. Scalable / future-proof

- [ ] Architecture rules in `agent_docs/CLAUDE.md` still describe the code.
- [ ] Dependencies are recorded in `agent_docs/DEPENDENCIES.md`.

## 4. Performant

- [ ] Cold start on the TV: not measured yet. The first Macrobenchmark run
      sets the budget, and it is a ratchet: lowered as it improves, never
      raised to let a change through.
- [ ] Release APK size: 1.6 MB unsigned on 2026-10-19 (R8 and resource
      shrinking on). Watch it when adding a dependency.
- [ ] No `PackageManager` query, file read or bitmap decode on the main thread.

## Open backlog

Findings not yet fixed, each with an issue link.
