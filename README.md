# game

Compose Multiplatform chess app targeting:

- Android
- Linux desktop (JVM)
- Web (Wasm)

## Project layout

- `app/src/commonMain` shared chess UI, game rules, and compose resources
- `app/src/androidMain` Android launcher and Stockfish integration
- `app/src/desktopMain` desktop launcher
- `app/src/wasmJsMain` web launcher

## Useful Gradle tasks

- `./gradlew test` runs shared unit tests
- `./gradlew :app:desktopRun` launches the desktop app
- `./gradlew :app:wasmJsBrowserDevelopmentRun` starts the web target
- `./gradlew connectedAndroidTest` runs Android UI tests

<img width="1768" height="2208" alt="Screenshot_20260416_142830" src="https://github.com/user-attachments/assets/3dc55dee-90e0-4aad-85ea-fab60a22a132" />
