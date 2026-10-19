# GlotzeOS

A fast, personal Android TV home screen, written in Kotlin with Compose for TV.
It replaces a stock launcher that shows ads before you reach an app.

Status: early. It lists the TV's apps and opens them. See
[`agent_docs/ROADMAP.md`](agent_docs/ROADMAP.md).

## Try it

Needs Android Studio (or JDK 17+ and the Android SDK) and a TV with developer
options on. [`docs/device.md`](docs/device.md) walks through connecting the TV,
installing, making GlotzeOS the home screen and undoing it.

```sh
./gradlew installDebug
```

## Development

See [`agent_docs/CLAUDE.md`](agent_docs/CLAUDE.md) for structure, commands and
conventions, and [`CONTRIBUTING.md`](CONTRIBUTING.md) for how changes land.

<!-- Created from https://github.com/sandrogekeler/AllesSandro. -->
