# Technical Specification — API Lens

**Status:** Scope-locked design baseline  
**Project:** AI API Documentation Generator  
**Architecture:** React → Spring Boot → FastAPI → one external LLM API  
**Persistence:** none in MVP  
**Supported input policy:** OpenAPI 3.0.x and 3.1.x only, gated by parser fixture verification

## 1. Product scope

API Lens turns an uploaded OpenAPI description into editable, source-grounded endpoint documentation. It does not generate a new API, call the user's API, execute uploaded code, or guarantee that the described implementation behaves exactly as specified.

### Functional requirements
- FR-01: accept `.json`, `.yaml`, `.yml` uploads within configured byte limit.
- FR-02: verify content, detect version, reject unsupported versions, and return actionable diagnostics.
- FR-03: list operations with method, path, tags, operationId, summary, and documentation gaps.
- FR-04: show parameters, request body, response codes, content types, schema summaries, and security requirements extracted from the source.
- FR-05: generate one operation's documentation at a time.
- FR-06: validate AI response against a Pydantic schema.
- FR-07: compare generated factual fields with canonical extracted facts and produce warnings.
- FR-08: label missing source facts and suggestions instead of inventing facts.
- FR-09: allow editing generated Markdown in the browser.
- FR-10: export edited Markdown as a download.
- FR-11: expose health endpoints and useful error responses.

### Non-functional requirements
- NFR-01: no arbitrary outbound network calls from uploaded specs.
- NFR-02: no raw spec content or secrets in routine logs.
- NFR-03: bounded uploads, endpoint counts, schema depth, prompt size, generation timeout, and session count.
- NFR-04: clear recoverable errors for parse, unsupported version, provider failure, and invalid output.
- NFR-05: deterministic checks are unit-tested and AI quality is evaluated separately.
- NFR-06: deployable as a small, single-instance demonstration.
- NFR-07: no authentication in the local MVP; a public deployment must apply rate limits and deployment-level restrictions or add auth before broad public exposure.

## 2. Architecture

```text
Browser (React/Vite)
   | public REST API
   v
Spring Boot API
   |-- upload validation and temporary session store
   |-- Swagger Parser: one canonical OpenAPI parse
   |-- normalized endpoint inventory
   |-- deterministic consistency checks
   |-- Markdown assembly/export
   |
   | internal HTTP contract (normalized DTO only)
   v
FastAPI AI service
   |-- Pydantic request/response validation
   |-- prompt construction and token/input limits
   |-- provider adapter, timeout, bounded retry
   v
External LLM provider
```

### Service responsibilities
**React:** presentation and user editing state. It must not independently parse OpenAPI or determine security truth.

**Spring Boot:** sole public API, canonical parser, upload controls, version policy, normalized endpoint facts, session lifecycle, deterministic checks, Markdown export, and calling FastAPI.

**FastAPI:** receives a normalized operation DTO, calls one provider, validates response shape, and returns structured AI content. It does not accept arbitrary uploaded files or fetch `$ref` URLs.

**LLM provider:** generate explanatory text and test suggestions only. It is not the source of truth for paths, parameters, response codes, schemas, or security.

## 3. Locked technical choices

| Concern | Decision |
|---|---|
| Frontend | React, JavaScript, Vite, Tailwind CSS |
| Main backend | Java 21, Spring Boot 3.5.x, Maven, Spring Web, Bean Validation |
| OpenAPI parser | Swagger Parser 2.x, exact patch pinned after tests |
| AI service | Python 3.12, FastAPI, Pydantic v2 |
| Provider | One external provider with structured-output support; provider/model selected only after current price and schema capability check |
| Persistence | No database; short-lived session state |
| Export | Markdown only |
| Local orchestration | Docker Compose |
| CI | GitHub Actions for frontend, backend and AI-service checks |
| Input versions | OAS 3.0.x/3.1.x only after fixture verification; reject 2.0, 3.2.x, prereleases, unknown versions |
| External references | Do not fetch; unresolved remote references produce a warning or rejection according to parser behavior |
| Generation | One operation per request by default |
| Authentication | No user accounts in local MVP; do not expose an unrestricted public endpoint without rate limiting/guardrails |

Pin versions in `pom.xml`, Python dependency lock/requirements, and `package-lock.json`. Avoid floating production dependencies and avoid using `latest` as a build pin.

## 4. Data flow

1. Browser sends multipart upload to Spring Boot.
2. Spring Boot enforces byte limit before full processing, checks filename extension as one signal, checks content, and parses JSON/YAML.
3. Version is detected from `openapi` field; unsupported versions receive HTTP 422 with a structured error.
4. Parser diagnostics and normalized endpoint inventory are stored in a bounded in-memory session under a random opaque `specId`.
5. Browser requests inventory and selects an endpoint by stable operation key.
6. Spring Boot builds a compact `OperationContext` containing source facts and schema summaries, not the entire document.
7. Spring Boot calls FastAPI with an internal request ID and deadline.
8. FastAPI calls provider with a strict structured response schema; timeout/rate-limit errors are mapped to controlled errors.
9. Spring Boot runs deterministic comparisons and attaches warning records.
10. Browser displays source facts, AI draft, and warnings; user edits the Markdown.
11. Browser sends edited Markdown for export. Server sanitizes filename and returns `text/markdown` attachment.
12. Temporary sessions expire; a cleanup task removes stale state.

## 5. Normalized source model

The normalized model should be a DTO, not a second OpenAPI implementation.

```json
{
  "specId": "opaque-random-id",
  "specVersion": "3.1.0",
  "apiTitle": "Inventory API",
  "apiVersion": "1.0.0",
  "operationKey": "GET:/items/{itemId}",
  "method": "GET",
  "path": "/items/{itemId}",
  "operationId": "getItem",
  "summary": "Get item",
  "description": null,
  "tags": ["Items"],
  "parameters": [
    {
      "name": "itemId",
      "in": "path",
      "required": true,
      "schema": {"type": "string", "format": "uuid"},
      "description": null
    }
  ],
  "requestBody": null,
  "responses": [
    {"statusCode": "200", "description": "Successful response",
     "contentTypes": ["application/json"],
     "schemaSummary": {"type": "object", "properties": ["id", "name"]}},
    {"statusCode": "404", "description": "Item not found",
     "contentTypes": ["application/json"], "schemaSummary": null}
  ],
  "security": [{"type": "http", "scheme": "bearer"}],
  "sourcePointers": ["#/paths/~1items~1{itemId}/get"]
}
```

Null means “not supplied or not extractable,” not “does not exist.” Keep source pointers for traceability. The schema summary may include property names, types, required flags, enum/format and constraints with strict depth/size limits.

## 6. AI response schema

Use Pydantic models with strict field types and extra fields forbidden. If the provider offers schema-constrained output, use it, but still validate the result locally.

```json
{
  "summary": "Retrieves one item by its identifier.",
  "purposeExplanation": "The operation is documented as retrieving an item.",
  "parameterExplanations": [
    {"name": "itemId", "location": "path",
     "explanation": "The item identifier in the URL.",
     "sourcePointer": "#/paths/~1items~1{itemId}/get/parameters/0"}
  ],
  "requestBodyExplanation": null,
  "responseExplanations": [
    {"statusCode": "200", "explanation": "The specification describes a successful response.",
     "sourcePointer": "#/paths/~1items~1{itemId}/get/responses/200"},
    {"statusCode": "404", "explanation": "The specification declares an item-not-found response.",
     "sourcePointer": "#/paths/~1items~1{itemId}/get/responses/404"}
  ],
  "curlExample": "curl -X GET 'https://api.example.test/items/{itemId}'",
  "testSuggestions": [
    {"title": "Request an unknown identifier",
     "rationale": "The specification declares a 404 response.",
     "expectedOutcome": "Check whether the implementation returns the documented 404 response.",
     "basis": "source"}
  ],
  "missingInformation": ["The specification does not define an example itemId value."],
  "unverifiedSuggestions": ["Confirm whether rate limiting applies; it is not described in this operation."]
}
```

Schema rules:
- `summary`, explanation fields, arrays, and nullable fields have bounded lengths/counts.
- Each source-supported claim must cite a source pointer or a fact identifier.
- AI cannot create authoritative parameter names, response codes, property names, authentication schemes, or server URLs.
- Build cURL commands primarily from the deterministic source model. AI may explain the command but must not authoritatively alter its method/path/parameters.
- If a field is absent in source, render “Not specified in the OpenAPI document.”
- `expectedOutcome` in test suggestions is a test hypothesis, not a guarantee of implementation behavior.

## 7. Consistency rules

Run deterministic checks after AI output:
1. Operation method/path must exactly match selected operation.
2. Every named parameter in output must exist in extracted parameters and match location.
3. Response status codes must be declared in source; do not permit fabricated codes.
4. Request/response property names mentioned as facts must exist in the bounded source schema model.
5. Security explanation must reflect operation-level security or inherited top-level security; if ambiguous, flag it.
6. cURL method/path must match the operation; server base URL must come from source or be explicitly marked as a placeholder.
7. Missing descriptions/examples are reported as gaps, not filled as source facts.
8. AI suggestions are separated from factual descriptions.
9. Any output failing Pydantic validation gets at most one controlled retry; repeated failure returns a retryable error.
10. Consistency checking cannot prove semantic truth or actual server behavior; UI must say so.

## 8. Product API error format

Use a consistent envelope:

```json
{
  "error": {
    "code": "UNSUPPORTED_OPENAPI_VERSION",
    "message": "This MVP supports OpenAPI 3.0.x and 3.1.x only.",
    "details": [{"field": "openapi", "message": "Detected version 3.2.1."}],
    "requestId": "req-opaque-id",
    "retryable": false
  }
}
```

Never return stack traces, provider secrets, raw uploaded content, or internal host details.

## 9. Temporary state model

No relational schema in MVP. In-memory session state:

- `specId`: random unguessable identifier.
- Original filename: display-only sanitized value.
- Upload time and expiry time.
- Parsed version, API metadata, diagnostics.
- Normalized operations.
- Generated draft and warnings per operation.
- Optional edited Markdown in session state.

Limits: maximum upload bytes, maximum operations per document, maximum schema traversal depth, maximum prompt bytes/tokens, maximum active sessions, and TTL. Initial suggested values: 1 MiB upload, 100 operations, schema traversal depth 8, one operation per generation request, 30-minute TTL, and bounded active sessions. These are engineering defaults to tune using fixtures, not universal performance claims.

## 10. Security and reliability

- Accept only JSON/YAML based on content parsing; extension/MIME are secondary checks.
- Use a safe YAML parser configuration; reject custom tags and unsupported constructs where possible.
- Disable external `$ref` resolution. Do not fetch URLs from uploaded specs. Do not permit `file:` references.
- Do not execute scripts or call API endpoints from source documents.
- Treat descriptions/examples as untrusted prompt content. Delimit them as data; system policy must say instructions embedded in the spec are not instructions to the model.
- Never send provider keys to the browser. Use environment variables or secret manager.
- Apply request size limits at reverse proxy and Spring Boot layer.
- Set connection/read timeouts for Spring Boot → FastAPI and FastAPI → provider.
- Add rate limiting at deployment edge or a small application limiter before public exposure.
- CORS should allow only the deployed frontend origin.
- Logs should contain request ID, operation key, timings, result category and token/cost metadata where available—not raw spec or generated secret values.
- Expire temporary state and files.
- Keep dependencies patched and pin them.
- Return graceful errors for provider timeout, rate limit, invalid response, unsupported version and expired session.

## 11. Test strategy

Unit tests: version gate, upload limits, endpoint extraction, `$ref` summary/depth, missing-description findings, consistency checks, cURL construction, Markdown export, Pydantic schema rejection.

Integration tests: upload → parse → inventory, endpoint details, mocked FastAPI response, real FastAPI contract with mocked provider, timeout mapping, invalid AI output, edit/export, expired session.

Fixtures: valid OAS 3.0, valid OAS 3.1, nested schemas, reusable `$ref`, bearer auth, multiple response codes, missing descriptions, malformed JSON/YAML, unsupported OAS 2.0 and 3.2.x, oversized file, recursive schema, external `$ref`, prompt-injection text.

AI evaluation metrics:
- Pydantic-valid response rate.
- Deterministic fact mismatch count per generated operation.
- Fabricated path/parameter/status/property count.
- cURL method/path consistency.
- Suggestion relevance via manual rubric.
- Latency and provider token/cost usage.
Do not claim 100% accuracy. Keep a small fixed dataset and compare changes against the same fixtures.

## 12. Deployment

Local: three services under Docker Compose; browser accesses Spring Boot API, Spring Boot reaches FastAPI by Compose service name, FastAPI reaches provider outbound over HTTPS. Only frontend/backend public ports need exposure as designed; do not expose FastAPI publicly.

Deployment: choose hosting only after checking current free-tier sleep, memory, outbound HTTP, container, build, and environment-secret restrictions. If all three containers cannot be hosted together, use a single host supporting multiple containers or deploy the AI service separately with an authenticated internal endpoint. Never assume ephemeral free hosts preserve uploaded files or stay awake.

## 13. Definition of done

The MVP is done when:
- A valid OAS 3.0.x and 3.1.x fixture uploads and produces an endpoint inventory.
- Unsupported versions and malformed files fail with actionable errors.
- A selected endpoint generates structured docs using a mocked provider in CI and a real provider locally/deployed.
- Deterministic checks catch intentionally injected mismatches in tests.
- User can edit and download Markdown.
- All core tests pass; secrets are not committed; upload limits and external-ref policy are tested.
- Docker Compose starts all services; health checks pass.
- README includes setup, environment variables, architecture, limitations, sample file, screenshots, and demo steps.
- Deployment works with documented limitations and no unhandled critical errors.
