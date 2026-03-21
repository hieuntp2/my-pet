---
description: "Use when writing, editing, or reviewing unit tests or instrumentation tests in this project. Covers JUnit4, coroutines testing, Room testing strategy, FakeEventBus pattern, and per-module testing rules."
applyTo: "**/*Test.kt"
---

# Testing Conventions

## Libraries in Use

- **JUnit4** (`@Test`, `@Before`, `@After`) — all modules
- **`kotlinx-coroutines-test`** (`runTest`, `advanceUntilIdle`) — app, brain, memory
- **Robolectric** — memory module only, for host-side Room integration tests
- **Compose UI test** (`createAndroidComposeRule`) — app instrumentation tests only
- No JUnit5, no MockK, no Mockito, no Turbine — do not introduce them

## Coroutines and Flow Testing

- Wrap every suspending test in `runTest { }`.
- Call `advanceUntilIdle()` after triggering async work before asserting outcomes.
- Observe flows by launching a collector coroutine inside `runTest`, cancel after assertion.
- Use `flow.first()` for single-value assertions from a `StateFlow`.

```kotlin
@Test
fun `emits FACE_DETECTED after face result`() = runTest {
    val events = mutableListOf<EventEnvelope>()
    val job = launch { fakeEventBus.events.collect { events.add(it) } }
    subject.onFaceResult(fakeFaceResult)
    advanceUntilIdle()
    assertThat(events.map { it.type }).containsExactly(EventType.FACE_DETECTED)
    job.cancel()
}
```

## FakeEventBus Pattern

- Use a **file-local** `FakeEventBus` — do not share one across modules.
- Back it with `MutableSharedFlow<EventEnvelope>()` and a `publishedEvents` list.
- Assert on `publishedEvents.map { it.type }` for ordered event-type checks.

```kotlin
private class FakeEventBus : EventBus {
    val publishedEvents = mutableListOf<EventEnvelope>()
    private val _events = MutableSharedFlow<EventEnvelope>(extraBufferCapacity = 64)
    override val events: SharedFlow<EventEnvelope> = _events
    override suspend fun publish(event: EventEnvelope) {
        publishedEvents.add(event)
        _events.emit(event)
    }
}
```

## Room Testing Strategy

| Layer | Approach |
|-------|---------|
| `*Store` unit tests | Inject a `Fake*Dao` backed by a `MutableStateFlow` list |
| DAO + migration tests | `Room.inMemoryDatabaseBuilder(...)` with Robolectric, in `:memory` module only |
| End-to-end / instrumentation | Real `Room.databaseBuilder(...)`, explicit migration run, teardown in `@After` |

- **Never** use `fallbackToDestructiveMigration()` in tests — run real migrations.
- Prefer fake DAOs for store-logic tests; reserve real Room for true integration scenarios.

```kotlin
// Fake DAO example
private class FakeEventDao : EventDao {
    private val _rows = MutableStateFlow<List<EventEntity>>(emptyList())
    override fun observeAll(): Flow<List<EventEntity>> = _rows
    override suspend fun insert(entity: EventEntity) {
        _rows.value = _rows.value + entity
    }
}
```

## Per-Module Rules

| Module | What to test | Notes |
|--------|-------------|-------|
| `:core-common` | Pure math/utility functions, edge cases | No coroutines, no DB |
| `:brain` | State machines, behavior resolvers, event emission | Use fake bus + in-memory stores |
| `:memory` | Store CRUD, ordering, cursor traversal | Fake DAO for unit, inMemoryDB for host |
| `:perception` | Engine preprocess/postprocess logic | No tests for camera/ML lifecycle yet |
| `:ui-avatar` | Animation selection, orchestrator state | Synchronous, no coroutines needed |
| `:app` | Reaction engines, settings stores, smoke flows | FakeEventBus + runTest; instrumentation for full DB paths |

## General Rules

- Tests must be **deterministic** — no `Thread.sleep`, no real timers.
- Use `TemporaryFolder` (JUnit Rule) for DataStore file-backed tests.
- One test class per production class. Name it `<ClassName>Test`.
- Do not test framework code (Room internals, ML Kit inference) — test your own logic around it.
- Leave `:perception` camera/lifecycle code untested until a test harness is established.
