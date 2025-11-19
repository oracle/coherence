# Document Loaders

Document loader implementations for the Oracle Coherence RAG framework, enabling document ingestion from various cloud storage services and repositories.

## Overview

The loaders module provides implementations of the `DocumentLoader` interface for different document sources. Each loader handles authentication, retrieval, and initial processing of documents before they are chunked and converted to vector embeddings.

## Available Loaders

### Cloud Storage Loaders

- **[AWS S3](coherence-rag-aws-s3-loader/)** - Load documents from Amazon S3
- **[Azure Blob Storage](coherence-rag-azure-blob-storage-loader/)** - Load documents from Azure Blob Storage
- **[Google Cloud Storage](coherence-rag-google-cloud-storage-loader/)** - Load documents from Google Cloud Storage
- **[OCI Object Storage](coherence-rag-oci-object-storage-loader/)** - Load documents from Oracle Cloud Infrastructure Object Storage

### Core Loaders (in coherence-rag module)

- **File Loader** - Load documents from local filesystem
- **HTTP Loader** - Load documents from HTTP URLs
- **HTTPS Loader** - Load documents from HTTPS URLs

## Features

### 🔐 **Authentication**
- Standard cloud provider authentication methods
- Service account and credential file support
- Environment variable and system property configuration
- Automatic credential chain resolution

### 📄 **Document Processing**
- Multiple document format support (PDF, Word, text, etc.)
- Automatic content type detection
- Metadata extraction and preservation
- Integration with Apache Tika for document parsing

### 🌐 **Cloud Integration**
- Native SDK integration for optimal performance
- Region-aware configuration
- Error handling and retry logic
- Secure communication with SSL/TLS

### ⚙️ **Configuration**
- Flexible configuration through multiple sources
- CDI integration for dependency injection
- Environment-specific settings
- Connection pooling and optimization

## Quick Start

### Dependencies

Add the specific loader dependency to your `pom.xml`:

```xml
<!-- For AWS S3 -->
<dependency>
    <groupId>com.oracle.coherence.ce</groupId>
    <artifactId>coherence-rag-aws-s3-loader</artifactId>
    <version>25.09</version>
</dependency>

<!-- For Azure Blob Storage -->
<dependency>
    <groupId>com.oracle.coherence.ce</groupId>
    <artifactId>coherence-rag-azure-blob-storage-loader</artifactId>
    <version>25.09</version>
</dependency>

<!-- For Google Cloud Storage -->
<dependency>
    <groupId>com.oracle.coherence.ce</groupId>
    <artifactId>coherence-rag-google-cloud-storage-loader</artifactId>
    <version>25.09</version>
</dependency>

<!-- For OCI Object Storage -->
<dependency>
    <groupId>com.oracle.coherence.ce</groupId>
    <artifactId>coherence-rag-oci-object-storage-loader</artifactId>
    <version>25.09</version>
</dependency>
```

### Basic Usage

```java
// AWS S3 Loader
DocumentLoader s3Loader = new AwsS3DocumentLoader();
Collection<Document> documents = s3Loader.load("s3://bucket-name/path/to/document.pdf");

// Azure Blob Storage Loader
DocumentLoader azureLoader = new AzureBlobStorageDocumentLoader();
Collection<Document> documents = azureLoader.load("azure.blob://container/path/to/document.pdf");

// Google Cloud Storage Loader
DocumentLoader gcsLoader = new GoogleCloudStorageDocumentLoader();
Collection<Document> documents = gcsLoader.load("gcs://bucket-name/path/to/document.pdf");

// OCI Object Storage Loader
DocumentLoader ociLoader = new OciObjectStorageDocumentLoader();
Collection<Document> documents = ociLoader.load("oci.os://namespace/bucket/path/to/document.pdf");
```

## URI Formats

Each loader supports a specific URI format for identifying documents:

| Service | URI Format | Example |
|---------|------------|---------|
| AWS S3 | `s3://bucket-name/path/to/object` | `s3://my-docs/manuals/user-guide.pdf` |
| Azure Blob | `azure.blob://container-name/path/to/blob` | `azure.blob://documents/specs/technical.pdf` |
| Google Cloud | `gcs://bucket-name/path/to/object` | `gcs://company-docs/reports/annual.pdf` |
| OCI Object Storage | `oci.os://namespace/bucket-name/path/to/object` | `oci.os://mycompany/docs/whitepaper.pdf` |

## Authentication

### AWS S3 Authentication

```bash
# Environment variables
export AWS_ACCESS_KEY_ID="your-access-key"
export AWS_SECRET_ACCESS_KEY="your-secret-key"
export AWS_REGION="us-east-1"

# Or use AWS credentials file
# ~/.aws/credentials
```

### Azure Blob Storage Authentication

```bash
# Connection string (preferred for development)
export AZURE_STORAGE_CONNECTION_STRING="DefaultEndpointsProtocol=https;AccountName=..."

# Or use managed identity (recommended for production)
export AZURE_CLIENT_ID="your-client-id"
```

### Google Cloud Storage Authentication

```bash
# Service account key file
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account-key.json"

# Or use default credentials (gcloud auth application-default login)
```

### OCI Object Storage Authentication

```bash
# OCI config file (default: ~/.oci/config)
export OCI_CONFIG_FILE="~/.oci/config"
export OCI_CONFIG_PROFILE="DEFAULT"

# Or use instance principals on OCI compute instances
```

## Configuration

### System Properties

```bash
# AWS Configuration
-Daws.region=us-east-1
-Daws.accessKeyId=your-key
-Daws.secretAccessKey=your-secret

# Azure Configuration
-Dazure.storage.connection-string="connection-string"

# Google Cloud Configuration
-Dgoogle.application.credentials="/path/to/key.json"

# OCI Configuration
-Doci.config.file="~/.oci/config"
-Doci.config.profile="DEFAULT"
```

### CDI Integration

```java
@Inject
@Named("s3")
private DocumentLoader s3Loader;

@Inject
@Named("azure")
private DocumentLoader azureLoader;

@Inject
@Named("gcs")
private DocumentLoader gcsLoader;

@Inject
@Named("oci")
private DocumentLoader ociLoader;
```

## Supported Document Formats

All loaders support the following document formats through Apache Tika integration:

- **PDF** - Portable Document Format
- **Microsoft Word** - .doc, .docx
- **Microsoft Excel** - .xls, .xlsx
- **Microsoft PowerPoint** - .ppt, .pptx
- **OpenDocument** - .odt, .ods, .odp
- **Plain Text** - .txt
- **HTML** - .html, .htm
- **Markdown** - .md
- **Rich Text Format** - .rtf
- **XML** - .xml
- **JSON** - .json
- **CSV** - .csv

## Metadata Handling

Loaders automatically extract and preserve metadata from both the document content and the storage service:

```java
Document document = loader.load("s3://bucket/document.pdf").iterator().next();

// Document metadata
String title = document.getMetadata("title");
String author = document.getMetadata("author");
String subject = document.getMetadata("subject");

// Storage metadata
String storageClass = document.getMetadata("storage-class");
String contentType = document.getMetadata("content-type");
Long fileSize = document.getMetadata("content-length");
String lastModified = document.getMetadata("last-modified");
```

## Performance Optimization

### Connection Pooling

```java
// Configure connection pooling (example for AWS S3)
S3ClientBuilder builder = S3Client.builder()
    .httpClientBuilder(ApacheHttpClient.builder()
        .maxConnections(100)
        .connectionTimeout(Duration.ofSeconds(30))
        .socketTimeout(Duration.ofSeconds(30)))
    .build();
```

### Batch Processing

```java
// Process multiple documents efficiently
List<String> documentUris = Arrays.asList(
    "s3://bucket/doc1.pdf",
    "s3://bucket/doc2.pdf",
    "s3://bucket/doc3.pdf"
);

List<Document> allDocuments = new ArrayList<>();
for (String uri : documentUris) {
    allDocuments.addAll(loader.load(uri));
}
```

### Caching

```java
// Enable caching for frequently accessed documents
@Cacheable("documents")
public Collection<Document> loadCachedDocument(String uri) {
    return loader.load(uri);
}
```

## Error Handling

### Retry Logic

```java
// Configure retry behavior
int maxRetries = 3;
Duration retryDelay = Duration.ofSeconds(1);

for (int attempt = 0; attempt < maxRetries; attempt++) {
    try {
        return loader.load(uri);
    } catch (Exception e) {
        if (attempt == maxRetries - 1) {
            throw e; // Final attempt failed
        }
        Thread.sleep(retryDelay.toMillis());
    }
}
```

### Exception Handling

```java
try {
    Collection<Document> documents = loader.load(uri);
} catch (DocumentNotFoundException e) {
    // Handle missing document
} catch (AuthenticationException e) {
    // Handle authentication failure
} catch (StorageException e) {
    // Handle storage service error
}
```

## Development

### Building All Loaders

```bash
mvn clean install
```

### Building Specific Loader

```bash
mvn clean install -pl coherence-rag-aws-s3-loader
```

### Running Tests

```bash
mvn test
```

### Integration Tests

```bash
# Run integration tests (requires actual cloud credentials)
mvn verify -P integration-tests
```

## Troubleshooting

### Common Issues

1. **Authentication failures**: Check credential configuration
2. **Network timeouts**: Increase timeout values
3. **Document not found**: Verify URI format and permissions
4. **Large documents**: Consider streaming for very large files

### Debug Mode

```bash
# Enable debug logging
export COHERENCE_RAG_LOG_LEVEL=DEBUG

# Specific loader debugging
export AWS_SDK_LOG_LEVEL=DEBUG
export AZURE_SDK_LOG_LEVEL=DEBUG
```

## License

This project is licensed under the Universal Permissive License (UPL) 1.0. 
