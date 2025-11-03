/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.anthropic.config;

import com.oracle.coherence.rag.config.AbstractConfig;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel.AnthropicChatModelBuilder;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration class for Anthropic chat model.
 * <p/>
 * Encapsulates configuration parameters that control how Anthropic chat
 * models are used within the Coherence RAG framework.
 *
 * @author Aleks Seovic/ Tim Middleton  2025.08.05
 * @since 25.09
 */
@SuppressWarnings("unused")
public class AnthropicChatModelConfig
        extends AbstractConfig
        implements PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor for POF and JSON serialization.
     */
    public AnthropicChatModelConfig()
        {
        }

    // ---- properties ------------------------------------------------------

    /**
     * Gets the Anthropic API base URL.
     *
     * @return the base URL to use for API requests
     */
    public String getBaseUrl()
        {
        return baseUrl;
        }

    /**
     * Sets the Anthropic API base URL.
     *
     * @param baseUrl the base URL to use for API requests
     *
     * @return this config instance for method chaining
     */
    public AnthropicChatModelConfig setBaseUrl(String baseUrl)
        {
        this.baseUrl = baseUrl;
        return this;
        }

    /**
     * Gets the Anthropic API key.
     *
     * @return the API key
     */
    public String getApiKey()
        {
        return apiKey;
        }

    /**
     * Sets the Anthropic API key.
     *
     * @param apiKey the API key
     *
     * @return this config instance for method chaining
     */
    public AnthropicChatModelConfig setApiKey(String apiKey)
        {
        this.apiKey = apiKey;
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
     * Sets the organization ID.
     *
     * @param organizationId the organization ID
     *
     * @return this config instance for method chaining
     */
    public AnthropicChatModelConfig setOrganizationId(String organizationId)
        {
        this.organizationId = organizationId;
        return this;
        }

    /**
     * Gets the model name to be used by the chat model.
     *
     * @return the model name
     */
    public String getModelName()
        {
        return modelName;
        }

    /**
     * Sets the model name to be used by the chat model.
     *
     * @param modelName the model name
     *
     * @return this config instance for method chaining
     */
    public AnthropicChatModelConfig setModelName(String modelName)
        {
        this.modelName = modelName;
        return this;
        }

    /**
     * Gets the temperature value that affects randomness in response
     * generation.
     *
     * @return the temperature
     */
    public Double getTemperature()
        {
        return temperature;
        }

    /**
     * Sets the temperature value that affects randomness in response
     * generation.
     *
     * @param temperature the temperature to use
     *
     * @return this config instance for method chaining
     */
    public AnthropicChatModelConfig setTemperature(Double temperature)
        {
        this.temperature = temperature;
        return this;
        }

    /**
     * Gets the top-p (nucleus sampling) value.
     *
     * @return the top-p value
     */
    public Double getTopP()
        {
        return topP;
        }

    /**
     * Sets the top-p (nucleus sampling) value.
     *
     * @param topP the top-p value to use
     *
     * @return this config instance for method chaining
     */
    public AnthropicChatModelConfig setTopP(Double topP)
        {
        this.topP = topP;
        return this;
        }

    /**
     * Gets the top-k sampling value.
     *
     * @return the top-k sampling value
     */
    public Integer getTopK()
        {
        return topK;
        }

    /**
     * Sets the top-k sampling value.
     *
     * @param topK the top-k value to use
     *
     * @return this config instance for method chaining
     */
    public AnthropicChatModelConfig setTopK(Integer topK)
        {
        this.topK = topK;
        return this;
        }

    /**
     * Gets the maximum number of tokens to generate.
     *
     * @return the maximum token count
     */
    public Integer getMaxTokens()
        {
        return maxTokens;
        }

    /**
     * Sets the maximum number of tokens to generate.
     *
     * @param maxTokens the maximum token count
     *
     * @return this config instance for method chaining
     */
    public AnthropicChatModelConfig setMaxTokens(Integer maxTokens)
        {
        this.maxTokens = maxTokens;
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
    public AnthropicChatModelConfig setTimeout(Duration timeout)
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
    public AnthropicChatModelConfig setMaxRetries(Integer maxRetries)
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
    public AnthropicChatModelConfig setLogRequests(Boolean logRequests)
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
    public AnthropicChatModelConfig setLogResponses(Boolean logResponses)
        {
        this.logResponses = logResponses;
        return this;
        }

    // ---- strongly typed builder conversion -------------------------------

    /**
     * Converts this configuration to a strongly-typed builder instance.
     *
     * @return a configured {@link AnthropicChatModelBuilder}
     */
    public AnthropicChatModelBuilder toBuilder()
        {
        return AnthropicChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .topP(topP)
                .topK(topK)
                .maxTokens(maxTokens)
                .timeout(timeout)
                .maxRetries(maxRetries)
                .logRequests(logRequests)
                .logResponses(logResponses);
        }

    // ---- Object methods --------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnthropicChatModelConfig that = (AnthropicChatModelConfig) o;
        return Objects.equals(baseUrl, that.baseUrl) &&
               Objects.equals(apiKey, that.apiKey) &&
               Objects.equals(organizationId, that.organizationId) &&
               Objects.equals(modelName, that.modelName) &&
               Objects.equals(temperature, that.temperature) &&
               Objects.equals(topP, that.topP) &&
               Objects.equals(maxTokens, that.maxTokens) &&
               Objects.equals(timeout, that.timeout) &&
               Objects.equals(maxRetries, that.maxRetries) &&
               Objects.equals(logRequests, that.logRequests) &&
               Objects.equals(logResponses, that.logResponses);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(baseUrl, apiKey, organizationId, modelName, temperature, topP,
                            topK, timeout, maxRetries, logRequests, logResponses);
        }

    @Override
    public String toString()
        {
        return "AnthropicChatModelConfig[" +
               "baseUrl=" + baseUrl +
               ", apiKey=" + apiKey +
               ", organizationId=" + organizationId +
               ", modelName=" + modelName +
               ", temperature=" + temperature +
               ", topP=" + topP +
               ", topP=" + topK +
               ", maxTokens=" + maxTokens +
               ", timeout=" + timeout +
               ", maxRetries=" + maxRetries +
               ", logRequests=" + logRequests +
               ", logResponses=" + logResponses + ']';
        }

    // ---- AbstractEvolvable interface -------------------------------------

    @Override
    public int getImplVersion()
        {
        return IMPLEMENTATION_VERSION;
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        baseUrl                  = in.readString(0);
        apiKey                   = in.readString(1);
        organizationId           = in.readString(2);
        modelName                = in.readString(4);
        temperature              = in.readDouble(5);
        topP                     = in.readDouble(6);
        topK                     = in.readInt(7);
        maxTokens                = in.readInt(8);
        timeout                  = in.readObject(9);
        maxRetries               = in.readInt(10);
        logRequests              = in.readBoolean(11);
        logResponses             = in.readBoolean(12);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, baseUrl);
        out.writeString(1, apiKey);
        out.writeString(2, organizationId);
        out.writeString(4, modelName);
        out.writeDouble(5, temperature);
        out.writeDouble(6, topP);
        out.writeInt(7, topK);
        out.writeInt(8, maxTokens);
        out.writeObject(9, timeout);
        out.writeInt(10, maxRetries);
        out.writeBoolean(11, logRequests);
        out.writeBoolean(12, logResponses);
        }

    // ---- constants -------------------------------------------------------

    /**
     * The implementation version for this class.
     */
    public static final int IMPLEMENTATION_VERSION = 1;

    // ---- data members ----------------------------------------------------

    private String baseUrl;
    private String apiKey;
    private String organizationId;
    private String modelName;
    private Double temperature;
    private Double topP;
    private Integer topK;
    private Integer maxTokens;
    private Duration timeout;
    private Integer maxRetries;
    private Boolean logRequests;
    private Boolean logResponses;
    }
