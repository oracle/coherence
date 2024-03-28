/*
 * Copyright (c) 2021, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.aggregations;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.guides.aggregations.model.Document;
import com.tangosol.util.InvocableMap;

/**
 * Streaming aggregator that will count the number of times a set of words
 * occurs in a {@link com.tangosol.net.NamedMap} containing {@link Document} objects.
 */
// tag::class[]
public class WordCount<K extends String, V extends Document>
        implements InvocableMap.StreamingAggregator<K, V, Map<String, Integer>, Map<String, Integer>>, // <1>
        Serializable { // <2>
// end::class[]

    /**
     * Words to search for.
     */
    private Set<String> setWords;

    /**
     * Map of words and counts of occurrences.
     */
    private Map<String, Integer> mapResults = new HashMap<>();

    // tag::constructor[]
    /**
     * Constructs a {@link WordCount}.
     *
     * @param setWords  {@link Set} of words to search for
     */
    public WordCount(Set<String> setWords) {
        this.setWords = setWords;
    }
    // end::constructor[]

    /**
     * Required for serialization.
     */
    public WordCount() {
    }
    
    // tag::supply[]
    @Override
    public InvocableMap.StreamingAggregator<K, V, Map<String, Integer>, Map<String, Integer>> supply() {
        return new WordCount<>(setWords);
    }
    // end::supply[]

    // tag::accumulate[]
    @Override
    public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry) {
        Document document = entry.getValue();

        for (String word : setWords) {
            // count how many times the word exists in the documents and accumulate
            int count = document.getContents().split("\\b" + word + "\\b", -1).length - 1;  // <1>
            this.mapResults.compute(word, (k, v) -> v == null ? count : v + count);  // <2>
        }

        return true;
    }
    // end::accumulate[]

    // tag::getPartialResult[]
    @Override
    public Map<String, Integer> getPartialResult() {
        Logger.info("getPartialResult: " + mapResults);
        return mapResults;
    }
    // end::getPartialResult[]

    // tag::combine[]
    @Override
    public boolean combine(Map<String, Integer> mapPartialResult) {
        Logger.info("combine: Received partial result " + mapPartialResult);
        // combine the results passed in with the current set of results.
        if (!mapPartialResult.isEmpty()) {
            mapPartialResult.forEach((k, v) -> mapResults.compute(k, (key, value) -> value == null ? v : value + v));
        }
        return true;
    }
    // end::combine[]

    // tag::finalizeResult[]
    @Override
    public Map<String, Integer> finalizeResult() {
        return mapResults;
    }
    // end::finalizeResult[]

    // tag::characteristics[]
    @Override
    public int characteristics() {
        return PARALLEL | PRESENT_ONLY;
    }
    // end::characteristics[]
}
