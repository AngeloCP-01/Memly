# Suggested Commands

## Build
```bash
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build release APK
./gradlew build                # Full build (compile + lint + test)
./gradlew clean                # Clean build artifacts
./gradlew clean assembleDebug  # Clean rebuild
```

## Testing
```bash
./gradlew test                 # Run unit tests
./gradlew testDebugUnitTest    # Run debug unit tests only
./gradlew connectedAndroidTest # Run instrumented tests (requires device/emulator)
```

## Linting & Analysis
```bash
./gradlew lint                 # Run Android lint
./gradlew lintDebug            # Lint debug variant only
```

## Install & Run
```bash
./gradlew installDebug         # Install debug APK on connected device
adb shell am start -n com.example.memly/.MainActivity  # Launch app
```

## Dependency Management
```bash
./gradlew dependencies --configuration debugRuntimeClasspath  # Show dependency tree
```

## Gradle Version Catalog
- Dependencies managed in `gradle/libs.versions.toml`
- AGP 9.0.1 with built-in Kotlin 2.0.21
- KSP 2.0.21-1.0.28, Hilt 2.59.2, Room 2.7.1

## System Utils (macOS/Darwin)
```bash
git log --oneline -20          # Recent commits
git diff --stat                # Changed files summary
git status                     # Working tree status
find app/src -name "*.kt" | wc -l  # Count Kotlin files
```
