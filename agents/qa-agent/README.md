# qa-agent

A **Quality Assurance agentic workflow** that accepts a financial regulatory
reporting specification PDF and extracts every **reportable attribute** defined
within it.

Built with:

* [koog](https://github.com/JetBrains/koog) – JetBrains Kotlin AI agent framework
* [Google GenAI SDK](https://github.com/googleapis/java-genai) (`com.google.genai:google-genai`) for Vertex AI Gemini
* Google GenAI **file caching** for attaching PDFs to Gemini without text extraction
* Spring Boot for the REST API

---

## Architecture

```
HTTP POST /api/qa/analyze (multipart PDF)
         │
         ▼
  QaAgentController
         │
         ▼
  QaAgentService
    1. Write PDF to temp file
    2. Build koog AIAgent
    3. Run agent → Vertex AI Gemini (via GenAIVertexPromptExecutor)
         │
         ├─ Upload PDF → GenAI cached content (binary attachment)
         │
         └─ LLM analyses attached PDF → JSON array of ReportableAttribute
    4. Parse JSON → AnalyzeResponse
    5. Delete temp file
         │
         ▼
  JSON response to client
```

### Key components

| Class | Role |
|-------|------|
| `QaAgent.kt` | Builds the koog `AIAgent` with strategy and tool registry |
| `GenAIVertexPromptExecutor` | Bridges koog's `UnifiedLLMPromptExecutor` to the Google GenAI SDK and injects cached PDF content |
| `QaAgentService` | Orchestrates file handling, agent invocation, response parsing |
| `QaAgentController` | REST endpoint `/api/qa/analyze` |

---

## Prerequisites

| Requirement | Details |
|-------------|---------|
| Java 17+ | `java -version` |
| Gradle 8.5+ | wrapper included (`./gradlew`) |
| GCP project with Vertex AI enabled | [Vertex AI docs](https://cloud.google.com/vertex-ai/docs) |
| Application Default Credentials (ADC) | `gcloud auth application-default login` |

---

## Configuration

Set these values in `src/main/resources/application.properties` or as
environment variables:

| Property | Env variable | Default | Description |
|----------|-------------|---------|-------------|
| `vertex.ai.project` | `GOOGLE_CLOUD_PROJECT` | *(required)* | GCP project ID |
| `vertex.ai.location` | `GOOGLE_CLOUD_LOCATION` | `us-central1` | Vertex AI region |
| `vertex.ai.model` | `GEMINI_MODEL` | `gemini-2.0-flash` | Gemini model name |
### Database profiles

Persistence is profile-driven:

* **local** (default): in-memory H2 configured in `application-local.properties`
* **gcp**: PostgreSQL configuration in `application-gcp.properties` (override via `QA_DB_URL`, `QA_DB_USERNAME`, `QA_DB_PASSWORD`)

Activate a profile with `--spring.profiles.active=local` or `--spring.profiles.active=gcp`.

---

## Running

```bash
# 1. Set environment variables
export GOOGLE_CLOUD_PROJECT=my-gcp-project
export GOOGLE_CLOUD_LOCATION=us-central1
export GEMINI_MODEL=gemini-2.0-flash

# 2. Authenticate with Google Cloud
gcloud auth application-default login

# 3. Build and run
cd agents/qa-agent
./gradlew bootRun
```

The server starts on **http://localhost:8081**.

---

## API

### POST /api/qa/analyze

Upload a regulatory reporting specification PDF and receive extracted
reportable attributes.

Jurisdiction used for persistence is derived from the uploaded document name (filename without extension).

**Request**
```
POST /api/qa/analyze
Content-Type: multipart/form-data

file=<binary PDF>
```

**Example – cURL**
```bash
curl -X POST http://localhost:8081/api/qa/analyze \
     -F "file=@EMIR_Refit_RTS_Spec.pdf" \
     -H "Accept: application/json"
```

**Response 200 OK**
```json
{
  "documentName": "EMIR_Refit_RTS_Spec.pdf",
  "totalAttributes": 3,
  "mandatoryCount": 2,
  "optionalCount": 1,
  "attributes": [
    {
      "name": "trade_date",
      "description": "Date on which the transaction was executed",
      "dataType": "Date",
      "mandatory": true,
      "format": "ISO 8601 (YYYY-MM-DD)",
      "source": "Section 2.1"
    },
    {
      "name": "notional_amount",
      "description": "Notional or nominal amount of the contract",
      "dataType": "Decimal",
      "mandatory": true,
      "source": "Section 2.3"
    },
    {
      "name": "trade_id",
      "description": "Unique trade identifier assigned by the counterparty",
      "dataType": "String",
      "mandatory": false,
      "format": "UTI",
      "source": "Section 2.2"
    }
  ]
}
```

**Response 400 Bad Request** – returned when the uploaded file is not a PDF.

---

## Testing

```bash
cd agents/qa-agent
./gradlew test
```

Unit tests are in `src/test/kotlin/com/agents/qaagent/QaAgentServiceTest.kt` and
test the JSON parsing and response-assembly logic without requiring a live
Vertex AI endpoint.
