# GlotzeOS

A fast, personal Android TV home screen

<!-- Project-owned. Seeded once from sandrogekeler/AllesSandro; rewrite freely.
Keep it under ~200 lines: it is imported into every session by ../CLAUDE.md.
Put rules that only matter for some files in .claude/rules/ with `paths:`
frontmatter instead, so they load only when those files are touched. -->

## Stack
- Kotlin, Gradle (Kotlin DSL, versions in `gradle/libs.versions.toml`)
- TODO: frameworks and versions that matter

## Project structure

TODO: the top-level directories and what lives in each.

## Architecture rules

TODO: the few rules a change must not break (layering, IPC boundaries, where
state lives). Short and checkable beats long and aspirational.

## Build & dev commands

```sh
./gradlew check
python3 .claude/suite-check.py   # every health check in .claude/suite.json
```

## Task tracking
Work is tracked in GitHub Issues. Every issue carries a `type:`, an `area:` and
a priority (`p0`-`p3`) label. Close issues from the pull request with
`Fixes #N`.

## Commits & pull requests

- A merged pull request's title is public release-notes copy: imperative,
  sentence case, no trailing period, no em dashes.
- One concern per pull request.
- Every pull request needs a `type:` and an `area:` label before it can merge
  (the `pr-labelled` status). Which `type:`: see `.github/pull_request_template.md`.

## Files that come from the template

This repo was created from sandrogekeler/AllesSandro and is kept in step with it
by Renovate (`.copier-answers.yml` records the version). Two kinds of file:

- **Locked** (re-applied on every template update): `.claude/suite-*.py`,
  `.github/workflows/{codeql,scorecard,pr-labelled,labels,template-guard}.yml`
  and the optional `aislop.yml`, `claude.yml`, `.aislop/base.yml`,
  `.github/scripts/release-notes.py`. Do not edit them here: change
  AllesSandro instead. A local edit is kept but shows up as a merge conflict on
  the next template update.
- **Seeded** (created once, owned by this repo): everything else, including
  this file.

A template-update PR with `<<<<<<< before updating` markers: see
`.claude/rules/template-files.md`.

## Do not

- Commit secrets, `.env` files or credentials.
- Edit generated files or lockfiles by hand; regenerate them.

## Deeper rules, loaded on demand

`.claude/rules/*.md`: each one names the paths that load it.
