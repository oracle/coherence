 /*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.lucene;

import com.oracle.coherence.ai.DocumentChunk;
import com.oracle.coherence.ai.QueryResult;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.ValueExtractor;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the {@link LuceneSearch} class.
 *
 * @author Aleks Seovic  2025.05.16
 */
public class LuceneSearchIT
    {
    private static Coherence coherence;

    private static NamedMap<String, DocumentChunk> documents;
    private static final ValueExtractor<DocumentChunk, String> CONTENT = ValueExtractor.of(DocumentChunk::text);
    private static final LuceneQueryParser queryParser = LuceneQueryParser.create(CONTENT);

    @SuppressWarnings("resource")
    @BeforeAll
    static void setupClass()
        {
        System.setProperty("coherence.cluster", "FullTextSearchIT");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.serializer", "pof");

        coherence = Coherence.clusterMember().start().join();

        Session session = coherence.getSession();
        documents = session.getMap("documents");
        documents.addIndex(new LuceneIndex<>(CONTENT).enableInverseMap());
        }

    @AfterAll
    static void cleanupClass()
        {
        if (coherence != null)
            {
            coherence.close();
            }
        }

    @BeforeEach
    void setup()
        {
        documents.clear();
        }

    @Test
    void shouldSearchAndRankResults()
        {
        // Add test documents
        documents.put("doc1", new DocumentChunk("This is a test document about machine learning"));
        documents.put("doc2", new DocumentChunk("Another document about artificial intelligence (AI)"));
        documents.put("doc3", new DocumentChunk("Document discussing machine learning and AI"));

        // Create and execute search
        var query = queryParser.parse("machine learning and AI");
        var results = documents.aggregate(new LuceneSearch<>(CONTENT, query, 10));

        results.forEach(r -> System.out.printf("\n%.3f: key=%s, value=%s", r.getDistance(), r.getKey(), r.getValue().text()));

        // Verify results
        assertNotNull(results);
        assertEquals(3, results.size());

        // Verify scores are normalized (between 0 and 1)
        for (QueryResult<String, DocumentChunk> result : results)
            {
            assertTrue(result.getDistance() >= 0.0f && result.getDistance() <= 1.0f);
            }

        // First result should be doc3 as it contains both "machine learning" and "AI"
        assertEquals("doc3", results.get(0).getKey());
        assertTrue(results.get(0).getDistance() > results.get(1).getDistance());
        }

    @Test
    void shouldApplyFilter()
        {
        // Add test documents
        documents.put("doc1", new DocumentChunk("This is a test document about machine learning"));
        documents.put("doc2", new DocumentChunk("Another document about artificial intelligence"));
        documents.put("doc3", new DocumentChunk("Document discussing machine learning and AI"));

        // Create search query
        var query = queryParser.parse("machine learning");

        // Test with filter that only matches documents ending with "AI"
        Filter<DocumentChunk> filter = Filters.like(CONTENT, "%AI");
        var results = documents.aggregate(filter, new LuceneSearch<>(CONTENT, query, 10).filter(filter));

        results.forEach(r -> System.out.printf("\n%.3f: key=%s, value=%s", r.getDistance(), r.getKey(), r.getValue().text()));

        // Should only return doc3
        assertEquals(1, results.size());
        assertEquals("doc3", results.get(0).getKey());

        // test that the results are the same even if we don't use pre-filtering
        // (which will cause Scanner to be used instead of Advancer)
        results = documents.aggregate(new LuceneSearch<>(CONTENT, query, 10).filter(filter));

        results.forEach(r -> System.out.printf("\n%.3f: key=%s, value=%s", r.getDistance(), r.getKey(), r.getValue().text()));

        // Should only return doc3
        assertEquals(1, results.size());
        assertEquals("doc3", results.get(0).getKey());

        // Test with filter that matches no documents
        filter  = Filters.never();
        results = documents.aggregate(new LuceneSearch<>(CONTENT, query, 10).filter(filter));

        // Should return no results
        assertTrue(results.isEmpty());
        }

    @Test
    void shouldLimitResults()
        {
        // Add test documents
        documents.put("doc1", new DocumentChunk("This is a test document about machine learning"));
        documents.put("doc2", new DocumentChunk("Another document about machine learning"));
        documents.put("doc3", new DocumentChunk("Document discussing machine learning"));
        documents.put("doc4", new DocumentChunk("More about machine learning"));
        documents.put("doc5", new DocumentChunk("Final document about machine learning"));

        // Search with limit of 3 results
        var query   = queryParser.parse("machine learning");
        var results = documents.aggregate(new LuceneSearch<>(CONTENT, query, 3));

        // Should only return top 3 results
        assertEquals(3, results.size());
        }

    @Test
    void shouldHandleEmptyResults()
        {
        // Add test documents
        documents.put("doc1", new DocumentChunk("This is a test document"));
        documents.put("doc2", new DocumentChunk("Another test document"));

        // Search for non-existent terms
        var query = queryParser.parse("nonexistent terms");
        var results = documents.aggregate(new LuceneSearch<>(CONTENT, query, 10));

        // Should return empty list
        assertTrue(results.isEmpty());
        }
    }
