# AI Engineering Portfolio Project — LatAm Economy Analyst

Sep 30, 2026 · @Ruby López

## Project idea

Turn your KMP LLM chat app into **LatAm Economy Analyst**: a bilingual (EN/ES) assistant that answers questions about Latin American economies using live World Bank data and Wikipedia articles, with citations and a measured eval suite. Expect about 5–8 weeks at 4–8 h/week.

What a user can do in the finished app:

- Ask "¿Cómo creció el PBI de Perú vs Chile desde 2021?" and get exact numbers from the World Bank API, with the year and indicator cited (tool calling).
- Ask "Why did Argentina's inflation spike?" and get an explanation grounded in Wikipedia passages, with links (RAG).
- Ask a combined question such as "Which Andean country had the lowest inflation in 2024, and what drives its economy?" The agent calls tools for the numbers and RAG for the context.

Why this project:

- **Exact ground truth for evals.** The API returns the true numbers, so you can check automatically whether the model reported the right value. That is the strongest kind of eval in a portfolio.
- **All core AI engineering skills in one app:** tool calling, RAG, multi-step agents, evals, tracing, cost and latency.
- **Distinctive but low-risk.** LatAm focus and Spanish support set it apart, while the World Bank data is clean and free.
- **One portfolio piece for two goals:** KMP and Compose Multiplatform on the client, AI engineering on the backend.

The Python backend is deliberate. Most AI engineer job postings list Python, and the eval tooling lives there. The KMP app stays the face of the product.

## Data sources (verified)

All endpoints below were tested on October 1, 2026. Both main sources are free and need no API key.

| Source | Endpoint tested | Result | Use in the app |
| --- | --- | --- | --- |
| [World Bank Indicators API](https://datahelpdesk.worldbank.org/knowledgebase/articles/889392-about-the-indicators-api-documentation) | `api.worldbank.org/v2/country/PER;CHL;COL/indicator/NY.GDP.MKTP.KD.ZG?format=json&date=2021:2025` | Works, no key. Data through 2025, last updated July 2026 | Tool: `get_indicator(countries, indicator, years)` |
| World Bank country metadata | `api.worldbank.org/v2/country/PER?format=json` | Works: region, income level, capital city, coordinates | Tool: `get_country_info(code)`. Replaces REST Countries |
| World Bank region list | `api.worldbank.org/v2/country?region=LCN&format=json&per_page=100` | Works: 42 countries and territories | Pick the country list for the app |
| Wikipedia (EN and ES) | `en.wikipedia.org/wiki/Economy_of_Peru`, `es.wikipedia.org/wiki/Economía_del_Perú` | Both exist, about 15,000–17,000 words each | RAG corpus; fetch plain text with the MediaWiki API (`action=query&prop=extracts&explaintext=1`) |
| ~~REST Countries v3.1~~ | `restcountries.com/v3.1/alpha/PER` | **Deprecated.** Redirects to an error; v5 needs an account and API key | Not used; World Bank metadata covers it |

Indicators checked, and how complete they are for the main countries (2021–2025):

| Indicator | Code | Coverage |
| --- | --- | --- |
| GDP growth (annual %) | `NY.GDP.MKTP.KD.ZG` | Complete through 2025 |
| Inflation, consumer prices (annual %) | `FP.CPI.TOTL.ZG` | Complete, except Argentina 2025 and Venezuela (all years missing) |
| Unemployment (% of labor force) | `SL.UEM.TOTL.ZS` | Complete through 2025 |
| Central government debt (% of GDP) | `GC.DOD.TOTL.GD.ZS` | Sparse: Chile has no data, Peru stops at 2021. Leave it out |
| Population, GDP per capita, exports % of GDP | `SP.POP.TOTL`, `NY.GDP.PCAP.CD`, `NE.EXP.GNFS.ZS` | Standard indicators; check coverage on day 1 |

What this means for the build:

- **Start with 8 countries:** Argentina, Bolivia, Brazil, Chile, Colombia, Ecuador, Mexico and Peru. Skip Venezuela at first because of its missing data, or keep it on purpose as a "data not available" test case.
- **One indicator per request.** A combined multi-indicator request returned "The provided parameter value is not valid", so the tool loops over indicators.
- **Handle `null` values.** The tool returns "no data for that year", and the model must say so rather than guess. This becomes an eval case.
- **Cache responses** in SQLite, so evals run offline and give the same results every time.
- **Licenses:** World Development Indicators are published under CC BY 4.0 (confirmed on the World Bank Data Catalog page for WDI, October 1, 2026). Wikipedia text is CC BY-SA 4.0. Credit both in the README and in the app's sources.

## Architecture

&#91;embedded content: LatAm Economy Analyst architecture · app, backend, LLM, data, evals\]

The KMP app talks only to `/chat`. The agent loop decides when to retrieve chunks or call tools, and the eval suite calls the same loop the app uses, so what you test is what ships. The Groq key lives only on the backend.

## Tech stack

Keep the stack small and free to run. Write the agent loop by hand first; a framework is optional later.

| Layer | Choice | Why |
| --- | --- | --- |
| Client | Your KMP app (Compose Multiplatform, Ktor client) | Already built; only the endpoint and a sources UI change |
| API | Python 3.12 + FastAPI, streaming over SSE in the OpenAI-compatible format | Standard in AI job postings. Matching Groq's stream format keeps the KMP changes minimal |
| LLM | Groq through the OpenAI-compatible Python SDK | Same provider as the app; free tier, fast, supports tool calling and JSON mode. Check Groq's model list for current model names |
| Data API | World Bank Indicators API via `httpx` | Free, no key, exact numbers (see Data sources) |
| Cache | SQLite table keyed by request URL | Offline, reproducible evals; avoids hitting the API repeatedly |
| RAG corpus | Wikipedia "Economy of X" articles, EN + ES, for 8 countries | About 16 articles; fetched as plain text through the MediaWiki API |
| Embeddings | `sentence-transformers` locally, with a multilingual model (e.g. `paraphrase-multilingual-MiniLM-L12-v2` or `multilingual-e5-small`) | Free and offline; a multilingual model lets Spanish questions find English passages and vice versa |
| Vector store | Chroma (local, persisted to disk) | Zero setup. Optional upgrade later: Postgres + pgvector |
| Evals | pytest + promptfoo; Ragas or DeepEval for RAG metrics | pytest runs in CI; promptfoo compares prompts and models side by side |
| Tracing | Langfuse (free cloud tier or self-hosted) | Shows each prompt, tool call, cost and latency |
| CI | GitHub Actions | Runs unit tests and the fast eval subset on every push, against the cache |
| Deploy (optional) | Docker on Render, Fly.io or Railway | A live URL for the README demo |

Two rules keep the project honest:

- **Provider behind an interface.** Put one `LLMClient` class in front of Groq, so changing model or provider is one line. Evals then compare models for you.
- **No framework in v1.** Write the RAG and tool loop yourself (about 150 lines). Then you can explain every step in an interview. Porting it to LangGraph in milestone 8 is a good optional extra.

## Roadmap

The project has 8 milestones and 29–45 hours in total. That's about 5–8 weeks at 4–8 h/week. Milestones 1–5 (about 20–30 h, 3–5 weeks) already give you a version worth showing. Each milestone ends with something that works and a commit you could show; if a week gets eaten by the bootcamp, skip it.

1. **Backend skeleton** (4–6 h). FastAPI app with a `/chat` endpoint that proxies to Groq and streams over SSE in the same format Groq uses. Point the KMP app at it.
   - Done when: the app chats through your backend, and the Groq key no longer ships in the app.
2. **Data tools** (3–5 h). Write `get_indicator(countries, indicator, years)` and `get_country_info(code)` over the World Bank API, with a SQLite cache. Handle pagination, `null` values and unknown codes. Unit-test them without the LLM.
   - Done when: `get_indicator(["PER","CHL"], "inflation", 2021, 2025)` returns clean rows, and a second call reads from the cache.
3. **RAG over Wikipedia** (5–7 h). Fetch the EN and ES "Economy of X" articles for 8 countries, split them by section, embed them with a multilingual model and store them in Chroma with metadata (`country`, `lang`, `section`, `url`). Answers cite their passages, and the app shows them as source chips.
   - Done when: "What drives Chile's economy?" answers with citations, and "What is Peru's GDP in 2030?" answers that the data doesn't exist.
4. **Agent loop** (4–6 h). The model chooses between the data tools and `search_docs(query)`, for at most 5 steps. Combined questions call both.
   - Done when: "Compare inflation in Peru and Colombia in 2023 and explain the difference" calls `get_indicator` and `search_docs`, and the app shows which tools ran.
5. **Evals v1, code checks** (4–6 h). Build a golden set of 30 cases with expected values computed from the cached API data. Add pytest checks for tool choice, tool arguments, exact numbers (within rounding), "no data" handling and Spanish answers to Spanish questions. Run them in GitHub Actions.
   - Done when: CI shows a pass rate, and a broken prompt makes CI fail.
6. **Evals v2, quality** (4–6 h). Add retrieval recall@5, an LLM-as-judge for faithfulness, and one experiment: 2 chunk sizes × 2 models, with the results written down.
   - Done when: the README has a results table, and you can say which settings you picked and why.
7. **Observability and guardrails** (2–4 h). Add Langfuse tracing per request (prompt, tools, tokens, latency), input limits, an instruction-override test, and a cost estimate per answer.
   - Done when: you can open one trace and walk through a whole answer.
8. **Polish and present** (3–5 h). Write the README with the architecture diagram, eval results, data licenses and a demo GIF, and deploy the backend (optional). Optional extras: a small chart in the app, or a LangGraph version of the loop.
   - Done when: someone who isn't you can run it in under 10 minutes.

## Evaluation plan

The eval suite is what sets this apart from a tutorial project, and the World Bank data gives exact answers to check against. Start with 30 cases and cheap code checks, then add judged metrics.

**Golden set** (`evals/golden.jsonl`, one case per line). Values below come from the API as fetched on October 1, 2026. Generate them from your cache with a script rather than typing them by hand:

```json
{"id": "num-01", "input": "What was Peru's inflation in 2024?", "type": "number", "expected_tool": "get_indicator", "expected_args": {"countries": ["PER"], "indicator": "FP.CPI.TOTL.ZG", "years": [2024]}, "expected_value": 2.01, "tolerance": 0.05}
{"id": "num-02-es", "input": "¿Cuánto creció el PBI de Perú en 2023?", "type": "number", "expected_value": -0.35, "tolerance": 0.05, "answer_lang": "es"}
{"id": "nodata-01", "input": "What was Venezuela's inflation in 2023?", "type": "no_data", "expect": "says data is not available, gives no number"}
{"id": "rag-01", "input": "What are the main sectors of Chile's economy?", "type": "rag", "must_cite_country": "CHL", "relevant_sections": ["Economy of Chile#Sectors"]}
{"id": "combo-01", "input": "Compare inflation in Colombia and Mexico in 2023 and explain the difference", "type": "combined", "expected_tools": ["get_indicator", "search_docs"], "expected_values": {"COL": 11.74, "MEX": 5.53}}
```

Aim for this mix: about 10 number questions, 8 RAG questions, 5 combined questions, 4 "no data" or out-of-scope cases (Venezuela, future years, non-LatAm countries) and 3 tricky ones (Spanish, vague wording, attempts to override instructions). Make at least a third of all cases Spanish.

| Metric | How it's measured | Target to start |
| --- | --- | --- |
| Number accuracy | Code: the number in the answer equals `expected_value` within the tolerance | ≥ 95% |
| Tool choice accuracy | Code: the tools called equal `expected_tool(s)` | ≥ 90% |
| Tool argument accuracy | Code: country codes, indicator code and years equal `expected_args` | ≥ 85% |
| No-data honesty | Code: no number in the answer, plus a "not available" phrase | 100% |
| Language match | Code: the answer is in the question's language (a simple language detector) | ≥ 95% |
| Retrieval recall@5 | Code: share of `relevant_sections` found in the top 5 chunks | ≥ 80% |
| Faithfulness | LLM-as-judge, yes/no: "Is every claim supported by the tool results or passages?" | ≥ 85% |
| Latency and cost | From Langfuse traces: p50 seconds and $ per answer | Record them; no target |

Rules that make the numbers trustworthy:

- **Evals run against the cache,** never the live API, so results don't change when the World Bank updates its data.
- **Run evals at temperature 0,** and run the judged ones 3 times so a single noisy run doesn't mislead you.
- **Check the judge.** Label 20 answers yourself and confirm the judge agrees on at least 17 before you trust it.
- **CI runs the fast subset** (code checks only). The judged set runs manually before you change prompts or models.
- **Every bug becomes a case.** When the app answers badly, add that input to `golden.jsonl` before you fix it.

## Repo structure and starter code

Use one repo with two top-level folders, so a reviewer sees the whole system in one place.

```text
latam-economy-analyst/
├── app-kmp/                 # your existing Compose Multiplatform app
├── backend/
│   ├── app/
│   │   ├── main.py          # FastAPI routes: /chat (SSE), /health
│   │   ├── llm.py           # LLMClient wrapper around Groq
│   │   ├── worldbank.py     # API client + SQLite cache
│   │   ├── ingest.py        # Wikipedia → sections → embed → Chroma
│   │   ├── retrieval.py     # search_docs(query, k)
│   │   ├── tools.py         # tool schemas + Python functions
│   │   ├── agent.py         # the tool-calling loop
│   │   └── prompts.py       # system prompt, versioned
│   ├── data/                # cache.sqlite, chroma/ (generated, not in git)
│   ├── evals/
│   │   ├── build_golden.py  # fills expected values from the cache
│   │   ├── golden.jsonl
│   │   ├── test_fast.py     # code checks (CI)
│   │   └── test_quality.py  # judge + recall (manual)
│   └── pyproject.toml
├── .github/workflows/evals.yml
└── README.md
```

**The World Bank tool** (`worldbank.py`). The response shape matches what the API returned in testing: a list of `[metadata, rows]`, or an error message in place of the metadata:

```python
import httpx

BASE = "https://api.worldbank.org/v2"
INDICATORS = {  # friendly name → World Bank code
    "gdp_growth": "NY.GDP.MKTP.KD.ZG",
    "inflation": "FP.CPI.TOTL.ZG",
    "unemployment": "SL.UEM.TOTL.ZS",
    "gdp_per_capita": "NY.GDP.PCAP.CD",
    "population": "SP.POP.TOTL",
}

def get_indicator(countries: list[str], indicator: str, start: int, end: int) -> list[dict]:
    code = INDICATORS[indicator]
    url = f"{BASE}/country/{';'.join(countries)}/indicator/{code}"
    params = {"format": "json", "date": f"{start}:{end}", "per_page": 500}
    data = cached_get(url, params)          # SQLite cache around httpx.get(...).json()
    if len(data) < 2 or data[1] is None:    # error or empty: [{"message": [...]}]
        return [{"error": "no data", "detail": data[0]}]
    return [
        {"country": r["countryiso3code"], "name": r["country"]["value"],
         "year": int(r["date"]), "value": r["value"]}   # value may be None
        for r in data[1]
    ]
```

**The agent loop** (`agent.py`). This is the core of milestone 4, and the part interviewers will ask you to explain:

```python
import json
from openai import OpenAI
from .tools import TOOL_SCHEMAS, TOOL_FUNCS
from .prompts import SYSTEM_PROMPT

client = OpenAI(base_url="https://api.groq.com/openai/v1", api_key=GROQ_API_KEY)

def run_agent(user_msg: str, model: str, max_steps: int = 5) -> dict:
    messages = [{"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": user_msg}]
    tools_called = []
    for _ in range(max_steps):
        resp = client.chat.completions.create(
            model=model, messages=messages, tools=TOOL_SCHEMAS, temperature=0)
        msg = resp.choices[0].message
        if not msg.tool_calls:                      # model is done
            return {"answer": msg.content, "tools_called": tools_called}
        messages.append(msg)
        for call in msg.tool_calls:
            args = json.loads(call.function.arguments)
            result = TOOL_FUNCS[call.function.name](**args)
            tools_called.append({"name": call.function.name, "args": args})
            messages.append({"role": "tool", "tool_call_id": call.id,
                             "content": json.dumps(result)})
    return {"answer": "I couldn't finish that request.", "tools_called": tools_called}
```

**The first eval** (`evals/test_fast.py`), runs in CI against the cache:

```python
import json, re, pytest
from app.agent import run_agent

CASES = [json.loads(l) for l in open("evals/golden.jsonl")]
NUMBER = [c for c in CASES if c["type"] == "number"]
NO_DATA = [c for c in CASES if c["type"] == "no_data"]

def numbers_in(text: str) -> list[float]:
    return [float(n.replace(",", ".")) for n in re.findall(r"-?\d+(?:[.,]\d+)?", text)]

@pytest.mark.parametrize("case", NUMBER, ids=lambda c: c["id"])
def test_reports_correct_number(case):
    out = run_agent(case["input"], model=EVAL_MODEL)
    assert any(abs(n - case["expected_value"]) <= case["tolerance"]
               for n in numbers_in(out["answer"]))

@pytest.mark.parametrize("case", NO_DATA, ids=lambda c: c["id"])
def test_admits_missing_data(case):
    out = run_agent(case["input"], model=EVAL_MODEL)
    assert re.search(r"not available|no data|no hay datos|no disponible", out["answer"], re.I)
```

The number parser above is deliberately simple. It mixes up years and values, and thousands separators like "1,234.5". Improving it once real answers break it is part of the work worth mentioning in interviews.

On the KMP side, the change is small. Point the client at `/chat`, and add a `sources` list and a `toolsCalled` list to your message model so the UI can show them.

## How to present it

The README and one CV bullet are what recruiters see. Lead with measured results, not the list of tools.

**README order:** a one-line pitch, then a demo GIF (one English and one Spanish question), the architecture diagram, a "How it works" section (tools, RAG, loop), the eval results table, design decisions and trade-offs, data sources and licenses, how to run it, and what you'd do next.

**CV bullets** (fill in your real numbers after milestone 6):

- Built LatAm Economy Analyst, a bilingual (EN/ES) Compose Multiplatform assistant backed by a Python FastAPI service, combining World Bank API tool calls with RAG over Wikipedia in a hand-written agent loop on Groq-hosted LLMs.
- Designed an evaluation suite (30-case golden set with API-verified answers, pytest in GitHub Actions, LLM-as-judge for faithfulness), reaching \_\_% number accuracy and \_\_% recall@5, and raising tool-call accuracy from \_\_% to \_\_% through prompt and chunking changes.
- Added Langfuse tracing and per-request cost and latency tracking (p50 \_\_ s, \~$\_\_ per answer).

**Interview talking points**, so you have an answer ready for each:

- *How do you know the numbers are right?* Expected values come from the cached API, and number accuracy is checked in CI.
- *What happens when data is missing?* The `null` handling, the Venezuela and future-year eval cases, and the 100% no-data honesty target.
- *Why a multilingual embedding model?* Spanish questions need to find English passages; show the recall numbers with and without it.
- *Why no LangChain?* The loop is about 50 lines; owning it made debugging and evals easier. Mention the optional LangGraph port if you did it.
- *How would you scale it?* Move to pgvector, add more indicators and countries, schedule cache refreshes, and run evals as a release gate.
- *What did the evals catch?* Keep 2–3 real examples of a prompt change that broke something.
- *Mobile angle:* streaming UX, showing sources and numbers on a small screen, and keeping keys off the device.
