/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model;

import jakarta.json.bind.annotation.JsonbProperty;

/**
 * Configuration record for pooling settings in transformer-based embedding models.
 * <p/>
 * This record encapsulates the pooling configuration parameters that determine
 * how token embeddings are aggregated into a single sentence or document embedding.
 * It supports the common pooling strategies used in BERT-style transformer models.
 * <p/>
 * The configuration is designed to be serialized from/to JSON configuration files
 * that typically accompany pre-trained embedding models, using JSON-B annotations
 * to map to the standard configuration format used by sentence-transformers and
 * similar libraries.
 * <p/>
 * Supported pooling modes:
 * <ul>
 *   <li>CLS token pooling: Uses the [CLS] token embedding as the sentence representation</li>
 *   <li>Mean token pooling: Averages all token embeddings to create the sentence representation</li>
 * </ul>
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 * 
 * @param dimension the dimensionality of the word embeddings
 * @param clsToken whether CLS token pooling is enabled
 * @param mean whether mean token pooling is enabled
 */
public record PoolingConfig(

	/**
	 * The dimensionality of the word embeddings produced by the model.
	 * <p/>
	 * This parameter specifies the size of the embedding vectors before
	 * pooling is applied. It corresponds to the hidden size of the
	 * transformer model.
	 */
	@JsonbProperty("word_embedding_dimension")
	int dimension,

	/**
	 * Whether CLS token pooling mode is enabled.
	 * <p/>
	 * When true, the model uses the [CLS] token embedding as the final
	 * sentence representation. This is common in BERT-style models where
	 * the [CLS] token is specifically trained for sentence-level tasks.
	 */
	@JsonbProperty("pooling_mode_cls_token")
	boolean clsToken,

	/**
	 * Whether mean token pooling mode is enabled.
	 * <p/>
	 * When true, the model averages all token embeddings (excluding special
	 * tokens) to create the final sentence representation. This approach
	 * often works well for sentence similarity tasks.
	 */
	@JsonbProperty("pooling_mode_mean_tokens")
	boolean mean
)
	{
	/**
	 * Determines the pooling mode based on the configuration flags.
	 * <p/>
	 * This method validates that exactly one pooling mode is enabled and
	 * returns the corresponding {@link PoolingMode} enum value. The configuration
	 * must specify either CLS token pooling or mean pooling, but not both.
	 * 
	 * @return the pooling mode to use for sentence embedding
	 * 
	 * @throws IllegalStateException if both or neither pooling modes are enabled
	 */
	public PoolingMode mode()
		{
		if (clsToken == mean)
			{
			throw new IllegalStateException("Pooling mode must be either Mean or CLS");
			}

		return mean ? PoolingMode.MEAN : PoolingMode.CLS;
		}
	}
