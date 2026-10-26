# Project Research — AI API Documentation Generator

**Status:** Research baseline and scope recommendation  
**Project working name:** API Lens  
**Research date:** 23 October 2026  
**Purpose:** Decide what to build in four weeks, not begin implementation.

## 1. Executive recommendation

Build a single-user, temporary-session web application that accepts one OpenAPI JSON/YAML file, parses it once in Spring Boot, exposes an endpoint inventory, generates endpoint-level explanatory documentation through a Python AI service, checks output against deterministic facts extracted from the source, lets the user edit the result, and exports Markdown.

The differentiator is not “AI writes docs.” Existing renderers and generators already do useful work. The differentiator is a review workflow that combines:
1. **Source-grounded generation:** model receives a compact, structured representation extracted by the canonical parser.
2. **Deterministic consistency checks:** method, path, parameters, request-body media types, response codes, and schema property names are checked against source facts.
3. **Uncertainty and test suggestions:** missing facts are stated as unspecified, and suggested tests are explicitly recommendations rather than claims about actual server behavior.

**Recommendation:** use React + Vite + Tailwind, Spring Boot as the only public API and canonical parser, FastAPI as an internal AI-generation service, and no database in the MVP. Do not add accounts, Git integration, background jobs, Redis, Kafka, vector search, or automatic API execution.

## 2. Problem, users, and value

### Problem statement
An OpenAPI file is machine-readable, but it may have sparse descriptions, confusing schemas, or insufficient usage guidance. API consumers need to understand what an operation does, how to supply inputs, which responses are described, and what to test. Standard renderers expose the specification; they do not necessarily fill missing explanations safely.

### Target users
- Backend developers preparing API handoff documentation.
- Full-stack developers consuming an unfamiliar API.
- Small teams with an OpenAPI contract but limited technical-writing time.
- Students demonstrating practical AI integration and backend engineering.

### Primary user story
“As a developer, I want to upload an OpenAPI file and get editable, source-grounded endpoint documentation with warnings and test suggestions, so I can review and export it without manually explaining every endpoint.”

### Out of scope
Production API hosting, API testing against live services, API key vaults, team collaboration, OAuth login, repository integrations, SDK generation, breaking-change comparison, persistent workspaces, and model training.

## 3. Competitive landscape

| Tool | Main purpose / input | Strength | Do not rebuild | Potential gap this project addresses |
|---|---|---|---|---|
| Swagger UI | Interactive rendering of OpenAPI | Familiar, browsable API reference | Endpoint navigation and standard parameter/response rendering | Explain sparse descriptions, surface uncertainty, compare AI claims to source |
| Swagger Editor | Authoring and validating OpenAPI descriptions | Edit/preview specification workflow | Full OpenAPI editor | Focus on generated explanatory docs rather than editing the source contract |
| Redoc / Redocly CLI | API documentation rendering, linting, bundling and related workflows | Mature docs-as-code tooling and lint rules | Basic renderer, general lint engine, static HTML docs | Combine AI explanations with a source-fact audit and human editing |
| OpenAPI Generator | Generate clients, servers, and related artifacts from specs | Broad code-generation ecosystem | SDK/server code generation | Human-readable documentation and consistency review |
| OpenAPI validators/linters | Detect structural/specification or style issues | Deterministic, reproducible checks | Reimplementing complete standards validation | Use a parser's validation diagnostics plus narrowly scoped product-specific quality findings |
| AI documentation products | Varies by product; often generate summaries or documentation from API descriptions/code | Can reduce writing effort | Avoid claiming uniqueness without a dated product-by-product audit | Demonstrate explicit source traceability, deterministic checks, uncertainty, and eval results |

Sources:
- OpenAPI Initiative FAQ: https://www.openapis.org/faq
- OpenAPI specification index: https://spec.openapis.org/oas/
- Swagger UI repository: https://github.com/swagger-api/swagger-ui
- Swagger Parser repository: https://github.com/swagger-api/swagger-parser
- OpenAPI Generator: https://openapi-generator.tech/
- Redocly CLI: https://redocly.com/docs/cli/
- Redocly lint: https://redocly.com/docs/cli/commands/lint

**Product differentiation:** do not market this as a replacement for Swagger UI, Redoc, or OpenAPI Generator. Position it as an AI-assisted documentation drafting and review layer that produces editable Markdown and makes unsupported statements visible.

## 4. OpenAPI findings

OpenAPI is a language-agnostic interface description for HTTP APIs. It describes operations, parameters, request bodies, responses, schemas, servers, tags, examples, and security requirements. It allows tooling and people to understand an API without reading its implementation. An OpenAPI document is the structured source; rendered documentation is one presentation of it.

- **JSON vs YAML:** both can represent the document. JSON is strict and easy to parse; YAML is more author-friendly but requires careful parser configuration. Accept both; never trust extension or Content-Type alone.
- **Swagger vs OpenAPI:** Swagger was the name of the earlier specification/tool ecosystem. The specification was donated and became the OpenAPI Specification; Swagger UI and Swagger Editor remain product/tool names.
- **OpenAPI 2.0:** legacy Swagger format, with structural differences from OAS 3.x.
- **OpenAPI 3.0:** uses an OpenAPI-specific Schema Object model and nullable conventions that differ from modern JSON Schema.
- **OpenAPI 3.1:** aligns Schema Object behavior much more closely with JSON Schema 2020-12 and supports dialect controls.
- **OpenAPI 3.2:** the official index currently lists 3.2.0 and 3.2.1. A current specification does not prove a particular parser fully supports it.

Authoritative references:
- Specification versions: https://spec.openapis.org/oas/
- FAQ/history: https://www.openapis.org/faq
- Learn OpenAPI: https://learn.openapis.org/specification/
- OAS 3.2.1: https://spec.openapis.org/oas/v3.2.1.html

### `$ref` and schema extraction
`$ref` reuses definitions, commonly under `components/schemas`, `components/parameters`, `components/responses`, and `components/securitySchemes`. A correct extractor must handle inline and referenced schemas, nested objects, arrays, required properties, enums, constraints, and recursive references. Avoid recursively expanding every schema into an unbounded string. Preserve reference identity and use depth/item limits for display and AI context.

### Input compatibility decision
**MVP policy:** accept OpenAPI 3.0.x and 3.1.x only, after confirming the pinned Swagger Parser release with fixtures. Detect and clearly reject 2.0, 3.2.x, prerelease versions, and unknown versions with an actionable compatibility error. This is deliberately conservative: the official spec is newer than the parser compatibility evidence currently established in this research.

Swagger Parser's README says support for OpenAPI 3.1 was added in version 2.1.0 and distinguishes its 1.x branch for OpenAPI 2.0: https://github.com/swagger-api/swagger-parser. The README alone does not establish complete 3.2 support. Before coding, inspect the exact release notes, dependency version, issue tracker, and run acceptance fixtures. Do not silently advertise 3.2 support.

## 5. Parsing and validation recommendation

Use **Swagger Parser in Spring Boot as the one canonical parser**. It parses JSON/YAML into Java OpenAPI model objects and can return validation messages. The Spring Boot service should:
1. Read bounded upload bytes.
2. Identify format and OpenAPI version safely.
3. Parse using a pinned library version and safe options.
4. reject unsupported versions.
5. retain parser diagnostics and build a normalized endpoint inventory.
6. pass only a selected operation's normalized representation to FastAPI.

Do not parse the same source independently in Python. Duplicate parsers risk divergent interpretations of `$ref`, schema details, and versions. Python/Pydantic validates the *AI output contract*, not the uploaded OpenAPI specification.

A separate linter such as Redocly CLI may be used during research or CI as an optional cross-check, but do not add it as a runtime dependency unless a specific requirement emerges. Redocly CLI supports linting and configurable rules; it is not a reason to build a second runtime parsing pipeline: https://redocly.com/docs/cli/commands/lint.

## 6. Stack decision

| Layer | Decision | Reason | Trade-off |
|---|---|---|---|
| Frontend | React + JavaScript + Vite + Tailwind CSS | Familiar, fast local feedback, simple UI | Must still handle loading/error/empty states cleanly |
| Public backend | Java 21 + Spring Boot 3.5.x + Spring Web + Bean Validation | Uses user's strongest backend skills; mature REST structure | Pin exact patch versions in Maven and test compatibility |
| OpenAPI parsing | Swagger Parser 2.x line, exact release pinned after fixture verification | One canonical Java model and validation diagnostics | Do not assume OAS 3.2 support |
| AI service | Python 3.12 + FastAPI + Pydantic v2 | Keeps model/provider-specific code isolated | Adds service and deployment complexity |
| LLM | One external provider with structured-output support; choose exact model only after pricing, availability, region and schema support check | Reliable JSON contract is central | Provider cost/limits can change; mock it in tests |
| Persistence | None in MVP; temporary session data with TTL | Avoids schema/auth/history work | Session lost on restart and unsuitable for multiple replicas |
| Export | Server-side Markdown generated from validated structured output | Predictable and testable | PDF/HTML export deferred |
| Local infrastructure | Docker Compose for the three services; CI builds/tests | Reproducible demo | Public hosting may require separate services |
| CI | GitHub Actions: frontend build/test, Maven tests, Python tests, image build if feasible | Shows engineering quality | Keep workflow small |

Version policy: use a currently supported patch release within the chosen major/minor line, record it in lock/build files, and verify compatibility before implementation. Spring Boot 3.5 system requirements document Java 17+ and Maven 3.6.3+; Java 21 is a conservative LTS choice for the project: https://docs.spring.io/spring-boot/3.5/system-requirements.html. Vite's current guide lists Node.js 20.19+ or 22.12+: https://vite.dev/guide/. Verify exact current versions at setup rather than copying `latest` into production builds.

## 7. Database decision

**No MySQL in MVP.** The first release needs upload, parse, generate, edit, export—not accounts, saved projects, or cross-session history. Store session state in a bounded local temporary store keyed by an opaque random ID, with TTL cleanup. For a simple demo, an in-memory session map plus a strict size/count cap is acceptable; if raw files are written to disk, use generated filenames, a directory outside webroot, restrictive permissions, and cleanup. Do not trust client filenames.

This is a single-instance demonstration architecture. Restart loses sessions; horizontal replicas are unsupported. Add MySQL only after core workflow is deployed and if persistence becomes a real requirement.

## 8. Feature prioritization

### Locked MVP
1. JSON/YAML upload with size and compatibility checks.
2. Parser validation diagnostics.
3. Endpoint inventory, method/path filtering, and endpoint details.
4. Generate documentation for one endpoint at a time.
5. AI-generated explanation and test suggestions with explicit missing-information markers.
6. Deterministic consistency checks for paths, parameters, request-body property names, response codes, and schema fields that can be extracted reliably.
7. Warnings grouped as **source fact**, **AI explanation**, **suggestion**, and **unverified/unsupported**.
8. Edit generated Markdown in browser.
9. Export Markdown.
10. Health checks, graceful failures, tests, Docker Compose, README and demo.

### Optional only if ahead
- Generate selected endpoint docs in a small batch.
- Download a JSON quality report.
- Simple count-based documentation completeness score, clearly labeled as a heuristic.

### Defer
- Database, user accounts, saved history, collaboration.
- GitHub/GitLab integration.
- OpenAPI 2.0 and 3.2 input support until parser compatibility is proven.
- Breaking-change detection.
- PDF export, SDK/code generation, automatic live API calls, generated test execution.
- Redis, Kafka, vector database, queues, multi-agent orchestration.
- Arbitrary external `$ref` fetching.

## 9. Four-week feasibility and main risks

A four-week finish is realistic only if the end-to-end workflow is prioritized and scope is frozen. The greatest risks are:
- Parser compatibility being assumed rather than tested.
- LLM-generated facts being treated as trustworthy without deterministic checks.
- Spending too long on UI polish before upload-to-export works.
- Hosting all three services on a free tier with incompatible runtime/sleep/persistence limits.
- Expanding into history, auth, Git integration, or more output formats.

### Critical path
Upload → parser/version gate → endpoint inventory → normalized endpoint DTO → mocked AI contract → real LLM provider → deterministic consistency report → edit/export → tests/security → deployment.

## 10. Research uncertainty register

| Question | Current position | Verification required |
|---|---|---|
| Does the exact Swagger Parser version support all OAS 3.1 features needed? | Likely usable; not assumed complete | Pin release and run fixture suite |
| Does it fully support OAS 3.2? | Not established | Release notes + tests; reject until proven |
| Which LLM model/provider is best value? | Use one structured-output-capable provider | Check current pricing, rate limits, region and supported schema subset |
| Can the target host run all three services cheaply? | Depends on current plan limits | Verify sleep, memory, container, outbound-network and build limits at deployment time |
| How robust is consistency checking? | Deterministic for extracted facts, not semantic truth | Evaluation dataset and manual review |

## 11. References

- OpenAPI specification index: https://spec.openapis.org/oas/
- OpenAPI 3.2.1: https://spec.openapis.org/oas/v3.2.1.html
- OpenAPI Initiative FAQ: https://www.openapis.org/faq
- Learn OpenAPI: https://learn.openapis.org/specification/
- Swagger Parser: https://github.com/swagger-api/swagger-parser
- Swagger UI: https://github.com/swagger-api/swagger-ui
- OpenAPI Generator: https://openapi-generator.tech/
- Redocly CLI lint: https://redocly.com/docs/cli/commands/lint
- Spring Boot 3.5 requirements: https://docs.spring.io/spring-boot/3.5/system-requirements.html
- Vite guide: https://vite.dev/guide/
- FastAPI upload files: https://fastapi.tiangolo.com/tutorial/request-files/
- Structured Outputs (provider-specific example): https://developers.openai.com/api/docs/guides/structured-outputs
- OWASP file upload guidance: https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html
- OWASP SSRF guidance: https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html
