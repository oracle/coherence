/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import com.tangosol.coherence.dsltools.precedence.OPParser;

import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import com.tangosol.net.cache.TypeAssertion;
import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.FilterBuildingException;
import com.tangosol.util.QueryHelper;

import data.persistence.Person;

import java.awt.Point;

import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.mockito.stubbing.Answer;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the FilterBuilding machinery.
 *
 * @author djl  2010.01.03
 */
public class SimpleQueryHelperTest
    {
    @Before
    public void setup()
        {
        m_executionContext = mock(ExecutionContext.class);
        m_cluster          = mock(Cluster.class);
        m_session          = mock(Session.class);
        m_cacheService     = mock(CacheService.class);
        m_map              = new HashMap();
        m_cacheTest1       = new WrapperNamedCache(m_map, "test", m_cacheService);
        m_cacheTest2       = new WrapperNamedCache(m_map, "test-2", m_cacheService);
        m_language         = new CoherenceQueryLanguage();

        when(m_executionContext.getCoherenceQueryLanguage()).thenReturn(m_language);
        when(m_executionContext.getSession()).thenReturn(m_session);
        when(m_executionContext.getCluster()).thenReturn(m_cluster);
        when(m_executionContext.instantiateParser(any())).thenAnswer(
                (Answer<OPParser>) invocationOnMock -> new OPParser((StringReader) invocationOnMock.getArgument(0),
                                                                    m_language.extendedSqlTokenTable(),
                                                                    m_language.getOperators()));
        when(m_session.getCache(eq("test"), any(TypeAssertion.class))).thenReturn(m_cacheTest1);
        when(m_session.getCache(eq("test-2"), any(TypeAssertion.class))).thenReturn(m_cacheTest2);
        when(m_session.isCacheActive(eq("test"), nullable(ClassLoader.class))).thenReturn(true);
        when(m_session.isCacheActive(eq("test-2"), nullable(ClassLoader.class))).thenReturn(false);
        when(m_cacheService.getCacheNames()).thenReturn(Collections.enumeration(Collections.singleton("test")));
        when(m_cluster.getServiceNames()).thenReturn(Collections.enumeration(Collections.singleton("test-service")));
        when(m_cluster.getService("test-service")).thenReturn(m_cacheService);

        for (int i = 1; i <= m_nAgeMax; i++)
            {
            m_nSalary = (i * 1000) + i;
            m_person  = new Person(i, "Person-" + i);
            m_person.setState("MA");
            m_person.setSalary(m_nSalary);
            m_person.setAge(i);

            m_cacheTest1.put(String.valueOf(i), m_person);

            m_nAgeTotal    = m_nAgeTotal + i;
            m_nSalaryTotal = m_nSalaryTotal + m_nSalary;
            assertEquals(i, m_cacheTest1.size());
            }
        }

    @Test
    public void testCreateFilter1()
        {
        Filter f = QueryHelper.createFilter("value() = 42");

        assertTrue(f.evaluate(42));
        }

    @Test
    public void testCreateFilter2()
        {
        Filter f = QueryHelper.createFilter("value() > 42 and value() <= 150");

        assertTrue(f.evaluate(43));
        assertTrue(f.evaluate(100));
        assertTrue(f.evaluate(150));
        assertFalse(f.evaluate(42));
        }

    @Test
    public void testCreateFilter3()
        {
        Filter f = QueryHelper.createFilter("value() between 42 and 150");

        assertTrue(f.evaluate(43));
        assertTrue(f.evaluate(100));
        assertTrue(f.evaluate(150));
        assertTrue(f.evaluate(42));
        }

    @Test
    public void testCreateFilter4()
        {
        Filter f = QueryHelper.createFilter("value() in (10,20,30)");

        assertTrue(f.evaluate(10));
        assertTrue(f.evaluate(20));
        assertTrue(f.evaluate(30));
        assertFalse(f.evaluate(42));
        }

    @Test
    public void testCreateFilter5()
        {
        Filter f = QueryHelper.createFilter("17 > value()");

        assertTrue(f.evaluate(16));
        assertTrue(false == f.evaluate(17));
        }

    @Test
    public void testCreateFilter6()
        {
        try
            {
            Filter f = QueryHelper.createFilter("17 > ");

            assertTrue(false);
            }
        catch (FilterBuildingException ex)
            {
            assertTrue(true);
            }
        }

    @Test
    public void testCreateFilter7()
        {
        Filter f = QueryHelper.createFilter("value() = ?3 ", new Object[] {"hello", "goodby", 42});

        assertTrue(f.evaluate(42));
        }

    @Test
    public void testCreateFilter8()
        {
        Filter f = QueryHelper.createFilter("toString() like '42'");

        assertTrue(f.evaluate(42));
        assertFalse(f.evaluate(43));
        }

    @Test
    public void testCreateFilter9()
        {
        Filter f = QueryHelper.createFilter("x >= 10.0d and y <=20.0d");

        assertTrue(f.evaluate(new Point(10, 20)));
        }

    @Test
    public void testCreateFilter10()
        {
        Filter f = QueryHelper.createFilter("x >= 10.0d and y <=20.0d or x = 3.0d");

        assertTrue(f.evaluate(new Point(10, 20)));
        assertTrue(f.evaluate(new Point(3, 100)));
        assertFalse(f.evaluate(new Point(90, 100)));
        }

    @Test
    public void testCreateFilter11()
        {
        Filter f = QueryHelper.createFilter("value() like 'dav%' ");

        assertTrue(f.evaluate("david"));
        }

    @Test
    public void testCreateFilter12()
        {
        Filter f = QueryHelper.createFilter("value() like 'dav_d' ");

        assertTrue(f.evaluate("david"));
        }

    @Test
    public void testCreateFilter13()
        {
        HashMap env = new HashMap();

        env.put("val", 765);
        env.put("big", "brother");

        Filter f = QueryHelper.createFilter("value() like :big ", env);

        assertTrue(f.evaluate("brother"));
        }

    @Test
    public void testCreateFilter14()
        {
        Filter f = QueryHelper.createFilter("value() is not null ");

        assertTrue(f.evaluate("brother"));
        }

    @Test
    public void testCreateFilter15()
        {
        ArrayList l = new ArrayList();

        l.add(10);
        l.add(20);
        l.add(30);

        Filter f = QueryHelper.createFilter("value() contains 20");

        assertTrue(f.evaluate(l));
        }

    @Test
    public void testCreateFilter16()
        {
        ArrayList l = new ArrayList();

        l.add(10);
        l.add(20);
        l.add(30);

        Filter f = QueryHelper.createFilter("value().toArray() contains 20");

        assertTrue(f.evaluate(l));
        }

    @Test
    public void testCreateFilter17()
        {
        ArrayList l = new ArrayList();

        l.add(10);
        l.add(20);
        l.add(30);

        Filter f = QueryHelper.createFilter("value() contains any (50, 100, 20)");

        assertTrue(f.evaluate(l));
        }

    @Test
    public void testCreateFilter18()
        {
        ArrayList l = new ArrayList();

        l.add(10);
        l.add(20);
        l.add(30);

        Filter f = QueryHelper.createFilter("value() contains all (10, 20, 30)");

        assertTrue(f.evaluate(l));
        }

    @Test
    public void testCreateFilter19()
        {
        ArrayList l = new ArrayList();

        l.add(10);
        l.add(20);
        l.add(30);

        Filter f = QueryHelper.createFilter("value() contains all (10, 20, 30, 40)");

        assertFalse(f.evaluate(l));
        }

    @Test
    public void testCreateFilter20()
        {
        try
            {
            Filter f = QueryHelper.createFilter("city EQUALS 'Boston'");

            assertTrue(false);
            }
        catch (FilterBuildingException ex)
            {
            assertEquals("Unexpected Identifier EQUALS in operator position", ex.getMessage());
            }
        }

    @Test
    public void testCreateFilter21()
        {
        Filter f = QueryHelper.createFilter("value() <> 42");

        assertTrue(f.evaluate(43));
        }

    @Test
    public void testCreateFilter22()
        {
        Filter f;

        f = QueryHelper.createFilter("value() = 42.0");
        assertTrue(f.evaluate(42.0));
        f = QueryHelper.createFilter("value() = 42.0d");
        assertTrue(f.evaluate(42.0));
        f = QueryHelper.createFilter("value() = 42.0f");
        assertTrue(f.evaluate(42.0f));
        f = QueryHelper.createFilter("value() = 6.12E10");
        assertTrue(f.evaluate(6.12E10));
        f = QueryHelper.createFilter("value() = 6.12E-10");
        assertTrue(f.evaluate(6.12E-10));
        f = QueryHelper.createFilter("value() = 6.12E-10D");
        assertTrue(f.evaluate(6.12E-10));
        f = QueryHelper.createFilter("value() = 6.12e-10f");
        assertTrue(f.evaluate(6.12e-10f));
        }

    @Test(expected = FilterBuildingException.class)
    public void testCreateFilterComparison2IdentifiersEQ()
        {
        Filter f = QueryHelper.createFilter("a = b");
        }

    @Test(expected = FilterBuildingException.class)
    public void testCreateFilterComparison2IdentifiersNE()
        {
        Filter f = QueryHelper.createFilter("a != b");
        }

    @Test
    public void testSelectCount()
        {
        m_sQuery   = "select count() from test";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        assertTrue(m_oResults instanceof Number);
        assertEquals(100, ((Number) m_oResults).intValue());
        }

    @Test
    public void testSelectStarWhere()
        {
        m_sQuery   = "select * from test where id == 1";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        assertTrue(m_oResults instanceof Collection);
        assertEquals(1, ((Set) m_oResults).size());
        }

    @Test
    public void testSelectCountWhereNoMatchingEntries()
        {
        m_sQuery   = "select count() from test where age > 200";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        assertTrue(m_oResults instanceof Number);
        assertEquals(0, ((Number) m_oResults).intValue());
        }

    @Test
    public void testSelectCountWhereMatchingEntries()
        {
        m_sQuery   = "select count() from test where salary > 99000";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        assertTrue(m_oResults instanceof Number);
        assertEquals(2, ((Number) m_oResults).intValue());
        }

    @Test
    public void testSelectSumAge()
        {
        m_sQuery   = "select sum(age) from test";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        assertTrue(m_oResults instanceof Number);
        assertEquals(m_nAgeTotal, ((Number) m_oResults).intValue());
        }

    @Test
    public void testSelectSumSalary()
        {
        m_sQuery   = "select sum(salary) from test";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        assertTrue(m_oResults instanceof Number);
        assertEquals(m_nSalaryTotal, ((Number) m_oResults).intValue());
        }

    @Test
    public void testSelectMax()
        {
        m_sQuery   = "select max(age),max(salary) from test";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        assertTrue((m_oResults instanceof Collection) || (m_oResults instanceof Map));
        assertEquals(m_nAgeMax, (((Number) (((Set) m_oResults).toArray())[0]).intValue()));
        assertEquals(((m_nAgeMax * 1000) + m_nAgeMax), (((Number) (((Set) m_oResults).toArray())[1]).intValue()));
        }

    @Test
    public void testSelectMin()
        {
        m_sQuery   = "select min(age),min(salary) from test";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        assertTrue((m_oResults instanceof Collection) || (m_oResults instanceof Map));
        assertEquals(1, (((Number) (((Set) m_oResults).toArray())[0]).intValue()));
        assertEquals(1001, (((Number) (((Set) m_oResults).toArray())[1]).intValue()));
        }

    @Test
    public void testSelectAverage()
        {
        m_sQuery   = "select avg(age),avg(salary) from test";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        assertTrue((m_oResults instanceof Collection) || (m_oResults instanceof Map));
        assertEquals(((double) m_nAgeTotal / (double) m_nAgeMax),
                     (((Number) (((Set) m_oResults).toArray())[0]).doubleValue()), 0.5);
        assertEquals(((double) m_nSalaryTotal / (double) m_nAgeMax),
                     (((Number) (((Set) m_oResults).toArray())[1]).doubleValue()), 0.5);
        }

    @Test
    public void testSelectAverageGroupByWhere()
        {
        m_sQuery   = "select avg(age),avg(salary) from test where salary > 100000";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        assertTrue((m_oResults instanceof Collection) || (m_oResults instanceof Map));
        assertEquals((double) m_nAgeMax,
                     (((Number) (((Set) m_oResults).toArray())[0]).doubleValue()), 0.5);
        assertEquals((double) ((m_nAgeMax * 1000) + m_nAgeMax),
                     (((Number) (((Set) m_oResults).toArray())[1]).doubleValue()), 0.5);
        }

    @Test
    public void testSelectAverageGroupBy()
        {
        m_sQuery   = "select age, avg(salary) from test group by age";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        assertTrue((m_oResults instanceof Collection) || (m_oResults instanceof Map));
        }

    @Test
    public void testSelectGroupBy()
        {
        m_sQuery   = "select age, salary from test group by age";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        assertThat(m_oResults, is(instanceOf(Map.class)));

        Map map = (Map) m_oResults;

        assertThat(map.size(), is(100));
        assertThat(map.get("35"), is(instanceOf(Collection.class)));
        assertThat(((Collection) map.get("35")).size(), is(2));
        }

    @Test
    public void testDeleteWhereNoMatchingEntries()
        {
        // ID 201 does not exist. So the total count should remain the same.
        m_sQuery   = "delete from test where age == 201";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        assertThat(m_oResults, is(instanceOf(Collection.class)));
        assertThat(((Collection) m_oResults).size(), is(0));
        }

    @Test
    public void testDeleteWhere()
        {
        m_sQuery   = "delete from test where id <= 10";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        assertTrue((m_oResults instanceof Collection) || (m_oResults instanceof Map));
        }

    @Test
    public void testInsert()
        {
        m_sQuery   = "insert into test key 1 value 1";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        assertNull(m_oResults);
        }

    @Test
    public void testInsertWhereValueExists()
        {
        m_cacheTest1.put(1, 1);
        m_sQuery   = "insert into test key 1 value 2";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        assertThat(m_oResults, is((Object) 1));
        }

    @Test
    public void testSelectStartWhereKeyEquals()
        {
        m_cacheTest1.put(1, 1);
        m_sQuery   = "select * from test where key() is 1";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        assertTrue(m_oResults instanceof Collection);
        assertEquals(1, ((Set) m_oResults).size());
        }

    @Test
    public void testSelect()
        {
        // this should throw a runtime exception as the query string is illegal.
        m_sQuery = "select state+age+count() from test group by state, age";

        try
            {
            QueryHelper.executeStatement(m_sQuery, m_executionContext);
            }
        catch (Exception e)
            {
            // Exception being thrown as expected
            return;
            }

        fail("Expected exception not thrown");

        }

    @Test(expected = AssertionError.class)
    public void shouldFailSanityCheck() throws Exception
        {
        when(m_executionContext.isSanityChecking()).thenReturn(true);

        m_sQuery   = "select * from 'test-2' where key() is 1";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        }

    @Test
    public void shouldSkipSanityCheck() throws Exception
        {
        when(m_executionContext.isSanityChecking()).thenReturn(false);

        m_cacheTest2.put(1, 1);

        m_sQuery   = "select * from 'test-2' where key() is 1";
        m_oResults = QueryHelper.executeStatement(m_sQuery, m_executionContext);
        assertTrue(m_oResults instanceof Collection);
        assertEquals(1, ((Set) m_oResults).size());
        }

    @Test
    public void shouldCreateExpectedExecutionContext()
        {
        ExecutionContext underTest = QueryHelper.createExecutionContext(m_session);
        assertThat(underTest.getSession(), is(m_session));
        assertThat(underTest.getCoherenceQueryLanguage(), notNullValue());
        assertThat(underTest.isExtendedLanguageEnabled(), is(true));
        assertThat(underTest.isSanityChecking(), is(true));
        assertThat(underTest.isSilent(), is(true));
        }

    @Test
    public void shouldCreateSimpleStatements()
        {
        m_cacheTest1.put(1, 1);

        ExecutionContext ctx = QueryHelper.createExecutionContext(m_session);
        Statement s = QueryHelper.createStatement("select * from test where key() is 1", ctx);
        assertThat(s, notNullValue());
        m_oResults = s.execute(ctx);
        assertThat(m_oResults, instanceOf(StatementResult.class));
        StatementResult statementResult = (StatementResult) m_oResults;
        assertThat(statementResult.getResult(), instanceOf(Collection.class));
        assertThat(1, is(((Collection) statementResult.getResult()).size()));
        }

    @Test
    public void shouldCreateStatementWithPositionalParameters()
        {
        ExecutionContext ctx = QueryHelper.createExecutionContext(m_session);
        Statement        s   = QueryHelper.createStatement("select * from test where name == ?1",
                                                           ctx, new Object[]{ "Person-3" });
        assertThat(s, notNullValue());

        m_oResults = s.execute(ctx);
        assertThat(m_oResults, instanceOf(StatementResult.class));
        StatementResult statementResult = (StatementResult) m_oResults;
        assertThat(statementResult.getResult(), instanceOf(Collection.class));

        Collection results = (Collection) statementResult.getResult();
        assertThat(results.size(), is(1));

        Optional optional = results.stream().findFirst();
        assertThat(((Map.Entry) optional.get()).getValue(), is(new Person(3, "Person-3")));
        }

    @Test
    public void shouldCreateStatementWithBindingParameters()
        {
        ExecutionContext ctx = QueryHelper.createExecutionContext(m_session);
        ctx.setExtendedLanguage(false); // cannot use extended language with binding parameters

        Statement s = QueryHelper.createStatement("select * from test where name == :p1",
                                                  ctx,
                                                  Collections.singletonMap("p1", "Person-3"));
        assertThat(s, notNullValue());
        m_oResults = s.execute(ctx);

        assertThat(m_oResults, instanceOf(StatementResult.class));
        StatementResult statementResult = (StatementResult) m_oResults;
        assertThat(statementResult.getResult(), instanceOf(Collection.class));

        Collection results = (Collection) statementResult.getResult();
        assertThat(results.size(), is(1));

        Optional optional = results.stream().findFirst();
        assertThat(((Map.Entry) optional.get()).getValue(), is(new Person(3, "Person-3")));
        }


    protected ExecutionContext         m_executionContext;
    protected CoherenceQueryLanguage   m_language;
    protected Cluster                  m_cluster;
    protected Session                  m_session;
    protected CacheService             m_cacheService;
    protected Map                      m_map;
    protected NamedCache               m_cacheTest1;
    protected NamedCache               m_cacheTest2;
    protected int                      m_nAgeTotal    = 0;
    protected int                      m_nSalaryTotal = 0;
    protected int                      m_nSalary      = 0;
    protected int                      m_nAgeMax      = 100;
    protected Person                   m_person       = null;
    protected String                   m_sQuery;
    protected Object                   m_oResults;
    }
