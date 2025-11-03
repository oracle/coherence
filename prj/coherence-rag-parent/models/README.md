# AI Model Providers

AI model provider implementations for the Oracle Coherence RAG framework, enabling integration with various AI services and local models for chat and embedding functionality.

## Overview

The models module provides implementations of the `ModelProvider` interface for different AI services and local model deployments. Each provider handles authentication, model configuration, and API integration for both chat and embedding models.

## Available Providers

### Cloud AI Services

- **[OpenAI](coherence-rag-open-ai/)** - GPT models and text embeddings
- **[OCI GenAI](coherence-rag-oci/)** - Oracle Cloud Infrastructure GenAI service
- **[DeepSeek](coherence-rag-deepseek/)** - DeepSeek AI models for reasoning and coding

### Local Deployment

- **[Ollama](coherence-rag-ollama/)** - Local open-source models
- **Local ONNX** - On-premises ONNX model deployment (in core module)

## Features

### 🤖 **Chat Models**
- GPT-4 and GPT-3.5 Turbo (OpenAI)
- Cohere Command models (OCI GenAI)
- DeepSeek reasoning models
- Llama, Mistral, and other open-source models (Ollama)

### 🔤 **Embedding Models**
- OpenAI text embeddings (ada-002, text-embedding-3)
- Cohere embedding models (OCI GenAI)
- Local ONNX embedding models
- Sentence transformers via Ollama

### 🌊 **Streaming Support**
- Real-time token streaming for chat models
- Server-sent events (SSE) parsing
- Asynchronous response handling
- Comprehensive event types

### 🔐 **Authentication**
- API key authentication
- Service account integration
- Instance principals (OCI)
- Local deployment (no authentication required)

### ⚙️ **Configuration**
- Model-specific parameter tuning
- Connection pooling and caching
- Retry logic and error handling
- CDI integration

## Quick Start

### Dependencies

Add the specific model provider dependency to your `pom.xml`:

```xml
<!-- For OpenAI -->
<dependency>
    <groupId>com.oracle.coherence.ce</groupId>
    <artifactId>coherence-rag-open-ai</artifactId>
    <version>25.09</version>
</dependency>

<!-- For OCI GenAI -->
<dependency>
    <groupId>com.oracle.coherence.ce</groupId>
    <artifactId>coherence-rag-oci</artifactId>
    <version>25.09</version>
</dependency>

<!-- For Ollama -->
<dependency>
    <groupId>com.oracle.coherence.ce</groupId>
    <artifactId>coherence-rag-ollama</artifactId>
    <version>25.09</version>
</dependency>

<!-- For DeepSeek -->
<dependency>
    <groupId>com.oracle.coherence.ce</groupId>
    <artifactId>coherence-rag-deepseek</artifactId>
    <version>25.09</version>
</dependency>
```

### Basic Usage

```java
// OpenAI Models
ModelProvider openaiProvider = new OpenAiModelProvider();
ChatModel gpt4 = openaiProvider.getChatModel("gpt-4");
EmbeddingModel ada002 = openaiProvider.getEmbeddingModel("text-embedding-ada-002");

// OCI GenAI Models
ModelProvider ociProvider = new OciModelProvider();
ChatModel cohere = ociProvider.getChatModel("cohere.command");
EmbeddingModel embed = ociProvider.getEmbeddingModel("cohere.embed-english-v3.0");

// Ollama Local Models
ModelProvider ollamaProvider = new OllamaModelProvider();
ChatModel llama2 = ollamaProvider.getChatModel("llama2");

// DeepSeek Models
ModelProvider deepseekProvider = new DeepSeekModelProvider();
ChatModel deepseek = deepseekProvider.getChatModel("deepseek-v2");
```

## Supported Models

### OpenAI Models

| Model | Type | Description |
|-------|------|-------------|
| gpt-4 | Chat | Latest GPT-4 model |
| gpt-4-turbo | Chat | Optimized GPT-4 for speed |
| gpt-3.5-turbo | Chat | Cost-effective chat model |
| text-embedding-ada-002 | Embedding | Primary embedding model |
| text-embedding-3-small | Embedding | Efficient embedding model |
| text-embedding-3-large | Embedding | High-performance embedding |

### OCI GenAI Models

| Model | Type | Description |
|-------|------|-------------|
| cohere.command | Chat | Cohere Command model |
| cohere.command-light | Chat | Lightweight Command model |
| cohere.embed-english-v3.0 | Embedding | English embedding model |
| cohere.embed-multilingual-v3.0 | Embedding | Multilingual embedding model |

### Ollama Models

| Model | Type | Description |
|-------|------|-------------|
| llama2 | Chat | Meta's Llama 2 model |
| mistral | Chat | Mistral AI model |
| codellama | Chat | Code-specialized Llama |
| alpaca | Chat | Instruction-tuned model |
| vicuna | Chat | Conversation-optimized model |

### DeepSeek Models

| Model | Type | Description |
|-------|------|-------------|
| deepseek-v2 | Chat | Latest reasoning model |
| deepseek-coder | Chat | Code generation model |
| deepseek-math | Chat | Mathematical reasoning model |

## Authentication

### OpenAI Authentication

```bash
# Environment variable
export OPENAI_API_KEY="your-openai-api-key"

# System property
-Dopenai.api.key=your-openai-api-key
```

### OCI GenAI Authentication

```bash
# OCI config file (default: ~/.oci/config)
export OCI_CONFIG_FILE="~/.oci/config"
export OCI_CONFIG_PROFILE="DEFAULT"

# Or use instance principals on OCI compute instances
```

### DeepSeek Authentication

```bash
# Environment variable
export DEEPSEEK_API_KEY="your-deepseek-api-key"

# System property
-Ddeepseek.api.key=your-deepseek-api-key
```

### Ollama Configuration

```bash
# Ollama server URL (default: http://localhost:11434)
export OLLAMA_BASE_URL="http://localhost:11434"

# System property
-Dollama.base.url=http://localhost:11434
```

## Configuration

### Model Parameters

```java
// OpenAI configuration
ChatModel chatModel = openaiProvider.getChatModel("gpt-4")
    .withTemperature(0.7)
    .withMaxTokens(1000)
    .withTopP(0.9);

// OCI GenAI configuration
ChatModel cohereModel = ociProvider.getChatModel("cohere.command")
    .withTemperature(0.8)
    .withMaxTokens(500)
    .withFrequencyPenalty(0.1);
```

### Connection Pooling

```java
// Configure connection pooling
ModelProvider provider = new OpenAiModelProvider()
    .withConnectionPoolSize(10)
    .withConnectionTimeout(Duration.ofSeconds(30))
    .withReadTimeout(Duration.ofSeconds(60));
```

### Retry Configuration

```java
// Configure retry behavior
ModelProvider provider = new OpenAiModelProvider()
    .withMaxRetries(3)
    .withRetryDelay(Duration.ofSeconds(1))
    .withExponentialBackoff(true);
```

## Streaming Chat

### Real-time Streaming

```java
// OCI GenAI streaming
StreamingChatModel streamingModel = ociProvider.getStreamingChatModel("cohere.command");

streamingModel.generate("Explain quantum computing", new StreamingResponseHandler() {
    @Override
    public void onNext(String token) {
        System.out.print(token);
    }
    
    @Override
    public void onComplete() {
        System.out.println("\nStreaming complete");
    }
    
    @Override
    public void onError(Throwable error) {
        System.err.println("Streaming error: " + error.getMessage());
    }
});
```

### Server-Sent Events

```java
// Handle different event types
streamingModel.generate("Tell me about AI", new StreamingResponseHandler() {
    @Override
    public void onNext(String token) {
        // Handle text tokens
    }
    
    @Override
    public void onCitation(String citation) {
        // Handle citations
    }
    
    @Override
    public void onFinishReason(String reason) {
        // Handle completion reason
    }
});
```

## Local ONNX Models

### Embedding Models

```java
// Local ONNX embedding model
EmbeddingModel embeddingModel = LocalOnnxEmbeddingModel.builder()
    .modelPath("models/all-MiniLM-L6-v2.onnx")
    .vocabularyPath("models/vocab.txt")
    .poolingMode(PoolingMode.MEAN)
    .useGpu(true)
    .build();
```

### Scoring Models

```java
// Local ONNX scoring model
ScoringModel scoringModel = LocalOnnxScoringModel.builder()
    .modelPath("models/ms-marco-MiniLM-L-12-v2.onnx")
    .vocabularyPath("models/vocab.txt")
    .build();
```

## Performance Optimization

### Caching

```java
// Enable model caching
ModelProvider provider = new OpenAiModelProvider()
    .withCaching(true)
    .withCacheSize(1000)
    .withCacheTtl(Duration.ofMinutes(30));
```

### Batch Processing

```java
// Process embeddings in batches
List<String> texts = Arrays.asList("text1", "text2", "text3");
List<Embedding> embeddings = embeddingModel.embedAll(texts);
```

### Connection Reuse

```java
// Reuse connections for multiple requests
try (ModelProvider provider = new OpenAiModelProvider()) {
    ChatModel chatModel = provider.getChatModel("gpt-4");
    
    // Multiple requests use the same connection pool
    String response1 = chatModel.generate("Question 1");
    String response2 = chatModel.generate("Question 2");
}
```

## Error Handling

### Exception Types

```java
try {
    String response = chatModel.generate("Question");
} catch (ModelException e) {
    // Handle model-specific errors
} catch (AuthenticationException e) {
    // Handle authentication failures
} catch (RateLimitException e) {
    // Handle rate limiting
} catch (NetworkException e) {
    // Handle network issues
}
```

### Retry Logic

```java
// Automatic retry with exponential backoff
ModelProvider provider = new OpenAiModelProvider()
    .withRetryPolicy(RetryPolicy.builder()
        .maxRetries(3)
        .initialDelay(Duration.ofSeconds(1))
        .exponentialBackoff(2.0)
        .maxDelay(Duration.ofSeconds(30))
        .build());
```

## CDI Integration

### Injection

```java
@Inject
@Named("openai")
private ModelProvider openaiProvider;

@Inject
@Named("oci")
private ModelProvider ociProvider;

@Inject
@Named("ollama")
private ModelProvider ollamaProvider;
```

### Configuration

```java
@ApplicationScoped
public class ModelConfiguration {
    
    @Produces
    @Named("openai")
    public ModelProvider createOpenAiProvider() {
        return new OpenAiModelProvider();
    }
    
    @Produces
    @Named("oci")
    public ModelProvider createOciProvider() {
        return new OciModelProvider();
    }
}
```

## Development

### Building All Providers

```bash
mvn clean install
```

### Building Specific Provider

```bash
mvn clean install -pl coherence-rag-open-ai
```

### Running Tests

```bash
mvn test
```

### Integration Tests

```bash
# Run integration tests (requires actual API keys)
mvn verify -P integration-tests
```

## Troubleshooting

### Common Issues

1. **Authentication failures**: Check API key configuration
2. **Rate limiting**: Implement exponential backoff
3. **Model not found**: Verify model name and availability
4. **Connection timeouts**: Increase timeout values
5. **Large responses**: Consider streaming for long responses

### Debug Mode

```bash
# Enable debug logging
export COHERENCE_RAG_LOG_LEVEL=DEBUG

# Provider-specific debugging
export OPENAI_LOG_LEVEL=DEBUG
export OCI_LOG_LEVEL=DEBUG
```

## License

This project is licensed under the Universal Permissive License (UPL) 1.0. 
