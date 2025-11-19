# Anthropic Model Provider

Model provider implementation for Anthropic, enabling integration with Anthropic's language models through the Oracle Coherence RAG framework.

## Overview

The Anthropic Model Provider provides comprehensive integration with Anthropic's language models. 
This provider supports both chat and embedding models with advanced features like streaming responses and function calling.

## Features

### 🤖 **Model Support**
- claude-opus-4-20250514
- claude-sonnet-4-20250514
- claude-3-7-sonnet-20250219
- claude-3-5-sonnet-20241022
- claude-3-5-haiku-20241022
- claude-3-5-sonnet-20240620
- claude-3-opus-20240229
- claude-3-haiku-20240307
- Custom model configurations
- Latest model versions

### 💬 **Chat Models**
- Conversational AI capabilities
- System message support
- Temperature and top-p controls
- Maximum token limits
- Streaming responses
- Function calling support

### ⚙️ **Configuration**
- API key authentication
- Organization ID support
- Custom endpoint support
- TopP / TopK
- Timeout configurations
- Retry mechanisms
- CDI integration

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.oracle.coherence.ce</groupId>
    <artifactId>coherence-rag-anthropic</artifactId>
    <version>25.09</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
// Create Anthropic model provider
ModelProvider provider = new AnthropicModelProvider();

// Get chat model
ChatLanguageModel chatModel = provider.getChatModel("claude-3-7-sonnet-20250219");

// Generate response
String response = chatModel.generate("What is artificial intelligence?");
System.out.println(response);
```

### Streaming Chat

```java
// Get streaming chat model
StreamingChatLanguageModel streamingModel = provider.getStreamingChatModel("claude-3-7-sonnet-20250219");

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
export ANTHROPIC_API_KEY=your-anthropic-api-key

# System property
-Danthropic.api.key=your-anthropic-api-key
```

### Configuration File

```properties
# config.properties
anthropic.api.key=your-anthropic-api-key
anthropic.organization.id=your-organization-id
anthropic.base.url=https://api.anthropic.com/v1/
anthropic.timeout=30000
anthropic.max.retries=3
```

## Supported Models

### Chat Models

| Model Name                     | Description       | Context Length | Cost   |
|--------------------------------|-------------------|----------------|--------|
| **claude-sonnet-4-20250514**   | Claude 4 Sonnet   | 200K tokens    | Medium |
| **claude-3-7-sonnet-20250219** | Claude 3.7 Sonnet | 200K tokens    | Medium |
| **claude-3-5-sonnet-20241022** | Claude 3.5 Sonnet | 200K tokens    | Medium |
| **claude-3-haiku-20240307**    | Claude 3 Haiku    | 200K tokens    | Low    |
| **claude-3-5-haiku-20241022**  | Claude 3.5 Haiku  | 200K tokens    | Low    |
| **claude-opus-4-20250514**     | Claude 4 Opus     | 200K tokens    | TBC    |
| **claude-3-opus-20240229**     | Claude 3 Opus     | 200K tokens    | High   |


## CDI Integration

### Model Provider Configuration

```java
@ApplicationScoped
public class AnthropicConfiguration {
    
    @Produces
    @Named("anthropic")
    public ModelProvider createAnthropicProvider() {
        return new AnthropicModelProvider();
    }
    
    @Produces
    @Named("anthropic-chat")
    public ChatLanguageModel createChatModel() {
        return new AnthropicModelProvider().getChatModel("claude-opus-4-20250514");
    }

    @Produces
    @Named("anthropic-streaming")
    public StreamingChatLanguageModel createStreamingChatModel() {
        return new AnthropicModelProvider().getStreamingChatModel("claude-opus-4-20250514");
    }
}
```

### Injection Usage

```java
@Inject
@Named("anthropic-chat")
private ChatLanguageModel chatModel;

@Inject
@Named("anthropic-streaming")
private StreamingChatLanguageModel streamingModel;
```

## Advanced Configuration

### Custom Model Parameters

```java
import dev.langchain4j.model.anthropic.AnthropicChatModel;

// Configure chat model with custom parameters
AnthropicChatModel chatModel = AnthropicChatModel.builder()
        .apiKey("your-api-key")
        .organizationId("your-org-id")
        .modelName("claude-opus-4-20250514")
        .temperature(0.7)
        .topP(0.9)
        .maxTokens(2048)
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
AnthropicChatModel chatModel = AnthropicChatModel.builder()
    .apiKey("your-api-key")
    .modelName("claude-opus-4-20250514")
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
} catch (AnthropicException e) {
    if (e.getStatusCode() == 401) {
        logger.error("Invalid API key: {}", e.getMessage());
    } else if (e.getStatusCode() == 429) {
        logger.warn("Rate limit exceeded: {}", e.getMessage());
        // Implement backoff strategy
    } else if (e.getStatusCode() == 400) {
        logger.error("Invalid request: {}", e.getMessage());
    } else {
        logger.error("Anthropic API error: {}", e.getMessage());
    }
} catch (Exception e) {
    logger.error("Unexpected error: {}", e.getMessage());
}
```

### Rate Limiting

```java
// Custom retry configuration with exponential backoff
AnthropicChatModel chatModel = AnthropicChatModel.builder()
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

AnthropicChatModel chatModel = AnthropicChatModel.builder()
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
public void testAnthropicChatModel() {
    // Mock Anthropic API responses
   AnthropicChatModel chatModel = AnthropicChatModel.builder()
        .apiKey("test-key")
        .modelName("claude-opus-4-20250514")
        .build();
    
    String response = chatModel.generate("Test prompt");
    assertThat(response).isNotNull();
}
```

### Integration Tests

```java
@Test
@EnabledIf("hasAnthropicCredentials")
public void testRealAnthropicIntegration() {
   AnthropicChatModel provider = new AnthropicChatModel();
    
    // Test chat model
    ChatLanguageModel chatModel = provider.getChatModel("claude-3-5-sonnet-latest");
    String response = chatModel.generate("Hello, how are you?");
    assertThat(response).isNotEmpty();
}

private static boolean hasAnthropicCredentials() {
    return System.getenv("ANTHROPIC_API_KEY") != null;
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
- Implement caching to reduce API calls
- Use streaming for better user experience

## Troubleshooting

### Common Issues

1. **Invalid API Key (401)**
   ```
   Solution: Verify ANTHROPIC_API_KEY environment variable is set correctly
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
