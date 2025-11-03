/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.oci;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.generativeaiinference.GenerativeAiInferenceClient;
import com.oracle.bmc.generativeaiinference.model.DedicatedServingMode;
import com.oracle.bmc.generativeaiinference.model.EmbedTextDetails;
import com.oracle.bmc.generativeaiinference.model.OnDemandServingMode;
import com.oracle.bmc.generativeaiinference.model.ServingMode;
import com.oracle.bmc.generativeaiinference.requests.EmbedTextRequest;
import com.oracle.bmc.generativeaiinference.responses.EmbedTextResponse;

import com.oracle.coherence.common.base.Logger;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.Config;

import static dev.langchain4j.internal.ValidationUtils.ensureNotEmpty;

/**
 * OCI GenAI embedding model implementation.
 * <p/>
 * This class implements the LangChain4J EmbeddingModel interface for Oracle Cloud 
 * Infrastructure's Generative AI service. It provides text embedding capabilities
 * using Cohere embedding models deployed on OCI.
 * <p/>
 * The model supports both on-demand and dedicated serving modes:
 * <ul>
 * <li>On-demand mode - Uses pre-deployed models identified by model ID</li>
 * <li>Dedicated mode - Uses custom model endpoints identified by endpoint OCID</li>
 * </ul>
 * <p/>
 * Features include:
 * <ul>
 * <li>Configurable text truncation strategy (start, end, or none)</li>
 * <li>Batch processing for improved performance</li>
 * <li>Automatic serving mode detection based on model name</li>
 * <li>Comprehensive error handling and logging</li>
 * </ul>
 * <p/>
 * Example usage:
 * <pre>{@code
 * OciGenAiEmbeddingModel model = OciGenAiEmbeddingModel.builder()
 *     .compartmentId("ocid1.compartment.oc1.....")
 *     .modelName("cohere.embed-multilingual-v3.0")
 *     .batchSize(32)
 *     .build();
 * 
 * List<TextSegment> segments = List.of(
 *     TextSegment.from("Hello world"),
 *     TextSegment.from("How are you?")
 * );
 * 
 * Response<List<Embedding>> response = model.embedAll(segments);
 * }</pre>
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
public class OciGenAiEmbeddingModel
        implements EmbeddingModel
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Constructs an OCI GenAI embedding model with the specified configuration.
     *
     * @param client          the OCI GenerativeAI client
     * @param compartmentId   the OCI compartment ID
     * @param servingMode     the serving mode (on-demand or dedicated)
     * @param truncate        the text truncation strategy
     * @param batchSize       the batch size for processing
     * @param fEcho          whether to echo input text in response
     */
    private OciGenAiEmbeddingModel(GenerativeAiInferenceClient client, String compartmentId, ServingMode servingMode, EmbedTextDetails.Truncate truncate, int batchSize, boolean fEcho)
        {
        this.client = client;
        this.compartmentId = compartmentId;
        this.servingMode = servingMode;
        this.truncate = truncate;
        this.batchSize = batchSize;
        this.fEcho = fEcho;
        }

    // ---- EmbeddingModel interface implementation ------------------------

    /**
     * {@inheritDoc}
     * <p/>
     * Generates embeddings for all provided text segments using OCI GenAI service.
     * The method processes all segments in a single batch request and returns
     * corresponding embeddings in the same order.
     *
     * @param segments the text segments to embed
     *
     * @return response containing the generated embeddings
     *
     * @throws IllegalArgumentException if segments list is null or empty
     * @throws IllegalStateException    if the number of returned embeddings doesn't match input segments
     */
    public Response<List<Embedding>> embedAll(List<TextSegment> segments)
        {
        ensureNotEmpty(segments, "segments");

        EmbedTextDetails embedTextDetails = EmbedTextDetails.builder()
                .inputs(segments.stream().map(TextSegment::text).toList())
                .servingMode(servingMode)
                .compartmentId(compartmentId)
                .isEcho(fEcho)
                .truncate(truncate)
                .inputType(EmbedTextDetails.InputType.SearchDocument).build();

        EmbedTextRequest request = EmbedTextRequest.builder().embedTextDetails(embedTextDetails).build();

        long start = System.nanoTime();
        EmbedTextResponse response = client.embedText(request);
        long duration = System.nanoTime() - start;

        List<Embedding> embeddings = response.getEmbedTextResult().getEmbeddings().stream().map(Embedding::from).toList();
        if (segments.size() != embeddings.size())
            {
            throw new IllegalStateException("The number of embeddings (%,d) does not match the number of text segments (%,d)".formatted(embeddings.size(), segments.size()));
            }

        Logger.finer("Embedded %,d input segments in %,d ms".formatted(segments.size(), Duration.ofNanos(duration).toMillis()));

        return Response.from(embeddings);
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Creates batches of input chunks for processing.
     * <p/>
     * This method is currently not used but provides infrastructure for
     * future batch processing capabilities.
     *
     * @param segments the text segments to batch
     *
     * @return list of batched input chunks
     */
    private List<List<Chunk>> createInputBatches(List<TextSegment> segments)
        {
        List<List<Chunk>> batchedInputs = new ArrayList<>(segments.size() / batchSize + 1);
        for (int i = 0; i < segments.size(); i += batchSize)
            {
            batchedInputs.add(segments.subList(i, Math.min(i + batchSize, segments.size())).stream()
                                      .map(TextSegment::text)
                                      .map(Chunk::new)
                                      .toList());
            }
        return batchedInputs;
        }

    // ---- factory methods -------------------------------------------------

    /**
     * Creates a new builder instance for constructing OciGenAiEmbeddingModel.
     *
     * @return new Builder instance
     */
    public static Builder builder(Config config)
        {
        return new Builder(config);
        }

    // ---- inner classes ---------------------------------------------------

    /**
     * Builder class for constructing OciGenAiEmbeddingModel instances.
     * <p/>
     * The builder supports various configuration options including authentication,
     * model selection, and processing parameters. It automatically detects the
     * appropriate serving mode based on the model name format.
     */
    public static class Builder
            extends AbstractOciModelBuilder
        {
        // ---- constructors -------------------------------------------------

        /**
         * Construct Builder instance.
         *
         * @param config  Eclipse MP configuration
         */
        public Builder(Config config)
            {
            super(config);
            }

        // ---- Builder API --------------------------------------------------

        /**
         * Creates a new OciGenAiEmbeddingModel instance with the configured parameters.
         *
         * @return configured OciGenAiEmbeddingModel instance
         */
        public OciGenAiEmbeddingModel build()
            {
            return new OciGenAiEmbeddingModel(
                    GenerativeAiInferenceClient.builder()
                            .endpoint(baseUrl())
                            .build(authenticationDetailsProvider()),
                    compartmentId(),
                    servingMode(),
                    truncate(),
                    batchSize(),
                    echo()
                );
            }

        /**
         * Returns the authentication details provider.
         *
         * @return the authentication provider
         */
        public AbstractAuthenticationDetailsProvider authenticationDetailsProvider()
            {
            return authenticationDetailsProvider == null
                   ? super.authenticationDetailsProvider()
                   : authenticationDetailsProvider;
            }

        /**
         * Sets the authentication details provider.
         *
         * @param authenticationDetailsProvider the authentication provider
         *
         * @return this builder instance
         */
        public Builder authenticationDetailsProvider(AbstractAuthenticationDetailsProvider authenticationDetailsProvider)
            {
            this.authenticationDetailsProvider = authenticationDetailsProvider;
            return this;
            }

        /**
         * Returns the OCI compartment ID.
         *
         * @return the compartment ID
         */
        public String compartmentId()
            {
            return compartmentId;
            }

        /**
         * Sets the OCI compartment ID.
         *
         * @param compartmentId the compartment ID
         *
         * @return this builder instance
         */
        public Builder compartmentId(String compartmentId)
            {
            this.compartmentId = compartmentId;
            return this;
            }

        /**
         * Returns the model name.
         *
         * @return the model name or default if not set
         */
        public String modelName()
            {
            return modelName == null
                    ? DEFAULT_MODEL
                    : modelName;
            }

        /**
         * Sets the model name.
         *
         * @param modelName the model name
         *
         * @return this builder instance
         */
        public Builder modelName(String modelName)
            {
            this.modelName = modelName;
            return this;
            }

        /**
         * Returns the serving mode, automatically determining it from model name if not set.
         *
         * @return the serving mode
         */
        public ServingMode servingMode()
            {
            return servingMode == null
                   ? modelName().startsWith("ocid1.")
                      ? DedicatedServingMode.builder().endpointId(modelName()).build()
                      : OnDemandServingMode.builder().modelId(modelName()).build()
                   : servingMode;
            }

        /**
         * Sets the serving mode.
         *
         * @param servingMode the serving mode
         *
         * @return this builder instance
         */
        public Builder servingMode(ServingMode servingMode)
            {
            this.servingMode = servingMode;
            return this;
            }

        /**
         * Returns the text truncation strategy.
         *
         * @return the truncation strategy
         */
        public EmbedTextDetails.Truncate truncate()
            {
            return truncate;
            }

        /**
         * Sets the text truncation strategy.
         *
         * @param truncate the truncation strategy
         *
         * @return this builder instance
         */
        public Builder truncate(EmbedTextDetails.Truncate truncate)
            {
            this.truncate = truncate;
            return this;
            }

        /**
         * Returns the batch size for processing.
         *
         * @return the batch size
         */
        public int batchSize()
            {
            return batchSize;
            }

        /**
         * Sets the batch size for processing.
         *
         * @param batchSize the batch size
         *
         * @return this builder instance
         */
        public Builder batchSize(int batchSize)
            {
            this.batchSize = batchSize;
            return this;
            }

        /**
         * Returns whether to echo input text in response.
         *
         * @return true if echo is enabled
         */
        public boolean echo()
            {
            return fEcho;
            }

        /**
         * Sets whether to echo input text in response.
         *
         * @param fEcho true to enable echo
         *
         * @return this builder instance
         */
        public Builder echo(boolean fEcho)
            {
            this.fEcho = fEcho;
            return this;
            }

        // ---- constants ---------------------------------------------------

        /**
         * Default embedding model name.
         */
        private final static String DEFAULT_MODEL = "cohere.embed-multilingual-v3.0";

        // ---- data members ------------------------------------------------

        /**
         * Authentication details provider.
         */
        private AbstractAuthenticationDetailsProvider authenticationDetailsProvider;

        /**
         * OCI compartment ID.
         */
        private String compartmentId;

        /**
         * Model name.
         */
        private String modelName;

        /**
         * Serving mode (on-demand or dedicated).
         */
        private ServingMode servingMode;

        /**
         * Text truncation strategy.
         */
        private EmbedTextDetails.Truncate truncate = EmbedTextDetails.Truncate.End;

        /**
         * Batch size for processing.
         */
        private int batchSize = 64;

        /**
         * Whether to echo input text in response.
         */
        private boolean fEcho;
        }

    /**
     * Internal representation of a text chunk with its embedding.
     * <p/>
     * This class is used internally for batch processing and maintaining
     * the association between text and its corresponding embedding.
     */
    private static final class Chunk
        {
        /**
         * Constructs a new chunk with the specified text.
         *
         * @param text the text content
         */
        private Chunk(String text)
            {
            this.text = text;
            }

        /**
         * Returns the text content of this chunk.
         *
         * @return the text content
         */
        public String text()
            {
            return text;
            }

        /**
         * Returns the embedding for this chunk.
         *
         * @return the embedding, may be null if not set
         */
        public Embedding embedding()
            {
            return embedding;
            }

        /**
         * Sets the embedding for this chunk.
         *
         * @param embedding the embedding
         *
         * @return this chunk instance
         */
        public Chunk setEmbedding(Embedding embedding)
            {
            this.embedding = embedding;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            return "Chunk[" +
                   "text=" + text + ", " +
                   "embedding=" + embedding + ']';
            }

        // ---- data members ------------------------------------------------

        /**
         * The text content of this chunk.
         */
        private final String text;

        /**
         * The embedding for this chunk.
         */
        private Embedding embedding;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The OCI GenerativeAI client.
     */
    private final GenerativeAiInferenceClient client;

    /**
     * The OCI compartment ID.
     */
    private final String compartmentId;

    /**
     * The serving mode (on-demand or dedicated).
     */
    private final ServingMode servingMode;

    /**
     * The text truncation strategy.
     */
    private final EmbedTextDetails.Truncate truncate;

    /**
     * The batch size for processing.
     */
    private final int batchSize;

    /**
     * Whether to echo input text in response.
     */
    private final boolean fEcho;
    }
