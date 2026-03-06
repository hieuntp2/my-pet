# AI Pet Robot — Agent Instructions

## Source of truth
Always read these files before changing architecture or creating new modules:

- docs/project_manifest.md
- docs/development_roadmap.md
- docs/02_android_pet_brain_architecture.md
- docs/06_personality_engine.md
- docs/07_robot_memory_system.md

For Phase 2:
- docs/03_ai_cloud_integration.md

For Phase 3:
- docs/04_robot_body_hardware.md
- docs/05_full_system_architecture.md

## Non-negotiable rules
- Do not use mock implementations for production logic.
- Do not leave empty methods, TODO placeholders, or fake data for core flows.
- Every task must end with a buildable, runnable result.
- If a task changes code, run the build before finishing.
- If a screen or feature is added, provide a visible way to trigger and verify it.
- Prefer small, isolated changes over large refactors.
- Do not change architecture unless the docs require it.

## Task execution rule
For each task:
1. Read only the minimum relevant docs.
2. Implement the smallest complete vertical slice.
3. Run build.
4. Report:
   - files changed
   - what works now
   - how to verify
   - any remaining risks

## Android verification
Use this command after each task:

./gradlew assembleDebug

If tests exist for the touched module, run them too.

## Output expectations
Never say a task is complete unless:
- code compiles
- app builds
- feature is visible or verifiable
- no placeholder implementation remains