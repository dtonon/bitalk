build:
    ./gradlew installDebug

build_release:
    ./gradlew assembleRelease
    ./gradlew installRelease

emulator:
    ./gradlew assembleDebug
    ./gradlew installDebug

emulator_release:
    ./gradlew assembleRelease
    ./gradlew installRelease
