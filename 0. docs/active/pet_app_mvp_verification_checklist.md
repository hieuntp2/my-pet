# Pet App MVP Verification Checklist

Batch: S5.1 (MVP Verification System)
Scope: Pet App MVP closure verification only (no feature expansion)

## How to use this checklist
1. Run each item exactly as written.
2. Mark PASS only when the expected behavior is verifiably true in code/runtime.
3. Mark FAIL for partial, cosmetic-only, debug-only, or non-persisted behavior.
4. If FAIL is MVP-blocking, fix it and re-verify.

## Core loop
| ID | Item | Scenario | PASS criteria | FAIL criteria |
|---|---|---|---|---|
| CL-01 | App open reaction | Cold launch app to Home | Pet shows immediate alive response (visual greeting/reaction path) in first seconds; app-open event path exists | Static/dead start or delayed/no greeting path |
| CL-02 | Tap reaction | Tap pet from Home | Tap causes visible pet reaction and interaction feedback path | Tap does nothing or only logs debug |
| CL-03 | Feed/Play/Rest loop | Trigger each Home menu action once | Each action executes and completes response loop | Any action is dead/unwired |
| CL-04 | Visible reaction | Perform tap + each care action | User-visible avatar/talk/FX response occurs | Hidden/internal-only state change |
| CL-05 | State mutation | Perform tap + each care action, inspect state flow/persistence path | Real pet state values are mutated by interaction/action logic | Cosmetic reaction without state change |
| CL-06 | Event persistence | Perform interactions/actions, reopen app/view diary/events | Event bus publishes and persists events to Room-backed store | Events only in memory or missing after restart |

## Continuity
| ID | Item | Scenario | PASS criteria | FAIL criteria |
|---|---|---|---|---|
| CT-01 | Time-based state change | Re-open after elapsed time / resume after absence | Decay/absence path updates pet state using elapsed time | No time delta impact |
| CT-02 | Restart continuity | Force-close then relaunch | Pet state and event-derived diary continuity survive restart | State resets unexpectedly |
| CT-03 | Behavior difference after time | Return after meaningful absence | Emotion/conditions/greeting resolve from updated decayed state and differ contextually | Same behavior regardless of elapsed time |

## Experience
| ID | Item | Scenario | PASS criteria | FAIL criteria |
|---|---|---|---|---|
| EX-01 | No dead UI | Use Home menu + key entry points (Home/Diary/Settings) | Controls are wired to real actions/screens | Buttons or paths do nothing |
| EX-02 | No duplicate reactions | Trigger repeated interactions and startup/reaction paths | Single ownership/guarding avoids obvious duplicate reaction firing | Same reaction path fires twice for one trigger |
| EX-03 | No spam | Rapid repeated taps/actions/audio triggers | Cooldowns/guards suppress spam and communicate blocked states | Rapid triggers flood reactions/audio |

## Audio
| ID | Item | Scenario | PASS criteria | FAIL criteria |
|---|---|---|---|---|
| AU-01 | Correct execution or suppression | Trigger behavior-driven audio requests while varying guard conditions | Audio request path either executes or emits explicit skipped reason (cooldown/guard/disabled) | Silent drop with no deterministic handling |
| AU-02 | No random playback | Observe audio trigger sources | Playback comes from explicit request->dispatch path tied to behavior/events | Audio plays without source/request path |

## Diary
| ID | Item | Scenario | PASS criteria | FAIL criteria |
|---|---|---|---|---|
| DY-01 | Meaningful memory visibility | Interact, then open Diary | Diary shows human-readable memory cards/summaries tied to pet moments | Only technical payload feel |
| DY-02 | Not raw debug dump | Inspect diary card text and summaries | User-facing labels/messages, notable moments, summaries are present | Raw JSON/event-debug presentation as primary UX |

## Settings / reset
| ID | Item | Scenario | PASS criteria | FAIL criteria |
|---|---|---|---|---|
| SR-01 | Sound toggle works | Toggle pet sound in Settings, trigger reactions | Setting persists and audio dispatch is suppressed when disabled | Toggle has no runtime effect or does not persist |
| SR-02 | Reset yields valid fresh state | Trigger reset flow, then return to Home | Pet data clears safely; app reinitializes into valid fresh/onboarding-ready state | Broken startup, stale pet data, or invalid state after reset |

## Offline
| ID | Item | Scenario | PASS criteria | FAIL criteria |
|---|---|---|---|---|
| OF-01 | Core loop works without network | Verify core pet loop dependencies and call paths | Home loop, state updates, persistence, diary and reactions are local/offline | Core loop depends on network availability |
