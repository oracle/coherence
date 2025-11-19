# Oracle Coherence RAG Framework

A comprehensive Retrieval Augmented Generation (RAG) solution built on top of Oracle Coherence, providing distributed document processing, vector storage, and AI model integration capabilities.

## Overview

The Oracle Coherence RAG Framework enables building scalable, enterprise-grade RAG applications that leverage Oracle Coherence's distributed data grid technology. The framework integrates with LangChain4J for document processing and provides flexible integration with various chat, embedding and scoring/re-ranking models.

## Architecture

The framework is organized into several modular components:

- **[coherence-rag](coherence-rag/)** - Core RAG framework with REST API
- **[loaders](loaders/)** - Document loaders for various storage services
- **[models](models/)** - AI model integrations (OpenAI, OCI GenAI, Ollama, etc.)

## Key Features

### 🚀 **Distributed Processing**
- Built on Oracle Coherence for scalability and fault tolerance
- Automatic partitioning and load balancing
- High availability with data replication

### 📄 **Document Processing**
- Multiple document formats (PDF, Word, text, etc.)
- Automatic chunking and embedding generation
- Metadata extraction and preservation
- Cloud storage integration (OCI Object Storage, AWS S3, Azure Blob Storage, Google Cloud Storage)

### 🤖 **AI Model Integration**
- Local ONNX models for on-premises deployment
- OCI GenAI service with streaming support
- OpenAI GPT models and embeddings
- DeepSeek AI for advanced reasoning
- Ollama for open-source model integration

### 🌐 **REST API**
- Document ingestion and processing
- Vector similarity search
- Hybrid search
- Configuration management
- Real-time streaming responses

## Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- Oracle Coherence 25.09+

## Modules

### Core Framework
- **[coherence-rag](coherence-rag/)** - Main RAG framework implementation

### Document Loaders
- **[coherence-rag-oci-object-storage-loader](loaders/coherence-rag-oci-object-storage-loader/)** - OCI Object Storage integration
- **[coherence-rag-aws-s3-loader](loaders/coherence-rag-aws-s3-loader/)** - AWS S3 document loading
- **[coherence-rag-azure-blob-storage-loader](loaders/coherence-rag-azure-blob-storage-loader/)** - Azure Blob Storage integration
- **[coherence-rag-google-cloud-storage-loader](loaders/coherence-rag-google-cloud-storage-loader/)** - Google Cloud Storage support

### AI Model Providers
- **[coherence-rag-oci](models/coherence-rag-oci/)** - OCI GenAI service integration
- **[coherence-rag-open-ai](models/coherence-rag-open-ai/)** - OpenAI GPT models and embeddings
- **[coherence-rag-deepseek](models/coherence-rag-deepseek/)** - DeepSeek AI integration
- **[coherence-rag-ollama](models/coherence-rag-ollama/)** - Local Ollama model support

## REST API

The framework provides a comprehensive REST API for document management and search:

[//]: # (TODO: document major REST endpoints)

## Performance and Scalability

### Distributed Architecture
- **Coherence Clustering**: Automatic scaling across multiple nodes
- **Data Partitioning**: Efficient distribution and search of vector embeddings
- **Load Balancing**: Automatic request distribution and parallelization
- **Fault Tolerance**: Automatic failover and recovery

### Performance Optimizations
- **Connection Pooling**: Efficient model API connections
- **Batch Processing**: Optimized embedding generation
- **Caching**: Intelligent caching of embeddings and results
- **Streaming**: Real-time response streaming

## Development

### Building from Source

```bash
# Build all modules
mvn clean install

# Build specific module
mvn clean install -pl coherence-rag

# Run tests
mvn test

# Generate documentation
mvn javadoc:javadoc
```

### Running Tests

```bash
# Run all tests
mvn test

# Run integration tests
mvn verify
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## License

This project is licensed under the Universal Permissive License (UPL) 1.0.

## Support

For questions, issues, or contributions:
- Create an issue in the repository
- Check the documentation in each module
- Review the Javadoc documentation

## Roadmap

- [ ] Third party Vector Store integrations
- [ ] More AI model providers
- [ ] Advanced chunking strategies
- [ ] Container images and Kubernetes deployment templates 
