/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link EntrySetResource}.
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
        m_cache.put(2, new Person("Aleks", new Date(74, 7, 24)));
        m_cache.put(3, new Person("Vaso", new Date(74, 7, 7)));
        m_cache.put(1, new Person("Ivan", new Date(78, 3, 25)));
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
        Response         response = resource.getValues(com.tangosol.coherence.rest.util.PropertySet.fromString("dateOfBirth"));

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

    // ---- data members ----------------------------------------------------

    protected NamedCache m_cache;
    }
