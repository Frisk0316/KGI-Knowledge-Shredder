# KGI Knowledge Shredder

Java/Spring Boot rebuild of the Knowledge Shredder prototype for financial micro-learning.

## Current Scope

This repository now contains the backend MVP plus a React operator console:

- Spring Boot 3.4.x, Maven, Java 21
- PostgreSQL schema managed by Flyway
- Apache Tika document extraction
- SHA-256 duplicate detection per trainer
- document versioning
- many-to-many financial domain taxonomy
- audit logging
- JWT-ready security boundary with a local dev trainer fallback
- Vite + React frontend for upload, AI pipeline tracking, RAG, learning feedback, audit, and incidents

## Run Requirements

- JDK 21
- Maven
- Docker Compose
- Node.js 20+ and npm for the frontend
- PostgreSQL with `pgcrypto` and `pgvector` available, provided locally by `docker-compose.yml`

Ubuntu 22.04 Java 21 setup:

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk
sudo update-alternatives --config java
sudo update-alternatives --config javac
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version
javac -version
mvn -version
```

Local defaults expect:

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/knowledge_shredder
DATABASE_USERNAME=kgi
DATABASE_PASSWORD=kgi
```

Copy the local env template if you want shell-exportable defaults:

```bash
cp .env.example .env
```

AI and security defaults:

```bash
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_STAGE1_MODEL=qwen2.5:7b
OPENAI_API_KEY=
OPENAI_STAGE2_MODEL=gpt-4o
KGI_DEV_TRAINER_ID=trainer_001
```

When `OPENAI_API_KEY` is blank or Ollama is unavailable, the MVP uses deterministic local fallbacks so development and tests do not require external model calls. For JWT-only mode, set `KGI_DEV_TRAINER_ID=` and provide `KGI_JWT_ISSUER_URI` or `KGI_JWT_JWK_SET_URI`.

Start local PostgreSQL/pgvector:

```bash
docker compose up -d postgres
docker compose ps
```

If your machine uses legacy Compose, use:

```bash
sudo docker-compose up -d postgres
sudo docker-compose ps
```

Start the app:

```bash
mvn spring-boot:run
```

Start the React frontend in a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173/
```

The Vite dev server proxies `/api` and `/actuator` to the Spring Boot backend on `http://localhost:8080`.

Dev role switching is header based and exposed in the React console:

- `learner_001`: admin/builder, can upload, replace sources, delete, reprocess, and view admin audit/incidents.
- `learner_002`: regular user, can view learner_001's generated learning files, run RAG, record learning attempts, mark read, leave comments, and submit change requests. Document mutation APIs return `403`.

For curl testing, add:

```bash
-H "X-Dev-User: learner_002"
```

Health and domain smoke tests:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/domains
```

Stop local PostgreSQL:

```bash
docker compose down
```

Legacy Compose:

```bash
sudo docker-compose down
```

Remove local PostgreSQL data volume:

```bash
docker compose down -v
```

Legacy Compose:

```bash
sudo docker-compose down -v
```

Upload smoke test:

```bash
curl -X POST http://localhost:8080/api/v1/documents/upload \
  -H "X-Dev-User: learner_001" \
  -F "file=@Project 1 The Knowledge Shredder Domain Taxonomy.docx" \
  -F domain_ids=3 \
  -F domain_ids=4
```

Repeated upload of the same file for the same trainer returns `409` with `existing_doc_id`.

Project 1 validation source files included in this repository:

- `Project 1 The Knowledge Shredder Domain Taxonomy.docx`: best fit for testing upload, multi-domain tagging, module generation, and split preview behavior.
- `The Forgetting Curve_Rethinking Knowledge in the Modern Financial Era.docx`: best fit for testing micro-learning, forgetting curve, spaced review, and RAG retrieval.

Replace polluted or updated source material and regenerate modules:

```bash
curl -X PUT "http://localhost:8080/api/v1/documents/{docId}/source" \
  -H "X-Dev-User: learner_001" \
  -F "file=@The Forgetting Curve_Rethinking Knowledge in the Modern Financial Era.docx" \
  -F domain_ids=3 \
  -F domain_ids=4

curl -X POST "http://localhost:8080/api/v1/documents/{docId}/reprocess" \
  -H "X-Dev-User: learner_001"
```

Learner feedback examples:

```bash
curl -X POST "http://localhost:8080/api/v1/documents/{docId}/feedback" \
  -H "X-Dev-User: learner_002" \
  -H "Content-Type: application/json" \
  -d '{"feedback_type":"READ_MARK"}'

curl -X POST "http://localhost:8080/api/v1/documents/{docId}/feedback" \
  -H "X-Dev-User: learner_002" \
  -H "Content-Type: application/json" \
  -d '{"feedback_type":"CHANGE_REQUEST","comment":"This source appears polluted; please replace it with the clean version."}'
```

P2-P5 APIs now include:

- `POST /api/v1/documents/{docId}/reprocess`
- `PUT /api/v1/documents/{docId}/source`
- `GET/POST /api/v1/documents/{docId}/feedback`
- `GET /api/v1/documents/{docId}/modules`
- `POST /api/v1/rag/query`
- `POST /api/v1/modules/{moduleId}/attempts`
- `GET /api/v1/learners/{learnerId}/reviews/due`
- `GET /api/v1/admin/audit-events`
- `GET /api/v1/admin/incidents`
- `PATCH /api/v1/admin/incidents/{incidentId}`

## Architecture Notes

See [docs/JAVA_REBUILD_IMPROVEMENT_PLAN.md](docs/JAVA_REBUILD_IMPROVEMENT_PLAN.md) for the improved Java rebuild plan based on the local project documents, the prior Flask prototype, and the review notes.
