/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model;

import com.oracle.coherence.common.io.Files;
import com.oracle.coherence.testing.http.UseProxy;

import dev.langchain4j.data.segment.TextSegment;

import java.io.IOException;

import java.nio.file.Path;

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
    public static void cleanup() throws IOException
        {
        Files.deleteDirectory(Path.of("models", "Xenova"));
        }

    @Test
    public void testDefaultModel()
        {
        ModelName name = new ModelName("-/ms-marco-TinyBERT-L-2-v2");
        LocalOnnxScoringModel model = LocalOnnxScoringModel.createDefault(name);

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

    @Test
    public void testLocalModelDownload()
        {
        ModelName name = new ModelName("Xenova/ms-marco-MiniLM-L-6-v2");
        LocalOnnxScoringModel model = LocalOnnxScoringModel.create(name);

        assertThat(exists(LocalOnnxScoringModel.pathTo(name, "model.onnx")), is(true));
        assertThat(exists(LocalOnnxScoringModel.pathTo(name, "tokenizer.json")), is(true));

        String question = "What is panda?";
        List<TextSegment> answers = List.of(
                TextSegment.from("Dolphins are mammals, not fish."),
                TextSegment.from("The giant panda (Ailuropoda melanoleuca), sometimes called a panda bear or simply panda, is a bear species endemic to China."));

        double lowScore   = model.score(answers.get(0), question).content();
        double hightScore = model.score(answers.get(1), question).content();

        System.out.println(question);
        System.out.println();
        System.out.printf("%.5f: %s\n", lowScore, answers.get(0).text());
        System.out.printf("%.5f: %s\n", hightScore, answers.get(1).text());

        assertThat(model.name(), is(name));
        assertThat(hightScore, is(greaterThan(lowScore)));
        }
    }
