---
description: "Use when writing, editing, or reviewing Compose UI code in this project. Covers screen structure, navigation model, state hoisting, state holders, debug screens, and composable discipline."
applyTo: "**/*.kt"
---

# Compose UI Guidelines

## Navigation Model

Navigation is **custom and enum-driven** — do not introduce Jetpack Navigation (`NavHost`, `NavController`).

- All top-level screens are entries in the `AppScreen` enum.
- Current route is `rememberSaveable { mutableStateOf(AppScreen.X.name) }` — a plain string.
- Screen switching is done by assigning `currentScreenName = AppScreen.Y.name`.
- Primary tab destinations use `PetPrimaryDestination` enum + `PetPrimaryNavigationBar`.

When adding a new screen:
1. Add an entry to `AppScreen`.
2. Add a `when` branch in `PetBrainApp` dispatching to the composable.
3. Pass all required state and callbacks from `PetBrainApp` — no global access inside screens.

## Screen Structure

- Root of every screen: `Column` (or `Box` for layered overlays).
- Include `PetPrimaryNavigationBar` at the top of each primary-destination screen.
- Use `verticalScroll` / `LazyColumn` for scrollable content in list screens.
- Layered screens (camera preview + overlays) use `Box` with ordered children.
- No `Scaffold` — this project does not use the Scaffold pattern.
- Composable nesting: target 3–5 levels maximum per screen.

## State Hoisting and State Holders

- **No `ViewModel`** — this project does not use `ViewModel` or `viewModel()`.
- State holders are plain Kotlin classes/stores (`*Store`, `*Controller`) created with `remember { ... }` inside `PetBrainApp`.
- Reactive state is consumed via `collectAsState()` at the composable that needs it.
- State flows **down** as immutable values or data classes; actions flow **up** as lambdas.
- Never hold mutable state inside a screen composable — hoist it to `PetBrainApp`.

```kotlin
// CORRECT — state passed down, action passed up
@Composable
fun HomeScreen(
    petState: PetState,
    onInteract: () -> Unit
) { ... }

// WRONG — reading store directly inside composable
@Composable
fun HomeScreen(store: BrainStateStore) {
    val state by store.state.collectAsState() // breaks the boundary — don't do this
}
```

## Debug Screens

- Every major subsystem must have a debug screen reachable from `DebugScreen`.
- Debug screens are added as `AppScreen` entries, navigated from debug action buttons.
- Camera overlay uses a dedicated overlay composable (`CameraDiagnosticsOverlay`) layered over preview — keep it separate from the main `CameraScreen` composable.
- Event/observation viewer screens use reactive lists via `collectAsState()` + `LazyColumn`.
- Debug screens are first-class — do not hide them behind compile flags.

## Composable Discipline

- One composable = one concern. Split large composables by extracting named private composables.
- No business logic inside composables — pass lambdas for all actions.
- Use `remember` for local derived state, `LaunchedEffect` for side effects tied to lifecycle.
- Theme access: `MaterialTheme.typography` and `MaterialTheme.colorScheme` only — do not hardcode colors or text sizes.
- `@Preview` annotations belong in `:ui-avatar` composables, not `:app` screens.
- Do not use `DisposableEffect` for CameraX cleanup unless already established in `CameraScreen` — follow the existing pattern.

## Theming

- `MaterialTheme` is applied once at app root (`MainActivity` / `PetBrainApp`).
- Screens inherit theme implicitly — do not re-wrap screens in `MaterialTheme`.
