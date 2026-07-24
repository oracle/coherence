/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.query;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.coherence.rest.RestQueryPolicy;

import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.util.ValueExtractor;

import data.pof.Person;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link AbstractQueryEngine}.
 *
 * @author as  2012.01.20
 */
public class QueryEngineTest extends AbstractQueryEngine
    {
    @After
    public void cleanup()
        {
        CoherenceModeHelper.clear();
        }

    public Query prepareQuery(String sQuery, Map<String, Object> mapParams)
        {
        return null;
        }

    @Test
    public void testQueryParsing()
        {
        String sQuery = "int = :int;i and str = :str and date = :date;date and person = :person;data.pof.Person";

        ParsedQuery pq = parseQueryString(sQuery);
        assertTrue(pq == parseQueryString(sQuery));

        assertEquals("int = :int and str = :str and date = :date and person = :person",
                pq.getQuery());

        Map<String, Class> mapTypes = pq.getParameterTypes();
        assertEquals(4, mapTypes.size());
        assertEquals(Integer.class, mapTypes.get("int"));
        assertEquals(String.class, mapTypes.get("str"));
        assertEquals(Date.class, mapTypes.get("date"));
        assertEquals(Person.class, mapTypes.get("person"));
        }

    @Test
    public void testQueryParsingCacheIncludesTypeHints()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            ParsedQuery queryInt  = parseQueryString("value = :v;i");
            ParsedQuery queryLong = parseQueryString("value = :v;long");

            assertSame(queryInt, parseQueryString("value = :v;i"));
            assertSame(queryLong, parseQueryString("value = :v;long"));
            assertNotSame(queryInt, queryLong);
            assertEquals("value = :v", queryInt.getQuery());
            assertEquals("value = :v", queryLong.getQuery());
            assertEquals(Integer.class, queryInt.getParameterTypes().get("v"));
            assertEquals(Long.class, queryLong.getParameterTypes().get("v"));
            }
        }

    @Test
    public void testCompatibilityQueryParsingCacheUsesStrippedQueryText()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            ParsedQuery queryInt  = parseQueryString("value = :v;i");
            ParsedQuery queryLong = parseQueryString("value = :v;long");

            assertSame(queryInt, queryLong);
            assertSame(queryInt, parseQueryString("value = :v;i"));
            assertEquals("value = :v", queryInt.getQuery());
            assertEquals(Integer.class, queryInt.getParameterTypes().get("v"));
            }
        }

    @Test
    public void testCompatibilityQueryParsingCacheSharedAcrossDirectQueryTypePolicy()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            String sQuery = "person = :person;data.pof.Person";
            ParsedQuery directQuery = RestQueryPolicy.withDirectQueryTypePolicy(() -> parseQueryString(sQuery));
            ParsedQuery operatorQuery = parseQueryString(sQuery);
            assertSame(directQuery, operatorQuery);
            assertSame(directQuery, RestQueryPolicy.withDirectQueryTypePolicy(() -> parseQueryString(sQuery)));
            assertEquals("person = :person", directQuery.getQuery());
            assertEquals(Person.class, directQuery.getParameterTypes().get("person"));
            }
        }

    @Test
    public void testQueryParsingCacheIncludesDirectQueryTypePolicy()
        {
        String      sQuery        = "person = :person;data.pof.Person";
        ParsedQuery operatorQuery = parseQueryString(sQuery);

        assertSame(operatorQuery, parseQueryString(sQuery));
        assertEquals(Person.class, operatorQuery.getParameterTypes().get("person"));

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            try
                {
                RestQueryPolicy.withDirectQueryTypePolicy(() ->
                    {
                    parseQueryString(sQuery);
                    return null;
                    });
                fail("Expected direct-query type policy to reject FQN type hint");
                }
            catch (IllegalArgumentException expected)
                {
                assertTrue(expected.getMessage().contains("unsupported REST query parameter type hint"));
                }
            }
        }

    @Test
    public void testQueryParsingCacheConcurrentMixedPolicies() throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            ExecutorService executor = Executors.newFixedThreadPool(8);
            CountDownLatch  latch    = new CountDownLatch(1);
            List<Future<Void>> listFutures = new ArrayList<>();

            // rest-01 prompt 04 design/features/security-bugs/plans/rest-01/prompts/04-slice-c-query-expression-implementation.md:
            // keep mode stable because it is process-global; mix only the thread-local direct-query policy to prove cache scopes cannot leak
            for (int i = 0; i < 8; i++)
                {
                listFutures.add(executor.submit(() ->
                    {
                    latch.await();
                    for (int j = 0; j < 250; j++)
                        {
                        assertParameterType("value = :v;i", "v", Integer.class);
                        assertParameterType("value = :v;long", "v", Long.class);
                        assertParameterType("person = :person;data.pof.Person", "person", Person.class);
                        assertStrictDirectQueryTypeRejected("person = :person;data.pof.Person");
                        }
                    return null;
                    }));
                }

            latch.countDown();
            try
                {
                for (Future<Void> future : listFutures)
                    {
                    future.get(30, TimeUnit.SECONDS);
                    }
                }
            finally
                {
                executor.shutdownNow();
                }
            }
        }

    @Test
    public void testStrictDirectQueryExecutionUsesStrippedTypeHints()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            CoherenceQueryLanguageEngine engine = new CoherenceQueryLanguageEngine();
            NamedCache<Integer, AgeRecord> cache = createAgeCache();

            // rest-01 Prompt 04 validates REST direct queries after stripping type hints;
            // these execution checks prove the same stripped form is what reaches the builder.
            assertDirectQueryAges(engine, cache, "age == :age;i", mapOf("age", "36"), 36);
            assertDirectQueryAges(engine, cache, "age>=:age;i", mapOf("age", "39"), 39, 40);
            assertDirectQueryAges(engine, cache, "age >= :min;i && age <= :max;long",
                    mapOf("min", "39", "max", "40"), 39, 40);

            // whitespace inside a hint token is deliberately rejected, not normalized,
            // so malformed hints cannot validate as one query shape and execute as another.
            assertDirectQueryRejected(engine, "age == :age ;i", mapOf("age", "36"));
            assertDirectQueryRejected(engine, "age == :age; i", mapOf("age", "36"));
            assertDirectQueryRejected(engine, "age == :age ; i", mapOf("age", "36"));
            assertDirectQueryRejected(engine, "person == :person;data.pof.Person", mapOf("person", "Ivan"));
            }
        }

    private NamedCache<Integer, AgeRecord> createAgeCache()
        {
        NamedCache<Integer, AgeRecord> cache = new WrapperNamedCache<>(new HashMap<>(), "ages");

        cache.put(1, new AgeRecord(36));
        cache.put(2, new AgeRecord(39));
        cache.put(3, new AgeRecord(40));
        return cache;
        }

    private void assertParameterType(String sQuery, String sParam, Class clzExpected)
        {
        assertEquals(clzExpected, parseQueryString(sQuery).getParameterTypes().get(sParam));
        }

    private void assertStrictDirectQueryTypeRejected(String sQuery)
        {
        try
            {
            RestQueryPolicy.withDirectQueryTypePolicy(() ->
                {
                parseQueryString(sQuery);
                return null;
                });
            fail("Expected direct-query type policy to reject FQN type hint");
            }
        catch (IllegalArgumentException expected)
            {
            assertTrue(expected.getMessage().contains("unsupported REST query parameter type hint"));
            }
        }

    private void assertDirectQueryAges(CoherenceQueryLanguageEngine engine,
            NamedCache<Integer, AgeRecord> cache, String sQuery, Map<String, Object> mapParams,
            long... alExpected)
        {
        Query query = RestQueryPolicy.withDirectQueryTypePolicy(() -> engine.prepareQuery(sQuery, mapParams));
        ValueExtractor<Map.Entry, AgeRecord> extractor = entry -> (AgeRecord) entry.getValue();
        Collection<AgeRecord> results = query.execute(cache, extractor, null, 0, -1);

        assertEquals(alExpected.length, results.size());
        for (long lExpected : alExpected)
            {
            assertTrue("missing age " + lExpected,
                    results.stream().anyMatch(record -> record.getAge().longValue() == lExpected));
            }
        }

    private void assertDirectQueryRejected(CoherenceQueryLanguageEngine engine,
            String sQuery, Map<String, Object> mapParams)
        {
        try
            {
            RestQueryPolicy.withDirectQueryTypePolicy(() -> engine.prepareQuery(sQuery, mapParams));
            fail("Expected strict REST direct query to reject " + sQuery);
            }
        catch (RuntimeException expected)
            {
            // expected
            }
        }

    private static Map<String, Object> mapOf(String sKey, Object oValue)
        {
        return Collections.singletonMap(sKey, oValue);
        }

    private static Map<String, Object> mapOf(String sKeyOne, Object oValueOne,
                                             String sKeyTwo, Object oValueTwo)
        {
        Map<String, Object> map = new HashMap<>();
        map.put(sKeyOne, oValueOne);
        map.put(sKeyTwo, oValueTwo);
        return map;
        }

    /**
     * Test value with numeric equality and ordering across hint target classes.
     */
    public static class AgeRecord
        {
        private AgeRecord(long lAge)
            {
            m_age = new ComparableNumber(lAge);
            }

        public ComparableNumber getAge()
            {
            return m_age;
            }

        private final ComparableNumber m_age;
        }

    /**
     * Comparable number used so the test can exercise mixed ;i and ;long hints.
     */
    public static class ComparableNumber
            implements Comparable<Object>
        {
        private ComparableNumber(long lValue)
            {
            m_lValue = lValue;
            }

        @Override
        public int compareTo(Object o)
            {
            if (o instanceof Number)
                {
                return Long.compare(m_lValue, ((Number) o).longValue());
                }
            if (o instanceof ComparableNumber)
                {
                return Long.compare(m_lValue, ((ComparableNumber) o).m_lValue);
                }
            throw new ClassCastException(String.valueOf(o));
            }

        @Override
        public boolean equals(Object o)
            {
            if (o instanceof Number)
                {
                return m_lValue == ((Number) o).longValue();
                }
            return o instanceof ComparableNumber
                    && m_lValue == ((ComparableNumber) o).m_lValue;
            }

        @Override
        public int hashCode()
            {
            return Long.hashCode(m_lValue);
            }

        public long longValue()
            {
            return m_lValue;
            }

        private final long m_lValue;
        }
    }
