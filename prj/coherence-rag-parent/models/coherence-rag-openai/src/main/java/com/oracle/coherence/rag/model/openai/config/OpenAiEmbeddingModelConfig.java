/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.openai.config;

import com.oracle.coherence.rag.config.model.EmbeddingModelConfig;

import dev.langchain4j.model.openai.OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder;

import java.time.Duration;
import java.util.Map;

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
        extends EmbeddingModelConfig<OpenAiEmbeddingModelBuilder>
    {
    // ---- properties ------------------------------------------------------

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

    // ---- AbstractConfig methods ------------------------------------------

    @Override
    public OpenAiEmbeddingModelBuilder apply(OpenAiEmbeddingModelBuilder target)
        {
        return target
                .organizationId(organizationId)
                .projectId(projectId)
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
    public String toString()
        {
        return "OpenAiEmbeddingModelConfig[" +
               "organizationId=" + organizationId +
               ", projectId=" + projectId +
               ", dimensions=" + dimensions +
               ", user=" + user +
               ", timeout=" + timeout +
               ", maxRetries=" + maxRetries +
               ", maxSegmentsPerBatch=" + maxSegmentsPerBatch +
               ", logRequests=" + logRequests +
               ", logResponses=" + logResponses +
               ", customHeaders=" + customHeaders + ']';
        }

    // ---- data members ----------------------------------------------------

    /**
     * The OpenAI organization ID, if applicable.
     */
    private String organizationId;

    /**
     * The OpenAI project ID, if applicable.
     */
    private String projectId;

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
