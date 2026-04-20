# game

Compose Multiplatform chess app targeting:

- Android
- Linux desktop (JVM)
- Web (Wasm)

## Setup

For the desktop target, install stockfish first:

```bash
sudo apt install stockfish  # For Ubuntu/Debian
sudo pacman -S stockfish    # For Arch
sudo dnf install stockfish  # For Fedora
```

## Project layout

- `app/src/commonMain` shared chess UI, game rules, and compose resources
- `app/src/androidMain` Android-specific shared implementation and Stockfish integration
- `androidApp/src/main` Android application manifest that depends on the shared KMP module
- `app/src/desktopMain` desktop launcher
- `app/src/wasmJsMain` web launcher

## Useful Gradle tasks

- `./gradlew test` runs shared unit tests
- `./gradlew assembleDebug installDebug` builds and installs the Android app
- `./gradlew :app:desktopRun` launches the desktop app
- `./gradlew :app:wasmJsBrowserDevelopmentRun` starts the web target
- `./gradlew :app:connectedAndroidDeviceTest` runs Android UI tests

<img width="1768" height="2208" alt="Screenshot_20260416_142830" src="https://github.com/user-attachments/assets/3dc55dee-90e0-4aad-85ea-fab60a22a132" />
