# Pleos SpeechToText connection

The Pleos flavor uses `ai.pleos.playground:SpeechToText:3.3.1` with `Mode.ON_DEVICE` and `Locale.KOREA`. The demo flavor retains Android on-device SpeechRecognizer. The Java domain engine contains no SDK calls. No server or hybrid fallback, project credentials, or audio uploads are configured.

`pleos.car.permission.STT_SERVICE` is declared only in the Pleos manifest. The local AVD declares it as a normal permission and grants it on install. The app separately asks for foreground `RECORD_AUDIO` consent before starting microphone use. A user's explicit story start initiates the final voice session after narration.

Initialization uses `OnInitializeListener`; the per-request `ResultListener` ignores interim text and duplicate finals. Generation checks reject callbacks after cancellation, background, or restriction changes. Stop removes the listener and stops the SDK; close also releases its connection. Existing engine checks gate rewards. Transcripts and microphone buffers are not retained or logged by app code.

## Runtime verification on the local Pleos AVD

- SDK initialization and Korean locale configuration: actual success callbacks.
- `STT_SERVICE` permission: granted on install.
- PCM recognition: generated Korean speech, 16 kHz / 16-bit / mono, streamed through official `sendAudio` and `completeAudioSend` APIs.
- Final SDK recognition result matched `별빛을 밝혀라` after removing spaces and punctuation. No recognition callback was fabricated.
- App microphone permission was granted through the system prompt; the story finale sent an actual SDK `request()`. With no spoken input, the application's timeout paused playback rather than awarding a stamp.
- Live human microphone recognition, child speech accuracy, and real vehicle acoustics remain unverified. PCM success does not prove those.
- Demo/Pleos builds, both lint variants, and 48 existing domain assertions passed during integration.

Raw test output: `verification/pleos-stt/pcm-runtime.txt`.

## Reproduce the isolated SDK recognition check

```bash
./gradlew :app:assemblePleosDebug :app:assemblePleosDebugAndroidTest -PpleosTestRunner=com.parkonwheels.demo.PleosSpeechVerification
adb install -r app/build/outputs/apk/pleos/debug/app-pleos-debug.apk
adb install -r app/build/outputs/apk/androidTest/pleos/debug/app-pleos-debug-androidTest.apk
adb shell am instrument -w com.parkonwheels.pleos.test/com.parkonwheels.demo.PleosSpeechVerification
```

The property selects a speech-only test runner; omitting it retains the existing capability runner. The test refuses to run on a real vehicle. Its PCM is generated test data and is packaged only in the test APK.

## Official references

- https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro
- https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/SpeechToText/importing-sdk
- https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/SpeechToText/declaring-permissions
- https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/SpeechToText/initialization-and-release
- https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/SpeechToText/result-listeners
- https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/SpeechToText/sending-audio-data
