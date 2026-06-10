# AI Code Reviewer — Spring Boot + Claude LLM + Google Colab NLP Pipeline

A production-ready code review engine for Enat Bank engineering teams.
Combines **Claude LLM** deep analysis with a **Google Colab NLP pipeline** running
KeyBERT, RoBERTa sentiment, BART zero-shot classification, and custom code smell detection.

---

## Architecture

```
POST /api/v1/code-review
         │
         ├──[CompletableFuture]──▶ ClaudeLlmService ──▶ Anthropic API
         │                            • Deep analysis
         │                            • Issue detection
         │                            • Refactoring
         │
         └──[CompletableFuture]──▶ NlpPipelineService ──▶ ngrok ──▶ Google Colab
                                       • KeyBERT keyphrases
                                       • Sentiment (RoBERTa)
                                       • Intent (BART MNLI)
                                       • Code smell rules
                                       • Design pattern detection
                                       • Cyclomatic complexity (lizard)
         │
         └──▶ Merge results ──▶ Audit log (PostgreSQL) ──▶ Response
```

---

## Quick Start

### Step 1: Start the Colab NLP Server

1. Open `colab/nlp_pipeline_server.ipynb` in [Google Colab](https://colab.research.google.com)
2. Set Runtime → T4 GPU
3. Run all cells in order
4. **Cell 5** prints your ngrok public URL, e.g.:
   ```
   nlp.pipeline.colab.base-url: https://abc123.ngrok-free.app
   ```
5. Paste this URL into your Spring Boot config (see Step 2)

### Step 2: Configure Spring Boot

Create `spring-boot/src/main/resources/application-local.yml`:

```yaml
claude:
  api:
    key: sk-ant-...   # your Anthropic API key

nlp:
  pipeline:
    colab:
      base-url: https://YOUR-NGROK-URL.ngrok-free.app

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/code_reviewer_db
    username: postgres
    password: postgres
```

Or use environment variables:
```bash
export CLAUDE_API_KEY=sk-ant-...
export COLAB_NLP_URL=https://YOUR-NGROK-URL.ngrok-free.app
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
```

### Step 3: Run Spring Boot

```bash
cd spring-boot
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

API is live at: `http://localhost:8080/api/v1`
Swagger UI:    `http://localhost:8080/api/v1/swagger-ui.html`

---

## API Usage

### Submit Code for Review

```bash
curl -X POST http://localhost:8080/api/v1/code-review \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public class UserService { @Autowired private UserRepository repo; public User findUser(String id) { return repo.findById(id).get(); } }",
    "language": "java",
    "context": "Banking service at Enat Bank",
    "enableNlpPipeline": true,
    "enableRefactoring": true,
    "reviewDepth": "DEEP"
  }'
```

### Response Structure

```json
{
  "reviewId": "uuid",
  "reviewedAt": "2025-06-10T10:30:00",
  "language": "java",
  "overallScore": 52,
  "grade": "C",
  "verdict": "Service has critical error handling gaps and security concerns.",
  "scores": {
    "readability": 70, "performance": 60,
    "security": 30, "maintainability": 55, "testability": 45
  },
  "summary": "...",
  "issues": [
    {
      "type": "critical",
      "title": "NoSuchElementException on empty Optional",
      "description": "Calling .get() without isPresent() check throws at runtime.",
      "line": "Line 4",
      "fix": "return repo.findById(id).orElseThrow(() -> new EntityNotFoundException(...));",
      "category": "logic"
    }
  ],
  "nlpAnalysis": {
    "complexity": "Low",
    "sentiment": "Neutral",
    "intent": "Data retrieval service",
    "keyphrases": ["user service", "find user", "repository"],
    "codeSmells": ["Empty catch block"],
    "designPatterns": ["Repository", "Dependency Inj."],
    "topics": ["Spring Boot", "JPA / Hibernate"],
    "cyclomaticComplexity": 2,
    "linesOfCode": 6,
    "commentDensityPercent": 0,
    "maintainabilityIndex": 82.5
  },
  "refactoredCode": "...",
  "refactorNotes": "Added Optional safety, proper exception, and @Slf4j logging.",
  "processingTimeMs": 4210,
  "modelUsed": "claude-opus-4-6"
}
```

### Get Statistics

```bash
curl http://localhost:8080/api/v1/code-review/stats
```

---

## Project Structure

```
ai-code-reviewer/
├── spring-boot/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/enatbank/codereviewer/
│       │   ├── AiCodeReviewerApplication.java
│       │   ├── config/
│       │   │   ├── AppConfig.java          # WebClient beans, OpenAPI
│       │   │   └── CorsConfig.java
│       │   ├── controller/
│       │   │   ├── CodeReviewController.java
│       │   │   └── GlobalExceptionHandler.java
│       │   ├── service/
│       │   │   ├── CodeReviewOrchestrator.java  # parallel pipeline
│       │   │   ├── ClaudeLlmService.java         # Anthropic API
│       │   │   └── NlpPipelineService.java       # Colab client
│       │   ├── nlp/
│       │   │   └── CodeMetricsCalculator.java    # local Java metrics
│       │   ├── model/
│       │   │   ├── CodeReviewAudit.java
│       │   │   └── CodeReviewAuditRepository.java
│       │   └── dto/
│       │       └── Dtos.java
│       └── resources/
│           └── application.yml
└── colab/
    └── nlp_pipeline_server.ipynb    # Google Colab NLP server
```

---

## Supported Languages

| Language   | Complexity | Smells | LLM Review | NLP |
|------------|-----------|--------|------------|-----|
| Java       | ✅        | ✅     | ✅         | ✅  |
| Python     | ✅        | ✅     | ✅         | ✅  |
| TypeScript | ✅        | ✅     | ✅         | ✅  |
| JavaScript | ✅        | ✅     | ✅         | ✅  |
| SQL        | ✅        | ✅     | ✅         | ✅  |
| Kotlin     | ✅        | ✅     | ✅         | ✅  |
| Go         | ✅        | ✅     | ✅         | ✅  |

---

## Environment Variables

| Variable          | Required | Description                        |
|-------------------|----------|------------------------------------|
| `CLAUDE_API_KEY`  | ✅       | Anthropic API key                  |
| `COLAB_NLP_URL`   | ⚠️       | ngrok URL from Colab (optional)   |
| `DB_USERNAME`     | ✅       | PostgreSQL username                |
| `DB_PASSWORD`     | ✅       | PostgreSQL password                |

---

## Build & Test

```bash
# Run tests (uses H2 in-memory — no Postgres needed)
mvn test

# Build fat JAR
mvn clean package -DskipTests

# Run JAR
java -jar target/ai-code-reviewer-1.0.0.jar
```

---

Built for **Enat Bank** engineering team | Spring Boot 3.2 | Java 17 | Claude claude-opus-4-6
