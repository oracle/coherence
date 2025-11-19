/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import com.oracle.coherence.rag.model.ScoringModelSupplier;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.scoring.ScoringModel;

import io.helidon.microprofile.cors.CrossOrigin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST API controller for document relevance scoring services.
 * <p/>
 * This JAX-RS resource provides HTTP endpoints for evaluating the relevance
 * of text segments (documents/answers) against a given query using specialized
 * scoring models. The scoring functionality is essential for reranking search
 * results and improving retrieval quality in RAG applications.
 * <p/>
 * The API supports both default and custom scoring models, enabling flexible
 * scoring strategies based on different cross-encoder models optimized for
 * various domains and use cases.
 * <p/>
 * Available endpoints:
 * <ul>
 *   <li>POST /api/score - Score multiple text segments against a query</li>
 * </ul>
 * <p/>
 * Usage example:
 * <pre>
 * POST /api/score
 * {
 *   "modelName": "-/ms-marco-TinyBERT-L-2-v2",
 *   "query": "What is machine learning?",
 *   "answers": [
 *     "Machine learning is a subset of artificial intelligence...",
 *     "Cats are domestic animals that like to sleep...",
 *     "ML algorithms learn patterns from data..."
 *   ]
 * }
 * 
 * Response: [0.85, 0.12, 0.78]
 * </pre>
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ApplicationScoped
@Path("/api/score")
public class Scoring
    {
    /**
     * Supplier for scoring model instances.
     * <p/>
     * This supplier provides access to both default and custom scoring models
     * based on configuration, supporting local ONNX models and remote providers.
     */
    @Inject
    private ScoringModelSupplier scoringModelSupplier;

    /**
     * Handles CORS preflight requests for cross-origin API access.
     * <p/>
     * This method enables web browsers to make cross-origin requests to the
     * scoring API from different domains.
     */
    @OPTIONS
    @CrossOrigin()
    public void cors() {}

    /**
     * Scores multiple text segments against a query for relevance assessment.
     * <p/>
     * This endpoint accepts a scoring request containing a query and multiple
     * text segments (answers/documents) and returns relevance scores for each
     * segment. Higher scores indicate greater relevance to the query.
     * <p/>
     * The endpoint supports optional model selection, falling back to the
     * default scoring model when no specific model is specified.
     * 
     * @param request the scoring request containing query, answers, and optional model name
     * 
     * @return Response containing a list of relevance scores (0.0 to 1.0) for each answer
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response score(ScoreRequest request)
        {
        List<Double> scores = score(request.modelName(), request.query(), request.answers());
        return Response.ok(scores).build();
        }

    /**
     * Performs relevance scoring of text segments against a query.
     * <p/>
     * This method implements the core scoring logic using the specified or
     * default scoring model. It converts the text answers to TextSegments
     * and uses the scoring model to evaluate their relevance to the query.
     * 
     * @param modelName the name of the scoring model to use, or null for default
     * @param query the search query to score against
     * @param answers list of text segments to score for relevance
     * 
     * @return list of relevance scores (0.0 to 1.0) corresponding to each answer
     */
    public List<Double> score(String modelName, String query, List<String> answers)
        {
        ScoringModel model = getModel(modelName);
        return model.scoreAll(textSegments(answers), query).content();
        }

    /**
     * Retrieves the appropriate scoring model based on the model name.
     * <p/>
     * This method returns either the default scoring model or a specific
     * model instance based on the provided model name.
     * 
     * @param modelName the name of the model to retrieve, or null for default
     * 
     * @return the ScoringModel instance to use for scoring
     */
    private ScoringModel getModel(String modelName)
        {
        return modelName == null ? scoringModelSupplier.get() : scoringModelSupplier.get(modelName);
        }

    /**
     * Converts string answers to TextSegment objects for model processing.
     * <p/>
     * This utility method transforms the input strings into the TextSegment
     * format required by the LangChain4J scoring model interface.
     * 
     * @param answers list of text strings to convert
     * 
     * @return list of TextSegment objects
     */
    private List<TextSegment> textSegments(List<String> answers)
        {
        return answers.stream().map(TextSegment::from).toList();
        }

    /**
     * Request object for relevance scoring operations.
     * <p/>
     * This record encapsulates the parameters needed for scoring text segments
     * against a query, including optional model selection.
     * 
     * @param modelName optional name of the scoring model to use (null for default)
     * @param query the search query to score against
     * @param answers list of text segments to evaluate for relevance
     */
    public record ScoreRequest(String modelName, String query, List<String> answers) {}
    }
