# OCI GenAI Integration Testing

This document describes how to run integration tests for the OCI GenAI model provider, which can operate in two modes: using real OCI GenAI API calls or using recorded WireMock responses.

## Overview

The `OciModelProviderIntegrationTest` class provides comprehensive testing of:
- **Embedding Models**: Cohere embedding models (`cohere.embed-english-v3.0`, `cohere.embed-multilingual-v3.0`)
- **Chat Models**: Cohere Command models (`cohere.command-r-08-2024`, `cohere.command-r-plus-08-2024`)
- **Streaming Chat Models**: Real-time response streaming for interactive applications

## Test Modes

### 1. WireMock Mode (Default/Offline)
Uses pre-recorded API responses for offline testing without making actual API calls.

**When it activates:**
- When `oci.compartment.id` starts with "test"
- When OCI authentication is not properly configured
- For CI/CD environments where real API access is not available

### 2. Real API Mode
Makes actual calls to OCI GenAI service for live testing and response capture.

**When it activates:**
- When `oci.compartment.id` is set to a real compartment OCID
- When OCI authentication is properly configured (config file or properties)

## Prerequisites

### For Real API Testing

1. **OCI Account**: Valid Oracle Cloud Infrastructure account
2. **GenAI Service**: Access to OCI Generative AI service in your tenancy
3. **Authentication**: One of the following configured:

#### Option A: OCI Config File Authentication (Recommended)
```bash
# Default location: ~/.oci/config
[DEFAULT]
user=ocid1.user.oc1..your-user-ocid
fingerprint=your:api:key:fingerprint
tenancy=ocid1.tenancy.oc1..your-tenancy-ocid
region=us-ashburn-1
key_file=~/.oci/your-private-key.pem
```

#### Option B: Properties-based Authentication
```properties
oci.compartment.id=ocid1.compartment.oc1..your-compartment-ocid
oci.tenant.id=ocid1.tenancy.oc1..your-tenancy-ocid
oci.user.id=ocid1.user.oc1..your-user-ocid
oci.region=us-ashburn-1
oci.auth.fingerprint=your:api:key:fingerprint
oci.auth.key=/path/to/your/private-key.pem
```

### For WireMock Testing
No additional setup required - uses pre-recorded responses from `src/test/resources/wiremock/`.

## Running Tests

### Quick Test (WireMock Mode)
```bash
mvn test -Dtest=OciModelProviderIntegrationTest
```

### Test with Real API
```bash
# Using config file authentication
mvn test -Dtest=OciModelProviderIntegrationTest \
  -Doci.compartment.id=ocid1.compartment.oc1..your-compartment-ocid

# Using properties authentication
mvn test -Dtest=OciModelProviderIntegrationTest \
  -Doci.compartment.id=ocid1.compartment.oc1..your-compartment-ocid \
  -Doci.tenant.id=ocid1.tenancy.oc1..your-tenancy-ocid \
  -Doci.user.id=ocid1.user.oc1..your-user-ocid \
  -Doci.region=us-ashburn-1 \
  -Doci.auth.fingerprint=your:api:key:fingerprint \
  -Doci.auth.key=/path/to/your/private-key.pem
```

## Capturing New API Responses

To update the WireMock recordings with fresh API responses:

### 1. Start WireMock Proxy
```bash
mvn exec:java@wiremock-proxy
```

This starts WireMock on port 8089, proxying to:
- `https://inference.generativeai.us-ashburn-1.oci.oraclecloud.com`

### 2. Override GenAI Endpoint (If Needed)
If your OCI configuration points to a different region, you may need to update the proxy target or configure your OCI client to use the proxy endpoint.

### 3. Run Tests with Real Credentials
```bash
mvn test -Dtest=OciModelProviderIntegrationTest \
  -Doci.compartment.id=ocid1.compartment.oc1..your-compartment-ocid
```

### 4. Copy Recorded Responses
```bash
# Copy new mappings and responses
cp -r target/wiremock/* src/test/resources/wiremock/
```

### 5. Stop WireMock Proxy
```bash
# Press Ctrl+C in the terminal running the proxy
```

### 6. Commit Updated Recordings
```bash
git add src/test/resources/wiremock/
git commit -m "Update OCI GenAI WireMock recordings"
```

## Configuration Properties

| Property | Description | Example |
|----------|-------------|---------|
| `oci.compartment.id` | OCI compartment OCID where models are deployed | `ocid1.compartment.oc1..aaaaa...` |
| `oci.config.file` | Path to OCI config file | `~/.oci/config` |
| `oci.config.profile` | Profile name in OCI config file | `DEFAULT` |
| `oci.tenant.id` | OCI tenancy OCID | `ocid1.tenancy.oc1..aaaaa...` |
| `oci.user.id` | OCI user OCID | `ocid1.user.oc1..aaaaa...` |
| `oci.region` | OCI region identifier | `us-ashburn-1` |
| `oci.auth.fingerprint` | API key fingerprint | `aa:bb:cc:dd:...` |
| `oci.auth.key` | Path to private key file | `/path/to/key.pem` |

## Model Configuration

### Embedding Models
- **cohere.embed-english-v3.0**: 1024-dimensional embeddings for English text
- **cohere.embed-multilingual-v3.0**: 1024-dimensional embeddings for multilingual text

### Chat Models  
- **cohere.command-r-08-2024**: Latest Cohere Command model optimized for RAG
- **cohere.command-r-plus-08-2024**: Enhanced version with improved reasoning

### Model Costs (Approximate)
- **Embedding**: ~$0.0001 per 1K tokens
- **Chat**: ~$0.0005 per 1K input tokens, ~$0.0015 per 1K output tokens

## Troubleshooting

### Authentication Issues
```
Error: Unable to determine authentication details
```
**Solution**: Verify OCI config file exists and is properly formatted, or check properties-based auth configuration.

### Compartment Access Issues
```
Error: NotAuthorized - User does not have permission
```
**Solution**: Ensure your user has access to Generative AI service in the specified compartment.

### Region Mismatch
```
Error: Service not available in region
```
**Solution**: Verify the GenAI service is available in your region. Available regions include:
- `us-ashburn-1` (US East - Ashburn)
- `us-phoenix-1` (US West - Phoenix)
- `eu-frankfurt-1` (EU - Frankfurt)

### Network Issues
```
Error: Connection timeout
```
**Solution**: Check firewall/proxy settings. OCI requires access to:
- `*.generativeai.*.oci.oraclecloud.com`
- `*.auth.*.oraclecloud.com`

### Model Availability
```
Error: Model not found
```
**Solution**: Verify the model name is correct and available in your region/compartment.

## CI/CD Integration

For automated testing in CI/CD pipelines:

```yaml
# Example GitHub Actions
- name: Run OCI GenAI Integration Tests
  run: mvn test -Dtest=OciModelProviderIntegrationTest
  env:
    # Uses WireMock mode by default (no real API calls)
    OCI_COMPARTMENT_ID: test.compartment.id
```

For periodic verification with real API:
```yaml
# Scheduled workflow with real credentials
- name: Verify OCI GenAI Integration
  run: mvn test -Dtest=OciModelProviderIntegrationTest
  env:
    OCI_COMPARTMENT_ID: ${{ secrets.OCI_COMPARTMENT_ID }}
    OCI_CONFIG_FILE: ${{ secrets.OCI_CONFIG_FILE }}
```

## API Response Formats

### Embedding Response
```json
{
  "embeddings": [
    {
      "index": 0,
      "embedding": [0.023, -0.045, ...]
    }
  ],
  "modelId": "cohere.embed-english-v3.0",
  "usage": {
    "promptTokens": 2,
    "totalTokens": 2
  }
}
```

### Chat Response
```json
{
  "chatResult": {
    "apiFormat": "COHERE",
    "chatResponse": {
      "text": "Response text",
      "finishReason": "COMPLETE",
      "meta": {
        "billedUnits": {
          "inputTokens": 3,
          "outputTokens": 47
        }
      }
    }
  }
}
```

### Streaming Response (Server-Sent Events)
```
data: {"apiFormat":"COHERE","text":"Hello","finishReason":null}
data: {"apiFormat":"COHERE","text":" there","finishReason":null}
data: {"apiFormat":"COHERE","text":"","finishReason":"COMPLETE"}
``` 