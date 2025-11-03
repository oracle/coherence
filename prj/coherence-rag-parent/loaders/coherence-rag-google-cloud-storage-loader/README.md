# Google Cloud Storage Document Loader

Document loader implementation for Google Cloud Storage, enabling seamless document ingestion from GCS buckets into the Oracle Coherence RAG framework.

## Overview

The Google Cloud Storage Document Loader provides integration with Google Cloud Storage for loading documents into the RAG framework. It supports Google Cloud authentication methods, automatic content type detection, and metadata preservation from GCS object metadata.

## Features

### 🔐 **Authentication**
- Service account key file (GOOGLE_APPLICATION_CREDENTIALS)
- Compute Engine managed service account
- Google Cloud SDK credentials (gcloud auth)
- Workload identity (for GKE)
- Default credential chain resolution

### 📄 **Document Processing**
- Multiple document formats via Apache Tika
- Automatic content type detection
- GCS object metadata preservation
- Large file streaming with channel-based I/O
- Multi-regional and dual-regional bucket support

### 🌐 **Google Cloud Integration**
- Google Cloud Storage client libraries
- Region-aware configuration
- Storage classes support (Standard, Nearline, Coldline, Archive)
- Customer-managed encryption keys (CMEK) support
- Uniform bucket-level access support

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
    <artifactId>coherence-rag-google-cloud-storage-loader</artifactId>
    <version>25.09</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
// Create Google Cloud Storage document loader
DocumentLoader loader = new GoogleCloudStorageDocumentLoader();

// Load document from GCS
Collection<Document> documents = loader.load("gcs://my-bucket/documents/manual.pdf");

// Process multiple documents
List<String> gcsUris = Arrays.asList(
    "gcs://my-bucket/docs/doc1.pdf",
    "gcs://my-bucket/docs/doc2.docx",
    "gcs://my-bucket/docs/doc3.txt"
);

List<Document> allDocuments = new ArrayList<>();
for (String uri : gcsUris) {
    allDocuments.addAll(loader.load(uri));
}
```

### URI Format

```
gcs://bucket-name/path/to/object
```

Examples:
- `gcs://company-docs/manuals/user-guide.pdf`
- `gcs://technical-specs/architecture/design.docx`
- `gcs://knowledge-base/articles/machine-learning.md`

## Authentication

### Service Account Key File

```bash
# Environment variable (recommended)
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account-key.json"

# System property
-Dgoogle.application.credentials="/path/to/service-account-key.json"
```

Service account key JSON structure:
```json
{
  "type": "service_account",
  "project_id": "your-project-id",
  "private_key_id": "key-id",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "service-account@your-project.iam.gserviceaccount.com",
  "client_id": "client-id",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token"
}
```

### Compute Engine Managed Service Account

For Compute Engine VMs, no additional configuration is needed if the VM has the appropriate scopes:

```bash
# When creating VM, add required scopes
gcloud compute instances create my-vm \
    --scopes=https://www.googleapis.com/auth/cloud-platform
```

### Google Cloud SDK Credentials

```bash
# Login with Google Cloud SDK
gcloud auth application-default login

# Set default project
gcloud config set project your-project-id
```

### Workload Identity (GKE)

```yaml
# Configure workload identity in Kubernetes
apiVersion: v1
kind: ServiceAccount
metadata:
  name: gcs-loader-sa
  annotations:
    iam.gke.io/gcp-service-account: gcs-loader@your-project.iam.gserviceaccount.com
```

## Configuration

### CDI Configuration

```java
@ApplicationScoped
public class GoogleCloudStorageConfiguration {
    
    @Produces
    @Named("gcs")
    public DocumentLoader createGcsLoader() {
        return new GoogleCloudStorageDocumentLoader();
    }
    
    @Produces
    public Storage createStorageClient() throws IOException {
        return StorageOptions.getDefaultInstance().getService();
    }
}
```

### Custom Storage Client

```java
// Configure custom storage client
GoogleCredentials credentials = GoogleCredentials.fromStream(
    new FileInputStream("path/to/service-account.json"));

Storage customStorage = StorageOptions.newBuilder()
    .setCredentials(credentials)
    .setProjectId("your-project-id")
    .setTransportOptions(HttpTransportOptions.newBuilder()
        .setConnectTimeout(30000)
        .setReadTimeout(60000)
        .build())
    .build()
    .getService();

GoogleCloudStorageDocumentLoader loader = new GoogleCloudStorageDocumentLoader(customStorage);
```

### Channel-based I/O Configuration

```java
// Configure for efficient streaming
Storage storage = StorageOptions.newBuilder()
    .setCredentials(credentials)
    .setTransportOptions(HttpTransportOptions.newBuilder()
        .setHttpTransportFactory(new NetHttpTransport.Builder().build())
        .build())
    .build()
    .getService();
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

The loader automatically extracts and preserves metadata from both the document content and GCS object:

### GCS Object Metadata

```java
Document document = loader.load("gcs://bucket/document.pdf").iterator().next();

// GCS-specific metadata
String bucket = document.getMetadata("gcs-bucket");
String objectName = document.getMetadata("gcs-object-name");
String generation = document.getMetadata("gcs-generation");
String storageClass = document.getMetadata("gcs-storage-class");
String contentType = document.getMetadata("content-type");
Long contentLength = document.getMetadata("content-length");
String lastModified = document.getMetadata("last-modified");
String etag = document.getMetadata("gcs-etag");
String crc32c = document.getMetadata("gcs-crc32c");
String md5Hash = document.getMetadata("gcs-md5-hash");

// Custom metadata
String customValue = document.getMetadata("custom-field");
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
    Collection<Document> documents = loader.load("gcs://bucket/document.pdf");
} catch (StorageException e) {
    if (e.getCode() == 404) {
        // Handle object not found
        logger.warn("GCS object not found: {}", e.getMessage());
    } else if (e.getCode() == 403) {
        // Handle access denied
        logger.error("Access denied to GCS object: {}", e.getMessage());
    } else {
        // Handle other GCS errors
        logger.error("GCS error: {}", e.getMessage());
    }
} catch (GoogleJsonResponseException e) {
    // Handle API errors
    logger.error("Google API error: {}", e.getMessage());
} catch (IOException e) {
    // Handle I/O errors
    logger.error("I/O error: {}", e.getMessage());
}
```

### Retry Configuration

```java
// Configure retry settings
RetrySettings retrySettings = RetrySettings.newBuilder()
    .setMaxAttempts(3)
    .setInitialRetryDelay(Duration.ofSeconds(1))
    .setMaxRetryDelay(Duration.ofSeconds(30))
    .setRetryDelayMultiplier(2.0)
    .setTotalTimeout(Duration.ofMinutes(5))
    .build();

Storage storage = StorageOptions.newBuilder()
    .setCredentials(credentials)
    .setRetrySettings(retrySettings)
    .build()
    .getService();
```

## Performance Optimization

### Parallel Processing

```java
// Process multiple documents in parallel
List<String> gcsUris = getDocumentUris();
List<CompletableFuture<Collection<Document>>> futures = gcsUris.stream()
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
// Cache documents to avoid repeated GCS calls
private final LoadingCache<String, Collection<Document>> documentCache = 
    Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofHours(1))
        .build(uri -> loader.load(uri));

public Collection<Document> loadCachedDocument(String gcsUri) {
    return documentCache.get(gcsUri);
}
```

### Streaming Large Files

```java
// Use channel-based I/O for large files
public void processLargeDocument(String gcsUri) {
    BlobId blobId = BlobId.of(extractBucket(gcsUri), extractObjectName(gcsUri));
    
    try (ReadChannel reader = storage.reader(blobId);
         InputStream inputStream = Channels.newInputStream(reader)) {
        
        // Process stream without loading entire file into memory
        processDocumentStream(inputStream);
    }
}
```

## Storage Classes and Lifecycle

### Working with Different Storage Classes

```java
// Check object storage class
BlobId blobId = BlobId.of(bucket, objectName);
Blob blob = storage.get(blobId);
StorageClass storageClass = blob.getStorageClass();

if (storageClass == StorageClass.ARCHIVE) {
    // Archive objects need to be restored before reading
    logger.info("Object is in Archive storage class");
}

// Get object with specific generation
Blob specificVersion = storage.get(blobId, Storage.BlobGetOption.generationMatch(generation));
```

### Lifecycle Management

```java
// Example lifecycle rule for automatic storage class transitions
BucketInfo.LifecycleRule lifecycleRule = new BucketInfo.LifecycleRule(
    BucketInfo.LifecycleRule.LifecycleAction.newSetStorageClassAction(StorageClass.NEARLINE),
    BucketInfo.LifecycleRule.LifecycleCondition.newBuilder()
        .setAge(30)
        .build()
);
```

## Testing

### Unit Tests

```java
@Test
public void testGcsDocumentLoading() {
    // Create mock storage client for testing
    Storage mockStorage = Mockito.mock(Storage.class);
    Blob mockBlob = Mockito.mock(Blob.class);
    ReadChannel mockChannel = Mockito.mock(ReadChannel.class);
    
    // Configure mock responses
    when(mockStorage.get(any(BlobId.class))).thenReturn(mockBlob);
    when(mockStorage.reader(any(BlobId.class))).thenReturn(mockChannel);
    when(mockChannel.read(any(ByteBuffer.class))).thenReturn(-1); // EOF
    
    GoogleCloudStorageDocumentLoader loader = new GoogleCloudStorageDocumentLoader(mockStorage);
    Collection<Document> documents = loader.load("gcs://test-bucket/test-document.pdf");
    
    assertThat(documents).hasSize(1);
}
```

### Integration Tests

```java
@Test
@EnabledIf("hasGoogleCredentials")
public void testRealGcsIntegration() {
    GoogleCloudStorageDocumentLoader loader = new GoogleCloudStorageDocumentLoader();
    
    // Test with real GCS bucket (requires credentials)
    Collection<Document> documents = loader.load("gcs://test-bucket/sample.pdf");
    
    assertThat(documents).isNotEmpty();
}

private static boolean hasGoogleCredentials() {
    return System.getenv("GOOGLE_APPLICATION_CREDENTIALS") != null ||
           Files.exists(Paths.get(System.getProperty("user.home"), ".config", "gcloud", "application_default_credentials.json"));
}
```

## Troubleshooting

### Common Issues

1. **Credentials not found**
   ```
   Solution: Set GOOGLE_APPLICATION_CREDENTIALS environment variable or run 'gcloud auth application-default login'
   ```

2. **Access denied (403)**
   ```
   Solution: Verify service account has Storage Object Viewer role
   ```

3. **Object not found (404)**
   ```
   Solution: Verify bucket name and object name in the GCS URI
   ```

4. **Project not set**
   ```
   Solution: Set GOOGLE_CLOUD_PROJECT environment variable or configure in StorageOptions
   ```

5. **Quota exceeded**
   ```
   Solution: Check Google Cloud Console quotas and increase limits if needed
   ```

### Debug Logging

```bash
# Enable Google Cloud SDK debug logging
export GOOGLE_CLOUD_LOG_LEVEL=DEBUG

# Enable Coherence RAG debug logging
export COHERENCE_RAG_LOG_LEVEL=DEBUG
```

```java
// Enable debug logging in code
System.setProperty("org.slf4j.simpleLogger.log.com.google.cloud", "DEBUG");
```

## Best Practices

### Security
- Use service accounts with minimal required permissions
- Enable uniform bucket-level access for consistent security
- Use customer-managed encryption keys (CMEK) for sensitive data
- Implement VPC Service Controls for additional security

### Performance
- Use regional buckets for better performance and lower costs
- Implement connection pooling for multiple requests
- Consider Cloud CDN for global document distribution
- Use parallel processing for bulk operations

### Cost Optimization
- Use appropriate storage classes based on access patterns
- Implement lifecycle policies for automatic cost optimization
- Monitor usage with Cloud Monitoring and billing alerts
- Consider committed use discounts for predictable workloads

## Google Cloud Storage Features

### Object Versioning

```java
// List all versions of an object
Page<Blob> blobs = storage.list(bucketName, 
    Storage.BlobListOption.prefix(objectName),
    Storage.BlobListOption.versions(true));

for (Blob blob : blobs.iterateAll()) {
    System.out.println("Generation: " + blob.getGeneration());
}
```

### Signed URLs

```java
// Generate signed URL for temporary access
BlobId blobId = BlobId.of(bucketName, objectName);
URL signedUrl = storage.signUrl(blobId, 1, TimeUnit.HOURS, 
    Storage.SignUrlOption.httpMethod(HttpMethod.GET));
```

### Event Notifications

```java
// Configure Cloud Pub/Sub notifications for bucket events
NotificationInfo notification = NotificationInfo.newBuilder("projects/project/topics/topic")
    .setEventTypes(NotificationInfo.EventType.OBJECT_FINALIZE)
    .setPayloadFormat(NotificationInfo.PayloadFormat.JSON_API_V1)
    .build();

Notification createdNotification = storage.createNotification(bucketName, notification);
```

## IAM Permissions

Required IAM roles for the service account:

```json
{
  "bindings": [
    {
      "role": "roles/storage.objectViewer",
      "members": [
        "serviceAccount:gcs-loader@your-project.iam.gserviceaccount.com"
      ]
    }
  ]
}
```

## License

This project is licensed under the Universal Permissive License (UPL) 1.0. 
