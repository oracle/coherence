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
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Core ONNX embedding model implementation that provides efficient text embeddings
 * using ONNX Runtime and HuggingFace tokenizers.
 * <p/>
 * This class handles the low-level ONNX model execution for embedding tasks,
 * including tokenization, tensor creation, model inference, and embedding pooling.
 * It supports both CLS and mean pooling strategies for generating sentence embeddings.
 * <p/>
 * Example usage:
 * <pre>
 * // Create from input streams
 * OnnxEmbeddingModel model = new OnnxEmbeddingModel(
 *     modelInputStream,
 *     tokenizerInputStream,
 *     sessionOptions,
 *     PoolingMode.MEAN
 * );
 * 
 * // Generate embeddings
 * float[] embedding = model.embed("Hello world");
 * List&lt;float[]&gt; embeddings = model.embedAll(Arrays.asList("Text 1", "Text 2"));
 * 
 * // Clean up resources
 * model.close();
 * </pre>
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
public class OnnxEmbeddingModel
    {
    /**
     * Maximum sequence length for tokenization.
     */
    public static final int MAX_SEQUENCE_LENGTH = 512;
    
    /**
     * Default tokenizer configuration options.
     */
    public static final Map<String, String> TOKENIZER_OPTIONS = Collections.singletonMap("padding", "false");

    /**
     * The ONNX Runtime environment.
     */
    private final OrtEnvironment env;
    
    /**
     * The ONNX Runtime session.
     */
    private final OrtSession session;
    
    /**
     * The HuggingFace tokenizer.
     */
    private final HuggingFaceTokenizer tokenizer;
    
    /**
     * The pooling mode for aggregating token embeddings.
     */
    private final PoolingMode poolingMode;

    /**
     * Constructs an ONNX embedding model with an existing ONNX Runtime environment and session.
     * 
     * @param env                   the ONNX Runtime environment
     * @param session               the ONNX Runtime session
     * @param tokenizerInputStream  the input stream for the tokenizer
     * @param poolingMode           the pooling mode to use
     *
     * @throws OrtException  if there's an error with ONNX Runtime operations
     * @throws IOException   if there's an error reading the tokenizer
     */
    public OnnxEmbeddingModel(OrtEnvironment env,
                              OrtSession session,
                              InputStream tokenizerInputStream,
                              PoolingMode poolingMode)
            throws OrtException, IOException
        {
        this.env = env;
        this.session = session;
        this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizerInputStream, TOKENIZER_OPTIONS);
        this.poolingMode = poolingMode;
        }

    /**
     * Constructs an ONNX embedding model from input streams with fallback session options.
     * 
     * @param modelInputStream      the input stream for the model
     * @param tokenizerInputStream  the input stream for the tokenizer
     * @param defaultOpts           the default session options
     * @param poolingMode           the pooling mode to use
     *
     * @throws OrtException  if there's an error with ONNX Runtime operations
     * @throws IOException   if there's an error reading the input streams
     */
    public OnnxEmbeddingModel(InputStream modelInputStream,
                              InputStream tokenizerInputStream,
                              OrtSession.SessionOptions defaultOpts,
                              PoolingMode poolingMode)
            throws OrtException, IOException
        {
        this(modelInputStream, tokenizerInputStream, defaultOpts, null, poolingMode);
        }

    /**
     * Constructs an ONNX embedding model from input streams with fallback session options.
     * 
     * @param modelInputStream      the input stream for the model
     * @param tokenizerInputStream  the input stream for the tokenizer
     * @param defaultOpts           the default session options
     * @param fallbackOpts          the fallback session options if default fails
     * @param poolingMode           the pooling mode to use
     *
     * @throws OrtException  if there's an error with ONNX Runtime operations
     * @throws IOException   if there's an error reading the input streams
     */
    public OnnxEmbeddingModel(InputStream modelInputStream,
                              InputStream tokenizerInputStream,
                              OrtSession.SessionOptions defaultOpts,
                              OrtSession.SessionOptions fallbackOpts,
                              PoolingMode poolingMode)
            throws OrtException, IOException
        {
        this.env = OrtEnvironment.getEnvironment();
        this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizerInputStream, TOKENIZER_OPTIONS);
        this.poolingMode = poolingMode;

        byte[] modelBytes = modelInputStream.readAllBytes();
        OrtSession session;
        try
            {
            session = env.createSession(modelBytes, defaultOpts);
            }
        catch (OrtException e)
            {
            if (fallbackOpts != null)
                {
                session = env.createSession(modelBytes, fallbackOpts);
                }
            else
                {
                throw e;
                }
            }
        this.session = session;
        }

    /**
     * Constructs an ONNX embedding model from file paths.
     * 
     * @param modelPath             the path to the model file
     * @param tokenizerPath         the path to the tokenizer file
     * @param defaultOpts           the default session options
     * @param poolingMode           the pooling mode to use
     *
     * @throws OrtException  if there's an error with ONNX Runtime operations
     * @throws IOException   if there's an error reading the files
     */
    public OnnxEmbeddingModel(Path modelPath,
                              Path tokenizerPath,
                              OrtSession.SessionOptions defaultOpts,
                              PoolingMode poolingMode)
            throws OrtException, IOException
        {
        this(modelPath, tokenizerPath, defaultOpts, null, poolingMode);
        }
    
    /**
     * Constructs an ONNX embedding model from file paths with fallback session options.
     * 
     * @param modelPath             the path to the model file
     * @param tokenizerPath         the path to the tokenizer file
     * @param defaultOpts           the default session options
     * @param fallbackOpts          the fallback session options if default fails
     * @param poolingMode           the pooling mode to use
     *
     * @throws OrtException  if there's an error with ONNX Runtime operations
     * @throws IOException   if there's an error reading the files
     */
    public OnnxEmbeddingModel(Path modelPath,
                              Path tokenizerPath,
                              OrtSession.SessionOptions defaultOpts,
                              OrtSession.SessionOptions fallbackOpts,
                              PoolingMode poolingMode)
            throws OrtException, IOException
        {
        this.env = OrtEnvironment.getEnvironment();
        this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizerPath, TOKENIZER_OPTIONS);
        this.poolingMode = poolingMode;

        ByteBuffer bufModel = mapFileToMemory(modelPath);
        OrtSession session;
        try
            {
            session = env.createSession(bufModel, defaultOpts);
            }
        catch (OrtException e)
            {
            if (fallbackOpts != null)
                {
                session = env.createSession(bufModel, fallbackOpts);
                }
            else
                {
                throw e;
                }
            }
        this.session = session;
        }

    /**
     * Generates an embedding for a single document.
     * 
     * @param document  the document text to embed
     *
     * @return the embedding vector as a float array
     */
    public float[] embed(String document)
        {
        return embedAll(Collections.singletonList(document)).getFirst();
        }

    /**
     * Generates embeddings for multiple documents efficiently as a batch.
     * 
     * @param documents  the list of document texts to embed
     *
     * @return a list of embedding vectors, one per document
     */
    public List<float[]> embedAll(List<String> documents)
        {
        if (documents == null || documents.isEmpty())
            {
            return Collections.emptyList();
            }

        try
            {
            int batchSize = documents.size();

            List<Encoding> encodedResults = documents.stream()
                    .map(tokenizer::encode)
                    .toList();

            int maxLength = encodedResults.stream()
                    .mapToInt(r -> r.getIds().length)
                    .max()
                    .orElse(1);

            maxLength = Math.min(maxLength, MAX_SEQUENCE_LENGTH);

            List<float[]> embeddings = new ArrayList<>(batchSize);
            Map<String, OnnxTensor> inputs = null;
            try
                {
                inputs = createInputs(encodedResults, batchSize, maxLength);

                try (OrtSession.Result result = session.run(inputs))
                    {
                    Optional<OnnxValue> sentenceEmbeddings = result.get("sentence_embedding");
                    if (sentenceEmbeddings.isPresent() && sentenceEmbeddings.get() instanceof OnnxTensor tensor)
                        {
                        FloatBuffer buf = tensor.getFloatBuffer();
                        int embeddingSize = buf.limit() / batchSize;
                        for (int of = 0; of < buf.limit(); of += embeddingSize)
                            {
                            float[] embedding = new float[embeddingSize];
                            buf.get(of, embedding);
                            embeddings.add(embedding);
                            }
                        }
                    else
                        {
                        FloatBuffer bufOutput = extractOutput(result);
                        int embeddingSize = bufOutput.limit() / batchSize / maxLength;
                        LongBuffer attentionMask = inputs.get("attention_mask").getLongBuffer();

                        for (int i = 0; i < batchSize; i++)
                            {
                            float[] embedding = new float[embeddingSize];
                            int of  = i * maxLength * embeddingSize;
                            int len = maxLength * embeddingSize;
                            if (poolingMode == PoolingMode.CLS)
                                {
                                clsPoolingAndNormalize(bufOutput.slice(of, len), embedding);
                                }
                            else
                                {
                                meanPoolingAndNormalize(bufOutput.slice(of, len), attentionMask.slice(i * maxLength, maxLength), maxLength, embedding);
                                }
                            embeddings.add(embedding);
                            }
                        }
                    }
                }
            finally
                {
                // Close input tensors
                if (inputs != null)
                    {
                    inputs.values().forEach(OnnxTensor::close);
                    }
                }

            return embeddings;
            }
        catch (Exception e)
            {
            throw new RuntimeException("Failed to generate embeddings", e);
            }
        }

    /**
     * Creates input tensors for the ONNX model from encoded tokens.
     * 
     * @param encodedResults  the encoded token results
     * @param batchSize       the batch size
     * @param maxLength       the maximum sequence length
     *
     * @return a map of input tensors for the model
     * @throws OrtException  if there's an error creating tensors
     */
    private Map<String, OnnxTensor> createInputs(List<Encoding> encodedResults, int batchSize, int maxLength)
            throws OrtException
        {
        long[] shape = new long[] {batchSize, maxLength};
        LongBuffer inputIds      = LongBuffer.allocate(batchSize * maxLength);
        LongBuffer attentionMask = LongBuffer.allocate(batchSize * maxLength);
        LongBuffer tokenTypeIds  = LongBuffer.allocate(batchSize * maxLength);

        for (int i = 0; i < batchSize; i++)
            {
            long[] ids     = encodedResults.get(i).getIds();
            long[] typeIds = encodedResults.get(i).getTypeIds();

            int seqLength = Math.min(ids.length, maxLength);
            inputIds.put(i * maxLength, ids);
            tokenTypeIds.put(i * maxLength, typeIds);

            for (int j = 0; j < seqLength; j++)
                {
                attentionMask.put(i * maxLength + j, 1);
                }
            }

        OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, inputIds, shape);
        OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(env, attentionMask, shape);

        Map<String, OnnxTensor> mapInputs = new HashMap<>();
        mapInputs.put("input_ids", inputIdsTensor);
        mapInputs.put("attention_mask", attentionMaskTensor);
        if (session.getInputNames().contains("token_type_ids"))
            {
            mapInputs.put("token_type_ids", OnnxTensor.createTensor(env, tokenTypeIds, shape));
            }

        return mapInputs;
        }

    /**
     * Extracts the output tensor from the model result.
     * 
     * @param result  the ONNX model result
     *
     * @return the output tensor as a FloatBuffer
     */
    private FloatBuffer extractOutput(OrtSession.Result result)
        {
        OnnxValue value = result.get(0);
        if (value instanceof OnnxTensor tensor)
            {
            return tensor.getFloatBuffer();
            }
        throw new IllegalStateException("Unexpected output type: " + value.getClass());
        }

    /**
     * Performs mean pooling on token embeddings and normalizes the result.
     * 
     * @param tokenEmbeddings  the token embeddings
     * @param attentionMask    the attention mask
     * @param maxLength        the maximum sequence length
     * @param embedding        the output embedding array
     */
    private void meanPoolingAndNormalize(FloatBuffer tokenEmbeddings, LongBuffer attentionMask, int maxLength, float[] embedding)
        {
        float count = 0f;
        Arrays.fill(embedding, 0f);

        int len = embedding.length;
        for (int j = 0; j < maxLength; j++)
            {
            if (attentionMask.get(j) != 0)
                {
                FloatBuffer token = tokenEmbeddings.slice(j * len, len);
                for (int k = 0; k < len; k++)
                    {
                    embedding[k] += token.get(k);
                    }
                count++;
                }
            }

        if (count > 0f)
            {
            float invCount = 1.0f / count;
            float normSquared = 0f;
            for (int k = 0; k < len; k++)
                {
                embedding[k] *= invCount;
                normSquared += embedding[k] * embedding[k];
                }
            if (normSquared > 0f)
                {
                float invNorm = (float) (1.0 / Math.sqrt(normSquared));
                for (int k = 0; k < len; k++)
                    {
                    embedding[k] *= invNorm;
                    }
                }
            }
        }

    /**
     * Performs CLS pooling on token embeddings and normalizes the result.
     * 
     * @param tokenEmbeddings  the token embeddings
     * @param embedding        the output embedding array
     */
    private void clsPoolingAndNormalize(FloatBuffer tokenEmbeddings, float[] embedding)
        {
        tokenEmbeddings.slice(0, embedding.length).put(embedding);
        float normSquared = 0f;
        for (float v : embedding)
            {
            normSquared += v * v;
            }
        if (normSquared > 0f)
            {
            float invNorm = (float) (1.0 / Math.sqrt(normSquared));
            for (int i = 0; i < embedding.length; i++)
                {
                embedding[i] *= invNorm;
                }
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
            env.close();
            }
        catch (OrtException e)
            {
            throw new RuntimeException("Failed to close ONNX resources", e);
            }
        }

    /**
     * Reads a file into a direct ByteBuffer for optimal performance.
     * 
     * @param path  the path to the file
     *
     * @return the file contents as a direct ByteBuffer
     * @throws IOException  if there's an error reading the file
     */
    public static ByteBuffer readFileToDirectByteBuffer(Path path)
            throws IOException
        {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ))
            {
            long fileSize = channel.size();
            if (fileSize > Integer.MAX_VALUE)
                {
                throw new IOException("File too large to map into ByteBuffer");
                }

            ByteBuffer buffer = ByteBuffer.allocateDirect((int) fileSize);
            while (buffer.hasRemaining())
                {
                channel.read(buffer);
                }
            buffer.flip(); // prepare for reading
            return buffer;
            }
        }

    /**
     * Maps a file to memory using memory-mapped I/O for zero-copy access.
     * 
     * @param path  the path to the file
     *
     * @return the memory-mapped file buffer
     * @throws IOException  if there's an error mapping the file
     */
    public static MappedByteBuffer mapFileToMemory(Path path) throws IOException
        {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ))
            {
            long fileSize = channel.size();
            if (fileSize > Integer.MAX_VALUE)
                {
                throw new IOException("File too large to map into memory (max ~2GB per buffer)");
                }

            return channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
            }
        }
    }
