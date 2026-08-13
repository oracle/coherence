/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model;

import com.oracle.coherence.common.io.Files;

import com.github.tomakehurst.wiremock.WireMockServer;

import dev.langchain4j.data.segment.TextSegment;

import io.helidon.webclient.api.WebClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Path;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public class LocalOnnxScoringModelTest
    {
    @BeforeAll
    public static void setup()
        {
        s_huggingFace = new WireMockServer(wireMockConfig().dynamicPort());
        s_huggingFace.start();

        s_huggingFace.stubFor(get(urlEqualTo(MODEL_URI))
                .willReturn(aResponse().withStatus(200).withBody(MODEL_CONTENT)));
        s_huggingFace.stubFor(get(urlEqualTo(TOKENIZER_URI))
                .willReturn(aResponse().withStatus(200).withBody(TOKENIZER_CONTENT)));

        s_client = WebClient.builder()
                .baseUri(s_huggingFace.baseUrl() + "/")
                .build();
        }

    @BeforeEach
    public void deleteDownloadedModel() throws Exception
        {
        ModelName name = new ModelName("Xenova/ms-marco-MiniLM-L-6-v2");
        deleteIfExists(LocalOnnxScoringModel.pathTo(name, "model.onnx"));
        deleteIfExists(LocalOnnxScoringModel.pathTo(name, "tokenizer.json"));
        }

    @AfterAll
    public static void cleanup() throws Exception
        {
        try
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
        finally
            {
            if (s_huggingFace != null && s_huggingFace.isRunning())
                {
                s_huggingFace.stop();
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
        try (InputStream model = LocalOnnxScoringModel.getModelStream(name, s_client);
             InputStream tokenizer = LocalOnnxScoringModel.getTokenizerStream(name, s_client))
            {
            assertThat(exists(LocalOnnxScoringModel.pathTo(name, "model.onnx")), is(true));
            assertThat(exists(LocalOnnxScoringModel.pathTo(name, "tokenizer.json")), is(true));

            assertThat(new String(model.readAllBytes(), StandardCharsets.UTF_8), is(MODEL_CONTENT));
            assertThat(new String(tokenizer.readAllBytes(), StandardCharsets.UTF_8), is(TOKENIZER_CONTENT));
            }

        s_huggingFace.verify(getRequestedFor(urlEqualTo(MODEL_URI)));
        s_huggingFace.verify(getRequestedFor(urlEqualTo(TOKENIZER_URI)));
        }

    // ---- constants ------------------------------------------------------

    private static final String MODEL_URI = "/Xenova/ms-marco-MiniLM-L-6-v2/resolve/main/onnx/model.onnx";

    private static final String TOKENIZER_URI = "/Xenova/ms-marco-MiniLM-L-6-v2/resolve/main/tokenizer.json";

    private static final String MODEL_CONTENT = "mock scoring model";

    private static final String TOKENIZER_CONTENT = "mock scoring tokenizer";

    // ---- data members ---------------------------------------------------

    private static WireMockServer s_huggingFace;

    private static WebClient s_client;
    }
