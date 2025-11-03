/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.deepseek.config;

import com.oracle.coherence.rag.config.model.StreamingChatModelConfig;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Configuration class for DeepSeek streaming chat model.
 * <p/>
 * Encapsulates configuration parameters that control how DeepSeek streaming chat
 * models are used within the Coherence RAG framework.
 *
 * @author Aleks Seovic  2025.07.13
 * @since 25.09
 */
@SuppressWarnings("unused")
public class DeepSeekStreamingChatModelConfig
        extends StreamingChatModelConfig<OpenAiStreamingChatModelBuilder>
    {
    // ---- properties ------------------------------------------------------

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
    public DeepSeekStreamingChatModelConfig setOrganizationId(String organizationId)
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
    public DeepSeekStreamingChatModelConfig setProjectId(String projectId)
        {
        this.projectId = projectId;
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
    public DeepSeekStreamingChatModelConfig setTemperature(Double temperature)
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
    public DeepSeekStreamingChatModelConfig setTopP(Double topP)
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
    public DeepSeekStreamingChatModelConfig setStop(List<String> stop)
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
    public DeepSeekStreamingChatModelConfig setMaxTokens(Integer maxTokens)
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
    public DeepSeekStreamingChatModelConfig setMaxCompletionTokens(Integer maxCompletionTokens)
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
    public DeepSeekStreamingChatModelConfig setPresencePenalty(Double presencePenalty)
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
    public DeepSeekStreamingChatModelConfig setFrequencyPenalty(Double frequencyPenalty)
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
    public DeepSeekStreamingChatModelConfig setLogitBias(Map<String, Integer> logitBias)
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
    public DeepSeekStreamingChatModelConfig setResponseFormat(String responseFormat)
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
    public DeepSeekStreamingChatModelConfig setStrictJsonSchema(Boolean strictJsonSchema)
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
    public DeepSeekStreamingChatModelConfig setSeed(Integer seed)
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
    public DeepSeekStreamingChatModelConfig setUser(String user)
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
    public DeepSeekStreamingChatModelConfig setStrictTools(Boolean strictTools)
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
    public DeepSeekStreamingChatModelConfig setParallelToolCalls(Boolean parallelToolCalls)
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
    public DeepSeekStreamingChatModelConfig setStore(Boolean store)
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
    public DeepSeekStreamingChatModelConfig setMetadata(Map<String, String> metadata)
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
    public DeepSeekStreamingChatModelConfig setServiceTier(String serviceTier)
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
    public DeepSeekStreamingChatModelConfig setTimeout(Duration timeout)
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
    public DeepSeekStreamingChatModelConfig setLogRequests(Boolean logRequests)
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
    public DeepSeekStreamingChatModelConfig setLogResponses(Boolean logResponses)
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
    public DeepSeekStreamingChatModelConfig setCustomHeaders(Map<String, String> customHeaders)
        {
        this.customHeaders = customHeaders;
        return this;
        }

    // ---- AbstractConfig methods ------------------------------------------

    @Override
    public OpenAiStreamingChatModelBuilder apply(OpenAiStreamingChatModelBuilder target)
        {
        return target
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
    public String toString()
        {
        return "DeepSeekStreamingChatModelConfig[" +
               "organizationId=" + organizationId +
               ", projectId=" + projectId +
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

    // ---- data members ----------------------------------------------------

    private String organizationId;
    private String projectId;
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
