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

import dev.langchain4j.data.segment.TextSegment;

import java.io.IOException;

import java.net.SocketTimeoutException;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Path;

import java.time.Duration;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static java.nio.file.Files.exists;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

@UseProxy
public class LocalOnnxScoringModelTest
    {
    @AfterAll
    public static void cleanup() throws Exception
        {
        for (int i = 0; i < 5; i++)
            {
            try
                {
                Files.deleteDirectory(Path.of("models", "Xenova"));
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
        ModelName name = new ModelName("-/ms-marco-TinyBERT-L-2-v2");
        try (LocalOnnxScoringModel model = LocalOnnxScoringModel.createDefault(name))
            {
            String question = "How many people live in Berlin?";
            List<TextSegment> answers = List.of(
                    TextSegment.from("New York City is famous for the Metropolitan Museum of Art."),
                    TextSegment.from("Berlin had a population of 3,520,031 registered inhabitants in an area of 891.82 square kilometers."));

            List<Double> scores = model.scoreAll(answers, question).content();

            System.out.println(question);
            System.out.println();

            for (int i = 0; i < scores.size(); i++)
                {
                System.out.printf("%.5f: %s\n", scores.get(i), answers.get(i).text());
                }

            assertThat(model.name(), is(name));
            assertThat(scores.get(1), is(greaterThan(scores.get(0))));
            }
        }

    @Test
    public void testLocalModelDownload() throws Exception
        {
        ModelName name = new ModelName("Xenova/ms-marco-MiniLM-L-6-v2");
        try (LocalOnnxScoringModel model = createWithDownloadRetry(name))
            {
            assertThat(exists(LocalOnnxScoringModel.pathTo(name, "model.onnx")), is(true));
            assertThat(exists(LocalOnnxScoringModel.pathTo(name, "tokenizer.json")), is(true));

            String question = "What is panda?";
            List<TextSegment> answers = List.of(
                    TextSegment.from("Dolphins are mammals, not fish."),
                    TextSegment.from("The giant panda (Ailuropoda melanoleuca), sometimes called a panda bear or simply panda, is a bear species endemic to China."));

            double lowScore = model.score(answers.get(0), question).content();
            double hightScore = model.score(answers.get(1), question).content();

            System.out.println(question);
            System.out.println();
            System.out.printf("%.5f: %s\n", lowScore, answers.get(0).text());
            System.out.printf("%.5f: %s\n", hightScore, answers.get(1).text());

            assertThat(model.name(), is(name));
            assertThat(hightScore, is(greaterThan(lowScore)));
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
    private static LocalOnnxScoringModel createWithDownloadRetry(ModelName name) throws Exception
        {
        for (int i = 1; i <= DOWNLOAD_ATTEMPTS; i++)
            {
            try
                {
                return LocalOnnxScoringModel.create(name);
                }
            catch (RuntimeException e)
                {
                if (i == DOWNLOAD_ATTEMPTS || !isRetriableDownloadFailure(e))
                    {
                    throw e;
                    }

                deleteDownloadedFiles(name, e);
                Logger.warn("Failed to download scoring model [%s] after attempt %d of %d; retrying in %d seconds"
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
            java.nio.file.Files.deleteIfExists(LocalOnnxScoringModel.pathTo(name, "model.onnx"));
            java.nio.file.Files.deleteIfExists(LocalOnnxScoringModel.pathTo(name, "tokenizer.json"));
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
