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

import dev.langchain4j.model.ollama.OllamaChatModel;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        extends AbstractConfig
        implements PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor for POF and JSON serialization.
     */
    public OllamaChatModelConfig()
        {
        }

    // ---- properties ------------------------------------------------------

    /**
     * Gets the Ollama API base URL.
     *
     * @return the base URL
     */
    public String getBaseUrl()
        {
        return baseUrl;
        }

    /**
     * Sets the Ollama API base URL.
     *
     * @param baseUrl the base URL
     *
     * @return this config instance
     */
    public OllamaChatModelConfig setBaseUrl(String baseUrl)
        {
        this.baseUrl = baseUrl;
        return this;
        }

    /**
     * Gets the embedding model name.
     *
     * @return the model name
     */
    public String getModelName()
        {
        return modelName;
        }

    /**
     * Sets the embedding model name.
     *
     * @param modelName the model name
     *
     * @return this config instance
     */
    public OllamaChatModelConfig setModelName(String modelName)
        {
        this.modelName = modelName;
        return this;
        }

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

    // ---- strongly typed builder conversion -------------------------------

    /**
     * Converts this configuration to a strongly-typed builder instance.
     *
     * @return a configured {@code OllamaChatModelBuilder}
     */
    public OllamaChatModel.OllamaChatModelBuilder toBuilder()
        {
        return dev.langchain4j.model.ollama.OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
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
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (!(o instanceof OllamaChatModelConfig that)) return false;
        return Objects.equals(baseUrl, that.baseUrl) &&
               Objects.equals(modelName, that.modelName) &&
               Objects.equals(temperature, that.temperature) &&
               Objects.equals(topK, that.topK) &&
               Objects.equals(topP, that.topP) &&
               Objects.equals(mirostat, that.mirostat) &&
               Objects.equals(mirostatEta, that.mirostatEta) &&
               Objects.equals(mirostatTau, that.mirostatTau) &&
               Objects.equals(numCtx, that.numCtx) &&
               Objects.equals(repeatLastN, that.repeatLastN) &&
               Objects.equals(repeatPenalty, that.repeatPenalty) &&
               Objects.equals(seed, that.seed) &&
               Objects.equals(numPredict, that.numPredict) &&
               Objects.equals(stop, that.stop) &&
               Objects.equals(minP, that.minP) &&
               Objects.equals(timeout, that.timeout) &&
               Objects.equals(maxRetries, that.maxRetries) &&
               Objects.equals(customHeaders, that.customHeaders) &&
               Objects.equals(logRequests, that.logRequests) &&
               Objects.equals(logResponses, that.logResponses);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(baseUrl, modelName, temperature, topK, topP, mirostat, mirostatEta, mirostatTau,
                numCtx, repeatLastN, repeatPenalty, seed, numPredict, stop, minP,
                timeout, maxRetries, customHeaders, logRequests, logResponses);
        }

    @Override
    public String toString()
        {
        return "OllamaChatModelConfig[" +
               "baseUrl=" + baseUrl +
               ", modelName=" + modelName +
               ", temperature=" + temperature +
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
        baseUrl        = reader.readString(0);
        modelName      = reader.readString(1);
        temperature    = reader.readDouble(2);
        topK           = reader.readInt(3);
        topP           = reader.readDouble(4);
        mirostat       = reader.readInt(5);
        mirostatEta    = reader.readDouble(6);
        mirostatTau    = reader.readDouble(7);
        numCtx         = reader.readInt(8);
        repeatLastN    = reader.readInt(9);
        repeatPenalty  = reader.readDouble(10);
        seed           = reader.readInt(11);
        numPredict     = reader.readInt(12);
        stop           = reader.readCollection(13, new java.util.ArrayList<>());
        minP           = reader.readDouble(14);
        timeout        = reader.readObject(17);
        maxRetries     = reader.readInt(18);
        customHeaders  = reader.readMap(19, new LinkedHashMap<>());
        logRequests    = reader.readBoolean(20);
        logResponses   = reader.readBoolean(21);
        }

    @Override
    public void writeExternal(PofWriter writer) throws IOException
        {
        writer.writeString(0, baseUrl);
        writer.writeString(1, modelName);
        writer.writeDouble(2, temperature);
        writer.writeInt(3, topK);
        writer.writeDouble(4, topP);
        writer.writeInt(5, mirostat);
        writer.writeDouble(6, mirostatEta);
        writer.writeDouble(7, mirostatTau);
        writer.writeInt(8, numCtx);
        writer.writeInt(9, repeatLastN);
        writer.writeDouble(10, repeatPenalty);
        writer.writeInt(11, seed);
        writer.writeInt(12, numPredict);
        writer.writeCollection(13, stop);
        writer.writeDouble(14, minP);
        writer.writeObject(17, timeout);
        writer.writeInt(18, maxRetries);
        writer.writeMap(19, customHeaders);
        writer.writeBoolean(20, logRequests);
        writer.writeBoolean(21, logResponses);
        }

    // ---- constants -------------------------------------------------------

    /**
     * The implementation version for this class.
     */
    public static final int IMPLEMENTATION_VERSION = 1;

    // ---- data members ----------------------------------------------------

    private String baseUrl;
    private String modelName;
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
