# KGI Knowledge Shredder Java 重構改善計畫 / Java Rebuild Improvement Plan

## 相較 Python 原型的調整 / Changes From The Python Prototype

上一版 Flask 原型已驗證上傳、領域標籤、批次生成，以及左右並排預覽的產品流程。商業級 Java 版本應保留這些產品概念，但在金融場景中要具備可信度，架構上必須做以下調整。

The previous Flask prototype proved the upload, domain tagging, batch generation, and side-by-side preview flow. The commercial Java version should keep those product ideas, but the architecture needs the following changes before it is credible for financial use.

1. Trainer 隔離必須來自 JWT claims，而不是 `X-Trainer-Id`、request body 欄位或 query string。  
   Trainer isolation must come from JWT claims, not `X-Trainer-Id`, request body fields, or query strings.

2. 原始文件、去識別化文字、生成模組、向量 chunks，以及驗證證據都必須分開版本控管。  
   Raw documents, redacted text, generated modules, vector chunks, and validation evidence must be separately versioned.

3. Domain tags 是核心的 many-to-many 分類字典，不只是 prompt 裡的提示文字。  
   Domain tags are a core many-to-many taxonomy, not prompt-only metadata.

4. Micro-learning 不能只被當成一般摘要。模組需要 sequence、閱讀時間、來源可追溯性、驗證分數、學員作答紀錄，以及 spaced repetition 狀態。  
   Micro-learning cannot be treated as a plain summary. Modules need sequence, reading time, source traceability, validation score, learner attempts, and spaced-repetition state.

5. PII 去識別化與文字壓縮應該在地端完成，再呼叫雲端 LLM。  
   PII redaction and text compression should happen locally before calling cloud LLMs.

6. RAG 必須用 `trainer_id` 做 tenant filter，並記錄 retrieved chunks、model、latency 與 token 使用量。  
   RAG must be tenant-filtered by `trainer_id` and must log retrieved chunks, model, latency, and token usage.

7. 檔案污染、分類錯誤或不良 AI 輸出應建立 incidents 與新版本，而不是靜默覆蓋既有紀錄。  
   File contamination, wrong classification, or bad AI output should create incidents and new versions instead of silently overwriting previous records.

## 對原 Java 計畫的改善 / Improvements To The Original Java Plan

### 上傳與切分 / Upload And Chunking

上傳階段應該用兩個層次分類文件。

The upload phase should classify each document at two levels.

- 使用者選擇的業務領域：`LifeInsurance`、`Compliance`、`CRM` 與其他 taxonomy tags。  
  User-selected business domains: `LifeInsurance`, `Compliance`, `CRM`, and the other taxonomy tags.

- AI 處理分類：`REGULATORY`、`PRODUCT_SPEC`、`PROCESS_GUIDE` 或 `OTHER`。  
  AI processing class: `REGULATORY`, `PRODUCT_SPEC`, `PROCESS_GUIDE`, or `OTHER`.

這樣系統才能區分「人壽法規」和「人壽銷售指南」的處理方式。法規文件應依 `第X條` 或 `Article X` 這類條文邊界切分；其他文件則使用 token 尺寸的語意 chunks，並保留中文標點。

This lets the system handle a life-insurance regulation differently from a life-insurance sales guide. A regulatory file should be chunked by article boundaries such as `第X條` or `Article X`; other files should use token-sized semantic chunks that preserve Chinese punctuation.

### 壓縮結果儲存 / Compression Storage

地端 Stage 1 的結果會存入 `document_versions.redacted_text`，並同時保留 `classification`、`processing_status` 與 audit events。這回答了「壓縮結果存在哪裡」的問題，也讓後續雲端呼叫能使用更小、更安全的內容，同時不失去 raw text lineage。

The local Stage 1 result is persisted in `document_versions.redacted_text` together with `classification`, `processing_status`, and audit events. This answers where compression lives and lets later cloud calls use a smaller, safer representation without losing raw-text lineage.

### Checkpoint 設計 / Checkpoint Design

雲端 Stage 2 會產生 `MicroModuleSet`。另一個獨立 GPT checkpoint model 會從下列面向驗證輸出。

Cloud Stage 2 produces `MicroModuleSet`. A separate GPT checkpoint model validates the output against the following dimensions.

- 是否有 source chunks 作為事實依據  
  factual grounding in source chunks

- 語調與 domain 是否匹配  
  tone and domain fit

- 是否有 PII 洩漏  
  PII leakage

- sequence 是否唯一  
  sequence uniqueness

- reading time 是否在合理範圍  
  reading-time bounds

只有 `overall_score >= kgi.checkpoint.min-score-to-pass` 的模組才會被標記為 validated。若驗證失敗，系統應保留 job evidence，並將 job 標為 failed 或進入人工審核。

Only modules with `overall_score >= kgi.checkpoint.min-score-to-pass` become validated. Failed validation should keep the job evidence and mark the job failed or require manual review.

### 文件異動與污染處理 / Document Mutation And Pollution

新版計畫加入以下設計。

The new plan includes the following design elements.

- `document_versions`：支援重新處理與來源更新  
  `document_versions`: supports reprocessing and source updates

- `source_documents` soft deletion：保留稽核軌跡，不直接刪除  
  `source_documents` soft deletion: keeps the audit trail instead of hard-deleting records

- `document_incidents`：記錄污染檔案、解析失敗或模型安全失敗  
  `document_incidents`: records contaminated files, extraction failures, or model-safety failures

- `document_lineage`：記錄替代上傳文件或衍生文件  
  `document_lineage`: records uploaded replacements or derived documents

這代表文件可以被修正，但不會破壞 audit trail。

This means a document can be corrected without destroying the audit trail.

### 學習回饋迴圈 / Learning Loop

兩份來源文件都強調 forgetting curve 與 flow-state learning，因此資料庫現在加入以下資料表。

The two source documents emphasize forgetting curves and flow-state learning, so the database now includes the following tables.

- `module_attempts`：記錄 quiz 或 module interaction events  
  `module_attempts`: records quiz or module interaction events

- `learner_memory_state`：支援 spaced repetition 排程  
  `learner_memory_state`: supports spaced-repetition scheduling

這讓平台不只是內容生成器，而是具備學習回饋能力的系統。

That makes the platform more than a content generator; it becomes a learning-feedback system.

## 實作階段 / Implementation Phases

### P1 基礎建設 / P1 Foundation

此 repo 已完成。Implemented in this repo.

- Maven / Spring Boot 3.4 / Java 21 專案骨架  
  Maven / Spring Boot 3.4 / Java 21 project skeleton

- PostgreSQL Flyway schema V1-V7  
  PostgreSQL Flyway schema V1-V7

- Tika 文件解析  
  Tika document parsing

- 依 `(trainer_id, content_hash)` 做 SHA-256 重複文件偵測  
  SHA-256 duplicate detection by `(trainer_id, content_hash)`

- source document 與 document version 持久化  
  source document and document version persistence

- domain many-to-many mapping  
  domain many-to-many mapping

- 從 security context 取得 trainer  
  trainer source from security context

- upload、list、detail、delete、reprocess job stub、domains 與 job polling APIs  
  upload, list, detail, delete, reprocess job stub, domains, and job polling APIs

- 針對 upload、version creation、duplicate rejection、soft delete 與 job queue 寫入 audit log  
  audit logging for upload, version creation, duplicate rejection, soft delete, and job queue

### P2 單階 AI / P2 Single-Stage AI

此 repo 已完成。Implemented in this repo.

- 實作 OpenAI `Stage2GenerationService`  
  implement OpenAI `Stage2GenerationService`

- 使用 Spring AI `BeanOutputConverter<MicroModuleSet>` 做型別安全的 JSON 解析  
  use Spring AI `BeanOutputConverter<MicroModuleSet>` for type-safe JSON parsing

- 持久化 `micro_modules`（含 domain_ids JSONB 欄位）  
  persist `micro_modules` (with domain_ids JSONB column)

- 加入 business validator，檢查 sequence 唯一性與 1-7 分鐘 reading time  
  add a business validator for unique sequence and 1-7 minute reading time

- Stage 2 prompt 包含領域語調指引  
  Stage 2 prompt includes domain-aware tone guidance

### P3 地端 Stage 1 與 Checkpoint / P3 Local Stage 1 And Checkpoint

此 repo 已完成。Implemented in this repo.

- 實作 Ollama `Stage1RedactionService`  
  implement Ollama `Stage1RedactionService`

- 持久化壓縮後的 redacted output  
  persist compressed redacted output

- 實作 GPT-based `CheckpointValidationService`  
  implement GPT-based `CheckpointValidationService`

- 將 `GenerationJobOrchestrator` 更新為完整狀態機  
  update `GenerationJobOrchestrator` into the full state machine

### P4 RAG

此 repo 已完成。Implemented in this repo.

- 持久化 `document_chunks`  
  persist `document_chunks`

- 將 embeddings 寫入 `document_embeddings`  
  write embeddings to `document_embeddings`

- 每次 vector query 強制套用 `trainer_id` 與可選的 `domain_ids` filter  
  force `trainer_id` and optional `domain_ids` filters in every vector query

- LLM 驅動的 RAG 回答合成（附 fallback）  
  LLM-powered RAG answer synthesis (with fallback)

- 回傳 sources 並寫入 `query_log`，包含 token 使用量  
  return sources and write `query_log` with token usage tracking

### P5 商業化強化 / P5 Commercial Hardening

此 repo 已完成。Implemented in this repo.

- 真正的 JWT issuer/JWK verification（含 dev mode fallback）  
  real JWT issuer/JWK verification (with dev mode fallback)

- Testcontainers PostgreSQL + pgvector 整合測試  
  Testcontainers PostgreSQL + pgvector integration tests

- Actuator readiness/liveness probes  
  Actuator readiness/liveness probes

- admin audit event 與 incident management API  
  admin audit event and incident management API

- data retention 排程任務與 incident workflow  
  data retention scheduled job and incident workflow

- `DocumentLineage` entity 追蹤文件替換關係  
  `DocumentLineage` entity tracking document replacement relationships
