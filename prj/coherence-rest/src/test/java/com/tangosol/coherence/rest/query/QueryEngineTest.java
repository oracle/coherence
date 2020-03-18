/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.query;

import data.pof.Person;

import java.util.Date;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link AbstractQueryEngine}.
 *
 * @author as  2012.01.20
 */
public class QueryEngineTest extends AbstractQueryEngine
    {
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
    }
