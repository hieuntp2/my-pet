phase1_interaction_demo_spec.md
# Phase 1 Interaction Demo Specification

Version: v1  
Purpose: Define the interaction scenarios that must work before Phase 1 of the AI Pet Robot project is considered complete.

This document describes **user-visible behaviors** that verify the system works end-to-end.

These demos validate the integration of:

- perception
- recognition
- behavior engine
- avatar animation
- audio response
- memory updates

If all demos pass, the Android pet is considered **alive enough for Phase 1**.

---

# 1. Demo Environment

The demo should run on a physical Android device with:


camera enabled
microphone enabled
audio playback enabled


The application should start in **Pet Mode**, not Debug Mode.

Main screen must show:


camera preview
avatar face
minimal UI controls


Debug screens may exist but must not be the primary interface.

---

# 2. Demo Scenario Overview

Phase 1 includes four interaction demonstrations.

| Demo | Capability Tested |
|-----|------------------|
Owner Greeting | face detection + recognition + behavior |
Stranger Curiosity | unknown person reaction |
Sound Reaction | audio perception |
Idle Behavior | internal state change |

Each demo validates a different part of the architecture.

---

# 3. Demo 1 — Owner Greeting

## Goal

Verify that the robot recognizes its owner and reacts emotionally.

---

## Setup

Before running the demo:


teach the owner's face using TeachPerson flow
ensure the person is marked as owner


---

## Steps

1. Start the application.
2. Ensure camera preview is visible.
3. Owner enters camera view.

---

## Expected Perception Events


FACE_DETECTED
FACE_EMBEDDING_CREATED
PERSON_RECOGNIZED(owner)


---

## Expected Brain Reaction


BrainState → HAPPY


---

## Expected Avatar Behavior

Avatar should:


eyes widen
smile
blink faster


---

## Expected Audio

Robot should play a greeting clip such as:


happy chirp
greeting tone


---

## Expected Memory Update

The system should record:


PersonSeenEvent
WorkingMemory update
familiarity score increase


---

## Success Criteria

User should clearly perceive:


robot recognized them
robot reacted happily


---

# 4. Demo 2 — Stranger Curiosity

## Goal

Verify the robot reacts differently to unknown people.

---

## Setup

Ensure the stranger is **not in the Person database**.

---

## Steps

1. Start the application.
2. A new person enters camera view.

---

## Expected Events


FACE_DETECTED
PERSON_UNKNOWN


---

## Expected Brain Reaction


BrainState → CURIOUS


---

## Expected Avatar Behavior

Avatar should:


tilt expression
wide eyes
slower blinking


---

## Expected Audio

Possible responses:


curious chirp
questioning tone


---

## Success Criteria

Robot visibly reacts with curiosity.

---

# 5. Demo 3 — Sound Reaction

## Goal

Verify the robot reacts to environmental sound.

---

## Steps

1. Start the application.
2. Produce a loud sound near the microphone (clap or speak).

---

## Expected Events


SOUND_ENERGY_CHANGED
SOUND_DETECTED
VOICE_ACTIVITY_STARTED


---

## Expected Brain Reaction

Possible states:


CURIOUS
ALERT


---

## Expected Avatar Behavior


quick blink
alert eyes
attention expression


---

## Expected Audio

Possible sound:


acknowledgment chirp


---

## Success Criteria

Robot responds to sound within ~1 second.

---

# 6. Demo 4 — Idle Behavior

## Goal

Verify the pet has internal state changes without external stimuli.

---

## Steps

1. Start the application.
2. Leave the device untouched.

---

## After 30 seconds

Expected:


BrainState → CALM


---

## After 60 seconds

Expected:


BrainState → SLEEPY


---

## Avatar Behavior


slow blinking
relaxed expression
lower animation activity


---

## Optional Audio


soft idle sound
sleepy chirp


---

# 7. Debug Observability

During demos the system should still log events for debugging.

Available tools:


Event Viewer
Observation Viewer
Audio Debug Screen


These tools help verify that:


perception events are emitted
behavior rules trigger
memory updates occur


However these tools should **not be required for normal interaction**.

---

# 8. Phase 1 Acceptance Criteria

Phase 1 is complete when:


robot reacts to owner
robot reacts to strangers
robot reacts to sound
robot changes mood over time


User should experience:


a responsive digital pet


Even without advanced AI conversation.

---

# 9. Expected User Experience

When opening the app, the user should see:


camera preview
animated robot face


Within seconds the pet should:


look at the environment
react to people
respond to sound
change mood over time


The pet should feel:


alive
curious
responsive


---

# 10. Future Phase Extensions

Future phases may add:


object recognition
voice commands
conversation
long-term memory
personality evolution
robot body movement


These are not required for Phase 1.

---

# End of Document