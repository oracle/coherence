/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.ollama.config;

import com.oracle.coherence.rag.config.model.ChatModelConfig;

import dev.langchain4j.model.ollama.OllamaChatModel.OllamaChatModelBuilder;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Configuration class for Ollama chat model.
 * <p/>
 * Encapsulates configuration parameters that control how Ollama chat
 * models are used within the Coherence RAG framework.
 *
 * @author Aleks Seovic  2025.07.13
 * @since 25.09
 */
@SuppressWarnings("unused")
public class OllamaChatModelConfig
        extends ChatModelConfig<OllamaChatModelBuilder>
    {
    // ---- properties ------------------------------------------------------

    /**
     * Gets the temperature value controlling randomness.
     *
     * @return the temperature
     */
    public Double getTemperature()
        {
        return temperature;
        }

    /**
     * Sets the temperature value controlling randomness.
     *
     * @param temperature the temperature value
     *
     * @return this config instance
     */
    public OllamaChatModelConfig setTemperature(Double temperature)
        {
        this.temperature = temperature;
        return this;
        }

    /**
     * Gets the top-K sampling value.
     *
     * @return the top-K value
     */
    public Integer getTopK()
        {
        return topK;
        }

    /**
     * Sets the top-K sampling value.
     *
     * @param topK the top-K value
     *
     * @return this config instance
     */
    public OllamaChatModelConfig setTopK(Integer topK)
        {
        this.topK = topK;
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
     * @param topP the top-p value
     *
     * @return this config instance
     */
    public OllamaChatModelConfig setTopP(Double topP)
        {
        this.topP = topP;
        return this;
        }

    /**
     * Gets the Mirostat sampling algorithm mode.
     *
     * @return the mirostat mode
     */
    public Integer getMirostat()
        {
        return mirostat;
        }

    /**
     * Sets the Mirostat sampling algorithm mode.
     *
     * @param mirostat the mirostat mode
     *
     * @return this config instance
     */
    public OllamaChatModelConfig setMirostat(Integer mirostat)
        {
        this.mirostat = mirostat;
        return this;
        }

    /**
     * Gets the Mirostat learning rate (eta).
     *
     * @return the mirostat eta
     */
    public Double getMirostatEta()
        {
        return mirostatEta;
        }

    /**
     * Sets the Mirostat learning rate (eta).
     *
     * @param mirostatEta the mirostat eta
     *
     * @return this config instance
     */
    public OllamaChatModelConfig setMirostatEta(Double mirostatEta)
        {
        this.mirostatEta = mirostatEta;
        return this;
        }

    /**
     * Gets the Mirostat tau parameter.
     *
     * @return the mirostat tau
     */
    public Double getMirostatTau()
        {
        return mirostatTau;
        }

    /**
     * Sets the Mirostat tau parameter.
     *
     * @param mirostatTau the mirostat tau
     *
     * @return this config instance
     */
    public OllamaChatModelConfig setMirostatTau(Double mirostatTau)
        {
        this.mirostatTau = mirostatTau;
        return this;
        }

    /**
     * Gets the context window size.
     *
     * @return the context window size
     */
    public Integer getNumCtx()
        {
        return numCtx;
        }

    /**
     * Sets the context window size.
     *
     * @param numCtx the context window size
     *
     * @return this config instance
     */
    public OllamaChatModelConfig setNumCtx(Integer numCtx)
        {
        this.numCtx = numCtx;
        return this;
        }

    /**
     * Gets the number of last tokens to consider for repetition penalty.
     *
     * @return the number of last tokens
     */
    public Integer getRepeatLastN()
        {
        return repeatLastN;
        }

    /**
     * Sets the number of last tokens to consider for repetition penalty.
     *
     * @param repeatLastN the number of last tokens
     *
     * @return this config instance
     */
    public OllamaChatModelConfig setRepeatLastN(Integer repeatLastN)
        {
        this.repeatLastN = repeatLastN;
        return this;
        }

    /**
     * Gets the repetition penalty value.
     *
     * @return the repetition penalty
     */
    public Double getRepeatPenalty()
        {
        return repeatPenalty;
        }

    /**
     * Sets the repetition penalty value.
     *
     * @param repeatPenalty the repetition penalty
     *
     * @return this config instance
     */
    public OllamaChatModelConfig setRepeatPenalty(Double repeatPenalty)
        {
        this.repeatPenalty = repeatPenalty;
        return this;
        }

    /**
     * Gets the random seed for reproducibility.
     *
     * @return the seed value
     */
    public Integer getSeed()
        {
        return seed;
        }

    /**
     * Sets the random seed for reproducibility.
     *
     * @param seed the seed value
     *
     * @return this config instance
     */
    public OllamaChatModelConfig setSeed(Integer seed)
        {
        this.seed = seed;
        return this;
        }

    /**
     * Gets the number of tokens to predict.
     *
     * @return the number of tokens to predict
     */
    public Integer getNumPredict()
        {
        return numPredict;
        }

    /**
     * Sets the number of tokens to predict.
     *
     * @param numPredict the number of tokens to predict
     *
     * @return this config instance
     */
    public OllamaChatModelConfig setNumPredict(Integer numPredict)
        {
        this.numPredict = numPredict;
        return this;
        }

    /**
     * Gets the list of stop sequences.
     *
     * @return the stop sequence list
     */
    public List<String> getStop()
        {
        return stop;
        }

    /**
     * Sets the list of stop sequences.
     *
     * @param stop the stop sequence list
     *
     * @return this config instance
     */
    public OllamaChatModelConfig setStop(List<String> stop)
        {
        this.stop = stop;
        return this;
        }

    /**
     * Gets the minimum p value for nucleus sampling.
     *
     * @return the min p value
     */
    public Double getMinP()
        {
        return minP;
        }

    /**
     * Sets the minimum p value for nucleus sampling.
     *
     * @param minP the min p value
     *
     * @return this config instance
     */
    public OllamaChatModelConfig setMinP(Double minP)
        {
        this.minP = minP;
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
     * @return this config instance
     */
    public OllamaChatModelConfig setTimeout(Duration timeout)
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
     * @return this config instance
     */
    public OllamaChatModelConfig setMaxRetries(Integer maxRetries)
        {
        this.maxRetries = maxRetries;
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
     * @return this config instance
     */
    public OllamaChatModelConfig setCustomHeaders(Map<String, String> customHeaders)
        {
        this.customHeaders = customHeaders;
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
     * @return this config instance
     */
    public OllamaChatModelConfig setLogRequests(Boolean logRequests)
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
     * @return this config instance
     */
    public OllamaChatModelConfig setLogResponses(Boolean logResponses)
        {
        this.logResponses = logResponses;
        return this;
        }

    // ---- AbstractConfig methods ------------------------------------------

    @Override
    public OllamaChatModelBuilder apply(OllamaChatModelBuilder target)
        {
        return target
                .temperature(temperature)
                .topK(topK)
                .topP(topP)
                .mirostat(mirostat)
                .mirostatEta(mirostatEta)
                .mirostatTau(mirostatTau)
                .numCtx(numCtx)
                .repeatLastN(repeatLastN)
                .repeatPenalty(repeatPenalty)
                .seed(seed)
                .numPredict(numPredict)
                .stop(stop)
                .minP(minP)
                .timeout(timeout)
                .maxRetries(maxRetries)
                .customHeaders(customHeaders)
                .logRequests(logRequests)
                .logResponses(logResponses);
        }

    // ---- Object methods --------------------------------------------------

    @Override
    public String toString()
        {
        return "OllamaChatModelConfig[" +
               "temperature=" + temperature +
               ", topK=" + topK +
               ", topP=" + topP +
               ", mirostat=" + mirostat +
               ", mirostatEta=" + mirostatEta +
               ", mirostatTau=" + mirostatTau +
               ", numCtx=" + numCtx +
               ", repeatLastN=" + repeatLastN +
               ", repeatPenalty=" + repeatPenalty +
               ", seed=" + seed +
               ", numPredict=" + numPredict +
               ", stop=" + stop +
               ", minP=" + minP +
               ", timeout=" + timeout +
               ", maxRetries=" + maxRetries +
               ", customHeaders=" + customHeaders +
               ", logRequests=" + logRequests +
               ", logResponses=" + logResponses + ']';
        }

    // ---- data members ----------------------------------------------------

    private Double temperature;
    private Integer topK;
    private Double topP;
    private Integer mirostat;
    private Double mirostatEta;
    private Double mirostatTau;
    private Integer numCtx;
    private Integer repeatLastN;
    private Double repeatPenalty;
    private Integer seed;
    private Integer numPredict;
    private List<String> stop;
    private Double minP;
    private Duration timeout;
    private Integer maxRetries;
    private Map<String, String> customHeaders;
    private Boolean logRequests;
    private Boolean logResponses;
    }
