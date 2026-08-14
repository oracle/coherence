/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model;

import com.oracle.coherence.common.io.Files;

import com.github.tomakehurst.wiremock.WireMockServer;

import dev.langchain4j.data.embedding.Embedding;

import io.helidon.webclient.api.WebClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Path;

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

public class LocalOnnxEmbeddingModelTest
    {
    @BeforeAll
    public static void setup()
        {
        s_huggingFace = new WireMockServer(wireMockConfig().dynamicPort());
        s_huggingFace.start();

        s_huggingFace.stubFor(get(urlEqualTo(POOLING_CONFIG_URI))
                .willReturn(aResponse().withStatus(200).withBody(POOLING_CONFIG)));
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
        ModelName name = new ModelName("TaylorAI/bge-micro");
        deleteIfExists(LocalOnnxEmbeddingModel.pathTo(name, "config.json"));
        deleteIfExists(LocalOnnxEmbeddingModel.pathTo(name, "model.onnx"));
        deleteIfExists(LocalOnnxEmbeddingModel.pathTo(name, "tokenizer.json"));
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
        PoolingConfig config = LocalOnnxEmbeddingModel.getPoolingConfig(name, s_client);
        try (InputStream model = LocalOnnxEmbeddingModel.getModelStream(name, s_client);
             InputStream tokenizer = LocalOnnxEmbeddingModel.getTokenizerStream(name, s_client))
            {
            assertThat(exists(LocalOnnxEmbeddingModel.pathTo(name, "config.json")), is(true));
            assertThat(exists(LocalOnnxEmbeddingModel.pathTo(name, "model.onnx")), is(true));
            assertThat(exists(LocalOnnxEmbeddingModel.pathTo(name, "tokenizer.json")), is(true));

            assertThat(config, is(new PoolingConfig(3, false, true)));
            assertThat(new String(model.readAllBytes(), StandardCharsets.UTF_8), is(MODEL_CONTENT));
            assertThat(new String(tokenizer.readAllBytes(), StandardCharsets.UTF_8), is(TOKENIZER_CONTENT));
            }

        s_huggingFace.verify(getRequestedFor(urlEqualTo(POOLING_CONFIG_URI)));
        s_huggingFace.verify(getRequestedFor(urlEqualTo(MODEL_URI)));
        s_huggingFace.verify(getRequestedFor(urlEqualTo(TOKENIZER_URI)));
        }

    // ---- constants ------------------------------------------------------

    private static final String POOLING_CONFIG_URI = "/TaylorAI/bge-micro/resolve/main/1_Pooling/config.json";

    private static final String MODEL_URI = "/TaylorAI/bge-micro/resolve/main/onnx/model.onnx";

    private static final String TOKENIZER_URI = "/TaylorAI/bge-micro/resolve/main/tokenizer.json";

    private static final String POOLING_CONFIG = """
            {"word_embedding_dimension":3,"pooling_mode_cls_token":false,"pooling_mode_mean_tokens":true}
            """;

    private static final String MODEL_CONTENT = "mock embedding model";

    private static final String TOKENIZER_CONTENT = "mock embedding tokenizer";

    // ---- data members ---------------------------------------------------

    private static WireMockServer s_huggingFace;

    private static WebClient s_client;
    }
