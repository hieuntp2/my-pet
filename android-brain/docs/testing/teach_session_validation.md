# Teach Session Persistence Validation (C60)

This guide standardizes the local test path for teach-session completion persistence validation.

## Prerequisites

- Android emulator or physical device is connected and visible to `adb devices`.
- Run from `android-brain/`.
- Debug variant and default instrumentation runner are used (`androidx.test.runner.AndroidJUnitRunner`).

## Standard command path

Run the full local validation flow in order:

1. `./gradlew assembleDebug`
2. `./gradlew test`
3. `./gradlew :app:connectedTeachSessionPersistenceDebugAndroidTest`

`connectedTeachSessionPersistenceDebugAndroidTest` is the standardized alias for running connected instrumentation validation.

## Optional focused connected run

To run only teach-session persistence instrumentation tests:

```bash
./gradlew :app:connectedTeachSessionPersistenceDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.aipet.brain.app.ui.persons.TeachPersonInstrumentationTest#teachPerson_completionConfirmationPersistsAndRestoresForSameTeachSessionId_onDevice,com.aipet.brain.app.ui.persons.TeachPersonInstrumentationTest#teachPerson_cleanupResetClearsPersistedCompletionForSameTeachSessionId_onDevice,com.aipet.brain.app.ui.persons.TeachPersonInstrumentationTest#teachPerson_completionDoesNotRestoreForDifferentTeachSessionId_onDevice
```

## Expected teach-session persistence coverage

Instrumentation class:

- `com.aipet.brain.app.ui.persons.TeachPersonInstrumentationTest`

Expected test methods:

- `teachPerson_completionConfirmationPersistsAndRestoresForSameTeachSessionId_onDevice`
- `teachPerson_cleanupResetClearsPersistedCompletionForSameTeachSessionId_onDevice`
- `teachPerson_completionDoesNotRestoreForDifferentTeachSessionId_onDevice`

## Design boundary reminder

- Completion restore is by design scoped to the **same** `teachSessionId`.
- A different `teachSessionId` must not restore prior completion state.

## Environment note

- Connected instrumentation execution remains environment-dependent.
- If no emulator/device is connected, connected tasks fail with `No connected devices!`.
