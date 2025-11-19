/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.openai.config;

import com.oracle.coherence.rag.config.AbstractConfig;

import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration class for OpenAI streaming chat model.
 * <p/>
 * Encapsulates configuration parameters that control how OpenAI streaming chat
 * models are used within the Coherence RAG framework.
 *
 * @author Aleks Seovic  2025.07.13
 * @since 25.09
 */
@SuppressWarnings("unused")
public class OpenAiStreamingChatModelConfig
        extends AbstractConfig
        implements PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor for POF and JSON serialization.
     */
    public OpenAiStreamingChatModelConfig()
        {
        }


    // ---- properties ------------------------------------------------------

    /**
     * Gets the OpenAI API base URL.
     *
     * @return the base URL to use for API requests
     */
    public String getBaseUrl()
        {
        return baseUrl;
        }

    /**
     * Sets the OpenAI API base URL.
     *
     * @param baseUrl the base URL to use for API requests
     *
     * @return this config instance for method chaining
     */
    public OpenAiStreamingChatModelConfig setBaseUrl(String baseUrl)
        {
        this.baseUrl = baseUrl;
        return this;
        }

    /**
     * Gets the OpenAI API key.
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
    public OpenAiStreamingChatModelConfig setApiKey(String apiKey)
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
    public OpenAiStreamingChatModelConfig setOrganizationId(String organizationId)
        {
        this.organizationId = organizationId;
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
     * Sets the project ID.
     *
     * @param projectId the project ID
     *
     * @return this config instance for method chaining
     */
    public OpenAiStreamingChatModelConfig setProjectId(String projectId)
        {
        this.projectId = projectId;
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
    public OpenAiStreamingChatModelConfig setModelName(String modelName)
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
    public OpenAiStreamingChatModelConfig setTemperature(Double temperature)
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
    public OpenAiStreamingChatModelConfig setTopP(Double topP)
        {
        this.topP = topP;
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
    public OpenAiStreamingChatModelConfig setStop(List<String> stop)
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
    public OpenAiStreamingChatModelConfig setMaxTokens(Integer maxTokens)
        {
        this.maxTokens = maxTokens;
        return this;
        }

    /**
     * Gets the limit for tokens in the completion output.
     *
     * @return the maximum completion token count
     */
    public Integer getMaxCompletionTokens()
        {
        return maxCompletionTokens;
        }

    /**
     * Sets the limit for tokens in the completion output.
     *
     * @param maxCompletionTokens the maximum completion token count
     *
     * @return this config instance for method chaining
     */
    public OpenAiStreamingChatModelConfig setMaxCompletionTokens(Integer maxCompletionTokens)
        {
        this.maxCompletionTokens = maxCompletionTokens;
        return this;
        }

    /**
     * Gets the presence penalty value, reducing repetition of new tokens.
     *
     * @return the presence penalty value
     */
    public Double getPresencePenalty()
        {
        return presencePenalty;
        }

    /**
     * Sets the presence penalty value to reduce repetition of new tokens.
     *
     * @param presencePenalty the presence penalty
     *
     * @return this config instance for method chaining
     */
    public OpenAiStreamingChatModelConfig setPresencePenalty(Double presencePenalty)
        {
        this.presencePenalty = presencePenalty;
        return this;
        }

    /**
     * Gets the frequency penalty value, discouraging frequent tokens.
     *
     * @return the frequency penalty value
     */
    public Double getFrequencyPenalty()
        {
        return frequencyPenalty;
        }

    /**
     * Sets the frequency penalty value to discourage frequent tokens.
     *
     * @param frequencyPenalty the frequency penalty
     *
     * @return this config instance for method chaining
     */
    public OpenAiStreamingChatModelConfig setFrequencyPenalty(Double frequencyPenalty)
        {
        this.frequencyPenalty = frequencyPenalty;
        return this;
        }

    /**
     * Gets the logit bias map to influence token likelihood.
     *
     * @return the logit bias map
     */
    public Map<String, Integer> getLogitBias()
        {
        return logitBias;
        }

    /**
     * Sets the logit bias map to influence token likelihood.
     *
     * @param logitBias the logit bias map
     *
     * @return this config instance for method chaining
     */
    public OpenAiStreamingChatModelConfig setLogitBias(Map<String, Integer> logitBias)
        {
        this.logitBias = logitBias;
        return this;
        }

    /**
     * Gets the expected response format.
     *
     * @return the response format
     */
    public String getResponseFormat()
        {
        return responseFormat;
        }

    /**
     * Sets the expected response format.
     *
     * @param responseFormat the response format
     *
     * @return this config instance for method chaining
     */
    public OpenAiStreamingChatModelConfig setResponseFormat(String responseFormat)
        {
        this.responseFormat = responseFormat;
        return this;
        }

    /**
     * Returns whether strict JSON schema validation is enabled.
     *
     * @return true if strict JSON schema validation is enabled
     */
    public Boolean isStrictJsonSchema()
        {
        return strictJsonSchema;
        }

    /**
     * Enables or disables strict JSON schema validation.
     *
     * @param strictJsonSchema whether to enable strict JSON schema validation
     *
     * @return this config instance for method chaining
     */
    public OpenAiStreamingChatModelConfig setStrictJsonSchema(Boolean strictJsonSchema)
        {
        this.strictJsonSchema = strictJsonSchema;
        return this;
        }

    /**
     * Gets the seed value used for deterministic generation.
     *
     * @return the seed value
     */
    public Integer getSeed()
        {
        return seed;
        }

    /**
     * Sets the seed value used for deterministic generation.
     *
     * @param seed the seed value
     *
     * @return this config instance for method chaining
     */
    public OpenAiStreamingChatModelConfig setSeed(Integer seed)
        {
        this.seed = seed;
        return this;
        }

    /**
     * Gets the user ID associated with the request.
     *
     * @return the user ID
     */
    public String getUser()
        {
        return user;
        }

    /**
     * Sets the user ID associated with the request.
     *
     * @param user the user ID
     *
     * @return this config instance for method chaining
     */
    public OpenAiStreamingChatModelConfig setUser(String user)
        {
        this.user = user;
        return this;
        }

    /**
     * Returns whether strict tool validation is enabled.
     *
     * @return true if strict tool validation is enabled
     */
    public Boolean isStrictTools()
        {
        return strictTools;
        }

    /**
     * Enables or disables strict tool validation.
     *
     * @param strictTools whether to enable strict tool validation
     *
     * @return this config instance for method chaining
     */
    public OpenAiStreamingChatModelConfig setStrictTools(Boolean strictTools)
        {
        this.strictTools = strictTools;
        return this;
        }

    /**
     * Returns whether parallel tool calls are enabled.
     *
     * @return true if parallel tool calls are enabled
     */
    public Boolean isParallelToolCalls()
        {
        return parallelToolCalls;
        }

    /**
     * Enables or disables parallel tool calls.
     *
     * @param parallelToolCalls whether to enable parallel tool calls
     *
     * @return this config instance for method chaining
     */
    public OpenAiStreamingChatModelConfig setParallelToolCalls(Boolean parallelToolCalls)
        {
        this.parallelToolCalls = parallelToolCalls;
        return this;
        }

    /**
     * Returns whether messages are stored.
     *
     * @return true if message storage is enabled
     */
    public Boolean isStore()
        {
        return store;
        }

    /**
     * Enables or disables message storage.
     *
     * @param store whether to enable message storage
     *
     * @return this config instance for method chaining
     */
    public OpenAiStreamingChatModelConfig setStore(Boolean store)
        {
        this.store = store;
        return this;
        }

    /**
     * Gets the metadata to include with the request.
     *
     * @return the metadata map
     */
    public Map<String, String> getMetadata()
        {
        return metadata;
        }

    /**
     * Sets the metadata to include with the request.
     *
     * @param metadata the metadata map
     *
     * @return this config instance for method chaining
     */
    public OpenAiStreamingChatModelConfig setMetadata(Map<String, String> metadata)
        {
        this.metadata = metadata;
        return this;
        }

    /**
     * Gets the service tier to use for OpenAI requests.
     *
     * @return the service tier
     */
    public String getServiceTier()
        {
        return serviceTier;
        }

    /**
     * Sets the service tier to use for OpenAI requests.
     *
     * @param serviceTier the service tier
     *
     * @return this config instance for method chaining
     */
    public OpenAiStreamingChatModelConfig setServiceTier(String serviceTier)
        {
        this.serviceTier = serviceTier;
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
    public OpenAiStreamingChatModelConfig setTimeout(Duration timeout)
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
    public OpenAiStreamingChatModelConfig setLogRequests(Boolean logRequests)
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
    public OpenAiStreamingChatModelConfig setLogResponses(Boolean logResponses)
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
    public OpenAiStreamingChatModelConfig setCustomHeaders(Map<String, String> customHeaders)
        {
        this.customHeaders = customHeaders;
        return this;
        }

    // ---- strongly typed builder conversion -------------------------------

    /**
     * Converts this configuration to a strongly-typed builder instance.
     *
     * @return a configured {@link OpenAiStreamingChatModelBuilder}
     */
    public OpenAiStreamingChatModelBuilder toBuilder()
        {
        return OpenAiStreamingChatModel.builder()
                .modelName(modelName)
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .organizationId(organizationId)
                .projectId(projectId)
                .temperature(temperature)
                .topP(topP)
                .stop(stop)
                .maxTokens(maxTokens)
                .maxCompletionTokens(maxCompletionTokens)
                .presencePenalty(presencePenalty)
                .frequencyPenalty(frequencyPenalty)
                .logitBias(logitBias)
                .responseFormat(responseFormat)
                .strictJsonSchema(strictJsonSchema)
                .seed(seed)
                .user(user)
                .strictTools(strictTools)
                .parallelToolCalls(parallelToolCalls)
                .store(store)
                .metadata(metadata)
                .serviceTier(serviceTier)
                .timeout(timeout)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .customHeaders(customHeaders);
        }

    // ---- Object methods --------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (!(o instanceof OpenAiStreamingChatModelConfig that)) return false;
        return Objects.equals(baseUrl, that.baseUrl) &&
               Objects.equals(apiKey, that.apiKey) &&
               Objects.equals(organizationId, that.organizationId) &&
               Objects.equals(projectId, that.projectId) &&
               Objects.equals(modelName, that.modelName) &&
               Objects.equals(temperature, that.temperature) &&
               Objects.equals(topP, that.topP) &&
               Objects.equals(stop, that.stop) &&
               Objects.equals(maxTokens, that.maxTokens) &&
               Objects.equals(maxCompletionTokens, that.maxCompletionTokens) &&
               Objects.equals(presencePenalty, that.presencePenalty) &&
               Objects.equals(frequencyPenalty, that.frequencyPenalty) &&
               Objects.equals(logitBias, that.logitBias) &&
               Objects.equals(responseFormat, that.responseFormat) &&
               Objects.equals(strictJsonSchema, that.strictJsonSchema) &&
               Objects.equals(seed, that.seed) &&
               Objects.equals(user, that.user) &&
               Objects.equals(strictTools, that.strictTools) &&
               Objects.equals(parallelToolCalls, that.parallelToolCalls) &&
               Objects.equals(store, that.store) &&
               Objects.equals(metadata, that.metadata) &&
               Objects.equals(serviceTier, that.serviceTier) &&
               Objects.equals(timeout, that.timeout) &&
               Objects.equals(logRequests, that.logRequests) &&
               Objects.equals(logResponses, that.logResponses) &&
               Objects.equals(customHeaders, that.customHeaders);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(baseUrl, apiKey, organizationId, projectId,
                            modelName, temperature, topP, stop, maxTokens, maxCompletionTokens,
                            presencePenalty, frequencyPenalty, logitBias, responseFormat, strictJsonSchema,
                            seed, user, strictTools, parallelToolCalls, store, metadata, serviceTier,
                            timeout, logRequests, logResponses, customHeaders);
        }

    @Override
    public String toString()
        {
        return "OpenAiStreamingChatModelConfig[" +
               "baseUrl=" + baseUrl +
               ", apiKey=" + apiKey +
               ", organizationId=" + organizationId +
               ", projectId=" + projectId +
               ", modelName=" + modelName +
               ", temperature=" + temperature +
               ", topP=" + topP +
               ", stop=" + stop +
               ", maxTokens=" + maxTokens +
               ", maxCompletionTokens=" + maxCompletionTokens +
               ", presencePenalty=" + presencePenalty +
               ", frequencyPenalty=" + frequencyPenalty +
               ", logitBias=" + logitBias +
               ", responseFormat=" + responseFormat +
               ", strictJsonSchema=" + strictJsonSchema +
               ", seed=" + seed +
               ", user=" + user +
               ", strictTools=" + strictTools +
               ", parallelToolCalls=" + parallelToolCalls +
               ", store=" + store +
               ", metadata=" + metadata +
               ", serviceTier=" + serviceTier +
               ", timeout=" + timeout +
               ", logRequests=" + logRequests +
               ", logResponses=" + logResponses +
               ", customHeaders=" + customHeaders + ']' ;
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
        organizationId           = reader.readString(2);
        projectId                = reader.readString(3);
        modelName                = reader.readString(5);
        temperature              = reader.readDouble(6);
        topP                     = reader.readDouble(7);
        stop                     = reader.readCollection(8, new ArrayList<>());
        maxTokens                = reader.readInt(9);
        maxCompletionTokens      = reader.readInt(10);
        presencePenalty          = reader.readDouble(11);
        frequencyPenalty         = reader.readDouble(12);
        logitBias                = reader.readMap(13, new LinkedHashMap<>());
        responseFormat           = reader.readString(14);
        strictJsonSchema         = reader.readBoolean(15);
        seed                     = reader.readInt(16);
        user                     = reader.readString(17);
        strictTools              = reader.readBoolean(18);
        parallelToolCalls        = reader.readBoolean(19);
        store                    = reader.readBoolean(20);
        metadata                 = reader.readMap(21, new LinkedHashMap<>());
        serviceTier              = reader.readString(22);
        timeout                  = reader.readObject(23);
        logRequests              = reader.readBoolean(24);
        logResponses             = reader.readBoolean(25);
        customHeaders            = reader.readMap(26, new LinkedHashMap<>());
        }

    @Override
    public void writeExternal(PofWriter writer) throws IOException
        {
        writer.writeString(0, baseUrl);
        writer.writeString(1, apiKey);
        writer.writeString(2, organizationId);
        writer.writeString(3, projectId);
        writer.writeString(5, modelName);
        writer.writeDouble(6, temperature);
        writer.writeDouble(7, topP);
        writer.writeCollection(8, stop);
        writer.writeInt(9, maxTokens);
        writer.writeInt(10, maxCompletionTokens);
        writer.writeDouble(11, presencePenalty);
        writer.writeDouble(12, frequencyPenalty);
        writer.writeMap(13, logitBias);
        writer.writeString(14, responseFormat);
        writer.writeBoolean(15, strictJsonSchema);
        writer.writeInt(16, seed);
        writer.writeString(17, user);
        writer.writeBoolean(18, strictTools);
        writer.writeBoolean(19, parallelToolCalls);
        writer.writeBoolean(20, store);
        writer.writeMap(21, metadata);
        writer.writeString(22, serviceTier);
        writer.writeObject(23, timeout);
        writer.writeBoolean(24, logRequests);
        writer.writeBoolean(25, logResponses);
        writer.writeMap(26, customHeaders);
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
    private String projectId;
    private String modelName;
    private Double temperature;
    private Double topP;
    private List<String> stop;
    private Integer maxTokens;
    private Integer maxCompletionTokens;
    private Double presencePenalty;
    private Double frequencyPenalty;
    private Map<String, Integer> logitBias;
    private String responseFormat;
    private Boolean strictJsonSchema;
    private Integer seed;
    private String user;
    private Boolean strictTools;
    private Boolean parallelToolCalls;
    private Boolean store;
    private Map<String, String> metadata;
    private String serviceTier;
    private Duration timeout;
    private Boolean logRequests;
    private Boolean logResponses;
    private Map<String, String> customHeaders;
    }
