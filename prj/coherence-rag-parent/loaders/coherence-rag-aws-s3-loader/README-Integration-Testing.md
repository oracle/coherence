# AWS S3 Document Loader Integration Testing

This document provides comprehensive guidance for running integration tests for the AWS S3 Document Loader component.

## Overview

The AWS S3 Document Loader integration tests support two modes:

1. **WireMock Mode (Default)**: Uses WireMock stubs for offline testing
2. **Real S3 Mode**: Tests against actual AWS S3 service

## Test Modes

### WireMock Mode (Offline Testing)

**When to use**: Development, CI/CD, offline testing
**Triggered by**: `aws.bucket.name` system property starting with "test"
**Benefits**: Fast, no AWS costs, works offline

### Real S3 Mode

**When to use**: Pre-production validation, end-to-end testing
**Triggered by**: `aws.bucket.name` system property not starting with "test"
**Benefits**: Tests against real AWS S3 service

## Configuration

### WireMock Mode Configuration

```bash
# System properties for WireMock mode
-Daws.bucket.name=test-bucket
-Daws.region=us-east-1
-Daws.access.key.id=test-key
-Daws.secret.access.key=test-secret
-Daws.endpoint.url=http://localhost:8089
```

### Real S3 Mode Configuration

```bash
# System properties for Real S3 mode
-Daws.bucket.name=coherence-rag
-Daws.region=us-east-1
# AWS credentials via standard credential chain
```

## AWS Credentials (Real S3 Mode)

The real S3 mode uses the standard AWS credential chain:

1. **Environment Variables**:
   ```bash
   export AWS_ACCESS_KEY_ID=your-access-key
   export AWS_SECRET_ACCESS_KEY=your-secret-key
   ```

2. **AWS Credentials File** (~/.aws/credentials):
   ```ini
   [default]
   aws_access_key_id = your-access-key
   aws_secret_access_key = your-secret-key
   ```

3. **AWS Config File** (~/.aws/config):
   ```ini
   [default]
   region = us-east-1
   ```

4. **IAM Roles** (when running on EC2 or ECS)

## Running Integration Tests

### Method 1: Maven Command Line

```bash
# Run with WireMock mode (default)
mvn clean test -Dtest="*IntegrationTest" \
  -Daws.bucket.name=test-bucket \
  -Daws.region=us-east-1 \
  -Daws.access.key.id=test-key \
  -Daws.secret.access.key=test-secret \
  -Daws.endpoint.url=http://localhost:8089

# Run with Real S3 mode
mvn clean test -Dtest="*IntegrationTest" \
  -Daws.bucket.name=coherence-rag \
  -Daws.region=us-east-1
```

### Method 2: IDE Configuration

**IntelliJ IDEA / Eclipse**:
1. Open Run/Debug Configuration
2. Add VM options:
   ```
   -Daws.bucket.name=test-bucket
   -Daws.region=us-east-1
   -Daws.access.key.id=test-key
   -Daws.secret.access.key=test-secret
   -Daws.endpoint.url=http://localhost:8089
   ```

## WireMock Response Capture

To capture responses from real S3 API for offline testing:

### 1. Start WireMock Proxy

```bash
# Start WireMock proxy to capture S3 responses
mvn exec:java@wiremock-proxy

# Or manually with explicit parameters
java -jar wiremock-standalone-3.13.1.jar \
  --proxy-all=https://coherence-rag.s3.us-east-1.amazonaws.com \
  --record-mappings \
  --port=8089 \
  --verbose \
  --root-dir=target/wiremock
```

### 2. Configure Test for Capture

```bash
# Run test with proxy endpoint to capture responses
mvn test -Dtest="*IntegrationTest" \
  -Daws.bucket.name=coherence-rag \
  -Daws.region=us-east-1 \
  -Daws.endpoint.url=http://localhost:8089
```

### 3. Copy Captured Files

```bash
# Copy captured mappings and responses
cp target/wiremock/mappings/* src/test/resources/wiremock/mappings/
cp target/wiremock/__files/* src/test/resources/wiremock/__files/
```

## Test Data

### WireMock Mode Test Data

- **Bucket**: `test-bucket`
- **Object Key**: `test-documents/sample.pdf`
- **Mock Response**: Simple PDF document with test content

### Real S3 Mode Test Data

- **Bucket**: `coherence-rag`
- **Object Key**: `coherence/en/middleware/fusion-middleware/coherence/14.1.2/administer-http-sessions/administering-http-session-management-oracle-coherenceweb.pdf`
- **Response**: Real PDF document from S3

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
=== AWS S3 Integration Test Configuration ===
Mode: WireMock (Offline)
Bucket: test-bucket
Region: us-east-1
Endpoint: http://localhost:8089
===============================================

Document loaded successfully:
  Text length: 67
  Content type: application/pdf
  Content length: 12345
  ETag: "d41d8cd98f00b204e9800998ecf8427e"
```

### Real S3 Mode

```
=== AWS S3 Integration Test Configuration ===
Mode: Real S3
Bucket: coherence-rag
Region: us-east-1
Endpoint: default
===============================================

Document loaded successfully:
  Text length: 251134
  Content type: application/pdf
  Content length: 1857231
  ETag: "actual-etag-value"
```

## Troubleshooting

### Common Issues

1. **Authentication Errors**:
   - Verify AWS credentials are configured correctly
   - Check IAM permissions for S3 access

2. **Network Issues**:
   - Verify internet connectivity for real S3 mode
   - Check firewall settings for WireMock proxy

3. **Missing Test Data**:
   - Ensure test documents exist in S3 bucket
   - Verify bucket permissions

4. **WireMock Issues**:
   - Check WireMock is running on correct port
   - Verify mapping files are present

### Debug Mode

Enable debug logging for detailed information:

```bash
# Add debug logging
-Dlogging.level.com.oracle.coherence=DEBUG
-Dlogging.level.software.amazon.awssdk=DEBUG
```

## Performance Considerations

### WireMock Mode
- **Execution Time**: ~100ms per test
- **Resource Usage**: Minimal
- **Network**: No external calls

### Real S3 Mode
- **Execution Time**: ~1-3 seconds per test
- **Resource Usage**: Network bandwidth for downloads
- **Costs**: AWS S3 request charges apply

## CI/CD Integration

### GitHub Actions Example

```yaml
name: AWS S3 Integration Tests

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
        cd loaders/coherence-rag-aws-s3-loader
        mvn test -Dtest="*IntegrationTest" \
          -Daws.bucket.name=test-bucket \
          -Daws.region=us-east-1 \
          -Daws.access.key.id=test-key \
          -Daws.secret.access.key=test-secret \
          -Daws.endpoint.url=http://localhost:8089
```

### Jenkins Example

```groovy
pipeline {
    agent any
    
    stages {
        stage('Test') {
            steps {
                script {
                    dir('loaders/coherence-rag-aws-s3-loader') {
                        sh '''
                            mvn clean test -Dtest="*IntegrationTest" \
                              -Daws.bucket.name=test-bucket \
                              -Daws.region=us-east-1 \
                              -Daws.access.key.id=test-key \
                              -Daws.secret.access.key=test-secret \
                              -Daws.endpoint.url=http://localhost:8089
                        '''
                    }
                }
            }
        }
    }
}
```

## Best Practices

1. **Use WireMock for Development**: Fast feedback loops
2. **Use Real S3 for Pre-Production**: Validate against actual service
3. **Capture Responses Periodically**: Keep WireMock stubs up-to-date
4. **Monitor Costs**: Real S3 tests incur charges
5. **Version Control Mappings**: Include WireMock files in repository
6. **Test Error Scenarios**: Verify error handling works correctly

## Security Considerations

1. **Credential Management**: Never commit real AWS credentials
2. **Test Data**: Use non-sensitive test documents
3. **Access Control**: Limit S3 bucket permissions for test accounts
4. **Network Security**: Use VPC endpoints for enhanced security

## Maintenance

1. **Regular Updates**: Keep WireMock mappings current
2. **Credential Rotation**: Update test credentials periodically
3. **Cleanup**: Remove unused test data and old responses
4. **Documentation**: Keep this guide updated with changes 