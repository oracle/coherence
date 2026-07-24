/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.coherence.rest.config.ExpressionAliasConfig;

import com.tangosol.coherence.rest.util.PartialObject;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.WrapperNamedCache;

import data.pof.Person;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link EntrySetResource}.
 * <p>
 * Prompt 04 at
 * design/features/security-bugs/plans/rest-01/prompts/04-slice-c-query-expression-implementation.md
 * adds alias-policy coverage for entry-set projection, aggregation, and
 * processor paths.
 *
 * @author ic  2011.07.01
 */
@SuppressWarnings({"unchecked", "deprecation"})
public class EntrySetResourceTest
    {
    @Before
    public void setUp()
        {
        m_cache = new WrapperNamedCache(new HashMap<Integer, Person>(), "persons");
        m_cache.put(2, new Person("Aleks", new Date(74, 7, 24), 39));
        m_cache.put(3, new Person("Vaso", new Date(74, 7, 7), 40));
        m_cache.put(1, new Person("Ivan", new Date(78, 3, 25), 36));
        }

    @After
    public void cleanup()
        {
        CoherenceModeHelper.clear();
        }

    @Test
    public void testGetValues()
        {
        Set<Integer>     setKeys  = new HashSet<>(Arrays.asList(1, 2));
        EntrySetResource resource = new EntrySetResource(m_cache, setKeys, Person.class);
        Response         response = resource.getValues(null);

        assertEquals(200 /* OK */, response.getStatus());

        Collection<String> colExpected = Arrays.asList("Ivan", "Aleks");
        Collection<Person> colPersons  = (Collection<Person>) response.getEntity();
        assertEquals(2, colPersons.size());
        for (Person person : colPersons)
            {
            assertTrue(colExpected.contains(person.getName()));
            }
        }

    @Test
    public void testGetEntries()
        {
        Set<Integer>     setKeys  = new HashSet<Integer>(Arrays.asList(1, 2));
        EntrySetResource resource = new EntrySetResource(m_cache, setKeys, Person.class);
        Response         response = resource.getEntries(null);

        assertEquals(200 /* OK */, response.getStatus());

        Collection<String> colExpected = Arrays.asList("Ivan", "Aleks");
        Collection<Map.Entry<Integer, Person>> colPersons  =
                (Collection<Map.Entry<Integer, Person>>) response.getEntity();
        assertEquals(2, colPersons.size());
        for (Map.Entry<Integer, Person> person : colPersons)
            {
            assertTrue(colExpected.contains(person.getValue().getName()));
            }
        }

    @Test
    public void testPartialGet()
        {
        EntrySetResource resource = new EntrySetResource(m_cache, Collections.singleton(1), Person.class);
        Response         response = resource.getValues("dateOfBirth");

        assertEquals(200 /* OK */, response.getStatus());

        Collection<PartialObject> colPartials = (Collection<PartialObject>) response.getEntity();
        assertEquals(1, colPartials.size());
        assertEquals(new Date(78, 3, 25), colPartials.iterator().next().get("dateOfBirth"));
        }

    @Test
    public void testDelete()
        {
        Set<Integer>     setKeys  = new HashSet<Integer>(Arrays.asList(1, 2));
        EntrySetResource resource = new EntrySetResource(m_cache, setKeys, Person.class) ;
        Response         response = resource.delete();

        assertEquals(200 /* OK */, response.getStatus());

        assertNull(m_cache.get(1));
        assertNull(m_cache.get(2));
        }

    @Test
    public void shouldRejectRawAndAllowAliasesInDev()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            EntrySetResource resource = new EntrySetResource(m_cache, Collections.singleton(1), Person.class);
            resource.setExpressionAliases(createExpressionAliases());

            assertEquals(400 /* Bad Request */, resource.getValues("dateOfBirth").getStatus());
            assertEquals(200 /* OK */, resource.getValues("dob").getStatus());

            resource.m_aggregatorRegistry = new com.tangosol.coherence.rest.util.aggregator.AggregatorRegistry();
            resource.m_processorRegistry  = new com.tangosol.coherence.rest.util.processor.ProcessorRegistry();

            assertEquals(400 /* Bad Request */, resource.aggregate("long-sum(dateOfBirth)").getStatus());
            assertEquals(200 /* OK */, resource.aggregate("long-sum(age)").getStatus());

            assertEquals(400 /* Bad Request */, resource.process("increment(dateOfBirth,1)").getStatus());
            assertEquals(200 /* OK */, resource.process("increment(age,1)").getStatus());
            }
        }

    protected ExpressionAliasConfig createExpressionAliases()
        {
        return ExpressionAliasConfig.builder()
                .addProjectionAlias("dob", "dateOfBirth")
                .addAggregatorArgumentAlias("long-sum", "age", "age")
                .addProcessorArgumentAlias("increment", "age", "age")
                .build();
        }

    // ---- data members ----------------------------------------------------

    protected NamedCache m_cache;
    }
