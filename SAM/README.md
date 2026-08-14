# SAM — Shortcut APK Maker

SAM temporarily acts as your Android launcher to intercept shortcut pin requests, then generates a standalone installable APK for each shortcut using a patched template.

## How it works

1. **Set as Launcher** — tap the button in SAM to set it as the home app.
2. **Pin a shortcut** — open any app and use its "add shortcut" feature. SAM intercepts the request.
3. **APK is built** — SAM patches the template APK with the shortcut's label, icon, and intent, signs it, and launches the installer.
4. **Done** — the installed APK appears on your home screen and launches the shortcut directly.

## Requirements

- Android 8.0+ (API 26)
- `template.apk` placed in `app/src/main/assets/` before building (see below)

## Building

### 1. Build `template.apk`

The template APK is a minimal stub app that queries SAM's ContentProvider at runtime to resolve and fire the shortcut intent.

- Open the [SAMTemplate](../SAMTemplate/) project in Android Studio.
- Build a **release** APK (Build → Generate Signed Bundle/APK → APK → release).
- Rename the output to `template.apk`.
- Place it at `SAM/app/src/main/assets/template.apk`.

`template.apk` is excluded from version control because it is a binary artifact.

### 2. Build SAM

- Open this project in Android Studio.
- Build and install as usual (`Run` or `./gradlew installDebug`).

## Architecture

| Component | Role |
|---|---|
| `HomeFakeActivity` | Fake launcher — receives the HOME intent so SAM can catch shortcut pins |
| `ShortcutCatcherActivity` | Handles `CONFIRM_PIN_SHORTCUT`; extracts label/icon/intent and triggers APK generation |
| `APKPatcher` | Unzips template, patches AXML manifest (package + label), replaces icons, re-zips, signs with V1+V2 |
| `AXMLPatcher` | In-place binary AXML string pool patcher (same-length replacements only) |
| `KeystoreManager` | Manages a persistent RSA 2048 signing key in the Android Keystore |
| `SAMContentProvider` | Returns shortcut intent URIs to installed template APKs via `content://fyi.reign.sam.provider/shortcut/{pkg}` |
| `MainActivity` | Compose dashboard listing all captured shortcuts |
| Room DB | Persists shortcut entries (label, intent URI, generated package name, icon path, install status) |

## Package naming

Generated APKs use the package `fyi.reign.sam.shortcut.{11 hex chars}` — exactly the same byte length as the placeholder `fyi.reign.sam.shortcut.PLACEHOLDER`, enabling in-place binary patching of the compiled manifest without changing any file offsets or chunk sizes.
