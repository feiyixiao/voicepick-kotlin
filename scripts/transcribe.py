"""Local speech recognition for the Kotlin app: prints the transcript of one audio file.
Uses the openai-whisper 'base' model on this machine, nothing is sent to a cloud service.
Usage: python3 scripts/transcribe.py audio.wav"""
import sys

import whisper

model = whisper.load_model("base")
result = model.transcribe(sys.argv[1], fp16=False)
print(result["text"].strip())
