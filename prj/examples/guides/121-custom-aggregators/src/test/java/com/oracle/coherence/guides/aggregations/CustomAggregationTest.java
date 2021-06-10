/*
 * Copyright (c) 2000, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.aggregations;

import com.oracle.coherence.guides.aggregations.model.Document;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

/**
 * Tests for {@link CustomAggregationExample}.
 *
 * @author Tim Middleton 2021.04.01
 */
public class CustomAggregationTest {
    @Test
    public void testWordCount() {
        CustomAggregationExample.setStorageEnabled(true);
        Coherence coherence = Coherence.clusterMember();
        coherence.start().join();
        NamedMap<String, Document> documents = coherence.getSession().getMap("documents");

        Document doc1 = new Document("doc1", "this is a test");
        Document doc2 = new Document("doc2", "this is a further test");
        Document doc3 = new Document("doc3", "welcome to the real WORLD");
        documents.put(doc1.getId(), doc1);
        documents.put(doc2.getId(), doc2);
        documents.put(doc3.getId(), doc3);

        Set<String> setWords = new HashSet<>();
        setWords.add("this");

        Map<String, Integer> results = documents.aggregate(new WordCount<>(setWords));
        assertThat(results.size(), is(1));
        assertThat(results.get("this"), is(2));

        setWords.add("welcome");
        results = documents.aggregate(new WordCount<>(setWords));
        assertThat(results.size(), is(2));
        assertThat(results.get("this"), is(2));
        assertThat(results.get("welcome"), is(1));

        setWords.clear();
        setWords.add("nothing");
        setWords.add("WORLD");
        results = documents.aggregate(new WordCount<>(setWords));
        assertThat(results.size(), is(2));
        assertThat(results.get("nothing"), is(0));
        assertThat(results.get("WORLD"), is(1));
    }

    @AfterAll
    public static void shutdownCoherence() {
        Coherence coherence = Coherence.getInstance();
        if (coherence != null) {
            coherence.close();
        }
    }
}
