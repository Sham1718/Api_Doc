# API Lens — First Working Flow & Future Backlog

**Purpose:** Build one complete, demonstrable journey first: upload an OpenAPI file, parse it, show its endpoints, generate documentation through the Python AI service, and display the result in React. Return to the hardening backlog after this basic flow works.

## 1. First milestone: the end-to-end user journey

1. User opens API Lens.
2. User uploads an OpenAPI JSON or YAML file.
3. Spring Boot validates and parses the file.
4. Backend returns a unique `specId`, metadata, status, and expiry time.
5. Frontend displays the specification and endpoint list.
6. User selects an endpoint.
7. Backend provides structured source facts for that operation.
8. Spring Boot sends those facts to the Python FastAPI AI service.
9. AI service generates endpoint documentation.
10. Backend returns the generated Markdown.
11. React displays the documentation.

**Definition of done:** A user can complete this journey in the browser, from upload through reading AI-generated documentation. Postman tests are necessary during development, but they do not alone complete this milestone.

For the first demo, generate documentation for one selected endpoint. Batch generation, editing, export, and advanced quality checks can come later.

## 2. Architecture and responsibilities

| Component | Responsibility |
|---|---|
| React frontend | Upload, show status and metadata, list/select endpoints, display generated docs and expiry countdown. |
| Spring Boot backend | Own public APIs, validate uploads, parse OpenAPI, extract source facts, store temporary sessions, call AI service. |
| Swagger Parser | Parse uploaded JSON/YAML into an OpenAPI model. |
| OpenAPI extractor | Convert parser-specific models into API Lens-owned DTOs. |
| Python FastAPI AI service | Generate human-readable documentation from structured endpoint facts. It does not parse the original file. |
| In-memory store | Temporarily retain specification data and generated results keyed by `specId`. |

Flow:
- Upload: React → Spring Boot → Swagger Parser → OpenAPI model → extractor → application DTOs → temporary store.
- Browse: React → Spring Boot endpoint APIs → endpoint DTOs → React.
- Generate: React → Spring Boot → structured source facts → FastAPI → generated Markdown → Spring Boot → React.

**Boundary:** Spring Boot owns parsing and source facts. FastAPI owns AI generation. AI output must be grounded in the supplied facts and must not invent unsupported API behavior.

## 3. Implementation phases — follow this order

### Phase A — Finish the OpenAPI upload foundation

- [ ] Organize the `openapi` feature package: `controller`, `service`, `dto`, `parser`, `extractor`, `model`, `store`.
- [ ] Put Swagger Parser-specific logic behind a parser component; keep the service responsible for coordinating the workflow.
- [ ] Make upload return an upload-response DTO instead of `void`.
- [ ] Include `specId`, filename, format, parse status, basic metadata, endpoint count, and `expiresAt`.
- [ ] Store application-owned data, not the original `MultipartFile` request object.
- [ ] Give every upload a distinct UUID; verify one upload cannot overwrite another.
- [ ] Implement retrieval by `specId` and expiry checks.
- [ ] Test two independent uploads and malformed input.

**Exit check:** A valid upload returns a usable `specId`; multiple files remain separate; each can be retrieved by ID.

### Phase B — Extract endpoint information

- [ ] Extract each HTTP operation from the parsed OpenAPI model.
- [ ] Define a stable `operationKey`. Do not use path alone: one path can have GET, POST, PUT, DELETE, etc.
- [ ] Use method + path as a reliable basis; use `operationId` only with a fallback strategy.
- [ ] Create API Lens-owned endpoint/source-facts DTOs.
- [ ] Include method, path, summary, description, tags, parameters, request body and response facts as applicable.
- [ ] Implement the endpoint-list API.
- [ ] Implement the single-operation source-facts API.
- [ ] Test paths containing multiple HTTP methods.

**Exit check:** Postman can retrieve the endpoint list and one operation's source facts without exposing Swagger Parser model classes.

### Phase C — Connect Spring Boot to FastAPI

- [ ] Define the internal request/response contract between Spring Boot and FastAPI.
- [ ] Send structured facts for one operation, not the original uploaded file.
- [ ] Define the generated-document response shape (at minimum Markdown content).
- [ ] Create a Spring Boot AI client.
- [ ] Configure the AI service base URL outside source code.
- [ ] Set connection and response timeouts.
- [ ] Handle service unavailable, timeout, invalid response and generation failure.
- [ ] Use a mock/deterministic provider if the real model integration is not ready.
- [ ] Test FastAPI directly, then test Spring Boot → FastAPI through Postman.

**Exit check:** Calling the Spring Boot generate API invokes FastAPI and returns generated documentation.

### Phase D — Build the minimum React experience

- [ ] Create an OpenAPI JSON/YAML upload screen.
- [ ] Show upload progress, parse result and basic metadata.
- [ ] Retain the returned `specId` for subsequent requests.
- [ ] Display endpoints with method and path.
- [ ] Allow endpoint selection.
- [ ] Add a generate-documentation action.
- [ ] Display loading, success and error states.
- [ ] Render returned Markdown safely.
- [ ] Display the session expiry countdown.
- [ ] Verify the complete journey in the browser.

**Exit check:** A user can upload, select an endpoint, generate its documentation and read it in the browser.

### Phase E — Freeze the first milestone

- [ ] Run backend, AI service and frontend together.
- [ ] Test valid OpenAPI fixtures and malformed input.
- [ ] Test multiple endpoints and multiple methods on one path.
- [ ] Record known limitations.
- [ ] Commit/tag the working milestone before hardening.

## 4. Temporary session and expiry policy

Initial MVP policy:
- [ ] Each upload receives a UUID.
- [ ] Specification data and generated results are held temporarily in memory.
- [ ] Start with a configurable 30-minute lifetime; adjust after product testing.
- [ ] Backend returns `expiresAt`; frontend displays a countdown.
- [ ] Backend checks expiry on every retrieval/generation request.
- [ ] A periodic cleanup task removes expired entries even if nobody requests them again.
- [ ] Expiry/deletion removes the specification and its generated results together.
- [ ] Expired sessions return a clear expired/not-found response.

The frontend timer is display only. Backend expiry is authoritative. Do not rely on refresh/tab-close events to clean server memory. In-memory data is lost on backend restart and is not shared across multiple backend instances; acceptable for a single-instance MVP, but revisit before scaling.

## 5. Deferred hardening backlog — return after the first flow works

### A. Upload and parser security

- [ ] Enforce maximum upload size at server and application levels.
- [ ] Reject empty files and unsupported formats.
- [ ] Validate supported OpenAPI versions.
- [ ] Prevent automatic fetching of external `$ref` URLs; test remote and local reference behavior.
- [ ] Protect against oversized/deeply nested documents and parser resource exhaustion.
- [ ] Do not trust filename or content type.
- [ ] Do not log full uploaded specifications.
- [ ] Return safe validation errors; never expose stack traces.

### B. Temporary storage and resource limits

- [ ] Limit active sessions and memory/file size per session.
- [ ] Decide whether original file content is needed after parsing.
- [ ] Ensure generated docs are included in expiry cleanup.
- [ ] Clean up abandoned/partially processed uploads.
- [ ] Consider bounded cache or temporary file storage if memory use grows.
- [ ] Define restart behavior.
- [ ] Before multiple backend instances, introduce shared session storage or deliberately design sticky sessions.

### C. AI safety and reliability

- [ ] Treat descriptions, examples and schema text from uploaded files as untrusted input.
- [ ] Defend against prompt injection embedded in API descriptions/examples.
- [ ] Ground generated content in source facts.
- [ ] Do not invent authentication, constraints, side effects or response behavior.
- [ ] Validate AI response structure.
- [ ] Handle malformed, incomplete or excessively long output.
- [ ] Add request/response limits and timeouts.
- [ ] Add retries only for safe transient failures; avoid duplicate expensive generation.
- [ ] Add deterministic quality checks comparing generated claims with source facts.
- [ ] Keep provider credentials in environment variables or a secret manager; never commit keys.

### D. API and backend robustness

- [ ] Standardize error response DTOs and global exception handling.
- [ ] Validate path parameters and request bodies.
- [ ] Use appropriate HTTP status codes.
- [ ] Add request correlation IDs and structured logs.
- [ ] Do not expose internal exception details.
- [ ] Add unit tests for parser, extractor, store, expiry and quality rules.
- [ ] Add integration tests for upload → parse → retrieve → generate.
- [ ] Test AI timeout/unavailable cases.
- [ ] Document the API Lens backend APIs.

### E. Frontend safety and UX

- [ ] Sanitize Markdown before rendering HTML.
- [ ] Never render arbitrary AI-generated HTML without sanitization.
- [ ] Handle expired sessions and backend restarts gracefully.
- [ ] Prevent accidental duplicate generation requests.
- [ ] Add accessible loading/error feedback.
- [ ] Make download availability before expiry clear.

### F. Deployment and operations

- [ ] Containerize backend, AI service and frontend.
- [ ] Configure environment-specific service URLs.
- [ ] Keep secrets out of source, container images and Git history.
- [ ] Add health/readiness checks.
- [ ] Add CI for builds, tests and linting.
- [ ] Set container resource limits.
- [ ] Monitor failures, latency, memory and AI usage.
- [ ] Avoid logging uploaded content; define log retention.
- [ ] Test restart and temporary-session loss behavior.

### G. Later product features

- [ ] Generate docs for all endpoints in one action.
- [ ] Edit generated Markdown.
- [ ] Export/download Markdown.
- [ ] Show deterministic quality reports.
- [ ] Add regeneration/version handling within a temporary session.
- [ ] Consider PDF export after Markdown is stable.
- [ ] Consider accounts, persistent history or collaboration only if requirements justify them.

## 6. Initial public API sequence

| API | Purpose | Priority |
|---|---|---|
| `POST /api/v1/specifications` | Upload/parse; return `specId` and metadata. | First flow |
| `GET /api/v1/specifications/{specId}` | Get metadata and expiry. | First flow |
| `GET /api/v1/specifications/{specId}/endpoints` | List operations. | First flow |
| `GET /api/v1/specifications/{specId}/endpoints/{operationKey}` | Get source facts for one operation. | First flow |
| `POST /api/v1/specifications/{specId}/endpoints/{operationKey}/generate` | Generate docs through FastAPI. | First flow |
| `GET /api/v1/specifications/{specId}/endpoints/{operationKey}/quality` | Get deterministic findings. | After first generated doc works |

Add a Markdown download endpoint when generated Markdown is ready to download. Do not block the first display milestone on export. Quality checks can follow the first generated document.

## 7. Project rules

1. Finish one vertical slice before broadening architecture.
2. Spring Boot owns parsing and source facts; FastAPI owns AI generation.
3. Pass application DTOs across boundaries; do not expose Swagger Parser classes.
4. Do not add database, Redis, Kafka or authentication just because a production system might need them.
5. Do not create every future class/package before its feature is ready.
6. Test backend steps with Postman, then verify the whole journey in the browser.
7. Keep deferred risks visible and return to them after the first milestone.
8. Commit working milestones.
9. Fix immediately any issue that could expose the machine, secrets or user data; not every security task can safely wait.

## 8. Immediate next action

Continue Phase A: finish the upload response and temporary store lifecycle. Then implement endpoint extraction and the endpoint-list/source-facts APIs. Next define the Spring Boot ↔ FastAPI contract and generate documentation for one endpoint. Finally build the minimum React screens to demonstrate the complete journey.

**Do not start with PDF export, authentication, database persistence, Kubernetes or advanced monitoring. Get the core workflow working first, then harden it systematically.**
