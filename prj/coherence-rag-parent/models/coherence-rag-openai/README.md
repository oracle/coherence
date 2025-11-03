# OpenAI Model Provider

Model provider implementation for OpenAI, enabling integration with OpenAI's language models through the Oracle Coherence RAG framework.

## Overview

The OpenAI Model Provider provides comprehensive integration with OpenAI's language models, including GPT-4, GPT-3.5, and embedding models. This provider supports both chat and embedding models with advanced features like streaming responses and function calling.

## Features

### 🤖 **Model Support**
- GPT-4 and GPT-4 Turbo models
- GPT-3.5 Turbo models
- Text embedding models (ada-002, text-embedding-3-small, text-embedding-3-large)
- Custom model configurations
- Latest model versions

### 💬 **Chat Models**
- Conversational AI capabilities
- System message support
- Temperature and top-p controls
- Maximum token limits
- Streaming responses
- Function calling support

### 🧠 **Embedding Models**
- Text embedding generation
- Batch processing support
- Configurable dimensions
- Multiple embedding model sizes
- Semantic similarity search

### ⚙️ **Configuration**
- API key authentication
- Organization ID support
- Custom endpoint support
- Timeout configurations
- Retry mechanisms
- CDI integration

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.oracle.coherence.ce</groupId>
    <artifactId>coherence-rag-open-ai</artifactId>
    <version>25.09</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
// Create OpenAI model provider
ModelProvider provider = new OpenAiModelProvider();

// Get chat model
ChatLanguageModel chatModel = provider.getChatModel("gpt-4");

// Generate response
String response = chatModel.generate("What is artificial intelligence?");
System.out.println(response);

// Get embedding model
EmbeddingModel embeddingModel = provider.getEmbeddingModel("text-embedding-3-small");

// Generate embeddings
List<Embedding> embeddings = embeddingModel.embedAll(Arrays.asList(
    "First document text",
    "Second document text"
));
```

### Streaming Chat

```java
// Get streaming chat model
StreamingChatLanguageModel streamingModel = provider.getStreamingChatModel("gpt-4");

// Stream response
streamingModel.generate("Tell me about machine learning", new StreamingResponseHandler<AiMessage>() {
    @Override
    public void onNext(String token) {
        System.out.print(token);
    }
    
    @Override
    public void onComplete(Response<AiMessage> response) {
        System.out.println("\nStreaming complete");
    }
});
```

## Authentication

### API Key

```bash
# Environment variable (recommended)
export OPENAI_API_KEY=your-openai-api-key

# System property
-Dopenai.api.key=your-openai-api-key
```

### Organization ID (Optional)

```bash
# Environment variable
export OPENAI_ORGANIZATION_ID=your-organization-id

# System property
-Dopenai.organization.id=your-organization-id
```

### Configuration File

```properties
# config.properties
openai.api.key=your-openai-api-key
openai.organization.id=your-organization-id
openai.base.url=https://api.openai.com/v1
openai.timeout=30000
openai.max.retries=3
```

## Supported Models

### Chat Models

| Model Name | Description | Context Length | Cost |
|------------|-------------|----------------|------|
| **gpt-4** | GPT-4 | 8K tokens | High |
| **gpt-4-turbo** | GPT-4 Turbo | 128K tokens | Medium |
| **gpt-3.5-turbo** | GPT-3.5 Turbo | 16K tokens | Low |
| **gpt-3.5-turbo-16k** | GPT-3.5 Turbo | 16K tokens | Low |

### Embedding Models

| Model Name | Description | Dimensions | Cost |
|------------|-------------|-----------|------|
| **text-embedding-3-large** | Large embedding model | 3072 | Medium |
| **text-embedding-3-small** | Small embedding model | 1536 | Low |
| **text-embedding-ada-002** | Ada embedding model | 1536 | Low |

## CDI Integration

### Model Provider Configuration

```java
@ApplicationScoped
public class OpenAiConfiguration {
    
    @Produces
    @Named("openai")
    public ModelProvider createOpenAiProvider() {
        return new OpenAiModelProvider();
    }
    
    @Produces
    @Named("openai-chat")
    public ChatLanguageModel createChatModel() {
        return new OpenAiModelProvider().getChatModel("gpt-4");
    }
    
    @Produces
    @Named("openai-embed")
    public EmbeddingModel createEmbeddingModel() {
        return new OpenAiModelProvider().getEmbeddingModel("text-embedding-3-small");
    }
    
    @Produces
    @Named("openai-streaming")
    public StreamingChatLanguageModel createStreamingChatModel() {
        return new OpenAiModelProvider().getStreamingChatModel("gpt-4");
    }
}
```

### Injection Usage

```java
@Inject
@Named("openai-chat")
private ChatLanguageModel chatModel;

@Inject
@Named("openai-embed")
private EmbeddingModel embeddingModel;

@Inject
@Named("openai-streaming")
private StreamingChatLanguageModel streamingModel;

public void processDocuments(List<String> documents) {
    // Generate embeddings
    List<Embedding> embeddings = embeddingModel.embedAll(documents);
    
    // Use chat model for analysis
    String analysis = chatModel.generate("Analyze these documents: " + String.join(", ", documents));
    
    // Stream analysis
    streamingModel.generate("Provide detailed analysis", new StreamingResponseHandler<AiMessage>() {
        @Override
        public void onNext(String token) {
            System.out.print(token);
        }
    });
}
```

## Advanced Configuration

### Custom Model Parameters

```java
// Configure chat model with custom parameters
OpenAiChatModel chatModel = OpenAiChatModel.builder()
    .apiKey("your-api-key")
    .organizationId("your-org-id")
    .modelName("gpt-4")
    .temperature(0.7)
    .topP(0.9)
    .maxTokens(2048)
    .timeout(Duration.ofSeconds(30))
    .maxRetries(3)
    .build();

// Configure embedding model
OpenAiEmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
    .apiKey("your-api-key")
    .modelName("text-embedding-3-small")
    .timeout(Duration.ofSeconds(30))
    .maxRetries(3)
    .build();
```

### Function Calling

```java
// Define function tools
List<ToolSpecification> tools = Arrays.asList(
    ToolSpecification.builder()
        .name("get_weather")
        .description("Get weather information for a location")
        .parameters(JsonSchemaProperty.object()
            .addProperty("location", JsonSchemaProperty.string()
                .description("The location to get weather for"))
            .required("location"))
        .build()
);

// Use function calling
OpenAiChatModel chatModel = OpenAiChatModel.builder()
    .apiKey("your-api-key")
    .modelName("gpt-4")
    .tools(tools)
    .build();

Response<AiMessage> response = chatModel.generate(
    UserMessage.from("What's the weather like in San Francisco?")
);
```

## Error Handling

### Exception Types

```java
try {
    String response = chatModel.generate("What is AI?");
} catch (OpenAiException e) {
    if (e.getStatusCode() == 401) {
        logger.error("Invalid API key: {}", e.getMessage());
    } else if (e.getStatusCode() == 429) {
        logger.warn("Rate limit exceeded: {}", e.getMessage());
        // Implement backoff strategy
    } else if (e.getStatusCode() == 400) {
        logger.error("Invalid request: {}", e.getMessage());
    } else {
        logger.error("OpenAI API error: {}", e.getMessage());
    }
} catch (Exception e) {
    logger.error("Unexpected error: {}", e.getMessage());
}
```

### Rate Limiting

```java
// Custom retry configuration with exponential backoff
OpenAiChatModel chatModel = OpenAiChatModel.builder()
    .apiKey("your-api-key")
    .maxRetries(5)
    .retryDelay(Duration.ofSeconds(1))
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

OpenAiChatModel chatModel = OpenAiChatModel.builder()
    .apiKey("your-api-key")
    .httpClient(httpClient)
    .build();
```

### Batch Processing

```java
// Process multiple embeddings efficiently
List<String> documents = loadDocuments();
List<List<String>> batches = partitionIntoBatches(documents, 100);

List<Embedding> allEmbeddings = batches.parallelStream()
    .flatMap(batch -> embeddingModel.embedAll(batch).stream())
    .collect(Collectors.toList());
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
public void testOpenAiChatModel() {
    // Mock OpenAI API responses
    OpenAiChatModel chatModel = OpenAiChatModel.builder()
        .apiKey("test-key")
        .modelName("gpt-4")
        .build();
    
    String response = chatModel.generate("Test prompt");
    assertThat(response).isNotNull();
}

@Test
public void testOpenAiEmbeddingModel() {
    OpenAiEmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
        .apiKey("test-key")
        .modelName("text-embedding-3-small")
        .build();
    
    List<Embedding> embeddings = embeddingModel.embedAll(Arrays.asList("test text"));
    assertThat(embeddings).hasSize(1);
    assertThat(embeddings.get(0).vector()).isNotEmpty();
}
```

### Integration Tests

```java
@Test
@EnabledIf("hasOpenAiCredentials")
public void testRealOpenAiIntegration() {
    OpenAiModelProvider provider = new OpenAiModelProvider();
    
    // Test chat model
    ChatLanguageModel chatModel = provider.getChatModel("gpt-3.5-turbo");
    String response = chatModel.generate("Hello, how are you?");
    assertThat(response).isNotEmpty();
    
    // Test embedding model
    EmbeddingModel embeddingModel = provider.getEmbeddingModel("text-embedding-3-small");
    List<Embedding> embeddings = embeddingModel.embedAll(Arrays.asList("test"));
    assertThat(embeddings).hasSize(1);
}

private static boolean hasOpenAiCredentials() {
    return System.getenv("OPENAI_API_KEY") != null;
}
```

## Best Practices

### Security
- Store API keys securely (environment variables, key management services)
- Use organization IDs for billing separation
- Monitor API usage and set billing limits
- Implement rate limiting to avoid quota exhaustion

### Performance
- Use appropriate model sizes for your use case
- Implement connection pooling for high-throughput applications
- Cache responses for repeated queries
- Use batch processing for multiple embeddings

### Cost Optimization
- Monitor token usage and optimize prompts
- Use GPT-3.5 for simpler tasks, GPT-4 for complex reasoning
- Implement caching to reduce API calls
- Use streaming for better user experience

## Troubleshooting

### Common Issues

1. **Invalid API Key (401)**
   ```
   Solution: Verify OPENAI_API_KEY environment variable is set correctly
   ```

2. **Rate Limit Exceeded (429)**
   ```
   Solution: Implement exponential backoff and respect rate limits
   ```

3. **Model Not Found (404)**
   ```
   Solution: Verify model name is correct and available to your organization
   ```

4. **Context Length Exceeded (400)**
   ```
   Solution: Reduce prompt length or use models with larger context windows
   ```

5. **Insufficient Quota (429)**
   ```
   Solution: Check billing limits and upgrade plan if needed
   ```

### Monitoring Usage

```java
// Track token usage
public class TokenUsageTracker {
    private final AtomicLong totalTokens = new AtomicLong(0);
    
    public void trackUsage(TokenUsage usage) {
        totalTokens.addAndGet(usage.totalTokenCount());
        logger.info("Token usage: input={}, output={}, total={}", 
            usage.inputTokenCount(), usage.outputTokenCount(), usage.totalTokenCount());
    }
}
```

## License

This project is licensed under the Universal Permissive License (UPL) 1.0. 
