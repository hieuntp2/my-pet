---
description: "Use when writing, editing, or reviewing any code in this AI Pet Robot project. Covers module boundaries, event-driven architecture, Android coding style, Room persistence, and agent task discipline."
applyTo: "**"
---

# AI Pet Robot — Copilot Instructions

## Architecture Data Flow

Always follow this direction. Never invert it.

```
Camera/Input → Perception → Events → Memory → Brain → Avatar/UI
```

- `:perception` publishes events. It never reads from `:memory` or `:brain`.
- `:brain` consumes events and owns behavior/personality state. It never touches UI directly.
- `:memory` persists and queries data. It never makes behavior decisions.
- `:ui-avatar` renders state. It never publishes events or calls business logic directly.
- `:core-common` is a pure utility layer. No business logic, no domain types.
- `:app` wires everything together with **manual dependency injection** (no Hilt/Dagger/Koin).

## Module Boundaries

| Module | Owns | Must Not |
|--------|------|---------|
| `:app` | Screen routing, DI wiring, lifecycle | Contain business logic |
| `:brain` | State machines, events, behavior, personality | Touch DB or UI directly |
| `:memory` | Room DB, DAOs, repositories, stores | Make behavior decisions |
| `:perception` | CameraX pipeline, ML Kit, frame analysis | Read from `:brain` or `:memory` |
| `:ui-avatar` | Avatar composables, pixel rendering | Publish events or call repositories |
| `:core-common` | Math utils, config, base types | Have module-level dependencies |

Never move business logic into `:app`. Never add cross-cutting concerns to single modules.

## Event-Driven Rules

- Important state changes become **events** (`EventBus.publish(...)`).
- Events are not optional. Use them for: perception results, behavior triggers, memory changes, user interactions.
- Events are typed via `EventType` enum and wrapped in `EventEnvelope`.
- Use `InMemoryEventBus` for real-time flow; `RoomEventStore` for persistence.
- Never bypass the event bus to directly call behavior methods from perception code.

## Android / Kotlin Coding Style

### State management
- Use `StateFlow` for UI-observable state. No `LiveData`, no ad-hoc mutable globals.
- Use `*Store` suffix for stateful holders (e.g., `BrainStateStore`).
- State is hoisted in composables — pass state down, events up.

### Persistence
- Use **Room** for all structured persistent data.
- Suffix: `*Entity` for DB entities, `*Dao` for DAOs, `Room*` for Room-backed implementations.
- Always write a Room migration when changing schema. Never use `fallbackToDestructiveMigration()` in production.

### Naming conventions
- `*Engine` — processes input and produces output (e.g., `FaceEmbeddingEngine`)
- `*Resolver` — makes a decision from inputs (e.g., `BehaviorResolver`)
- `*Mapper` — transforms data between layers
- `*Repository` — abstracts data access (interface + `Room*` implementation)
- `*Store` — holds and exposes stateful data

### Composables
- Keep composables small and focused. One composable = one concern.
- No business logic inside composables. Pass lambdas for actions.
- Use `remember` and `collectAsState` for local state and flows.

### Coroutines
- Prefer `viewModelScope` or `CoroutineScope(SupervisorJob() + Dispatchers.Default)` — never `GlobalScope`.
- Use `runCatching` / `Result` for recoverable errors. Do not swallow exceptions silently.
- All DB and IO work goes on `Dispatchers.IO`.

## Agent Task Discipline

- **Build after every task**: run `./gradlew assembleDebug` before claiming a task is done.
- **Smallest complete vertical slice**: implement one capability end-to-end rather than partial layers.
- **No fake logic**: no `TODO`, no empty method bodies, no `NotImplementedError` in production code.
- **No dead code**: do not add helpers, abstractions, or screens "for future use".
- **No silent refactors**: do not rename files, restructure packages, or change patterns unless the task requires it.
- If tests exist for touched code, also run `./gradlew test`.

## Phase Constraints

- **Phase 1 (current)**: offline-first. No cloud AI, no vector DB, no LLM calls.
- **Phase 2**: LLM may suggest intents/speech/emotions. Android brain validates and decides what to execute.
- **Phase 3**: Android sends high-level commands only. ESP32/MCU handles realtime control and safety.

## Error Handling

- Surface meaningful error state in UI (don't show blank screens on failures).
- Log failures from model loading, camera init, and DB operations.
- Prefer explicit error states in `StateFlow` over try/catch swallowing.
