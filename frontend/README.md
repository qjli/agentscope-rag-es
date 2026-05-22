# RAG 运维前端

基于 `03-simple-es-code` Ops API 的 Dashboard / 文档管理 / 对话界面（Finexis 风格参考）。

## 开发

```bash
# 终端 1：后端
cd .. && mvn spring-boot:run

# 终端 2：前端（代理 /api → 8082）
npm install
npm run dev
```

访问 http://localhost:5173/ops/

## 生产构建

```bash
npm run build
```

构建产物在 `frontend/dist`，由 Spring Boot 挂载到 http://localhost:8082/ops/

## 功能

| 页面 | 说明 |
|------|------|
| Dashboard | ES chunk 数、doc_id 数、物料分布图 |
| 文档 | TextReader / WordReader / PdfReader 入库、按 doc_id 删除 |
| AI 对话 | 先选知识库，再 RAG 对话 |

## API 前缀

`/api/v1/ops/knowledge-bases/*`
