# Gold Melt Check — working notes

Android app (Kotlin, Jetpack Compose, Material 3) that converts a gold or silver quote in any currency to USD
and shows it as a percent of melt value. See README.md for the user-facing behaviour.

## Layout

```
app/src/main/java/com/goldmeltcheck/
  core/   pure Kotlin: MeltMath, Metal (gold/silver: purities, bands), Catalog (currencies), PriceParsers, GemMath, Gems
          (lab-grown signs + dated price ballparks), History + Alerts (trend stats, alert rules). No Android imports.
  data/   PriceStore (DataStore cache + price history), PriceApi (HTTP), PriceSync (WorkManager), PriceAlerts (notifications),
          HistorySeed (loads the bundled year of history on launch)
  ui/     GoldScreen (Check), RatesScreen (Metal Rates), TrendsScreen (Trends), CurrencyScreen (Currency Rates), GemsScreen (Gems), GoldViewModel, Theme
app/src/test/  JUnit tests for core/
```

## Rules

- **Offline first.** The calculator must always work from saved prices. Network code never blocks the UI.
- **Network use:** exchange rates refresh only on WiFi (`NetworkType.UNMETERED`). The hourly gold/silver check
  (history + alerts) and the explicit "Refresh now" button may use any connection; keep those requests tiny.
- Anything metal-specific (purities, fallback spot, markup bands) belongs on `Metal`, not in a `when` in the UI,
  so a new metal is one enum entry.
- Keep `core/` free of Android imports so it stays unit-testable on the JVM. New math goes there, with tests.
- Price sources the app calls must be free and keyless. If one changes shape, fix `PriceParsers` and its tests together.
  The one keyed source is gold-api.com `/history`, called only by release.yml (repo secret `GOLD_API_KEY`) to bundle
  a year of daily history as `assets/history_XAU.json` / `history_XAG.json`; never ship a key in the app.
- Never show a bundled estimate as live: anything not fetched is labelled as an estimate or the user's number.
- Gem price ballparks are static and dated (`Gems.PRICES_AS_OF`); keep the date current whenever you change them,
  and leave a range null rather than guess one. Lab-grown signs are hints, never presented as proof.

## Checks

`./gradlew testDebugUnitTest assembleDebug` must pass before opening a PR. CI runs the same on every PR.

## CI hygiene (private repo, shared Actions quotas)

- CI runs on `pull_request` only (plus manual dispatch); no duplicate `push:` trigger.
- One job; new checks are steps in it. Keep `concurrency` + `cancel-in-progress`, `timeout-minutes`,
  and `permissions: contents: read`.
- APKs ship via GitHub Releases (release.yml, when a GitHub Release is published or a `release/v*` branch is pushed), never as long-lived artifacts.
