# Gold Melt Check

An Android app for checking a gold quote. Pick the currency you're quoted in, enter the weight in
grams, the purity, and the asking price. It shows:

- the price in **US dollars**
- the price as a **percent of melt value**, with a label (below melt / close to melt / moderate markup / high markup)
- melt value, premium over melt, price per gram, and grams of pure gold

## Offline first, refreshes on WiFi

Everything runs on the phone. The gold spot price and exchange rates are saved on the device, so the
calculator works with no signal, using the last saved prices and showing how old they are.

| When | What happens |
|---|---|
| Every ~6 hours, on WiFi only | Background refresh (WorkManager, `NetworkType.UNMETERED`). Never uses mobile data. |
| App opened | Queues a refresh that runs once you're on WiFi. |
| **Refresh now** button | Refreshes right away over any connection. |
| You type your own spot or rate | Saved as an override and labelled "Your number"; **Use live** goes back. |

Until the first successful refresh the app uses bundled estimates and says so.

### Price sources (free, no API key)

- Gold spot (USD per troy oz): [gold-api.com](https://gold-api.com) — `GET https://api.gold-api.com/price/XAU`
- Exchange rates (units per USD): [ExchangeRate-API open access](https://www.exchangerate-api.com/docs/free) — `GET https://open.er-api.com/v6/latest/USD`

## The math

```
price in USD     = asking price ÷ (currency units per 1 USD)
melt value (USD) = grams × purity ÷ 31.1034768 × spot (USD/oz)
percent of melt  = price in USD ÷ melt value × 100
```

Lives in `app/src/main/java/com/goldmeltcheck/core/MeltMath.kt`, with no Android imports, and is
unit tested.

## Install on your phone

1. Open the repo's **Releases** page on your phone and download the latest `gold-melt-check-x.y.z.apk`.
2. Open it. Android will ask you to allow installs from your browser or files app the first time.

### Cutting a release

On GitHub (the website or the mobile app): **Releases → Draft a new release**, type a new tag such as
`v0.1.0`, then **Publish release**. The **Release** workflow tests, builds a signed APK, and attaches
it to that release within about 5 minutes.

### One-time: signing key

Android only installs an update over an existing app if both are signed with the same key. Make a
key once and store it as repo secrets (Settings → Secrets and variables → Actions):

```bash
keytool -genkeypair -v -keystore release.jks -alias goldmelt -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks   # paste the output into GMC_KEYSTORE_BASE64
```

| Secret | Value |
|---|---|
| `GMC_KEYSTORE_BASE64` | base64 of `release.jks` |
| `GMC_KEYSTORE_PASSWORD` | the keystore password |
| `GMC_KEY_ALIAS` | `goldmelt` |
| `GMC_KEY_PASSWORD` | the key password |

Keep `release.jks` somewhere safe outside the repo (it's gitignored). Without these secrets, releases
are signed with a throwaway debug key and you'll have to uninstall before each update.

## Develop

Open the folder in Android Studio, or:

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

Stack: Kotlin, Jetpack Compose (Material 3), DataStore, WorkManager. minSdk 26, targetSdk 35.
