/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model;

import com.oracle.coherence.common.io.Files;
import com.oracle.coherence.testing.http.UseProxy;

import dev.langchain4j.data.embedding.Embedding;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Path;

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
        try (LocalOnnxEmbeddingModel model = LocalOnnxEmbeddingModel.create(name))
            {
            assertThat(exists(LocalOnnxEmbeddingModel.pathTo(name, "config.json")), is(true));
            assertThat(exists(LocalOnnxEmbeddingModel.pathTo(name, "model.onnx")), is(true));
            assertThat(exists(LocalOnnxEmbeddingModel.pathTo(name, "tokenizer.json")), is(true));

            Embedding embedding = model.embed("Create a vector").content();
            assertThat(embedding.dimension(), is(model.dimension()));
            assertThat(model.name(), is(name));
            }
        }
    }
