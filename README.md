# SimpleKnowledge + Elasticsearch 知识库服务

基于 [README-vES.md](../README-vES.md) 方案落地：`SimpleKnowledge` + **`ElasticsearchStore`**（默认），通用文档入库 + 向量检索 + RAG 对话。FAQ 为可选适配器。

| 项 | 值 |
|----|-----|
| 端口 | `8082` |
| 向量库 | `ElasticsearchStore`（`store-type=elasticsearch`） |
| ES 索引 | `agentscope_kb`（可配置） |
| AgentScope | `1.1.0-RC2` |

## 架构

```
POST /api/v1/kb/documents
    → IngestService（doc_id + 正文 + payload）
    → TextReader 分块 → SimpleKnowledge.addDocuments
    → DashScope embed → ElasticsearchStore（kNN 索引）

POST /api/v1/kb/retrieve  → knowledge.retrieve → ES kNN
POST /api/v1/kb/chat      → ReActAgent + GENERIC/AGENTIC

PUT/DELETE /documents/{docId}  → ElasticsearchDocMaintenance（delete_by_query）
```

向量库实现位于 AgentScope `VDBStoreBase` / `ElasticsearchStore`；本工程在 `StoreConfiguration` 装配 Bean，在 `ElasticsearchDocMaintenance` 补 `doc_id` 级删除。

## 前置条件

- JDK 17+
- 本地 Elasticsearch（示例 9.4.1，`http://localhost:9200`）
- DashScope API Key（Embedding + Chat）

```bash
docker run -d --name elasticsearch -p 9200:9200 \
  -e discovery.type=single-node \
  -e xpack.security.enabled=false \
  elasticsearch:9.4.1
```

## 启动

```bash
export DASHSCOPE_API_KEY=sk-your-key
export ES_URL=http://localhost:9200
export ES_INDEX=agentscope_kb
# 可选：启动时导入 FAQ
export FAQ_BOOTSTRAP=true

cd 03-simple-es-code
mvn spring-boot:run
```

- Swagger: http://localhost:8082/swagger-ui.html  
- 健康检查: http://localhost:8082/actuator/health  

## API

### 知识库 `/api/v1/kb`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/documents` | 入库 |
| PUT | `/documents/{docId}` | 覆盖更新（先删后写） |
| DELETE | `/documents/{docId}` | 按 doc_id 删除 chunk |
| POST | `/retrieve` | 向量检索 |
| POST | `/chat` | RAG 对话 |
| GET | `/status` | ES 状态与文档数 |

入库示例：

```bash
curl -s -X POST http://localhost:8082/api/v1/kb/documents \
  -H 'Content-Type: application/json' \
  -d '{
    "docId": "hr-leave-policy-v1",
    "title": "年假制度",
    "text": "员工入职满一年可享受带薪年假……",
    "payload": {"source": "wiki", "category": "HR"}
  }'
```

检索示例：

```bash
curl -s -X POST http://localhost:8082/api/v1/kb/retrieve \
  -H 'Content-Type: application/json' \
  -d '{"query": "年假可以结转吗", "limit": 3}'
```

### FAQ 兼容 `/api/v1/faq`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/status` | 同 KB 状态 |
| POST | `/reload` | 从 `classpath:faq/faq-items.json` 写入 ES（每条 faq-id 为 doc_id） |

## 配置要点

```yaml
agentscope.rag.simple.store-type: elasticsearch   # 或 memory（单测/无 ES）
agentscope.rag.simple.elasticsearch.url: http://localhost:9200
agentscope.rag.simple.elasticsearch.index-name: agentscope_kb
```

## 与 02-simple-kg-code 差异

| | 02 PoC | 03 ES |
|--|--------|--------|
| 向量库 | InMemoryStore | ElasticsearchStore |
| 数据 | 仅 FAQ JSON | 通用文本 + 可选 FAQ |
| 持久化 | 否 | 是 |
| 更新 | clear 全量 | delete_by_query + 按 doc 写入 |

## 参考

- [README-vES.md](../README-vES.md) — 方案与向量库落点说明  
- [03-es-ingest-sample](../03-es-ingest-sample/) — 独立 main 入库演示  
