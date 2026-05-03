# Fitness

Personal Android app — a Freeletics Gym Coach replica. Single user, sideloaded.

Project plan: `C:\Users\peter\.claude\plans\here-s-a-handoff-prompt-pure-teacup.md`.

## Setup

1. Install [Android Studio](https://developer.android.com/studio) (bundles JDK 17 and the Android SDK).
2. Open this folder in Android Studio. Accept the prompt to download the Gradle wrapper jar and the missing SDK components on first sync.
3. First sync takes 5–10 minutes (downloads Gradle 8.11.1, AGP 8.7.3, Compose, Hilt, Room). Subsequent syncs are seconds.
4. Run the `app` configuration on a connected device or emulator.

Before the first build, set Drive sync exclusions (next section).

## Google Drive sync — required exclusions

This project lives inside `G:\My Drive\Fitness App`. Without exclusions, Drive will try to sync gigabytes of transient build outputs and will conflict-loop with Gradle's file locks. **Set these once** after the first Gradle sync:

1. Open Drive desktop preferences → **Google Drive** → your account → **Folders from your computer** (or **Sync settings**, varies by version).
2. For the project folder, set sync mode to **Mirror files** (so they live on disk and are not virtual).
3. In the project root, exclude these subfolders from sync — Drive desktop's UI varies; the most reliable approach is folder-level **Stop syncing** via right-click in File Explorer, or via the Drive icon's per-folder options:
   - `.gradle/`
   - `build/`
   - `app/build/`
   - `.idea/`
   - `app/.cxx/` (if it ever appears)

If a folder above doesn't exist yet, create it as an empty folder, exclude it, then delete it — the exclusion persists.

Verification: trigger a Gradle sync from Android Studio, then watch Drive's **Activity** panel. You should see *zero* mentions of the excluded folders. If you see them, the exclusion didn't take.

## Project layout (Phase 0)

```
.
├── app/                                   the only Gradle module today
│   ├── build.gradle.kts
│   └── src/main/java/com/peter/fitness/
│       ├── FitnessApplication.kt          Hilt + WorkManager + notification channels
│       ├── MainActivity.kt
│       ├── core/ui/theme/                 Material 3 theme
│       ├── feature/hello/                 placeholder screen confirming the wiring
│       └── service/timer/                 RestTimerService stub (Phase 2)
├── gradle/libs.versions.toml              version catalogue
├── detekt.yml
├── settings.gradle.kts
├── build.gradle.kts
└── .github/workflows/ci.yml
```

## Common commands

After Android Studio has bootstrapped the wrapper:

| Command | Purpose |
|---|---|
| `./gradlew assembleDebug` | Build debug APK |
| `./gradlew testDebugUnitTest` | Run JVM unit tests (JUnit 5 + Kotest) |
| `./gradlew detekt` | Static analysis |
| `./gradlew check` | Tests + Detekt + lint |
| `./gradlew installDebug` | Build and install on a connected device |

## Phase 0 deliverable

The app installs, opens, and shows a placeholder screen with a tap counter. CI runs Detekt, unit tests, and `assembleDebug` on every push to `main` and every pull request.

## Verifying Phase 0

After Android Studio finishes its first sync, run these in order:

1. **Build**: `./gradlew assembleDebug` — completes without errors. APK at `app/build/outputs/apk/debug/`.
2. **Unit tests**: `./gradlew testDebugUnitTest` — `HelloViewModelTest` runs and passes (2 tests).
3. **Static analysis**: `./gradlew detekt` — no issues reported.
4. **Install + run**: `./gradlew installDebug` (phone in developer mode + USB debugging) or run from Android Studio. The app should open to "Phase 0 wired up", a tap counter, and a "Tap me" button that increments on tap.
5. **CI**: push the repo to GitHub. The `CI` workflow runs Detekt, unit tests, and `assembleDebug`; goes green within ~5 minutes.

If any step fails, capture the error and we'll diagnose before moving to Phase 1.

Phases 1–8 are documented in the project plan.
