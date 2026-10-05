# Home Market Android

Native Android client for **Home Market Manager**, a family shopping list with a weekly list, roles
(admin and member), a shopping mode and purchase history. It uses the same Firebase backend as the
existing [web app](https://github.com/crlsribeiro/home-market-app) and
[iOS app](https://github.com/crlsribeiro/home-market-app-ios), and aims for feature parity with the iOS
app.

The app is in early development. Today it opens on a start screen; sign-in and the shopping list come in
the next milestones. [`docs/feature-parity.md`](docs/feature-parity.md) tracks every iOS feature and its
status on Android.

## Stack

| Area | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 (dynamic color on Android 12+, light and dark) |
| Architecture | MVVM, data / domain / presentation layers |
| Dependency injection | Hilt (KSP) |
| Local storage | Room |
| Navigation | Navigation Compose with type-safe routes |
| Backend | Firebase Auth, Cloud Firestore, Cloud Storage, Cloud Messaging |
| Tests | JUnit, MockK, Turbine, kotlinx-coroutines-test, Compose UI tests |
| Quality | Android Lint, detekt, ktlint |
| CI | GitHub Actions on every pull request |

Minimum SDK 26, compile and target SDK 37. All library versions live in
[`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Project layout

```
app/src/main/java/app/carlosribeiro/homemarket/
├── data/           Room, Firestore and Storage data sources, mappers, repositories
├── domain/         Plain Kotlin models, repository interfaces, use cases
├── presentation/   Compose screens, ViewModels, navigation and theme
└── di/             Hilt modules (added with the first module)
```

## Backend documentation

- [`docs/backend.md`](docs/backend.md): the Firestore and Storage data model the app reads and writes,
  and the decisions made for the Android client.
- [`docs/backend-proposals.md`](docs/backend-proposals.md): backend changes found while building the
  app. The Android app never changes the backend itself.

## Set up Firebase locally

The app needs a `google-services.json` from the Firebase project. The file is git-ignored and must never
be committed.

1. Open the [Firebase console](https://console.firebase.google.com/) and select the Home Market project.
2. Go to **Project settings → Your apps** and select the Android app with package
   `app.carlosribeiro.homemarket`.
3. Download `google-services.json` and put it in the `app/` folder.
4. For Google Sign-In (from M1), add your debug SHA-1 to the same Android app in the console. Get it
   with `./gradlew signingReport`.

CI builds without the real file: the workflow writes a placeholder `google-services.json` before the
build.

## Build and run

Open the project in Android Studio, let Gradle sync (install Android SDK Platform 37 if it asks), pick a
device and press **Run**.

From the terminal:

```bash
./gradlew assembleDebug          # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug           # installs on a connected device
```

## Tests and checks

```bash
./gradlew testDebugUnitTest      # unit tests
./gradlew connectedDebugAndroidTest   # Compose UI tests, needs a device or emulator
./gradlew lint detekt ktlintCheck     # Android Lint, detekt and ktlint
./gradlew ktlintFormat           # fixes formatting
```

CI runs `assembleDebug testDebugUnitTest lint detekt ktlintCheck` on every pull request.

## Contributing

Work happens on feature branches (`feat/`, `fix/`, `docs/`, `chore/`, `test/`) with one pull request per
feature. Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/). Pull
requests must be green in CI before review.

## License

[MIT](LICENSE)
