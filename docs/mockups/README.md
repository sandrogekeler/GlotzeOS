# Mockups

Visual direction for the home screen, not a spec. App names, tape lengths and
the clock are placeholders; what the screen can actually show is decided by the
roadmap and the ADRs.

The mockups come from a design canvas (claude.ai Design artifact, private to
Sandro). Each `.dc.html` is that canvas's source for one board. It needs the
canvas runtime (`support.js`), so it does not open on its own in a browser:
read it for layout, colours and timings, and look at the `.png` for the result.

| Board | Files | What it shows |
|---|---|---|
| G · Tapes | `tapes.dc.html`, `tapes.png` | An experimental launcher: apps as VHS cassettes in a dim shelf on the left, a CRT TV on a VCR under a spotlight on the right. Up and down pull a tape out of the shelf; OK flies it into the VCR, the screen shows static, and the camera zooms until the TV screen fills the display. Back ejects. |

## Board G notes

- Palette: black ground, warm greys, cream labels; the only light is the
  spotlight on the TV and a warm glow on the selected tape.
- Timings: tape to slot 540 ms, into the VCR 520 ms, zoom 800 ms. The whole
  sequence is about 1.9 s before an app would open, too long for every launch.
  Open options: play it once per wake and let OK skip it, or let the zoom cover
  the app's own start-up time.
- The TV, VCR and cassettes are CSS boxes standing in for 3D renders. For the
  app, the plan is pre-rendered images (TV with a transparent screen, cassette
  spine and top) moved with Compose `graphicsLayer`, not a 3D engine, because
  startup speed is the product.
