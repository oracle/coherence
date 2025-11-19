# DeepSeek Model Provider

Model provider implementation for DeepSeek AI models, enabling integration with DeepSeek's language models through the Oracle Coherence RAG framework.

## Overview

The DeepSeek Model Provider provides integration with DeepSeek AI's language models, including the DeepSeek-V2 series and other models offered through their OpenAI-compatible API. This provider supports both chat and embedding models for comprehensive RAG applications.

## Features

### 🤖 **Model Support**
- DeepSeek-V2 Chat models
- DeepSeek-V2 Lite models
- DeepSeek Coder models
- Custom model configurations
- OpenAI-compatible API interface

### 💬 **Chat Models**
- Conversational AI capabilities
- System message support
- Temperature and top-p controls
- Maximum token limits
- Response streaming (if supported)

### 🧠 **Embedding Models**
- Text embedding generation
- Batch processing support
- Configurable dimensions
- Semantic similarity search

### ⚙️ **Configuration**
- API key authentication
- Custom endpoint support
- Timeout configurations
- Retry mechanisms
- CDI integration

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.oracle.coherence.ce</groupId>
    <artifactId>coherence-rag-deepseek</artifactId>
    <version>25.09</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
// Create DeepSeek model provider
ModelProvider provider = new DeepSeekModelProvider();

// Get chat model
ChatLanguageModel chatModel = provider.getChatModel("deepseek-chat");

// Generate response
String response = chatModel.generate("What is artificial intelligence?");
System.out.println(response);

// Get embedding model
EmbeddingModel embeddingModel = provider.getEmbeddingModel("deepseek-embed");

// Generate embeddings
List<Embedding> embeddings = embeddingModel.embedAll(Arrays.asList(
    "First document text",
    "Second document text"
));
```

### Configuration

```java
// Configure with API key
System.setProperty("deepseek.api.key", "your-api-key");

// Or use environment variable
// export DEEPSEEK_API_KEY=your-api-key

// Create provider with custom configuration
DeepSeekModelProvider provider = new DeepSeekModelProvider();
```

## Authentication

### API Key

```bash
# Environment variable (recommended)
export DEEPSEEK_API_KEY=your-deepseek-api-key

# System property
-Ddeepseek.api.key=your-deepseek-api-key
```

### Configuration File

```properties
# config.properties
deepseek.api.key=your-deepseek-api-key
deepseek.api.base.url=https://api.deepseek.com/v1
deepseek.timeout=30000
deepseek.max.retries=3
```

## Supported Models

### Chat Models

| Model Name | Description | Context Length |
|------------|-------------|----------------|
| **deepseek-chat** | DeepSeek-V2 Chat | 32K tokens |
| **deepseek-coder** | DeepSeek Coder | 16K tokens |
| **deepseek-lite** | DeepSeek-V2 Lite | 16K tokens |

### Embedding Models

| Model Name | Description | Dimensions |
|------------|-------------|-----------|
| **deepseek-embed** | DeepSeek Embedding | 1536 |

## CDI Integration

### Model Provider Configuration

```java
@ApplicationScoped
public class DeepSeekConfiguration {
    
    @Produces
    @Named("deepseek")
    public ModelProvider createDeepSeekProvider() {
        return new DeepSeekModelProvider();
    }
    
    @Produces
    @Named("deepseek-chat")
    public ChatLanguageModel createChatModel() {
        return new DeepSeekModelProvider().getChatModel("deepseek-chat");
    }
    
    @Produces
    @Named("deepseek-embed")
    public EmbeddingModel createEmbeddingModel() {
        return new DeepSeekModelProvider().getEmbeddingModel("deepseek-embed");
    }
}
```

### Injection Usage

```java
@Inject
@Named("deepseek-chat")
private ChatLanguageModel chatModel;

@Inject
@Named("deepseek-embed")
private EmbeddingModel embeddingModel;

public void processDocuments(List<String> documents) {
    // Generate embeddings
    List<Embedding> embeddings = embeddingModel.embedAll(documents);
    
    // Use chat model for analysis
    String analysis = chatModel.generate("Analyze these documents: " + String.join(", ", documents));
}
```

## Advanced Configuration

### Custom Model Parameters

```java
// Configure chat model with custom parameters
ChatLanguageModel chatModel = DeepSeekChatModel.builder()
    .apiKey("your-api-key")
    .modelName("deepseek-chat")
    .temperature(0.7)
    .topP(0.9)
    .maxTokens(2048)
    .timeout(Duration.ofSeconds(30))
    .maxRetries(3)
    .build();

// Configure embedding model
EmbeddingModel embeddingModel = DeepSeekEmbeddingModel.builder()
    .apiKey("your-api-key")
    .modelName("deepseek-embed")
    .timeout(Duration.ofSeconds(30))
    .maxRetries(3)
    .build();
```

### Batch Processing

```java
// Process multiple documents efficiently
List<String> documents = loadDocuments();
List<List<String>> batches = partitionIntoBatches(documents, 100);

List<Embedding> allEmbeddings = batches.parallelStream()
    .flatMap(batch -> embeddingModel.embedAll(batch).stream())
    .collect(Collectors.toList());
```

## Error Handling

### Exception Types

```java
try {
    String response = chatModel.generate("What is AI?");
} catch (DeepSeekException e) {
    if (e.getStatusCode() == 401) {
        logger.error("Invalid API key: {}", e.getMessage());
    } else if (e.getStatusCode() == 429) {
        logger.warn("Rate limit exceeded: {}", e.getMessage());
        // Implement backoff strategy
    } else {
        logger.error("DeepSeek API error: {}", e.getMessage());
    }
} catch (Exception e) {
    logger.error("Unexpected error: {}", e.getMessage());
}
```

### Retry Logic

```java
// Custom retry configuration
DeepSeekChatModel chatModel = DeepSeekChatModel.builder()
    .apiKey("your-api-key")
    .maxRetries(5)
    .retryDelay(Duration.ofSeconds(2))
    .retryMultiplier(2.0)
    .build();
```

## Performance Optimization

### Connection Pooling

```java
// Configure HTTP client with connection pooling
HttpClient httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(30))
    .build();

// Use with custom configuration
DeepSeekChatModel chatModel = DeepSeekChatModel.builder()
    .apiKey("your-api-key")
    .httpClient(httpClient)
    .build();
```

### Caching

```java
// Cache model responses
private final LoadingCache<String, String> responseCache = 
    Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofHours(1))
        .build(prompt -> chatModel.generate(prompt));

public String getCachedResponse(String prompt) {
    return responseCache.get(prompt);
}
```

## Testing

### Unit Tests

```java
@Test
public void testDeepSeekChatModel() {
    // Mock DeepSeek API responses
    DeepSeekChatModel chatModel = DeepSeekChatModel.builder()
        .apiKey("test-key")
        .build();
    
    String response = chatModel.generate("Test prompt");
    assertThat(response).isNotNull();
}

@Test
public void testDeepSeekEmbeddingModel() {
    DeepSeekEmbeddingModel embeddingModel = DeepSeekEmbeddingModel.builder()
        .apiKey("test-key")
        .build();
    
    List<Embedding> embeddings = embeddingModel.embedAll(Arrays.asList("test text"));
    assertThat(embeddings).hasSize(1);
    assertThat(embeddings.get(0).vector()).isNotEmpty();
}
```

### Integration Tests

```java
@Test
@EnabledIf("hasDeepSeekCredentials")
public void testRealDeepSeekIntegration() {
    DeepSeekModelProvider provider = new DeepSeekModelProvider();
    
    // Test chat model
    ChatLanguageModel chatModel = provider.getChatModel("deepseek-chat");
    String response = chatModel.generate("Hello, how are you?");
    assertThat(response).isNotEmpty();
    
    // Test embedding model
    EmbeddingModel embeddingModel = provider.getEmbeddingModel("deepseek-embed");
    List<Embedding> embeddings = embeddingModel.embedAll(Arrays.asList("test"));
    assertThat(embeddings).hasSize(1);
}

private static boolean hasDeepSeekCredentials() {
    return System.getenv("DEEPSEEK_API_KEY") != null;
}
```

## Best Practices

### Security
- Store API keys securely (environment variables, key management services)
- Use HTTPS for all API calls
- Implement rate limiting to avoid API quotas
- Monitor API usage and costs

### Performance
- Use batch processing for multiple embeddings
- Implement connection pooling for high-throughput applications
- Cache responses for repeated queries
- Use appropriate timeout values

### Cost Optimization
- Monitor token usage and optimize prompts
- Use appropriate model sizes for your use case
- Implement caching to reduce API calls
- Consider model fine-tuning for specific tasks

## Troubleshooting

### Common Issues

1. **Invalid API Key (401)**
   ```
   Solution: Verify DEEPSEEK_API_KEY environment variable is set correctly
   ```

2. **Rate Limit Exceeded (429)**
   ```
   Solution: Implement exponential backoff and respect rate limits
   ```

3. **Model Not Found (404)**
   ```
   Solution: Verify model name is correct and available
   ```

4. **Timeout Errors**
   ```
   Solution: Increase timeout values or check network connectivity
   ```

## License

This project is licensed under the Universal Permissive License (UPL) 1.0. 
