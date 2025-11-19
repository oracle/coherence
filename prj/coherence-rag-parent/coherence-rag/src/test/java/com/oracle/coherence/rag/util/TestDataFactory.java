/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.util;

import com.oracle.coherence.ai.DocumentChunk;
import com.oracle.coherence.rag.config.index.HnswIndexConfig;
import com.oracle.coherence.rag.config.index.IndexConfig;
import com.oracle.coherence.rag.config.StoreConfig;
import com.oracle.coherence.rag.model.ModelName;
import com.oracle.coherence.rag.model.PoolingConfig;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Factory for creating test data objects used in unit and integration tests.
 * <p/>
 * This utility class provides methods to create various types of test data
 * including documents, embeddings, configurations, and other domain objects
 * commonly used in Coherence RAG testing. The factory supports creating
 * both simple and complex test scenarios with realistic data.
 * <p/>
 * Key features:
 * <ul>
 * <li>Document creation with various content types and metadata</li>
 * <li>Embedding generation with configurable dimensions</li>
 * <li>Configuration objects for different test scenarios</li>
 * <li>Random data generation for load testing</li>
 * <li>Realistic test data that mimics production scenarios</li>
 * </ul>
 * <p/>
 * Usage examples:
 * <pre>{@code
 * // Create a simple test document
 * Document doc = TestDataFactory.createDocument("Test content", "test.txt");
 * 
 * // Create an embedding with specific dimensions
 * Embedding embedding = TestDataFactory.createEmbedding(384);
 * 
 * // Create a store configuration for testing
 * StoreConfig config = TestDataFactory.createStoreConfig("test-store");
 * }</pre>
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@SuppressWarnings("unused")
public class TestDataFactory
    {
    // ---- constants -------------------------------------------------------

    /**
     * Default embedding dimension for test embeddings.
     */
    public static final int DEFAULT_EMBEDDING_DIMENSION = 384;

    /**
     * Default chunk size for document chunking.
     */
    public static final int DEFAULT_CHUNK_SIZE = 1000;

    /**
     * Default chunk overlap for document chunking.
     */
    public static final int DEFAULT_CHUNK_OVERLAP = 200;

    /**
     * Random number generator for test data creation.
     */
    private static final Random RANDOM = new Random(42); // Fixed seed for reproducible tests

    // ---- document creation methods ---------------------------------------

    /**
     * Creates a simple test document with the specified content and filename.
     *
     * @param content  the document content
     * @param filename the document filename
     *
     * @return a new Document instance
     */
    public static Document createDocument(String content, String filename)
        {
        Metadata metadata = Metadata.metadata("filename", filename);
        metadata.put("content_type", detectContentType(filename));
        metadata.put("content_length", (long) content.length());
        metadata.put("url", "file://" + filename);
        
        return Document.from(content, metadata);
        }

    /**
     * Creates a test document with comprehensive metadata.
     *
     * @param content    the document content
     * @param filename   the document filename
     * @param properties additional metadata properties
     *
     * @return a new Document instance with rich metadata
     */
    public static Document createDocumentWithMetadata(String content, String filename, Map<String, Object> properties)
        {
        Metadata metadata = Metadata.from(properties);
        metadata.put("filename", filename);
        metadata.put("content_type", detectContentType(filename));
        metadata.put("content_length", (long) content.length());
        metadata.put("url", "file://" + filename);
        metadata.put("creation_time", System.currentTimeMillis());
        metadata.put("document_id", UUID.randomUUID().toString());

        return Document.from(content, metadata);
        }

    /**
     * Creates a large test document suitable for performance testing.
     *
     * @param sizeInChars the approximate size in characters
     * @param filename    the document filename
     *
     * @return a new Document instance with specified size
     */
    public static Document createLargeDocument(int sizeInChars, String filename)
        {
        StringBuilder content = new StringBuilder();
        String paragraph = "This is a test paragraph used for generating large documents. " +
                          "It contains multiple sentences and provides realistic content structure. " +
                          "The content is repeated to reach the desired document size. ";
        
        while (content.length() < sizeInChars)
            {
            content.append(paragraph);
            }
        
        return createDocument(content.toString(), filename);
        }

    // ---- embedding creation methods -------------------------------------

    /**
     * Creates a test embedding with the specified dimensions.
     *
     * @param dimensions the number of dimensions
     *
     * @return a new Embedding instance
     */
    public static Embedding createEmbedding(int dimensions)
        {
        float[] vector = new float[dimensions];
        for (int i = 0; i < dimensions; i++)
            {
            vector[i] = RANDOM.nextFloat() * 2.0f - 1.0f; // Range [-1, 1]
            }
        
        return new Embedding(vector);
        }

    /**
     * Creates a test embedding with default dimensions.
     *
     * @return a new Embedding instance with default dimensions
     */
    public static Embedding createEmbedding()
        {
        return createEmbedding(DEFAULT_EMBEDDING_DIMENSION);
        }

    /**
     * Creates a list of test embeddings with specified dimensions.
     *
     * @param count      the number of embeddings to create
     * @param dimensions the number of dimensions for each embedding
     *
     * @return a list of Embedding instances
     */
    public static List<Embedding> createEmbeddings(int count, int dimensions)
        {
        List<Embedding> embeddings = new ArrayList<>();
        for (int i = 0; i < count; i++)
            {
            embeddings.add(createEmbedding(dimensions));
            }
        return embeddings;
        }

    // ---- document chunk creation methods --------------------------------

    /**
     * Creates a test document chunk with the specified content and metadata.
     *
     * @param content  the chunk content
     * @param metadata the chunk metadata
     *
     * @return a new DocumentChunk instance
     */
    public static DocumentChunk createDocumentChunk(String content, Map<String, Object> metadata)
        {
        DocumentChunk chunk = new DocumentChunk(content, metadata);
        Embedding embedding = createEmbedding();
        chunk.setVector(embedding.vector());
        return chunk;
        }

    /**
     * Creates a test document chunk with minimal data.
     *
     * @param content the chunk content
     *
     * @return a new DocumentChunk instance
     */
    public static DocumentChunk createDocumentChunk(String content)
        {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("chunk_index", 0);
        metadata.put("document_id", UUID.randomUUID().toString());
        metadata.put("content_length", content.length());
        
        return createDocumentChunk(content, metadata);
        }

    /**
     * Creates a list of test document chunks from a document.
     *
     * @param document  the source document
     * @param chunkSize the size of each chunk
     *
     * @return a list of DocumentChunk instances
     */
    public static List<DocumentChunk> createDocumentChunks(Document document, int chunkSize)
        {
        List<DocumentChunk> chunks = new ArrayList<>();
        String content = document.text();
        String documentId = UUID.randomUUID().toString();
        
        for (int i = 0; i < content.length(); i += chunkSize)
            {
            int endIndex = Math.min(i + chunkSize, content.length());
            String chunkContent = content.substring(i, endIndex);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("chunk_index", chunks.size());
            metadata.put("document_id", documentId);
            metadata.put("content_length", chunkContent.length());
            metadata.put("start_index", i);
            metadata.put("end_index", endIndex);
            
            chunks.add(createDocumentChunk(chunkContent, metadata));
            }
        
        return chunks;
        }

    // ---- configuration creation methods ---------------------------------

    /**
     * Creates a test store configuration with the specified name.
     *
     * @param storeName the name of the store
     *
     * @return a new StoreConfig instance
     */
    public static StoreConfig createStoreConfig(String storeName)
        {
        StoreConfig config = new StoreConfig();
        config.setEmbeddingModel(new ModelName("test/embedding-model").fullName());
        config.setChunkSize(DEFAULT_CHUNK_SIZE);
        config.setChunkOverlap(DEFAULT_CHUNK_OVERLAP);
        return config;
        }

    /**
     * Creates a test index configuration with default settings.
     *
     * @return a new IndexConfig instance
     */
    public static IndexConfig<?> createIndexConfig()
        {
        return new HnswIndexConfig();
        }

    /**
     * Creates a test pooling configuration with default settings.
     *
     * @return a new PoolingConfig instance
     */
    public static PoolingConfig createPoolingConfig()
        {
        return new PoolingConfig(384, false, true);
        }

    // ---- message creation methods ---------------------------------------

    /**
     * Creates a test user message with the specified content.
     *
     * @param content the message content
     *
     * @return a new UserMessage instance
     */
    public static UserMessage createUserMessage(String content)
        {
        return new UserMessage(content);
        }

    /**
     * Creates a test AI message with the specified content.
     *
     * @param content the message content
     *
     * @return a new AiMessage instance
     */
    public static AiMessage createAiMessage(String content)
        {
        return new AiMessage(content);
        }

    // ---- utility methods ------------------------------------------------

    /**
     * Detects the content type based on the filename extension.
     *
     * @param filename the filename
     *
     * @return the detected content type
     */
    private static String detectContentType(String filename)
        {
        if (filename.endsWith(".pdf"))
            return "application/pdf";
        else if (filename.endsWith(".txt"))
            return "text/plain";
        else if (filename.endsWith(".html"))
            return "text/html";
        else if (filename.endsWith(".json"))
            return "application/json";
        else if (filename.endsWith(".xml"))
            return "application/xml";
        else
            return "application/octet-stream";
        }

    /**
     * Creates a random string with the specified length.
     *
     * @param length the length of the string
     *
     * @return a random string
     */
    public static String createRandomString(int length)
        {
        StringBuilder sb = new StringBuilder();
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ";
        
        for (int i = 0; i < length; i++)
            {
            sb.append(characters.charAt(RANDOM.nextInt(characters.length())));
            }
        
        return sb.toString();
        }

    /**
     * Creates a test URI with the specified scheme and path.
     *
     * @param scheme the URI scheme
     * @param path   the URI path
     *
     * @return a new URI instance
     */
    public static URI createTestUri(String scheme, String path)
        {
        return URI.create(scheme + "://" + path);
        }

    /**
     * Creates a map of test metadata properties.
     *
     * @param properties varargs of key-value pairs
     *
     * @return a map of metadata properties
     */
    public static Map<String, Object> createMetadata(Object... properties)
        {
        Map<String, Object> metadata = new HashMap<>();
        for (int i = 0; i < properties.length; i += 2)
            {
            if (i + 1 < properties.length)
                {
                metadata.put(properties[i].toString(), properties[i + 1]);
                }
            }
        return metadata;
        }

    // ---- private constructor --------------------------------------------

    /**
     * Private constructor to prevent instantiation.
     */
    private TestDataFactory()
        {
        }
    } 
