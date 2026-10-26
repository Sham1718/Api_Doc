# Architecture Decision Records — API Lens

**Status:** Initial decisions locked unless explicitly marked “verify before implementation.”  
**Date:** 23 October 2026

## ADR-001 — Spring Boot is the public backend

**Context:** The project should reinforce Java backend skills and provide a stable public REST API.

**Options considered:** Spring Boot only; FastAPI only; React + Spring Boot + FastAPI.

**Decision:** React calls Spring Boot. Spring Boot owns public endpoints and orchestration. FastAPI is internal and AI-specific.

**Rationale:** Aligns with existing Java/Spring experience and keeps provider-specific Python logic isolated. The three-service option has extra deployment complexity, so FastAPI must remain narrow.

**Consequences:** Three services must be run/tested/deployed. Do not add more services. If AI service deployment proves to be the critical blocker, first simplify deployment rather than changing the core product.

**Evidence:** Spring Boot docs https://docs.spring.io/spring-boot/3.5/ ; FastAPI docs https://fastapi.tiangolo.com/

**Status:** Locked.

## ADR-002 — One canonical OpenAPI parser in Java

**Context:** Java and Python libraries could interpret references and schema details differently.

**Options considered:** Swagger Parser in Spring; Python parser in FastAPI; both parse raw spec.

**Decision:** Swagger Parser in Spring Boot is the only runtime parser. Python receives normalized operation DTOs only.

**Rationale:** Avoid duplicate parsing pipelines and inconsistent source facts.

**Consequences:** Parser behavior and version support must be tested in Java. Python tests validate only the AI contract.

**Evidence:** https://github.com/swagger-api/swagger-parser

**Status:** Locked; exact dependency patch must be verified against fixtures.

## ADR-003 — Accept OpenAPI 3.0.x and 3.1.x only

**Context:** Current OAS specification includes 3.2.1, but selected parser compatibility is not established for 3.2.

**Options considered:** Accept latest OAS versions; support 3.0/3.1; support only 3.0.

**Decision:** MVP supports 3.0.x and 3.1.x after pinned parser verification. Reject 2.0, 3.2.x and unknown versions with a clear error.

**Rationale:** A deliberately smaller compatibility promise is more credible than claiming support without evidence.

**Consequences:** Some uploads are rejected. Compatibility can be expanded later only with fixtures and parser evidence.

**Evidence:** OAS versions https://spec.openapis.org/oas/ ; parser README https://github.com/swagger-api/swagger-parser

**Status:** Locked; verify exact parser release.

## ADR-004 — No database in MVP

**Context:** Core workflow is temporary upload, generation, editing and export; history and accounts are not requirements.

**Options considered:** MySQL from day one; in-memory/temporary session; file-backed persistence.

**Decision:** Use bounded in-memory session state with TTL; optional temporary files only where necessary, outside webroot.

**Rationale:** Avoids schema, migrations, persistence deployment, authentication and cleanup complexity during four weeks.

**Consequences:** Restart loses sessions; one instance only; no saved projects. If a persistent file store is needed for parser handling, use random names and cleanup.

**Status:** Locked.

## ADR-005 — One external LLM provider with structured output

**Context:** Free-form text is hard to validate and can invent fields.

**Options considered:** Free-form prompt; structured JSON output; local model; multiple providers.

**Decision:** Choose one external provider with schema-constrained output if available; validate locally with Pydantic regardless. Keep a small provider adapter and a mocked implementation for tests.

**Rationale:** Minimizes implementation and API spend while making output contract testable.

**Consequences:** Provider/model may change; current price, limits and schema support must be checked at implementation time. Never depend on model output for authoritative API facts.

**Evidence (example provider capability; not a provider lock):** https://developers.openai.com/api/docs/guides/structured-outputs

**Status:** Architecture locked; provider/model requires a cost and capability check before implementation.

## ADR-006 — Deterministic consistency checks are separate from AI

**Context:** Prompting alone cannot guarantee the model never invents facts.

**Options considered:** Prompt-only grounding; model self-review; deterministic source comparison plus explicit warnings.

**Decision:** Run deterministic checks in Spring Boot against canonical extracted facts. Use AI for explanation and suggestions only.

**Rationale:** Path, method, parameter names, response codes and extracted schema property names are machine-checkable. Semantic correctness and actual runtime behavior are not fully provable from a spec.

**Consequences:** Checker must be narrow, explicit and tested. Do not claim it eliminates all hallucinations.

**Status:** Locked.

## ADR-007 — External references are not fetched

**Context:** Uploaded specifications can contain remote or local references that trigger SSRF or file access.

**Options considered:** Fetch all references; allowlist remote domains; disable remote references for MVP.

**Decision:** Do not resolve external URLs or `file:` references. Support tested local component references only.

**Rationale:** Avoids arbitrary outbound requests, DNS rebinding, internal-network access and local file disclosure. An allowlist alone is not a complete SSRF defense.

**Evidence:** OWASP SSRF guidance https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html

**Status:** Locked.

## ADR-008 — Markdown is the only export format

**Context:** Multiple output formats expand rendering, escaping and test requirements.

**Options considered:** Markdown, HTML, PDF, generated SDKs.

**Decision:** Export editable Markdown only.

**Rationale:** Simple, useful, portable, easy to diff and test.

**Consequences:** No PDF/HTML export in MVP; existing renderers remain available to users.

**Status:** Locked.

## ADR-009 — Generate one endpoint at a time

**Context:** Sending the whole specification can exceed context limits, increase cost and make attribution harder.

**Options considered:** Entire spec per call; batch all endpoints; operation-level generation.

**Decision:** Generate one operation per request by default.

**Rationale:** Bounded context, clear source pointers, easier retries, lower blast radius and straightforward evaluation.

**Consequences:** More calls for large APIs; optional batch generation is deferred.

**Status:** Locked.

## ADR-010 — Use feature-oriented package structure

**Context:** The backend is a small modular monolith with distinct upload, parsing, endpoint, generation, quality, export and session responsibilities.

**Options considered:** Global controller/service/repository layers; feature packages.

**Decision:** Feature-oriented packages with local controller/service/DTO boundaries.

**Rationale:** Keeps related logic together without pretending the project is a large enterprise system.

**Consequences:** Avoid creating abstractions for hypothetical future modules.

**Status:** Locked.

## ADR-011 — Version and dependency policy

**Context:** Latest patches and tool compatibility change over time.

**Decision:** Use Java 21, Spring Boot 3.5.x, Python 3.12, Pydantic v2, Node version supported by selected Vite, and pin exact dependency versions in build/lock files. Confirm current stable patches before coding.

**Rationale:** Java 21 is a conservative LTS baseline; Spring Boot 3.5 documentation specifies Java 17+ and Maven 3.6.3+; Vite documents current Node requirements in its guide.

**Evidence:**
- https://docs.spring.io/spring-boot/3.5/system-requirements.html
- https://vite.dev/guide/
- https://fastapi.tiangolo.com/

**Status:** Version families locked; exact patches to verify during setup.

## ADR-012 — Single-instance deployment first

**Context:** Four-week solo project, low budget and temporary state.

**Decision:** Deploy one instance per service or a single host supporting all containers. Do not promise horizontal scaling or durable sessions.

**Rationale:** Reduces infrastructure work and fits the demo goal.

**Consequences:** Hosting provider capabilities must be verified; free-tier behavior may include sleep or ephemeral storage. Public deployment requires rate limiting and internal AI service protection.

**Status:** Locked.
