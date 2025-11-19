# OpenAI Model Provider Integration Testing

This module includes comprehensive integration tests for the OpenAI model provider using WireMock in proxy/replay mode. This allows testing against both real OpenAI APIs and recorded responses.

## Overview

The integration tests can operate in two modes:

1. **Real API Mode**: Tests run against the actual OpenAI API
2. **WireMock Mode**: Tests run against recorded API responses

The mode is automatically determined based on the `OPENAI_API_KEY` environment variable:
- Real API: When `OPENAI_API_KEY` does NOT start with "test"
- WireMock: When `OPENAI_API_KEY` starts with "test" or is not set

## Cost-Effective Testing

The tests use the most cost-effective OpenAI models:
- **Embedding Model**: `text-embedding-3-small` (cheaper than text-embedding-3-large)
- **Chat Model**: `gpt-3.5-turbo` (significantly cheaper than GPT-4 models)

## Setup and Usage

### 1. Running Tests Against Recorded Responses (Default)

This is the recommended approach for CI/CD and regular development:

```bash
# Set test API key (or don't set it at all)
export OPENAI_API_KEY=test-key

# Run the integration tests
mvn test -Dtest=OpenAiModelProviderIntegrationTest
```

### 2. Capturing New API Responses

When you need to update the recorded responses or test new functionality:

#### Step 1: Start WireMock Proxy
```bash
# Set your real OpenAI API key
export OPENAI_API_KEY=your-real-api-key

# Start WireMock proxy
mvn exec:java@wiremock-proxy
```

#### Step 2: Run Tests Through Proxy
In another terminal:
```bash
# Keep the real API key set
export OPENAI_API_KEY=your-real-api-key

# Point tests to WireMock proxy
export OPENAI_BASE_URL=http://localhost:8089/v1

# Run tests to capture responses
mvn test -Dtest=OpenAiModelProviderIntegrationTest
```

#### Step 3: Stop Proxy and Verify
- Stop the WireMock proxy (Ctrl+C)
- Check that new mapping files were created in `target/wiremock/mappings/`
- Check that response files were created in `target/wiremock/__files/`
- Copy mapping and response files as needed to `test/resources/wiremock`

### 3. Running Tests Against Real API

For validation or testing new functionality:

```bash
# Set your real OpenAI API key (must NOT start with "test")
export OPENAI_API_KEY=sk-your-real-api-key

# Run tests against real API
mvn test -Dtest=OpenAiModelProviderIntegrationTest
```

## Directory Structure

```
src/test/resources/wiremock/
├── mappings/                                    # Request/response mappings
│   ├── chat-completion-request.json             # Chat completion API mapping
│   ├── embedding-request.json                   # Embedding API mapping
│   └── streaming-chat-completion-request.json   # Streaming Chat completion API mapping
└── __files/                                     # Response body files
    ├── chat-completion-response.json            # Example chat completion response
    ├── embedding-response.json                  # Example embedding response
    └── streaming-chat-completion-response.txt   # Example streaming chat completion response
```

## Test Coverage

The integration tests cover:

- **Embedding Model**: Text embedding generation with validation
- **Chat Model**: Conversation handling with response validation
- **Streaming Chat Model**: Real-time conversation with streaming responses

## Troubleshooting

### WireMock Proxy Not Starting
- Ensure port 8089 is available
- Check that Java is installed and accessible

### Tests Failing in WireMock Mode
- Ensure recorded responses exist in `src/test/resources/wiremock/`
- Check that mapping files match the expected request patterns
- Verify response files contain valid JSON or SSE responses (for streaming chat)

### Tests Failing in Real API Mode
- Verify your OpenAI API key is valid and has sufficient credits
- Check internet connectivity
- Ensure you're not hitting rate limits

### API Key Issues
- Real API keys must start with `sk-`
- Test API keys should start with `test` to trigger WireMock mode
- Never commit real API keys to version control

## Security Notes

- **Never commit real API keys** to version control
- Use test keys for recorded responses
- The example responses contain only synthetic data
- Real API responses may contain sensitive information - review before committing

## Cost Management

When testing against the real API:
- Tests use the cheapest available models
- Each test run costs approximately $0.01-0.02 USD
- Monitor your OpenAI usage dashboard
- Consider using lower limits on your API key for testing 
