#!/bin/sh
# Needs a JDK and kotlinc:  brew install openjdk kotlin
# Usage: ./build.sh test | run | run --console
set -e
cd "$(dirname "$0")"
export PATH="/opt/homebrew/opt/openjdk/bin:/opt/homebrew/bin:$PATH"
mkdir -p build
case "$1" in
  test) kotlinc src/main/kotlin/voicepick/*.kt src/test/kotlin/voicepick/Tests.kt -include-runtime -d build/tests.jar 2>&1 | grep -v "^warning: " || true
        java -cp build/tests.jar voicepick.TestsKt ;;
  run)  shift; kotlinc src/main/kotlin/voicepick/*.kt -include-runtime -d build/voicepick.jar 2>&1 | grep -v "^warning: " || true
        java -jar build/voicepick.jar "$@" ;;
  audio) ./scripts/make_demo_audio.sh
        kotlinc src/main/kotlin/voicepick/*.kt -include-runtime -d build/voicepick.jar 2>&1 | grep -v "^warning: " || true
        java -jar build/voicepick.jar --audio build/audio/u1.wav build/audio/u2.wav build/audio/u3.wav ;;
  *)    echo "usage: ./build.sh test | run [--console] | audio"; exit 2 ;;
esac
