/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import com.oracle.coherence.ai.DocumentChunk;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.rag.DocumentLoader;
import com.oracle.coherence.rag.config.index.IndexConfig;
import com.oracle.coherence.ai.QueryResult;
import com.oracle.coherence.rag.config.StoreConfig;
import com.oracle.coherence.ai.hnsw.HnswIndex;
import com.oracle.coherence.ai.index.BinaryQuantIndex;
import com.oracle.coherence.rag.ChatAssistant;
import com.oracle.coherence.rag.model.LocalOnnxEmbeddingModel;
import com.oracle.coherence.rag.model.StreamingChatModelSupplier;
import com.oracle.coherence.rag.model.EmbeddingModelSupplier;
import com.oracle.coherence.rag.model.ModelName;
import com.oracle.coherence.rag.util.CdiHelper;
import com.oracle.coherence.rag.util.Timer;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.lucene.LuceneIndex;
import com.oracle.coherence.lucene.LuceneQueryParser;
import com.oracle.coherence.lucene.LuceneSearch;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.net.management.model.AbstractModel;
import com.tangosol.internal.net.management.model.ModelAttribute;
import com.tangosol.internal.net.management.model.SimpleModelAttribute;
import com.tangosol.internal.net.management.model.SimpleModelOperation;
import com.tangosol.internal.net.metrics.Meter;
import com.tangosol.internal.util.VirtualThreads;

import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import com.tangosol.net.ValueTypeAssertion;

import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Publisher.OrderBy;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.Element;

import com.tangosol.util.Filters;
import com.tangosol.util.ValueExtractor;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.coherence.CoherenceEmbeddingStore;

import io.helidon.microprofile.cors.CrossOrigin;
import io.helidon.webclient.api.HttpClientResponse;
import io.helidon.webclient.api.WebClient;

import jakarta.annotation.PreDestroy;
import jakarta.json.bind.annotation.JsonbCreator;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import java.io.IOException;
import java.io.OutputStream;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.management.DynamicMBean;
import org.apache.lucene.search.Query;

/**
 * Main REST API controller for Coherence RAG document store operations.
 * <p/>
 * This JAX-RS resource provides comprehensive REST endpoints for managing document
 * stores in the Coherence RAG framework, including document ingestion, chunking,
 * embedding generation, vector search, and chat functionality. It serves as the
 * primary interface for client applications to interact with the RAG system.
 * <p/>
 * Key features include:
 * <ul>
 *   <li>Document management - import, retrieve, and delete documents</li>
 *   <li>Automatic document chunking and embedding generation</li>
 *   <li>Hybrid search - combining vector similarity and full-text search</li>
 *   <li>Real-time chat interface with RAG-enhanced responses</li>
 *   <li>Configurable indexing strategies (HNSW, binary quantization, Lucene)</li>
 *   <li>Streaming response support for chat interactions</li>
 *   <li>Cross-origin resource sharing (CORS) support</li>
 *   <li>Performance monitoring and statistics</li>
 * </ul>
 * <p/>
 * The store operates asynchronously using dedicated background threads for
 * document processing and batch embedding generation, ensuring responsive
 * API performance even during heavy document ingestion periods.
 * <p/>
 * Document processing pipeline:
 * <ol>
 *   <li>Documents are imported via URI and published to processing topics</li>
 *   <li>DocumentProcessor thread loads and chunks documents</li>
 *   <li>BatchingEmbedder thread generates embeddings in efficient batches</li>
 *   <li>Processed chunks are stored in both Coherence caches and optionally, external vector store</li>
 * </ol>
 * <p/>
 * The API supports configurable search strategies including:
 * <ul>
 *   <li>Pure vector search using embedding similarity</li>
 *   <li>Full-text search using Lucene indexing</li>
 *   <li>Hybrid search with configurable result fusion weights</li>
 *   <li>Optional re-ranking using specialized scoring models</li>
 * </ul>
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@SuppressWarnings({"CdiManagedBeanInconsistencyInspection", "unchecked"})
public class Store
    {
    private static final ValueExtractor<DocumentChunk.Id, String> DOC_ID = ValueExtractor.of(DocumentChunk.Id::docId).fromKey();
    private static final ValueExtractor<DocumentChunk, String>    TEXT   = ValueExtractor.of(DocumentChunk::text);

    private final String name;

    private final NamedMap<String, StoreConfig> storeConfig;
    private final NamedMap<String, Document> docs;
    private final NamedMap<DocumentChunk.Id, DocumentChunk> chunks;

    private final NamedTopic<String> docsTopic;
    private Publisher<String> docsPublisher;
    private final Subscriber<String> docsSubscriber;

    private final NamedTopic<DocumentChunk> chunksTopic;
    private Publisher<DocumentChunk> chunksPublisher;
    private final Subscriber<DocumentChunk> chunksSubscriber;

    private final EmbeddingModelSupplier embeddingModelSupplier;
    private final StreamingChatModelSupplier chatModelSupplier;

    private final LuceneQueryParser queryParser = LuceneQueryParser.create(TEXT);

    private final Thread documentProcessor;
    private final Thread batchingEmbedder;

    private final Stats stats;

    /**
     * Cleanup method called when the store is destroyed.
     * <p/>
     * This method gracefully shuts down the background processing threads
     * for document processing and embedding generation. It ensures that
     * ongoing operations are interrupted and resources are properly released.
     */
    @PreDestroy
    void close()
        {
        try
            {
            documentProcessor.interrupt();
            batchingEmbedder.interrupt();
            }
        catch (Throwable t)
            {
            Logger.err(t);
            }
        }

    /**
     * Constructs a new Store instance with the specified configuration.
     * <p/>
     * This constructor initializes all the necessary components for the store,
     * including distributed maps, topics, publishers, subscribers, and background
     * processing threads. The store is ready to handle document operations
     * immediately after construction.
     *
     * @param session the Coherence session for accessing distributed resources
     * @param name the unique name of this store
     * @param embeddingModelSupplier supplier for embedding model instances
     * @param chatModelSupplier supplier for chat model instances
     */
    @SuppressWarnings("unchecked")
    public Store(Session session, String name, EmbeddingModelSupplier embeddingModelSupplier, StreamingChatModelSupplier chatModelSupplier)
        {
        Logger.info("Initializing store " + name);

        this.name = name;
        this.storeConfig = session.getMap("storeConfig");

        this.docsTopic      = session.getTopic("docs-" + name);
        this.docsPublisher  = ensureDocsPublisher();
        this.docsSubscriber = docsTopic.createSubscriber(Subscriber.inGroup("docs-" + name));

        this.chunksTopic      = session.getTopic("chunks-" + name);
        this.chunksPublisher  = ensureChunksPublisher();
        this.chunksSubscriber = chunksTopic.createSubscriber(Subscriber.inGroup("chunks-" + name));

        this.docs   = session.getMap("documents-" + name);
        this.chunks = session.getMap("chunks-" + name);

        this.embeddingModelSupplier = embeddingModelSupplier;
        this.chatModelSupplier      = chatModelSupplier;

        chunks.addIndex(DOC_ID);
        chunks.addIndex(new LuceneIndex<>(TEXT));

        documentProcessor = Thread.ofPlatform()
                .name("DocumentProcessor")
                .priority(Thread.MIN_PRIORITY)
                .daemon()
                .start(new DocumentProcessor());

        batchingEmbedder = Thread.ofPlatform()
                .name("BatchingEmbedder")
                .priority(Thread.MIN_PRIORITY)
                .daemon()
                .start(new BatchingChunkEmbedder());

        stats = new Stats();
        registerMBean();
        }

    /**
     * Handles CORS preflight requests for the root store endpoint.
     * <p/>
     * This method enables cross-origin resource sharing for web applications
     * that need to access the store API from different domains.
     */
    @OPTIONS
    @CrossOrigin
    public void cors() {}

    /**
     * Handles CORS preflight requests for document-related endpoints.
     * <p/>
     * This method enables cross-origin access to document import and
     * retrieval operations.
     */
    @OPTIONS
    @CrossOrigin
    @Path("docs")
    public void corsDocs() {}

    /**
     * Handles CORS preflight requests for document statistics endpoints.
     * <p/>
     * This method enables cross-origin access to document statistics
     * and monitoring information.
     */
    @OPTIONS
    @CrossOrigin
    @Path("docs/stats")
    public void corsDocStats() {}

    /**
     * Handles CORS preflight requests for general statistics endpoints.
     * <p/>
     * This method enables cross-origin access to store-wide statistics
     * and performance metrics.
     */
    @OPTIONS
    @CrossOrigin
    @Path("stats")
    public void corsStats() {}

    /**
     * Handles CORS preflight requests for chunk-related endpoints.
     * <p/>
     * This method enables cross-origin access to document chunk
     * operations and retrieval.
     */
    @OPTIONS
    @CrossOrigin
    @Path("chunks")
    public void corsChunks() {}

    /**
     * Handles CORS preflight requests for chunk statistics endpoints.
     * <p/>
     * This method enables cross-origin access to chunk-level statistics
     * and performance information.
     */
    @OPTIONS
    @CrossOrigin
    @Path("chunks/stats")
    public void corsChunkStats() {}

    /**
     * Handles CORS preflight requests for chat endpoints.
     * <p/>
     * This method enables cross-origin access to the RAG-enhanced
     * chat functionality.
     */
    @OPTIONS
    @CrossOrigin
    @Path("chat")
    public void corsChat() {}

    /**
     * Retrieves the current configuration for this store.
     * <p/>
     * This endpoint returns the store's configuration including embedding model,
     * chat model, normalization settings, and indexing configuration.
     *
     * @return Response containing the store configuration
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("config")
    public Response getConfig()
        {
        return Response.ok(config()).build();
        }

    /**
     * Removes the current vector index from the store.
     * <p/>
     * This operation removes any existing vector index (HNSW, binary quantization, etc.)
     * from the document chunks. The underlying data is preserved but vector search
     * performance will be impacted until a new index is created.
     *
     * @return Response with 202 Accepted if index was removed, 304 Not Modified if no index exists
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("index")
    public Response removeIndex()
        {
        IndexConfig<?> indexConfig = config().getIndex();

        if (indexConfig != null)
            {
            ValueExtractor<?, ?> index = createExtractor(indexConfig);
            if (index != null)
                {
                Logger.config("Removing %s index for store %s".formatted(indexConfig.type(), name));
                chunks.removeIndex(index);
                storeConfig.invoke(name, e ->
                    {
                    StoreConfig config = e.getValue();
                    e.setValue(config.setIndex(null));
                    return null;
                    });
                return Response.accepted().build();
                }

            }

        return Response.notModified().build();
        }

    /**
     * Creates a new vector index for the store.
     * <p/>
     * This operation creates a vector index based on the provided configuration.
     * Any existing index is removed first to ensure clean state. Supported index
     * types include HNSW for approximate nearest neighbor search, binary quantization
     * for memory efficiency, and simple vector storage.
     *
     * @param indexConfig configuration specifying the index type and parameters
     * 
     * @return Response with 202 Accepted if index was created successfully
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("index")
    public Response createIndex(IndexConfig<?> indexConfig)
        {
        try (Response r = removeIndex())
            {
            ValueExtractor<?, ?> index = createExtractor(indexConfig);
            if (index != null)
                {
                Logger.config("Creating %s index for store %s".formatted(indexConfig.type(), name));
                chunks.addIndex(index);
                storeConfig.invoke(name, e ->
                    {
                    StoreConfig config = e.getValue();
                    e.setValue(config.setIndex(indexConfig));
                    return null;
                    });
                return Response.accepted().build();
                }

            return r;
            }
        }

    /**
     * Creates a ValueExtractor for the specified index configuration.
     * <p/>
     * This helper method creates the appropriate index extractor based on the
     * index type. Different index types have different performance characteristics
     * and memory requirements.
     *
     * @param indexConfig the index configuration specifying type and parameters
     * 
     * @return ValueExtractor for the specified index type, or null if unsupported
     */
    private ValueExtractor<?, ?> createExtractor(IndexConfig<?> indexConfig)
        {
        return switch (indexConfig.type())
            {
            case "HNSW"   -> new HnswIndex<>(DocumentChunk::vector, getEmbeddingModel().dimension());
            case "BINARY" -> new BinaryQuantIndex<>(DocumentChunk::vector);
            case "SIMPLE" -> ValueExtractor.of(DocumentChunk::vector);
            default -> null;
            };
        }

    /**
     * Performs hybrid search combining vector similarity and full-text search.
     * <p/>
     * This endpoint executes a comprehensive search across document chunks using
     * both vector embeddings and full-text indexing. Results can be optionally
     * re-ranked using specialized scoring models for improved relevance.
     * <p/>
     * The search process includes:
     * <ul>
     *   <li>Vector similarity search using embedding models</li>
     *   <li>Full-text search using Lucene indexing (if enabled)</li>
     *   <li>Result fusion with configurable weighting</li>
     *   <li>Optional re-ranking using cross-encoder models</li>
     * </ul>
     *
     * @param req the search request containing query, parameters, and options
     * 
     * @return Response containing search results with execution timing
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("search")
    public Response search(SearchRequest req)
        {
        Timer timer = new Timer().start();
        List<ChunkResult> results = findChunks(req.query(), req.maxResults(), req.minScore(), req.fullTextWeight(), req.scoringModel());
        timer.stop();
        stats.recordSearch(timer.duration());

        // now that we've updated the scores, we can filter, sort and limit results
        results = results.stream()
                .filter(chunk -> chunk.getScore() >= req.minScore())
                .sorted(Comparator.comparingDouble(ChunkResult::getScore).reversed())
                .limit(req.maxResults())
                .toList();

        return Response.ok(new SearchResult(results, timer.duration().toMillis())).build();
        }

    /**
     * Core search logic that finds relevant document chunks.
     * <p/>
     * This method orchestrates the hybrid search process, combining vector
     * similarity search with optional full-text search. It handles result
     * fusion, optional re-ranking, and parallel execution for optimal performance.
     *
     * @param query the search query text
     * @param maxResults maximum number of results to return
     * @param minScore minimum relevance score threshold
     * @param fullTextWeight weight for full-text search (0.0 = vector only, 1.0 = text only)
     * @param scoringModelName optional model name for result re-ranking
     * 
     * @return list of matching document chunks with relevance scores
     * 
     * @throws IllegalArgumentException if fullTextWeight is not in [0.0, 1.0]
     */
    List<ChunkResult> findChunks(String query, int maxResults, double minScore, double fullTextWeight, String scoringModelName)
        {
        if (fullTextWeight < 0.0 || fullTextWeight > 1.0) {
            throw new IllegalArgumentException("fullTextWeight must be in [0.0, 1.0]");
        }

        Embedding embedding = getEmbeddingModel().embed(query).content();

        boolean fScoring    = scoringModelName != null;
        int     cMaxResults = fScoring ? maxResults * 2 : maxResults;
        double  nMinScore   = fScoring ? 0 : minScore;

        CompletableFuture<List<ChunkResult>> fullTextResults = CompletableFuture.completedFuture(null);
        if (fullTextWeight > 0.0d)
            {
            fullTextResults = CompletableFuture.supplyAsync(() -> fullTextSearch(query, cMaxResults));
            }

        List<ChunkResult> vectorResults = vectorSearch(embedding, cMaxResults, nMinScore);
        List<ChunkResult> results       = fuseResults(vectorResults, fullTextResults.join(), fullTextWeight);

        if (fScoring)
            {
            scoreResults(query, results, scoringModelName);
            }

        return results;
        }

    /**
     * Performs full-text search using Lucene indexing.
     * <p/>
     * This method executes a Lucene-based full-text search across document chunks.
     * It uses the configured query parser to handle complex query syntax and
     * returns results based on text relevance.
     *
     * @param query the search query text
     * @param cMaxResults maximum number of results to return
     * 
     * @return list of matching chunks with Lucene relevance scores
     */
    private List<ChunkResult> fullTextSearch(String query, int cMaxResults)
        {
        Query luceneQuery = queryParser.parse(query);
        return chunks.aggregate(new LuceneSearch<>(TEXT, luceneQuery, cMaxResults))
                .stream()
                .map(ChunkResult::new)
                .toList();
        }

    /**
     * Performs vector similarity search using embedding models.
     * <p/>
     * This method executes a vector similarity search against document embeddings
     * using the configured embedding store. Results are ranked by cosine similarity
     * or other distance metrics.
     *
     * @param embedding the query embedding vector
     * @param cMaxResults maximum number of results to return
     * @param nMinScore minimum similarity score threshold
     * 
     * @return list of matching chunks with similarity scores
     */
    private List<ChunkResult> vectorSearch(Embedding embedding, int cMaxResults, double nMinScore)
        {
        return createEmbeddingStore().search(new EmbeddingSearchRequest(embedding, cMaxResults, nMinScore, null))
                    .matches()
                    .stream()
                    .map(ChunkResult::new)
                    .toList();
        }

    /**
     * Re-ranks search results using a specialized scoring model.
     * <p/>
     * This method uses a cross-encoder model to re-score search results based on
     * query-document relevance. The scoring model provides more accurate relevance
     * assessment than simple embedding similarity.
     *
     * @param query the original search query
     * @param results the list of search results to re-score
     * @param scoringModelName the name of the scoring model to use
     */
    private static void scoreResults(String query, List<ChunkResult> results, String scoringModelName)
        {
        Scoring scoring = CdiHelper.getBean(Scoring.class);
        assert scoring != null;
        
        List<Double> scores = scoring.score(scoringModelName, query, results.stream().map(ChunkResult::getText).toList());
        for (int i = 0; i < scores.size(); i++)
            {
            results.get(i).setScore(scores.get(i));
            }
        }

    /**
     * Fuses vector and full-text search results using weighted scoring.
     * <p/>
     * This method combines results from vector similarity search and full-text
     * search using a weighted fusion approach. The fullTextWeight parameter
     * controls the balance between the two search methods.
     * <p/>
     * The fusion algorithm:
     * <ul>
     *   <li>Normalizes scores from both search methods</li>
     *   <li>Applies weighted combination: (1-weight) * vector + weight * text</li>
     *   <li>Merges results and removes duplicates</li>
     *   <li>Re-ranks by combined score</li>
     * </ul>
     *
     * @param vectorResults   results from vector similarity search
     * @param fullTextResults results from full-text search (can be null)
     * @param fullTextWeight  weight for full-text scores (0.0 = vector only, 1.0 = text only)
     * 
     * @return fused list of results with combined scores
     */
    private List<ChunkResult> fuseResults(
            List<ChunkResult> vectorResults,
            List<ChunkResult> fullTextResults,
            double fullTextWeight)
        {
        if (fullTextResults == null || fullTextResults.isEmpty())
            {
            return vectorResults;
            }

        double vectorWeight = 1.0 - fullTextWeight;

        Map<String, ChunkResult> fused = vectorResults.stream()
                .collect(Collectors.toMap(ChunkResult::getChunkId, Function.identity()));

        for (ChunkResult chunk : fullTextResults)
            {
            fused.merge(chunk.getChunkId(), chunk, (chunkOld, chunkNew) ->
                {
                double score = chunkOld.getScore() * vectorWeight
                               + chunkNew.getScore() * fullTextWeight;
                return chunkOld.setIndex("HYBRID").setScore(score);
                });
            }

        return fused.values().stream().toList();
        }


    /**
     * Clears all document chunks from the store.
     * <p/>
     * This operation removes all document chunks and their embeddings from the
     * store. The operation is irreversible and all documents will need to be
     * re-imported. Vector indexes and configurations are preserved.
     *
     * @return Response with 204 No Content indicating successful clearing
     */
    @DELETE
    public Response clearStore()
        {
        chunks.truncate();
        return Response.noContent().build();
        }

    /**
     * Resets performance and usage statistics for the store.
     * <p/>
     * This operation clears all accumulated statistics and performance metrics
     * for the store, providing a clean slate for monitoring.
     *
     * @return Response with 204 No Content indicating successful reset
     */
    @DELETE
    @Path("stats")
    public Response resetStats()
        {
        return Response.noContent().build();
        }

    /**
     * Retrieves documents from the store.
     * <p/>
     * This endpoint can list all document IDs in the store or retrieve a specific
     * document by its ID. When no docId parameter is provided, it returns an array
     * of all document IDs. When a docId is provided, it returns the full document
     * content.
     *
     * @param docId optional document ID to retrieve specific document
     * 
     * @return Response containing document list or specific document content,
     *         404 Not Found if specific document doesn't exist
     */
    @GET
    @Path("docs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocuments(@QueryParam("docId") String docId)
        {
        if (docId == null)
            {
            return Response.ok(docs.keySet()).build();
            }

        Document d = docs.get(docId);
        if (d == null)
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        return Response.ok(new Doc(docId, d.text())).build();
        }

    /**
     * Imports documents from the specified URIs for processing.
     * <p/>
     * This endpoint queues documents for asynchronous processing. Documents are
     * loaded from the provided URIs, chunked, embedded, and stored automatically
     * by background processing threads.
     * <p/>
     * Supported URI schemes:
     * <ul>
     *   <li>http/https - Web documents</li>
     *   <li>file - Local file system</li>
     *   <li>s3 - AWS S3 storage</li>
     *   <li>azure.blob - Azure Blob Storage</li>
     *   <li>gcs - Google Cloud Storage</li>
     *   <li>oci.os - OCI Object Storage</li>
     * </ul>
     *
     * @param uris list of document URIs to import
     * 
     * @return Response with 204 No Content indicating documents were queued
     */
    @POST
    @Path("docs")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response importDocuments(List<String> uris)
        {
        //noinspection resource
        Publisher<String> publisher = ensureDocsPublisher();
        uris.forEach(publisher::publish);
        publisher.flush().join();

        return Response.noContent().build();
        }

    /**
     * Retrieves document chunks for a specific document.
     * <p/>
     * This endpoint returns all chunks for the specified document, including
     * their text content, metadata, and vector embeddings. The chunks are
     * returned in their original order from the document.
     *
     * @param docId the document ID to retrieve chunks for
     * 
     * @return Response containing document chunks with embeddings,
     *         404 Not Found if document doesn't exist
     */
    @GET
    @Path("chunks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocumentChunks(@QueryParam("docId") String docId)
        {
        if (docId == null)
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        Map<DocumentChunk.Id, DocumentChunk> mapChunks = getChunks(docId);
        if (mapChunks.isEmpty())
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        DocChunk[] aChunks = new DocChunk[mapChunks.size()];
        for (int i = 0; i < aChunks.length; i++)
            {
            aChunks[i] = new DocChunk(mapChunks.get(DocumentChunk.id(docId, i)));
            }

        return Response.ok(new DocChunks(docId, aChunks)).build();
        }

    /**
     * Adds pre-processed document chunks to the store.
     * <p/>
     * This endpoint allows direct addition of document chunks with their
     * embeddings, bypassing the normal document loading and processing pipeline.
     * This is useful for batch operations or when chunks are processed externally.
     *
     * @param aDocs array of documents containing chunks to add
     * 
     * @return Response with 204 No Content indicating chunks were added
     */
    @POST
    @Path("chunks")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addDocumentChunks(DocChunks[] aDocs)
        {
        for (DocChunks document : aDocs)
            {
            String     docId   = document.id();
            DocChunk[] aChunks = document.chunks();

            for (int i = 0; i < aChunks.length; i++)
                {
                DocChunk chunk = aChunks[i];
                DocumentChunk documentChunk = new DocumentChunk(chunk.text(), chunk.metadata());
                documentChunk.metadata().put("url", docId);
                documentChunk.metadata().put("index", i);

                chunksPublisher.publish(documentChunk);
                }
            }
        chunksPublisher.flush().join();

        return Response.noContent().build();
        }

    /**
     * Retrieves statistics about document chunks in the store.
     * <p/>
     * This endpoint returns detailed statistics about chunk distribution and
     * memory usage across cluster members. The statistics are retrieved from
     * the Coherence management interface.
     *
     * @return Response containing chunk statistics from cluster members
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("chunks/stats")
    public Response getDocumentChunkStats()
        {
        WebClient client = WebClient.create();
        try (HttpClientResponse res = client.get("http://localhost:30000/management/coherence/cluster/caches/chunks-%s/members".formatted(name))
                .queryParam("links", "\\")
                .queryParam("fields", "nodeId,unitsBytes")
                .request())
            {
            return Response.ok(res.as(String.class)).build();
            }
        }

    /**
     * Provides RAG-enhanced conversational AI functionality.
     * <p/>
     * This endpoint creates a chat assistant that combines context retrieval
     * with conversational AI. The assistant retrieves relevant document chunks
     * based on the question and uses them as context for generating responses.
     * The response is streamed in real-time as tokens are generated.
     *
     * @param request the chat request containing question and parameters
     * 
     * @return Response containing streamed AI-generated answer with retrieved context
     */
    @POST
    @Path("chat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response chat(ChatRequest request)
        {
        StreamingChatModel chatModel = request.chatModel() == null
                                  ? chatModelSupplier.get()
                                  : chatModelSupplier.get(new ModelName(request.chatModel()));
        ChatAssistant assistant = createChatAssistant(chatModel, request.maxResults(), request.minScore());
        TokenStream   answer    = assistant.answer(request.question());

        return Response.ok(new StreamingAnswer(request.question(), answer)).build();
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Retrieves the current configuration for this store.
     * <p/>
     * This helper method provides access to the store's configuration from
     * the distributed configuration cache.
     *
     * @return the store configuration
     */
    private StoreConfig config()
        {
        return storeConfig.get(name);
        }

    /**
     * Gets the embedding model instance for this store.
     * <p/>
     * This method creates an embedding model based on the store's configuration.
     * The model is used for generating vector embeddings from text.
     *
     * @return the configured embedding model
     */
    private EmbeddingModel getEmbeddingModel()
        {
        return embeddingModelSupplier.get(config().getEmbeddingModel());
        }

    /**
     * Creates an embedding store instance for vector operations.
     * <p/>
     * This method creates a Coherence-based embedding store that provides
     * vector similarity search capabilities. The store is configured based
     * on the current store settings.
     *
     * @return configured embedding store for vector operations
     */
    private CoherenceEmbeddingStore createEmbeddingStore()
        {
        return CoherenceEmbeddingStore.builder()
                .name(chunks.getName())
                .normalizeEmbeddings(config().isNormalizeEmbeddings())
                .build();
        }

    /**
     * Retrieves all chunks for a specific document.
     * <p/>
     * This helper method fetches all document chunks belonging to the specified
     * document ID from the chunks cache.
     *
     * @param docId the document ID to retrieve chunks for
     * 
     * @return map of chunk IDs to document chunks
     */
    private Map<DocumentChunk.Id, DocumentChunk> getChunks(String docId)
        {
        return chunks.entrySet(Filters.equal(DOC_ID, docId)).stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

    /**
     * Ensures a chunks publisher is available and active.
     * <p/>
     * This method creates a new chunks publisher if none exists or if the
     * current publisher is inactive. The publisher is used to distribute
     * document chunks for processing.
     *
     * @return active chunks publisher
     */
    private Publisher<DocumentChunk> ensureChunksPublisher()
        {
        Publisher<DocumentChunk> publisher = chunksPublisher;
        if (publisher == null || !publisher.isActive())
            {
            chunksPublisher = publisher = chunksTopic.createPublisher(OrderBy.none());
            }
        return publisher;
        }

    /**
     * Ensures a documents publisher is available and active.
     * <p/>
     * This method creates a new documents publisher if none exists or if the
     * current publisher is inactive. The publisher is used to distribute
     * document URIs for processing.
     *
     * @return active documents publisher
     */
    private Publisher<String> ensureDocsPublisher()
        {
        Publisher<String> publisher = docsPublisher;
        if (publisher == null || !publisher.isActive())
            {
            docsPublisher = publisher = docsTopic.createPublisher(OrderBy.none());
            }
        return publisher;
        }

    /**
     * Creates a chat assistant with RAG capabilities.
     * <p/>
     * This method creates a chat assistant that combines a streaming chat model
     * with a content retriever for RAG functionality. The assistant can retrieve
     * relevant context from the document store and use it to generate responses.
     *
     * @param chatModel the streaming chat model to use
     * @param maxResults maximum number of context documents to retrieve
     * @param minScore minimum relevance score for context documents
     * 
     * @return configured chat assistant with RAG capabilities
     */
    private ChatAssistant createChatAssistant(StreamingChatModel chatModel, int maxResults, double minScore)
        {
        ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(createEmbeddingStore())
                .embeddingModel(getEmbeddingModel())
                .maxResults(maxResults)
                .minScore(minScore)
                .build();

        return AiServices.builder(ChatAssistant.class)
                .streamingChatModel(chatModel)
                .contentRetriever(retriever)
                .build();
        }

    /**
     * Utility method to get a typed NamedMap from the default Coherence session.
     * <p/>
     * This helper method provides type-safe access to Coherence maps with
     * proper generic typing.
     *
     * @param name the name of the map
     * @param keyClass the class of the map keys
     * @param valueClass the class of the map values
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * 
     * @return typed NamedMap instance
     */
    @SuppressWarnings({"unused", "SameParameterValue"})
    private static <K, V> NamedMap<K, V> getNamedMap(String name, Class<K> keyClass, Class<V> valueClass)
        {
        return Coherence.getInstance().getSession().getMap(name);
        }

    /**
     * Utility method to get a typed NamedTopic from the default Coherence session.
     * <p/>
     * This helper method provides type-safe access to Coherence topics with
     * proper generic typing and type assertions.
     *
     * @param name the name of the topic
     * @param elementClass the class of the topic elements
     * @param <V> the type of topic elements
     * 
     * @return typed NamedTopic instance
     */
    @SuppressWarnings({"unused", "SameParameterValue"})
    private static <V> NamedTopic<V> getTopic(String name, Class<V> elementClass)
        {
        return Coherence.getInstance().getSession().getTopic(name, ValueTypeAssertion.withType(elementClass));
        }

    /**
     * Register the MBean for this repository with Coherence.
     */
    private void registerMBean()
        {
        try
            {
            Cluster cluster  = docs.getService().getCluster();
            Registry registry = cluster.getManagement();

            if (registry != null)
                {
                String sName = registry.ensureGlobalName("type=RAG," + Registry.KEY_NAME + name);
                registry.register(sName, stats);
                Logger.info("Registered RAG MBean " + sName + " for knowledge store " + name);
                }
            }
        catch (Exception e)
            {
            Logger.err("Failed to register RAG MBean for knowledge store " + name, e);
            }
        }


    // ---- inner classes ---------------------------------------------------

    /**
     * Background thread for processing document URIs into chunks.
     * <p/>
     * This class implements a continuous processing loop that consumes document
     * URIs from the documents topic, loads the documents using appropriate loaders,
     * chunks them into smaller segments, and publishes the chunks for embedding
     * generation.
     * <p/>
     * The processor supports various document sources including web URLs, local
     * files, and cloud storage services. It handles errors gracefully and
     * continues processing even if individual documents fail to load.
     */
    class DocumentProcessor implements Runnable
        {
        /**
         * Main processing loop for document loading and chunking.
         * <p/>
         * This method runs continuously, consuming document URIs from the topic
         * and processing them into chunks. The loop handles interruption gracefully
         * and logs any processing errors.
         */
        public void run()
            {
            Logger.info("Started main DocumentProcessor thread");

            try (var executor = VirtualThreads.newVirtualThreadPerTaskExecutor())
                {
                while (docsSubscriber.isActive())
                    {
                    try
                        {
                        Element<String> e = docsSubscriber.receive().join();
                        String docId = e.getValue();

                        executor.execute(() ->
                             {
                             Timer loadTimer = new Timer();
                             Timer splitTimer = new Timer();

                             Document doc = docs.get(docId);
                             if (doc == null)
                                 {
                                 loadTimer.start();

                                 doc = loadDocument(docId);
                                 long time = loadTimer.stop().duration().toMillis();

                                 if (doc != null)
                                     {
                                     docs.put(docId, doc);
                                     Logger.fine("Loaded %s in %,d ms".formatted(docId, time));
                                     }
                                 }

                             if (doc != null)
                                 {
                                 splitTimer.start();
                                 DocumentSplitter splitter = DocumentSplitters.recursive(config().getChunkSize(), config().getChunkOverlap());
                                 List<TextSegment> segments = splitter.split(doc);

                                 long splitTime = splitTimer.stop().duration().toMillis();
                                 int chunkCount = segments.size();
                                 Logger.fine("Split %s into %,d segments in %,d ms".formatted(docId, chunkCount, splitTime));

                                 //noinspection resource
                                 Publisher<DocumentChunk> publisher = ensureChunksPublisher();
                                 for (TextSegment segment : segments)
                                     {
                                     DocumentChunk chunk = new DocumentChunk(segment.text(), segment.metadata().toMap());
                                     publisher.publish(chunk);
                                     }
                                 publisher.flush().join();

                                 stats.finishDocument(loadTimer.duration(), splitTimer.duration());
                                 }
                             else
                                 {
                                 stats.failDocument(loadTimer.duration(), splitTimer.duration());
                                 }

                             e.commit();
                             });
                        }
                    catch (Exception e)
                        {
                        Logger.err(e);
                        if (!docsSubscriber.isActive())
                            {
                            throw e;
                            }
                        }
                    }
                }
            }

        /**
         * Loads a document from the specified URI using appropriate {@link DocumentLoader}.
         * <p/>
         * This method implements the core document loading logic with the following steps:
         * <ol>
         *   <li>Parse the document ID as a URI to determine the scheme</li>
         *   <li>Find the appropriate DocumentLoader implementation for the scheme</li>
         *   <li>Load the document using the selected loader</li>
         * </ol>
         *
         * @param docId the document identifier, must be a valid URI string
         *
         * @return the loaded Document object, or null if loading fails
         */
        public Document loadDocument(String docId)
            {
            URI uri     = URI.create(docId);
            String sScheme = uri.getScheme();

            DocumentLoader loader = findDocumentLoader(sScheme);
            if (loader == null)
                {
                return null;
                }

            try
                {
                Document doc = loader.load(uri);
                if (doc == null)
                    {
                    Logger.info("Unable to load document %s".formatted(docId));
                    }

                return doc;
                }
            catch (Throwable e)
                {
                Throwable cause = Exceptions.getRootCause(e);
                Logger.info("Failed to load document %s: %s".formatted(cause.getClass().getName(), cause.getMessage()));
                }

            return null;
            }

        /**
         * Finds the appropriate DocumentLoader implementation for a given URI scheme.
         * <p/>
         * This method uses CDI BeanManager to dynamically locate DocumentLoader
         * implementations that are registered with a name matching the URI scheme.
         * This enables protocol-specific document loading (file, http, https, etc.).
         *
         * @param scheme the URI scheme (e.g., "file", "http", "https")
         *
         * @return the DocumentLoader for the scheme, or null if none found
         */
        private DocumentLoader findDocumentLoader(String scheme)
            {
            var loader = CdiHelper.getNamedBean(DocumentLoader.class, scheme);
            if (loader == null)
                {
                Logger.warn("Unable to find document loader for URI scheme '%s'".formatted(scheme));
                return null;
                }

            return loader;
            }
        }

    /**
     * Background thread for batch processing document chunks into embeddings.
     * <p/>
     * This class implements a continuous processing loop that consumes document
     * chunks from the chunks topic, generates vector embeddings in batches for
     * efficiency, and stores the embedded chunks in the distributed cache.
     * <p/>
     * The embedder processes chunks in configurable batch sizes to optimize
     * embedding model throughput and reduce API calls. It uses parallel
     * execution for batch processing while maintaining proper ordering
     * and error handling.
     */
    class BatchingChunkEmbedder
            implements Runnable
        {
        /**
         * Main processing loop for batch embedding generation.
         * <p/>
         * This method runs continuously, consuming document chunks from the topic
         * and processing them in batches to generate embeddings. The loop handles
         * interruption gracefully and logs performance metrics.
         */
        public void run()
            {
            int batchSize = Config.getInteger("coherence.rag.embed.batch.size", 64);
            EmbeddingModel embeddingModel = getEmbeddingModel();

            try (var executor = embeddingModel instanceof LocalOnnxEmbeddingModel
                                       ? ForkJoinPool.commonPool()
                                       : VirtualThreads.newVirtualThreadPerTaskExecutor())
                {
                Logger.info("Started BatchingEmbedder with batch size of %d".formatted(batchSize));

                while (chunksSubscriber.isActive())
                    {
                    try
                        {
                        List<Element<DocumentChunk>> chunkList = chunksSubscriber.receive(batchSize).join();
                        if (!chunkList.isEmpty())
                            {
                            ChunkBatch batch = new ChunkBatch(chunkList);

                            executor.execute(() ->
                                 {
                                 Timer timer = new Timer();
                                 int count = batch.chunks().size();

                                 try
                                     {
                                     timer.start();
                                     batch.embedAll(embeddingModel);
                                     long time = timer.stop().duration().toMillis();

                                     Logger.fine("Created %,d embeddings in %,d ms (%,.3f ms/embedding)".formatted(count, time, 1.0f * time / count));

                                     chunks.putAll(batch.chunks());
                                     chunksSubscriber.commit(chunkList.stream().collect(
                                             Collectors.toMap(Element::getChannel, Element::getPosition, (p1, p2) -> p1.compareTo(p2) < 0 ? p2 : p1)));
                                     stats.finishEmbeddings(count, timer.duration());
                                     }
                                 catch (Throwable t)
                                     {
                                     Logger.err("Failed to create %,d embeddings".formatted(count), t);
                                     stats.failEmbeddings(count, timer.duration());
                                     }
                                 });
                            }
                        }
                    catch (Exception e)
                        {
                        if (chunksSubscriber.isActive())
                            {
                            Logger.err(e);
                            }
                        }
                    }
                }
            }
        }

    /**
     * Record representing a batch of document chunks for efficient embedding processing.
     * <p/>
     * This record encapsulates a collection of document chunks that can be processed
     * together for embedding generation. It provides utilities for batch operations
     * and maintains the mapping between chunk IDs and their content.
     *
     * @param chunks map of chunk IDs to document chunks
     */
    record ChunkBatch(Map<DocumentChunk.Id, DocumentChunk> chunks)
        {
        /**
         * Creates a new empty chunk batch with the specified initial capacity.
         *
         * @param cSize initial capacity for the chunk map
         */
        public ChunkBatch(int cSize)
            {
            this(HashMap.newHashMap(cSize));
            }

        /**
         * Creates a new chunk batch from a list of topic elements.
         * <p/>
         * This constructor extracts the chunks from topic elements and adds
         * them to the batch for processing.
         *
         * @param chunks list of topic elements containing document chunks
         */
        public ChunkBatch(List<Element<DocumentChunk>> chunks)
            {
            this(chunks.size());
            chunks.forEach(e -> add(e.getValue()));
            }

        /**
         * Adds a document chunk to the batch.
         * <p/>
         * The chunk is indexed by its generated ID for efficient lookup
         * and processing.
         *
         * @param chunk the document chunk to add
         */
        public void add(DocumentChunk chunk)
            {
            chunks.put(chunkId(chunk), chunk);
            }

        /**
         * Generates embeddings for all chunks in the batch.
         * <p/>
         * This method uses the provided embedding model to generate vector
         * embeddings for all chunks in a single batch operation. The embeddings
         * are then assigned to their corresponding chunks.
         *
         * @param model the embedding model to use for generation
         */
        public void embedAll(EmbeddingModel model)
            {
            List<Embedding> embeddings = model.embedAll(segments()).content();
            Iterator<Embedding> iterator = embeddings.iterator();
            for (DocumentChunk chunk : chunks.values())
                {
                chunk.setVector(iterator.next().vector());
                }
            }

        /**
         * Generates a unique ID for a document chunk.
         * <p/>
         * The ID is constructed from the document URL and chunk index
         * stored in the chunk's metadata.
         *
         * @param chunk the document chunk to generate ID for
         * 
         * @return unique chunk ID
         */
        private DocumentChunk.Id chunkId(DocumentChunk chunk)
            {
            return new DocumentChunk.Id((String) chunk.metadata().get("url"), Integer.parseInt((String) chunk.metadata().get("index")));
            }

        /**
         * Converts all chunks in the batch to text segments.
         * <p/>
         * This method extracts the text content from all chunks and converts
         * them to TextSegment objects suitable for embedding generation.
         *
         * @return list of text segments for embedding
         */
        private List<TextSegment> segments()
            {
            return chunks.values().stream().map(chunk -> TextSegment.from(chunk.text())).toList();
            }
        }

    /**
     * Streaming output implementation for real-time chat responses.
     * <p/>
     * This class handles the streaming of AI-generated chat responses to HTTP clients.
     * It implements JAX-RS StreamingOutput to provide real-time token-by-token
     * streaming, enabling responsive chat interfaces with immediate feedback.
     * <p/>
     * The streaming includes performance metrics such as time to first token
     * and total token count for monitoring and optimization purposes.
     */
    static class StreamingAnswer implements StreamingOutput
        {
        /**
         * The original user question for context.
         */
        private final String question;
        
        /**
         * The token stream from the chat model.
         */
        private final TokenStream answer;

        /**
         * Count of tokens streamed so far.
         */
        private int  cTokens = 0;
        
        /**
         * Time taken to receive the first token.
         */
        private long timeToFirstToken;

        /**
         * Creates a new streaming answer for the given question and token stream.
         *
         * @param question the user question being answered
         * @param answer the token stream from the AI model
         */
        public StreamingAnswer(String question, TokenStream answer)
            {
            this.question = question;
            this.answer   = answer;
            }

        /**
         * Writes the streaming response to the HTTP output stream.
         * <p/>
         * This method sets up the token stream handlers and blocks until the
         * complete response has been streamed to the client. Tokens are written
         * as they arrive from the AI model.
         *
         * @param out the HTTP response output stream
         * 
         * @throws WebApplicationException if streaming fails
         */
        public void write(OutputStream out)
                throws WebApplicationException
            {
            CountDownLatch finished = new CountDownLatch(1);
            long           start    = System.currentTimeMillis();

            answer.onPartialResponse(token -> writeToken(out, token, start))
                  .onCompleteResponse(res -> finishStreaming(out, start, finished))
                  .onError(Throwable::printStackTrace)
                  .start();

            try
                {
                finished.await();
                }
            catch (InterruptedException e)
                {
                throw new RuntimeException(e);
                }
            }

        /**
         * Writes a single token to the output stream.
         * <p/>
         * This method handles the streaming of individual tokens, tracking
         * performance metrics and ensuring proper UTF-8 encoding.
         *
         * @param out the output stream to write to
         * @param token the token text to write
         * @param startTime the start time for measuring time to first token
         */
        private void writeToken(OutputStream out, String token, long startTime)
            {
            try
                {
                if (cTokens++ == 0)
                    {
                    timeToFirstToken = System.currentTimeMillis() - startTime;
                    }

                out.write(token.getBytes(StandardCharsets.UTF_8));
                out.flush();
                }
            catch (IOException e)
                {
                throw new RuntimeException(e);
                }
            }

        /**
         * Completes the streaming response and logs performance metrics.
         * <p/>
         * This method is called when the token stream is complete. It logs
         * performance information and signals completion to waiting threads.
         *
         * @param out the output stream
         * @param startTime the streaming start time
         * @param finished the latch to signal completion
         */
        @SuppressWarnings("unused")
        private void finishStreaming(OutputStream out, long startTime, CountDownLatch finished)
            {
            long duration = System.currentTimeMillis() - startTime;
            finished.countDown();

            Logger.fine("Answered question '%s' in %,d ms (timeToFirstToken = %,d ms, tokenCount = %,d)"
                                .formatted(question, duration, timeToFirstToken, cTokens));
            }
        }

    /**
     * Request object for chat operations with RAG context retrieval.
     * <p/>
     * This record encapsulates all parameters needed for RAG-enhanced chat
     * functionality, including model selection, context retrieval settings,
     * and optional result re-ranking.
     *
     * @param chatModel name of the chat model to use (null for default)
     * @param question the user question to answer
     * @param maxResults maximum number of context documents to retrieve
     * @param minScore minimum relevance score for context documents
     * @param fullTextWeight weight for full-text search in context retrieval (0.0-1.0)
     * @param scoringModel optional scoring model for context re-ranking
     */
    public record ChatRequest(String chatModel, String question, int maxResults, double minScore, double fullTextWeight, String scoringModel)
        {
        /**
         * Returns the maximum results with a default value.
         * <p/>
         * This method ensures a sensible default when maxResults is 0.
         *
         * @return maxResults or 5 if maxResults is 0
         */
        public int maxResults()
            {
            return maxResults == 0 ? 5 : maxResults;
            }
        }

    /**
     * Request object for search operations with hybrid search capabilities.
     * <p/>
     * This record encapsulates all parameters needed for hybrid search
     * combining vector similarity and full-text search with optional
     * result re-ranking.
     *
     * @param query the search query text
     * @param maxResults maximum number of results to return
     * @param minScore minimum relevance score threshold
     * @param fullTextWeight weight for full-text search (0.0 = vector only, 1.0 = text only)
     * @param scoringModel optional scoring model for result re-ranking
     */
    public record SearchRequest(String query, int maxResults, double minScore, double fullTextWeight, String scoringModel)
        {
        /**
         * Returns the maximum results with a default value.
         * <p/>
         * This method ensures a sensible default when maxResults is 0.
         *
         * @return maxResults or 5 if maxResults is 0
         */
        public int maxResults()
            {
            return maxResults == 0 ? 5 : maxResults;
            }
        }

    /**
     * Response object containing search results with timing information.
     * <p/>
     * This record encapsulates the search results along with performance
     * metrics for monitoring and optimization purposes.
     *
     * @param results list of matching document chunks with relevance scores
     * @param searchDuration total search execution time in milliseconds
     */
    public record SearchResult(List<ChunkResult> results, long searchDuration)
        {}

    /**
     * Represents a search result containing a document chunk with relevance score.
     * <p/>
     * This class encapsulates a document chunk that matched a search query,
     * including its content, relevance score, and the index type used for
     * retrieval. It supports results from both vector similarity search
     * and full-text search.
     */
    public static final class ChunkResult
        {
        /**
         * Unique identifier for the chunk.
         */
        private final String chunkId;
        
        /**
         * Text content of the chunk.
         */
        private final String text;
        
        /**
         * Relevance score (0.0 to 1.0).
         */
        private double score;
        
        /**
         * Index type used for retrieval (e.g., "VECTOR", "FULL_TEXT").
         */
        private String index;

        /**
         * Creates a new chunk result with specified parameters.
         *
         * @param chunkId unique identifier for the chunk
         * @param text text content of the chunk
         * @param score relevance score
         * @param index index type used for retrieval
         */
        private ChunkResult(String chunkId, String text, double score, String index)
            {
            this.chunkId = chunkId;
            this.text = text;
            this.score = score;
            this.index = index;
            }

        /**
         * Creates a chunk result from an embedding store match.
         * <p/>
         * This constructor is used for results from vector similarity search.
         *
         * @param match the embedding match containing chunk and similarity score
         */
        public ChunkResult(EmbeddingMatch<TextSegment> match)
            {
            this(match.embeddingId(), match.embedded().text(), match.score(), "VECTOR");
            }

        /**
         * Creates a chunk result from a Lucene query result.
         * <p/>
         * This constructor is used for results from full-text search.
         *
         * @param result the query result containing chunk and relevance score
         */
        public ChunkResult(QueryResult<DocumentChunk.Id, DocumentChunk> result)
            {
            this(result.getKey().toString(), result.getValue().text(), result.getDistance(), "FULL_TEXT");
            }

        /**
         * Gets the unique identifier for this chunk.
         *
         * @return the chunk ID
         */
        @SuppressWarnings("unused")
        public String getChunkId()
            {
            return chunkId;
            }

        /**
         * Gets the relevance score for this chunk.
         *
         * @return the relevance score (0.0 to 1.0)
         */
        public double getScore()
            {
            return score;
            }

        /**
         * Sets the relevance score for this chunk.
         * <p/>
         * This method is used during result processing and re-ranking.
         *
         * @param score the new relevance score
         * 
         * @return this ChunkResult for method chaining
         */
        public ChunkResult setScore(double score)
            {
            this.score = score;
            return this;
            }

        /**
         * Gets the index type used to retrieve this chunk.
         *
         * @return the index type (e.g., "VECTOR", "FULL_TEXT")
         */
        public String getIndex()
            {
            return index;
            }

        /**
         * Sets the index type for this chunk.
         * <p/>
         * This method is used to track which search method produced this result.
         *
         * @param index the index type
         * 
         * @return this ChunkResult for method chaining
         */
        public ChunkResult setIndex(String index)
            {
            this.index = index;
            return this;
            }

        /**
         * Gets the text content of this chunk.
         *
         * @return the chunk text content
         */
        public String getText()
            {
            return text;
            }

        @Override
        public boolean equals(Object obj)
            {
            if (obj == this)
                {
                return true;
                }
            if (obj == null || obj.getClass() != this.getClass())
                {
                return false;
                }
            var that = (ChunkResult) obj;
            return Objects.equals(this.chunkId, that.chunkId) &&
                   Double.doubleToLongBits(this.score) == Double.doubleToLongBits(that.score) &&
                   Objects.equals(this.index, that.index) &&
                   Objects.equals(this.text, that.text);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(chunkId, score, index, text);
            }

        @Override
        public String toString()
            {
            return "ChunkResult[" +
                   "chunkId=" + chunkId + ", " +
                   "score=" + score + ", " +
                   "index=" + index + ", " +
                   "text=" + text + ']';
            }

        }

        /**
     * Record representing a document with its ID and text content.
     * <p/>
     * This record is used for document retrieval operations, encapsulating
     * the document identifier and its full text content.
     *
     * @param id the unique document identifier
     * @param text the full text content of the document
     */
    public record Doc(String id, String text)
        {}

    /**
     * Record representing a collection of document chunks for a specific document.
     * <p/>
     * This record is used for batch operations involving document chunks,
     * associating multiple chunks with their parent document.
     *
     * @param id the document identifier
     * @param chunks array of document chunks
     */
    public record DocChunks(String id, DocChunk[] chunks)
        {}

    /**
     * Record representing a single document chunk with its content and embedding.
     * <p/>
     * This record encapsulates a text chunk along with its metadata and
     * vector embedding. It is used for direct chunk manipulation and
     * batch processing operations.
     *
     * @param text      the text content of the chunk
     * @param metadata  additional metadata for the chunk
     * @param embedding the vector embedding for the chunk (can be null)
     */
    public record DocChunk(String text, Map<String, Object> metadata, float[] embedding)
        {
        /**
         * JSON-B creator constructor for deserialization.
         * <p/>
         * This constructor is used by JSON-B for deserializing DocChunk
         * instances from JSON representations.
         */
        @JsonbCreator
        public DocChunk
            {
            }

        /**
         * Creates a DocChunk from a DocumentChunk instance.
         * <p/>
         * This constructor converts internal DocumentChunk objects to
         * the external API representation.
         *
         * @param chunk the internal document chunk to convert
         */
        public DocChunk(DocumentChunk chunk)
            {
            this(chunk.text(), chunk.metadata(), chunk.vector().get());
            }
        }

    // ---- inner class: Stats MBean ----------------------------------------

    /**
     * Store stats exposed via JMX.
     */
    class Stats
            extends AbstractModel<Stats>
            implements DynamicMBean
        {
        private static final String MBEAN_DESCRIPTION = "A RAG knowledge base store.";

        protected static final SimpleModelAttribute<Stats> DOCUMENT_COUNT =
                SimpleModelAttribute.intBuilder("DocumentCount", Stats.class)
                    .withDescription("The number of documents stored on this member")
                    .metric(true)
                    .withFunction(Stats::documentCount)
                    .build();

        protected static final SimpleModelAttribute<Stats> DOCUMENT_COUNT_PROCESSED =
                SimpleModelAttribute.intBuilder("DocumentCountProcessed", Stats.class)
                    .withDescription("The number of documents ingested by this member")
                    .metric(true)
                    .withFunction(Stats::documentProcessedCount)
                    .build();

        protected static final SimpleModelAttribute<Stats> DOCUMENT_COUNT_FAILED =
                SimpleModelAttribute.intBuilder("DocumentCountFailed", Stats.class)
                    .withDescription("The number of document ingestion failures on this member")
                    .metric(true)
                    .withFunction(Stats::documentFailedCount)
                    .build();

        protected static final SimpleModelAttribute<Stats> DOCUMENT_COUNT_PENDING =
                SimpleModelAttribute.intBuilder("DocumentCountPending", Stats.class)
                    .withDescription("The number of pending documents that still need to be ingested")
                    .metric(true)
                    .withFunction(Stats::documentPendingCount)
                    .build();

        protected static final SimpleModelAttribute<Stats> DOCUMENT_LOAD_TIME =
                SimpleModelAttribute.longBuilder("DocumentLoadTime", Stats.class)
                    .withDescription("The total amount of time (im milliseconds) spent loading documents")
                    .metric(true)
                    .withFunction(Stats::documentLoadDuration)
                    .build();

        protected static final SimpleModelAttribute<Stats> DOCUMENT_SPLIT_TIME =
                SimpleModelAttribute.longBuilder("DocumentSplitTime", Stats.class)
                    .withDescription("The total amount of time (im milliseconds) spent splitting documents into chunks")
                    .metric(true)
                    .withFunction(Stats::documentSplitDuration)
                    .build();

        protected static final SimpleModelAttribute<Stats> EMBEDDING_COUNT =
                SimpleModelAttribute.intBuilder("EmbeddingCount", Stats.class)
                    .withDescription("The number of embeddings stored on this member")
                    .metric(true)
                    .withFunction(Stats::embeddingCount)
                    .build();

        protected static final SimpleModelAttribute<Stats> EMBEDDING_COUNT_PROCESSED =
                SimpleModelAttribute.longBuilder("EmbeddingCountProcessed", Stats.class)
                    .withDescription("The number of embeddings created by this member")
                    .metric(true)
                    .withFunction(Stats::embeddingProcessedCount)
                    .build();

        protected static final SimpleModelAttribute<Stats> EMBEDDING_COUNT_FAILED =
                SimpleModelAttribute.intBuilder("EmbeddingCountFailed", Stats.class)
                    .withDescription("The number of chunks we failed to create vector embeddings for")
                    .metric(true)
                    .withFunction(Stats::embeddingFailedCount)
                    .build();

        protected static final SimpleModelAttribute<Stats> EMBEDDING_COUNT_PENDING =
                SimpleModelAttribute.intBuilder("EmbeddingCountPending", Stats.class)
                    .withDescription("The number of pending document chunks that need to be embedded")
                    .metric(true)
                    .withFunction(Stats::embeddingPendingCount)
                    .build();

        protected static final SimpleModelAttribute<Stats> EMBEDDING_TIME =
                SimpleModelAttribute.longBuilder("EmbeddingTime", Stats.class)
                    .withDescription("The total amount of time (im milliseconds) spent creating vector embeddings")
                    .metric(true)
                    .withFunction(Stats::embeddingDuration)
                    .build();

        protected static final SimpleModelAttribute<Stats> EMBEDDING_TIME_MEAN =
                SimpleModelAttribute.doubleBuilder("EmbeddingTimeMean", Stats.class)
                    .withDescription("The average time (im milliseconds) it took to create a single vector embedding")
                    .metric(true)
                    .withFunction(Stats::embeddingAverageDuration)
                    .build();

        protected static final SimpleModelAttribute<Stats> EMBEDDING_RATE_MEAN =
                SimpleModelAttribute.doubleBuilder("EmbeddingRateMean", Stats.class)
                    .withDescription("The embedding request mean rate")
                    .metric("EmbeddingRate")
                    .withMetricLabels("rate", ModelAttribute.RATE_MEAN)
                    .withFunction(Stats::embeddingMeanRate)
                    .build();

        protected static final SimpleModelAttribute<Stats> EMBEDDING_RATE_ONE_MINUTE =
                SimpleModelAttribute.doubleBuilder("EmbeddingRate01", Stats.class)
                    .withDescription("The embedding request one-minute rate")
                    .metric("EmbeddingRate")
                    .withMetricLabels("rate", ModelAttribute.RATE_1MIN)
                    .withFunction(Stats::embeddingOneMinuteRate)
                    .build();

        protected static final SimpleModelAttribute<Stats> EMBEDDING_RATE_FIVE_MINUTE =
                SimpleModelAttribute.doubleBuilder("EmbeddingRate05", Stats.class)
                    .withDescription("The embedding request five-minute rate")
                    .metric("EmbeddingRate")
                    .withMetricLabels("rate", ModelAttribute.RATE_5MIN)
                    .withFunction(Stats::embeddingFiveMinuteRate)
                    .build();

        protected static final SimpleModelAttribute<Stats> EMBEDDING_RATE_FIFTEEN_MINUTE =
                SimpleModelAttribute.doubleBuilder("EmbeddingRate15", Stats.class)
                    .withDescription("The embedding request fifteen-minute rate")
                    .metric("EmbeddingRate")
                    .withMetricLabels("rate", ModelAttribute.RATE_15MIN)
                    .withFunction(Stats::embeddingFifteenMinuteRate)
                    .build();
        
        protected static final SimpleModelAttribute<Stats> SEARCH_COUNT =
                SimpleModelAttribute.longBuilder("SearchCount", Stats.class)
                    .withDescription("The number of searches performed")
                    .metric(true)
                    .withFunction(Stats::searchCount)
                    .build();

        protected static final SimpleModelAttribute<Stats> SEARCH_TIME =
                SimpleModelAttribute.longBuilder("SearchTime", Stats.class)
                    .withDescription("The total amount of time (im milliseconds) spent searching")
                    .metric(true)
                    .withFunction(Stats::searchDuration)
                    .build();

        protected static final SimpleModelAttribute<Stats> SEARCH_TIME_MEAN =
                SimpleModelAttribute.doubleBuilder("SearchTimeMean", Stats.class)
                    .withDescription("The average time (im milliseconds) each search took")
                    .metric(true)
                    .withFunction(Stats::averageSearchDuration)
                    .build();

        protected static final SimpleModelAttribute<Stats> SEARCH_RATE_MEAN =
                SimpleModelAttribute.doubleBuilder("SearchRateMean", Stats.class)
                    .withDescription("The search request mean rate")
                    .metric("SearchRate")
                    .withMetricLabels("rate", ModelAttribute.RATE_MEAN)
                    .withFunction(Stats::searchMeanRate)
                    .build();

        protected static final SimpleModelAttribute<Stats> SEARCH_RATE_ONE_MINUTE =
                SimpleModelAttribute.doubleBuilder("SearchRate01", Stats.class)
                    .withDescription("The search request one-minute rate")
                    .metric("SearchRate")
                    .withMetricLabels("rate", ModelAttribute.RATE_1MIN)
                    .withFunction(Stats::searchOneMinuteRate)
                    .build();

        protected static final SimpleModelAttribute<Stats> SEARCH_RATE_FIVE_MINUTE =
                SimpleModelAttribute.doubleBuilder("SearchRate05", Stats.class)
                    .withDescription("The search request five-minute rate")
                    .metric("SearchRate")
                    .withMetricLabels("rate", ModelAttribute.RATE_5MIN)
                    .withFunction(Stats::searchFiveMinuteRate)
                    .build();

        protected static final SimpleModelAttribute<Stats> SEARCH_RATE_FIFTEEN_MINUTE =
                SimpleModelAttribute.doubleBuilder("SearchRate15", Stats.class)
                    .withDescription("The search request fifteen-minute rate")
                    .metric("SearchRate")
                    .withMetricLabels("rate", ModelAttribute.RATE_15MIN)
                    .withFunction(Stats::searchFifteenMinuteRate)
                    .build();
        
        protected static final SimpleModelOperation<Stats> RESET_STATISTICS =
                SimpleModelOperation.builder("resetStatistics", Stats.class)
                   .withDescription("Reset statistics")
                   .withFunction(Stats::reset)
                   .build();

        Stats()
            {
            super(MBEAN_DESCRIPTION);

            addAttribute(DOCUMENT_COUNT);
            addAttribute(DOCUMENT_COUNT_PROCESSED);
            addAttribute(DOCUMENT_COUNT_FAILED);
            addAttribute(DOCUMENT_COUNT_PENDING);
            addAttribute(DOCUMENT_LOAD_TIME);
            addAttribute(DOCUMENT_SPLIT_TIME);

            addAttribute(EMBEDDING_COUNT);
            addAttribute(EMBEDDING_COUNT_PROCESSED);
            addAttribute(EMBEDDING_COUNT_FAILED);
            addAttribute(EMBEDDING_COUNT_PENDING);
            addAttribute(EMBEDDING_TIME);
            addAttribute(EMBEDDING_TIME_MEAN);
            addAttribute(EMBEDDING_RATE_MEAN);
            addAttribute(EMBEDDING_RATE_ONE_MINUTE);
            addAttribute(EMBEDDING_RATE_FIVE_MINUTE);
            addAttribute(EMBEDDING_RATE_FIFTEEN_MINUTE);

            addAttribute(SEARCH_COUNT);
            addAttribute(SEARCH_TIME);
            addAttribute(SEARCH_TIME_MEAN);
            addAttribute(SEARCH_RATE_MEAN);
            addAttribute(SEARCH_RATE_ONE_MINUTE);
            addAttribute(SEARCH_RATE_FIVE_MINUTE);
            addAttribute(SEARCH_RATE_FIFTEEN_MINUTE);

            addOperation(RESET_STATISTICS);

            var cluster  = docs.getService().getCluster();
            var registry = cluster.getManagement();
            proxy = registry.getMBeanServerProxy().local();

            sNameDocs   = getCacheMBeanName(registry, docs);
            sNameChunks = getCacheMBeanName(registry, chunks);
            }

        private String getCacheMBeanName(Registry registry, NamedMap<?, ?> cache)
            {
            CacheService service = cache.getService();
            String sName = Registry.CACHE_TYPE +
                "," + Registry.KEY_SERVICE + service.getInfo().getServiceName() +
                ",name=" + cache.getName();

            sName = registry.ensureGlobalName(sName);
            sName = sName + ",tier=back";

            return sName;
            }

        int documentCount()
            {
            return (int) proxy.getAttribute(sNameDocs, "Size");
            }

        int documentProcessedCount()
            {
            return documentProcessedCount.get();
            }

        int documentFailedCount()
            {
            return documentFailedCount.get();
            }

        int documentPendingCount()
            {
            return docsSubscriber.getRemainingMessages();
            }

        long documentLoadDuration()
            {
            return documentLoadDuration.get();
            }

        long documentSplitDuration()
            {
            return documentSplitDuration.get();
            }

        int embeddingCount()
            {
            return (int) proxy.getAttribute(sNameChunks, "Size");
            }

        long embeddingProcessedCount()
            {
            return embeddingMeter.getCount();
            }

        int embeddingFailedCount()
            {
            return embeddingFailedCount.get();
            }

        int embeddingPendingCount()
            {
            return chunksSubscriber.getRemainingMessages();
            }

        long embeddingDuration()
            {
            return embeddingDuration.get();
            }

        double embeddingAverageDuration()
            {
            return embeddingDuration.doubleValue() / embeddingMeter.getCount();
            }

        double embeddingFifteenMinuteRate()
            {
            return embeddingMeter.getFifteenMinuteRate();
            }

        double embeddingFiveMinuteRate()
            {
            return embeddingMeter.getFiveMinuteRate();
            }

        double embeddingOneMinuteRate()
            {
            return embeddingMeter.getOneMinuteRate();
            }

        double embeddingMeanRate()
            {
            return embeddingMeter.getMeanRate();
            }
        
        public long searchCount()
            {
            return searchMeter.getCount();
            }

        public long searchDuration()
            {
            return searchDuration.get();
            }

        double averageSearchDuration()
            {
            return searchDuration.doubleValue() / searchMeter.getCount();
            }

        double searchFifteenMinuteRate()
            {
            return searchMeter.getFifteenMinuteRate();
            }

        double searchFiveMinuteRate()
            {
            return searchMeter.getFiveMinuteRate();
            }

        double searchOneMinuteRate()
            {
            return searchMeter.getOneMinuteRate();
            }

        double searchMeanRate()
            {
            return searchMeter.getMeanRate();
            }
        
        void finishDocument(Duration loadDuration, Duration splitDuration)
            {
            documentProcessedCount.incrementAndGet();
            documentLoadDuration.addAndGet(loadDuration.toMillis());
            documentSplitDuration.addAndGet(splitDuration.toMillis());
            }

        void failDocument(Duration loadDuration, Duration splitDuration)
            {
            documentFailedCount.incrementAndGet();
            documentLoadDuration.addAndGet(loadDuration.toMillis());
            documentSplitDuration.addAndGet(splitDuration.toMillis());
            }

        void finishEmbeddings(int count, Duration duration)
            {
            embeddingMeter.mark(count);
            embeddingDuration.addAndGet(duration.toMillis());
            }

        void failEmbeddings(int count, Duration duration)
            {
            embeddingFailedCount.addAndGet(count);
            embeddingDuration.addAndGet(duration.toMillis());
            }

        void recordSearch(Duration duration)
            {
            searchMeter.mark();
            searchDuration.addAndGet(duration.toMillis());
            }

        void reset(Object[] objects)
            {
            documentProcessedCount.set(0);
            documentFailedCount.set(0);
            documentLoadDuration.set(0);
            documentSplitDuration.set(0);

            embeddingMeter = new Meter();
            embeddingFailedCount.set(0);
            embeddingDuration.set(0);

            searchMeter = new Meter();
            searchDuration.set(0);
            }

        private final AtomicInteger documentProcessedCount = new AtomicInteger();
        private final AtomicInteger documentFailedCount = new AtomicInteger();
        private final AtomicLong documentLoadDuration = new AtomicLong();
        private final AtomicLong documentSplitDuration = new AtomicLong();

        private volatile Meter embeddingMeter = new Meter();
        private final AtomicInteger embeddingFailedCount = new AtomicInteger();
        private final AtomicLong embeddingDuration = new AtomicLong();

        private volatile Meter searchMeter = new Meter();
        private final AtomicLong searchDuration = new AtomicLong();

        private final MBeanServerProxy proxy;
        private final String sNameDocs;
        private final String sNameChunks;
        }
    }
