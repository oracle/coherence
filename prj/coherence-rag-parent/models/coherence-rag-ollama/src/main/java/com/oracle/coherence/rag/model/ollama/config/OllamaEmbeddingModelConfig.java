/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.ollama.config;

import com.oracle.coherence.rag.config.AbstractConfig;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration class for Ollama embedding model.
 * <p/>
 * Encapsulates configuration parameters that control how Ollama embedding
 * models are used within the Coherence RAG framework.
 *
 * @author Aleks Seovic  2025.07.13
 * @since 25.09
 */
@SuppressWarnings("unused")
public class OllamaEmbeddingModelConfig
        extends AbstractConfig
        implements PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor for POF and JSON serialization.
     */
    public OllamaEmbeddingModelConfig()
        {
        }

    // ---- properties ------------------------------------------------------

    /**
     * Gets the Ollama API base URL.
     *
     * @return the base URL to use for API requests
     */
    public String getBaseUrl()
        {
        return baseUrl;
        }

    /**
     * Sets the Ollama API base URL.
     *
     * @param baseUrl the base URL to use for API requests
     *
     * @return this config instance for method chaining
     */
    public OllamaEmbeddingModelConfig setBaseUrl(String baseUrl)
        {
        this.baseUrl = baseUrl;
        return this;
        }

    /**
     * Gets the embedding model name.
     *
     * @return the embedding model name
     */
    public String getModelName()
        {
        return modelName;
        }

    /**
     * Sets the embedding model name.
     *
     * @param modelName the embedding model name
     *
     * @return this config instance for method chaining
     */
    public OllamaEmbeddingModelConfig setModelName(String modelName)
        {
        this.modelName = modelName;
        return this;
        }

    /**
     * Gets the request timeout duration.
     *
     * @return the timeout duration
     */
    public Duration getTimeout()
        {
        return timeout;
        }

    /**
     * Sets the request timeout duration.
     *
     * @param timeout the timeout duration
     *
     * @return this config instance for method chaining
     */
    public OllamaEmbeddingModelConfig setTimeout(Duration timeout)
        {
        this.timeout = timeout;
        return this;
        }

    /**
     * Gets the maximum number of retry attempts.
     *
     * @return the retry count
     */
    public Integer getMaxRetries()
        {
        return maxRetries;
        }

    /**
     * Sets the maximum number of retry attempts.
     *
     * @param maxRetries the retry count
     *
     * @return this config instance for method chaining
     */
    public OllamaEmbeddingModelConfig setMaxRetries(Integer maxRetries)
        {
        this.maxRetries = maxRetries;
        return this;
        }

    /**
     * Returns whether API requests should be logged.
     *
     * @return true if request logging is enabled
     */
    public Boolean isLogRequests()
        {
        return logRequests;
        }

    /**
     * Enables or disables logging of API requests.
     *
     * @param logRequests whether to log requests
     *
     * @return this config instance for method chaining
     */
    public OllamaEmbeddingModelConfig setLogRequests(Boolean logRequests)
        {
        this.logRequests = logRequests;
        return this;
        }

    /**
     * Returns whether API responses should be logged.
     *
     * @return true if response logging is enabled
     */
    public Boolean isLogResponses()
        {
        return logResponses;
        }

    /**
     * Enables or disables logging of API responses.
     *
     * @param logResponses whether to log responses
     *
     * @return this config instance for method chaining
     */
    public OllamaEmbeddingModelConfig setLogResponses(Boolean logResponses)
        {
        this.logResponses = logResponses;
        return this;
        }

    /**
     * Gets the custom HTTP headers to include with requests.
     *
     * @return the custom headers map
     */
    public Map<String, String> getCustomHeaders()
        {
        return customHeaders;
        }

    /**
     * Sets the custom HTTP headers to include with requests.
     *
     * @param customHeaders the custom headers map
     *
     * @return this config instance for method chaining
     */
    public OllamaEmbeddingModelConfig setCustomHeaders(Map<String, String> customHeaders)
        {
        this.customHeaders = customHeaders;
        return this;
        }

    // ---- strongly typed builder conversion -------------------------------

    /**
     * Converts this configuration to a strongly-typed builder instance.
     *
     * @return a configured {@code OllamaEmbeddingModelBuilder}
     */
    public dev.langchain4j.model.ollama.OllamaEmbeddingModel.OllamaEmbeddingModelBuilder toBuilder()
        {
        return dev.langchain4j.model.ollama.OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(timeout)
                .maxRetries(maxRetries)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .customHeaders(customHeaders);
        }

    // ---- Object methods --------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (!(o instanceof OllamaEmbeddingModelConfig that)) return false;
        return Objects.equals(baseUrl, that.baseUrl) &&
               Objects.equals(modelName, that.modelName) &&
               Objects.equals(timeout, that.timeout) &&
               Objects.equals(maxRetries, that.maxRetries) &&
               Objects.equals(logRequests, that.logRequests) &&
               Objects.equals(logResponses, that.logResponses) &&
               Objects.equals(customHeaders, that.customHeaders);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(baseUrl, modelName, timeout, maxRetries, logRequests, logResponses, customHeaders);
        }

    @Override
    public String toString()
        {
        return "OllamaEmbeddingModelConfig[" +
               "baseUrl=" + baseUrl +
               ", modelName=" + modelName +
               ", timeout=" + timeout +
               ", maxRetries=" + maxRetries +
               ", logRequests=" + logRequests +
               ", logResponses=" + logResponses +
               ", customHeaders=" + customHeaders +
               ']';
        }

    // ---- AbstractEvolvable interface -------------------------------------

    @Override
    public int getImplVersion()
        {
        return IMPLEMENTATION_VERSION;
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader reader) throws IOException
        {
        baseUrl      = reader.readString(0);
        modelName    = reader.readString(1);
        timeout      = reader.readObject(2);
        maxRetries   = reader.readInt(3);
        logRequests  = reader.readBoolean(4);
        logResponses = reader.readBoolean(5);
        customHeaders = reader.readMap(6, new LinkedHashMap<>());
        }

    @Override
    public void writeExternal(PofWriter writer) throws IOException
        {
        writer.writeString(0, baseUrl);
        writer.writeString(1, modelName);
        writer.writeObject(2, timeout);
        writer.writeInt(3, maxRetries);
        writer.writeBoolean(4, logRequests);
        writer.writeBoolean(5, logResponses);
        writer.writeMap(6, customHeaders);
        }

    // ---- constants -------------------------------------------------------

    /**
     * The implementation version for this class.
     */
    public static final int IMPLEMENTATION_VERSION = 1;

    // ---- data members ----------------------------------------------------

    private String baseUrl;
    private String modelName;
    private Duration timeout;
    private Integer maxRetries;
    private Boolean logRequests;
    private Boolean logResponses;
    private Map<String, String> customHeaders;
    }
