# API Contracts — API Lens

**Public API owner:** Spring Boot  
**Internal AI API owner:** FastAPI  
**Public prefix:** `/api/v1`  
**Internal contract:** `/internal/v1`  
**No user authentication in local MVP.** Add rate limiting and access controls before unrestricted public deployment.

## 1. Common conventions

- JSON response bodies use UTF-8.
- Upload uses `multipart/form-data`.
- All resource IDs are opaque random identifiers.
- Do not expose parser library classes directly.
- Product errors use:

```json
{
  "error": {
    "code": "INVALID_SPEC_FORMAT",
    "message": "The uploaded file is not a valid supported OpenAPI document.",
    "details": [{"field": "file", "message": "Unable to parse YAML."}],
    "requestId": "req-a1b2",
    "retryable": false
  }
}
```

- Standard status policy: `400` malformed request, `413` too large, `415` unsupported media/content, `422` unsupported version or invalid specification, `404` missing/expired session or operation, `429` rate limit, `502` internal AI service/provider failure, `504` timeout, `500` unexpected server error.
- Do not return stack traces, provider raw responses, keys, or uploaded content in error details.

## 2. Public endpoints

### `POST /api/v1/specifications`
**Purpose:** upload and parse a specification.  
**Request:** multipart field `file`.  
**Success:** `201 Created`.

Response:
```json
{
  "specId": "spec_opaque_random",
  "filename": "inventory-api.yaml",
  "openapiVersion": "3.1.0",
  "api": {"title": "Inventory API", "version": "1.0.0"},
  "summary": {"operationCount": 12, "warningCount": 3, "errorCount": 0},
  "expiresAt": "2026-10-23T08:00:00Z"
}
```
Validation: 1 MiB default max; `.json`, `.yaml`, `.yml` allowed as hints, but content must parse; reject unsupported versions. No AI call. Errors: 400, 413, 415, 422.

### `GET /api/v1/specifications/{specId}`
**Purpose:** retrieve metadata and parse diagnostics.  
**Success:** 200.

```json
{
  "specId": "spec_opaque_random",
  "openapiVersion": "3.1.0",
  "api": {"title": "Inventory API", "version": "1.0.0"},
  "diagnostics": [
    {"severity": "WARNING", "code": "MISSING_OPERATION_DESCRIPTION",
     "sourcePointer": "#/paths/~1items/get", "message": "Operation has no description."}
  ],
  "expiresAt": "2026-10-23T08:00:00Z"
}
```
Errors: 404 if absent/expired. No AI call.

### `GET /api/v1/specifications/{specId}/endpoints`
**Purpose:** list endpoint inventory with optional filters.  
Query: `search`, `method`, `tag`, `page` and `size` optional; default `page=0`, `size=50`, maximum `size=100`.  
Success: 200.

```json
{
  "items": [
    {
      "operationKey": "GET:/items/{itemId}",
      "method": "GET",
      "path": "/items/{itemId}",
      "operationId": "getItem",
      "summary": "Get an item",
      "tags": ["Items"],
      "deprecated": false,
      "missingDescription": true
    }
  ],
  "page": 0,
  "size": 50,
  "totalItems": 12
}
```
Errors: 404 unknown/expired spec. No AI call.

### `GET /api/v1/specifications/{specId}/endpoints/{operationKey}`
**Purpose:** show normalized source facts for one operation. URL-encode operation key or use a stable opaque operation ID if path encoding is awkward. Prefer `operationId` only as display metadata, not unique identity.  
Success: 200.

Response includes method/path, parameters, request body summary, all responses, effective security representation, schema summaries, and source pointers. Do not return a raw parser object. Errors: 404 unknown spec/operation.

### `POST /api/v1/specifications/{specId}/endpoints/{operationKey}/generate`
**Purpose:** generate explanatory documentation for selected operation.  
Request:
```json
{
  "includeCurl": true,
  "includeTestSuggestions": true,
  "style": "concise"
}
```
Success: 200.

```json
{
  "generationId": "gen_opaque",
  "operationKey": "GET:/items/{itemId}",
  "document": {
    "summary": "Retrieves an item by its identifier.",
    "markdown": "## GET /items/{itemId}\n\n...",
    "testSuggestions": [
      {"title": "Try an unknown identifier",
       "rationale": "The specification declares a 404 response.",
       "basis": "source"}
    ],
    "missingInformation": ["The specification does not provide an example itemId."]
  },
  "consistency": {
    "status": "WARNINGS",
    "warnings": [
      {"code": "SERVER_URL_PLACEHOLDER",
       "severity": "INFO",
       "message": "A placeholder base URL is used because no server URL is declared."}
    ]
  },
  "usage": {"inputTokens": 0, "outputTokens": 0, "reportedByProvider": false}
}
```
Usage numbers must be populated only if the provider returns them; otherwise omit or mark unavailable. The example zeros are illustrative only and must not be fabricated in a real response. This endpoint calls FastAPI and the external provider. Errors: 404, 429, 502, 504. Timeout: configure a bounded end-to-end deadline (initial target 30 seconds; tune after provider tests). One retry maximum for schema-invalid output, not for every provider failure.

### `GET /api/v1/specifications/{specId}/endpoints/{operationKey}/quality`
**Purpose:** return deterministic findings for source facts and generated content.  
Success: 200.

```json
{
  "status": "WARNINGS",
  "findings": [
    {"code": "UNKNOWN_RESPONSE_CODE", "severity": "ERROR",
     "message": "Generated content mentions 418, which is not declared for this operation.",
     "sourcePointer": "#/paths/~1items~1{itemId}/get/responses"}
  ]
}
```
No AI call. If quality findings are already returned by generation, this endpoint may re-run checks against the current draft or return the stored result. Keep one behavior consistent in implementation.

### `POST /api/v1/specifications/{specId}/endpoints/{operationKey}/export`
**Purpose:** export the current edited Markdown.  
Request:
```json
{
  "filename": "get-item-documentation.md",
  "markdown": "# GET /items/{itemId}\n\nUser-edited documentation..."
}
```
Success: `200 OK`, `Content-Type: text/markdown; charset=utf-8`, `Content-Disposition: attachment`. Validate size, sanitize filename and do not trust path characters. No AI call. Errors: 400, 404, 413.

This MVP does not persist arbitrary editing history; the client may hold edits and submit the current Markdown at export. If the server needs to keep edits during a session, store them in the bounded temporary session only.

### `GET /api/v1/health`
**Purpose:** health check for public backend.  
Success:
```json
{"status": "UP"}
```
No secrets or internal host details. A separate readiness check may verify FastAPI connectivity without calling the LLM.

## 3. Internal Spring Boot → FastAPI contract

### `POST /internal/v1/generate`
FastAPI must not be directly exposed to the browser. Protect internal endpoint using network isolation and, where deployment requires it, a service token.

Request:
```json
{
  "contractVersion": "1",
  "requestId": "req-a1b2",
  "operation": {
    "method": "GET",
    "path": "/items/{itemId}",
    "summary": "Get an item",
    "description": null,
    "parameters": [
      {"name": "itemId", "in": "path", "required": true,
       "schema": {"type": "string"}, "description": null,
       "sourcePointer": "#/paths/~1items~1{itemId}/get/parameters/0"}
    ],
    "requestBody": null,
    "responses": [
      {"statusCode": "200", "description": "Item found",
       "contentTypes": ["application/json"],
       "schemaSummary": {"type": "object", "properties": ["id", "name"]},
       "sourcePointer": "#/paths/~1items~1{itemId}/get/responses/200"},
      {"statusCode": "404", "description": "Item not found",
       "contentTypes": [], "schemaSummary": null,
       "sourcePointer": "#/paths/~1items~1{itemId}/get/responses/404"}
    ],
    "servers": [{"url": "https://api.example.test/v1", "sourcePointer": "#/servers/0"}],
    "security": [{"type": "http", "scheme": "bearer"}]
  },
  "options": {
    "includeCurl": true,
    "includeTestSuggestions": true,
    "style": "concise",
    "maxOutputTokens": 1800
  }
}
```

Response:
```json
{
  "contractVersion": "1",
  "requestId": "req-a1b2",
  "result": {
    "summary": "Retrieves an item by its identifier.",
    "purposeExplanation": "The operation is documented as retrieving an item.",
    "parameterExplanations": [
      {"name": "itemId", "location": "path",
       "explanation": "The item identifier in the URL.",
       "sourcePointer": "#/paths/~1items~1{itemId}/get/parameters/0"}
    ],
    "requestBodyExplanation": null,
    "responseExplanations": [
      {"statusCode": "200", "explanation": "The specification declares a successful response.",
       "sourcePointer": "#/paths/~1items~1{itemId}/get/responses/200"},
      {"statusCode": "404", "explanation": "The specification declares an item-not-found response.",
       "sourcePointer": "#/paths/~1items~1{itemId}/get/responses/404"}
    ],
    "curlExplanation": "Sends a GET request to the selected item path.",
    "testSuggestions": [
      {"title": "Try an unknown identifier",
       "rationale": "A 404 response is declared.",
       "expectedOutcome": "Verify the implementation matches the documented 404 response.",
       "basis": "source"}
    ],
    "missingInformation": ["No example itemId is provided."],
    "unverifiedSuggestions": []
  },
  "providerUsage": null
}
```

### Internal contract rules
- `contractVersion` is mandatory; breaking changes use `/internal/v2` and a matching contract.
- Pydantic rejects unknown fields and invalid types.
- Bound list sizes, strings, schema depth and total request bytes.
- The AI service treats all operation descriptions, examples and names as untrusted data.
- Provider SDK response objects never leave FastAPI.
- Provider errors map to safe internal error codes such as `PROVIDER_RATE_LIMIT`, `PROVIDER_TIMEOUT`, `INVALID_MODEL_OUTPUT`, `PROVIDER_UNAVAILABLE`.
- Spring Boot owns final consistency checks and Markdown rendering.
- Internal endpoint timeout is shorter than the outer public request deadline to leave time for mapping the error response.

## 4. API contract test checklist

- Valid upload and unsupported-version error.
- Invalid JSON/YAML and upload-too-large errors.
- Endpoint listing filters and pagination bounds.
- Unknown/expired spec and operation.
- Internal contract schema agreement across Java/Python.
- Provider timeout/rate limit/invalid structured output.
- Unknown response code and fabricated parameter detection.
- Export preserves edited Markdown and sanitizes filename.
- Health endpoints return no secrets.
