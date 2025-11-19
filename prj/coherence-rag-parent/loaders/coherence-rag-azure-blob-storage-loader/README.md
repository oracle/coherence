# Azure Blob Storage Document Loader

Document loader implementation for Microsoft Azure Blob Storage, enabling seamless document ingestion from Azure storage containers into the Oracle Coherence RAG framework.

## Overview

The Azure Blob Storage Document Loader provides integration with Microsoft Azure Blob Storage for loading documents into the RAG framework. It supports various Azure authentication methods, automatic content type detection, and metadata preservation from blob properties and tags.

## Features

### 🔐 **Authentication**
- Connection string authentication
- Managed identity support (recommended for production)
- Service principal with client secret
- Service principal with certificate
- Azure CLI credentials
- Default Azure credential chain

### 📄 **Document Processing**
- Multiple document formats via Apache Tika
- Automatic content type detection
- Blob metadata and tags preservation
- Large file streaming support
- Hot, cool, and archive tier support

### 🌐 **Azure Integration**
- Azure SDK for Java integration
- Region-aware configuration
- Azure Data Lake Storage Gen2 support
- Custom endpoint support (sovereign clouds)
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
    <artifactId>coherence-rag-azure-blob-storage-loader</artifactId>
    <version>25.09</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
// Create Azure Blob Storage document loader
DocumentLoader loader = new AzureBlobStorageDocumentLoader();

// Load document from Azure Blob Storage
Collection<Document> documents = loader.load("azure.blob://container/documents/manual.pdf");

// Process multiple documents
List<String> azureUris = Arrays.asList(
    "azure.blob://documents/docs/doc1.pdf",
    "azure.blob://documents/docs/doc2.docx",
    "azure.blob://documents/docs/doc3.txt"
);

List<Document> allDocuments = new ArrayList<>();
for (String uri : azureUris) {
    allDocuments.addAll(loader.load(uri));
}
```

### URI Format

```
azure.blob://container-name/path/to/blob
```

Examples:
- `azure.blob://company-docs/manuals/user-guide.pdf`
- `azure.blob://technical-specs/architecture/design.docx`
- `azure.blob://knowledge-base/articles/machine-learning.md`

## Authentication

### Connection String (Development)

```bash
# Environment variable (preferred)
export AZURE_STORAGE_CONNECTION_STRING="DefaultEndpointsProtocol=https;AccountName=myaccount;AccountKey=mykey;EndpointSuffix=core.windows.net"

# System property
-Dazure.storage.connection-string="DefaultEndpointsProtocol=https;AccountName=..."
```

### Managed Identity (Production)

```bash
# For user-assigned managed identity
export AZURE_CLIENT_ID="your-managed-identity-client-id"

# For system-assigned managed identity (no additional configuration needed)
```

### Service Principal

```bash
# Service principal with client secret
export AZURE_CLIENT_ID="your-client-id"
export AZURE_CLIENT_SECRET="your-client-secret"
export AZURE_TENANT_ID="your-tenant-id"

# Service principal with certificate
export AZURE_CLIENT_ID="your-client-id"
export AZURE_CLIENT_CERTIFICATE_PATH="/path/to/certificate.pfx"
export AZURE_CLIENT_CERTIFICATE_PASSWORD="certificate-password"
export AZURE_TENANT_ID="your-tenant-id"
```

### Azure CLI Credentials

```bash
# Login with Azure CLI
az login

# The loader will automatically use CLI credentials if available
```

## Configuration

### CDI Configuration

```java
@ApplicationScoped
public class AzureBlobConfiguration {
    
    @Produces
    @Named("azure")
    public DocumentLoader createAzureLoader() {
        return new AzureBlobStorageDocumentLoader();
    }
    
    @Produces
    public BlobServiceClient createBlobServiceClient() {
        String connectionString = System.getenv("AZURE_STORAGE_CONNECTION_STRING");
        return new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient();
    }
}
```

### Custom Blob Service Client

```java
// Configure custom client with managed identity
BlobServiceClient customClient = new BlobServiceClientBuilder()
    .endpoint("https://myaccount.blob.core.windows.net")
    .credential(new DefaultAzureCredentialBuilder().build())
    .httpClient(new NettyAsyncHttpClientBuilder()
        .connectionTimeout(Duration.ofSeconds(30))
        .readTimeout(Duration.ofSeconds(60))
        .build())
    .buildClient();

AzureBlobStorageDocumentLoader loader = new AzureBlobStorageDocumentLoader(customClient);
```

### Connection Pooling

```java
// Configure HTTP client with connection pooling
HttpClient httpClient = new NettyAsyncHttpClientBuilder()
    .connectionTimeout(Duration.ofSeconds(30))
    .readTimeout(Duration.ofSeconds(60))
    .writeTimeout(Duration.ofSeconds(60))
    .responseTimeout(Duration.ofSeconds(120))
    .build();

BlobServiceClient client = new BlobServiceClientBuilder()
    .connectionString(connectionString)
    .httpClient(httpClient)
    .buildClient();
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

The loader automatically extracts and preserves metadata from both the document content and Azure blob:

### Azure Blob Metadata

```java
Document document = loader.load("azure.blob://container/document.pdf").iterator().next();

// Azure-specific metadata
String container = document.getMetadata("azure-container");
String blobName = document.getMetadata("azure-blob-name");
String etag = document.getMetadata("azure-etag");
String blobType = document.getMetadata("azure-blob-type");
String accessTier = document.getMetadata("azure-access-tier");
String contentType = document.getMetadata("content-type");
Long contentLength = document.getMetadata("content-length");
String lastModified = document.getMetadata("last-modified");

// Custom blob metadata
String customValue = document.getMetadata("custom-field");

// Blob tags
String tagValue = document.getMetadata("tag:category");
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
    Collection<Document> documents = loader.load("azure.blob://container/document.pdf");
} catch (BlobStorageException e) {
    if (e.getStatusCode() == 404) {
        // Handle blob not found
        logger.warn("Azure blob not found: {}", e.getMessage());
    } else if (e.getStatusCode() == 403) {
        // Handle access denied
        logger.error("Access denied to Azure blob: {}", e.getMessage());
    } else {
        // Handle other Azure errors
        logger.error("Azure Blob Storage error: {}", e.getMessage());
    }
} catch (ClientAuthenticationException e) {
    // Handle authentication failures
    logger.error("Azure authentication failed: {}", e.getMessage());
} catch (Exception e) {
    // Handle other errors
    logger.error("Unexpected error: {}", e.getMessage());
}
```

### Retry Configuration

```java
// Configure retry policy
RequestRetryOptions retryOptions = new RequestRetryOptions(
    RetryPolicyType.EXPONENTIAL,
    3,          // max retries
    Duration.ofSeconds(1),   // retry delay
    Duration.ofSeconds(30),  // max retry delay
    Duration.ofSeconds(120), // timeout per try
    null        // secondary host
);

BlobServiceClient client = new BlobServiceClientBuilder()
    .connectionString(connectionString)
    .retryOptions(retryOptions)
    .buildClient();
```

## Performance Optimization

### Parallel Processing

```java
// Process multiple documents in parallel
List<String> azureUris = getDocumentUris();
List<CompletableFuture<Collection<Document>>> futures = azureUris.stream()
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
// Cache documents to avoid repeated Azure calls
private final LoadingCache<String, Collection<Document>> documentCache = 
    Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofHours(1))
        .build(uri -> loader.load(uri));

public Collection<Document> loadCachedDocument(String azureUri) {
    return documentCache.get(azureUri);
}
```

### Streaming Large Files

```java
// For very large files, consider streaming
public void processLargeDocument(String azureUri) {
    BlobClient blobClient = getBlobClient(azureUri);
    
    try (BlobInputStream inputStream = blobClient.openInputStream()) {
        // Process stream without loading entire file into memory
        processDocumentStream(inputStream);
    }
}
```

## Storage Tiers

### Working with Different Access Tiers

```java
// Check blob access tier
BlobClient blobClient = getBlobClient(azureUri);
BlobProperties properties = blobClient.getProperties();
AccessTier tier = properties.getAccessTier();

if (tier == AccessTier.ARCHIVE) {
    // Rehydrate archived blob before reading
    blobClient.setAccessTier(AccessTier.HOT);
    
    // Wait for rehydration to complete
    while (blobClient.getProperties().getArchiveStatus() == ArchiveStatus.REHYDRATE_PENDING_TO_HOT) {
        Thread.sleep(1000);
    }
}

// Now load the document
Collection<Document> documents = loader.load(azureUri);
```

## Testing

### Unit Tests

```java
@Test
public void testAzureBlobDocumentLoading() {
    // Create mock blob service client for testing
    BlobServiceClient mockClient = Mockito.mock(BlobServiceClient.class);
    BlobContainerClient mockContainer = Mockito.mock(BlobContainerClient.class);
    BlobClient mockBlob = Mockito.mock(BlobClient.class);
    
    // Configure mock responses
    when(mockClient.getBlobContainerClient("container")).thenReturn(mockContainer);
    when(mockContainer.getBlobClient("document.pdf")).thenReturn(mockBlob);
    when(mockBlob.openInputStream()).thenReturn(createMockInputStream());
    
    AzureBlobStorageDocumentLoader loader = new AzureBlobStorageDocumentLoader(mockClient);
    Collection<Document> documents = loader.load("azure.blob://container/document.pdf");
    
    assertThat(documents).hasSize(1);
    assertThat(documents.iterator().next().getText()).isNotEmpty();
}
```

### Integration Tests

```java
@Test
@EnabledIf("hasAzureCredentials")
public void testRealAzureIntegration() {
    AzureBlobStorageDocumentLoader loader = new AzureBlobStorageDocumentLoader();
    
    // Test with real Azure container (requires credentials)
    Collection<Document> documents = loader.load("azure.blob://test-container/sample.pdf");
    
    assertThat(documents).isNotEmpty();
}

private static boolean hasAzureCredentials() {
    return System.getenv("AZURE_STORAGE_CONNECTION_STRING") != null ||
           System.getenv("AZURE_CLIENT_ID") != null;
}
```

## Troubleshooting

### Common Issues

1. **Connection string not found**
   ```
   Solution: Set AZURE_STORAGE_CONNECTION_STRING environment variable
   ```

2. **Access denied (403)**
   ```
   Solution: Verify Storage Blob Data Reader role assignment for managed identity
   ```

3. **Blob not found (404)**
   ```
   Solution: Verify container name and blob name in the Azure URI
   ```

4. **Managed identity not found**
   ```
   Solution: Ensure managed identity is assigned to the resource (VM, App Service, etc.)
   ```

5. **Archive tier access**
   ```
   Solution: Rehydrate archived blobs before accessing content
   ```

### Debug Logging

```bash
# Enable Azure SDK debug logging
export AZURE_LOG_LEVEL=DEBUG

# Enable Coherence RAG debug logging
export COHERENCE_RAG_LOG_LEVEL=DEBUG
```

```java
// Enable debug logging in code
System.setProperty("org.slf4j.simpleLogger.log.com.azure", "DEBUG");
```

## Best Practices

### Security
- Use managed identities in production environments
- Apply principle of least privilege (Storage Blob Data Reader role)
- Enable storage account firewall and virtual network rules
- Use private endpoints for secure access

### Performance
- Use appropriate access tiers for data lifecycle
- Implement caching for frequently accessed documents
- Consider Azure CDN for global document distribution
- Use streaming for large files

### Cost Optimization
- Use lifecycle management policies for automatic tiering
- Monitor blob access patterns and optimize tiers
- Consider blob index tags for metadata-based queries
- Use Azure Storage reserved capacity for predictable workloads

## Azure Storage Features

### Blob Index Tags

```java
// Set blob tags for better organization
Map<String, String> tags = Map.of(
    "category", "technical",
    "department", "engineering",
    "confidentiality", "internal"
);

blobClient.setTags(tags);
```

### Soft Delete and Versioning

```java
// Enable soft delete protection
BlobContainerClient container = blobServiceClient.getBlobContainerClient("documents");
if (container.exists()) {
    // Soft delete is configured at storage account level
    logger.info("Container exists with soft delete protection");
}
```

## License

This project is licensed under the Universal Permissive License (UPL) 1.0. 
