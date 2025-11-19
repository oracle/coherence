/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.providers.OrtCUDAProviderOptions;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.util.MemorySize;

import com.tangosol.coherence.config.Config;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;

import io.helidon.http.Status;

import io.helidon.webclient.api.HttpClientRequest;
import io.helidon.webclient.api.HttpClientResponse;
import io.helidon.webclient.api.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.exp;

/**
 * Local ONNX scoring model implementation that integrates with LangChain4J.
 * <p/>
 * This class provides a local scoring model that can run inference using ONNX Runtime,
 * with support for both CPU and GPU acceleration via CUDA. It automatically downloads
 * model files from HuggingFace when needed and provides efficient document scoring
 * capabilities for retrieval-augmented generation (RAG) applications.
 * <p/>
 * The model implements {@link ScoringModel} to provide seamless integration
 * with LangChain4J's ecosystem. It uses BERT cross-encoder architecture for
 * calculating relevance scores between queries and documents.
 * <p/>
 * Example usage:
 * <pre>
 * // Create default model from classpath
 * LocalOnnxScoringModel model = LocalOnnxScoringModel.createDefault(ModelName.MS_MARCO_MINILM_L6_V2);
 * 
 * // Create model with automatic download
 * LocalOnnxScoringModel model = LocalOnnxScoringModel.create(ModelName.MS_MARCO_TINYBERT_L6_V2);
 * 
 * // Score a single document
 * String query = "What is machine learning?";
 * TextSegment document = TextSegment.from("Machine learning is a subset of artificial intelligence...");
 * Response&lt;Double&gt; score = model.score(document, query);
 * 
 * // Score multiple documents
 * List&lt;TextSegment&gt; documents = Arrays.asList(
 *     TextSegment.from("Document 1 content"),
 *     TextSegment.from("Document 2 content")
 * );
 * Response&lt;List&lt;Double&gt;&gt; scores = model.scoreAll(documents, query);
 * </pre>
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
public class LocalOnnxScoringModel
        implements ScoringModel, AutoCloseable
    {
    /**
     * Base path for model storage.
     */
    private static final Path BASE_PATH = Path.of("models");
    
    /**
     * Web client for downloading models from HuggingFace.
     */
    private static final WebClient CLIENT = WebClient.builder()
                            .baseUri("https://huggingface.co/")
                            .build();

    /**
     * The model name identifier.
     */
    private final ModelName name;
    
    /**
     * The underlying ONNX BERT cross-encoder model.
     */
    private final OnnxBertCrossEncoder model;
    
    /**
     * Flag indicating if CUDA acceleration is enabled.
     */
    private boolean cuda = false;

    /**
     * Creates a default scoring model from the classpath.
     * 
     * @param name  the model name to create
     *
     * @return the configured scoring model
     */
    public static LocalOnnxScoringModel createDefault(ModelName name)
        {
        Logger.config("Loading scoring model [%s] from class path".formatted(name.name()));
        InputStream model     = inputStreamFor(name, "model.onnx");
        InputStream tokenizer = inputStreamFor(name, "tokenizer.json");

        return new LocalOnnxScoringModel(name, model, tokenizer);
        }

    /**
     * Creates a scoring model with automatic download if needed.
     * 
     * @param name  the model name to create
     *
     * @return the configured scoring model
     */
    public static LocalOnnxScoringModel create(ModelName name)
        {
        try
            {
            InputStream   model     = getModelStream(name);
            InputStream   tokenizer = getTokenizerStream(name);

            return new LocalOnnxScoringModel(name, model, tokenizer);
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }
        }

    /**
     * Constructs a local ONNX scoring model with the specified components.
     * 
     * @param name         the model name
     * @param inModel      the input stream for the model
     * @param inTokenizer  the input stream for the tokenizer
     */
    protected LocalOnnxScoringModel(ModelName name, InputStream inModel, InputStream inTokenizer)
        {
        this.name  = name;
        this.model = createOnnxModel(inModel, inTokenizer);
        }

    /**
     * Creates the underlying ONNX model with optional CUDA acceleration.
     * 
     * @param inModel      the input stream for the model
     * @param inTokenizer  the input stream for the tokenizer
     *
     * @return the configured ONNX BERT cross-encoder model
     */
    private OnnxBertCrossEncoder createOnnxModel(InputStream inModel, InputStream inTokenizer)
        {
        try
            {
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session;

            byte[] abModel = inModel.readAllBytes();
            int deviceId = Config.getInteger("cuda.id", 0);
            try
                {
                // attempt to enable CUDA first
                var opts = new OrtSession.SessionOptions();
                var cudaOpts = new OrtCUDAProviderOptions(deviceId);
                cudaOpts.add("arena_extend_strategy","kSameAsRequested");
                cudaOpts.add("cudnn_conv_algo_search","DEFAULT");
                cudaOpts.add("do_copy_in_default_stream","1");
                cudaOpts.add("cudnn_conv_use_max_workspace","1");
                cudaOpts.add("cudnn_conv1d_pad_to_nc1d","1");
                opts.addCUDA(cudaOpts);

                session = env.createSession(abModel, opts);
                cuda = true;
                }
            catch (OrtException e)
                {
                // fall back to CPU
                session = env.createSession(abModel);
                }

            Logger.config("Configured scoring model ONNX Runtime: " + (cuda ? "CUDA #" + deviceId : "CPU"));
            return new OnnxBertCrossEncoder(env, session, inTokenizer);
            }
        catch (OrtException | IOException e)
            {
            throw new RuntimeException(e);
            }
        }


    // ---- accessors -------------------------------------------------------

    /**
     * Returns the model name.
     * 
     * @return the model name
     */
    public ModelName name()
        {
        return name;
        }

    /**
     * Returns the underlying ONNX BERT cross-encoder model.
     * 
     * @return the ONNX BERT cross-encoder model
     */
    protected OnnxBertCrossEncoder model()
        {
        return model;
        }

    // ---- ScoringModel interface ------------------------------------------

    /**
     * Scores a single text segment against a query.
     * 
     * @param segment  the text segment to score
     * @param query    the query to score against
     *
     * @return the response containing the relevance score
     */
    public Response<Double> score(TextSegment segment, String query)
        {
        return Response.from(sigmoid(model.encode(query, segment.text())));
        }

    /**
     * Scores multiple text segments against a query.
     * 
     * @param segments  the list of text segments to score
     * @param query     the query to score against
     *
     * @return the response containing the list of relevance scores
     */
    public Response<List<Double>> scoreAll(List<TextSegment> segments, String query)
        {
        List<Double> scores = new ArrayList<>();

        double score;
        for (TextSegment segment : segments)
            {
            score = sigmoid(model.encode(query, segment.text()));
            scores.add(score);
            }
        return Response.from(scores);
        }

    // ---- AutoCloseable interface -----------------------------------------

    /**
     * Close the native ONNX model and release associated resources.
     *
     * @throws Exception  if an error occurs
     */
    public void close() throws Exception
        {
        if (model != null)
            {
            model.close();
            }
        }

    // ---- Object methods --------------------------------------------------

    /**
     * Returns a string representation of this model.
     * 
     * @return the string representation
     */
    public String toString()
        {
        return "LocalOnnxScoringModel{" +
                "modelName=" + name.fullName() +
               '}';
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Applies sigmoid function to convert raw scores to probabilities.
     * 
     * @param x  the raw score
     *
     * @return the sigmoid-transformed score
     */
    static double sigmoid(double x) {
        // clip x in [-36, 36] to prevent overflow/underflow.
        x = Math.max(-36, Math.min(x, 36));
        return 1.0 / (1.0 + exp(-x));
    }

    /**
     * Gets the model input stream, downloading if necessary.
     * 
     * @param name  the model name
     *
     * @return the model input stream
     * @throws IOException  if there's an error accessing the model
     */
    static InputStream getModelStream(ModelName name)
            throws IOException
        {
        Path path = pathTo(name, "model.onnx");
        if (!Files.exists(path))
            {
            fetch(name, path, "onnx/model.onnx", new MemorySize("64k"));
            }

        return Files.newInputStream(path);
        }

    /**
     * Gets the tokenizer input stream, downloading if necessary.
     * 
     * @param name  the model name
     *
     * @return the tokenizer input stream
     * @throws IOException  if there's an error accessing the tokenizer
     */
    static InputStream getTokenizerStream(ModelName name)
            throws IOException
        {
        Path path = pathTo(name, "tokenizer.json");
        if (!Files.exists(path))
            {
            fetch(name, path, "tokenizer.json", new MemorySize("64k"));
            }

        return Files.newInputStream(path);
        }

    /**
     * Gets an input stream for a model file from the classpath.
     * 
     * @param name      the model name
     * @param fileName  the file name
     *
     * @return the input stream for the file
     */
    static InputStream inputStreamFor(ModelName name, String fileName)
        {
        return LocalOnnxEmbeddingModel.class.getResourceAsStream("/models/" + name.name() + "/" + fileName);
        }

    /**
     * Creates a path to a model file.
     * 
     * @param modelName  the model name
     * @param path       the path components
     *
     * @return the complete path to the model file
     */
    static Path pathTo(ModelName modelName, String... path)
        {
        return BASE_PATH
                .resolve(modelName.provider())
                .resolve(Path.of(modelName.name(), path));
        }

    /**
     * Fetches a model file from HuggingFace if it doesn't exist locally.
     * 
     * @param modelName   the model name
     * @param path        the local path to save the file
     * @param uri         the URI to download from
     * @param bufferSize  the buffer size for downloading
     *
     * @throws IOException  if there's an error downloading the file
     */
    private static void fetch(ModelName modelName, Path path, String uri, MemorySize bufferSize)
            throws IOException
        {
        int cbBufferSize = (int) bufferSize.getByteCount();

        String url = "%s/resolve/main/%s".formatted(modelName.fullName(), uri);
        HttpClientRequest request = CLIENT.get(url);
        Logger.info("Downloading %s to %s".formatted(request.uri(), path));

        Files.createDirectories(path.getParent());
        try (HttpClientResponse response = request.request())
            {
            if (response.status() != Status.OK_200)
                {
                throw new IOException("Failed to download %s: %s".formatted(request.uri(), response.status()));
                }

            try (OutputStream out = Files.newOutputStream(path))
                {
                InputStream in  = response.inputStream();
                byte[]      buf = new byte[cbBufferSize];

                int cbRead;
                while ((cbRead = in.read(buf, 0, cbBufferSize)) != -1)
                    {
                    out.write(buf, 0, cbRead);
                    }
                }
            }
        }
    }
