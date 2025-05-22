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
import com.tangosol.util.ValueExtractor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.search.Query;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.LogByteSizeMergePolicy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the {@link LuceneIndex} class.
 *
 * @author Aleks Seovic  2025.05.17
 */
public class LuceneIndexIT
    {
    private static Coherence coherence;
    private static NamedMap<String, DocumentChunk> documents;
    private static final ValueExtractor<DocumentChunk, String> TEXT = ValueExtractor.of(DocumentChunk::text);
    private static final LuceneQueryParser queryParser = LuceneQueryParser.create(TEXT);

    @SuppressWarnings("resource")
    @BeforeAll
    static void setupClass()
        {
        System.setProperty("coherence.cluster", "LuceneIndexIT");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.serializer", "pof");

        coherence = Coherence.clusterMember().start().join();

        Session session = coherence.getSession();
        documents = session.getMap("documents");
        documents.addIndex(new LuceneIndex<>(TEXT));
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

    private Map<String, DocumentChunk> findDocuments(Query query)
        {
        var results = documents.aggregate(new LuceneSearch<>(TEXT, query, 5));

        for (var result : results)
            {
            System.out.printf("\n%.3f: id=%s, text=%s", result.getDistance(), result.getKey(), result.getValue().text());
            }

        return results.stream()
                .collect(Collectors.toMap(QueryResult::getKey, QueryResult::getValue));

        }

    @Test
    void shouldIndexAndSearchDocuments()
        {
        // Add test documents
        documents.put("doc1", new DocumentChunk("This is a test document about machine learning"));
        documents.put("doc2", new DocumentChunk("Another document about artificial intelligence"));
        documents.put("doc3", new DocumentChunk("Document discussing deep learning and AI"));

        // Search for "machine learning"
        Query query = queryParser.parse("machine learning");
        Map<String, DocumentChunk> results = findDocuments(query);

        // Should find doc1 and doc3
        assertEquals(2, results.size());
        assertTrue(results.containsKey("doc1"));
        assertTrue(results.containsKey("doc3"));

        // Search for "artificial intelligence"
        query = queryParser.parse("artificial intelligence");
        results = findDocuments(query);

        // Should find doc2
        assertEquals(1, results.size());
        assertTrue(results.containsKey("doc2"));
        }

    @Test
    void shouldUpdateDocuments()
        {
        // Add initial document
        documents.put("doc1", new DocumentChunk("Initial document about artificial intelligence"));

        // Verify initial content is searchable
        Query query = queryParser.parse("artificial intelligence");
        Map<String, DocumentChunk> results = findDocuments(query);
        assertEquals(1, results.size());
        assertTrue(results.containsKey("doc1"));

        // Update document
        documents.put("doc1", new DocumentChunk("Updated document about machine learning"));

        // Verify old content is not searchable
        results = findDocuments(query);
        assertTrue(results.isEmpty());

        // Verify new content is searchable
        query = queryParser.parse("machine learning");
        results = findDocuments(query);
        assertEquals(1, results.size());
        assertTrue(results.containsKey("doc1"));
        }

    @Test
    void shouldDeleteDocuments()
        {
        // Add test document
        documents.put("doc1", new DocumentChunk("Document about machine learning"));

        // Verify document is searchable
        Query query = queryParser.parse("machine learning");
        Map<String, DocumentChunk> results = findDocuments(query);
        assertEquals(1, results.size());

        // Delete document
        documents.remove("doc1");

        // Verify document is no longer searchable
        results = findDocuments(query);
        assertTrue(results.isEmpty());
        }

    @Test
    void shouldHandleMultipleUpdates()
        {
        List<String> updates = List.of("person", "woman", "man", "camera", "TV");

        // Perform multiple updates
        for (String update : updates)
            {
            documents.put("doc1", new DocumentChunk(update));
            }

        // Verify only the latest version is searchable
        Query query = queryParser.parse("TV");
        Map<String, DocumentChunk> results = findDocuments(query);
        assertEquals(1, results.size());
        assertEquals("TV", results.get("doc1").text());

        // Verify old versions are not searchable
        for (int i = 0; i < 4; i++)
            {
            query = queryParser.parse(updates.get(i));
            results = findDocuments(query);
            assertTrue(results.isEmpty());
            }
        }

    @Test
    void shouldHandleConcurrentOperations()
        {
        // Add multiple documents concurrently
        Set<String> keys = Set.of("doc1", "doc2", "doc3", "doc4", "doc5");
        keys.parallelStream().forEach(key ->
            documents.put(key, new DocumentChunk("Document " + key + " about machine learning")));

        // Verify all documents are searchable
        Query query = queryParser.parse("machine learning");
        Map<String, DocumentChunk> results = findDocuments(query);
        assertEquals(5, results.size());

        // Update documents concurrently
        keys.parallelStream().forEach(key ->
            documents.put(key, new DocumentChunk("Updated " + key + " about artificial intelligence")));

        // Verify updated content is searchable
        query = queryParser.parse("artificial intelligence");
        results = findDocuments(query);
        assertEquals(5, results.size());

        // Delete documents concurrently
        keys.parallelStream().forEach(documents::remove);

        // Verify all documents are removed from index
        query = queryParser.parse("artificial intelligence");
        results = findDocuments(query);
        assertTrue(results.isEmpty());
        }

    @Test
    void shouldUseCustomAnalyzer()
        {
        // Create new map with French analyzer
        NamedMap<String, DocumentChunk> customDocs = coherence.getSession().getMap("custom-analyzer-docs");
        customDocs.addIndex(new LuceneIndex<>(TEXT)
            .analyzer(FrenchAnalyzer::new));

        // Add test documents with French text
        customDocs.put("doc1", new DocumentChunk("Le chat noir est sur la table"));
        customDocs.put("doc2", new DocumentChunk("Le chien blanc court dans le jardin"));

        // Verify documents are searchable (should find both due to French stemming)
        Query query = queryParser.parse("chien");
        List<QueryResult<String, DocumentChunk>> results = 
            customDocs.aggregate(new LuceneSearch<>(TEXT, query, 10));
        assertEquals(1, results.size());
        }

    @Test
    void shouldUseCustomSearcherFactory()
        {
        // Create new map with custom searcher factory that wraps searchers
        NamedMap<String, DocumentChunk> customDocs = coherence.getSession().getMap("custom-searcher-docs");
        customDocs.addIndex(new LuceneIndex<>(TEXT)
            .searcher((cur, prev) ->
                    {
                    IndexSearcher searcher = new IndexSearcher(cur);
                    searcher.setSimilarity(new BM25Similarity(1.2f, 0.75f));
                    return searcher;
                    }));

        // Add test documents
        customDocs.put("doc1", new DocumentChunk("Document with custom searcher factory"));
        customDocs.put("doc2", new DocumentChunk("Another document with custom searcher"));

        // Verify documents are searchable with custom similarity
        Query query = queryParser.parse("custom searcher");
        List<QueryResult<String, DocumentChunk>> results = 
            customDocs.aggregate(new LuceneSearch<>(TEXT, query, 10));
        assertEquals(2, results.size());
        }

    @Test
    void shouldUseCustomDirectory()
        {
        // Create new map with custom directory
        NamedMap<String, DocumentChunk> customDirDocs = coherence.getSession().getMap("custom-dir-docs");
        customDirDocs.addIndex(new LuceneIndex<>(TEXT)
            .directory(partId ->
                           {
                           try
                               {
                               return FSDirectory.open(Path.of(".lucene", "test-" + partId));
                               }
                           catch (IOException e)
                               {
                               throw new RuntimeException(e);
                               }
                           }));

        // Add test documents
        customDirDocs.put("doc1", new DocumentChunk("Document with custom directory"));
        customDirDocs.put("doc2", new DocumentChunk("Another document with custom directory"));

        // Verify documents are searchable
        Query query = queryParser.parse("custom directory");
        List<QueryResult<String, DocumentChunk>> results = 
            customDirDocs.aggregate(new LuceneSearch<>(TEXT, query, 10));
        assertEquals(2, results.size());
        }

    @Test
    void shouldConfigureIndexWriter()
        {
        // Create new map with custom index writer configuration
        NamedMap<String, DocumentChunk> customDocs = coherence.getSession().getMap("custom-writer-docs");
        customDocs.addIndex(new LuceneIndex<>(TEXT)
            .configureIndexWriter(config -> {
                config.setRAMBufferSizeMB(32.0);
                LogByteSizeMergePolicy policy = new LogByteSizeMergePolicy();
                policy.setMinMergeMB(16.0);
                policy.setMaxMergeMB(128.0);
                policy.setMergeFactor(5);
                config.setMergePolicy(policy);
                config.setUseCompoundFile(true);
            }));

        // Add test documents
        customDocs.put("doc1", new DocumentChunk("First document with custom writer config"));
        customDocs.put("doc2", new DocumentChunk("Second document for testing"));
        customDocs.put("doc3", new DocumentChunk("Third document with more content"));

        // Verify documents are searchable
        Query query = queryParser.parse("document");
        List<QueryResult<String, DocumentChunk>> results = 
            customDocs.aggregate(new LuceneSearch<>(TEXT, query, 10));
        assertEquals(3, results.size(), "Should find all documents");

        // Test more specific search
        query = queryParser.parse("custom writer");
        results = customDocs.aggregate(new LuceneSearch<>(TEXT, query, 10));
        assertEquals(1, results.size(), "Should find document with 'custom writer'");
        }

    @Test
    void shouldUseCombinedConfiguration()
        {
        // Create new map with combined configuration
        NamedMap<String, DocumentChunk> combinedDocs = coherence.getSession().getMap("combined-config-docs");
        combinedDocs.addIndex(new LuceneIndex<>(TEXT)
            .analyzer(FrenchAnalyzer::new)
            .directory(partId ->
                           {
                           try
                               {
                               return FSDirectory.open(Path.of(".lucene", "combined-" + partId));
                               }
                           catch (IOException e)
                               {
                               throw new RuntimeException(e);
                               }
                           }));

        // Add test documents
        combinedDocs.put("doc1", new DocumentChunk("Le chat noir est sur la table"));
        combinedDocs.put("doc2", new DocumentChunk("Le chien blanc court dans le jardin"));

        // Verify documents are searchable
        Query query = queryParser.parse("chien");
        List<QueryResult<String, DocumentChunk>> results = 
            combinedDocs.aggregate(new LuceneSearch<>(TEXT, query, 10));
        assertEquals(1, results.size());
        }
    }
