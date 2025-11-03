/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.config;

import com.oracle.coherence.rag.config.index.IndexConfig;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;
import java.util.Objects;

/**
 * Configuration class for Coherence RAG document store.
 * <p/>
 * This class encapsulates all configuration parameters needed for setting up
 * a document store, including embedding model settings, document chunking parameters, 
 * and vector store configuration.
 * 
 * @author Aleks Seovic  2025.06.28
 * @since 25.09
 */
@SuppressWarnings("unused")
public class StoreConfig
        extends AbstractConfig<Object>
    {
    public StoreConfig()
        {
        }

    public StoreConfig(String embeddingModel, boolean normalizeEmbeddings, IndexConfig<?> index, int chunkSize, int chunkOverlap)
        {
        this.embeddingModel = embeddingModel;
        this.normalizeEmbeddings = normalizeEmbeddings;
        this.index = index;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        }

    /**
     * Gets the current embedding model identifier.
     *
     * @return the embedding model identifier, or null if not set
     */
    public String getEmbeddingModel()
        {
        return embeddingModel;
        }

    /**
     * Sets the embedding model and returns this instance for method chaining.
     *
     * @param embeddingModel  the embedding model identifier to use
     *
     * @return this StoreConfig instance for method chaining
     */
    public StoreConfig setEmbeddingModel(String embeddingModel)
        {
        this.embeddingModel = embeddingModel;
        return this;
        }

    /**
     * Checks whether vector embeddings should be normalized.
     *
     * @return true if embeddings should be normalized, false otherwise
     */
    public boolean isNormalizeEmbeddings()
        {
        return normalizeEmbeddings;
        }

    /**
     * Sets the normalize embeddings flag and returns this instance for method chaining.
     *
     * @param normalizeEmbeddings whether to normalize vector embeddings
     *
     * @return this StoreConfig instance for method chaining
     */
    public StoreConfig setNormalizeEmbeddings(boolean normalizeEmbeddings)
        {
        this.normalizeEmbeddings = normalizeEmbeddings;
        return this;
        }

    /**
     * Gets the current index configuration.
     *
     * @return the index configuration, or null if not set
     */
    public IndexConfig<?> getIndex()
        {
        return index;
        }

    /**
     * Sets the index configuration and returns this instance for method chaining.
     *
     * @param index the index configuration settings
     *
     * @return this StoreConfig instance for method chaining
     */
    public StoreConfig setIndex(IndexConfig<?> index)
        {
        this.index = index;
        return this;
        }

    /**
     * Gets the current chunk size.
     *
     * @return the size of document chunks
     */
    public int getChunkSize()
        {
        return chunkSize;
        }

    /**
     * Sets the chunk size and returns this instance for method chaining.
     *
     * @param chunkSize the size of document chunks
     *
     * @return this StoreConfig instance for method chaining
     */
    public StoreConfig setChunkSize(int chunkSize)
        {
        this.chunkSize = chunkSize;
        return this;
        }

    /**
     * Gets the current chunk overlap.
     *
     * @return the overlap between consecutive chunks
     */
    public int getChunkOverlap()
        {
        return chunkOverlap;
        }

    /**
     * Sets the chunk overlap and returns this instance for method chaining.
     *
     * @param chunkOverlap the overlap between consecutive chunks
     *
     * @return this StoreConfig instance for method chaining
     */
    public StoreConfig setChunkOverlap(int chunkOverlap)
        {
        this.chunkOverlap = chunkOverlap;
        return this;
        }

    // ---- Object methods --------------------------------------------------

    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        StoreConfig that = (StoreConfig) o;
        return normalizeEmbeddings == that.normalizeEmbeddings &&
               chunkSize == that.chunkSize &&
               chunkOverlap == that.chunkOverlap &&
               Objects.equals(embeddingModel, that.embeddingModel) &&
               Objects.equals(index, that.index);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(embeddingModel, normalizeEmbeddings, index, chunkSize, chunkOverlap);
        }

    @Override
    public String toString()
        {
        return "StoreConfig[" +
               "embeddingModel=" + embeddingModel + ", " +
               "normalizeEmbeddings=" + normalizeEmbeddings + ", " +
               "index=" + index + ", " +
               "chunkSize=" + chunkSize + ", " +
               "chunkOverlap=" + chunkOverlap + ']';
        }

    // ---- AbstractEvolvable methods ---------------------------------------

    @Override
    public int getImplVersion()
        {
        return IMPLEMENTATION_VERSION;
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader pofReader) throws IOException
        {
        embeddingModel = pofReader.readString(0);
        normalizeEmbeddings = pofReader.readBoolean(1);
        index = (IndexConfig) pofReader.readObject(2);
        chunkSize = pofReader.readInt(3);
        chunkOverlap = pofReader.readInt(4);
        }

    @Override
    public void writeExternal(PofWriter pofWriter) throws IOException
        {
        pofWriter.writeString(0, embeddingModel);
        pofWriter.writeBoolean(1, normalizeEmbeddings);
        pofWriter.writeObject(2, index);
        pofWriter.writeInt(3, chunkSize);
        pofWriter.writeInt(4, chunkOverlap);
        }

    // ---- constants -------------------------------------------------------

    /**
     * The implementation version for this class.
     */
    public static final int IMPLEMENTATION_VERSION = 1;

    // ---- data members ----------------------------------------------------

    /**
     * The embedding model identifier to use for generating vector embeddings.
     * <p/>
     * This field specifies which AI model should be used to convert document
     * text into vector representations. The value should be a valid model identifier
     * that the embedding service can recognize.
     */
    private String embeddingModel;

    /**
     * Flag indicating whether vector embeddings should be normalized.
     * <p/>
     * When true, vector embeddings will be normalized to unit length, which
     * can improve similarity search performance and accuracy in certain scenarios.
     */
    private boolean normalizeEmbeddings;

    /**
     * Configuration for document indexing settings.
     * <p/>
     * This field contains the index configuration that determines how documents
     * are indexed and stored for efficient retrieval.
     */
    private IndexConfig<?> index;

    /**
     * The size of document chunks in characters or tokens.
     * <p/>
     * Documents are split into chunks of this size for processing. Larger chunks
     * may preserve more context but require more memory and processing time.
     */
    private int chunkSize;

    /**
     * The overlap between consecutive document chunks in characters or tokens.
     * <p/>
     * This overlap helps maintain context between chunks and can improve
     * the quality of vector embeddings and retrieval results.
     */
    private int chunkOverlap;
    }
