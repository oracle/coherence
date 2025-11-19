# OCI GenAI Model Provider

Model provider implementation for Oracle Cloud Infrastructure Generative AI service, enabling integration with OCI GenAI models through the Oracle Coherence RAG framework.

## Overview

The OCI GenAI Model Provider provides comprehensive integration with Oracle Cloud Infrastructure's Generative AI service, supporting both chat and embedding models with advanced features like streaming responses, batch processing, and flexible authentication methods.

## Features

### 🤖 **Model Support**
- Cohere Command models
- Cohere Embed models
- Meta Llama models
- Custom model configurations
- Multi-region deployment support

### 💬 **Chat Models**
- Synchronous and streaming chat
- System message support
- Temperature and top-p controls
- Maximum token limits
- Cohere and generic model formats

### 🧠 **Embedding Models**
- Text embedding generation
- Batch processing with automatic chunking
- Configurable dimensions
- Input truncation support
- Multiple input types (SEARCH_DOCUMENT, SEARCH_QUERY, etc.)

### ⚙️ **Configuration**
- Multiple authentication methods
- Custom endpoint support
- Timeout configurations
- Retry mechanisms with exponential backoff
- CDI integration

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.oracle.coherence.ce</groupId>
    <artifactId>coherence-rag-oci</artifactId>
    <version>25.09</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
// Create OCI GenAI model provider
ModelProvider provider = new OciModelProvider();

// Get chat model
ChatLanguageModel chatModel = provider.getChatModel("cohere.command");

// Generate response
String response = chatModel.generate("What is artificial intelligence?");
System.out.println(response);

// Get embedding model
EmbeddingModel embeddingModel = provider.getEmbeddingModel("cohere.embed-english-v3.0");

// Generate embeddings
List<Embedding> embeddings = embeddingModel.embedAll(Arrays.asList(
    "First document text",
    "Second document text"
));
```

### Streaming Chat

```java
// Get streaming chat model
StreamingChatLanguageModel streamingModel = provider.getStreamingChatModel("cohere.command");

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

### Config File Authentication

Create `~/.oci/config` file:

```ini
[DEFAULT]
user=ocid1.user.oc1..aaaaaaaafakeuser
fingerprint=aa:bb:cc:dd:ee:ff:00:11:22:33:44:55:66:77:88:99
tenancy=ocid1.tenancy.oc1..aaaaaaaafaketenancy
region=us-ashburn-1
key_file=~/.oci/oci_api_key.pem
```

### Instance Principals

For compute instances with instance principals enabled:

```java
// Automatically uses instance principals
OciModelProvider provider = new OciModelProvider();
```

### Environment Variables

```bash
# Alternative to config file
export OCI_CLI_USER=ocid1.user.oc1..aaaaaaaafakeuser
export OCI_CLI_FINGERPRINT=aa:bb:cc:dd:ee:ff:00:11:22:33:44:55:66:77:88:99
export OCI_CLI_TENANCY=ocid1.tenancy.oc1..aaaaaaaafaketenancy
export OCI_CLI_REGION=us-ashburn-1
export OCI_CLI_KEY_FILE=~/.oci/oci_api_key.pem
```

## Supported Models

### Chat Models

| Model Name | Description | Context Length |
|------------|-------------|----------------|
| **cohere.command** | Cohere Command | 4K tokens |
| **cohere.command-light** | Cohere Command Light | 4K tokens |
| **meta.llama-2-70b-chat** | Meta Llama 2 70B | 4K tokens |

### Embedding Models

| Model Name | Description | Dimensions |
|------------|-------------|-----------|
| **cohere.embed-english-v3.0** | Cohere Embed English v3.0 | 1024 |
| **cohere.embed-multilingual-v3.0** | Cohere Embed Multilingual v3.0 | 1024 |

## CDI Integration

### Model Provider Configuration

```java
@ApplicationScoped
public class OciGenAiConfiguration {
    
    @Produces
    @Named("oci")
    public ModelProvider createOciProvider() {
        return new OciModelProvider();
    }
    
    @Produces
    @Named("oci-chat")
    public ChatLanguageModel createChatModel() {
        return new OciModelProvider().getChatModel("cohere.command");
    }
    
    @Produces
    @Named("oci-embed")
    public EmbeddingModel createEmbeddingModel() {
        return new OciModelProvider().getEmbeddingModel("cohere.embed-english-v3.0");
    }
    
    @Produces
    @Named("oci-streaming")
    public StreamingChatLanguageModel createStreamingChatModel() {
        return new OciModelProvider().getStreamingChatModel("cohere.command");
    }
}
```

### Injection Usage

```java
@Inject
@Named("oci-chat")
private ChatLanguageModel chatModel;

@Inject
@Named("oci-embed")
private EmbeddingModel embeddingModel;

@Inject
@Named("oci-streaming")
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
OciGenAiChatModel chatModel = OciGenAiChatModel.builder()
    .compartmentId("ocid1.compartment.oc1..aaaaaaaafakecompartment")
    .modelId("cohere.command")
    .temperature(0.7)
    .topP(0.9)
    .maxTokens(2048)
    .timeout(Duration.ofSeconds(30))
    .maxRetries(3)
    .build();

// Configure embedding model
OciGenAiEmbeddingModel embeddingModel = OciGenAiEmbeddingModel.builder()
    .compartmentId("ocid1.compartment.oc1..aaaaaaaafakecompartment")
    .modelId("cohere.embed-english-v3.0")
    .inputType(EmbedTextDetails.InputType.SearchDocument)
    .timeout(Duration.ofSeconds(30))
    .maxRetries(3)
    .build();
```

### Batch Processing

```java
// Process large documents with automatic batching
List<String> documents = loadLargeDocumentSet();

// Embedding model automatically handles batching
List<Embedding> embeddings = embeddingModel.embedAll(documents);

// For very large batches, use parallel processing
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
} catch (BmcException e) {
    if (e.getStatusCode() == 401) {
        logger.error("Authentication failed: {}", e.getMessage());
    } else if (e.getStatusCode() == 429) {
        logger.warn("Rate limit exceeded: {}", e.getMessage());
        // Implement backoff strategy
    } else if (e.getStatusCode() == 400) {
        logger.error("Invalid request: {}", e.getMessage());
    } else {
        logger.error("OCI GenAI API error: {}", e.getMessage());
    }
} catch (Exception e) {
    logger.error("Unexpected error: {}", e.getMessage());
}
```

### Streaming Error Handling

```java
streamingModel.generate("Analyze this data", new StreamingResponseHandler<AiMessage>() {
    @Override
    public void onNext(String token) {
        System.out.print(token);
    }
    
    @Override
    public void onError(Throwable error) {
        logger.error("Streaming error: {}", error.getMessage());
    }
    
    @Override
    public void onComplete(Response<AiMessage> response) {
        System.out.println("\nStreaming complete");
    }
});
```

## Performance Optimization

### Connection Pooling

```java
// Configure with custom authentication provider
AuthenticationDetailsProvider authProvider = new ConfigFileAuthenticationDetailsProvider(
    "~/.oci/config", "DEFAULT");

GenerativeAiInferenceClient client = GenerativeAiInferenceClient.builder()
    .build(authProvider);
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

### Embedding Optimization

```java
// Optimize embedding generation
OciGenAiEmbeddingModel embeddingModel = OciGenAiEmbeddingModel.builder()
    .compartmentId("your-compartment-id")
    .modelId("cohere.embed-english-v3.0")
    .inputType(EmbedTextDetails.InputType.SearchDocument)
    .truncate(EmbedTextDetails.Truncate.End)  // Truncate instead of error
    .build();
```

## Testing

### Unit Tests

```java
@Test
public void testOciGenAiChatModel() {
    // Mock OCI client responses
    OciGenAiChatModel chatModel = OciGenAiChatModel.builder()
        .compartmentId("test-compartment")
        .modelId("cohere.command")
        .build();
    
    String response = chatModel.generate("Test prompt");
    assertThat(response).isNotNull();
}

@Test
public void testOciGenAiEmbeddingModel() {
    OciGenAiEmbeddingModel embeddingModel = OciGenAiEmbeddingModel.builder()
        .compartmentId("test-compartment")
        .modelId("cohere.embed-english-v3.0")
        .build();
    
    List<Embedding> embeddings = embeddingModel.embedAll(Arrays.asList("test text"));
    assertThat(embeddings).hasSize(1);
    assertThat(embeddings.get(0).vector()).isNotEmpty();
}
```

### Integration Tests

```java
@Test
@EnabledIf("hasOciCredentials")
public void testRealOciIntegration() {
    OciModelProvider provider = new OciModelProvider();
    
    // Test chat model
    ChatLanguageModel chatModel = provider.getChatModel("cohere.command");
    String response = chatModel.generate("Hello, how are you?");
    assertThat(response).isNotEmpty();
    
    // Test streaming model
    StreamingChatLanguageModel streamingModel = provider.getStreamingChatModel("cohere.command");
    StringBuilder responseBuilder = new StringBuilder();
    
    streamingModel.generate("Count to 5", new StreamingResponseHandler<AiMessage>() {
        @Override
        public void onNext(String token) {
            responseBuilder.append(token);
        }
    });
    
    assertThat(responseBuilder.toString()).isNotEmpty();
}

private static boolean hasOciCredentials() {
    return Files.exists(Paths.get(System.getProperty("user.home"), ".oci", "config"));
}
```

## Best Practices

### Security
- Use instance principals for compute instances
- Store API keys securely (OCI Vault, environment variables)
- Apply principle of least privilege for IAM policies
- Enable audit logging for API calls

### Performance
- Use batch processing for multiple embeddings
- Implement connection pooling for high-throughput applications
- Cache responses for repeated queries
- Use appropriate timeout values

### Cost Optimization
- Monitor token usage and optimize prompts
- Use appropriate model sizes for your use case
- Implement caching to reduce API calls
- Consider regional deployment for lower latency

## Troubleshooting

### Common Issues

1. **Authentication Error (401)**
   ```
   Solution: Verify OCI config file or instance principal setup
   ```

2. **Compartment Not Found (404)**
   ```
   Solution: Verify compartment OCID is correct and accessible
   ```

3. **Model Not Available (404)**
   ```
   Solution: Verify model name and availability in your region
   ```

4. **Rate Limiting (429)**
   ```
   Solution: Implement exponential backoff and respect rate limits
   ```

5. **Token Limit Exceeded (400)**
   ```
   Solution: Reduce prompt length or increase max tokens
   ```

## License

This project is licensed under the Universal Permissive License (UPL) 1.0. 
