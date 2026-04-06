# Phase 1 Batch A - Home Avatar Signal Chain Audit

Date: 2026-04-04  
Scope: N1 audit note for the active production signal chain only.

## Active production chain

1. `PetBrainApp` runs behavior medium-loop (`BehaviorEngine -> BehaviorExperienceBinder -> PetIntentionExecutor`).
2. Executor emits `activeVisualIntent` (`PixelPetAvatarIntent`) plus execution debug state.
3. `PetBrainApp` builds `HomePixelPetAvatarSignal` from:
   - pet emotion/conditions/brain state
   - perception flags
   - behavior-driven visual intent
   - behavior source intention
   - attention mode/intensity
   - greeting/transient reaction windows
4. `RealPixelPetBridgeStateAdapter` converts signal -> `HomePixelPetAvatarBridgeInput`.
5. `HomePixelPetAvatarIntentResolver` applies priority policy and resolves one intent.
6. `PixelPetBridgeState` feeds `HomePixelPetAvatar` -> `FaceAnimationRuntime`.

## Active module ownership

- Production runtime: `ui-avatar` (`LiveFaceAnimationCanvas`, `FaceAnimationRuntime`).
- Home bridge and policy layer: `app/avatar`.
- Legacy module `pixel-avatar` is not the active home runtime path.

## Priority model (current)

1. behavior-driven intent (with attention/stimulus tuning)
2. greeting boost
3. transient tap/long-press reaction
4. transient sound reaction
5. baseline signal candidate priority (`processing -> engaged -> excited -> asking -> looking -> ... -> neutral`)

## Debug visibility

- `BehaviorIntelligenceDebugScreen` shows:
  - behavior engine intention/candidates
  - behavior execution mapping
  - attention mode/intensity/target
- `PixelPetBridgeDebugMetadata` exposes final chosen intent + reason + source summary.
