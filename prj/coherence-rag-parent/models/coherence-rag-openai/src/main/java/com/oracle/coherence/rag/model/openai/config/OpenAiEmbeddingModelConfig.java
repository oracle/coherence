/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.openai.config;

import com.oracle.coherence.rag.config.AbstractConfig;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration class for OpenAI embedding model.
 * <p/>
 * Encapsulates configuration parameters that control how OpenAI embedding
 * models are used within the Coherence RAG framework.
 *
 * @author Aleks Seovic  2025.07.13
 * @since 25.09
 */
@SuppressWarnings("unused")
public class OpenAiEmbeddingModelConfig
        extends AbstractConfig
        implements PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor for POF and JSON serialization.
     */
    public OpenAiEmbeddingModelConfig()
        {
        }

    // ---- properties ------------------------------------------------------

    /**
     * Sets the base URL for the OpenAI API.
     *
     * @param baseUrl the OpenAI API base URL
     *
     * @return this config instance
     */
    public OpenAiEmbeddingModelConfig setBaseUrl(String baseUrl)
        {
        this.baseUrl = baseUrl;
        return this;
        }

    /**
     * Gets the base URL for the OpenAI API.
     *
     * @return the OpenAI API base URL
     */
    public String getBaseUrl()
        {
        return baseUrl;
        }

    /**
     * Sets the API key.
     *
     * @param apiKey the OpenAI API key
     *
     * @return this config instance
     */
    public OpenAiEmbeddingModelConfig setApiKey(String apiKey)
        {
        this.apiKey = apiKey;
        return this;
        }

    /**
     * Gets the API key.
     *
     * @return the OpenAI API key
     */
    public String getApiKey()
        {
        return apiKey;
        }

    /**
     * Sets the organization ID.
     *
     * @param organizationId the organization ID
     *
     * @return this config instance
     */
    public OpenAiEmbeddingModelConfig setOrganizationId(String organizationId)
        {
        this.organizationId = organizationId;
        return this;
        }

    /**
     * Gets the organization ID.
     *
     * @return the organization ID
     */
    public String getOrganizationId()
        {
        return organizationId;
        }

    /**
     * Sets the project ID.
     *
     * @param projectId the project ID
     *
     * @return this config instance
     */
    public OpenAiEmbeddingModelConfig setProjectId(String projectId)
        {
        this.projectId = projectId;
        return this;
        }

    /**
     * Gets the project ID.
     *
     * @return the project ID
     */
    public String getProjectId()
        {
        return projectId;
        }

    /**
     * Sets the model name.
     *
     * @param modelName the model name
     *
     * @return this config instance
     */
    public OpenAiEmbeddingModelConfig setModelName(String modelName)
        {
        this.modelName = modelName;
        return this;
        }

    /**
     * Gets the model name.
     *
     * @return the model name
     */
    public String getModelName()
        {
        return modelName;
        }

    /**
     * Sets the embedding dimensions.
     *
     * @param dimensions the number of dimensions
     *
     * @return this config instance
     */
    public OpenAiEmbeddingModelConfig setDimensions(Integer dimensions)
        {
        this.dimensions = dimensions;
        return this;
        }

    /**
     * Gets the number of embedding dimensions.
     *
     * @return the number of dimensions
     */
    public Integer getDimensions()
        {
        return dimensions;
        }

    /**
     * Sets the user identifier.
     *
     * @param user the user identifier
     *
     * @return this config instance
     */
    public OpenAiEmbeddingModelConfig setUser(String user)
        {
        this.user = user;
        return this;
        }

    /**
     * Gets the user identifier.
     *
     * @return the user identifier
     */
    public String getUser()
        {
        return user;
        }

    /**
     * Sets the request timeout duration.
     *
     * @param timeout the timeout duration
     *
     * @return this config instance
     */
    public OpenAiEmbeddingModelConfig setTimeout(Duration timeout)
        {
        this.timeout = timeout;
        return this;
        }

    /**
     * Gets the timeout duration.
     *
     * @return the timeout duration
     */
    public Duration getTimeout()
        {
        return timeout;
        }

    /**
     * Sets the maximum number of retries.
     *
     * @param maxRetries the retry limit
     *
     * @return this config instance
     */
    public OpenAiEmbeddingModelConfig setMaxRetries(Integer maxRetries)
        {
        this.maxRetries = maxRetries;
        return this;
        }

    /**
     * Gets the maximum number of retries.
     *
     * @return the retry limit
     */
    public Integer getMaxRetries()
        {
        return maxRetries;
        }

    /**
     * Sets the max number of segments per batch.
     *
     * @param maxSegmentsPerBatch the batch segment limit
     *
     * @return this config instance
     */
    public OpenAiEmbeddingModelConfig setMaxSegmentsPerBatch(Integer maxSegmentsPerBatch)
        {
        this.maxSegmentsPerBatch = maxSegmentsPerBatch;
        return this;
        }

    /**
     * Gets the max number of segments per batch.
     *
     * @return the segment batch limit
     */
    public Integer getMaxSegmentsPerBatch()
        {
        return maxSegmentsPerBatch;
        }

    /**
     * Enables or disables request logging.
     *
     * @param logRequests true to enable logging
     *
     * @return this config instance
     */
    public OpenAiEmbeddingModelConfig setLogRequests(Boolean logRequests)
        {
        this.logRequests = logRequests;
        return this;
        }

    /**
     * Returns whether request logging is enabled.
     *
     * @return true if logging is enabled
     */
    public Boolean isLogRequests()
        {
        return logRequests;
        }

    /**
     * Enables or disables response logging.
     *
     * @param logResponses true to enable logging
     *
     * @return this config instance
     */
    public OpenAiEmbeddingModelConfig setLogResponses(Boolean logResponses)
        {
        this.logResponses = logResponses;
        return this;
        }

    /**
     * Returns whether response logging is enabled.
     *
     * @return true if response logging is enabled
     */
    public Boolean isLogResponses()
        {
        return logResponses;
        }

    /**
     * Sets custom headers to send with requests.
     *
     * @param customHeaders the custom headers map
     *
     * @return this config instance
     */
    public OpenAiEmbeddingModelConfig setCustomHeaders(Map<String, String> customHeaders)
        {
        this.customHeaders = customHeaders;
        return this;
        }

    /**
     * Gets the custom headers for requests.
     *
     * @return the custom headers map
     */
    public Map<String, String> getCustomHeaders()
        {
        return customHeaders;
        }

    // ---- strongly typed builder conversion -------------------------------

    /**
     * Converts this configuration to a strongly-typed builder instance.
     *
     * @return a configured {@link OpenAiEmbeddingModel} builder
     */
    public OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder toBuilder()
        {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .organizationId(organizationId)
                .projectId(projectId)
                .modelName(modelName)
                .dimensions(dimensions)
                .user(user)
                .timeout(timeout)
                .maxRetries(maxRetries)
                .maxSegmentsPerBatch(maxSegmentsPerBatch)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .customHeaders(customHeaders);
        }

    // ---- Object methods --------------------------------------------------

    @Override
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
        OpenAiEmbeddingModelConfig that = (OpenAiEmbeddingModelConfig) o;
        return Objects.equals(baseUrl, that.baseUrl) &&
               Objects.equals(apiKey, that.apiKey) &&
               Objects.equals(organizationId, that.organizationId) &&
               Objects.equals(projectId, that.projectId) &&
               Objects.equals(modelName, that.modelName) &&
               Objects.equals(dimensions, that.dimensions) &&
               Objects.equals(user, that.user) &&
               Objects.equals(timeout, that.timeout) &&
               Objects.equals(maxRetries, that.maxRetries) &&
               Objects.equals(maxSegmentsPerBatch, that.maxSegmentsPerBatch) &&
               Objects.equals(logRequests, that.logRequests) &&
               Objects.equals(logResponses, that.logResponses) &&
               Objects.equals(customHeaders, that.customHeaders);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(baseUrl, apiKey, organizationId, projectId, modelName, dimensions, user, timeout,
                            maxRetries, maxSegmentsPerBatch, logRequests, logResponses, customHeaders);
        }

    @Override
    public String toString()
        {
        return "OpenAiEmbeddingModelConfig[" +
               "baseUrl=" + baseUrl +
               ", apiKey=" + apiKey +
               ", organizationId=" + organizationId +
               ", projectId=" + projectId +
               ", modelName=" + modelName +
               ", dimensions=" + dimensions +
               ", user=" + user +
               ", timeout=" + timeout +
               ", maxRetries=" + maxRetries +
               ", maxSegmentsPerBatch=" + maxSegmentsPerBatch +
               ", logRequests=" + logRequests +
               ", logResponses=" + logResponses +
               ", customHeaders=" + customHeaders + ']';
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
        baseUrl             = reader.readString(0);
        apiKey              = reader.readString(1);
        organizationId      = reader.readString(2);
        projectId           = reader.readString(3);
        modelName           = reader.readString(4);
        dimensions          = reader.readInt(5);
        user                = reader.readString(6);
        timeout             = reader.readObject(7);
        maxRetries          = reader.readInt(8);
        maxSegmentsPerBatch = reader.readInt(9);
        logRequests         = reader.readBoolean(10);
        logResponses        = reader.readBoolean(11);
        customHeaders       = reader.readMap(12, new LinkedHashMap<>());
        }

    @Override
    public void writeExternal(PofWriter writer) throws IOException
        {
        writer.writeString(0, baseUrl);
        writer.writeString(1, apiKey);
        writer.writeString(2, organizationId);
        writer.writeString(3, projectId);
        writer.writeString(4, modelName);
        writer.writeInt(5, dimensions);
        writer.writeString(6, user);
        writer.writeObject(7, timeout);
        writer.writeInt(8, maxRetries);
        writer.writeInt(9, maxSegmentsPerBatch);
        writer.writeBoolean(10, logRequests);
        writer.writeBoolean(11, logResponses);
        writer.writeMap(12, customHeaders);
        }

    // ---- constants -------------------------------------------------------

    /**
     * The implementation version for this class.
     */
    public static final int IMPLEMENTATION_VERSION = 1;

    // ---- data members ----------------------------------------------------

    /**
     * The base URL for the OpenAI API endpoint.
     */
    private String baseUrl;

    /**
     * The OpenAI API key for authentication.
     */
    private String apiKey;

    /**
     * The OpenAI organization ID, if applicable.
     */
    private String organizationId;

    /**
     * The OpenAI project ID, if applicable.
     */
    private String projectId;

    /**
     * The name of the embedding model to use.
     */
    private String modelName;

    /**
     * The number of embedding dimensions expected in the output.
     */
    private Integer dimensions;

    /**
     * The user identifier to be sent with the request, if any.
     */
    private String user;

    /**
     * The timeout duration for requests to the OpenAI API.
     */
    private Duration timeout;

    /**
     * The maximum number of retries for failed API requests.
     */
    private Integer maxRetries;

    /**
     * The maximum number of document segments allowed per batch.
     */
    private Integer maxSegmentsPerBatch;

    /**
     * Whether to log the OpenAI API requests.
     */
    private Boolean logRequests;

    /**
     * Whether to log the OpenAI API responses.
     */
    private Boolean logResponses;

    /**
     * Custom headers to send with each request.
     */
    private Map<String, String> customHeaders;
    }
