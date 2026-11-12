# GlotzeOS: roadmap

Intent, not record. What is done lives in the code and the closed issues; this
file says what is next and why. Unchecked items are not done.

## Now: foundations, then on the TV

Tracked in #17. The systems everything later sits on, and the tooling to test
them on the PC and the TV, come before features (Sandro, 2026-11-12).

- [ ] Local testing: `tools/tv.py`, the TV emulator, `docs/testing.md`.
- [ ] Core systems: live app list, a launch that cannot crash the home
      screen, lazy banners at card size, StrictMode, focus tests.
- [ ] Startup measurement: Macrobenchmark and the Baseline Profile generator.
- [ ] Connect the TV over ADB and install the release build (`docs/device.md`).
      Record the model and Android version with `tools/tv.py info`.
- [ ] Set `minSdk` to the TV's API level. Live Edit on the TV needs Android 11
      (API 30) or newer.
- [ ] Make GlotzeOS the home screen on that TV, with the undo tested first.
- [ ] Measure cold start on the TV (Macrobenchmark) and write the number into
      `HEALTH_CHECKLIST.md` as the first budget.

## Next: a home screen worth keeping

- [ ] Baseline Profile, generated on the TV and committed, so first launch is
      compiled code (the generator exists; the profile does not yet).
- [ ] Personalised home: favourites row, own order, hidden apps (stored locally;
      the storage decision comes first, #30).
- [ ] Intro animation when the TV wakes.
- [ ] Transitions between the home screen and an app opening.
- [ ] Design tokens, so colours, spacing and durations live in one place (#29).
- [ ] Screenshot tests, once the design settles.

## Later

- [ ] A custom boot animation. System-level: may need root or may be impossible
      on the target TV. Research per model before promising anything.
- [ ] Clock or weather on the home screen. Weather needs the network, which is
      an ADR first (see the no-network rule in `agent_docs/CLAUDE.md`).
