# DroidClean

DroidClean is an Android device diagnostics and safe-cleanup project.

## Current prototype

- Kotlin and Jetpack Compose dashboard
- Device model and Android version
- Available and total RAM snapshot
- Available storage snapshot for the app's storage volume
- In-app update screen
- Checks the latest GitHub Release for a newer semantic version
- Downloads an APK with Android DownloadManager
- Starts Android's package installer only after the user taps Install

## Publishing an update

1. Increase `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Build the APK with the same persistent signing key used for the previous release.
3. Create a GitHub Release with a tag such as `v0.3.0`.
4. Attach the APK to that release. Its filename must end in `.apk`.
5. The app checks:
   `https://api.github.com/repos/Postmen1971/DroidClean/releases/latest`

## Important signing requirement

Android only installs an update over an existing app when both APKs are signed with the same signing certificate. The current CI debug build uses the runner's debug key and is not suitable for repeatable in-place updates across separate GitHub Actions runs. Before distributing updates, configure a persistent release signing key through GitHub Actions Secrets. Never commit the keystore or its passwords to this public repository.

The updater only downloads APKs attached to the official repository's GitHub Releases. Android still asks the user to confirm installation. On Android 8+, the user may need to allow DroidClean to install unknown apps in system settings.

## Compatibility

- Minimum Android version: Android 8.0 (API 26)
- Compile SDK: 35
- Java: 17

## Limitations

Android restricts what ordinary applications can clean or terminate. DroidClean uses public Android APIs and explicit user consent. It does not claim to clear other apps' private caches or boost performance where Android does not allow it.
