# Four-Week Implementation Plan — API Lens

## Execution rule

The first milestone is a complete vertical slice, not a beautiful frontend. Build the smallest path through upload, parse, select, mocked generation, quality findings, edit, and export. Replace the mock with a real provider only after contracts and deterministic behavior are tested.

## Week 1 — Skeleton, upload, parse, endpoint explorer

### Sessions 1–2: repository and environment
Tasks:
- Create monorepo folders from `FOLDER_STRUCTURE.md`.
- Pin Java 21, Spring Boot 3.5.x patch, Node version supported by Vite, Python 3.12 and dependencies.
- Add `.env.example`, `.gitignore`, baseline CI.
- Create health endpoints for Spring Boot and FastAPI.
- Add Dockerfiles and a minimal Compose file.

Acceptance:
- Each service starts independently.
- CI can build/test each empty service.
- No secrets committed.
- Health checks pass.

### Sessions 3–4: upload and version gate
Tasks:
- Implement multipart upload limit and safe content parsing.
- Implement supported-version policy (OAS 3.0.x/3.1.x only).
- Integrate pinned Swagger Parser and capture diagnostics.
- Add malformed, unsupported, too-large and external-ref fixtures.

Acceptance:
- Supported fixtures parse.
- OAS 2.0, 3.2.x and unknown versions fail with actionable messages.
- External URLs are never fetched.
- Tests cover invalid input and upload limits.

### Sessions 5–7: endpoint inventory
Tasks:
- Extract method/path, operationId, summary, tags, deprecated flag, parameters, request body, responses, security and source pointers.
- Build bounded schema summaries for inline and local `$ref` schemas.
- Add temporary session store with TTL and limits.
- Build simple React upload screen and endpoint list/detail.

Acceptance:
- User uploads a fixture and sees correct inventory and details.
- Multi-response operations and referenced schemas work.
- Expired sessions return 404.
- Frontend shows parse diagnostics.

**Drop if behind:** complex visual design, pagination beyond basic bounded listing. Do not drop version tests or input safety.

## Week 2 — AI contract and generation

### Sessions 8–9: contract-first AI service
Tasks:
- Define internal JSON contract and Pydantic request/response models.
- Implement mocked provider returning deterministic fixture output.
- Implement FastAPI validation and safe error mapping.
- Add Spring Boot HTTP client with connect/read timeouts.

Acceptance:
- Contract test passes without any paid API call.
- Invalid fields are rejected.
- Provider SDK objects do not leak into response.
- Request size/output size limits exist.

### Sessions 10–12: real LLM provider
Tasks:
- Compare current provider model pricing, structured-output support, region/availability, token limits and rate limits.
- Select one model; store key in environment variable.
- Use structured output where supported and Pydantic validation regardless.
- Create prompt builder that labels uploaded-spec text as untrusted data.
- Implement bounded retry for schema-invalid output and explicit timeout/rate-limit handling.

Acceptance:
- One real endpoint generates valid structured content.
- Missing source facts become explicit missing-information entries.
- Prompt injection fixture does not cause external actions or override rules.
- Mock provider remains available for CI.

### Sessions 13–14: docs and cURL
Tasks:
- Build deterministic cURL method/path and parameter/body representation.
- Generate explanatory sections for parameters and responses.
- Add missing-documentation findings and test suggestions.
- Display source facts separately from AI content.

Acceptance:
- No fabricated path, parameter or response status passes the checker.
- cURL path/method match source.
- Missing descriptions are not silently filled as facts.

**Drop if behind:** multi-endpoint batch generation. Keep one-endpoint generation stable.

## Week 3 — Consistency, editor, export, tests

### Sessions 15–17: consistency and quality report
Tasks:
- Implement deterministic comparison rules.
- Add warning categories, severity and source pointers.
- Add quality-report UI.
- Test deliberately corrupted model output.

Acceptance:
- Unit tests detect unknown response codes, parameters and schema property names.
- Suggestions are visibly distinguished from source facts.
- UI communicates limits of consistency checks.

### Sessions 18–19: editing and Markdown export
Tasks:
- Implement editable Markdown.
- Export current edited text as UTF-8 `.md`.
- Sanitize filename and enforce output size.
- Handle expired session and export errors.

Acceptance:
- Export contains edits made after generation.
- Filename cannot traverse directories.
- No PDF/HTML or revision-history scope appears.

### Sessions 20–21: integration tests
Tasks:
- Upload → parse → inventory integration tests.
- Spring Boot → mocked FastAPI tests.
- FastAPI → mocked provider tests.
- Failure-path tests for timeout, rate limit, invalid output and session expiry.
- Create small AI evaluation dataset and manual rubric.

Acceptance:
- Core workflow runs in CI with no paid provider call.
- Evaluation reports unsupported factual claims and cURL mismatch counts.
- No known high-severity security issue remains.

**Drop if behind:** numeric completeness score and usage/cost dashboard.

## Week 4 — Reliability, deploy, portfolio polish

### Sessions 22–24: hardening
Tasks:
- Verify upload limits, YAML safety, external reference rejection, logging redaction, CORS, provider key handling, timeouts and cleanup.
- Add tests for prompt-injection descriptions and oversized/deep documents.
- Add rate limiting or configure deployment edge limits.

Acceptance:
- Security checklist has evidence/tests, not just claims in README.
- API errors are consistent and do not expose secrets.
- Expired state is cleaned.

### Sessions 25–26: containers and deployment
Tasks:
- Run Compose from clean clone.
- Verify container health checks and service DNS.
- Research host limits for all services; deploy to a host that supports chosen architecture.
- If hosting cannot support all services, use a separate private/secured AI service rather than exposing it unrestricted.

Acceptance:
- Public frontend and API work end-to-end.
- Environment secrets configured outside repository.
- Document sleep/ephemeral-storage limitations if present.
- Demo does not require a local provider key from the evaluator if a controlled demo mode is available.

### Sessions 27–28: documentation and demo
Tasks:
- Finalize README, architecture diagram, screenshots, example fixture and demo flow.
- Add limitations and supported-version policy.
- Run full regression suite and verify fresh setup.
- Record a short demo if useful.

Acceptance:
- A recruiter can follow README and understand problem, architecture, differentiator, trade-offs and tests.
- A new clone can be started using documented commands.
- No unimplemented feature is presented as complete.

## Critical path

```text
Version gate + parser
       ↓
Normalized endpoint model
       ↓
Endpoint inventory UI
       ↓
AI contract with mock provider
       ↓
Real provider integration
       ↓
Deterministic consistency checks
       ↓
Editor + Markdown export
       ↓
Integration/security tests
       ↓
Deployment + README/demo
```

## Daily operating rule

At the start of each work session, choose one acceptance criterion. At the end, record: changed files, tests run, result, blocker, next task. Do not spend a full session researching a feature unless it blocks the current critical path.

## Contingency plan

If one week behind:
1. Keep upload, parser, one-endpoint generation, consistency checks and Markdown export.
2. Remove multi-endpoint generation, score, cost dashboard, extensive animations, advanced schema visualization.
3. Keep tests, upload limits, external-ref policy and secret handling.
4. Deploy a single-instance demo with clearly documented limitations.
5. Do not add a database or auth just to make it look production-like.
