/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.Result;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ai.onnxruntime.OnnxTensor.createTensor;
import static dev.langchain4j.internal.Exceptions.illegalArgument;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static java.nio.LongBuffer.wrap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

/**
 * ONNX-based BERT bi-encoder implementation for generating text embeddings.
 * <p/>
 * This class provides a bi-encoder architecture for generating embeddings from text
 * using ONNX Runtime and HuggingFace tokenizers. It supports both CLS and mean
 * pooling strategies and can handle long texts by partitioning them into smaller
 * chunks and aggregating the results.
 * <p/>
 * The bi-encoder architecture is particularly useful for semantic search and
 * similarity tasks where texts need to be embedded independently before comparison.
 * <p/>
 * Example usage:
 * <pre>
 * // Create bi-encoder from streams
 * OnnxBertBiEncoder encoder = new OnnxBertBiEncoder(
 *     modelInputStream,
 *     tokenizerInputStream,
 *     PoolingMode.MEAN
 * );
 * 
 * // Generate embedding for text
 * EmbeddingAndTokenCount result = encoder.embed("This is a sample text");
 * float[] embedding = result.embedding;
 * int tokenCount = result.tokenCount;
 * </pre>
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
public class OnnxBertBiEncoder
    {
    /**
     * Maximum sequence length for tokenization (512 - 2 special tokens).
     */
    private static final int MAX_SEQUENCE_LENGTH = 510; // 512 - 2 (special tokens [CLS] and [SEP])

    /**
     * The ONNX Runtime environment.
     */
    private final OrtEnvironment environment;
    
    /**
     * The ONNX Runtime session.
     */
    private final OrtSession session;
    
    /**
     * The set of expected model input names.
     */
    private final Set<String> expectedInputs;
    
    /**
     * The HuggingFace tokenizer.
     */
    private final HuggingFaceTokenizer tokenizer;
    
    /**
     * The pooling mode for aggregating token embeddings.
     */
    private final PoolingMode poolingMode;

    /**
     * Constructs a BERT bi-encoder from input streams.
     * 
     * @param model        the input stream for the model
     * @param tokenizer    the input stream for the tokenizer
     * @param poolingMode  the pooling mode to use
     */
    public OnnxBertBiEncoder(InputStream model, InputStream tokenizer, PoolingMode poolingMode)
        {
        try
            {
            this.environment = OrtEnvironment.getEnvironment();
            this.session = environment.createSession(loadModel(model));
            this.expectedInputs = session.getInputNames();
            this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizer, singletonMap("padding", "false"));
            this.poolingMode = ensureNotNull(poolingMode, "poolingMode");
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    /**
     * Constructs a BERT bi-encoder with existing ONNX Runtime components.
     * 
     * @param environment  the ONNX Runtime environment
     * @param session      the ONNX Runtime session
     * @param tokenizer    the input stream for the tokenizer
     * @param poolingMode  the pooling mode to use
     */
    public OnnxBertBiEncoder(OrtEnvironment environment, OrtSession session, InputStream tokenizer, PoolingMode poolingMode)
        {
        try
            {
            this.environment = environment;
            this.session = session;
            this.expectedInputs = session.getInputNames();
            this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizer, singletonMap("padding", "false"));
            this.poolingMode = ensureNotNull(poolingMode, "poolingMode");
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    /**
     * Container for embedding results and token count.
     */
    static class EmbeddingAndTokenCount
        {
        /**
         * The generated embedding vector.
         */
        float[] embedding;
        
        /**
         * The number of tokens processed.
         */
        int tokenCount;

        /**
         * Constructs an embedding result container.
         * 
         * @param embedding   the embedding vector
         * @param tokenCount  the token count
         */
        EmbeddingAndTokenCount(float[] embedding, int tokenCount)
            {
            this.embedding = embedding;
            this.tokenCount = tokenCount;
            }
        }

    /**
     * Generates an embedding for the given text.
     * 
     * @param text  the text to embed
     *
     * @return the embedding result with token count
     */
    EmbeddingAndTokenCount embed(String text)
        {

        List<String> tokens = tokenizer.tokenize(text);
        List<List<String>> partitions = partition(tokens, MAX_SEQUENCE_LENGTH);

        List<float[]> embeddings = new ArrayList<>();
        for (List<String> partition : partitions)
            {
            try (Result result = encode(partition))
                {
                float[] embedding = toEmbedding(result);
                embeddings.add(embedding);
                }
            catch (OrtException e)
                {
                throw new RuntimeException(e);
                }
            }

        List<Integer> weights = partitions.stream()
                .map(List::size)
                .collect(toList());

        float[] embedding = normalize(weightedAverage(embeddings, weights));

        return new EmbeddingAndTokenCount(embedding, tokens.size());
        }

    /**
     * Partitions tokens into chunks that fit within the sequence length limit.
     * 
     * @param tokens        the list of tokens to partition
     * @param partitionSize the maximum partition size
     *
     * @return the list of token partitions
     */
    static List<List<String>> partition(List<String> tokens, int partitionSize)
        {
        List<List<String>> partitions = new ArrayList<>();
        int from = 1; // Skip the first (CLS) token

        while (from < tokens.size() - 1)
            { // Skip the last (SEP) token
            int to = from + partitionSize;

            if (to >= tokens.size() - 1)
                {
                to = tokens.size() - 1;
                }
            else
                {
                // ensure we don't split word across partitions
                while (tokens.get(to).startsWith("##"))
                    {
                    to--;
                    }
                }

            partitions.add(tokens.subList(from, to));

            from = to;
            }

        return partitions;
        }

    /**
     * Encodes a list of tokens using the ONNX model.
     * 
     * @param tokens  the tokens to encode
     *
     * @return the ONNX model result
     * @throws OrtException  if there's an error during encoding
     */
    private Result encode(List<String> tokens) throws OrtException
        {

        Encoding encoding = tokenizer.encode(toText(tokens), true, false);

        long[] inputIds = encoding.getIds();
        long[] attentionMask = encoding.getAttentionMask();
        long[] tokenTypeIds = encoding.getTypeIds();

        long[] shape = {1, inputIds.length};

        try (
                OnnxTensor inputIdsTensor = createTensor(environment, wrap(inputIds), shape);
                OnnxTensor attentionMaskTensor = createTensor(environment, wrap(attentionMask), shape);
                OnnxTensor tokenTypeIdsTensor = createTensor(environment, wrap(tokenTypeIds), shape)
        )
            {
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", inputIdsTensor);
            inputs.put("attention_mask", attentionMaskTensor);

            if (expectedInputs.contains("token_type_ids"))
                {
                inputs.put("token_type_ids", tokenTypeIdsTensor);
                }

            return session.run(inputs);
            }
        }

    /**
     * Converts a list of tokens back to text for encoding.
     * 
     * @param tokens  the tokens to convert
     *
     * @return the reconstructed text
     */
    private String toText(List<String> tokens)
        {

        String text = tokenizer.buildSentence(tokens);

        List<String> tokenized = tokenizer.tokenize(text);
        List<String> tokenizedWithoutSpecialTokens = new LinkedList<>(tokenized);
        tokenizedWithoutSpecialTokens.remove(0);
        tokenizedWithoutSpecialTokens.remove(tokenizedWithoutSpecialTokens.size() - 1);

        if (tokenizedWithoutSpecialTokens.equals(tokens))
            {
            return text;
            }
        else
            {
            return String.join("", tokens);
            }
        }

    /**
     * Converts the model result to an embedding vector.
     * 
     * @param result  the ONNX model result
     *
     * @return the embedding vector
     * @throws OrtException  if there's an error extracting the embedding
     */
    private float[] toEmbedding(Result result) throws OrtException
        {
        float[][] vectors = ((float[][][]) result.get(0).getValue())[0];
        return pool(vectors);
        }

    /**
     * Applies pooling to the token vectors based on the pooling mode.
     * 
     * @param vectors  the token vectors
     *
     * @return the pooled embedding vector
     */
    private float[] pool(float[][] vectors)
        {
        switch (poolingMode)
            {
            case CLS:
                return clsPool(vectors);
            case MEAN:
                return meanPool(vectors);
            default:
                throw illegalArgument("Unsupported pooling mode: " + poolingMode);
            }
        }

    /**
     * Performs CLS pooling by extracting the first token's embedding.
     * 
     * @param vectors  the token vectors
     *
     * @return the CLS token embedding
     */
    private static float[] clsPool(float[][] vectors)
        {
        return vectors[0];
        }

    /**
     * Performs mean pooling by averaging all token embeddings.
     * 
     * @param vectors  the token vectors
     *
     * @return the mean-pooled embedding
     */
    private static float[] meanPool(float[][] vectors)
        {
        float[] mean = new float[vectors[0].length];
        for (float[] vector : vectors)
            {
            for (int i = 0; i < vector.length; i++)
                {
                mean[i] += vector[i];
                }
            }

        for (int i = 0; i < mean.length; i++)
            {
            mean[i] /= vectors.length;
            }

        return mean;
        }

    /**
     * Computes a weighted average of multiple embeddings.
     * 
     * @param embeddings  the list of embeddings to average
     * @param weights     the weights for each embedding
     *
     * @return the weighted average embedding
     */
    private float[] weightedAverage(List<float[]> embeddings, List<Integer> weights)
        {
        if (embeddings.isEmpty())
            {
            throw illegalArgument("Cannot compute weighted average of empty embeddings list");
            }

        float[] avg = new float[embeddings.get(0).length];
        int totalWeight = 0;

        for (int i = 0; i < embeddings.size(); i++)
            {
            float[] embedding = embeddings.get(i);
            int weight = weights.get(i);

            for (int j = 0; j < embedding.length; j++)
                {
                avg[j] += embedding[j] * weight;
                }

            totalWeight += weight;
            }

        for (int i = 0; i < avg.length; i++)
            {
            avg[i] /= totalWeight;
            }

        return avg;
        }

    /**
     * Normalizes a vector to unit length.
     * 
     * @param vector  the vector to normalize
     *
     * @return the normalized vector
     */
    private static float[] normalize(float[] vector)
        {
        float norm = 0.0f;
        for (float value : vector)
            {
            norm += value * value;
            }

        norm = (float) Math.sqrt(norm);

        if (norm == 0.0f)
            {
            return vector;
            }

        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++)
            {
            normalized[i] = vector[i] / norm;
            }

        return normalized;
        }

    /**
     * Counts the number of tokens in a text.
     * 
     * @param text  the text to tokenize
     *
     * @return the token count
     */
    int countTokens(String text)
        {
        return tokenizer.tokenize(text).size();
        }

    /**
     * Loads the model from an input stream.
     * 
     * @param modelInputStream  the model input stream
     *
     * @return the model bytes
     */
    private byte[] loadModel(InputStream modelInputStream)
        {
        try (
                InputStream inputStream = modelInputStream;
                ByteArrayOutputStream buffer = new ByteArrayOutputStream()
        )
            {
            int nRead;
            byte[] data = new byte[1024];

            while ((nRead = inputStream.read(data, 0, data.length)) != -1)
                {
                buffer.write(data, 0, nRead);
                }

            buffer.flush();
            return buffer.toByteArray();
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }
        }
    }
