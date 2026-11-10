# Cat Board

Cat Board is an iOS cat image gallery built with Kotlin Multiplatform and Compose Multiplatform. Kotlin owns the application logic and UI. The Swift host provides the application entry point and Core ML screening.

## Architecture

| Module | Boundary |
|---|---|
| [cat-api](modules/cat-api) | Paged requests to The Cat API, response validation, and requested-count limits |
| [image-store](modules/image-store) | Durable FIFO URL consumption and background replenishment with Room |
| [image-loader](modules/image-loader) | Coil loading, bounded image caches, and encoded image data |
| [image-screener](modules/image-screener) | The screening interface and immutable settings |
| [image-prefetcher](modules/image-prefetcher) | Durable queues of accepted images, partitioned by screening settings |
| [app](modules/app) | Constructor injection, ViewModel state, Compose screens, and the exported iOS framework |

The [iOS convention plugin](build-logic/src/main/kotlin/catboard.ios.gradle.kts) owns the supported targets. The application exports one static framework. Platform-specific networking, database construction, preferences, and image data conversion live in `iosMain`; application logic and UI live in `commonMain`.

The gallery loads progressively, fetches more images at the older end of the list, and starts a new gallery at the display limit. Its tiered layout uses lazy groups of square images. Settings changes cancel pending work and discard prefetched images from the previous policy.

Screening is disabled by default in the simulator and enabled by default on devices. Normal mode accepts safe images; scary mode accepts unsafe images. Classification failures are returned as errors and do not accept images. The implementation is [CoreMLClassifier.swift](ios/CatBoardApp/CoreMLClassifier.swift), using the package declared in [project.yml](ios/project.yml).

## Toolchain and build

The prerequisites are an Apple Silicon Mac, Xcode, JDK 17 or newer, Homebrew with Mint, and the Ruby selected by [ios/.ruby-version](ios/.ruby-version). Kotlin dependencies and plugins are defined in [the Gradle version catalog](gradle/libs.versions.toml); Swift tools are pinned to reviewed commits in [ios/Mintfile](ios/Mintfile).

The [Kotlin compatibility guide](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html) describes supported toolchain combinations. [CI](.github/workflows/ci.yml) selects an available Apple Silicon runner and a compatible Xcode version.

Setup, framework compilation, and simulator application compilation are available from the repository root:

```sh
make setup
make framework
make build-ios
```

`make setup` installs Ruby dependencies and Swift tools, then generates `ios/CatBoardApp.xcodeproj`. Xcode builds invoke Gradle to build and embed the Kotlin framework. The generated project is available for development in Xcode.

The unsigned archive target is `make build-release`. Its output is `ios/fastlane/build/release/archive/CatBoardApp.xcarchive`. Signing lanes use `APPLE_ID` and `TEAM_ID` from the environment.

## Verification

```sh
make test
```

This runs Kotlin simulator tests, Swift screening-boundary tests, and XCTest UI tests. The simulator selector reads the installed inventory and chooses an iPhone matching the active Xcode SDK. `LOCAL_SIMULATOR_UDID` selects a specific simulator when provided.

The narrower targets are `make test-kotlin`, `make unit-test`, and `make ui-test`. `make fix` formats and autocorrects Swift; `make check` applies those fixes before format and lint validation.

The UI tests use explicit launch arguments for deterministic local image data and simulated errors. Their contracts cover initial display, refresh, retry, loading beyond the initial batch, and settings changes. Native accessibility identifiers come from Compose test tags.

Automated validation uses simulators. Physical-device screening and scroll-performance measurements are outside this migration's verification scope.
