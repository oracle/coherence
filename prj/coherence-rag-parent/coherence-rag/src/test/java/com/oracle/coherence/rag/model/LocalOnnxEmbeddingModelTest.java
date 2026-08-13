/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.io.Files;
import com.oracle.coherence.testing.http.UseProxy;

import dev.langchain4j.data.embedding.Embedding;

import java.io.IOException;

import java.net.SocketTimeoutException;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Path;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static java.nio.file.Files.exists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

@UseProxy
public class LocalOnnxEmbeddingModelTest
    {
    @AfterAll
    public static void cleanup() throws Exception
        {
        for (int i = 0; i < 5; i++)
            {
            try
                {
                Files.deleteDirectory(Path.of("models", "TaylorAI"));
                break;
                }
            catch (DirectoryNotEmptyException e)
                {
                System.gc();
                Thread.sleep(1000L * (1L << i)); // 1s, 2s, 4s, ...
                }
            }
        }

    @Test
    public void testDefaultModel() throws Exception
        {
        ModelName name = new ModelName("-/all-MiniLM-L6-v2");
        try (LocalOnnxEmbeddingModel model = LocalOnnxEmbeddingModel.createDefault(name))
            {
            Embedding embedding = model.embed("Create a vector").content();
            assertThat(embedding.dimension(), is(model.dimension()));
            assertThat(model.name(), is(name));
            }
        }

    @Test
    public void testLocalModelDownload() throws Exception
        {
        ModelName name = new ModelName("TaylorAI/bge-micro");
        try (LocalOnnxEmbeddingModel model = createWithDownloadRetry(name))
            {
            assertThat(exists(LocalOnnxEmbeddingModel.pathTo(name, "config.json")), is(true));
            assertThat(exists(LocalOnnxEmbeddingModel.pathTo(name, "model.onnx")), is(true));
            assertThat(exists(LocalOnnxEmbeddingModel.pathTo(name, "tokenizer.json")), is(true));

            Embedding embedding = model.embed("Create a vector").content();
            assertThat(embedding.dimension(), is(model.dimension()));
            assertThat(model.name(), is(name));
            }
        }

    /**
     * Create a model, retrying transient HuggingFace download failures.
     *
     * @param name  the model name
     *
     * @return the model
     *
     * @throws Exception if creation fails
     */
    private static LocalOnnxEmbeddingModel createWithDownloadRetry(ModelName name) throws Exception
        {
        for (int i = 1; i <= DOWNLOAD_ATTEMPTS; i++)
            {
            try
                {
                return LocalOnnxEmbeddingModel.create(name);
                }
            catch (RuntimeException e)
                {
                if (i == DOWNLOAD_ATTEMPTS || !isRetriableDownloadFailure(e))
                    {
                    throw e;
                    }

                deleteDownloadedFiles(name, e);
                Logger.warn("Failed to download embedding model [%s] after attempt %d of %d; retrying in %d seconds"
                        .formatted(name.fullName(), i, DOWNLOAD_ATTEMPTS, DOWNLOAD_RETRY_DELAY.toSeconds()), e);
                sleepBeforeRetry();
                }
            }

        throw new AssertionError("unreachable");
        }

    /**
     * Return {@code true} if the exception chain reports a transient model
     * download failure.
     *
     * @param thrown  the exception
     *
     * @return {@code true} if the exception is a transient download failure
     */
    private static boolean isRetriableDownloadFailure(Throwable thrown)
        {
        while (thrown != null)
            {
            if (thrown instanceof SocketTimeoutException)
                {
                return true;
                }

            String message = thrown.getMessage();
            if (message != null && message.contains("Failed to download") && message.contains("403"))
                {
                return true;
                }
            if (message != null && message.contains("Read timed out"))
                {
                return true;
                }

            thrown = thrown.getCause();
            }
        return false;
        }

    /**
     * Delete local model files before retrying to avoid reusing a partial
     * download.
     *
     * @param name   the model name
     * @param cause  the retry cause
     */
    private static void deleteDownloadedFiles(ModelName name, RuntimeException cause)
        {
        try
            {
            java.nio.file.Files.deleteIfExists(LocalOnnxEmbeddingModel.pathTo(name, "config.json"));
            java.nio.file.Files.deleteIfExists(LocalOnnxEmbeddingModel.pathTo(name, "model.onnx"));
            java.nio.file.Files.deleteIfExists(LocalOnnxEmbeddingModel.pathTo(name, "tokenizer.json"));
            }
        catch (IOException e)
            {
            cause.addSuppressed(e);
            }
        }

    /**
     * Sleep before retrying a transient download failure.
     *
     * @throws InterruptedException if interrupted
     */
    private static void sleepBeforeRetry() throws InterruptedException
        {
        try
            {
            Thread.sleep(DOWNLOAD_RETRY_DELAY.toMillis());
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw e;
            }
        }

    // ---- constants ------------------------------------------------------

    /**
     * Maximum number of download attempts for transient failures.
     */
    private static final int DOWNLOAD_ATTEMPTS = 5;

    /**
     * Delay between download attempts for transient failures.
     */
    private static final Duration DOWNLOAD_RETRY_DELAY = Duration.ofSeconds(30);
    }
