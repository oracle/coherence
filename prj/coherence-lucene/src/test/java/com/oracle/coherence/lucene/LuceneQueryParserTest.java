/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.lucene;

import com.tangosol.util.ValueExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link LuceneQueryParser}.
 */
class LuceneQueryParserTest
    {

    // Minimal Document class for testing
    record Document(String title, String body)
        {
        }

    private static final ValueExtractor<Document, String> TITLE = Document::title;

    private static final ValueExtractor<Document, String> BODY = Document::body;

    @Test
    void testSingleFieldParse()
        {
        LuceneQueryParser parser = LuceneQueryParser.builder()
                .field(TITLE)
                .build();
        Query query = parser.parse("hello world");
        assertThat(query, is(notNullValue()));
        assertThat(query.toString(), containsString("title"));
        }

    @Test
    void testMultiFieldParseWithBoosts()
        {
        LuceneQueryParser parser = LuceneQueryParser.builder()
                .field(TITLE, 2.0f)
                .field(BODY)
                .build();
        Query query = parser.parse("test");
        assertThat(query, is(notNullValue()));
        assertThat(query.toString(), containsString("(title():test)^2.0"));
        assertThat(query.toString(), containsString("(body():test)^1.0"));
        }

    @Test
    void testCustomAnalyzer()
        {
        Analyzer analyzer = new WhitespaceAnalyzer();
        LuceneQueryParser parser = LuceneQueryParser.builder()
                .field(TITLE)
                .analyzer(analyzer)
                .build();
        Query query = parser.parse("foo bar");
        assertThat(query, is(notNullValue()));
        }

    @Test
    void testStopWords()
        {
        LuceneQueryParser parser = LuceneQueryParser.builder()
                .field(TITLE)
                .stopWords(Set.of("the", "and"))
                .build();
        Query query = parser.parse("the quick and brown fox");
        assertThat(query, is(notNullValue()));
        assertThat(query.toString(), not(containsString("the")));
        assertThat(query.toString(), not(containsString("and")));
        }

    @Test
    void testSynonymsMap()
        {
        LuceneQueryParser parser = LuceneQueryParser.builder()
                .field(TITLE)
                .synonyms(Map.of("car", List.of("automobile", "vehicle")))
                .build();
        Query query = parser.parse("car");
        assertThat(query, is(notNullValue()));
        assertThat(query.toString(), containsString(":automobile"));
        assertThat(query.toString(), containsString(":vehicle"));
        }

    @Test
    void testPreprocessor()
        {
        LuceneQueryParser parser = LuceneQueryParser.builder()
                .field(TITLE)
                .analyzer(new WhitespaceAnalyzer())
                .preprocessor(String::toUpperCase)
                .build();
        Query query = parser.parse("foo");
        assertThat(query.toString(), containsString("FOO"));
        }

    @Test
    void testConfigureParser()
        {
        LuceneQueryParser parser = LuceneQueryParser.builder()
                .field(TITLE)
                .configureParser(qp -> qp.setDefaultOperator(QueryParser.Operator.AND))
                .build();
        Query query = parser.parse("foo bar");
        assertThat(query, is(notNullValue()));
        }

    @Test
    void testCustomQueryParser()
        {
        QueryParser custom = new QueryParser("title", new StandardAnalyzer());
        LuceneQueryParser parser = LuceneQueryParser.builder()
                .parser(custom)
                .build();
        Query query = parser.parse("baz");
        assertThat(query, is(notNullValue()));
        }

    @Test
    void testCreateStatic()
        {
        LuceneQueryParser parser = LuceneQueryParser.create(TITLE);
        Query query = parser.parse("foo");
        assertThat(query, is(notNullValue()));
        }

    @Test
    void testErrorIfNoField()
        {
        LuceneQueryParser.Builder builder = LuceneQueryParser.builder();
        assertThrows(IllegalStateException.class, builder::build);
        }

    @Test
    void testErrorIfBothSynonymSources()
        {
        LuceneQueryParser.Builder builder = LuceneQueryParser.builder()
                .field(TITLE)
                .synonyms(Map.of("car", List.of("automobile")))
                .synonyms(analyzer -> null);
        assertThrows(IllegalStateException.class, builder::build);
        }

    @Test
    void testSolrSynonyms(@TempDir Path tempDir)
            throws IOException
        {
        Path file = tempDir.resolve("synonyms.txt");
        Files.writeString(file, "car, automobile, vehicle\n");
        LuceneQueryParser parser = LuceneQueryParser.builder()
                .field(TITLE)
                .synonyms(LuceneQueryParser.solrSynonyms(file, true))
                .build();
        Query query = parser.parse("car");
        assertThat(query, is(notNullValue()));
        assertThat(query.toString(), containsString(":automobile"));
        assertThat(query.toString(), containsString(":vehicle"));
        }

    @Test
    void testWordNetSynonyms(@TempDir Path tempDir) throws IOException
        {
        Path file = tempDir.resolve("wordnet.txt");
        // Provide a valid WordNet synonym group, or expect the exception
        Files.writeString(file, "s(100000001,1,'car',n,1,0).\ns(100000001,2,'automobile',n,1,0).\n");
        LuceneQueryParser parser = LuceneQueryParser.builder()
                .field(TITLE)
                .synonyms(LuceneQueryParser.wordNetSynonyms(file))
                .build();
        Query query = parser.parse("car");
        assertThat(query, is(notNullValue()));
        assertThat(query.toString(), containsString(":automobile"));
        }
    }
