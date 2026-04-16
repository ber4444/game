# Stockfish packaging

    This project vendors official prebuilt Stockfish Android executables as packaged native binaries so `StockfishEngine` can launch them from Android's native library directory at runtime.

## Included ABIs

- `app/src/main/jniLibs/arm64-v8a/libstockfish.so`
- `app/src/main/jniLibs/armeabi-v7a/libstockfish.so`

## Upstream source

Version: `sf_18`

Downloaded from the official Stockfish GitHub releases:

- `https://github.com/official-stockfish/Stockfish/releases/download/sf_18/stockfish-android-armv8.tar`
- `https://github.com/official-stockfish/Stockfish/releases/download/sf_18/stockfish-android-armv7-neon.tar`

## License

Upstream license and documentation copied into:

- `docs/Stockfish-COPYING.txt`
- `docs/Stockfish-README.md`

## Runtime behavior

On Android devices reporting a supported ABI such as `arm64-v8a` or `armeabi-v7a`, `StockfishEngine` will prefer the packaged real executable from `nativeLibraryDir`.

If the app runs on an ABI without a bundled Stockfish binary, the existing embedded fallback path is still used.

## Why the earlier asset approach stayed off

The earlier packaging put Stockfish under `assets/` and extracted it into the app's writable files directory.
That works poorly on modern Android because writable app storage is often not executable.
So the engine could be found but still fail to launch, leaving the UI in the off state.

