# Google Cloud Storage Document Loader Integration Testing

This document provides comprehensive guidance for running integration tests for the Google Cloud Storage Document Loader component.

## Overview

The Google Cloud Storage Document Loader integration tests support two modes:

1. **WireMock Mode (Default)**: Uses WireMock stubs for offline testing
2. **Real GCS Mode**: Tests against actual Google Cloud Storage service

## Test Modes

### WireMock Mode (Offline Testing)

**When to use**: Development, CI/CD, offline testing
**Triggered by**: `google.application.credentials` system property containing "test"
**Benefits**: Fast, no GCS costs, works offline

### Real GCS Mode

**When to use**: Pre-production validation, end-to-end testing
**Triggered by**: `google.application.credentials` system property not containing "test"
**Benefits**: Tests against real Google Cloud Storage service

## Configuration

### WireMock Mode Configuration

```bash
# System properties for WireMock mode (uses defaults)
# No additional configuration needed
```

### Real GCS Mode Configuration

```bash
# System properties for Real GCS mode
-Dgoogle.application.credentials=path/to/service-account.json

# Or set environment variable
export GOOGLE_APPLICATION_CREDENTIALS=path/to/service-account.json
```

## Google Cloud Credentials (Real GCS Mode)

The real GCS mode uses Google Cloud's standard credential resolution chain:

1. **Service Account File**:
   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS=path/to/service-account.json
   ```

2. **Application Default Credentials**:
   ```bash
   gcloud auth application-default login
   ```

3. **Compute Engine Service Account** (when running on GCE)

4. **Cloud Shell Credentials** (when running in Cloud Shell)

## Running Integration Tests

### Method 1: Maven Command Line

```bash
# Run with WireMock mode (default)
mvn clean verify -Dit.test="*IT"

# Run with Real GCS mode
mvn clean verify -Dit.test="*IT" \
  -Dgoogle.application.credentials=path/to/service-account.json
```

### Method 2: IDE Configuration

**IntelliJ IDEA / Eclipse**:
1. Open Run/Debug Configuration
2. Add VM options:
   ```
   -Dgoogle.application.credentials=path/to/service-account.json
   ```

## WireMock Response Capture

To capture responses from real GCS API for offline testing:

### 1. Start WireMock Proxy

```bash
# Start WireMock proxy to capture GCS responses
mvn exec:java@wiremock-proxy

# Or manually with explicit parameters
java -jar wiremock-standalone-3.13.1.jar \
  --proxy-all=https://storage.googleapis.com \
  --record-mappings \
  --port=8089 \
  --verbose \
  --root-dir=target/wiremock
```

### 2. Configure Test for Capture

```bash
# Run test with proxy endpoint to capture responses
mvn verify -Dit.test="*IT" \
  -Dgoogle.application.credentials=path/to/service-account.json \
  -Dhttp.proxyHost=localhost \
  -Dhttp.proxyPort=8089 \
  -Dhttps.proxyHost=localhost \
  -Dhttps.proxyPort=8089
```

### 3. Copy Captured Files

```bash
# Copy captured mappings and responses
cp target/wiremock/mappings/* src/test/resources/wiremock/mappings/
cp target/wiremock/__files/* src/test/resources/wiremock/__files/
```

## Test Data

### WireMock Mode Test Data

- **Bucket**: `coherence-rag-demo`
- **Object Key**: `test-documents/sample-document.pdf`
- **Mock Response**: Simple PDF document with test content

### Real GCS Mode Test Data

- **Bucket**: `coherence-rag-demo`
- **Object Key**: `coherence/en/middleware/fusion-middleware/coherence/14.1.2/administer-http-sessions/administering-http-session-management-oracle-coherenceweb.pdf`
- **Response**: Real PDF document from Google Cloud Storage

## Test Scenarios

### 1. Document Loading Test

**Purpose**: Verify document can be loaded and parsed correctly
**Assertions**:
- Document content is not empty
- Metadata fields are populated correctly
- Content type is detected properly
- Content length is positive
- ETag is present

### 2. Nonexistent Document Test

**Purpose**: Verify error handling for missing documents
**Assertions**:
- Exception is thrown for nonexistent keys
- Error message is meaningful

## Expected Test Results

### WireMock Mode

```
=== Google Cloud Storage Integration Test Configuration ===
Mode: WireMock (Offline)
Credentials: test-credentials.json
Endpoint: http://localhost:8089
============================================================

Document loaded successfully:
  Text length: 67
  Content type: application/pdf
  Content length: 654
  ETag: CKfUpuaJrucCEAE=
```

### Real GCS Mode

```
=== Google Cloud Storage Integration Test Configuration ===
Mode: Real GCS
Credentials: /path/to/service-account.json
Endpoint: default
============================================================

Document loaded successfully:
  Text length: 251134
  Content type: application/pdf
  Content length: 1857231
  ETag: actual-etag-value
```

## Troubleshooting

### Common Issues

1. **Authentication Errors**:
   - Verify Google Cloud credentials are configured correctly
   - Check service account permissions for Storage access

2. **Network Issues**:
   - Verify internet connectivity for real GCS mode
   - Check firewall settings for WireMock proxy

3. **Missing Test Data**:
   - Ensure test documents exist in GCS bucket
   - Verify bucket permissions

4. **WireMock Issues**:
   - Check WireMock is running on correct port
   - Verify mapping files are present

### Debug Mode

Enable debug logging for detailed information:

```bash
# Add debug logging
-Dlogging.level.com.oracle.coherence=DEBUG
-Dlogging.level.com.google.cloud=DEBUG
```

## Performance Considerations

### WireMock Mode
- **Execution Time**: ~100ms per test
- **Resource Usage**: Minimal
- **Network**: No external calls

### Real GCS Mode
- **Execution Time**: ~1-3 seconds per test
- **Resource Usage**: Network bandwidth for downloads
- **Costs**: Google Cloud Storage request charges apply

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Google Cloud Storage Integration Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Run WireMock Integration Tests
      run: |
        cd loaders/coherence-rag-google-cloud-storage-loader
        mvn clean verify -Dit.test="*IT"
```

### Jenkins Example

```groovy
pipeline {
    agent any
    
    stages {
        stage('Test') {
            steps {
                script {
                    dir('loaders/coherence-rag-google-cloud-storage-loader') {
                        sh 'mvn clean verify -Dit.test="*IT"'
                    }
                }
            }
        }
    }
}
```

## Google Cloud Storage API Details

### REST API Endpoints

The integration tests mock the following Google Cloud Storage REST API endpoints:

1. **Object Metadata**: `GET /storage/v1/b/{bucket}/o/{object}`
2. **Object Content**: `GET /storage/v1/b/{bucket}/o/{object}?alt=media`

### Authentication

- **WireMock Mode**: No authentication required
- **Real GCS Mode**: Uses Google Cloud authentication (service account, ADC, etc.)

## Best Practices

1. **Use WireMock for Development**: Fast feedback loops
2. **Use Real GCS for Pre-Production**: Validate against actual service
3. **Capture Responses Periodically**: Keep WireMock stubs up-to-date
4. **Monitor Costs**: Real GCS tests incur charges
5. **Version Control Mappings**: Include WireMock files in repository
6. **Test Error Scenarios**: Verify error handling works correctly

## Security Considerations

1. **Credential Management**: Never commit real service account keys
2. **Test Data**: Use non-sensitive test documents
3. **Access Control**: Limit GCS bucket permissions for test accounts
4. **Network Security**: Use VPC endpoints for enhanced security

## Service Account Permissions

For real GCS mode, the service account needs the following permissions:

```json
{
  "bindings": [
    {
      "role": "roles/storage.objectViewer",
      "members": [
        "serviceAccount:test-service-account@project-id.iam.gserviceaccount.com"
      ]
    }
  ]
}
```

## Maintenance

1. **Regular Updates**: Keep WireMock mappings current
2. **Credential Rotation**: Update service account keys periodically
3. **Cleanup**: Remove unused test data and old responses
4. **Documentation**: Keep this guide updated with changes 