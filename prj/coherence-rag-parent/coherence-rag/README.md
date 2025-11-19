# Coherence RAG Core Framework

The core Oracle Coherence RAG framework providing foundational interfaces, implementations, and REST API endpoints for building RAG applications on top of Oracle Coherence.

## Overview

This module contains the main RAG framework implementation including:
- Core interfaces and abstract classes
- REST API controllers for external access
- Document processing and chunking
- Vector storage integration
- AI model integration
- Local ONNX model support
- Configuration management
- Utility classes

## Features

### 🔧 **Core Framework**
- `VectorStore` interface for pluggable vector storage
- `DocumentLoader` interface for document ingestion
- `ModelProvider` interface for AI model integration
- `ChatAssistant` main entry point for RAG operations

### 🌐 **REST API**
- Document upload and processing endpoints
- Vector similarity search API
- Configuration management endpoints
- Real-time streaming responses
- CORS support for web applications

### 📄 **Document Processing**
- Automatic document chunking with configurable parameters
- Metadata extraction and preservation
- Integration with LangChain4J document parsers
- Support for multiple document formats

### 🤖 **AI Model Integration**
- Abstract model suppliers with caching and pooling
- Local ONNX model support for on-premises deployment
- Connection pooling for efficient API usage
- Configurable model parameters

### ⚙️ **Configuration System**
- Type-safe configuration classes
- JSON serialization for persistence
- Default value handling
- Dynamic configuration updates

## Quick Start

### Dependencies

Add the core framework dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.oracle.coherence.ce</groupId>
    <artifactId>coherence-rag</artifactId>
    <version>25.09</version>
</dependency>
```

### Basic Usage

```java
// Initialize components
VectorStore vectorStore = new CoherenceVectorStore();
ModelProvider modelProvider = new OpenAiModelProvider();

// Create chat assistant
ChatAssistant assistant = new ChatAssistant(vectorStore, modelProvider);

// Load and process documents
DocumentLoader loader = new FileDocumentLoader();
Collection<Document> documents = loader.load("file:///path/to/documents");
assistant.ingest(documents);

// Chat with your documents
String response = assistant.chat("What is the main topic of the documents?");
```

### REST API Usage

Start the REST API server:

```bash
mvn exec:java -Dexec.mainClass="com.oracle.coherence.rag.api.Server"
```

Upload documents:
```bash
curl -X POST "http://localhost:8080/api/store/documents/upload" \
  -F "file=@document.pdf"
```

Search documents:
```bash
curl -X POST "http://localhost:8080/api/store/documents/search" \
  -H "Content-Type: application/json" \
  -d '{"query": "machine learning", "maxResults": 10}'
```

## Architecture

### Core Interfaces

```java
// Vector storage abstraction
public interface VectorStore {
    void store(Collection<DocumentChunk> chunks);
    Collection<DocumentChunk> search(String query, int maxResults, double minScore);
}

// Document loading abstraction
public interface DocumentLoader {
    Collection<Document> load(String uri);
}

// AI model abstraction
public interface ModelProvider {
    ChatModel getChatModel(String modelName);
    EmbeddingModel getEmbeddingModel(String modelName);
}
```

### REST API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/store/{storeName}/documents` | POST | Upload and process documents |
| `/api/store/{storeName}/search` | POST | Search for similar documents |
| `/api/store/{storeName}/config` | GET/PUT | Store configuration management |
| `/api/db/stores` | GET | List all document stores |
| `/api/config` | GET | Global configuration |
| `/api/scoring/rerank` | POST | Re-rank search results |

### Configuration

#### Store Configuration
```java
StoreConfig config = StoreConfig.builder()
    .chunkSize(1000)
    .chunkOverlap(200)
    .embeddingModel("text-embedding-ada-002")
    .vectorStore("coherence")
    .build();
```

#### Model Configuration
```java
// Local ONNX embedding model
EmbeddingModel embeddingModel = LocalOnnxEmbeddingModel.builder()
    .modelPath("models/all-MiniLM-L6-v2.onnx")
    .vocabularyPath("models/vocab.txt")
    .poolingMode(PoolingMode.MEAN)
    .build();

// Local ONNX scoring model
ScoringModel scoringModel = LocalOnnxScoringModel.builder()
    .modelPath("models/ms-marco-MiniLM-L-12-v2.onnx")
    .vocabularyPath("models/vocab.txt")
    .build();
```

## Local ONNX Models

The framework supports local ONNX models for embedding generation and document scoring:

### Supported Models
- **Embedding Models**: Convert text to vector embeddings
- **Scoring Models**: Score document relevance (cross-encoders)
- **Bi-Encoders**: BERT-based models for embedding generation
- **Cross-Encoders**: BERT-based models for document scoring

### GPU Support
```java
// Enable GPU execution
EmbeddingModel model = LocalOnnxEmbeddingModel.builder()
    .modelPath("models/model.onnx")
    .useGpu(true)
    .build();
```

### Pooling Strategies
- `MEAN` - Mean pooling (default)
- `MAX` - Max pooling
- `CLS` - Use CLS token embedding

## Document Processing

### Chunking Configuration
```java
StoreConfig config = StoreConfig.builder()
    .chunkSize(1000)           // Characters per chunk
    .chunkOverlap(200)         // Overlap between chunks
    .build();
```

### Supported Document Formats
- PDF documents
- Microsoft Word documents
- Plain text files
- HTML documents
- Markdown files

### Metadata Handling
```java
Document doc = Document.builder()
    .text("Document content")
    .metadata("title", "Document Title")
    .metadata("author", "Author Name")
    .metadata("category", "Technical Manual")
    .build();
```

## Configuration

### Environment Variables
```bash
# Model Configuration
export COHERENCE_RAG_EMBEDDING_MODEL="text-embedding-ada-002"
export COHERENCE_RAG_CHAT_MODEL="gpt-4"

# Vector Store Configuration
export COHERENCE_RAG_VECTOR_STORE="coherence"

# Document Processing
export COHERENCE_RAG_CHUNK_SIZE=1000
export COHERENCE_RAG_CHUNK_OVERLAP=200
```

### System Properties
```bash
# API Configuration
-Dcoherence.rag.api.port=8080
-Dcoherence.rag.api.host=localhost

# Processing Configuration
-Dcoherence.rag.processing.threads=4
-Dcoherence.rag.processing.batch.size=100

# Cache Configuration
-Dcoherence.rag.cache.size=10000
-Dcoherence.rag.cache.ttl=3600
```

## Performance Tuning

### Connection Pooling
```java
// Configure model connection pooling
ModelSupplier supplier = new ChatModelSupplier(modelProvider)
    .withPoolSize(10)
    .withTimeout(Duration.ofSeconds(30));
```

### Batch Processing
```java
// Process documents in batches
int batchSize = 100;
List<Document> documents = loadDocuments();
for (int i = 0; i < documents.size(); i += batchSize) {
    List<Document> batch = documents.subList(i, Math.min(i + batchSize, documents.size()));
    processBatch(batch);
}
```

### Caching Strategies
- **Document Cache**: Cache processed documents
- **Embedding Cache**: Cache generated embeddings
- **Model Cache**: Cache model instances
- **Configuration Cache**: Cache store configurations

## Monitoring and Observability

### Performance Metrics
```java
// Time operations
Timer timer = Timer.start();
performOperation();
long duration = timer.stop();
System.out.println("Operation took: " + duration + "ms");
```

### Logging Configuration
```properties
# Enable debug logging
com.oracle.coherence.rag.level=DEBUG

# Enable API request logging
com.oracle.coherence.rag.api.level=INFO
```

## Development

### Building
```bash
mvn clean install
```

### Running Tests
```bash
mvn test
```

### Generating Documentation
```bash
mvn javadoc:javadoc
```

## Integration Examples

### Custom Vector Store
```java
public class CustomVectorStore implements VectorStore {
    @Override
    public void store(Collection<DocumentChunk> chunks) {
        // Custom storage implementation
    }
    
    @Override
    public Collection<DocumentChunk> search(String query, int maxResults, double minScore) {
        // Custom search implementation
        return Collections.emptyList();
    }
}
```

### Custom Document Loader
```java
public class CustomDocumentLoader implements DocumentLoader {
    @Override
    public Collection<Document> load(String uri) {
        // Custom document loading logic
        return Collections.emptyList();
    }
}
```

### Custom Model Provider
```java
public class CustomModelProvider implements ModelProvider {
    @Override
    public ChatModel getChatModel(String modelName) {
        // Return custom chat model
        return new CustomChatModel();
    }
    
    @Override
    public EmbeddingModel getEmbeddingModel(String modelName) {
        // Return custom embedding model
        return new CustomEmbeddingModel();
    }
}
```

## Troubleshooting

### Common Issues

1. **OutOfMemoryError**: Increase heap size or reduce batch size
2. **Connection timeouts**: Increase model API timeouts
3. **Slow embeddings**: Consider using local ONNX models
4. **Large documents**: Adjust chunk size and overlap parameters

### Debug Mode
```bash
mvn exec:java -Dexec.mainClass="com.oracle.coherence.rag.ChatAssistant" \
  -Dcoherence.rag.debug=true
```

## License

This project is licensed under the Universal Permissive License (UPL) 1.0. 
