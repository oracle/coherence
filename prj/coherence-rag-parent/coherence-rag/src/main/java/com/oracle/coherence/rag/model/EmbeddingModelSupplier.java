/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model;

import com.oracle.coherence.rag.ModelProvider;

import com.oracle.coherence.rag.util.CdiHelper;
import dev.langchain4j.model.embedding.EmbeddingModel;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * CDI supplier for embedding models in the Coherence RAG framework.
 * <p/>
 * This supplier extends {@link AbstractModelSupplier} to provide embedding model
 * instances for converting text into vector representations. It supports both
 * remote AI service providers and local ONNX-based models, with intelligent
 * fallback strategies for maximum flexibility.
 * <p/>
 * The supplier provides the following model resolution strategies:
 * <ul>
 *   <li>Local ONNX models: Using the "-" provider prefix (e.g., "-/all-MiniLM-L6-v2")</li>
 *   <li>Remote providers: Through {@link ModelProvider} implementations (OpenAI, OCI, etc.)</li>
 *   <li>Fallback to local ONNX: If a provider is not found, attempts local ONNX model</li>
 * </ul>
 * <p/>
 * This design enables seamless switching between cloud-based and edge-based
 * embedding generation, supporting both connected and offline deployment scenarios.
 * <p/>
 * Configuration:
 * <ul>
 *   <li>Default model: {@value #DEFAULT_EMBEDDING_MODEL}</li>
 *   <li>Configuration property: {@code model.embedding}</li>
 *   <li>Supports runtime configuration changes</li>
 * </ul>
 * <p/>
 * Usage examples:
 * <pre>
 * // Get default embedding model (local ONNX)
 * EmbeddingModel model = embeddingModelSupplier.get();
 * 
 * // Get specific remote provider model
 * EmbeddingModel openAiModel = embeddingModelSupplier.get("openai/text-embedding-ada-002");
 * 
 * // Get specific local ONNX model
 * EmbeddingModel localModel = embeddingModelSupplier.get("-/sentence-transformers/all-MiniLM-L6-v2");
 * </pre>
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ApplicationScoped
public class EmbeddingModelSupplier
        extends AbstractModelSupplier<EmbeddingModel>
    {
    /**
     * The default embedding model used when no specific model is configured.
     * <p/>
     * This uses a local ONNX model that provides good quality embeddings
     * without requiring external API calls, making it suitable for
     * development and edge deployment scenarios.
     */
    public static final String DEFAULT_EMBEDDING_MODEL = "-/all-MiniLM-L6-v2";

    /**
     * Returns a human-readable description of the model type.
     * <p/>
     * This description is used in logging and error messages to identify
     * the type of models this supplier manages.
     * 
     * @return "embedding" identifying this as an embedding model supplier
     */
    protected String description()
        {
        return "embedding";
        }

    /**
     * Returns the default model name when no configuration is provided.
     * <p/>
     * This method provides the fallback embedding model that will be used
     * when no explicit configuration is available.
     * 
     * @return the default embedding model name
     */
    protected String defaultModel()
        {
        return DEFAULT_EMBEDDING_MODEL;
        }

    /**
     * Returns the configuration property key for embedding models.
     * <p/>
     * This property can be used to configure the default embedding model
     * at runtime through various configuration sources.
     * 
     * @return "model.embedding" as the configuration property key
     */
    protected String configProperty()
        {
        return "model.embedding";
        }

    /**
     * Creates a new embedding model instance for the specified model name.
     * <p/>
     * This method implements intelligent model resolution with the following strategy:
     * <ol>
     *   <li>If provider is "-", create a local ONNX model using the default factory</li>
     *   <li>Otherwise, look up the named {@link ModelProvider} for the provider</li>
     *   <li>If provider is found, delegate creation to the provider</li>
     *   <li>If provider is not found, fall back to local ONNX model creation</li>
     * </ol>
     * <p/>
     * This fallback strategy ensures that embedding models are always available,
     * even when specific providers are not configured or accessible.
     * 
     * @param modelName the name of the embedding model to create
     * 
     * @return a new EmbeddingModel instance
     */
    public EmbeddingModel create(ModelName modelName)
        {
        if ("-".equals(modelName.provider()))
            {
            return LocalOnnxEmbeddingModel.createDefault(modelName);
            }

        ModelProvider provider = CdiHelper.getNamedBean(ModelProvider.class, modelName.provider());
        return provider == null
               ? LocalOnnxEmbeddingModel.create(modelName)
               : provider.getEmbeddingModel(modelName.name());
        }
    }
