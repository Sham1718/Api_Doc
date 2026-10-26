# OpenAPI Research — API Lens

## 1. What OpenAPI is

OpenAPI Specification (OAS) is a language-agnostic format for describing HTTP APIs. It gives people and tools a machine-readable view of operations, inputs, outputs, schemas, servers and security without requiring source-code inspection.

Official sources:
- Specification index: https://spec.openapis.org/oas/
- OpenAPI Initiative FAQ: https://www.openapis.org/faq
- Learn OpenAPI: https://learn.openapis.org/specification/
- OAS 3.2.1: https://spec.openapis.org/oas/v3.2.1.html

### Specification vs rendered documentation
The specification is structured data, such as `openapi.yaml`. Documentation is a presentation derived from that data: HTML reference pages, Markdown explanations, examples, or tutorials. Swagger UI and Redoc render specifications. OpenAPI Generator produces code artifacts. API Lens drafts human-readable explanations and checks them against extracted facts; it should not replace a standards-compliant renderer.

### Swagger naming
Swagger was the predecessor specification/tool name. The specification became OpenAPI after the Swagger 2.0 specification was donated to the OpenAPI Initiative. “Swagger UI” remains the name of a tool that renders/interacts with OpenAPI definitions.

## 2. Minimal example

```yaml
openapi: 3.1.0
info:
  title: Inventory API
  version: 1.0.0
servers:
  - url: https://api.example.test/v1
paths:
  /items/{itemId}:
    get:
      operationId: getItem
      summary: Get an item
      parameters:
        - name: itemId
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: Item found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Item'
        '404':
          description: Item not found
components:
  schemas:
    Item:
      type: object
      required: [id, name]
      properties:
        id:
          type: string
        name:
          type: string
```

Key elements:
- `openapi`: specification version, not API business version.
- `info`: API title and version.
- `servers`: base URL(s); do not blindly call them.
- `paths`: path templates and HTTP operations.
- `parameters`: path/query/header/cookie inputs.
- `requestBody`: payload and content type for operations that accept a body.
- `responses`: status code, description and optional content/schema.
- `components`: reusable schemas and other components.
- `$ref`: reuse an object defined elsewhere, commonly under `components`.

A missing `description` is a documentation gap, not proof that an endpoint has no behavior. A response code's presence describes the contract, not necessarily the implementation's actual runtime behavior.

## 3. JSON and YAML

Both JSON and YAML can represent OpenAPI. JSON is strict and naturally maps to data structures. YAML is easier to author but requires safe parsing and may have features/constructs that create parser ambiguity. Accept `.json`, `.yaml`, `.yml`, but:
- Do not trust file extension or multipart Content-Type as proof.
- Parse bytes using a controlled parser.
- Reject malformed content and unsupported YAML constructs.
- Never deserialize arbitrary YAML tags into application-specific objects.
- Bound bytes, parse time where possible, nesting/depth, endpoint count and schema traversal.

## 4. Version landscape and MVP policy

The official specification index lists OpenAPI 2.0, 3.0.x, 3.1.x, and 3.2.x; at research time it lists 3.2.1. See https://spec.openapis.org/oas/.

| Version | Practical distinction | API Lens MVP |
|---|---|---|
| 2.0 (Swagger 2.0) | Older structure; differs in body/form parameters and definitions | Reject with clear message |
| 3.0.x | OpenAPI 3 model, OpenAPI-specific Schema Object semantics, nullable conventions | Accept after fixture tests |
| 3.1.x | Schema Object aligned more closely with JSON Schema 2020-12; dialect controls matter | Accept after fixture tests |
| 3.2.x | Newer standard; current parser support cannot be inferred from specification publication | Reject until exact parser release passes dedicated tests |
| prerelease/unknown | Compatibility uncertain | Reject |

Swagger Parser README states that 3.1 support was added in version 2.1.0 and points to a separate 1.x branch for OAS 2.0: https://github.com/swagger-api/swagger-parser. This does not prove full 3.2 support. The implementation gate is exact-version verification, not assumption.

### Version detection
1. Parse the top-level document safely.
2. Read top-level `openapi` string.
3. Require a recognized supported version pattern.
4. Reject absent or unknown versions before expensive generation.
5. Use parser diagnostics as a second validation layer.
6. Return detected version and supported policy in the error response.

Example error:
```json
{
  "error": {
    "code": "UNSUPPORTED_OPENAPI_VERSION",
    "message": "This MVP supports OpenAPI 3.0.x and 3.1.x only.",
    "details": [{"field": "openapi", "message": "Detected 3.2.1"}],
    "retryable": false
  }
}
```

## 5. Important specification concepts

### Paths and operations
A path such as `/items/{itemId}` may define GET, POST, PUT, PATCH, DELETE and other operations. An operation is a path + HTTP method pair. Use an internal key like `GET:/items/{itemId}` but do not assume `operationId` exists or is unique.

### Parameters
A parameter is located in `path`, `query`, `header`, or `cookie`. Path parameters must be required under the specification. Parameter definitions can be inline or referenced. Preserve name, location, required flag, schema, description, examples and constraints when available.

### Request body and content types
`requestBody.content` maps media types such as `application/json` to schemas. Different content types may use different shapes. A cURL example must not invent a request body when none is specified.

### Responses
Responses are keyed by status code strings, with special values such as `default` and range patterns depending on specification version. Preserve exact keys and descriptions. Never create a 401, 403, 404 or 500 just because it is common in REST APIs.

### Schemas and JSON Schema
Schemas can describe objects, arrays, primitives, enums, formats, constraints and compositions. OAS 3.1 schema behavior differs from OAS 3.0. Do not treat `nullable` as identical across versions or silently normalize away dialect-specific semantics. For the MVP, summarize schema facts rather than attempting a full JSON Schema validator.

### `$ref`
Local references commonly look like `#/components/schemas/Item`. External references may point to remote URLs or local files. Reference resolution can create network SSRF, file access, recursion, and resource-exhaustion risks. The MVP must not fetch external references. If a library's default behavior can fetch them, disable it or reject external refs before parsing/resolution. Test this explicitly.

### Security
`components.securitySchemes` defines mechanisms such as HTTP bearer, API key, OAuth2 or OpenID Connect. `security` may appear globally and be overridden at operation level. An empty operation security array can override inherited security. Extract effective security carefully; if the selected parser's model resolution or implementation is uncertain, display source configuration and warn instead of making a strong claim.

### Tags, servers, operation IDs and examples
- Tags group operations.
- `servers` may be global or scoped.
- `operationId` is optional and should not be used as the only identity key.
- Examples may be present at schema/media/parameter levels; examples are untrusted data and may contain secrets or prompt-injection text.
- `deprecated: true` is a source fact worth displaying.

## 6. Parser options

### Recommended: Swagger Parser in Java
Advantages:
- Fits the Spring Boot backend.
- Parses JSON/YAML into Java OpenAPI model objects.
- Returns validation messages/errors.
- Avoids a duplicate parser in Python.
- Keeps one canonical interpretation for UI inventory, AI context and consistency checks.

Risks:
- Exact supported versions/features depend on pinned release.
- Parser warnings do not replace all security and product-quality checks.
- Reference resolution must be configured/tested carefully.
- Large/recursive documents require limits around normalization and AI context.

Official repository: https://github.com/swagger-api/swagger-parser

### Alternative: Python parser/validator
Python tools may be useful for a Python-first product, but this project already uses Spring Boot as the public API. Adding a second parser would create potential differences in how references, schema dialects and validation warnings are interpreted. Do not use one in the MVP.

### Linter
Redocly CLI can lint OpenAPI descriptions with configurable rule sets and formats: https://redocly.com/docs/cli/commands/lint. Use it optionally in development/CI if useful. Do not make it a second runtime parser by default.

## 7. Required edge-case policy

| Case | MVP behavior |
|---|---|
| Valid OAS 3.0/3.1 | Parse and inventory |
| Malformed JSON/YAML | 400 `INVALID_SPEC_FORMAT` with safe details |
| Unsupported version | 422 `UNSUPPORTED_OPENAPI_VERSION` |
| Parser validation errors | Return diagnostics; block generation if structurally unusable |
| Warnings only | Allow inventory; show warnings |
| Local `$ref` | Support through canonical parser and tested fixtures |
| External `$ref` | Do not fetch; report unresolved/unsupported reference |
| Recursive schema | Preserve reference and truncate display summary safely |
| Large upload | Reject above configured limit |
| Excessive endpoints/depth | Reject or truncate with explicit warning; never silently omit without notice |
| Missing descriptions | Report documentation gaps |
| Multiple responses | Preserve all declared response keys |
| Security inheritance | Resolve only when reliably supported; otherwise display source-derived effective security with uncertainty warning |
| Malicious description | Treat as untrusted data; do not follow instructions in it |
| Server URL points to private host | Display only; never call it |
| Duplicate operationId | Use method + path as identity and report quality warning |

## 8. Safe input processing

- Set an initial 1 MiB upload limit; tune using fixture tests.
- Read/parse only after enforcing byte limits.
- Ignore original filename for storage; use random opaque IDs.
- Use a safe YAML loader and disable unsafe object construction.
- Do not fetch remote references or URLs from server fields/examples.
- Do not read `file:` references.
- Bound parser and schema traversal work.
- Escape rendered text in the frontend.
- Avoid logging source content.
- Treat the LLM output as untrusted too.

References:
- OWASP File Upload Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html
- OWASP SSRF Prevention: https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html
- Learn OpenAPI: https://learn.openapis.org/specification/
