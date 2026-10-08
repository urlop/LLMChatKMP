# LatAm Economy Analyst — Execution Plan

**Goal:** a bilingual (EN/ES) assistant for Latin American economies, built from the existing KMP chat app plus a Python/FastAPI backend, with World Bank tool calls, Wikipedia RAG, and measured eval results.

**Spec:** [docs/project-spec.md](docs/project-spec.md) (renamed from `docs/AI Engineering Portfolio Project - latam economy analyst - Job Copilot.md`, DL-29).

**Status:** Current milestone: 1 · Last updated: 2026-10-02

## How to use this plan

- Work one task at a time. Each task fits in one session (≤ 2 h). Tick the box when its acceptance command passes.
- After each session, add an entry to the Session log at the bottom. After a skipped week, read the last entry and the Status line, then continue.
- Milestones 1–5 are the showable version. If time runs out, stop at 5 and polish.
- The spec's starter code is untested. Wherever a task uses it, there is a verification task next to it.
- Paths: Python commands run from `backend/` unless stated. Gradle commands run from the repo root. Shell examples are PowerShell (Windows 11).

## Glossary for an Android developer

Plain-language meanings of the backend and AI words used in this plan. Android/Kotlin equivalents are approximate.

| Word | What it means here |
| --- | --- |
| Backend | A small server program that the app calls over the internet. It holds the secret key and does the AI work. |
| Python 3.12 | The language of the backend. Version 3.12 is the one the spec fixes. |
| FastAPI | A Python library for building the server and its endpoints. Closest Kotlin relative: Ktor server. |
| `uvicorn` | The program that actually runs the FastAPI server (like pressing Run). `--reload` restarts it when you save a file. |
| Endpoint / `/chat` | One URL on the server. The app sends a request to `/chat` and reads the reply. |
| SSE (streaming) | The server sends the reply in small pieces while it is being written, so the app can show it word by word. Your app already reads this. |
| `pyproject.toml` | Python's project file with the list of libraries. Like `build.gradle.kts` plus `libs.versions.toml`. |
| Virtualenv (`.venv`) | A private folder of libraries for this project only, so it does not mix with other Python projects. |
| `pip install` | Downloads the libraries from the list. Like Gradle sync. |
| `pytest` | The test runner. Like JUnit; a test is a function whose name starts with `test_`. |
| Fixture / mock | Saved sample data, or a fake object, used so a test does not call the real internet. Like `MockEngine` in your Ktor tests. |
| Environment variable | A setting kept outside the code, e.g. `LLM_MODEL`. In PowerShell: `$env:LLM_MODEL = "..."` (lasts for that terminal window). |
| `curl` | A command-line way to send a request to the server and see the raw reply. Good for checking the backend without the app. |
| LLM | The language model (here run by Groq). |
| `LLMClient` | A small interface in front of Groq, so the rest of the code does not care which model or company is behind it. Like a repository interface. |
| Tool calling | The model can ask our code to run a function (for example "get Peru's inflation for 2024") and then use the result in its answer. |
| Agent loop | The loop: ask the model, run the tool it asks for, give it the result, repeat. Maximum 5 rounds. |
| World Bank API | A free web service that returns economic numbers. `null` in its data means "no number for that year". |
| Cache (SQLite) | A small local database file where we keep the World Bank answers, so repeated requests do not hit the internet and tests give the same numbers every time. SQLite is the same engine Room uses on Android. |
| RAG | Retrieval-Augmented Generation: before answering, find relevant text passages and give them to the model, so it answers from them and can cite them. |
| Embedding | Turning a piece of text into a list of numbers so that texts with similar meaning are close to each other. |
| Chroma | A local database that stores those number lists and finds the closest passages to a question. |
| Chunk | A piece of an article (a section or part of one) stored as one searchable unit. |
| Eval | An automatic test of answer quality: ask the app a fixed question and check the answer. |
| Golden set / `golden.jsonl` | The list of 30 test questions with the correct answers. JSONL means one JSON object per line. |
| Recall@5 | For a question, did the right passage appear among the top 5 found? |
| LLM-as-judge / faithfulness | A second model call that checks whether every claim in an answer is backed by the data or passages. |
| Langfuse trace | A web page showing everything that happened for one answer: prompts, tool calls, time, cost. |
| ISO3 code | The 3-letter country code: PER, CHL, COL... |
| CI / GitHub Actions | The automatic checks that run on GitHub after you push. |

## Definition of done

Showable version (after milestone 5):
- [ ] The KMP app chats through the FastAPI backend, and the app has no provider, model or API-key settings: it only talks to the backend.
- [ ] The agent answers number questions from World Bank data (cached in SQLite), with the year and indicator cited, and says "no data" when a value is null.
- [ ] RAG over EN + ES Wikipedia "Economy of X" articles for 8 countries answers with citations shown as source chips; combined questions call both tools (max 5 steps).
- [ ] A 30-case golden set runs in GitHub Actions against the cache, shows a pass rate, and fails when a prompt is broken.

Full version (after milestone 8):
- [ ] The README has a results table (number accuracy, tool choice/args, no-data honesty, language match, recall@5, faithfulness, p50 latency, $ per answer) and a 2 chunk sizes × 2 models experiment with the chosen settings explained.
- [ ] One Langfuse trace can be walked through for a full answer; the README has the architecture diagram, data licenses (CC BY 4.0 World Bank, CC BY-SA 4.0 Wikipedia) and a demo GIF; someone else can run it in under 10 minutes.

Optional, iOS verification (Definition of done B; do it whenever you have a Mac, it does not block the other two lists; DL-28):
- [ ] The app builds and launches in the iOS simulator from `iosApp/iosApp.xcodeproj` (`./gradlew` iOS framework tasks and Xcode both succeed).
- [ ] The app reaches the backend: either the backend runs on the Mac (`localhost`), or `url=` in `backend.local.properties` points to the LAN address of the machine that runs it (the plain-HTTP setting in `Info.plist` allows it).
- [ ] A chat message streams a reply word by word, and the token from `backend.local.properties` is accepted (no 401).
- [ ] A question with tools shows the tool chips, and a question with sources shows source chips; tapping a chip opens the Wikipedia link.
- [ ] The backend-unreachable case (backend stopped) shows the "Check the connection to the backend" message (DL-20).
- [ ] Conversations survive an app restart (Room persistence, including the sources column from 3.9).
- [ ] Write the result in the session log and the README iOS note: change "compiles, not run" to "verified on simulator, <date>", or list what failed. Real-device and Keychain checks stay out of scope (the `NSUserDefaults` storage question from `docs/PLAN_LLM.md`; `SecureStorage` is expected to be removed in 1.7).

---

## Milestone 1 — Backend skeleton

**Goal:** the KMP app chats through a FastAPI `/chat` endpoint that proxies Groq and streams SSE.
In plain words: build a small server that sits between the app and Groq. The app talks to the server; the server holds the key and forwards the question to Groq.
**Estimated hours:** 4–6 (spec range) · Sum of task estimates below: 13.5 h (see Q17)

### Tasks
- [ ] 1.1 Run the Day-1 checklist items D1–D3 and D9 (Python, Groq models, tool calling/usage/limits, country codes). Write the results in the Day-1 section of this file. (~1.5 h)
- [ ] 1.2 Create `backend/pyproject.toml`, `backend/app/__init__.py`, `backend/app/main.py` with `GET /health`, and a virtualenv in `backend/.venv`. Add `.venv/`, `backend/data/`, `.env`, `backend.local.properties` and the generated config directory (see 1.6) to `.gitignore`. Dependencies (names from the spec): fastapi, uvicorn, httpx, openai, pytest. Add the others when a milestone needs them. (~1 h)
- [ ] 1.3 Write `backend/app/llm.py`: an `LLMClient` interface (a `typing.Protocol` or ABC) with a streaming chat method, and a `GroqLLMClient` using the `openai` SDK with `base_url` Groq's OpenAI-compatible URL. Config loading (in `backend/app/config.py`, new): the Groq key comes from the `provider="groq"` entry of the repo-root `api-keys.local.xml` (gitignored) and can be overridden by `GROQ_API_KEY` (for CI and deploys, which have no such file); the temperature comes from a `temperature` attribute on that entry (value `0.7`; you add the attribute yourself, I do not edit the file), defaulting to 0.7 if absent; the access token (DL-18) comes from `backend.local.properties` (`token=`, override `BACKEND_TOKEN`); `LLM_MODEL` comes from the environment. No model name in code, and the `model` attribute in the XML is not used (Q2b). `run_agent` takes temperature as a parameter, so evals can pass their own value (Q16). Read the XML with the standard library (`xml.etree.ElementTree`); no `.env` loader library (DL-19). If the key is missing from both the XML and `GROQ_API_KEY`, fail at startup with a message that says which file and which entry to fill in. Add `backend/.env.example` as a plain list of the optional environment variables with placeholder values (`LLM_MODEL`, later the Langfuse keys); it is documentation only, nothing loads it. (~1.5 h)
- [ ] 1.4 Add `POST /chat` to `backend/app/main.py`: accept an OpenAI-style body (`messages`, `stream`; tolerate and ignore `model` and `temperature`; the backend uses its configured temperature), stream `data: {chunk json}\n\n` lines and a final `data: [DONE]\n\n`. Map upstream errors (401, 429, timeout) to HTTP statuses the app's `ErrorMapping.kt` already understands. Check the access token on `/chat` (DL-18): 401 if the `Authorization: Bearer` header is missing or wrong; `/health` stays open. At this milestone the backend sends only plain text chunks; the extra `x_analyst` chunks are added in 3.7 and 4.6 (DL-15). (~1.5 h)
- [ ] 1.5 Write `backend/tests/test_chat_sse.py` with a fake `LLMClient`: chunk format, `[DONE]` last, upstream error path, 401 without or with a wrong token, success with the right token, `/health` without token. (~1 h)
- [ ] 1.6 Add the build-time config (DL-17): create `backend.local.properties.example` (committed) and a Gradle task in `shared/build.gradle.kts` that reads `backend.local.properties` (if present) and generates `BackendBuildConfig.kt` (nullable `url` and `token`) into a `build/generated/...` directory added to the `commonMain` source set; make sure the existing KSP (Room) and ktlint tasks still pass (`.editorconfig` already excludes some generated code; check the new path). Time-box 2 h; fallback is described in DL-17. Verify: `./gradlew :androidApp:assembleDebug` passes with the file present and with it absent. (~2 h)
- [ ] 1.6b Change the KMP wiring: remove `GROQ_BASE_URL` and the per-request `ChatApiConfig` (key, model, temperature read from settings) in `shared/src/commonMain/kotlin/com/ruby/myllmchatkmp/di/CommonModule.kt`; build the config from `BackendBuildConfig.url` or the per-platform default supplied by each `platformModule()` (`shared/src/androidMain/.../di/PlatformModule.android.kt` and the iOS and desktop equivalents); delete `shared/data/src/commonMain/kotlin/com/ruby/myllmchatkmp/data/repository/ConfiguredReplySource.kt` and bind `RemoteReplySource` directly; keep `FakeReplySource` for tests only. In `shared/data/.../network/ChatCompletionsApi.kt`, keep the `Authorization: Bearer` header but fill it with `BackendBuildConfig.token` (skip the header if null), and stop sending `model` and `temperature` in the request. (~1.5 h)
- [ ] 1.7 Remove the API key, model and temperature controls from `shared/src/commonMain/.../presentation/settings/SettingsScreen.kt` and `SettingsViewModel.kt` (theme stays), and drop `model` and `temperature` (and the `gpt-4o-mini` default) from `shared/data/.../settings/AppSettings.kt` and its DataStore repository. Then check whether `SecureStorage` (`shared/data/.../storage/` and its Android, iOS and JVM implementations) has any user left; remove it if not; DL-18 means the token is not user-stored, so the expected result is that it can be removed. Keep `ChatApiConfig` and `ChatCompletionsApi` generic (DL-20). Update `ChatError.NoNetwork` and `Timeout` display text in `ChatUiState.kt` (`toDisplayMessage`) as DL-20 says, and update the `ChatUiState` tests. (~1.5 h)
- [ ] 1.8 Allow the dev backend on Android: `androidApp/src/main/AndroidManifest.xml` has no cleartext setting and the emulator reaches the host through a special address (verify, see Gotchas). Check `iosApp/iosApp/Info.plist` for the equivalent. Run the app on the emulator. (~1 h)
- [ ] 1.9 Update `shared/data/src/commonTest/.../ChatCompletionsApiTest.kt` and any ViewModel test touched. Run `./gradlew ktlintCheck allTests`. (~1 h)
- [ ] 1.10 Create `backend/README.md` (stub): what the backend does, Python 3.12 setup, `.env` variables, how to create `backend.local.properties` from the `.example` file, run and test commands from this milestone. Extend it in each later milestone when a new command or variable appears. (~0.5 h)
- [ ] 1.11 Rename the GitHub repo to `latam-economy-analyst` (DL-29). Then `git remote set-url origin <new url>`, and update the old name `urlop/LLMChatKMP` in the root `README.md` badge URLs (CI, iOS, coverage endpoint). Check with `git grep -n "LLMChatKMP"`. `rootProject.name` in `settings.gradle.kts` and the local folder name can stay. Also rename the branch (DL-30): `git branch -m master main`, `git push -u origin main`, set `main` as the default branch in the GitHub settings, then delete the old remote `master` (`git push origin --delete master`) only after you see `main` on GitHub. Check that the `push` triggers of `ci.yml` and `ios.yml` run on the first push to `main`. (~0.75 h)
- [ ] 1.12 Update the docs that describe the app as a generic multi-provider client, after 1.6 and 1.7 land so they match the code. `README.md`: intro (the "any OpenAI-compatible endpoint" and "add a key in Settings" sentences), the live-Groq demo caption, the Data-flow/design notes about `ConfiguredReplySource` and `FakeReplySource` as the no-key fallback, the "Talking to a real model (Groq)" section (replace with a "Required local configuration" setup section, see DL-19: it must say that the app and the backend do not work until you create two git-ignored local files, `api-keys.local.xml` with the Groq key and `backend.local.properties` with the token and URL, and show the exact format of each, then "run the backend, point the app at it"), the architecture diagram node `RemoteReplySource (OpenAI-compatible /chat/completions)`, and the `SecureStorage`/provider line in the module list. `docs/ROADMAP.md`: the definition of done ("add an API key"), the secure-key outcome, the settings checklist item ("model name, temperature"), the screens item ("API key, model"), and the stretch item "Multiple providers behind one interface". Mark changed items as superseded instead of deleting them, so the history stays readable. Leave `docs/PLAN_LLM.md` as history. Check with `git grep -n -i -E "provider|api key|any OpenAI-compatible" -- README.md docs/ROADMAP.md`. (~1 h)

### Acceptance criteria
- App chats through the backend; the Groq key is not in the app.
  - Backend running: `uvicorn app.main:app --reload` and `curl http://localhost:8000/health` returns HTTP 200.
  - `curl -N -X POST http://localhost:8000/chat -H "Content-Type: application/json" -d "{\"messages\":[{\"role\":\"user\",\"content\":\"Say hi\"}],\"stream\":true}"` prints several lines starting with `data: {` whose `choices[0].delta.content` is text, and the last line is `data: [DONE]`.
  - In the emulator, a message streams token by token into the bubble, and the Settings screen has no API key, model or temperature controls.
  - `git grep -n -i -E "api.groq.com|apiKey|getApiKey" -- shared androidApp iosApp desktopApp` returns nothing (the token is read through `BackendBuildConfig`, not through a stored `apiKey`).
  - With a token set in `backend.local.properties`, `curl` to `/chat` without the header returns HTTP 401, and with `-H "Authorization: Bearer <token>"` it streams; `curl http://localhost:8000/health` still returns 200 without a token.
  - `./gradlew :androidApp:assembleDebug` also passes with `backend.local.properties` absent (as on CI).
  - `./gradlew ktlintCheck allTests` passes.

### Tests to write
- `backend/tests/test_chat_sse.py` (format, `[DONE]`, error mapping, ignores client `model`).
- Updated `ChatCompletionsApiTest` and settings tests for the removed fields.

### Commands
```powershell
cd backend
py -3.12 -m venv .venv
.venv\Scripts\Activate.ps1
pip install -e ".[dev]"        # exact extras name is whatever you put in pyproject.toml
uvicorn app.main:app --reload
pytest
# repo root
./gradlew ktlintCheck allTests
./gradlew :androidApp:assembleDebug
```

### Risks / gotchas
- The SSE format must match what `ChatSseParser.kt` reads: `data:` lines, JSON with `choices[0].delta.content`, `data: [DONE]`. It ignores non-`data:` lines and uses `ignoreUnknownKeys = true`. The extra `x_analyst` chunks (DL-15) come later, in milestones 3 and 4.
- Android emulator cannot reach the host's `localhost`; the usual alias is `10.0.2.2` (verify). Cleartext HTTP may be blocked by default on Android and iOS (verify); the manifest currently has no setting.
- iOS cannot be run without a Mac (only occasional access, per `docs/PLAN_LLM.md`); do not claim it works until the optional iOS checklist in the Definition of done is done (DL-28).
- The Groq key lives only on the backend. `api-keys.local.xml` (repo root, gitignored) is a local scratch file with `<key provider=... model=... baseUrl=...>` entries for groq and openai; no code reads it yet, and its header says the app does not read it. Only the backend config loader (task 1.3) may read it. Never commit it, print it, or paste values into logs, traces or this file; confirm `git check-ignore api-keys.local.xml` prints the path.
- The XML's `model` value is a local note, not config; model names come from Day-1 item D2.
- `backend.local.properties` holds the token: it must be git-ignored before the first commit (`git check-ignore backend.local.properties` prints the path). Never paste the token into logs, this file or screenshots.
- The token is built into the app, so it can be extracted from an installed copy (DL-18). Do not describe it as strong security in the README.
- The generated Kotlin file lives under `build/`; do not edit it by hand, and do not commit it.
- Removing settings fields changes the DataStore content on existing installs; check that an old settings file still loads (task 1.9).

---

## Milestone 2 — Data tools

**Goal:** `get_indicator` and `get_country_info` over the World Bank API with a SQLite cache, tested without the LLM.
In plain words: two Python functions that fetch economic numbers from the World Bank and save them locally, so later steps and tests can reuse them.
**Estimated hours:** 3–5 (spec range) · Sum of task estimates below: 9 h (see Q17)

### Tasks
- [ ] 2.1 Run Day-1 items D4, D5 and D8 (indicator coverage, error shapes, pagination). Save 4–5 real responses as fixtures in `backend/tests/fixtures/`. (~1.5 h)
- [ ] 2.2 Write `cached_get(url, params)` in `backend/app/worldbank.py` (this function is missing from the spec's sketch): SQLite table in `backend/data/cache.sqlite`, key = full request URL including query string, stores response JSON and fetch time. Add an offline mode (environment variable, name your choice) that raises on a cache miss instead of calling the network. (~1.5 h)
- [ ] 2.3 Write `get_indicator(countries, indicator, start_year, end_year)` (DL-23) in `backend/app/worldbank.py`, starting from the spec's sketch but verifying each line: one indicator per request, `value` may be `None`, error shape `[{"message": [...]}]`, pagination per D5. `indicator` accepts a friendly name (`inflation`, `gdp`, ...) or a raw World Bank code; unknown values return an error result that lists the valid names, not an exception. A single year is `start_year == end_year`. Add the exports indicator `NE.EXP.GNFS.ZS` to `INDICATORS` with the friendly name `exports` (the sketch lacks it). Keep `GC.DOD.TOTL.GD.ZS` out. (~1.5 h)
- [ ] 2.4 Write `get_country_info(code)` in `backend/app/worldbank.py` using `country/<code>?format=json`; unknown code returns a clear error row. (~1 h)
- [ ] 2.5 Write `backend/tests/test_worldbank.py` with `httpx.MockTransport` and the fixtures: normal rows, null value, error message, unknown country, unknown indicator name, second call served from cache (count calls on the transport), offline miss raises. (~1.5 h)
- [ ] 2.6 Add a cache warm-up entry point in `backend/app/worldbank.py` (`python -m app.worldbank warm`) that fetches every indicator for the 8 countries over a fixed year range, so evals can run offline. Fix the year range and write it in the Decision log. (~1 h)
- [ ] 2.7 Create `backend/app/tools.py` with Python wrappers and JSON schemas for the two data tools (schemas follow the spec's `get_indicator(countries, indicator, years)` / `get_country_info(code)`; see Q5 for the argument convention). (~1 h)

### Acceptance criteria
- `get_indicator(["PER","CHL"], "inflation", 2021, 2025)` returns clean rows, and a second call reads from the cache.
  - `python -c "from app.worldbank import get_indicator; r=get_indicator(['PER','CHL'],'inflation',2021,2025); print(len(r)); print(r[0])"` prints `10` and a dict with `country`, `name`, `year`, `value` (the spec says inflation is complete for these two countries over 2021–2025).
  - Run it again with offline mode on: same output, no network. Turn the network off, or use the offline flag, to prove it.
  - `get_indicator(["VEN"], "inflation", 2021, 2025)` returns rows with `value: None` or a "no data" row, not an exception.
  - Unknown code, e.g. `get_country_info("XXX")`, returns an error row, not an exception.
  - `pytest backend/tests/test_worldbank.py` passes.

### Tests to write
- `backend/tests/test_worldbank.py` (cases listed in 2.5).

### Commands
```powershell
pytest tests/test_worldbank.py
python -m app.worldbank warm
python -c "from app.worldbank import get_country_info; print(get_country_info('PER'))"
```

### Risks / gotchas
- Multi-indicator requests fail ("The provided parameter value is not valid"). One indicator per request; multiple countries joined with `;` worked for 3 countries in testing.
- `value` can be `null`; years can be missing. Return explicit nulls; never fill in numbers.
- Pagination: the spec says to handle it, and the sketch only sets `per_page=500`. Check the metadata fields in D5 before deciding.
- Error responses have no row list (`data[1]` is missing or `None`). The sketch handles `len(data) < 2 or data[1] is None`; confirm it covers real errors.
- Sketch gaps: `cached_get` is undefined; the signature `(countries, indicator, start, end)` differs from the spec's tool description `years`.
- The cache key must include query params, or different date ranges collide.

---

## Milestone 3 — RAG over Wikipedia

**Goal:** answers cite Wikipedia passages, shown as source chips in the app.
In plain words: download the Wikipedia economy articles, cut them into pieces, make them searchable by meaning, and show the passages used as tappable chips under the answer.
**Estimated hours:** 5–7 (spec range) · Sum of task estimates below: 15 h (see Q17)

### Tasks
- [ ] 3.1 Run Day-1 items D6 and D7 (embedding model choice; the 16 article titles resolve and how sections appear in the extract). (~1.5 h)
- [ ] 3.2 Write `backend/app/ingest.py` step 1: fetch plain text for the 16 articles through the MediaWiki API (`action=query&prop=extracts&explaintext=1`) and save raw text to `backend/data/wiki/` (not in git) so ingestion is reproducible. (~1.5 h)
- [ ] 3.3 Write `backend/app/ingest.py` step 2: split by section, then into chunks; chunk size and overlap are parameters (milestone 6 compares two sizes). Metadata: `country`, `lang`, `section`, `url`, and the article title so `relevant_sections` like `Economy of Chile#Sectors` can be matched. (~1.5 h)
- [ ] 3.4 Write `backend/app/ingest.py` step 3: embed with the model chosen in D6 and store in Chroma persisted at `backend/data/chroma/`. Make the collection name include the chunk size so two configs can coexist. (~1.5 h)
- [ ] 3.5 Write `backend/app/retrieval.py` `search_docs(query, k)` returning text, metadata and url; optional `country` and `lang` filters only if tests show they help. (~1 h)
- [ ] 3.6 Write `backend/tests/test_retrieval.py`: ingest a tiny two-article fixture into a temporary Chroma directory; check metadata, ES query finds EN passage, k respected. (~1.5 h)
- [ ] 3.7 Add a temporary retrieve-then-answer path to `POST /chat` in `backend/app/main.py` (retrieve top-k, put passages in the prompt, and send one `x_analyst` `sources` chunk before the text, as defined in DL-15) and the first `backend/app/prompts.py` with a version constant. Add a test to `backend/tests/test_chat_sse.py` that checks the `sources` chunk shape (`choices` is `[]`, `x_analyst.sources` is a list). This stands in for the agent loop until milestone 4 (see Q3). (~1.5 h)
- [ ] 3.8 KMP model: add `sources` to `shared/domain/src/commonMain/.../model/Message.kt` and a `ChatEvent.Sources` variant in `ChatEvent.kt`; add the optional `x_analyst` field to `ChatCompletionChunkDto` in `shared/data/.../network/ChatCompletionsDto.kt` (`@SerialName("x_analyst")`, default `null`) and make `ChatSseParser.kt` emit `ChatEvent.Sources` for it (DL-15); map in `shared/data/.../repository/Mappers.kt`. Add a case to `ChatSseParserTest`: a `sources` line gives `Sources`, a normal text line still gives `Delta`. (~1.5 h)
- [ ] 3.9 KMP persistence: add the column in `shared/data/.../local/Entities.kt`, write `MIGRATION_2_3`, export schema `3.json`, extend `shared/data/src/jvmTest/.../local/MigrationTest.kt`. Update `RoomChatRepository.kt` to persist sources. (~2 h)
- [ ] 3.10 KMP UI: render source chips with links in `MessageBubble` in `shared/src/commonMain/.../presentation/chat/ChatScreen.kt`. Check that the link opens on Android and desktop (iOS unverified, DL-28). (~1.5 h)

### Acceptance criteria
- "What drives Chile's economy?" answers with citations (the "no data" check for "What is Peru's GDP in 2030?" moved to Milestone 4, DL-21).
  - After `python -m app.ingest`, the Chroma collection holds chunks for all 16 articles: a count query per `country`/`lang` shows 8 × 2 non-empty groups.
  - `python -c "from app.retrieval import search_docs; [print(r['metadata']) for r in search_docs('What drives Chile\'s economy?', 5)]"` prints 5 results, each with `country`, `lang`, `section`, `url`.
  - A Spanish query (`¿Qué impulsa la economía de Chile?`) returns at least one `CHL` result.
  - In the app, the Chile answer shows at least one source chip that opens a Wikipedia URL.
  - `pytest` and `./gradlew ktlintCheck allTests` pass.

### Tests to write
- `backend/tests/test_retrieval.py`; section splitting unit test in `backend/tests/test_ingest.py`.
- `MigrationTest` for 2→3; a `ChatSseParserTest` case for the sources payload; a `ChatViewModelTest` case showing sources are exposed.

### Commands
```powershell
python -m app.ingest
pytest tests/test_retrieval.py tests/test_ingest.py
./gradlew :shared:data:jvmTest      # run from repo root
```

### Risks / gotchas
- The first run downloads the embedding model; check size and time before a work session.
- Spanish article titles differ from the English pattern; D7 must confirm all 16.
- Wikipedia text changes over time. Keep raw text under `backend/data/wiki/` so eval recall is stable.
- If the embedding model needs query/passage prefixes (check the model card), forgetting them lowers recall silently.
- Chunk size is a milestone-6 variable; do not hardcode it.
- Room needs a real migration (schema v2 → v3) or existing installs crash; the old `MigrationTest` shows the pattern.
- `ChatSseParser` currently drops everything except `delta.content`; the new chunks follow DL-15. A chunk with `choices: []` must not crash the parser (today it produces no event); the new test covers it.

---

## Milestone 4 — Agent loop

**Goal:** the model chooses between data tools and `search_docs`, up to 5 steps; the app shows which tools ran.
In plain words: let the model decide whether it needs numbers, passages or both, run what it asks for, and show in the app which tools were used.
**Estimated hours:** 4–6 (spec range) · Sum of task estimates below: 11.5 h (see Q17)

### Tasks
- [ ] 4.1 Verify the spec's `run_agent` sketch before reusing it: it builds its own `OpenAI` client (must go through `LLMClient`), `GROQ_API_KEY` and `EVAL_MODEL` are undefined, `messages.append(msg)` appends an SDK object, `json.loads` and tool calls have no error handling. Write the list of fixes in the session log. (~1 h)
- [ ] 4.2 Add `search_docs` schema and wrapper to `backend/app/tools.py`; add a `tool_calls` capable method to `LLMClient` in `backend/app/llm.py`. (~1.5 h)
- [ ] 4.3 Write `backend/app/agent.py`: hand-written loop, `max_steps=5`, temperature passed in as a parameter (Q16), returns `{"answer", "tools_called", "sources"}`. Every model call in the loop is non-streaming (DL-27). Tool errors (unknown tool, bad JSON args, exceptions) go back to the model as tool results. (~2 h)
- [ ] 4.4 Write the agent system prompt in `backend/app/prompts.py` (versioned): when to use each tool, "never guess a number", answer in the question's language, country-name to ISO3 hints, no-data wording. (~1 h)
- [ ] 4.5 Write `backend/tests/test_agent.py` with a scripted fake `LLMClient`: tool then answer, two tools, max-steps reached, bad arguments, unknown tool, tool exception. (~1.5 h)
- [ ] 4.6 Wire the loop into `POST /chat` in `backend/app/main.py`, replacing the temporary path from 3.7; stream the final answer and send `x_analyst` `tool_call` chunks (`running` when a tool starts, `done` when it ends) and the `sources` chunk as defined in DL-15; order and chunking follow DL-27: the `tool_call` chunks and the `sources` chunk come first, then the final answer text in small chunks. Extend `backend/tests/test_chat_sse.py` with a `tool_call` chunk case. (~1.5 h)
- [ ] 4.7 KMP: add `toolsCalled` to `Message.kt`, add `ChatEvent.ToolCall(name, status)` in `ChatEvent.kt`, extend `ChatCompletionsDto.kt` and `ChatSseParser.kt` for the `tool_call` chunk (DL-15), and update `Mappers.kt` and `Entities.kt`; write `MIGRATION_3_4` and test it in `MigrationTest.kt`. (~2 h)
- [ ] 4.8 KMP UI: show the tools that ran in `MessageBubble` (`ChatScreen.kt`), e.g. a small row of chips. (~1 h)

### Acceptance criteria
- "Compare inflation in Peru and Colombia in 2023 and explain the difference" calls `get_indicator` and `search_docs`, and the app shows which tools ran.
- "What is Peru's GDP in 2030?" answers that no data exists and gives no GDP figure (moved here from Milestone 3, DL-21).
  - Run through the agent, the question returns a "no data" answer with no number in it; add it as a unit test with a scripted fake model, and check it once with the real model.
  - `python -c "from app.agent import run_agent; import os; r=run_agent('Compare inflation in Peru and Colombia in 2023 and explain the difference', model=os.environ['LLM_MODEL']); print([t['name'] for t in r['tools_called']])"` includes both `get_indicator` and `search_docs`.
  - A scripted fake model that always requests a tool stops after exactly 5 steps and returns the fallback message (unit test).
  - In the app, the answer shows tool chips for both tools.
  - `pytest` and `./gradlew ktlintCheck allTests` pass.

### Tests to write
- `backend/tests/test_agent.py` (cases in 4.5).
- `MigrationTest` for 3→4; parser test for the tools payload.

### Commands
```powershell
pytest tests/test_agent.py
uvicorn app.main:app --reload
```

### Risks / gotchas
- The sketch loop is non-streaming; `/chat` must stream (Q9).
- Models may emit parallel tool calls, wrong ISO codes, or years as strings. Validate arguments in `tools.py`.
- Tool-calling support differs per model (D3); a model without it breaks the loop.
- Keep `max_steps` as a parameter; evals need the same loop as the app.
- Use offline cache mode in tests so the loop never calls the live World Bank API.

---

## Milestone 5 — Evals v1, code checks

**Goal:** a 30-case golden set with cache-derived expected values, pytest code checks, and CI that shows a pass rate and fails on a broken prompt.
In plain words: 30 fixed questions with known correct answers, automatic checks, and GitHub running them on every push, so a bad change turns the build red.
**Estimated hours:** 4–6 (spec range) · Sum of task estimates below: 11 h (see Q17)

### Tasks
- [ ] 5.1 Write the 30 case inputs (EN and ES) following the Golden set plan below, in a draft file `backend/evals/golden_inputs.jsonl` (inputs and types only, no numbers). (~1.5 h)
- [ ] 5.2 Write `backend/evals/build_golden.py`: read each case, query the cache (offline mode) for the expected value(s), write `backend/evals/golden.jsonl`. Fail loudly if a value is missing from the cache. Never type values by hand. Write each case's expected tool arguments in the DL-23 form (`start_year`, `end_year`, friendly name or code), not `years: [..]`; rewrite the spec's golden examples that use `years` when copying them. (~1.5 h)
- [ ] 5.3 Write `backend/evals/test_fast.py` for number accuracy, tool choice, tool arguments, no-data honesty, language match. Start from the spec's sketch; note the sketch relies on undefined `EVAL_MODEL` and `run_agent` hitting the live LLM. Decide Q16 (temperature for evals) first. Gate style is DL-24: each metric is computed per case and the test asserts the pass rate against its target, not each case; a failed case is retried once before it counts. Write the per-metric results to a JSON file that 5.6 reads for the job summary. (~2 h)
- [ ] 5.4 Replace the spec's `numbers_in` parser with a tested one in `backend/evals/numbers.py`; add `backend/evals/test_numbers.py` (negative values, decimal comma, thousands separators, years, percent signs, Unicode minus). (~1.5 h)
- [ ] 5.5 Add a language detector for the language-match check (`lingua-language-detector` restricted to English and Spanish, DL-25; add it to the eval dependencies in `backend/pyproject.toml`) and a unit test with 6 EN and 6 ES strings, including at least 2 short, number-heavy answers per language (for example "Peru's inflation in 2023 was 6.5%."). If `lingua` fails those, switch to a stopword-count check and record it in the session log. (~1 h)
- [ ] 5.6 Add `.github/workflows/evals.yml`: Python 3.12, install, run `pytest evals/test_fast.py` in offline cache mode, print pass rate per metric in the job summary, fail when any metric is below its Day-1 target, or when a case in the small `must_pass` group fails twice (DL-24). Add the `GROQ_API_KEY` repository secret; CI gets the cache from the committed snapshot and builds Chroma itself (DL-22): add a step that restores the pip and Hugging Face model caches (`actions/cache`), then runs `python -m app.ingest` for Chroma. Add a script `backend/scripts/make_cache_snapshot.py` that copies only the rows the golden cases need into `backend/tests/fixtures/cache_snapshot.sqlite`, and a check that fails if a golden case needs a row that is not in the snapshot. Give it a `paths:` filter (`backend/**`, `.github/workflows/evals.yml`) so Kotlin-only changes do not trigger it. Per DL-35, the push-triggered run uses the ~10-case subset (`must_pass` plus about 7 more, marked in the golden set) with thresholds applied to it; add a `workflow_dispatch` input that runs all 30. The LLM client must back off on 429 using `retry-after`. (~2 h)
- [ ] 5.6b Add `paths-ignore: ['backend/**']` (plus `docs/**` if you want) to the `push` and `pull_request` triggers in `.github/workflows/ci.yml` and `.github/workflows/ios.yml`, so backend-only commits do not run Gradle. Verify with one backend-only commit (no Gradle run) and one Kotlin-only commit (no evals run). If either workflow is a required status check, check how skipped runs are treated before merging. (~0.5 h)
- [ ] 5.7 Break the prompt on purpose (edit `backend/app/prompts.py` on a throwaway branch), push, confirm CI fails, revert. Write the example in the session log; it is an interview talking point. Check that the broken prompt fails by a wide margin (not just one point under a threshold), so the demo is reliable. (~1 h)

### Acceptance criteria
- CI shows a pass rate, and a broken prompt makes CI fail.
  - `python evals/build_golden.py` produces `evals/golden.jsonl` with exactly 30 lines; `python -c "import json;print(len([json.loads(l) for l in open('evals/golden.jsonl')]))"` prints `30`.
  - `pytest evals/test_fast.py -q` runs and prints a pass/fail count; with offline mode on and the network unplugged, no World Bank request is made.
  - At least 10 of the 30 cases have `answer_lang: "es"` (or Spanish input).
  - The GitHub Actions run on a normal commit is green and its summary shows the pass rate per metric.
  - The run on the broken-prompt branch is red.

### Tests to write
- `evals/test_fast.py`, `evals/test_numbers.py`, language detector test, `build_golden.py` test that fails on a missing cache value.

### Commands
```powershell
python evals/build_golden.py
pytest evals/test_fast.py -q
pytest evals/test_numbers.py
```

### Risks / gotchas
- Number parsing: the spec's regex mixes up years and values and breaks on `1,234.5`; Spanish answers may use `,` as decimal separator; negative values like `-0.35`.
- Evals use the cached World Bank data, but the LLM is still live; results are not bit-reproducible. Temperature 0 helps, not guarantees.
- Groq free-tier limits (D3) do not fit 30 live cases per push; CI runs a ~10-case subset and the full set is manual (DL-35).
- `golden.jsonl` must be generated from the cache; the spec's values are from Oct 1, 2026 and can differ.
- "Every bug becomes a case": add inputs to `golden_inputs.jsonl` before fixing.
- Existing workflows trigger on `main`; the local branch is `master` until task 1.11 renames it (DL-30). `evals.yml` (5.6) uses `main` too.

---

## Milestone 6 — Evals v2, quality

**Goal:** retrieval recall@5, an LLM-as-judge for faithfulness, and a 2 chunk sizes × 2 models experiment with written results.
In plain words: measure how often the right passage is found and whether answers stick to their sources, then compare two chunk sizes and two models and write down what won.
**Estimated hours:** 4–6 (spec range) · Sum of task estimates below: 8.5 h (see Q17)

### Tasks
- [ ] 6.1 Write `backend/evals/test_quality.py` recall@5: share of `relevant_sections` found in the top 5 chunks for RAG and combined cases. Fill `relevant_sections` for the 8 RAG cases by reading the ingested sections (not guessing titles). (~2 h)
- [ ] 6.2 Write the judge prompt and `judge()` in `backend/evals/test_quality.py` (yes/no: "Is every claim supported by the tool results or passages?"), run through `LLMClient` with a judge model different from the answer model, set by its own environment variable `JUDGE_MODEL` (DL-26); judged runs go 3 times at temperature 0. (~1.5 h)
- [ ] 6.3 Label 20 answers yourself in `backend/evals/judge_labels.jsonl` and compute agreement; trust the judge only at ≥ 17/20. If lower, change the prompt and re-label a fresh sample. (~2 h)
- [ ] 6.4 Run the experiment: chunk size A and B (values chosen after a look at the section lengths from M3) × 2 models (names from D2), 1 judged run per config, spread over several days (DL-35); do the 3 judged runs only for the chosen config. Save raw results under `backend/evals/results/` and a summary table. (~2 h)
- [ ] 6.5 Write down the chosen settings and why in the Decision log, and add the results table to the root README. (~1 h)

### Acceptance criteria
- The README has a results table, and you can say which settings you picked and why.
  - `pytest evals/test_quality.py -q` prints recall@5 and faithfulness for the chosen config.
  - Judge agreement on the 20 labeled answers is ≥ 17 (the test or script prints `agree/20`).
  - `backend/evals/results/` holds 4 result files (one per chunk size × model) and the README table has 4 rows with the same numbers.
  - Decision log has an entry naming the chosen chunk size and model.

### Tests to write
- Recall@5 test; judge agreement check; unit test for the recall computation with a fixed fake retriever.

### Commands
```powershell
python -m app.ingest --help            # confirm chunk-size options you built in M3
pytest evals/test_quality.py -q
```

### Risks / gotchas
- Not run in CI (manual). Run it before changing prompts or models.
- Rate limits: 30 cases × 3 runs × 2 models × 2 chunk sizes is about 1.5–2M tokens against 200K/day; DL-35 scales it down (1 judged run per config, spread over days).
- Chunk-size experiments need a re-ingest per size and separate Chroma collections; do not overwrite the baseline.
- `relevant_sections` must match the metadata format from M3 exactly.
- The judge model differs from the answer model (DL-26); if Groq rate limits make that impossible (Q15), fall back to the same model with a stricter rubric and say so in the README.

---

## Milestone 7 — Observability and guardrails

**Goal:** Langfuse tracing per request, input limits, an instruction-override test, and a cost estimate per answer.
In plain words: be able to look at any answer and see what happened inside, block oversized or abusive requests, and know what an answer costs.
**Estimated hours:** 2–4 (spec range) · Sum of task estimates below: 6 h (see Q17)

### Tasks
- [ ] 7.1 Create the Langfuse project on Langfuse cloud, free tier (DL-22) and add keys to `backend/.env.example`; add the dependency in `backend/pyproject.toml`. (~0.5 h)
- [ ] 7.2 Add tracing in `backend/app/llm.py` and `backend/app/agent.py`: one trace per `/chat` request, spans for each LLM call and tool call, tokens, latency. Tracing must be off when keys are missing, so tests and CI run without it. (~2 h)
- [ ] 7.3 Add input limits in `backend/app/main.py` (message length, number of messages, request size); choose values and log them in the Decision log. Return a clear 4xx the KMP app can display. (~1 h)
- [ ] 7.4 Add an instruction-override case to `backend/evals/golden_inputs.jsonl` and a unit test in `backend/tests/test_guardrails.py` (the answer must not follow "ignore previous instructions"). (~1.5 h)
- [ ] 7.5 Add a cost estimate per answer using token usage and the current Groq prices (look up, D3; do not guess) in `backend/app/llm.py`; log it on the trace and print it in the eval summary. (~1 h)

### Acceptance criteria
- You can open one trace and walk through a whole answer.
  - After one `/chat` call, the Langfuse UI shows one trace with: the system prompt, each LLM call, each tool call with arguments and result, token counts, latency.
  - A request over the limit returns a 4xx (`curl` with an oversized body shows the status) and the app shows an error banner, not a crash.
  - `pytest tests/test_guardrails.py` passes.
  - A cost value appears for one answer on the trace and in the eval summary.

### Tests to write
- `backend/tests/test_guardrails.py` (limits, override); a test that tracing is a no-op without keys.

### Commands
```powershell
uvicorn app.main:app --reload
pytest tests/test_guardrails.py
```

### Risks / gotchas
- Do not send API keys or user data you would not want stored into traces.
- Streaming responses may not include usage unless requested; check in D3.
- Input limits must apply before the LLM call; the static token (DL-18) is a deterrent only, so these limits are the real protection of a public deploy.
- p50 latency and $/answer feed the README table; record from traces.

---

## Milestone 8 — Polish and present

**Goal:** a README with measured results, a demo, and an optional deployment; someone else can run it in under 10 minutes.
In plain words: write it up so a stranger understands it and can run it, with real numbers, a diagram and a demo.
**Estimated hours:** 3–5 (spec range) · Sum of task estimates below: 10.5 h (see Q17)

### Tasks
- [ ] 8.1 Write the root `README.md` in the spec's order: pitch, demo GIF (one EN, one ES question), architecture diagram, How it works, eval results table, design decisions, data sources and licenses, how to run, what next. The root `README.md` currently describes only the KMP app; fold that content into a "Client" section and link to `backend/README.md` for backend details. (~2 h)
- [ ] 8.2 Recreate the architecture diagram (the spec's is an embedded image not stored in the repo) as a file under `docs/`. (~1 h)
- [ ] 8.3 Record the demo GIF with an EN and an ES question; save under `docs/screenshots/`. (~1 h)
- [ ] 8.4 Add data credits: World Bank WDI (CC BY 4.0) and Wikipedia (CC BY-SA 4.0) in the README and in the app (for example in `shared/src/commonMain/.../presentation/settings/SettingsScreen.kt`). (~1 h)
- [ ] 8.5 Add `backend/Dockerfile` and (optional) deploy; set the deployed `url=` and `token=` in `backend.local.properties` for the release build, and `BACKEND_TOKEN` as an environment variable on the host. The image runs `python -m app.ingest` at build time for Chroma and copies the cache snapshot (DL-22); check that the image size and build time are acceptable. (~2 h)
- [ ] 8.6 Cold-start test: clone into a fresh folder (or have someone else do it) and follow only the README; fix every gap. Time it. (~1.5 h)
- [ ] 8.7 Optional: a chart in the app, a LangGraph port of the loop, or a promptfoo or Ragas/DeepEval comparison against the hand-written judge (DL-26). Only if everything above is done. (~2 h)

### Acceptance criteria
- Someone who isn't you can run it in under 10 minutes.
  - A fresh clone plus the README's commands starts the backend and answers `curl .../health` with 200 within 10 minutes (timed).
  - README contains: results table with real numbers, GIF, diagram, licenses (`git grep -n "CC BY" README.md` finds both).
  - CV bullet blanks from the spec are filled with real numbers.

### Tests to write
- None new. Run the full checks: `pytest`, `pytest evals/test_fast.py`, `./gradlew ktlintCheck allTests`.

### Commands
```powershell
docker build -t latam-analyst backend     # only if deploying
pytest ; pytest evals/test_fast.py
```

### Risks / gotchas
- Fresh clones have no `backend/data/` (not in git): the README must say how to build the cache and the Chroma index, and how long that takes.
- The first embedding model download is slow; say so.
- Do not publish `api-keys.local.xml` or any `.env`.

---

## Golden set plan

Target: 30 cases in `backend/evals/golden.jsonl` (10 number, 8 RAG, 5 combined, 4 no-data/out-of-scope, 3 tricky); at least 10 in Spanish. Tick when the input exists in `golden_inputs.jsonl` and the case passes build.

- [ ] Task: write `backend/evals/build_golden.py` that fills `expected_value`, `expected_values`, `expected_args` from the cache and fails if a value is missing (task 5.2).

Number (10; expected values from the cache; use all three indicators in the spec's checked set, plus population / GDP per capita / exports only if D5 shows they are complete):
- [ ] num-01 … num-10 (at least 4 in Spanish; include one value that is negative, e.g. the spec's Peru GDP growth 2023 case)

RAG (8; `must_cite_country`, `relevant_sections` from ingested sections):
- [ ] rag-01 … rag-08 (at least 3 in Spanish; cover several of the 8 countries)

Combined (5; `expected_tools` both, `expected_values` from cache):
- [ ] combo-01 … combo-05 (at least 2 in Spanish)

No-data / out-of-scope (4):
- [ ] nodata-01 Venezuela inflation (all years missing per the spec)
- [ ] nodata-02 a future year
- [ ] nodata-03 a non-LatAm country
- [ ] nodata-04 Argentina inflation 2025 (missing per the spec; confirm in the cache)

Tricky (3):
- [ ] tricky-01 Spanish with accents or regional wording (counts toward the Spanish third)
- [ ] tricky-02 vague wording (no country or year)
- [ ] tricky-03 instruction-override attempt (see 7.4)

Tracking: Spanish cases so far: __ / 10.

---

## Day-1 verification checklist

Record the result next to each item (model IDs, counts, dates). Nothing below is assumed.

- [ ] **D1 Python 3.12 present.** `py -3.12 --version` prints `Python 3.12.x`.
- [ ] **D2 Groq current model list.** Call the OpenAI-compatible models endpoint. The key lives in `api-keys.local.xml`; for this check, set it for the current terminal window only (`$env:GROQ_API_KEY = "..."`), never in a file or in a command you paste elsewhere (`Invoke-RestMethod -Uri https://api.groq.com/openai/v1/models -Headers @{Authorization="Bearer $env:GROQ_API_KEY"}`) and read Groq's docs model page. Pick 2 models for milestone 6. Record IDs and context sizes. Do not hardcode them; put in `LLM_MODEL`.
- [x] **D3 Tool calling and streaming on those models.** Send one chat completion with a single dummy tool and a prompt that needs it; check `tool_calls` in the response. Repeat with `stream: true`. Check whether a `usage` field is returned (needed in M7). Read the rate limits from Groq's console/docs and record them. Look up current per-token prices for the cost estimate.
  - **Result (2026-10-08).** Models visible to this key (from `/models`, partial D2): `openai/gpt-oss-120b`, `openai/gpt-oss-20b`, `openai/gpt-oss-safeguard-20b`, `qwen/qwen3.8-27b` (all 131072 context); no Llama 3.x chat models listed. Tested the first, second and fourth (the safeguard model was not tested).
  - Tool calling works non-streaming and streaming on all three tested models; `tool_calls` came back in every response. `usage` is returned non-streaming, and in the stream when `stream_options: {include_usage: true}` is sent (needed for M7).
  - Argument convention varies: gpt-oss-120b and qwen used `"Peru"` / `"Inflation (annual %)"`, gpt-oss-20b used `"PER"`; two of three did not use the World Bank indicator code on their own. The tool description must say which form is expected (DL-23).
  - Rate limits (response headers, 3 models): 1000 requests/day, 8000 tokens/minute. Docs list 200K tokens/day for both gpt-oss models; the qwen daily token limit was not found. Reasoning tokens count in `completion_tokens` (gpt-oss-20b: 130 completion tokens for a one-line tool call). A 429 carries `retry-after`.
  - Prices per 1M tokens, input/output (docs, 2026-10-08): gpt-oss-120b $0.15/$0.60; gpt-oss-20b $0.075/$0.30; qwen3.8-27b $0.80/$4.00.
  - Still open: the daily token limit for qwen (Q15 resolved, DL-35); whether "cached tokens do not count" refers to Groq's own prompt caching.
- [ ] **D4 Coverage of the 3 unchecked indicators.** For `SP.POP.TOTL`, `NY.GDP.PCAP.CD`, `NE.EXP.GNFS.ZS`, one request each:
  `python -c "import httpx; d=httpx.get('https://api.worldbank.org/v2/country/ARG;BOL;BRA;CHL;COL;ECU;MEX;PER/indicator/SP.POP.TOTL', params={'format':'json','date':'2021:2025','per_page':500}).json(); print(d[0]); print(len(d[1]), sum(r['value'] is None for r in d[1]))"`
  Expected: 40 rows if complete; the second number is the null count. Record gaps by country/year. Also confirm the 8-country list in one request works (the spec tested 3).
- [ ] **D5 Pagination and metadata.** From the `print(d[0])` above, note which fields describe pages/totals, and test with `per_page=5` to see multiple pages. Decide how `get_indicator` handles it.
- [ ] **D6 Embedding model.** Read both model cards (`paraphrase-multilingual-MiniLM-L12-v2`, `multilingual-e5-small`): size, max sequence length, Spanish support, any required prefixes. Run a small bake-off: 10 EN/ES query-passage pairs from the fetched articles, compare top-1 and top-5 hits. Record the choice in the Decision log.
- [ ] **D7 The 16 Wikipedia articles.** For each country in EN and ES, call the MediaWiki API (`action=query&prop=extracts&explaintext=1&titles=<title>&format=json`) and confirm: the page exists (no `missing`), redirects, extract word count (spec says ~15–17k words for Peru), and how section headings appear in the plain text. Record the 16 exact titles. The spec verified only Peru.
- [ ] **D8 Error shapes.** Request an unknown country (`.../country/XXX/indicator/FP.CPI.TOTL.ZG?format=json`), an unknown indicator, and a date range with no data; save the raw JSON as test fixtures. Check Venezuela (`VEN`) inflation to see what "missing" looks like.
- [ ] **D9 Country codes.** `https://api.worldbank.org/v2/country?region=LCN&format=json&per_page=100` includes ARG, BOL, BRA, CHL, COL, ECU, MEX, PER.

---

## Decision log

Every technical decision gets a row with an ID (DL-n). Tasks refer to the ID when they depend on a decision. When an open question is answered, add a row here and replace the question with a pointer.

| ID | Date | Decision | Reason |
| --- | --- | --- | --- |
| DL-1 | spec | Python 3.12 + FastAPI backend; KMP app only calls `/chat` over OpenAI-style SSE | Most AI engineer postings list Python; eval tooling lives there; matching Groq's stream keeps KMP changes small |
| DL-2 | spec | No agent framework in v1: hand-written tool loop, max 5 steps | Explainable in interviews; easier to debug and eval; LangGraph port is optional in M8 |
| DL-3 | spec | Groq via the OpenAI SDK behind an `LLMClient` interface; model names not hardcoded | Swapping model or provider is one change; evals compare models |
| DL-4 | spec | World Bank country metadata replaces REST Countries | REST Countries v3.1 is deprecated; v5 needs an account and key |
| DL-5 | spec | Debt indicator `GC.DOD.TOTL.GD.ZS` dropped | Too sparse (Chile none, Peru stops at 2021) |
| DL-6 | spec | One indicator per World Bank request | Multi-indicator requests failed in testing |
| DL-7 | spec | Evals run against the SQLite cache, not the live API; CI runs only fast code checks | Reproducible results when the World Bank updates data |
| DL-8 | spec | 8 countries: ARG, BOL, BRA, CHL, COL, ECU, MEX, PER; Venezuela excluded (kept at most as a no-data case) | Venezuela data missing |
| DL-9 | spec | RAG: Wikipedia EN + ES, multilingual embeddings, Chroma | Spanish questions must find English passages and vice versa |
| DL-10 | 2026-10-02 | The existing KMP app stays at the repo root (`shared/`, `androidApp/`, `iosApp/`, `desktopApp/`); `backend/` is added at the root; no `app-kmp/` folder | Moving a Gradle project would break builds and CI; the spec's tree is only a sketch |
| DL-11 | 2026-10-02 | KMP tasks name real files under `shared/data` (network, repository, local), `shared/domain` (model) and `shared/src/commonMain` (di, presentation) | Real layout found in the repo |
| DL-12 | 2026-10-02 | Keep one repo: `backend/` is a sibling of the KMP modules, not a separate project; CI workflows get `paths:` filters; `backend/README.md` holds backend setup and the root README gives the system overview | Portfolio reviewers see the whole system in one place; the evals and the app share the SSE contract; one repo is cheaper to maintain at 4–8 h/week; a split stays cheap later because `backend/` is self-contained |
| DL-13 | 2026-10-02 | The KMP app is no longer a generic multi-provider client: it connects only to the LatAm backend. Provider, model, API key and temperature settings are removed from the app; the backend owns them. The Groq key and the runtime temperature (0.7) are read by the backend from `api-keys.local.xml` (override: `GROQ_API_KEY`); only the backend reads that file | Product direction; keeps keys off the device and lets evals test exactly what ships. The earlier "check many providers" goal now lives in the backend's `LLMClient` interface |
| DL-14 | 2026-10-02 | Rename the GitHub repo (spec's suggestion: `latam-economy-analyst`, name to confirm) | The current name describes only the chat client |
| DL-15 | 2026-10-02 | **Sources and tool names reach the app as extra chunks in the same `/chat` stream.** The backend sends normal `data:` lines with `"choices": []` and one extra top-level field, `x_analyst`, holding exactly one of: `{"tool_call": {"name": "...", "status": "running" | "done"}}` or `{"sources": [{"title", "url", "country", "lang", "section"}]}`. Text still travels as `choices[0].delta.content`; the stream still ends with `data: [DONE]`. App side: one optional field on `ChatCompletionChunkDto`, two new `ChatEvent` variants (`ToolCall`, `Sources`), no new parser state. Not chosen: one chunk at the end only, named SSE `event:` lines, links inside the answer text, a separate endpoint | The app already ignores unknown JSON fields (`ignoreUnknownKeys = true`) and unknown line types, so the change is small and old clients keep working; separate chunks let the UI show tool chips while the model is still working; both sides can be tested with one line of input. The field name `x_analyst` and the exact fields are final unless a task finds a problem; if renamed, change backend and app together |
| DL-16 | 2026-10-02 | This `PLAN.md` is at the repo root; `docs/PLAN_LLM.md` is the finished KMP app plan and is historical | The earlier plan was renamed and moved under `docs/` |
| DL-17 | 2026-10-02 | **The app gets the backend URL (and the token, DL-18) from a build-time config generated by Gradle, with a built-in per-platform default as fallback.** A git-ignored file at the repo root, `backend.local.properties`, holds `url=` and `token=`. A Gradle task in `shared/build.gradle.kts` reads it and writes a plain Kotlin file (`BackendBuildConfig`, nullable `url` and `token`) into the `commonMain` source set, so Android, iOS and desktop all get it through Gradle, with no Xcode or `BuildConfig` involved. If the file is missing (fresh clone, CI), the values are `null` and the app uses a per-platform default URL supplied by each `platformModule()` (Android emulator alias, `localhost` for iOS and desktop; both to be verified in 1.8) and sends no token. A committed `backend.local.properties.example` documents the format. Time-box: if the Gradle generation is not working after one 2 h session, fall back to the per-platform defaults only, with the URL and token as constants in a git-ignored Kotlin file, and record that in this log. Not chosen: a debug-only URL field in Settings (you asked to remove settings), Android-only `BuildConfig`, passing values through `Config.xcconfig` | Judged low risk: the generated file is ordinary Kotlin that Gradle compiles for every target, and the per-platform default (the simple option) stays as the fallback. Remaining risk: wiring the task with the existing KSP, ktlint and the `com.android.kotlin.multiplatform.library` plugin; iOS cannot be run here (Q10), so iOS is verified only by compiling common code |
| DL-18 | 2026-10-02 | **The backend checks a static access token; the app sends it automatically. There is no login or user password.** The token is a long random string stored in the git-ignored `backend.local.properties` (`token=`). The app receives it at build time (DL-17) and sends it in the same `Authorization: Bearer <token>` header that `ChatCompletionsApi.kt` already sends; the backend reads the same file (override: environment variable `BACKEND_TOKEN`, for deploys) and answers 401 when the header is missing or wrong. `GET /health` needs no token. If the backend has no token configured (local development), it accepts requests and logs a warning. The app already maps HTTP 401 to `ChatError.Unauthorized` in `ErrorMapping.kt`. `SecureStorage` is not needed: nothing is typed or stored by the user. Not chosen: no protection at all, a per-user login | Stops casual use of a public endpoint and of your Groq quota. Limit: a token inside an app can be extracted from the installed app, so it is a deterrent, not real security; input limits (7.3) and the free-tier quota remain the real protection |
| DL-19 | 2026-10-05 | **`api-keys.local.xml` is the main config source for the backend; `GROQ_API_KEY` overrides it; no `.env` file is used.** The backend reads the `provider="groq"` entry (key as the element text, optional `temperature` attribute, default 0.7). The `model` and `baseUrl` attributes and the OpenAI entry are ignored. The README (task 1.12) gets a "Required local configuration" section that tells the reader to create `api-keys.local.xml` (gitignored) with that entry, and `backend.local.properties` (copied from the committed `.example`) with `token=` and `url=`, and says plainly that the app and the backend do not work without them. `backend.local.properties.example` (1.6) and a committed `api-keys.local.xml.example` hold the format with placeholders. The comment inside your local XML says "The app doesn't read this file"; update that comment when 1.3 lands (you edit the file, I do not) | One place for the key, no extra dependency, nothing to set by hand in each PowerShell window. CI and deploys have no XML file, so they use `GROQ_API_KEY`. Not chosen: `backend/.env` with `python-dotenv` (a second place for the same key, a dependency not in the spec) |
| DL-20 | 2026-10-05 | **`ChatApiConfig` and `ChatCompletionsApi` stay generic. When the backend cannot be reached, the app keeps a built-in fake reply and tells the user to check the connection to the backend.** The fake reply (the existing `FakeReplySource`) is used only for connection failures (`ChatError.NoNetwork`, `Timeout`), not for 401 or 429, and is clearly marked as a fallback. The message text changes from "No network connection." to "Can't reach the backend. Check the connection to the backend." (Timeout: "The backend did not answer. Check the connection to the backend."). `Unauthorized` text changes from "Invalid API key." to "Invalid access token. Check `backend.local.properties`." because the user no longer types a key (DL-18). The exact way to combine the fallback reply with the message (a banner above the fake reply, or the error alone) is decided in task 1.7 when I see the code; tell me if you prefer one | Removing the generic classes is churn with no benefit (small, already tested). A fresh clone with no backend still shows a working UI, and the message says what to do. Limit: a fake reply can hide a real outage, so it must be visibly labelled |
| DL-21 | 2026-10-05 | **Milestone 3 is narrowed to cited answers from Wikipedia passages; the "no data" check for "What is Peru's GDP in 2030?" moves to Milestone 4.** M3 keeps the temporary always-retrieve path (task 3.7) and its acceptance criteria are: cited answers, source chips, ingestion counts, retrieval of `CHL` for the Spanish query. The Peru-2030 criterion is now in M4's acceptance criteria, where the agent has the data tools and can see that the value does not exist. `docs/` spec text is not changed | The temporary path has no data tools, so it cannot know that a 2030 value is missing; forcing it would need a special case that M4 deletes. Limit: until M4, the app may answer the Peru-2030 question from passages with a made-up or vague figure, so do not demo that question before M4 |
| DL-22 | 2026-10-05 | **CI uses a small committed cache snapshot and rebuilds Chroma; Langfuse runs on the cloud free tier.** `cache.sqlite` and `chroma/` stay out of git, as the spec says. A small snapshot of the World Bank cache, only the rows the golden cases need, is committed as `backend/tests/fixtures/cache_snapshot.sqlite` and CI runs in offline mode (task 2.2) against it, so number answers are deterministic and CI never calls the World Bank. CI builds Chroma with `python -m app.ingest`, with the pip and Hugging Face model caches stored by `actions/cache` so `sentence-transformers` is not downloaded every run. The ingest step needs the Wikipedia articles; if calling Wikipedia in CI proves flaky, also commit the cleaned article text (decide in 5.6). The same snapshot and build-time ingest are used in the Docker image (8.5). Not chosen: a CI artifact (extra moving part), rebuilding the World Bank cache in CI (slow, non-deterministic), self-hosted Langfuse (extra work, no benefit for a portfolio project) | Deterministic evals with the least extra infrastructure. Limits: the snapshot must be regenerated when the golden set or the cache policy (Q13) changes; the first CI run is slow until the caches are warm; the Langfuse free tier has usage limits (check in 7.1) |
| DL-23 | 2026-10-05 | **The data tool is `get_indicator(countries, indicator, start_year, end_year)`.** `countries` is a list of ISO3 codes, `indicator` is a friendly name or a raw World Bank code (the tool maps names to codes), and a single year is `start_year == end_year`. `exports` is added for `NE.EXP.GNFS.ZS`. The tool schema given to the model shows the friendly names only, so the model rarely has to produce raw codes. Golden cases and the eval checks use this form. Not chosen: a `years` list (longer for the model to write, and the tool would have to expand it) | One shape across the spec's three versions, the tool schema, the tests and the golden set. A range is easier for the model to get right and simpler to test. Limit: asking for non-consecutive years takes two calls; accepted |
| DL-24 | 2026-10-05 | **The CI gate is aggregate pass-rate thresholds per metric, with one retry per failed case and a small `must_pass` group asserted per case.** Everything that needs the live model (tool choice, tool arguments, numbers in the answer, "no data" honesty, language match) is scored per case, then compared with the spec's Day-1 target for that metric; CI fails if any metric is below target. A failed case is retried once before it counts as a failure. A small group of cases marked `must_pass` in the golden set (about 3: a plain number question, the Venezuela "no data" case, the instruction-override case) must pass in both tries or CI fails, so a single obvious regression cannot hide inside an average. Deterministic code (`numbers.py`, the language detector, `build_golden.py`, the loop's 5-step stop with a scripted fake model) is covered by normal per-test asserts that never touch the LLM. The job summary prints the pass rate per metric. Not chosen: per-case asserts for every case (one model flake fails CI) | A broken prompt drops several metrics at once, so thresholds catch it, while one LLM flake does not turn the build red. Limit: a threshold below 100% lets a small real regression through; the per-metric results are printed so a drift is visible. Evals run at the temperature chosen in Q16, still open |
| DL-25 | 2026-10-05 | **The language-match check uses `lingua-language-detector`, restricted to English and Spanish.** It is used only in `backend/evals/` to check that the answer is in the language of the question; the app and the runtime backend do not use it. Not chosen: `langdetect` (needs a fixed seed, weak on short text), a hand-written stopword counter (fails on short, number-heavy answers; kept as the fallback), an LLM judge for this check (costs a call per case, not deterministic, and arrives only in M6) | Short answers like a single figure with a sentence are common here, and `lingua` is accurate and deterministic on short text. Limit: a large dependency; acceptable because CI already installs `sentence-transformers` |
| DL-26 | 2026-10-05 | **The judge is a different model from the answer model, and v1 uses plain pytest with the hand-written judge; promptfoo and Ragas/DeepEval are out of v1.** The judge model comes from `JUDGE_MODEL` and the README says which one it is. If rate limits (Q15) make a second model impossible, use the same model with a stricter rubric and state it. The hand-written judge (about 50 lines, task 6.2) is checked against your 20 labels (task 6.3). promptfoo and Ragas/DeepEval stay listed only as an optional comparison in 8.7. The spec's tech stack table lists them, so the README says they were considered and left out on purpose | A model grading its own answers tends to be lenient, and a hand-written judge can be explained line by line in an interview. Limit: less tooling to show on a CV; the 20 hand labels are the evidence that the judge can be trusted |
| DL-27 | 2026-10-05 | **`/chat` streams only the final answer; tool steps run silently apart from the `tool_call` progress chunks.** The agent loop (4.3) keeps non-streaming model calls. Order of SSE chunks: `tool_call` `running`/`done` chunks as tools run, then the `sources` chunk, then the final answer text, then `[DONE]`. When a model step returns no tool calls, its text is the final answer and `/chat` sends it in small chunks (for example a few words each), so the app's existing streaming UI works unchanged. This is replayed text, not live token streaming: the first word appears only after the last model call finishes, and the README must not call it live streaming. Not chosen: streaming every step (intermediate text is rarely useful), and live streaming of the last model call (needs streamed tool-call delta handling in the loop). Possible later upgrade if the pause is too long | Simplest loop that fits the existing UI, and the tool chips fill the wait. Limit: a delay before the first word on tool-heavy questions (the model calls plus tool time); measure it in 4.6 and report it in the session log |
| DL-28 | 2026-10-05 | **iOS is built to compile and is marked "compiles, not run" until you verify it on a Mac; the verification is an optional Definition of done B.** You have occasional access to a Mac, so iOS does not block milestones. While there is no verification, the README and the session log say "iOS: compiles, not run" and never that it works. The `Info.plist` setting for plain-HTTP dev URLs is added in 1.8 and marked unverified. The generated build config (DL-17) is verified on iOS only by compiling common code until then. When you have the Mac, run the optional checklist in the Definition of done (simulator launch, backend reachable, streaming, token accepted, chips and links, unreachable-backend message, persistence) and record the result | Honest status without waiting for hardware. Limit: iOS-only bugs (ATS, the URL on the simulator, link opening, the generated config) can sit unseen for weeks; the checklist is short so one Mac session covers it |
| DL-29 | 2026-10-05 | **The spec file is renamed to `docs/project-spec.md` and the repo to `latam-economy-analyst`.** The file was moved with `git mv`, so its history is kept, and the link in `PLAN.md` is updated; no other file linked to the old name. The GitHub repo rename is task 1.11 (new remote URL, README badge URLs). `rootProject.name` and the local folder stay as they are | A short, stable name to link to; the repo name matches the project name in the spec. Limit: GitHub redirects the old repo URL, but the badge URLs and any outside links must still be updated by hand |
| DL-30 | 2026-10-05 | **The branch is renamed from `master` to `main`; the workflows stay as they are.** `ci.yml` and `ios.yml` already trigger on `main`, and the new `evals.yml` uses `main`. The rename is part of task 1.11, done together with the repo rename: until then, pushes to `master` run no checks (pull requests still do), so do the rename before relying on CI for direct pushes. Not chosen: changing the workflows to `master` (keeps a non-default name) | Matches the workflows and the GitHub default. Limit: the old remote `master` branch is deleted only after `main` is confirmed on GitHub, and any local clone elsewhere needs `git fetch` and `git branch -u origin/main main` |
| DL-31 | 2026-10-08 | **World Bank cache never expires.** Each entry stores its fetch date. Refresh is manual (a script flag) and done only before regenerating the README results. Not chosen: a TTL | Annual data barely changes; a TTL would make eval results drift between runs. Limit: stale data is possible until someone refreshes; the fetch date makes it visible |
| DL-32 | 2026-10-08 | **Data tools accept any well-formed World Bank country code.** The 8 LatAm countries stay the focus of the prompt and corpus. Missing data comes back as a "no data" tool result from the API. Not chosen: rejecting codes outside the 8 | "Non-LatAm" and "Venezuela" eval cases then test how the agent handles missing data, not an artificial rejection. Limit: larger cache and more varied calls; validate the code format only |
| DL-33 | 2026-10-08 | **Temperature: 0 for tool-calling steps, 0.7 only for the final answer.** Evals run the tool steps at 0 as shipped, and the final answer at 0, plus one extra run at 0.7 reported separately. Decided before task 5.3. Not chosen: 0.7 everywhere | Best fit for "what you test is what ships" while keeping tool-argument accuracy reproducible. Limit: the final-answer eval at 0 is not exactly what ships; the separate 0.7 run covers the gap |
| DL-34 | 2026-10-08 | **Hour estimates are not rescaled yet.** Time the first milestone-1 tasks, then multiply the remaining estimates by the real ratio. Cut candidates, in order: 1.12, 8.5 (deploy), 8.7, part of M6. Not chosen: trimming scope now | Estimates sum to about 85 h vs the spec's 29–45 h; at 4–8 h/week that is 11–21 weeks. Measuring first avoids cutting on a guess |
| DL-35 | 2026-10-08 | **Eval volume is fitted to the Groq free tier (1000 requests/day, 8000 tokens/minute, 200K tokens/day on gpt-oss).** (1) The LLM client honors `retry-after` on 429 and paces calls to stay under the per-minute limit. (2) CI runs a subset of about 10 golden cases (the `must_pass` group plus about 7 more) on backend pushes, with the DL-24 thresholds applied to that subset; the full 30 cases run manually or on `workflow_dispatch` or a nightly trigger, once before README numbers are frozen. (3) M6: the 2×2 experiment uses 1 judged run per config; the 3 judged runs happen only for the chosen config; runs are spread over several days; the judge uses the other gpt-oss model (qwen's daily limit is unknown), with the DL-26 same-model fallback if that is too tight. (4) Not chosen for now: record/replay cache of LLM responses (hash of model, messages, tools, temperature); add it only if dev iteration burns the quota in practice. Token figures behind this are estimates (about 5–8K tokens per case), to be replaced by measured numbers in 5.3 | A full 30-case run is about the whole daily token budget and 20–30 minutes at the minute limit, so running it per push does not fit; the subset still catches a broken prompt because several metrics drop at once. Limit: a small regression outside the subset is seen only in the full run |

---

## Open questions

Not decided; each needs an answer before the task that depends on it.

- **Q1** (transport for sources and tool names): resolved, see DL-15.
- **Q2a** (backend URL and token): resolved, see DL-17 and DL-18.
- **Q2b** (backend config source): resolved, see DL-19.
- **Q2c** (leftover generic code and unreachable backend): resolved, see DL-20.
- **Q3** (M3 answers before the agent loop): resolved, see DL-21.
- **Q4** (data in CI and heavy dependencies): resolved, see DL-22.
- **Q5** (one argument convention): resolved, see DL-23.
- **Q6** (CI gate style): resolved, see DL-24.
- **Q7** (language detector): resolved, see DL-25.
- **Q8** (judge and tooling): resolved, see DL-26.
- **Q9** (streaming with tools): resolved, see DL-27.
- **Q10** (iOS): resolved, see DL-28.
- **Q11** (names and docs): resolved, see DL-29.
- **Q12** (branch): resolved, see DL-30.
- **Q13** (cache freshness): resolved, see DL-31.
- **Q14** (tool scope): resolved, see DL-32.
- **Q15** (Groq limits vs eval volume): resolved, see DL-35.
- **Q16** (temperature): resolved, see DL-33.
- **Q17** (hour estimates): resolved, see DL-34.

---

## Session log

Template (copy one per session; newest at the bottom):

### YYYY-MM-DD · Milestone N · Xh
- Done:
- Blocked:
- Next:
