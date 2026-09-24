# PLAN.md — Execution plan (Phases 1–6, pre-stretch)

Derived from `ROADMAP.md`. Numbered steps are meant to be done in order; each maps to one roadmap checklist item. Stretch goals are intentionally excluded — do not start them until Phase 6 is fully done.

**Current state (as of 2026-09-24):** the repo is a freshly generated KMP wizard project (`shared`, `androidApp`, `desktopApp`, `iosApp`) with only the default "Hello" template code (`Greeting`, `Platform`, `App.kt`). No domain/data/presentation split, no networking, no persistence. Git repo initialized locally (not yet pushed to GitHub).

## Phase 1 — Project setup

1. [x] Confirm the wizard-generated project (already done — Android + iOS + Compose Multiplatform shared UI, plus an extra `desktopApp` target) builds cleanly: `./gradlew build`.
2. [ ] Split `shared` into `shared:domain`, `shared:data`, `shared:presentation` modules (or equivalent source-set packages if keeping one Gradle module); move existing template code out or delete it.
3. [ ] Set up `gradle/libs.versions.toml` as the single version catalog for every dependency added from here on.
4. [ ] Add Koin: one `commonModule` in shared code plus a `platformModule` per target via `expect`/`actual`.
5. [ ] Add ktlint or detekt plus a root `.editorconfig`.
6. [ ] Replace the default "Hello" screen with a placeholder "Hello chat" screen and run it on an Android emulator and the iOS simulator.
7. [ ] Initialize git and push to a public GitHub repo with a placeholder README.

**Done when:** both apps launch and `./gradlew build` passes locally.

## Phase 2 — Domain and streaming API

8. [ ] Model the domain: `Conversation`, `Message(role, content, status, createdAt)`, `MessageStatus` (Sending, Streaming, Done, Failed).
9. [ ] Define `ChatRepository` interface in `domain` with `streamReply(conversationId, prompt): Flow<ChatEvent>`.
10. [ ] Write `SendMessageUseCase`: save the user message, then collect the stream.
11. [ ] Add Ktor client (OkHttp engine on Android, Darwin on iOS) with `ContentNegotiation` and `Logging` plugins.
12. [ ] Define request/response DTOs with kotlinx.serialization plus mappers to domain types.
13. [ ] Implement SSE parsing into `ChatEvent.Delta`, `ChatEvent.Done`, `ChatEvent.Error`.
14. [ ] Map transport/HTTP errors to domain error types: no network, 401 bad key, 429 rate limit, timeout.
15. [ ] Support cancellation: stopping Flow collection cancels the underlying HTTP call.
16. [ ] Add `FakeChatRepository` that streams canned text so UI work never needs a real API key.

**Done when:** a unit test streams a fake SSE response through Ktor `MockEngine` and gets the right sequence of events.

## Phase 3 — Persistence and secure key storage

17. [ ] Add Room KMP (or SQLDelight) with tables for conversations and messages.
18. [ ] Expose DAO queries as `Flow` so the UI updates live as tokens arrive.
19. [ ] Batch streamed-delta writes to the DB (e.g. every 50 ms) instead of writing per token.
20. [ ] Make the DB the single source of truth: network layer writes, UI only reads from DB flows.
21. [ ] On app start, mark any message stuck in `Streaming` as `Failed` with a retry option.
22. [ ] Define `expect class SecureStorage`; implement `actual` with EncryptedSharedPreferences/Keystore on Android and Keychain on iOS.
23. [ ] Store settings (model name, temperature, theme) in DataStore KMP.
24. [ ] Add a DB migration test (even trivial: version 1 → 2 adding one column).

**Done when:** killing the app mid-stream and reopening it leaves history intact with the partial reply marked failed.

## Phase 4 — Shared ViewModel and UI

25. [ ] Create `ChatViewModel` (androidx lifecycle KMP) exposing a single `StateFlow<ChatUiState>`.
26. [ ] Model intents as a sealed interface: `Send`, `Stop`, `Retry`, `NewConversation`, `SelectConversation`.
27. [ ] Build screens: conversation list, chat, settings (API key, model).
28. [ ] Build the chat screen: message bubbles, streaming cursor, auto-scroll that stops when the user scrolls up.
29. [ ] Render Markdown and code blocks in replies.
30. [ ] Add empty/loading/error-with-retry/offline-banner states.
31. [ ] Add Compose Multiplatform navigation between screens.
32. [ ] Add dark mode, dynamic type/font scaling, and content descriptions for accessibility.
33. [ ] Add copy-message and share via `expect`/`actual` (Android share intent, iOS `UIActivityViewController`).

**Done when:** a full conversation works on both platforms, a reply can be stopped mid-stream, and a failed one can be retried.

## Phase 5 — Testing and CI

34. [ ] `commonTest`: use cases tested against fake repositories.
35. [ ] `commonTest`: `ChatViewModel` state transitions with Turbine and `runTest`.
36. [ ] `commonTest`: SSE parser edge cases (split chunks, empty lines, malformed JSON).
37. [ ] Repository test using Ktor `MockEngine` plus an in-memory DB.
38. [ ] One Compose UI test covering send → stream → done.
39. [ ] GitHub Actions workflow: lint + `allTests` + Android assemble on Ubuntu.
40. [ ] GitHub Actions workflow: iOS framework build and iOS tests on a macOS runner.
41. [ ] Add Kover coverage reporting and a CI badge in the README.

**Done when:** a pull request shows green checks for Android and iOS.

## Phase 6 — Polish, README and release

42. [ ] Write the README: one-line pitch, Android + iOS screenshots side by side, short streaming GIF.
43. [ ] Add an architecture diagram (Mermaid) showing modules and data flow.
44. [ ] Add a "Decisions and trade-offs" section: Room vs SQLDelight, shared UI vs SwiftUI, batching DB writes, DB-as-source-of-truth.
45. [ ] Add a "How to run" section with key setup, plus the fake-backend mode for trying it without a key.
46. [ ] Build and publish a signed release APK on GitHub Releases.
47. [ ] Add the project to CV/LinkedIn with 2–3 bullets naming concrete skills demonstrated.

**Done when:** a friend can clone it and run it without asking any questions.

---

Stretch goals (`ROADMAP.md`, "Stretch goals" section) start only after step 47 is complete — pick one or two, not all.
