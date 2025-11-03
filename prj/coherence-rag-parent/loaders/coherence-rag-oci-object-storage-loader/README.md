# OCI Object Storage Document Loader

Document loader implementation for Oracle Cloud Infrastructure Object Storage, enabling seamless document ingestion from OCI buckets into the Oracle Coherence RAG framework.

## Overview

The OCI Object Storage Document Loader provides integration with Oracle Cloud Infrastructure Object Storage for loading documents into the RAG framework. It supports various OCI authentication methods, automatic content type detection, and metadata preservation from object metadata.

## Features

### 🔐 **Authentication**
- Config file authentication (~/.oci/config)
- Instance principals (for compute instances)
- Resource principals (for functions, other services)
- User principals with API keys
- Security token authentication

### 📄 **Document Processing**
- Multiple document formats via Apache Tika
- Automatic content type detection
- Object metadata preservation
- Large file streaming support
- Cross-region bucket access

### 🌐 **OCI Integration**
- OCI SDK for Java integration
- Multi-region support
- Storage tiers (Standard, Infrequent Access, Archive)
- Customer-managed encryption keys
- Private endpoint support

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
    <artifactId>coherence-rag-oci-object-storage-loader</artifactId>
    <version>25.09</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
// Create OCI Object Storage document loader
DocumentLoader loader = new OciObjectStorageDocumentLoader();

// Load document from OCI Object Storage
Collection<Document> documents = loader.load("oci.os://namespace/bucket/documents/manual.pdf");

// Process multiple documents
List<String> ociUris = Arrays.asList(
    "oci.os://namespace/bucket/docs/doc1.pdf",
    "oci.os://namespace/bucket/docs/doc2.docx",
    "oci.os://namespace/bucket/docs/doc3.txt"
);

List<Document> allDocuments = new ArrayList<>();
for (String uri : ociUris) {
    allDocuments.addAll(loader.load(uri));
}
```

### URI Format

```
oci.os://namespace/bucket-name/path/to/object
```

Examples:
- `oci.os://mytenancy/company-docs/manuals/user-guide.pdf`
- `oci.os://mytenancy/technical-specs/architecture/design.docx`
- `oci.os://mytenancy/knowledge-base/articles/machine-learning.md`

## Authentication

### Config File Authentication

Create `~/.oci/config` file:

```ini
[DEFAULT]
user=ocid1.user.oc1..aaaaaaaafakeuser
fingerprint=aa:bb:cc:dd:ee:ff:00:11:22:33:44:55:66:77:88:99
tenancy=ocid1.tenancy.oc1..aaaaaaaafaketenancy
region=us-ashburn-1
key_file=~/.oci/oci_api_key.pem
```

### Instance Principals

For compute instances with instance principals enabled:

```java
// No additional configuration needed
OciObjectStorageDocumentLoader loader = new OciObjectStorageDocumentLoader();
```

### Resource Principals

For OCI Functions and other services:

```bash
# Set resource principal environment
export OCI_RESOURCE_PRINCIPAL_VERSION=2.2
```

### Environment Variables

```bash
# Alternative to config file
export OCI_CLI_USER=ocid1.user.oc1..aaaaaaaafakeuser
export OCI_CLI_FINGERPRINT=aa:bb:cc:dd:ee:ff:00:11:22:33:44:55:66:77:88:99
export OCI_CLI_TENANCY=ocid1.tenancy.oc1..aaaaaaaafaketenancy
export OCI_CLI_REGION=us-ashburn-1
export OCI_CLI_KEY_FILE=~/.oci/oci_api_key.pem
```

## Configuration

### CDI Configuration

```java
@ApplicationScoped
public class OciObjectStorageConfiguration {
    
    @Produces
    @Named("oci")
    public DocumentLoader createOciLoader() {
        return new OciObjectStorageDocumentLoader();
    }
    
    @Produces
    public ObjectStorageClient createObjectStorageClient() {
        return ObjectStorageClient.builder()
            .build(ConfigFileReader.parseDefault());
    }
}
```

### Custom Authentication

```java
// Instance principals
AuthenticationDetailsProvider instancePrincipals = 
    InstancePrincipalsAuthenticationDetailsProvider.builder().build();

ObjectStorageClient client = ObjectStorageClient.builder()
    .build(instancePrincipals);

OciObjectStorageDocumentLoader loader = new OciObjectStorageDocumentLoader(client);
```

## Error Handling

### Exception Types

```java
try {
    Collection<Document> documents = loader.load("oci.os://namespace/bucket/document.pdf");
} catch (BmcException e) {
    if (e.getStatusCode() == 404) {
        // Handle object not found
        logger.warn("OCI object not found: {}", e.getMessage());
    } else if (e.getStatusCode() == 401) {
        // Handle authentication error
        logger.error("OCI authentication failed: {}", e.getMessage());
    } else {
        // Handle other OCI errors
        logger.error("OCI error: {}", e.getMessage());
    }
} catch (Exception e) {
    // Handle other errors
    logger.error("Unexpected error: {}", e.getMessage());
}
```

## Testing

### Unit Tests

```java
@Test
public void testOciObjectStorageDocumentLoading() {
    // Create mock client for testing
    ObjectStorageClient mockClient = Mockito.mock(ObjectStorageClient.class);
    GetObjectResponse mockResponse = Mockito.mock(GetObjectResponse.class);
    
    // Configure mock responses
    when(mockClient.getObject(any(GetObjectRequest.class))).thenReturn(mockResponse);
    when(mockResponse.getInputStream()).thenReturn(createMockInputStream());
    
    OciObjectStorageDocumentLoader loader = new OciObjectStorageDocumentLoader(mockClient);
    Collection<Document> documents = loader.load("oci.os://namespace/bucket/document.pdf");
    
    assertThat(documents).hasSize(1);
}
```

## Best Practices

### Security
- Use instance principals for compute instances
- Apply principle of least privilege
- Use private endpoints for secure access
- Enable audit logging for access tracking

### Performance
- Use regional buckets for better performance
- Implement caching for frequently accessed documents
- Use parallel processing for bulk operations
- Consider OCI FastConnect for high-throughput scenarios

### Cost Optimization
- Use appropriate storage tiers based on access patterns
- Implement lifecycle policies for automatic cost optimization
- Monitor usage with OCI Monitoring
- Consider committed use discounts for predictable workloads

## License

This project is licensed under the Universal Permissive License (UPL) 1.0. 
