/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.anthropic.config;

import com.oracle.coherence.rag.config.AbstractConfig;

import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel.AnthropicStreamingChatModelBuilder;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration class for OpenAI streaming chat model.
 * <p/>
 * Encapsulates configuration parameters that control how Anthropic streaming chat
 * models are used within the Coherence RAG framework.
 *
 * @author Aleks Seovic/ Tim Middleton  2025.08.05
 * @since 25.09
 */
@SuppressWarnings("unused")
public class AnthropicStreamingChatModelConfig
        extends AbstractConfig
        implements PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor for POF and JSON serialization.
     */
    public AnthropicStreamingChatModelConfig()
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
    public AnthropicStreamingChatModelConfig setBaseUrl(String baseUrl)
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
     * Sets the OpenAI API key.
     *
     * @param apiKey the API key
     *
     * @return this config instance for method chaining
     */
    public AnthropicStreamingChatModelConfig setApiKey(String apiKey)
        {
        this.apiKey = apiKey;
        return this;
        }

    /**
     * Gets the model name to be used by the streaming chat model.
     *
     * @return the model name
     */
    public String getModelName()
        {
        return modelName;
        }

    /**
     * Sets the model name to be used by the streaming chat model.
     *
     * @param modelName the model name
     *
     * @return this config instance for method chaining
     */
    public AnthropicStreamingChatModelConfig setModelName(String modelName)
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
    public AnthropicStreamingChatModelConfig setTemperature(Double temperature)
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
    public AnthropicStreamingChatModelConfig setTopP(Double topP)
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
    public AnthropicStreamingChatModelConfig setTopK(Integer topK)
        {
        this.topK = topK;
        return this;
        }

    /**
     * Gets the list of stop sequences that end model output generation.
     *
     * @return the list of stop sequences
     */
    public List<String> getStop()
        {
        return stop;
        }

    /**
     * Sets the list of stop sequences that end model output generation.
     *
     * @param stop the stop sequences
     *
     * @return this config instance for method chaining
     */
    public AnthropicStreamingChatModelConfig setStop(List<String> stop)
        {
        this.stop = stop;
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
    public AnthropicStreamingChatModelConfig setMaxTokens(Integer maxTokens)
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
    public AnthropicStreamingChatModelConfig setTimeout(Duration timeout)
        {
        this.timeout = timeout;
        return this;
        }

    /**
     * Returns whether API requests should be logged.
     *
     * @return true if request logging is enabled
     */
    public Boolean isLogRequests() { return logRequests; }

    /**
     * Enables or disables logging of API requests.
     *
     * @param logRequests whether to log requests
     *
     * @return this config instance for method chaining
     */
    public AnthropicStreamingChatModelConfig setLogRequests(Boolean logRequests)
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
    public AnthropicStreamingChatModelConfig setLogResponses(Boolean logResponses)
        {
        this.logResponses = logResponses;
        return this;
        }

    // ---- strongly typed builder conversion -------------------------------

    /**
     * Converts this configuration to a strongly-typed builder instance.
     *
     * @return a configured {@link AnthropicStreamingChatModelBuilder}
     */
    public AnthropicStreamingChatModelBuilder toBuilder()
        {
        return AnthropicStreamingChatModel.builder()
                .modelName(modelName)
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .temperature(temperature)
                .topP(topP)
                .topK(topK)
                .maxTokens(maxTokens)
                .timeout(timeout)
                .logRequests(logRequests)
                .logResponses(logResponses);
        }

    // ---- Object methods --------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (!(o instanceof AnthropicStreamingChatModelConfig that)) return false;
        return Objects.equals(baseUrl, that.baseUrl) &&
               Objects.equals(apiKey, that.apiKey) &&
               Objects.equals(modelName, that.modelName) &&
               Objects.equals(temperature, that.temperature) &&
               Objects.equals(topP, that.topP) &&
               Objects.equals(topK, that.topK) &&
               Objects.equals(stop, that.stop) &&
               Objects.equals(maxTokens, that.maxTokens) &&
               Objects.equals(timeout, that.timeout) &&
               Objects.equals(logRequests, that.logRequests) &&
               Objects.equals(logResponses, that.logResponses);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(baseUrl, apiKey,
                            modelName, temperature, topP, topK, stop, maxTokens,
                            timeout, logRequests, logResponses);
        }

    @Override
    public String toString()
        {
        return "AnthropicStreamingChatModelConfig[" +
               "baseUrl=" + baseUrl +
               ", apiKey=" + apiKey +
               ", modelName=" + modelName +
               ", temperature=" + temperature +
               ", topP=" + topP +
               ", topK=" + topK +
               ", stop=" + stop +
               ", maxTokens=" + maxTokens +
               ", timeout=" + timeout +
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
    public void readExternal(PofReader reader) throws IOException
        {
        baseUrl                  = reader.readString(0);
        apiKey                   = reader.readString(1);
        modelName                = reader.readString(5);
        temperature              = reader.readDouble(6);
        topP                     = reader.readDouble(7);
        topK                     = reader.readInt(8);
        stop                     = reader.readCollection(9, new ArrayList<>());
        maxTokens                = reader.readInt(10);
        timeout                  = reader.readObject(23);
        logRequests              = reader.readBoolean(24);
        logResponses             = reader.readBoolean(25);
        }

    @Override
    public void writeExternal(PofWriter writer) throws IOException
        {
        writer.writeString(0, baseUrl);
        writer.writeString(1, apiKey);
        writer.writeString(5, modelName);
        writer.writeDouble(6, temperature);
        writer.writeDouble(7, topP);
        writer.writeInt(8, topK);
        writer.writeCollection(9, stop);
        writer.writeInt(10, maxTokens);
        writer.writeObject(23, timeout);
        writer.writeBoolean(24, logRequests);
        writer.writeBoolean(25, logResponses);
        }

    // ---- constants -------------------------------------------------------

    /**
     * The implementation version for this class.
     */
    public static final int IMPLEMENTATION_VERSION = 1;

    // ---- data members ----------------------------------------------------

    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Double temperature;
    private Double topP;
    private Integer topK;
    private List<String> stop;
    private Integer maxTokens;
    private Duration timeout;
    private Boolean logRequests;
    private Boolean logResponses;
    }
