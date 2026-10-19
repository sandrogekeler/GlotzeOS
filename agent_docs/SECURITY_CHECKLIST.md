# GlotzeOS: security checklist

The threat surface of this project and how each part is held. Pair with
`SECURITY.md` (how to report) and the scanners below (what runs automatically).

## Scanners

- CodeQL (`.github/workflows/codeql.yml`): every push and PR to main, weekly.
- OpenSSF Scorecard (`.github/workflows/scorecard.yml`): weekly.
- Dependabot alerts and updates (`.github/dependabot.yml`).
- GitHub secret scanning and push protection: enable in the repo settings.

## S1. Inputs

- [ ] Other apps' labels and banners come from `PackageManager` and are
      treated as data: shown, never parsed or executed. A huge banner is a
      memory risk; decode at display size before the list grows large.
- [ ] Apps are opened only through `PackageManager`'s launch intents for a
      listed package, never from a string a user or another app supplied.
- [ ] No `INTERNET` permission, so nothing reaches the network
      (`agent_docs/CLAUDE.md`, architecture rules).

## S2. Credentials and secrets

- [ ] No secrets in the tree; `.claude/settings.json` denies reading `.env*`.
- [ ] Tokens used by CI are repo secrets with least privilege.

## S3. Paths and processes

- [ ] The app runs no shell commands and calls no `pm`; system changes are
      made by hand over ADB (`docs/device.md`).
- [ ] No cloud backup or device transfer of app data (`res/xml/*_rules.xml`).

## S4. CI and supply chain

- [ ] Every third-party action is pinned to a commit SHA.
- [ ] Workflows declare minimal `permissions:`; `pull_request_target` jobs never
      check out PR code.

## Open backlog
