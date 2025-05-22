/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.lucene.pof;

import com.oracle.coherence.lucene.pof.LucenePofSerializers;
import com.tangosol.io.pof.SimplePofContext;
import com.tangosol.util.ExternalizableHelper;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Lucene POF serializers.
 * Tests serialization and deserialization of various Lucene query types
 * using Coherence POF serialization.
 *
 * @author Aleks Seovic  2025.05.16
 */
public class LucenePofSerializersTest
    {

    /**
     * The POF context used for serialization tests.
     * Initialized once before all tests with all necessary serializers.
     */
    private static SimplePofContext context;

    /**
     * Sets up the POF context with all necessary serializers before running tests.
     */
    @BeforeAll
    public static void setup()
        {
        context = new SimplePofContext();
        context.registerUserType(26999, Term.class, new LucenePofSerializers.TermSerializer());
        context.registerUserType(27000, TermQuery.class, new LucenePofSerializers.TermQuerySerializer());
        context.registerUserType(27001, BooleanClause.class, new LucenePofSerializers.BooleanClauseSerializer());
        context.registerUserType(27002, BooleanQuery.class, new LucenePofSerializers.BooleanQuerySerializer());
        context.registerUserType(27003, MatchAllDocsQuery.class, new LucenePofSerializers.MatchAllDocsQuerySerializer());
        context.registerUserType(27004, PhraseQuery.class, new LucenePofSerializers.PhraseQuerySerializer());
        context.registerUserType(27005, WildcardQuery.class, new LucenePofSerializers.WildcardQuerySerializer());
        context.registerUserType(27006, PrefixQuery.class, new LucenePofSerializers.PrefixQuerySerializer());
        context.registerUserType(27007, FuzzyQuery.class, new LucenePofSerializers.FuzzyQuerySerializer());
        context.registerUserType(27008, RegexpQuery.class, new LucenePofSerializers.RegexpQuerySerializer());
        context.registerUserType(27009, BoostQuery.class, new LucenePofSerializers.BoostQuerySerializer());
        }

    /**
     * Helper method to test serialization roundtrip.
     *
     * @param original the object to serialize and deserialize
     * @param <T>     the type of object
     *
     * @return the deserialized object
     */
    private <T> T roundTrip(T original)
        {
        return ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(original, context), context);
        }

    /**
     * Tests serialization roundtrip of TermQuery.
     */
    @Test
    public void testTermQueryRoundTrip()
        {
        TermQuery original = new TermQuery(new Term("field", "value"));
        TermQuery result = roundTrip(original);
        assertEquals(original.getTerm(), result.getTerm());
        }

    /**
     * Tests serialization roundtrip of BooleanClause.
     */
    @Test
    public void testBooleanClauseRoundTrip()
        {
        BooleanClause original = new BooleanClause(new MatchAllDocsQuery(), BooleanClause.Occur.MUST);
        BooleanClause result = roundTrip(original);
        assertEquals(original.occur(), result.occur());
        assertTrue(result.query() instanceof MatchAllDocsQuery);
        }

    /**
     * Tests serialization roundtrip of BooleanQuery.
     */
    @Test
    public void testBooleanQueryRoundTrip()
        {
        BooleanQuery original = new BooleanQuery.Builder().add(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD).build();
        BooleanQuery result = roundTrip(original);
        List<BooleanClause> origClauses = original.clauses();
        List<BooleanClause> resClauses = result.clauses();
        assertEquals(origClauses.size(), resClauses.size());
        for (int i = 0; i < origClauses.size(); i++)
            {
            assertEquals(origClauses.get(i).occur(), resClauses.get(i).occur());
            assertTrue(resClauses.get(i).query() instanceof MatchAllDocsQuery);
            }
        }

    /**
     * Tests serialization roundtrip of MatchAllDocsQuery.
     */
    @Test
    public void testMatchAllDocsQueryRoundTrip()
        {
        MatchAllDocsQuery original = new MatchAllDocsQuery();
        MatchAllDocsQuery result = roundTrip(original);
        assertEquals(original.getClass(), result.getClass());
        }

    /**
     * Tests serialization roundtrip of PhraseQuery.
     */
    @Test
    public void testPhraseQueryRoundTrip()
        {
        PhraseQuery original = new PhraseQuery.Builder().add(new Term("field", "value"), 0).build();
        PhraseQuery result = roundTrip(original);
        assertArrayEquals(original.getTerms(), result.getTerms());
        assertArrayEquals(original.getPositions(), result.getPositions());
        }

    /**
     * Tests serialization roundtrip of WildcardQuery.
     */
    @Test
    public void testWildcardQueryRoundTrip()
        {
        WildcardQuery original = new WildcardQuery(new Term("field", "val*"));
        WildcardQuery result = roundTrip(original);
        assertEquals(original.getTerm(), result.getTerm());
        }

    /**
     * Tests serialization roundtrip of PrefixQuery.
     */
    @Test
    public void testPrefixQueryRoundTrip()
        {
        PrefixQuery original = new PrefixQuery(new Term("field", "val"));
        PrefixQuery result = roundTrip(original);
        assertEquals(original.getPrefix(), result.getPrefix());
        }

    /**
     * Tests serialization roundtrip of FuzzyQuery.
     */
    @Test
    public void testFuzzyQueryRoundTrip()
        {
        FuzzyQuery original = new FuzzyQuery(new Term("field", "value"), 2);
        FuzzyQuery result = roundTrip(original);
        assertEquals(original.getTerm(), result.getTerm());
        assertEquals(original.getMaxEdits(), result.getMaxEdits());
        }

    /**
     * Tests serialization roundtrip of RegexpQuery.
     */
    @Test
    public void testRegexpQueryRoundTrip()
        {
        RegexpQuery original = new RegexpQuery(new Term("field", "val.*"));
        RegexpQuery result = roundTrip(original);
        assertEquals(original.getRegexp(), result.getRegexp());
        }

    /**
     * Tests serialization roundtrip of BoostQuery.
     */
    @Test
    public void testBoostQueryRoundTrip()
        {
        BoostQuery original = new BoostQuery(new MatchAllDocsQuery(), 2.5f);
        BoostQuery result = roundTrip(original);
        assertTrue(result.getQuery() instanceof MatchAllDocsQuery);
        assertEquals(original.getBoost(), result.getBoost(), 0.0);
        }
    }
