/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model;

/**
 * Enumeration of pooling modes for text embedding models.
 * <p/>
 * This enum defines the different strategies for pooling token embeddings
 * to create a single vector representation of a text sequence. The pooling
 * mode determines how the individual token embeddings from a transformer
 * model are combined into a final document or sentence embedding.
 * <p/>
 * Different pooling modes can affect the quality and characteristics of
 * the resulting embeddings, with some being more suitable for certain
 * types of text or downstream tasks.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
public enum PoolingMode
    {
    /**
     * Use the [CLS] token embedding as the final representation.
     * <p/>
     * This mode uses the special classification token embedding that is
     * typically placed at the beginning of the input sequence in BERT-style
     * models. The [CLS] token is specifically trained to aggregate information
     * from the entire sequence during pre-training.
     * <p/>
     * This is commonly used with BERT and similar transformer models where
     * the [CLS] token serves as a sentence-level representation.
     */
    CLS,

    /**
     * Use the mean (average) of all token embeddings as the final representation.
     * <p/>
     * This mode computes the element-wise average of all token embeddings
     * in the sequence, excluding special tokens like [PAD]. Mean pooling
     * captures information from all tokens equally and often produces
     * robust sentence embeddings.
     * <p/>
     * This approach is particularly effective for sentence similarity tasks
     * and is commonly used with models like Sentence-BERT.
     */
    MEAN
    }
