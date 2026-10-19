# GlotzeOS: roadmap

Intent, not record. What is done lives in the code and the closed issues; this
file says what is next and why. Unchecked items are not done.

## Now: on the TV

The app builds and lists apps. Nothing has run on real hardware yet.

- [ ] Connect the TV over ADB and install the debug build (`docs/device.md`).
      Record the model and Android version in `docs/device.md`.
- [ ] Set `minSdk` to the TV's API level. Live Edit on the TV needs Android 11
      (API 30) or newer.
- [ ] Make GlotzeOS the home screen on that TV, with the undo tested first.
- [ ] Measure cold start on the TV (Macrobenchmark) and write the number into
      `HEALTH_CHECKLIST.md` as the first budget.

## Next: a home screen worth keeping

- [ ] Baseline Profile, generated on the TV, so first launch is compiled code.
- [ ] Refresh the app list when an app is installed or removed.
- [ ] Personalised home: favourites row, own order, hidden apps (stored locally).
- [ ] Intro animation when the TV wakes.
- [ ] Transitions between the home screen and an app opening.
- [ ] Design tokens, so colours, spacing and durations live in one place.
- [ ] Compose UI tests for remote focus (Robolectric) and screenshot tests.

## Later

- [ ] A custom boot animation. System-level: may need root or may be impossible
      on the target TV. Research per model before promising anything.
- [ ] Clock or weather on the home screen. Weather needs the network, which is
      an ADR first (see the no-network rule in `agent_docs/CLAUDE.md`).
