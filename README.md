A platform that allows users to upload their documents, automatically index them in a vector database, and ask questions about the content using an LLM. Answers are generated only based on the uploaded documents (RAG — Retrieval-Augmented Generation), with references to sources.
Key features:
Registration/login (JWT), data isolation per user.
Document upload → parsing (Tika) → chunking → embedding (Ollama nomic-embed-text) → pgvector.
Semantic search (cosine similarity) within the user's documents.
Answer generation with SSE streaming (Ollama qwen3:4b-instruct).
Chat history, sources (chunkId, documentId, similarity, preview).
Observability: OpenTelemetry → Jaeger, JSON logs → Loki → Grafana, Prometheus metrics.
Architectural style: microservices, event-driven (Kafka), event-based saga (sagaId), API Gateway, Database-per-service.
