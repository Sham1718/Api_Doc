# Feature Scope — API Lens

## 1. Scope-lock rule

The target is a complete, demonstrable MVP in four weeks. A feature is included only if it supports upload → parse → endpoint selection → generated draft → consistency review → edit → Markdown export. New feature requests go to the post-MVP list unless they fix a critical feasibility or security problem.

## 2. Feature classification

| # | Feature | Decision | Reason / acceptance condition |
|---:|---|---|---|
| 1 | OpenAPI specification validation | MVP must-have | Parse supported versions and show parser diagnostics |
| 2 | Endpoint inventory/filtering | MVP must-have | List method, path, tags and search/filter |
| 3 | Missing documentation detection | MVP must-have, lightweight | Flag missing operation/parameter/schema descriptions and examples using deterministic rules |
| 4 | AI endpoint descriptions | MVP must-have | Generate one endpoint at a time with source-aware uncertainty |
| 5 | Request/response explanations | MVP must-have | Explain only source-described inputs/outputs; missing data labeled |
| 6 | cURL examples | MVP must-have | Deterministically construct method/path and supported parameters; base URL only from spec or explicit placeholder |
| 7 | Error-response explanations | MVP must-have | Explain declared non-2xx responses only; do not invent common errors |
| 8 | Edge-case test suggestions | MVP must-have | Suggestions cite the source fact that motivates them and are labeled as hypotheses |
| 9 | Generated-content consistency checks | MVP must-have | Deterministically flag unknown method/path/parameter/status/property facts |
| 10 | Unsupported-claim warnings | MVP must-have | Surface factual mismatches and missing source evidence |
| 11 | Interactive documentation preview | MVP must-have, simple | Preview selected operation's facts and generated sections; no Swagger UI clone |
| 12 | Editing generated Markdown | MVP must-have | Editable text and preserve user changes until export/session expiry |
| 13 | Markdown export | MVP must-have | Download a `.md` file with sanitized name |
| 14 | OpenAPI quality report | MVP must-have, lightweight | Parser diagnostics + deterministic documentation gaps + consistency warnings |
| 15 | Breaking-change detection | Post-MVP | Requires comparison semantics and careful false-positive handling |
| 16 | Documentation completeness score | Optional only if ahead | Count-based heuristic only; do not imply semantic quality |
| 17 | AI usage/cost tracking | Optional only if ahead | Record provider usage metadata if returned; do not estimate as exact billing |
| 18 | Saved projects/version history | Post-MVP | Requires persistence, retention, project identity and potentially auth |
| 19 | API authentication examples | MVP limited | Explain declared security scheme; no secret storage or real token generation |
| 20 | Generated test skeletons | Post-MVP | Separate code generation and language/runtime choices expand scope |

## 3. Three differentiators

1. **Source-grounded generation:** send normalized facts for one operation, not the entire raw document.
2. **Deterministic consistency and uncertainty report:** verify factual fields that can be checked against extracted data; label missing or unverified information.
3. **Useful test suggestions:** suggest cases based on declared constraints, enum values, required fields, response codes and schema bounds. They are suggestions, not executed tests or proof of server behavior.

Missing-documentation analysis belongs in the MVP because it is mostly deterministic and makes the product useful even when the LLM is unavailable. A score is not a differentiator by itself and is optional.

## 4. Feature acceptance criteria

### Upload and validation
- Accept `.json`, `.yaml`, `.yml` within 1 MiB initial limit.
- Verify parseable content; do not trust extension/MIME alone.
- Accept OAS 3.0.x/3.1.x only after pinned parser fixtures pass.
- Reject OAS 2.0, 3.2.x, unknown and malformed documents with a useful error.
- Never fetch external references.

### Endpoint inventory
- Each operation identified by method + path.
- Search by method/path/summary/tag.
- Display parser warnings and missing documentation indicators.
- Show multiple response codes, parameters, request body, security and schema summaries when available.

### Generation
- Generate one endpoint per request by default.
- Validate model output against Pydantic schema.
- Bound prompt size and request timeout.
- On invalid output, at most one retry; then show recoverable error.
- Clearly label source facts, generated explanations, suggestions and missing details.

### Consistency
- Tests must prove the checker detects intentionally fabricated path, parameter, status code and property name.
- The checker must not mark suggestions as source facts.
- UI explains that checks cannot prove the actual API implementation matches the OpenAPI file.

### Editor/export
- User can edit generated Markdown without rerunning generation.
- Export contains current edited text.
- Filename is sanitized server-side.
- Export is UTF-8 Markdown; no PDF/HTML export in MVP.

## 5. Explicitly rejected for this release

- User registration/login and roles.
- Database and persistent history.
- GitHub/GitLab import/sync.
- Live API calls, test execution, or auto-generated credentials.
- Breaking-change comparison.
- PDF export.
- SDK generation and test-code generation.
- Arbitrary remote `$ref` resolution.
- Multi-agent workflows, RAG/vector database, Kafka, Redis, job queues.
- Full Swagger UI/Redoc renderer clone.
- Claims of guaranteed hallucination-free AI.

## 6. Scope-change procedure

A proposed addition is accepted only if all are true:
1. It fixes a demonstrated blocker or directly supports the core workflow.
2. It can be delivered and tested within the remaining time.
3. It does not require a new infrastructure dependency or major data model.
4. A lower-value feature is removed if the schedule is at risk.
5. The change is recorded in `ARCHITECTURE_DECISIONS.md`.

## 7. Release gate

Do not start optional features until one fixture completes the full upload → endpoint inventory → generation → consistency report → edit → Markdown download flow. If behind schedule, drop optional batch generation, score, usage tracking and UI polish—not tests, security limits, or export.
