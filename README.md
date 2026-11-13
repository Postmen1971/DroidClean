# DroidClean

DroidClean is an Android device diagnostics and safe-cleanup project.

## Current prototype

- Kotlin and Jetpack Compose dashboard
- Device model and Android version
- Available and total RAM snapshot
- Available storage snapshot for the app's storage volume
- No automatic deletion and no intrusive permissions

## Build

The GitHub Actions workflow builds a debug APK on every push to `main` and on manual dispatch.

The APK is published as the workflow artifact **DroidClean-debug-apk**.

## Compatibility

- Minimum Android version: Android 8.0 (API 26)
- Compile SDK: 35
- Java: 17

## Important limitations

Android restricts what ordinary applications can clean or terminate. DroidClean will use public Android APIs and explicit user consent. It will not claim to clear other apps' private caches or boost performance where Android does not allow it.
