# Ollama Model Provider

Model provider implementation for Ollama, enabling integration with locally hosted open-source language models through the Oracle Coherence RAG framework.

## Overview

The Ollama Model Provider provides integration with Ollama, a local LLM server that runs open-source language models. This provider supports both chat and embedding models, allowing you to run models locally without external API dependencies.

## Features

### 🤖 **Model Support**
- Llama 2 and Llama 3 models
- Code Llama models
- Mistral models
- Gemma models
- Phi models
- Custom model configurations

### 💬 **Chat Models**
- Conversational AI capabilities
- System message support
- Temperature and top-p controls
- Maximum token limits
- Streaming responses

### 🧠 **Embedding Models**
- Text embedding generation
- Batch processing support
- Configurable dimensions
- Local inference (no external API calls)

### ⚙️ **Configuration**
- Local server connection
- Custom endpoint support
- Timeout configurations
- Retry mechanisms
- CDI integration

## Installation

### Prerequisites

First, install and run Ollama:

```bash
# Install Ollama (macOS)
brew install ollama

# Install Ollama (Linux)
curl -fsSL https://ollama.com/install.sh | sh

# Start Ollama server
ollama serve
```

### Maven Dependency

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.oracle.coherence.ce</groupId>
    <artifactId>coherence-rag-ollama</artifactId>
    <version>25.09</version>
</dependency>
```

## Quick Start

### Download Models

First, download the models you want to use:

```bash
# Download Llama 2 7B model
ollama pull llama2

# Download Code Llama model
ollama pull codellama

# Download Mistral model
ollama pull mistral

# Download embedding model
ollama pull nomic-embed-text
```

### Basic Usage

```java
// Create Ollama model provider
ModelProvider provider = new OllamaModelProvider();

// Get chat model
ChatLanguageModel chatModel = provider.getChatModel("llama2");

// Generate response
String response = chatModel.generate("What is artificial intelligence?");
System.out.println(response);

// Get embedding model
EmbeddingModel embeddingModel = provider.getEmbeddingModel("nomic-embed-text");

// Generate embeddings
List<Embedding> embeddings = embeddingModel.embedAll(Arrays.asList(
    "First document text",
    "Second document text"
));
```

### Streaming Chat

```java
// Get streaming chat model
StreamingChatLanguageModel streamingModel = provider.getStreamingChatModel("llama2");

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

## Configuration

### Default Configuration

```java
// Default configuration (localhost:11434)
OllamaModelProvider provider = new OllamaModelProvider();
```

### Custom Configuration

```java
// Custom Ollama server configuration
OllamaModelProvider provider = new OllamaModelProvider();
provider.setBaseUrl("http://your-ollama-server:11434");
provider.setTimeout(Duration.ofSeconds(60));
```

### Environment Variables

```bash
# Set custom Ollama server URL
export OLLAMA_BASE_URL=http://your-ollama-server:11434

# Set timeout
export OLLAMA_TIMEOUT=60000
```

## Supported Models

### Chat Models

| Model Name | Description | Parameters | Context Length |
|------------|-------------|------------|----------------|
| **llama2** | Meta Llama 2 | 7B | 4K tokens |
| **llama2:13b** | Meta Llama 2 | 13B | 4K tokens |
| **llama2:70b** | Meta Llama 2 | 70B | 4K tokens |
| **codellama** | Code Llama | 7B | 16K tokens |
| **mistral** | Mistral | 7B | 8K tokens |
| **gemma** | Google Gemma | 7B | 8K tokens |
| **phi** | Microsoft Phi | 3B | 2K tokens |

### Embedding Models

| Model Name | Description | Dimensions |
|------------|-------------|-----------|
| **nomic-embed-text** | Nomic Embed Text | 768 |
| **all-minilm** | All-MiniLM-L6-v2 | 384 |

## CDI Integration

### Model Provider Configuration

```java
@ApplicationScoped
public class OllamaConfiguration {
    
    @Produces
    @Named("ollama")
    public ModelProvider createOllamaProvider() {
        return new OllamaModelProvider();
    }
    
    @Produces
    @Named("ollama-chat")
    public ChatLanguageModel createChatModel() {
        return new OllamaModelProvider().getChatModel("llama2");
    }
    
    @Produces
    @Named("ollama-embed")
    public EmbeddingModel createEmbeddingModel() {
        return new OllamaModelProvider().getEmbeddingModel("nomic-embed-text");
    }
    
    @Produces
    @Named("ollama-streaming")
    public StreamingChatLanguageModel createStreamingChatModel() {
        return new OllamaModelProvider().getStreamingChatModel("llama2");
    }
}
```

### Injection Usage

```java
@Inject
@Named("ollama-chat")
private ChatLanguageModel chatModel;

@Inject
@Named("ollama-embed")
private EmbeddingModel embeddingModel;

@Inject
@Named("ollama-streaming")
private StreamingChatLanguageModel streamingModel;

public void processDocuments(List<String> documents) {
    // Generate embeddings locally
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
OllamaChatModel chatModel = OllamaChatModel.builder()
    .baseUrl("http://localhost:11434")
    .modelName("llama2")
    .temperature(0.7)
    .topP(0.9)
    .maxTokens(2048)
    .timeout(Duration.ofSeconds(30))
    .build();

// Configure embedding model
OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
    .baseUrl("http://localhost:11434")
    .modelName("nomic-embed-text")
    .timeout(Duration.ofSeconds(30))
    .build();
```

### Multiple Ollama Instances

```java
// Configure multiple Ollama instances
OllamaModelProvider provider1 = new OllamaModelProvider();
provider1.setBaseUrl("http://ollama-server-1:11434");

OllamaModelProvider provider2 = new OllamaModelProvider();
provider2.setBaseUrl("http://ollama-server-2:11434");

// Load balance between instances
ChatLanguageModel chatModel1 = provider1.getChatModel("llama2");
ChatLanguageModel chatModel2 = provider2.getChatModel("mistral");
```

## Error Handling

### Exception Types

```java
try {
    String response = chatModel.generate("What is AI?");
} catch (OllamaException e) {
    if (e.getStatusCode() == 404) {
        logger.error("Model not found: {}", e.getMessage());
        // Suggest downloading the model
    } else if (e.getStatusCode() == 503) {
        logger.error("Ollama server unavailable: {}", e.getMessage());
        // Retry or fallback logic
    } else {
        logger.error("Ollama error: {}", e.getMessage());
    }
} catch (ConnectException e) {
    logger.error("Cannot connect to Ollama server: {}", e.getMessage());
    // Check if Ollama is running
} catch (Exception e) {
    logger.error("Unexpected error: {}", e.getMessage());
}
```

### Health Check

```java
// Check if Ollama server is available
public boolean isOllamaAvailable() {
    try {
        OllamaClient client = new OllamaClient("http://localhost:11434");
        return client.isHealthy();
    } catch (Exception e) {
        return false;
    }
}
```

## Performance Optimization

### Model Management

```java
// Preload models for better performance
public void preloadModels() {
    OllamaClient client = new OllamaClient("http://localhost:11434");
    
    // Preload chat model
    client.loadModel("llama2");
    
    // Preload embedding model
    client.loadModel("nomic-embed-text");
}
```

### Connection Pooling

```java
// Configure HTTP client with connection pooling
HttpClient httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(30))
    .build();

OllamaChatModel chatModel = OllamaChatModel.builder()
    .baseUrl("http://localhost:11434")
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
public void testOllamaChatModel() {
    // Mock Ollama responses
    OllamaChatModel chatModel = OllamaChatModel.builder()
        .baseUrl("http://localhost:11434")
        .modelName("llama2")
        .build();
    
    String response = chatModel.generate("Test prompt");
    assertThat(response).isNotNull();
}

@Test
public void testOllamaEmbeddingModel() {
    OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
        .baseUrl("http://localhost:11434")
        .modelName("nomic-embed-text")
        .build();
    
    List<Embedding> embeddings = embeddingModel.embedAll(Arrays.asList("test text"));
    assertThat(embeddings).hasSize(1);
    assertThat(embeddings.get(0).vector()).isNotEmpty();
}
```

### Integration Tests

```java
@Test
@EnabledIf("isOllamaRunning")
public void testRealOllamaIntegration() {
    OllamaModelProvider provider = new OllamaModelProvider();
    
    // Test chat model
    ChatLanguageModel chatModel = provider.getChatModel("llama2");
    String response = chatModel.generate("Hello, how are you?");
    assertThat(response).isNotEmpty();
    
    // Test streaming model
    StreamingChatLanguageModel streamingModel = provider.getStreamingChatModel("llama2");
    StringBuilder responseBuilder = new StringBuilder();
    
    streamingModel.generate("Count to 5", new StreamingResponseHandler<AiMessage>() {
        @Override
        public void onNext(String token) {
            responseBuilder.append(token);
        }
    });
    
    assertThat(responseBuilder.toString()).isNotEmpty();
}

private static boolean isOllamaRunning() {
    try {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:11434/api/tags"))
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200;
    } catch (Exception e) {
        return false;
    }
}
```

## Best Practices

### Security
- Run Ollama on secure networks
- Use firewalls to restrict access
- Monitor resource usage
- Keep models updated

### Performance
- Use appropriate model sizes for your hardware
- Implement connection pooling
- Cache responses for repeated queries
- Preload models for better response times

### Resource Management
- Monitor GPU/CPU usage
- Implement model rotation for multiple use cases
- Use appropriate batch sizes for embeddings
- Consider model quantization for smaller footprints

## Troubleshooting

### Common Issues

1. **Ollama Server Not Running**
   ```bash
   # Start Ollama server
   ollama serve
   ```

2. **Model Not Found (404)**
   ```bash
   # Download the model
   ollama pull llama2
   ```

3. **Connection Refused**
   ```
   Solution: Check if Ollama is running on the correct port (default: 11434)
   ```

4. **Out of Memory**
   ```
   Solution: Use smaller models or increase system memory
   ```

5. **Slow Response Times**
   ```
   Solution: Use GPU acceleration or smaller models
   ```

### Debug Commands

```bash
# List available models
ollama list

# Show model information
ollama show llama2

# Check Ollama server status
curl http://localhost:11434/api/tags

# Monitor resource usage
htop
nvidia-smi  # For GPU monitoring
```

## License

This project is licensed under the Universal Permissive License (UPL) 1.0. 
