# AWS S3 Document Loader

Document loader implementation for Amazon S3 storage service, enabling seamless document ingestion from S3 buckets into the Oracle Coherence RAG framework.

## Overview

The AWS S3 Document Loader provides integration with Amazon S3 for loading documents into the RAG framework. It supports standard AWS authentication methods, automatic content type detection, and metadata preservation from S3 object metadata.

## Features

### 🔐 **Authentication**
- AWS credential chain support
- Environment variables and system properties
- AWS credentials file (~/.aws/credentials)
- IAM instance profile credentials
- Cross-account role assumption

### 📄 **Document Processing**
- Multiple document formats via Apache Tika
- Automatic content type detection
- S3 object metadata preservation
- Large file streaming support
- Multipart upload handling

### 🌐 **S3 Integration**
- AWS SDK v2 integration
- Region-aware configuration
- S3-compatible storage support
- Transfer acceleration support
- Server-side encryption support

### ⚙️ **Configuration**
- Flexible authentication configuration
- CDI integration
- Connection pooling optimization
- Retry logic with exponential backoff

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.oracle.coherence.ce</groupId>
    <artifactId>coherence-rag-aws-s3-loader</artifactId>
    <version>25.09</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
// Create S3 document loader
DocumentLoader loader = new AwsS3DocumentLoader();

// Load document from S3
Collection<Document> documents = loader.load("s3://my-bucket/documents/manual.pdf");

// Process multiple documents
List<String> s3Uris = Arrays.asList(
    "s3://my-bucket/docs/doc1.pdf",
    "s3://my-bucket/docs/doc2.docx",
    "s3://my-bucket/docs/doc3.txt"
);

List<Document> allDocuments = new ArrayList<>();
for (String uri : s3Uris) {
    allDocuments.addAll(loader.load(uri));
}
```

### URI Format

```
s3://bucket-name/path/to/object
```

Examples:
- `s3://company-docs/manuals/user-guide.pdf`
- `s3://technical-specs/architecture/design.docx`
- `s3://knowledge-base/articles/machine-learning.md`

## Authentication

### Environment Variables

```bash
# AWS credentials
export AWS_ACCESS_KEY_ID="your-access-key-id"
export AWS_SECRET_ACCESS_KEY="your-secret-access-key"
export AWS_SESSION_TOKEN="your-session-token"  # Optional for temporary credentials

# AWS region
export AWS_REGION="us-east-1"
export AWS_DEFAULT_REGION="us-east-1"
```

### System Properties

```bash
# AWS credentials
-Daws.accessKeyId=your-access-key-id
-Daws.secretAccessKey=your-secret-access-key
-Daws.sessionToken=your-session-token

# AWS region
-Daws.region=us-east-1
```

### AWS Credentials File

Create `~/.aws/credentials`:
```ini
[default]
aws_access_key_id = your-access-key-id
aws_secret_access_key = your-secret-access-key

[production]
aws_access_key_id = prod-access-key-id
aws_secret_access_key = prod-secret-access-key
region = us-west-2
```

Create `~/.aws/config`:
```ini
[default]
region = us-east-1
output = json

[profile production]
region = us-west-2
output = json
```

### IAM Instance Profile

For EC2 instances, attach an IAM role with S3 permissions:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:GetObjectMetadata",
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::your-bucket/*",
                "arn:aws:s3:::your-bucket"
            ]
        }
    ]
}
```

## Configuration

### CDI Configuration

```java
@ApplicationScoped
public class AwsS3Configuration {
    
    @Produces
    @Named("s3")
    public DocumentLoader createS3Loader() {
        return new AwsS3DocumentLoader();
    }
    
    @Produces
    public S3Client createS3Client() {
        return S3Client.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }
}
```

### Custom S3 Client

```java
// Configure custom S3 client
S3Client customClient = S3Client.builder()
    .region(Region.US_WEST_2)
    .credentialsProvider(StaticCredentialsProvider.create(
        AwsBasicCredentials.create("access-key", "secret-key")))
    .endpointOverride(URI.create("https://custom-s3-endpoint.com"))
    .httpClientBuilder(ApacheHttpClient.builder()
        .maxConnections(100)
        .connectionTimeout(Duration.ofSeconds(30)))
    .build();

AwsS3DocumentLoader loader = new AwsS3DocumentLoader(customClient);
```

### Connection Pooling

```java
// Configure HTTP client with connection pooling
ApacheHttpClient httpClient = ApacheHttpClient.builder()
    .maxConnections(50)
    .connectionTimeout(Duration.ofSeconds(30))
    .socketTimeout(Duration.ofSeconds(60))
    .connectionAcquisitionTimeout(Duration.ofSeconds(10))
    .useIdleConnectionReaper(true)
    .build();

S3Client s3Client = S3Client.builder()
    .httpClient(httpClient)
    .build();
```

## Supported Document Formats

The loader supports all document formats available through Apache Tika:

| Format | Extensions | MIME Types |
|--------|------------|------------|
| **PDF** | .pdf | application/pdf |
| **Microsoft Word** | .doc, .docx | application/msword, application/vnd.openxmlformats-officedocument.wordprocessingml.document |
| **Microsoft Excel** | .xls, .xlsx | application/vnd.ms-excel, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet |
| **Microsoft PowerPoint** | .ppt, .pptx | application/vnd.ms-powerpoint, application/vnd.openxmlformats-officedocument.presentationml.presentation |
| **Plain Text** | .txt | text/plain |
| **HTML** | .html, .htm | text/html |
| **Markdown** | .md | text/markdown |
| **Rich Text** | .rtf | application/rtf |
| **OpenDocument** | .odt, .ods, .odp | application/vnd.oasis.opendocument.* |

## Metadata Handling

The loader automatically extracts and preserves metadata from both the document content and S3 object:

### S3 Object Metadata

```java
Document document = loader.load("s3://bucket/document.pdf").iterator().next();

// S3-specific metadata
String bucket = document.getMetadata("s3-bucket");
String key = document.getMetadata("s3-key");
String etag = document.getMetadata("s3-etag");
String storageClass = document.getMetadata("s3-storage-class");
String contentType = document.getMetadata("content-type");
Long contentLength = document.getMetadata("content-length");
String lastModified = document.getMetadata("last-modified");

// Custom S3 metadata
String customValue = document.getMetadata("x-amz-meta-custom-field");
```

### Document Content Metadata

```java
// Document properties extracted by Tika
String title = document.getMetadata("title");
String author = document.getMetadata("author");
String subject = document.getMetadata("subject");
String creator = document.getMetadata("creator");
String keywords = document.getMetadata("keywords");
String language = document.getMetadata("language");
```

## Error Handling

### Exception Types

```java
try {
    Collection<Document> documents = loader.load("s3://bucket/document.pdf");
} catch (S3Exception e) {
    if (e.statusCode() == 404) {
        // Handle object not found
        logger.warn("S3 object not found: {}", e.getMessage());
    } else if (e.statusCode() == 403) {
        // Handle access denied
        logger.error("Access denied to S3 object: {}", e.getMessage());
    } else {
        // Handle other S3 errors
        logger.error("S3 error: {}", e.getMessage());
    }
} catch (NoCredentialsException e) {
    // Handle missing credentials
    logger.error("AWS credentials not found: {}", e.getMessage());
} catch (SdkException e) {
    // Handle SDK errors
    logger.error("AWS SDK error: {}", e.getMessage());
}
```

### Retry Configuration

```java
// Configure retry policy
RetryPolicy retryPolicy = RetryPolicy.builder()
    .numRetries(3)
    .backoffStrategy(BackoffStrategy.defaultStrategy())
    .throttlingBackoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
    .build();

S3Client s3Client = S3Client.builder()
    .overrideConfiguration(ClientOverrideConfiguration.builder()
        .retryPolicy(retryPolicy)
        .build())
    .build();
```

## Performance Optimization

### Parallel Processing

```java
// Process multiple documents in parallel
List<String> s3Uris = getDocumentUris();
List<CompletableFuture<Collection<Document>>> futures = s3Uris.stream()
    .map(uri -> CompletableFuture.supplyAsync(() -> loader.load(uri)))
    .collect(Collectors.toList());

List<Document> allDocuments = futures.stream()
    .flatMap(future -> {
        try {
            return future.get().stream();
        } catch (Exception e) {
            logger.error("Failed to load document", e);
            return Stream.empty();
        }
    })
    .collect(Collectors.toList());
```

### Caching

```java
// Cache documents to avoid repeated S3 calls
private final LoadingCache<String, Collection<Document>> documentCache = 
    Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofHours(1))
        .build(uri -> loader.load(uri));

public Collection<Document> loadCachedDocument(String s3Uri) {
    return documentCache.get(s3Uri);
}
```

### Streaming Large Files

```java
// For very large files, consider streaming
public void processLargeDocument(String s3Uri) {
    S3Object s3Object = s3Client.getObject(GetObjectRequest.builder()
        .bucket(extractBucket(s3Uri))
        .key(extractKey(s3Uri))
        .build());
    
    try (InputStream inputStream = s3Object) {
        // Process stream without loading entire file into memory
        processDocumentStream(inputStream);
    }
}
```

## Testing

### Unit Tests

```java
@Test
public void testS3DocumentLoading() {
    // Create mock S3 client for testing
    S3Client mockS3Client = Mockito.mock(S3Client.class);
    
    // Configure mock responses
    when(mockS3Client.getObject(any(GetObjectRequest.class)))
        .thenReturn(createMockS3Object());
    
    AwsS3DocumentLoader loader = new AwsS3DocumentLoader(mockS3Client);
    Collection<Document> documents = loader.load("s3://test-bucket/test-document.pdf");
    
    assertThat(documents).hasSize(1);
    assertThat(documents.iterator().next().getText()).isNotEmpty();
}
```

### Integration Tests

```java
@Test
@EnabledIf("hasAwsCredentials")
public void testRealS3Integration() {
    AwsS3DocumentLoader loader = new AwsS3DocumentLoader();
    
    // Test with real S3 bucket (requires credentials)
    Collection<Document> documents = loader.load("s3://test-bucket/sample.pdf");
    
    assertThat(documents).isNotEmpty();
}

private static boolean hasAwsCredentials() {
    return System.getenv("AWS_ACCESS_KEY_ID") != null;
}
```

## Troubleshooting

### Common Issues

1. **Credentials not found**
   ```
   Solution: Set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment variables
   ```

2. **Access denied (403)**
   ```
   Solution: Verify IAM permissions for s3:GetObject and s3:GetObjectMetadata
   ```

3. **Object not found (404)**
   ```
   Solution: Verify bucket name and object key in the S3 URI
   ```

4. **Region mismatch**
   ```
   Solution: Set AWS_REGION environment variable or configure region in code
   ```

5. **Connection timeouts**
   ```
   Solution: Increase connection timeout in HTTP client configuration
   ```

### Debug Logging

```bash
# Enable AWS SDK debug logging
export AWS_LOG_LEVEL=DEBUG

# Enable Coherence RAG debug logging
export COHERENCE_RAG_LOG_LEVEL=DEBUG
```

```java
// Enable debug logging in code
System.setProperty("aws.crt.log.level", "Debug");
System.setProperty("org.slf4j.simpleLogger.log.software.amazon.awssdk", "DEBUG");
```

## Best Practices

### Security
- Use IAM roles instead of access keys when possible
- Apply principle of least privilege for S3 permissions
- Enable S3 bucket versioning and access logging
- Use VPC endpoints for private S3 access

### Performance
- Use connection pooling for multiple requests
- Implement caching for frequently accessed documents
- Consider S3 Transfer Acceleration for global access
- Use multipart uploads for large files

### Cost Optimization
- Use appropriate S3 storage classes
- Implement lifecycle policies for document archival
- Monitor S3 request patterns and optimize
- Consider S3 Intelligent Tiering

## License

This project is licensed under the Universal Permissive License (UPL) 1.0. 
