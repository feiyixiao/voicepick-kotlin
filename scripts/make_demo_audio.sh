#!/bin/sh
# Makes three spoken demo utterances with the macOS 'say' command (voice Anna, German).
set -e
cd "$(dirname "$0")/.."
mkdir -p build/audio
i=1
for text in "Bitte nimm drei Stück von A 17" "Bestätigt" "Hol mir zwei von B 4"; do
  say -v Anna "$text" -o "build/audio/u$i.aiff"
  ffmpeg -loglevel error -y -i "build/audio/u$i.aiff" -ar 16000 -ac 1 "build/audio/u$i.wav"
  i=$((i+1))
done
ls build/audio/*.wav
