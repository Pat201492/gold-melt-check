# Gold Melt Check — working notes

Android app (Kotlin, Jetpack Compose, Material 3) that converts a gold quote in any currency to USD
and shows it as a percent of melt value. See README.md for the user-facing behaviour.

## Layout

```
app/src/main/java/com/goldmeltcheck/
  core/   pure Kotlin: MeltMath, Catalog (currencies, purities), PriceParsers. No Android imports.
  data/   PriceStore (DataStore cache), PriceApi (HTTP), PriceSync (WorkManager)
  ui/     GoldScreen, GoldViewModel, Theme
app/src/test/  JUnit tests for core/
```

## Rules

- **Offline first.** The calculator must always work from saved prices. Network code never blocks the UI.
- **Background sync is WiFi only** (`NetworkType.UNMETERED`). Only the explicit "Refresh now" button may
  use mobile data.
- Keep `core/` free of Android imports so it stays unit-testable on the JVM. New math goes there, with tests.
- Price sources must be free and keyless. If one changes shape, fix `PriceParsers` and its tests together.
- Never show a bundled estimate as live: anything not fetched is labelled as an estimate or the user's number.

## Checks

`./gradlew testDebugUnitTest assembleDebug` must pass before opening a PR. CI runs the same on every PR.

## CI hygiene (private repo, shared Actions quotas)

- CI runs on `pull_request` only (plus manual dispatch); no duplicate `push:` trigger.
- One job; new checks are steps in it. Keep `concurrency` + `cancel-in-progress`, `timeout-minutes`,
  and `permissions: contents: read`.
- APKs ship via GitHub Releases (release.yml, on `v*` tags), never as long-lived artifacts.
