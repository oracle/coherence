# Ollama Integration Testing

This document describes how to run integration tests for the Ollama model provider.

## Overview

The integration tests support two modes:

1. **WireMock Mode** (default): Uses recorded API responses for fast, reliable offline testing
2. **Real API Mode**: Makes actual calls to local Ollama server for validation and response capture

## Prerequisites

For real API testing, you need:
- Ollama installed and running: `ollama serve`
- Test models available: `ollama pull llama3.2:1b` and `ollama pull nomic-embed-text`

## Test Modes

### 1. WireMock Mode (Default)

Tests run against recorded responses stored in `src/test/resources/wiremock/`. This is the default mode and requires no Ollama server or models.

```bash
# No special setup required - tests use recorded responses
mvn test -Dtest=OllamaModelProviderIntegrationTest
```

### 2. Real API Mode  

Tests make actual calls to your local Ollama server. Requires Ollama to be running with the test models.

```bash
# Point tests to Ollama server
export OLLAMA_BASE_URL=http://localhost:11434

# Run tests against real API
mvn test -Dtest=OllamaModelProviderIntegrationTest
```

## Capturing New API Responses

To update the recorded responses with new API calls:

#### Step 1: Setup Ollama Server

```bash
# Start Ollama server
ollama serve

# Pull required models
ollama pull llama3.2:1b
ollama pull nomic-embed-text
```

#### Step 2: Start WireMock Proxy

```bash
# Start WireMock proxy
mvn exec:java@wiremock-proxy
```

#### Step 3: Run Tests Through Proxy

```bash
# In another terminal

# Point tests to WireMock proxy
export OLLAMA_BASE_URL=http://localhost:8089

# Run tests to capture responses
mvn test -Dtest=OllamaModelProviderIntegrationTest
```

#### Step 4: Stop Proxy and Verify
- Stop the WireMock proxy (Ctrl+C)
- Check that new mapping files were created in `target/wiremock/mappings/`
- Check that response files were created in `target/wiremock/__files/`
- Copy mapping and response files as needed to `test/resources/wiremock`

### 3. Running Tests Against Real API

For validation or when recorded responses are insufficient:

```bash
# Start Ollama server
ollama serve

# Point tests to Ollama server
export OLLAMA_BASE_URL=http://localhost:11434

# Run tests
mvn test -Dtest=OllamaModelProviderIntegrationTest
```

## Test Resources

```
src/test/resources/wiremock/
├── mappings/                                   # Request/response mappings
│   ├── embedding-request.json                  # Embedding API mapping
│   ├── chat-completion-request.json            # Chat completion API mapping
│   └── streaming-chat-completion-request.json  # Streaming Chat completion API mapping
└── __files/                                    # Response body files
    ├── embedding-response.json                 # Example embedding response
    ├── chat-completion-response.json           # Example chat completion response
    └── streaming-chat-completion-response.json # Example streaming chat completion response
```

## Test Coverage

The integration tests verify:
- **Embedding Model**: Text embedding with vector response validation
- **Chat Model**: Conversation handling with response validation
- **Streaming Chat Model**: Real-time conversation with streaming responses

## Models Used

The tests use lightweight models suitable for laptop development:

- **Chat Model**: `llama3.2:1b` - Very small (1GB) but capable chat model
- **Embedding Model**: `nomic-embed-text` - Lightweight text embedding model

These models are chosen for their small size and good performance on consumer hardware.

## Troubleshooting

### WireMock Proxy Fails to Start
- Ensure port 8089 is available
- Check that Java is installed and accessible

### Tests Failing in WireMock Mode
- Ensure recorded responses exist in `src/test/resources/wiremock/`
- Check that mapping files match the expected request patterns
- Verify response files contain valid JSON or streaming responses

### Tests Failing in Real API Mode
- Verify Ollama server is running: `ollama serve`
- Check that required models are available: `ollama list`
- Pull missing models: `ollama pull llama3.2:1b`
- Ensure network connectivity to Ollama server
- Check that `OLLAMA_BASE_URL` points to Ollama server: `http://localhost:11434`

### Proxy Mode Issues
- Make sure `OLLAMA_BASE_URL` points to the proxy: `http://localhost:8089`
- Verify Ollama server is running behind the proxy
- Check that the proxy is capturing requests in the terminal output

### Model Loading Issues
- Ensure you have enough memory for the models (llama3.2:1b needs ~1GB)
- Check disk space for model storage
- Verify model download completed successfully: `ollama list`

## Performance Notes

When running against the real API:
- Tests use `llama3.2:1b` which is very lightweight
- Model loading may take a few seconds on first request
- Subsequent requests should be faster due to model caching
- Total memory usage should be manageable on laptops with 8GB+ RAM

## Security Notes

- Ollama runs locally, so no external API keys are required
- WireMock mode allows testing without running any local services
- The integration tests don't expose any credentials or sensitive data
- All test models are publicly available and free to use 
