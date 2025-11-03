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

import java.io.InputStream;

import java.util.HashMap;
import java.util.Map;

import static ai.onnxruntime.OnnxTensor.createTensor;
import static java.nio.LongBuffer.wrap;

/**
 * ONNX-based BERT cross-encoder implementation for scoring text pairs.
 * <p/>
 * This class provides a cross-encoder architecture for computing relevance scores
 * between query-document pairs using ONNX Runtime and HuggingFace tokenizers.
 * Unlike bi-encoders, cross-encoders process both texts together, enabling
 * more sophisticated interaction modeling at the cost of not being able to
 * pre-compute embeddings.
 * <p/>
 * The cross-encoder architecture is particularly effective for reranking tasks
 * in information retrieval and question-answering systems where high accuracy
 * is more important than computational efficiency.
 * <p/>
 * Example usage:
 * <pre>
 * // Create cross-encoder with existing ONNX components
 * OnnxBertCrossEncoder encoder = new OnnxBertCrossEncoder(
 *     environment,
 *     session,
 *     tokenizerInputStream
 * );
 * 
 * // Score query-document pair
 * String query = "What is machine learning?";
 * String document = "Machine learning is a subset of artificial intelligence...";
 * float score = encoder.encode(query, document);
 * </pre>
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
public class OnnxBertCrossEncoder
    {
    /**
     * The ONNX Runtime environment.
     */
    private final OrtEnvironment environment;
    
    /**
     * The ONNX Runtime session.
     */
    private final OrtSession session;
    
    /**
     * The HuggingFace tokenizer.
     */
    private final HuggingFaceTokenizer tokenizer;

    /**
     * Constructs a BERT cross-encoder with existing ONNX Runtime components.
     * 
     * @param environment           the ONNX Runtime environment
     * @param session               the ONNX Runtime session
     * @param tokenizerInputStream  the input stream for the tokenizer
     */
    public OnnxBertCrossEncoder(OrtEnvironment environment, OrtSession session, InputStream tokenizerInputStream)
        {
        try
            {
            this.environment = environment;
            this.session     = session;
            this.tokenizer   = HuggingFaceTokenizer.newInstance(tokenizerInputStream, Map.of("truncation", "LONGEST_FIRST"));
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    /**
     * Closes the ONNX Runtime session and releases resources.
     */
    public void close()
        {
        try
            {
            tokenizer.close();
            session.close();
            environment.close();
            }
        catch (OrtException e)
            {
            throw new RuntimeException("Failed to close ONNX resources", e);
            }
        }

    /**
     * Encodes a query-document pair and returns the relevance score.
     * 
     * @param query  the query text
     * @param text   the document text
     *
     * @return the raw relevance score
     */
    public float encode(String query, String text)
        {
        try (Result result = run(query, text))
            {
            return ((float[][]) result.get(0).getValue())[0][0];
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    /**
     * Runs the ONNX model with the given query and text.
     * 
     * @param query  the query text
     * @param text   the document text
     *
     * @return the ONNX model result
     * @throws OrtException  if there's an error during model execution
     */
    private Result run(String query, String text) throws OrtException
        {
        Encoding encoding = tokenizer.encode(query, text, true, false);

        long[] inputIds      = encoding.getIds();
        long[] attentionMask = encoding.getAttentionMask();
        long[] tokenTypeIds  = encoding.getTypeIds();
        long[] shape         = new long[] {1L, inputIds.length};

        try (OnnxTensor tokensTensor = createTensor(environment, wrap(inputIds), shape);
             OnnxTensor attentionMasksTensor = createTensor(environment, wrap(attentionMask), shape);
             OnnxTensor tokenTypeIdsTensor = createTensor(environment, wrap(tokenTypeIds), shape))
            {
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", tokensTensor);
            inputs.put("attention_mask", attentionMasksTensor);
            if (session.getInputNames().contains("token_type_ids"))
                {
                inputs.put("token_type_ids", tokenTypeIdsTensor);
                }
            
            return session.run(inputs);
            }
        }
    }
