# Folder Structure — API Lens

This tree is the fixed monorepo layout for the MVP. Do not add folders preemptively; create files when a responsibility is implemented.

```text
api-lens/
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── app/
│   │   │   ├── App.jsx
│   │   │   └── routes.jsx
│   │   ├── components/
│   │   │   ├── common/
│   │   │   └── layout/
│   │   ├── features/
│   │   │   ├── upload/
│   │   │   ├── endpoint-explorer/
│   │   │   ├── documentation-editor/
│   │   │   └── quality-report/
│   │   ├── services/
│   │   │   └── apiClient.js
│   │   ├── hooks/
│   │   ├── utils/
│   │   ├── styles/
│   │   └── main.jsx
│   ├── tests/
│   ├── Dockerfile
│   ├── package.json
│   ├── package-lock.json
│   ├── vite.config.js
│   └── tailwind.config.js
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/apilens/
│   │   │   │   ├── ApiLensApplication.java
│   │   │   │   ├── config/
│   │   │   │   ├── common/
│   │   │   │   │   ├── error/
│   │   │   │   │   └── web/
│   │   │   │   ├── upload/
│   │   │   │   ├── specification/
│   │   │   │   ├── endpoint/
│   │   │   │   ├── generation/
│   │   │   │   ├── quality/
│   │   │   │   ├── export/
│   │   │   │   └── session/
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/
│   │       └── java/com/apilens/
│   │           ├── upload/
│   │           ├── specification/
│   │           ├── endpoint/
│   │           ├── generation/
│   │           ├── quality/
│   │           └── export/
│   ├── pom.xml
│   └── Dockerfile
├── ai-service/
│   ├── app/
│   │   ├── main.py
│   │   ├── api/
│   │   │   ├── routes/
│   │   │   └── dependencies.py
│   │   ├── schemas/
│   │   │   ├── generation_request.py
│   │   │   └── generation_response.py
│   │   ├── services/
│   │   │   ├── generation_service.py
│   │   │   └── prompt_builder.py
│   │   ├── providers/
│   │   │   ├── base.py
│   │   │   └── selected_provider.py
│   │   └── core/
│   │       ├── config.py
│   │       └── errors.py
│   ├── tests/
│   │   ├── unit/
│   │   └── integration/
│   ├── Dockerfile
│   ├── pyproject.toml
│   └── requirements.lock
├── contracts/
│   ├── internal-ai/
│   │   └── generation-contract-v1.json
│   └── examples/
│       ├── valid-oas-3.0.yaml
│       ├── valid-oas-3.1.yaml
│       ├── nested-ref.yaml
│       ├── bearer-auth.yaml
│       ├── missing-descriptions.yaml
│       ├── malformed.yaml
│       ├── unsupported-3.2.yaml
│       ├── external-ref.yaml
│       └── prompt-injection-description.yaml
├── deployment/
│   ├── docker-compose.yml
│   └── nginx/
│       └── default.conf
├── docs/
│   ├── architecture/
│   │   └── system-overview.md
│   ├── research/
│   │   ├── PROJECT_RESEARCH.md
│   │   └── OPENAPI_RESEARCH.md
│   ├── decisions/
│   │   └── ARCHITECTURE_DECISIONS.md
│   └── api/
│       ├── TECHNICAL_SPECIFICATION.md
│       └── API_CONTRACTS.md
├── scripts/
│   ├── verify-environment.ps1
│   └── verify-environment.sh
├── .github/
│   └── workflows/
│       └── ci.yml
├── .env.example
├── .gitignore
├── README.md
├── FEATURE_SCOPE.md
├── IMPLEMENTATION_PLAN.md
└── FOLDER_STRUCTURE.md
```

## 1. Root-level responsibilities

- `frontend/`: browser UI only. It must not parse OpenAPI, hold provider secrets, or implement authoritative consistency checks.
- `backend/`: public API and canonical OpenAPI processing.
- `ai-service/`: model/provider integration and AI output schema validation. It must not independently parse uploaded OpenAPI files.
- `contracts/`: versioned internal service contract and deterministic test fixtures. Treat fixtures as tests, not production secrets.
- `deployment/`: local container orchestration and optional reverse-proxy config.
- `docs/`: architecture and product documentation. The eight requested deliverables are source-of-truth docs and should not be duplicated with conflicting copies.
- `scripts/`: small environment checks only; no hidden application logic.
- `.github/workflows/`: CI. Secrets are configured in repository settings, never committed.

## 2. Spring Boot package boundaries

Use feature-oriented packages because this is a small modular monolith. Each feature may have controller, service, and DTO classes internally. Do not create a global `controllers/`, `services/`, and `repositories/` package that mixes unrelated features.

- `upload/`: multipart controller, file limit and content checks.
- `specification/`: version policy, Swagger Parser adapter, parse result, diagnostics.
- `endpoint/`: endpoint inventory and operation detail projection.
- `generation/`: Spring-to-FastAPI client, generation orchestration and response mapping.
- `quality/`: deterministic checks and warning model.
- `export/`: Markdown renderer and download response.
- `session/`: in-memory bounded session store, TTL and expiry.
- `common/error/`: exception mapping and error envelope.
- `config/`: application configuration, HTTP clients, CORS, limits.

Dependency rule: controllers call feature services; services depend on parser/provider abstractions; features do not reach into another feature's private implementation. The canonical OpenAPI model stays inside backend parsing/extraction code. Return DTOs to the browser, never library-specific parser objects.

## 3. React organization

Use feature folders with local components and hooks:
- `features/upload/`: drop zone, upload state and validation result.
- `features/endpoint-explorer/`: search/filter list and endpoint detail view.
- `features/documentation-editor/`: generated sections, editable Markdown, export.
- `features/quality-report/`: warnings, source-supported facts and missing-information findings.
- `services/apiClient.js`: all HTTP calls and shared error decoding.
- `components/common/`: reusable button, badge, loading state, error panel.
- `utils/`: filename sanitization for display, formatting, constants; no security-critical server validation here.

Avoid Redux unless actual shared state complexity justifies it. React state/context is enough for the single workflow. Do not introduce a second API client per feature.

## 4. FastAPI organization

- `api/routes/`: HTTP routes only; validate input and call service layer.
- `schemas/`: Pydantic request and response models.
- `services/`: generation orchestration and prompt building.
- `providers/`: provider adapter and provider-specific SDK details.
- `core/config.py`: settings from environment variables.
- `core/errors.py`: controlled internal error types and safe mapping.

Dependency rule: routes → services → provider interface. Provider SDK response objects must not leak into the API response. FastAPI validates the AI request and response; Spring Boot owns uploaded-spec parsing and factual checks.


# write down in diary 
## 5. Naming and conventions

- Java packages: lowercase; classes `PascalCase`; methods/fields `camelCase`.
- REST paths: plural nouns where appropriate; kebab-case path segments; version public API under `/api/v1`.
- Python modules/functions: `snake_case`; Pydantic classes `PascalCase`.
- React components: `PascalCase.jsx`; hooks `useSomething.js`.
- Environment variables: uppercase snake case, e.g. `AI_SERVICE_URL`, `LLM_API_KEY`, `MAX_UPLOAD_BYTES`.
- Contract changes: update `contracts/internal-ai/generation-contract-v1.json`, both service models, and contract tests in the same change. Breaking contract changes require a new version.
- Test names should describe behavior, e.g. `rejectsOpenApi32Version`, `flagsUnknownResponseCode`.

## 6. What must not be added now

No `repository/` package without persistence, no `auth/` module without a real auth requirement, no `messaging/`, `queue/`, `cache/`, `vector/`, `agent/`, or `microservices/` directories. Do not create abstractions for hypothetical future providers beyond a tiny provider interface. One implementation is enough.

## 7. Folder acceptance criteria

- All source belongs to one of the three services.
- Only Spring Boot parses the uploaded OpenAPI document.
- Only FastAPI calls the LLM provider.
- Browser never sees provider secrets.
- Internal request/response schema is shared as a contract and tested.
- Fixtures are used by automated tests.
- Temporary state and cleanup are contained in `session/`.
- No empty placeholder folders are committed.
