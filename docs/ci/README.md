# CI: stamping the release tag into the APK version

`build-appshare.yml` in this folder is the **proposed replacement** for
`.github/workflows/build-appshare.yml`.

It cannot be pushed automatically: GitHub refuses `git push` and the Contents API
for any file under `.github/workflows/` unless the client has the `workflows`
permission, which the Genspark GitHub App does not have.

> `! [remote rejected] ... refusing to allow a GitHub App to create or update
> workflow '.github/workflows/build-appshare.yml' without 'workflows' permission`

## How to apply it

1. Open <https://github.com/DaknamaTv/DarkNamaApp/edit/genspark_ai_developer/.github/workflows/build-appshare.yml>
2. Select all, and paste the contents of `docs/ci/build-appshare.yml`.
3. Commit to the `genspark_ai_developer` branch.

Or from a local clone (a normal user push is not restricted):

```bash
cp docs/ci/build-appshare.yml .github/workflows/build-appshare.yml
git add .github/workflows/build-appshare.yml
git commit -m "ci: inject release tag as app versionName/versionCode"
git push
```

## What changes

| | Before | After |
|---|---|---|
| `Get tag name` step | runs **after** `Build APK` | runs **before** `Build APK` |
| Gradle invocation | `./gradlew assembleRelease` | `./gradlew assembleRelease -PappVersionName="${APP_VERSION_NAME}"` |
| APK `versionName` | always the fallback (`2.1.0`) | the release tag (`v2.5.0` → `2.5.0`) |
| APK `versionCode` | derived from fallback (`20100`) | derived from the tag (`2.5.0` → `20500`) |
| Asset file name | `DarkNama-universal.apk` | `DarkNama-v2.5.0-universal.apk` |
| Release notes | — | includes `**App version:** 2.5.0` |

`APP_VERSION_NAME` is the tag with a leading `v`/`V` stripped, so the version
shown inside the app is `2.5.0`, not `v2.5.0`.

## Where the version lives in the app

`app/build.gradle.kts` resolves the version once and feeds
`BuildConfig.VERSION_NAME` / `BuildConfig.VERSION_CODE`:

```kotlin
val fallbackVersionName = "2.1.0"                       // local builds only

val resolvedVersionName: String =
    (project.findProperty("appVersionName") as String?) // CI: -PappVersionName
        ?.trim()?.removePrefix("v")?.removePrefix("V")
        ?.takeIf { it.isNotBlank() }
        ?: fallbackVersionName
```

`versionCode` is computed as `major * 10000 + minor * 100 + patch`
(`2.5.0` → `20500`) and can still be overridden with `-PappVersionCode=<n>`.

Consumers of `BuildConfig.VERSION_NAME`:

* `screens/AboutScreen.kt` — version line under the app name (`R.string.app_version`)
* `screens/SettingsScreen.kt` — "current version" row (`R.string.current_version`)
* `screens/SettingsScreen.kt` — comparison against the latest GitHub release tag

## Tagging rules

`versionCode` must increase monotonically or Android refuses to install the
update over an older build:

| Tag | `versionCode` | Installs over previous |
|---|---|---|
| `v2.1.0` | 20100 | — |
| `v2.2.0` | 20200 | yes |
| `v2.2.1` | 20201 | yes |
| `v2.0.9` | 20009 | no (lower) |

The workflow triggers on tags matching `v*`, so tags must start with `v`.
