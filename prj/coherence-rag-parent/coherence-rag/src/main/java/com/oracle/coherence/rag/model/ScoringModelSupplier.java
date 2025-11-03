/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model;

import com.oracle.coherence.rag.ModelProvider;

import com.oracle.coherence.rag.util.CdiHelper;
import dev.langchain4j.model.scoring.ScoringModel;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * CDI supplier for scoring models in the Coherence RAG framework.
 * <p/>
 * This supplier extends {@link AbstractModelSupplier} to provide scoring model
 * instances for document relevance scoring and reranking. Scoring models are
 * specialized cross-encoder models that can determine the relevance between
 * a query and document pairs, enabling more sophisticated result ranking.
 * <p/>
 * The supplier supports both remote AI service providers and local ONNX-based
 * models, with intelligent fallback strategies:
 * <ul>
 *   <li>Local ONNX models: Using the "-" provider prefix (e.g., "-/ms-marco-TinyBERT-L-2-v2")</li>
 *   <li>Remote providers: Through {@link ModelProvider} implementations</li>
 *   <li>Fallback to local ONNX: If a provider is not found, attempts local ONNX model</li>
 * </ul>
 * <p/>
 * Scoring models are particularly useful for:
 * <ul>
 *   <li>Reranking search results based on query-document relevance</li>
 *   <li>Improving retrieval quality in RAG applications</li>
 *   <li>Fine-grained relevance assessment for search results</li>
 * </ul>
 * <p/>
 * Configuration:
 * <ul>
 *   <li>Default model: {@value #DEFAULT_SCORING_MODEL}</li>
 *   <li>Configuration property: {@code model.scoring}</li>
 *   <li>Supports runtime configuration changes</li>
 * </ul>
 * <p/>
 * Usage examples:
 * <pre>
 * // Get default scoring model (local ONNX)
 * ScoringModel model = scoringModelSupplier.get();
 * 
 * // Get specific local ONNX model
 * ScoringModel localModel = scoringModelSupplier.get("-/cross-encoder/ms-marco-MiniLM-L-6-v2");
 * </pre>
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ApplicationScoped
public class ScoringModelSupplier
        extends AbstractModelSupplier<ScoringModel>
    {
    /**
     * The default scoring model used when no specific model is configured.
     * <p/>
     * This uses a local ONNX TinyBERT model optimized for MS MARCO dataset,
     * providing good quality relevance scoring with fast inference times.
     */
    public static final String DEFAULT_SCORING_MODEL   = "-/ms-marco-TinyBERT-L-2-v2";

    /**
     * Returns a human-readable description of the model type.
     * <p/>
     * This description is used in logging and error messages to identify
     * the type of models this supplier manages.
     * 
     * @return "scoring" identifying this as a scoring model supplier
     */
    protected String description()
        {
        return "scoring";
        }

    /**
     * Returns the default model name when no configuration is provided.
     * <p/>
     * This method provides the fallback scoring model that will be used
     * when no explicit configuration is available.
     * 
     * @return the default scoring model name
     */
    protected String defaultModel()
        {
        return DEFAULT_SCORING_MODEL;
        }

    /**
     * Returns the configuration property key for scoring models.
     * <p/>
     * This property can be used to configure the default scoring model
     * at runtime through various configuration sources.
     * 
     * @return "model.scoring" as the configuration property key
     */
    protected String configProperty()
        {
        return "model.scoring";
        }

    /**
     * Creates a new scoring model instance for the specified model name.
     * <p/>
     * This method implements intelligent model resolution with the following strategy:
     * <ol>
     *   <li>If provider is "-", create a local ONNX model using the default factory</li>
     *   <li>Otherwise, look up the named {@link ModelProvider} for the provider</li>
     *   <li>If provider is found, delegate creation to the provider</li>
     *   <li>If provider is not found, fall back to local ONNX model creation</li>
     * </ol>
     * <p/>
     * This fallback strategy ensures that scoring models are always available,
     * even when specific providers are not configured or accessible.
     * 
     * @param modelName the name of the scoring model to create
     * 
     * @return a new ScoringModel instance
     */
    public ScoringModel create(ModelName modelName)
        {
        if ("-".equals(modelName.provider()))
            {
            return LocalOnnxScoringModel.createDefault(modelName);
            }

        ModelProvider provider = CdiHelper.getNamedBean(ModelProvider.class, modelName.provider());
        return provider == null
               ? LocalOnnxScoringModel.create(modelName)
               : provider.getScoringModel(modelName.name());
        }
    }
