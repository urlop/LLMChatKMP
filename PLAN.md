# PLAN.md — Execution plan (Phases 1–6, pre-stretch)

Derived from `ROADMAP.md`. Numbered steps are meant to be done in order; each maps to one roadmap checklist item. Stretch goals are intentionally excluded — do not start them until Phase 6 is fully done.

**Status (as of 2026-09-24):** Phases 1–4 are functionally complete and verified end-to-end on the Android emulator (send → stream → persist → retry/stop paths all wired; screenshots confirmed no crashes and correct spacing/layout after two real bugs found and fixed during manual QA). iOS/JVM targets compile cleanly (including Room/KSP codegen and Kotlin/Native klibs) but were **not runtime-verified** — this sandbox has no macOS/Xcode/simulator, so iOS framework linking and any on-device behavior there is unconfirmed. Phase 5 has partial test coverage; CI workflows and Phase 6 docs/release steps are not yet done. See the "❓ NEEDS HUMAN" notes inline for what's blocked and why.

## Phase 1 — Project setup

1. [x] Confirm the wizard-generated project (already done — Android + iOS + Compose Multiplatform shared UI, plus an extra `desktopApp` target) builds cleanly: `./gradlew build`.
2. [x] Split `shared` into `shared:domain`, `shared:data`, `shared:presentation` modules (or equivalent source-set packages if keeping one Gradle module); move existing template code out or delete it.
   **Decision:** real Gradle modules for `shared:domain` (models, repository/use-case interfaces, `FakeChatRepository`) and `shared:data` (Room, Ktor, DataStore, secure storage, connectivity). Presentation (ViewModels, Compose screens, Koin wiring, platform entry points) stays as organized packages inside the existing `shared` module rather than a fourth Gradle module, since it's also where the iOS framework and Compose resources are produced — splitting it out further would add framework-export complexity for little benefit at this project's size.
3. [x] Set up `gradle/libs.versions.toml` as the single version catalog for every dependency added from here on.
4. [x] Add Koin: one `commonModule` in shared code plus a `platformModule` per target via `expect`/`actual`.
5. [x] Add ktlint plus a root `.editorconfig`. `./gradlew ktlintCheck` passes clean across all modules (generated Compose-resource/KSP code excluded via `.editorconfig`, not detekt — ktlint was the simpler fit for this project).
6. [x] Replaced the default "Hello" screen with the full chat UI; **verified running on the Android emulator** (installed, launched, navigated screens, sent a message, watched it stream and persist — with screenshots). **❓ NEEDS HUMAN:** iOS Simulator run — this sandbox has no macOS/Xcode, so `linkDebugFrameworkIosArm64`/`linkDebugFrameworkIosSimulatorArm64` are skipped by Gradle and the app has never actually been launched on iOS. Needs a Mac to open `iosApp.xcodeproj` and run it.
7. [x] Initialize git (done locally). **❓ NEEDS HUMAN:** push to a public GitHub repo — needs your GitHub account/repo name; I only have local git access, not your GitHub credentials.

**Done when:** both apps launch and `./gradlew build` passes locally. *(True for Android + build-level iOS/JVM; iOS on-device unverified.)*

## Phase 2 — Domain and streaming API

8. [x] Model the domain: `Conversation`, `Message(role, content, status, createdAt)`, `MessageStatus` (Sending, Streaming, Done, Failed).
9. [x] Define `ChatRepository` interface in `domain` with `sendMessage`/`retryMessage`/`stopStreaming` plus `observeConversations`/`observeMessages` (DB-backed reads).
10. [x] Write `SendMessageUseCase` that saves the user message, then collects the stream.
11. [x] Add Ktor client (OkHttp engine on Android/JVM, Darwin on iOS) with ContentNegotiation and Logging.
12. [x] Define request/response DTOs with kotlinx.serialization and mappers to domain (targets an OpenAI-compatible `/chat/completions` shape — see step 16 note on why nothing calls a real provider yet).
13. [x] Implement streaming: `ChatSseParser` parses SSE into `ChatEvent.Delta`/`Done`/`Error`, tolerating chunk boundaries that split mid-line.
14. [x] Map errors to domain types: no network, 401 bad key, 429 rate limit, timeout (`ErrorMapping.kt`).
15. [x] Support cancellation: `ChatCompletionsApi.streamReply` runs inside the collector's coroutine, so cancelling collection cancels the HTTP call (standard Ktor+Flow behavior; not separately stress-tested).
16. [x] Add `FakeReplySource`/`FakeChatRepository` that stream canned text, so UI work never needs a real key — this is the app's **default Koin binding**. **❓ NEEDS HUMAN:** `RemoteReplySource` (real network path) is fully coded and unit-tested (`ChatCompletionsApiTest` with Ktor `MockEngine`) but isn't wired into the running app. Before it can be: (a) pick an LLM provider/endpoint (OpenAI, Groq, Together, a local Ollama server, etc. — anything speaking the OpenAI chat-completions shape works as-is), (b) supply a real API key, entered via the Settings screen (stored through `SecureStorage`), (c) bind `single<ReplySource> { RemoteReplySource(...) }` in `commonModule()` in place of `FakeReplySource`.

**Done when:** a unit test streams a fake SSE response through Ktor `MockEngine` and gets the right sequence of events. — **Done**, see `ChatCompletionsApiTest`.

## Phase 3 — Persistence and secure key storage

17. [x] Add Room KMP: `ConversationEntity`/`MessageEntity` tables (`shared/data/.../local/`).
18. [x] Expose DAO queries as `Flow` so the UI updates as tokens arrive.
19. [x] Batch streamed-delta writes to the DB every ~50ms (`RoomChatRepository.streamAndPersist`, via `TimeSource.Monotonic`) instead of writing per token.
20. [x] Make the DB the single source of truth: `RoomChatRepository` persists as it streams; the UI only reads via `observeMessages`/`observeConversations`.
21. [x] On app start, mark any message stuck in `Streaming`/`Sending` as `Failed` (`MessageDao.markDanglingStreamsAsFailed()`, run from `RoomChatRepository`'s init block).
22. [~] Secure key storage:
    - [x] Android: real `EncryptedSharedPreferences`/`MasterKey` (`AndroidSecureStorage`).
    - [x] JVM/desktop (bonus, not a roadmap target but the wizard scaffolded `desktopApp`): JDK `Preferences` (plaintext — noted in code as "fine for a demo, not a real secrets store").
    - [ ] **❓ NEEDS HUMAN:** iOS: shipped as `NSUserDefaults` (unencrypted), not real Keychain. Real Keychain access needs raw `Security.framework` C interop (`SecItemAdd`/`SecItemCopyMatching`) that only compiles here — there's no Mac/simulator to actually run and confirm a read-back round-trips correctly, and getting low-level CoreFoundation dictionary interop right without that feedback loop is too risky to ship unverified. Flagged clearly with a `TODO` in `IosSecureStorage.kt`.
23. [x] Store settings (model name, temperature, theme) in DataStore KMP (`AppSettings`/`SettingsRepository`/`DataStoreSettingsRepository`).
24. [x] DB migration test: bumped schema 1→2 (added a nullable `MessageEntity.model` column), wrote `MIGRATION_1_2`, and a real test (`MigrationTest`, JVM) that builds an actual v1 database file, migrates it, and asserts the data + new column survive.

**Done when:** kill the app mid-stream, reopen it, and history is intact with the partial reply marked failed. — Implemented (`markDanglingStreamsAsFailed`); the exact "kill mid-stream" scenario wasn't manually reproduced on the emulator (would need a scripted `adb shell am kill` mid-stream), but the DB-as-source-of-truth design and the dangling-stream sweep make it work by construction.

## Phase 4 — Shared ViewModel and UI

25. [x] `ChatViewModel` (androidx lifecycle KMP) exposing `StateFlow<ChatUiState>`.
26. [x] Intents as a sealed interface: `Send`, `Stop`, `Retry`, `NewConversation`, `SelectConversation` (plus `UpdateDraft`/`DismissError`).
27. [x] Screens: conversation list, chat, settings (API key, model, temperature, theme).
28. [x] Chat screen: message bubbles, streaming cursor (▍), auto-scroll that stops when the user has scrolled up.
29. [x] Render Markdown and code blocks in replies (`com.mikepenz:multiplatform-markdown-renderer-m3`).
30. [x] States: empty ("Say hello…"/"No conversations yet…"), loading (spinner), error banner with dismiss, offline banner.
31. [x] Compose Multiplatform navigation (`org.jetbrains.androidx.navigation:navigation-compose`) between conversation list / chat / settings.
32. [x] Dark mode (`isSystemInDarkTheme()` → `darkColorScheme()`/`lightColorScheme()`); Material typography scales with system font size automatically; content descriptions added on icon buttons.
33. [ ] Copy message and share via `expect`/`actual` (Android share intent, iOS `UIActivityViewController`) — **not implemented**, ran out of scope for this pass; no blocker, just not done yet.

**Done when:** a full conversation works on both platforms, a reply can be stopped mid-stream, and a failed one can be retried. — **Verified on Android** (send/stream/persist confirmed live on the emulator with screenshots; two real bugs found and fixed this way — a layout bug hiding the input bar, and missing spaces between streamed words). Stop/Retry are implemented the same way Send is but weren't separately click-tested. **iOS unverified** (no simulator available here).

## Phase 5 — Testing and CI

34. [x] `commonTest`: use cases/repositories tested against fakes (`FakeChatRepositoryTest`).
35. [x] `commonTest`: `ChatViewModel` state transitions with Turbine and `runTest` (`ChatViewModelTest`).
36. [x] `commonTest`: SSE parser edge cases (split chunks, empty lines, malformed JSON) — `ChatSseParserTest`.
37. [x] Repository test with Ktor `MockEngine` (`ChatCompletionsApiTest`) + Room migration test with a real in-memory-file DB (`MigrationTest`) + full `RoomChatRepository` integration test against a real file-backed DB and `FakeReplySource` together (`RoomChatRepositoryTest`: send-to-Done persistence, retry-from-Failed, stop-mid-stream). Writing the stop-mid-stream case surfaced and fixed a real race in `stopStreaming` (it cancelled the streaming job without joining it, so a late in-flight write could clobber the Done/Failed repair back to Streaming) — `stopStreaming` now `cancelAndJoin()`s before reading and repairing status.
38. [ ] One Compose UI test for send → stream → done — not written yet.
39. [ ] GitHub Actions: lint + `allTests` + Android assemble on Ubuntu — not written yet.
40. [ ] GitHub Actions: iOS framework build and iOS tests on a macOS runner — not written yet.
41. [ ] Kover coverage report + CI badge — not started.

**Done when:** a pull request shows green checks for Android and iOS. — **❓ NEEDS HUMAN:** requires step 7 (a GitHub remote) before any CI can run at all.

## Phase 6 — Polish, README and release

42. [ ] README: pitch, Android + iOS screenshots side by side, streaming GIF — not started (have Android screenshots from manual QA to build from; no iOS screenshots possible here).
43. [ ] Architecture diagram (Mermaid) — not started.
44. [ ] "Decisions and trade-offs" section — not started (this PLAN.md's inline notes are the raw material for it).
45. [ ] "How to run" + fake-backend mode doc — not started (the fake-backend mode itself is done and is the default; just needs documenting).
46. [ ] **❓ NEEDS HUMAN:** signed release APK on GitHub Releases — needs a signing keystore (generate + password, a decision only you should make) and a GitHub Releases target (depends on step 7).
47. [ ] **❓ NEEDS HUMAN:** add to CV/LinkedIn — inherently something only you can do.

**Done when:** a friend can clone it and run it without asking any questions.

---

Stretch goals (`ROADMAP.md`, "Stretch goals" section) start only after step 47 is complete — pick one or two, not all.
