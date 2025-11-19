# DeepSeek Integration Testing

This document describes how to run integration tests for the DeepSeek model provider.

## Overview

The integration tests support two modes:

1. **WireMock Mode** (default): Uses recorded API responses for fast, reliable offline testing
2. **Real API Mode**: Makes actual calls to DeepSeek API for validation and response capture

## Test Modes

### 1. WireMock Mode (Default)

Tests run against recorded responses stored in `src/test/resources/wiremock/`. This is the default mode and requires no API key or internet connection.

```bash
# No special setup required - tests use recorded responses
mvn test -Dtest=DeepSeekModelProviderIntegrationTest
```

### 2. Real API Mode  

Tests make actual calls to DeepSeek API. Requires a valid API key and incurs usage costs.

```bash
# Set API key
export DEEPSEEK_API_KEY=sk-your-real-api-key

# Run tests against real API
mvn test -Dtest=DeepSeekModelProviderIntegrationTest
```

## Capturing New API Responses

To update the recorded responses with new API calls:

#### Step 1: Start WireMock Proxy

```bash
# Set your real API key
export DEEPSEEK_API_KEY=your-real-api-key

# Start WireMock proxy
mvn exec:java@wiremock-proxy
```

#### Step 2: Run Tests Through Proxy

```bash
# In another terminal

# Point tests to WireMock proxy
export DEEPSEEK_BASE_URL=http://localhost:8089/v1

# Run tests to capture responses
mvn test -Dtest=DeepSeekModelProviderIntegrationTest
```

#### Step 3: Stop Proxy and Verify
- Stop the WireMock proxy (Ctrl+C)
- Check that new mapping files were created in `target/wiremock/mappings/`
- Check that response files were created in `target/wiremock/__files/`
- Copy mapping and response files as needed to `test/resources/wiremock`

### 3. Running Tests Against Real API

For validation or when recorded responses are insufficient:

```bash
# Set real API key (must NOT start with "test")
export DEEPSEEK_API_KEY=sk-your-real-api-key

# Optionally set custom base URL
export DEEPSEEK_BASE_URL=https://api.deepseek.com/v1

# Run tests
mvn test -Dtest=DeepSeekModelProviderIntegrationTest
```

## Test Resources

```
src/test/resources/wiremock/
├── mappings/                                     # Request/response mappings
│   ├── chat-completion-request.json              # Chat completion API mapping
│   └── streaming-chat-completion-request.json    # Streaming Chat completion API mapping
└── __files/                                      # Response body files
    ├── chat-completion-response.json             # Example chat completion response
    └── streaming-chat-completion-response.txt    # Example streaming chat completion response
```

## Test Coverage

The integration tests verify:
- **Chat Model**: Conversation handling with response validation
- **Streaming Chat Model**: Real-time conversation with streaming responses
- **Embedding Models**: Verifies that null is returned (DeepSeek doesn't support embeddings)

## Troubleshooting

### WireMock Proxy Fails to Start
- Ensure port 8089 is available
- Check that Java is installed and accessible

### Tests Failing in WireMock Mode
- Ensure recorded responses exist in `src/test/resources/wiremock/`
- Check that mapping files match the expected request patterns
- Verify response files contain valid JSON or SSE responses (for streaming chat)

### Tests Failing in Real API Mode
- Verify `DEEPSEEK_API_KEY` is set correctly
- Check API key has sufficient credits/quota
- Ensure network connectivity to `api.deepseek.com`
- Verify the base URL is correct (should end with `/v1`)

### Proxy Mode Issues
- Make sure `DEEPSEEK_BASE_URL` points to the proxy: `http://localhost:8089/v1`
- Check that the real `DEEPSEEK_API_KEY` is set for the proxy to authenticate
- Verify the proxy is capturing requests in the terminal output

## Cost Management

When running against the real API:
- Tests use `deepseek-chat` model which is cost-effective
- Each test makes minimal API calls (1-2 requests per test)
- Total cost per test run should be minimal (< $0.01)

## Security Notes

- Never commit real API keys to version control
- Use test API keys (starting with "test") for automated CI/CD pipelines
- Store real API keys in environment variables or secure credential storage
- The WireMock mode allows testing without exposing credentials 