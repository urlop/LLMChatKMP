# KMP LLM Chat — Roadmap

## Goal and stack

Build a Kotlin Multiplatform chat app for Android and iOS that streams LLM replies, keeps history offline, and shares everything but the platform shells. The finished repo should read as senior work: clean modules, tests, CI, and a README that explains the trade-offs.

**Definition of done:** one person can clone it, add an API key, and chat on both platforms; CI is green; README has screenshots from both.

| Layer | Choice | What it proves |
| --- | --- | --- |
| UI | Compose Multiplatform | Shared UI on Android + iOS |
| State | Shared ViewModel + StateFlow | Lifecycle-aware shared presentation |
| Networking | Ktor client + SSE streaming | Streaming, serialization, error handling |
| Persistence | Room KMP (or SQLDelight) | Offline-first data with Flow queries |
| Settings | DataStore KMP | Key-value prefs in common code |
| DI | Koin | Wiring across platforms |
| Platform APIs | expect/actual | Secure key storage, share sheet |
| Quality | commonTest, Turbine, GitHub Actions | Testing and CI on both targets |

## Phase 1 — Project setup

Outcome: an empty app that builds and runs on both platforms with the final module layout in place.

- [ ] Generate the project with the KMP wizard (Android + iOS, Compose Multiplatform shared UI)
- [ ] Split modules: `shared:domain`, `shared:data`, `shared:presentation`, `composeApp`, `iosApp`
- [ ] Set up a version catalog (`libs.versions.toml`) for every dependency
- [ ] Add Koin with one `commonModule` plus `platformModule` via expect/actual
- [ ] Add ktlint or detekt and a `.editorconfig`
- [ ] Run a "Hello chat" screen on an Android emulator and the iOS simulator
- [ ] Push to a public GitHub repo with a placeholder README

**Done when:** both apps launch and `./gradlew build` passes locally.

## Phase 2 — Domain and streaming API

Outcome: shared code can send a prompt and receive the reply token by token as a `Flow`.

- [ ] Model the domain: `Conversation`, `Message(role, content, status, createdAt)`, `MessageStatus` (Sending, Streaming, Done, Failed)
- [ ] Define `ChatRepository` interface in `domain` with `streamReply(conversationId, prompt): Flow<ChatEvent>`
- [ ] Write `SendMessageUseCase` that saves the user message, then collects the stream
- [ ] Add Ktor client (OkHttp engine on Android, Darwin on iOS) with ContentNegotiation and Logging
- [ ] Define request/response DTOs with kotlinx.serialization and mappers to domain
- [ ] Implement streaming: parse server-sent events into `ChatEvent.Delta`, `Done`, `Error`
- [ ] Map errors to domain types: no network, 401 bad key, 429 rate limit, timeout
- [ ] Support cancellation: stopping collection cancels the HTTP call
- [ ] Add a `FakeChatRepository` that streams canned text, so UI work never needs a real key

**Done when:** a unit test streams a fake SSE response through Ktor `MockEngine` and gets the right sequence of events.

## Phase 3 — Persistence and secure key storage

Outcome: conversations survive app restarts, and the API key is stored with each platform's secure storage.

- [ ] Add Room KMP (or SQLDelight): tables for conversations and messages
- [ ] Expose DAO queries as `Flow` so the UI updates as tokens arrive
- [ ] Write streamed deltas to the DB in batches (e.g. every 50 ms) rather than per token
- [ ] Make the DB the single source of truth: the network writes, the UI only reads
- [ ] On app start, mark any message stuck in `Streaming` as `Failed` with a retry option
- [ ] Define `expect class SecureStorage`; `actual` = EncryptedSharedPreferences or Keystore on Android, Keychain on iOS
- [ ] Store settings (model name, temperature, theme) in DataStore KMP
- [ ] Add a DB migration test, even for version 1 → 2 with one new column

**Done when:** kill the app mid-stream, reopen it, and history is intact with the partial reply marked failed.

## Phase 4 — Shared ViewModel and UI

Outcome: a usable chat UI on both platforms, driven by one shared ViewModel.

- [ ] Create `ChatViewModel` (androidx lifecycle KMP) exposing a single `StateFlow<ChatUiState>`
- [ ] Handle intents as a sealed interface: `Send`, `Stop`, `Retry`, `NewConversation`, `SelectConversation`
- [ ] Build screens: conversation list, chat, settings (API key, model)
- [ ] Chat screen: message bubbles, streaming cursor, auto-scroll that stops when the user scrolls up
- [ ] Render Markdown and code blocks in replies
- [ ] Show states: empty, loading, error with retry, offline banner
- [ ] Add Compose Multiplatform navigation between screens
- [ ] Dark mode, dynamic type/font scaling, content descriptions for accessibility
- [ ] Copy message and share via expect/actual (Android share intent, iOS `UIActivityViewController`)

**Done when:** you can hold a full conversation on both platforms, stop a reply mid-stream, and retry a failed one.

## Phase 5 — Testing and CI

Outcome: tests cover the shared logic, and every push is checked on both platforms.

- [ ] `commonTest`: use cases with fake repositories
- [ ] `commonTest`: `ChatViewModel` state transitions with Turbine and `runTest`
- [ ] `commonTest`: SSE parser edge cases (split chunks, empty lines, malformed JSON)
- [ ] Repository test with Ktor `MockEngine` + in-memory DB
- [ ] One Compose UI test for send → stream → done
- [ ] GitHub Actions: lint + `allTests` + Android assemble on Ubuntu
- [ ] GitHub Actions: iOS framework build and iOS tests on a macOS runner
- [ ] Add Kover coverage report and a CI badge in the README

**Done when:** a pull request shows green checks for Android and iOS.

## Phase 6 — Polish, README and release

Outcome: a recruiter or interviewer understands the project in 60 seconds and can try it.

- [ ] README: one-line pitch, Android + iOS screenshots side by side, and a short streaming GIF
- [ ] Architecture diagram (Mermaid) showing modules and data flow
- [ ] "Decisions and trade-offs" section: Room vs SQLDelight, shared UI vs SwiftUI, batching DB writes, DB as source of truth
- [ ] "How to run" with key setup, plus the fake-backend mode for trying it without a key
- [ ] Signed release APK on GitHub Releases
- [ ] Add the project to your CV and LinkedIn with 2–3 bullets naming the concrete skills

**Done when:** a friend clones it and runs it without asking you anything.

## Stretch goals

Pick one or two only after Phase 6 ships. Each adds a distinct talking point for interviews.

- [ ] Multiple providers behind one interface (Strategy pattern + Koin qualifiers)
- [ ] Image input: pick a photo and send it with the prompt (expect/actual pickers)
- [ ] Tool calling: a simple "get weather" or "calculator" tool the model can invoke
- [ ] Desktop (JVM) target to show a third platform with zero extra logic
- [ ] Full-text search across conversations
- [ ] On-device model for offline replies