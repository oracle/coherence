/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.aggregations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.oracle.coherence.guides.aggregations.model.Document;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;

/**
 * Class to show custom aggregation example.
 *
 * @author Tim Middleton  2022.03.31
 */
public class CustomAggregationExample {

    /**
     * Random instance with predictable seed.
     */
    protected static final Random RANDOM = new Random(12312383838L);

    /**
     * Random words to add to a document.
     */
    private static final String[] WORDS = new String[] {
            "this", "word", "thanks", "welcome", "fifteen",
            "out", "car", "dog", "cat", "free", "fire",
            "TV", "radio", "why", "and", "then", "freedom", "tv",
            "coherence", "not", "able", "this is not", "agree",
            "fight", "launch", "wind", "sky", "green", "trees",
            "ample", "parade", "trumpet", "guy", "half", "this", "this",
            "is", "help", "appreciate", "this", "this", "ok"
    };

    private static final String STORAGE_ENABLED = "coherence.distributed.localstorage";

    /**
     * Maximum documents to add.
     */
    private static final int MAX_DOCUMENTS = 2000;

    /**
     * {@link NamedMap} to store documents.
     */
    private NamedMap<String, Document> documents;

    /**
     * Entry point to run from IDE.
     *
     * @param args arguments
     */
    public static void main(String[] args) {
        // if storage-enabled is not set then set it
        if (getStorageEnabled() == null) {
            setStorageEnabled(true);
        }

        CustomAggregationExample example = new CustomAggregationExample();
        example.populate();
        example.runExample();
    }

    /**
     * Constructor.
     */
    public CustomAggregationExample() {
        Coherence coherence = Coherence.clusterMember();
        coherence.start().join();
        documents = coherence.getSession().getMap("documents");
    }

    /**
     * Populate the cache with data.
     */
    public void populate() {
        documents.clear();
        documents.putAll(populate(MAX_DOCUMENTS));
    }

    // tag::runExample[]
    /**
     * Run the example.
     */
    public void runExample() {
        System.out.println("Documents added " + documents.size());

        // choose up to 5 random words from the list to search for
        Set<String> setWords = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            setWords.add(getRandomValue(WORDS));
        }

        System.out.println("Running against the following words: " + setWords);
        Map<String, Integer> results = documents.aggregate(new WordCount<>(setWords)); // <1>

        results.forEach((k, v) -> System.out.println("Word " + k + ", number of occurrences: " + v));
    }
    // end::runExample[]

    /**
     * Generate a number of {@link Document}s to add to the {@link NamedMap}.
     *
     * @param count number of documents to create
     *
     * @return the {@link Map} of {@link Document}s
     */
    private static Map<String, Document> populate(int count) {
        Map<String, Document> map     = new HashMap<>();
        Random                random  = new Random();
        int                   wordLen = WORDS.length;

        for (int i = 0; i < count; i++) {
            StringBuilder sb         = new StringBuilder();
            int           wordsToAdd = random.nextInt(15) + 1;

            for (int j = 0; j < wordsToAdd; j++) {
                sb.append(WORDS[random.nextInt(wordLen)]).append(" ");
            }
            Document document = new Document("ID-" + i, sb.toString().trim());
            map.put(document.getId(), document);
        }

        return map;
    }

    /**
     * Return a random value from an array.
     *
     * @param array array to return from
     *
     * @return a random value
     */
    protected String getRandomValue(String[] array) {
        int len = array.length;
        return array[RANDOM.nextInt(len)];
    }
    
    /**
     * Return true if the member is storage-enabled.
     *
     * @return rue if the member is storage-enabled
     */
    protected static String getStorageEnabled() {
        return System.getProperty(STORAGE_ENABLED);
    }

    /**
     * Set if the member is storage-enabled.
     *
     * @param storageEnabled if the member is storage-enabled
     */
    protected static void setStorageEnabled(boolean storageEnabled) {
        System.setProperty(STORAGE_ENABLED, Boolean.toString(storageEnabled));
    }
}
