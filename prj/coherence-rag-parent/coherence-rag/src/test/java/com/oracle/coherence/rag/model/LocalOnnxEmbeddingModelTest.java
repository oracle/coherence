/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model;

import com.oracle.coherence.testing.http.UseProxy;

import dev.langchain4j.data.embedding.Embedding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

@UseProxy
public class LocalOnnxEmbeddingModelTest
    {
    @AfterAll
    public static void cleanup() throws IOException
        {
        ModelName name = new ModelName("TaylorAI/bge-micro");
        Files.delete(LocalOnnxEmbeddingModel.pathTo(name, "config.json"));
        Files.delete(LocalOnnxEmbeddingModel.pathTo(name, "model.onnx"));
        Files.delete(LocalOnnxEmbeddingModel.pathTo(name, "tokenizer.json"));
        Files.delete(LocalOnnxEmbeddingModel.pathTo(name));
        Files.delete(Path.of("models", "TaylorAI"));
        }

    @Test
    public void testDefaultModel()
        {
        ModelName name = new ModelName("-/all-MiniLM-L6-v2");
        LocalOnnxEmbeddingModel model = LocalOnnxEmbeddingModel.createDefault(name);

        Embedding embedding = model.embed("Create a vector").content();
        assertThat(embedding.dimension(), is(model.dimension()));
        assertThat(model.name(), is(name));
        }

    @Test
    public void testLocalModelDownload()
        {
        ModelName name = new ModelName("TaylorAI/bge-micro");
        LocalOnnxEmbeddingModel model = LocalOnnxEmbeddingModel.create(name);

        assertThat(Files.exists(LocalOnnxEmbeddingModel.pathTo(name, "config.json")), is(true));
        assertThat(Files.exists(LocalOnnxEmbeddingModel.pathTo(name, "model.onnx")), is(true));
        assertThat(Files.exists(LocalOnnxEmbeddingModel.pathTo(name, "tokenizer.json")), is(true));

        Embedding embedding = model.embed("Create a vector").content();
        assertThat(embedding.dimension(), is(model.dimension()));
        assertThat(model.name(), is(name));
        }
    }
