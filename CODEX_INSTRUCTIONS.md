# Codex 修改指令 — KGI Knowledge Shredder Gap Fix

本文件列出 9 項修改任務。每項標明要動的檔案、具體改法與驗證方式。
修改完成後執行 `mvn compile` 確認零錯誤。

---

## 背景

本專案是 Spring Boot 3.4 / Java 21 微學習內容生成平台，依據三份需求文件（Domain Taxonomy docx、Forgetting Curve docx、JAVA_REBUILD_IMPROVEMENT_PLAN.md）驗證後發現以下 gap，需逐一修補。

---

## 任務 1：RAG 回答合成改用 LLM + 記錄 token 使用量

### 問題
`PgVectorRagRetrievalService.synthesizeAnswer()` 只是把 source excerpt 拼接在一起，不是真正的 LLM 回答。`QueryLog` 的 `tokenCountIn` / `tokenCountOut` 永遠是 null。

### 修改檔案
- `src/main/java/com/kgi/shredder/domain/QueryLog.java`
- `src/main/java/com/kgi/shredder/service/ai/PgVectorRagRetrievalService.java`

### 具體做法

#### QueryLog.java
在既有 constructor 之後新增一個 7 參數 constructor：
```java
public QueryLog(String trainerId, String queryText, List<Map<String, Object>> retrievedChunks,
                Integer latencyMs, String modelUsed, Integer tokenCountIn, Integer tokenCountOut) {
    this(trainerId, queryText, retrievedChunks, latencyMs, modelUsed);
    this.tokenCountIn = tokenCountIn;
    this.tokenCountOut = tokenCountOut;
}
```

#### PgVectorRagRetrievalService.java
1. 新增 import：`com.fasterxml.jackson.databind.JsonNode`、`org.springframework.beans.factory.annotation.Value`、`org.springframework.web.client.RestClient`
2. 新增三個 field：`RestClient openAiClient`、`String apiKey`、`String model`
3. Constructor 加入三個參數：`RestClient.Builder restClientBuilder`、`@Value("${spring.ai.openai.api-key:}") String apiKey`、`@Value("${spring.ai.openai.chat.options.model:gpt-4o}") String model`；在 constructor body 中初始化：
   ```java
   this.openAiClient = restClientBuilder.baseUrl("https://api.openai.com").build();
   this.apiKey = apiKey;
   this.model = model;
   ```
4. 新增一個 private record：
   ```java
   private record SynthesisResult(String answer, String modelUsed, Integer tokenCountIn, Integer tokenCountOut) {}
   ```
5. 將 `synthesizeAnswer` 回傳型別從 `String` 改為 `SynthesisResult`：
   - 若 sources 為空，回傳 `new SynthesisResult("No tenant-scoped source chunks were found for: " + query, "none", null, null)`
   - 將 sources 組成 `[Source 1] ... [Source 2] ...` 的 context 字串
   - 若 apiKey 為空，走 fallback（保持原本拼接邏輯，modelUsed = "fallback"，token = null）
   - 否則呼叫 `openAiClient.post().uri("/v1/chat/completions")` 傳入 system prompt：
     ```
     You are a financial knowledge assistant. Answer the user's question based ONLY on the provided source chunks.
     Be concise, accurate, and cite source numbers when possible. If the sources do not contain enough information, say so.
     ```
     user prompt：`"Question: " + query + "\n\nSources:\n" + context`
   - 從 response 的 `usage` 節點提取 `prompt_tokens` 和 `completion_tokens`
   - 任何 exception 走 fallback
6. `answer()` 方法中用 `SynthesisResult synthesis = synthesizeAnswer(query, sources)`，並改用 7 參數 QueryLog constructor 傳入 `synthesis.modelUsed`、`synthesis.tokenCountIn`、`synthesis.tokenCountOut`

---

## 任務 2：MicroModule 加入 domain_ids 欄位

### 問題
MicroModule 沒有 domain 關聯。`MicroModuleSet.MicroModuleItem` 的 `domainIds` 在 persistence 時被丟棄。

### 修改檔案
- 新增 `src/main/resources/db/migration/V7__module_domain_ids_and_lineage_entity.sql`
- `src/main/java/com/kgi/shredder/domain/MicroModule.java`
- `src/main/java/com/kgi/shredder/service/ai/MicroModulePersistenceService.java`
- `src/main/java/com/kgi/shredder/api/v1/dto/MicroModuleResponse.java`
- `src/main/java/com/kgi/shredder/api/v1/DocumentsController.java`

### 具體做法

#### V7 migration
```sql
ALTER TABLE micro_modules ADD COLUMN domain_ids JSONB NOT NULL DEFAULT '[]'::jsonb;
```

#### MicroModule.java
1. 新增 import：`java.util.List`、`org.hibernate.annotations.JdbcTypeCode`、`org.hibernate.type.SqlTypes`
2. 在 `validated` 欄位之後新增：
   ```java
   @JdbcTypeCode(SqlTypes.JSON)
   @Column(name = "domain_ids", nullable = false, columnDefinition = "jsonb")
   private List<Long> domainIds = List.of();
   ```
3. Constructor 最後加一個參數 `List<Long> domainIds`，body 中 `this.domainIds = domainIds == null ? List.of() : domainIds;`
4. 新增 getter `public List<Long> getDomainIds()`

#### MicroModulePersistenceService.java
1. 新增注入 `DocumentDomainMapRepository documentDomainMapRepository`
2. 在 `replaceModules()` 中，刪除舊 modules 後，從 `documentDomainMapRepository.findBySourceDocumentDocId(docId)` 取得文件層級的 domain IDs 作為 fallback
3. 在 map 每個 item 時，若 `item.domainIds()` 非空就用它，否則用文件層級的 domain IDs
4. `new MicroModule(...)` 第 10 個參數傳入 `domainIds`

#### MicroModuleResponse.java
在 `validated` 之後、`createdAt` 之前新增 `List<Long> domainIds` 欄位。

#### DocumentsController.java
`toModuleResponse()` 方法中新增 `module.getDomainIds()` 參數，放在 `module.isValidated()` 之後、`module.getCreatedAt()` 之前。

---

## 任務 3：Stage 2 prompt 加入領域語調指引

### 問題
system prompt 太泛，沒有根據 domain 調整語調。Domain Taxonomy docx 明確要求 prompt 要強調 domain 相關的語調。

### 修改檔案
- `src/main/java/com/kgi/shredder/service/ai/OpenAiStage2GenerationService.java`

### 具體做法
1. `systemPrompt()` 改為接受 `List<String> domainNames` 參數
2. 將 domain 名稱注入 prompt 開頭：`You are creating training content for the domains of [CRM, LifeInsurance].`
3. 加入語調指引：
   ```
   Ensure the tone and emphasis match the target domains:
   - For CRM or client-facing domains, emphasize relationship building and service quality.
   - For Compliance or Regulatory domains, emphasize accuracy, legal obligations, and risk awareness.
   - For Insurance or Product domains, emphasize product features, benefits, and suitability.
   - For Wealth Management or Tax domains, emphasize planning strategies and client advisory.
   ```
4. 呼叫處改為 `systemPrompt(domainNames)`

---

## 任務 4：Stage 2 改用 Spring AI BeanOutputConverter

### 問題
改善計畫 P2 要求使用 Spring AI `BeanOutputConverter<MicroModuleSet>`，但實作用的是手動 JSON 解析。

### 修改檔案
- `src/main/java/com/kgi/shredder/service/ai/OpenAiStage2GenerationService.java`

### 具體做法
1. 移除 `ObjectMapper` field 和 constructor 參數（不再需要）
2. 新增 field：`private final BeanOutputConverter<MicroModuleSet> outputConverter;`，在 constructor 中 `this.outputConverter = new BeanOutputConverter<>(MicroModuleSet.class);`
3. Import `org.springframework.ai.converter.BeanOutputConverter`
4. `generateModules()` 中的 `objectMapper.readValue(content, MicroModuleSet.class)` 改為 `outputConverter.convert(content)`
5. `systemPrompt()` 中使用 `outputConverter.getFormat()` 取代手寫的 JSON schema 範例

---

## 任務 5：建立 DocumentLineage entity 並接入 replaceSource()

### 問題
V4 migration 建了 `document_lineage` table，但沒有對應的 JPA entity 或 repository。

### 新增檔案
- `src/main/java/com/kgi/shredder/domain/DocumentLineage.java`
- `src/main/java/com/kgi/shredder/repository/DocumentLineageRepository.java`

### 修改檔案
- `src/main/java/com/kgi/shredder/service/document/DocumentIngestionService.java`

### 具體做法

#### DocumentLineage.java
```java
@Entity @Table(name = "document_lineage")
// fields: lineageId (UUID PK), sourceDoc (ManyToOne SourceDocument), derivedDoc (ManyToOne SourceDocument),
//         relationshipType (String 80), createdAt (OffsetDateTime)
// constructor: (SourceDocument sourceDoc, SourceDocument derivedDoc, String relationshipType)
```

#### DocumentLineageRepository.java
```java
public interface DocumentLineageRepository extends JpaRepository<DocumentLineage, UUID> {}
```

#### DocumentIngestionService.java
1. 新增注入 `DocumentLineageRepository`
2. 在 `replaceSource()` 方法中，`sourceDocumentRepository.save(sourceDocument)` 之後新增：
   ```java
   documentLineageRepository.save(new DocumentLineage(sourceDocument, sourceDocument, "SOURCE_REPLACED"));
   ```

---

## 任務 6：刪除空的 RedactionTransformer

### 問題
`src/main/java/com/kgi/shredder/pipeline/RedactionTransformer.java` 是空 class，沒有被任何地方引用。

### 做法
刪除該檔案。若 `pipeline` 套件為空，一併刪除套件目錄。

---

## 任務 7：設定 Actuator readiness/liveness probes

### 修改檔案
- `src/main/resources/application.yml`
- `src/main/java/com/kgi/shredder/config/SecurityConfig.java`

### 具體做法

#### application.yml
將 `management` 區段改為：
```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
      show-details: when_authorized
  endpoints:
    web:
      exposure:
        include: health,info,metrics,readiness,liveness
```

#### SecurityConfig.java
將 `.requestMatchers("/actuator/health", "/actuator/info").permitAll()` 改為：
```java
.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
```

---

## 任務 8：實作 Data Retention 排程任務

### 新增檔案
- `src/main/java/com/kgi/shredder/service/retention/DataRetentionService.java`

### 修改檔案
- `src/main/java/com/kgi/shredder/KnowledgeShredderApplication.java`
- `src/main/java/com/kgi/shredder/repository/SourceDocumentRepository.java`

### 具體做法

#### KnowledgeShredderApplication.java
新增 `@EnableScheduling` 註解（import `org.springframework.scheduling.annotation.EnableScheduling`）。

#### SourceDocumentRepository.java
新增查詢方法：
```java
List<SourceDocument> findByDeletedTrueAndUpdatedAtBefore(OffsetDateTime cutoff);
```
（需要 import `java.time.OffsetDateTime` 和 `java.util.List`）

#### DataRetentionService.java
```java
@Service
public class DataRetentionService {
    // inject: SourceDocumentRepository, KgiProperties
    // @Scheduled(cron = "0 0 3 * * *") 每天凌晨 3 點
    // @Transactional
    // 計算 cutoff = now - retentionDays (from properties.retention().softDeleteRetentionDays())
    // 查詢 findByDeletedTrueAndUpdatedAtBefore(cutoff)
    // 若非空，log + deleteAll
}
```

---

## 任務 9：更新 JAVA_REBUILD_IMPROVEMENT_PLAN.md

### 修改檔案
- `docs/JAVA_REBUILD_IMPROVEMENT_PLAN.md`

### 具體做法
將 P2～P5 的狀態全部從「下一步 / Next」改為「此 repo 已完成 / Implemented in this repo」，並補充以下新完成項目：

- P2：加入「Stage 2 prompt 包含領域語調指引」、「micro_modules 含 domain_ids JSONB 欄位」、「使用 Spring AI BeanOutputConverter」
- P4：加入「LLM 驅動的 RAG 回答合成（附 fallback）」、「query_log 含 token 使用量追蹤」
- P5：加入「Actuator readiness/liveness probes」、「data retention 排程任務」、「DocumentLineage entity 追蹤文件替換關係」
- V1-V5 改為 V1-V7

---

## 驗證

所有修改完成後，執行：
```bash
mvn compile
mvn test
```
預期結果：
- compile 零錯誤
- 5 個 unit test 通過（FlywayMigrationTest 若無 Docker 會 skip，屬正常）
