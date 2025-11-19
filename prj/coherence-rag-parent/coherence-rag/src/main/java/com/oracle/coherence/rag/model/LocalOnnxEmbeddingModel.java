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
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;

import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import io.helidon.http.Status;
import io.helidon.webclient.api.HttpClientRequest;
import io.helidon.webclient.api.HttpClientResponse;
import io.helidon.webclient.api.WebClient;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Local ONNX embedding model implementation that integrates with LangChain4J.
 * <p/>
 * This class provides a local embedding model that can run inference using ONNX Runtime,
 * with support for both CPU and GPU acceleration via CUDA. It automatically downloads
 * model files from HuggingFace when needed and supports efficient batch processing.
 * <p/>
 * The model extends {@link DimensionAwareEmbeddingModel} to provide seamless integration
 * with LangChain4J's ecosystem. It supports automatic model downloading, CUDA acceleration,
 * and efficient resource management.
 * <p/>
 * Example usage:
 * <pre>
 * // Create default model from classpath
 * LocalOnnxEmbeddingModel model = LocalOnnxEmbeddingModel.createDefault(ModelName.ALL_MINILM_L6_V2);
 * 
 * // Create model with automatic download
 * LocalOnnxEmbeddingModel model = LocalOnnxEmbeddingModel.create(ModelName.ALL_MPNET_BASE_V2);
 * 
 * // Generate embeddings
 * Response&lt;Embedding&gt; response = model.embed("Hello world");
 * Embedding embedding = response.content();
 * 
 * // Batch processing
 * List&lt;TextSegment&gt; segments = Arrays.asList(
 *     TextSegment.from("First text"), 
 *     TextSegment.from("Second text")
 * );
 * Response&lt;List&lt;Embedding&gt;&gt; batchResponse = model.embedAll(segments);
 * </pre>
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
public class LocalOnnxEmbeddingModel
        extends DimensionAwareEmbeddingModel
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
     * JSON-B instance for configuration parsing.
     */
    private static final Jsonb JSONB = JsonbBuilder.create();

    /**
     * The model name identifier.
     */
    private final ModelName name;
    
    /**
     * The underlying ONNX embedding model.
     */
    private final OnnxEmbeddingModel model;

    /**
     * Flag indicating if CUDA acceleration is enabled.
     */
    private boolean cuda = false;
    
    /**
     * Creates a default embedding model from the classpath.
     * 
     * @param name  the model name to create
     *
     * @return the configured embedding model
     */
    public static LocalOnnxEmbeddingModel createDefault(ModelName name)
        {
        Logger.config("Loading embedding model [%s] from class path".formatted(name.name()));
        PoolingConfig config    = JSONB.fromJson(inputStreamFor(name, "config.json"), PoolingConfig.class);
        InputStream   model     = inputStreamFor(name, "model.onnx");
        InputStream   tokenizer = inputStreamFor(name, "tokenizer.json");

        return new LocalOnnxEmbeddingModel(name, model, tokenizer, config);
        }

    /**
     * Creates an embedding model with automatic download if needed.
     * 
     * @param name  the model name to create
     *
     * @return the configured embedding model
     */
    public static LocalOnnxEmbeddingModel create(ModelName name)
        {
        try
            {
            PoolingConfig config    = getPoolingConfig(name);
            InputStream   model     = getModelStream(name);
            InputStream   tokenizer = getTokenizerStream(name);

            return new LocalOnnxEmbeddingModel(name, model, tokenizer, config);
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }
        }

    /**
     * Constructs a local ONNX embedding model with the specified components.
     * 
     * @param name         the model name
     * @param inModel      the input stream for the model
     * @param inTokenizer  the input stream for the tokenizer
     * @param config       the pooling configuration
     */
    protected LocalOnnxEmbeddingModel(ModelName name, InputStream inModel, InputStream inTokenizer, PoolingConfig config)
        {
        this.name      = name;
        this.model     = createOnnxModel(inModel, inTokenizer, config);
        this.dimension = config.dimension();
        }

    /**
     * Creates the underlying ONNX model with optional CUDA acceleration.
     * 
     * @param inModel      the input stream for the model
     * @param inTokenizer  the input stream for the tokenizer
     * @param config       the pooling configuration
     *
     * @return the configured ONNX embedding model
     */
    private OnnxEmbeddingModel createOnnxModel(InputStream inModel, InputStream inTokenizer, PoolingConfig config)
        {
        try
            {
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession session;

            byte[] abModel = inModel.readAllBytes();
            var opts = new OrtSession.SessionOptions();
            int deviceId = Config.getInteger("cuda.id", 0);
            try
                {
                // attempt to enable CUDA first
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

            Logger.config("Configured embedding model ONNX Runtime: " + (cuda ? "CUDA #" + deviceId : "CPU"));
            return new OnnxEmbeddingModel(env, session, inTokenizer, config.mode());
            }
        catch (OrtException | IOException e)
            {
            throw new RuntimeException(e);
            }
        }

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
     * Returns the underlying ONNX embedding model.
     * 
     * @return the ONNX embedding model
     */
    protected OnnxEmbeddingModel model()
        {
        return model;
        }

    /**
     * Embeds a single text string.
     * 
     * @param text  the text to embed
     *
     * @return the response containing the embedding
     */
    public Response<Embedding> embed(String text)
        {
        float[] embedding = model().embed(text);
        return Response.from(
                Embedding.from(embedding),
                new TokenUsage(0) // do not count special tokens [CLS] and [SEP])
        );
        }

    /**
     * Embeds a single text segment.
     * 
     * @param textSegment  the text segment to embed
     *
     * @return the response containing the embedding
     */
    public Response<Embedding> embed(TextSegment textSegment)
        {
        return embed(textSegment.text());
        }

    /**
     * Embeds multiple text segments efficiently as a batch.
     * 
     * @param segments  the list of text segments to embed
     *
     * @return the response containing the list of embeddings
     */
    public Response<List<Embedding>> embedAll(List<TextSegment> segments)
        {
        int           tokenCount = 0;
        List<float[]> embeddings = model().embedAll(segments.stream().map(TextSegment::text).toList());

        return Response.from(embeddings.stream().map(Embedding::from).toList(), new TokenUsage(tokenCount));
        }

    /**
     * Returns a string representation of this model.
     * 
     * @return the string representation
     */
    public String toString()
        {
        return "LocalOnnxEmbeddingModel{" +
               "name=" + name.fullName() +
               ", dimension=" + dimension +
               ", runtime=" + (cuda ? "CUDA" : "CPU") +
               '}';
        }

    // ---- helpers ---------------------------------------------------------

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
     * Gets the pooling configuration, downloading if necessary.
     * 
     * @param name  the model name
     *
     * @return the pooling configuration
     * @throws IOException  if there's an error accessing the configuration
     */
    static PoolingConfig getPoolingConfig(ModelName name)
            throws IOException
        {
        Path path = pathTo(name, "config.json");
        if (!Files.exists(path))
            {
            fetch(name, path, "1_Pooling/config.json", new MemorySize(1024));
            }

        return JSONB.fromJson(Files.newInputStream(path), PoolingConfig.class);
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
