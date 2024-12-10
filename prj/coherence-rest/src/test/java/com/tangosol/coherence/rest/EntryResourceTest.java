/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.tangosol.coherence.rest.io.Marshaller;
import com.tangosol.coherence.rest.io.MarshallerRegistry;

import com.tangosol.coherence.rest.util.PartialObject;
import com.tangosol.coherence.rest.util.PropertySet;

import com.tangosol.coherence.rest.util.processor.ProcessorRegistry;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.WrapperNamedCache;

import data.pof.Address;
import data.pof.Person;
import data.pof.PortablePerson;
import data.pof.VersionablePerson;
import data.pof.VersionablePortablePerson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link EntryResource}.
 *
 * @author ic  2011.06.29
 */
@SuppressWarnings({"unchecked", "deprecation"})
public class EntryResourceTest
    {
    protected Person     m_person;
    protected NamedCache m_cache;

    @Before
    public void setUp()
        {
        m_cache = new WrapperNamedCache(new HashMap<Integer, Person>(), "persons");
        m_cache.put(1, m_person = Person.create());
        }

    @Test
    public void testGet()
        {
        EntryResource resource = createEntryResource(m_cache, 1, Person.class);
        Response      response = resource.get(null, GET_REQUEST);

        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(m_person, response.getEntity());
        }

    @Test
    public void testVersionableMarshalling()
            throws IOException
        {
        VersionablePortablePerson person = VersionablePortablePerson.create();
        assertEquals(1, person.getVersionIndicator());

        MarshallerRegistry mr = new MarshallerRegistry();
        Marshaller         m  = mr.getMarshaller(VersionablePortablePerson.class, MediaType.APPLICATION_JSON);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.marshal(person, out, null);

        byte[] binPerson = out.toByteArray();
        System.out.println(new String(binPerson));

        VersionablePortablePerson person2 =
                (VersionablePortablePerson) m.unmarshal(new ByteArrayInputStream(binPerson), null);
        assertEquals(person, person2);
        assertEquals(1, person2.getVersionIndicator());
        }

    @Test
    public void testConditionalGet()
        {
        VersionablePortablePerson person = VersionablePortablePerson.create();
        m_cache.put(2, person);

        EntryResource resource = createEntryResource(m_cache, 2, VersionablePortablePerson.class);
        Response      response = resource.get(null, createRequest("GET", "1"));

        assertEquals(304 /* Not Modified */, response.getStatus());
        assertEquals(null, response.getEntity());

        person.incrementVersion();
        m_cache.put(2, person);
        response = resource.get(null, createRequest("GET", "1"));

        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(person, response.getEntity());
        }

    @Test
    public void testGetMissing()
        {
        EntryResource resource = createEntryResource(m_cache, 2, Person.class);
        Response      response = resource.get(null, GET_REQUEST);

        assertEquals(404 /* Not Found */, response.getStatus());
        assertFalse(m_cache.containsKey(2));
        assertNull(m_cache.get(2));
        }

    @Test
    public void testGetNull()
        {
        m_cache.put(2, null);
        EntryResource resource = createEntryResource(m_cache, 2, Person.class);
        Response      response = resource.get(null, GET_REQUEST);

        assertEquals(404 /* Not Found */, response.getStatus());
        assertNull(response.getEntity());
        assertTrue(m_cache.containsKey(2));
        assertNull(m_cache.get(2));
        }

    @Test
    public void testGetPartial()
        {
        EntryResource resource = createEntryResource(m_cache, 1, Person.class);
        Response      response = resource.get(PropertySet
                .fromString("name,dateOfBirth,address:(city,state)"), GET_REQUEST);

        assertEquals(200 /* OK */, response.getStatus());

        PartialObject person = (com.tangosol.coherence.rest.util.PartialObject) response.getEntity();
        assertEquals("Aleksandar Seovic", person.get("name"));
        assertEquals(new Date(74, 7, 24), person.get("dateOfBirth"));

        PartialObject address = (com.tangosol.coherence.rest.util.PartialObject) person.get("address");
        assertEquals("Tampa", address.get("city"));
        assertEquals("FL", address.get("state"));
        assertNull(address.get("street"));
        }

    @Test
    public void testDelete()
        {
        assertNotNull(m_cache.get(1));
        Response response = createEntryResource(m_cache, 1, Person.class).delete();

        assertEquals(200 /* OK */, response.getStatus());
        assertFalse(m_cache.containsKey(1));
        }

    @Test
    public void testDeleteMissing()
        {
        assertNull(m_cache.get(-1));
        Response response = createEntryResource(m_cache, -1, Person.class).delete();

        assertEquals(404 /* Not Found */, response.getStatus());
        assertFalse(m_cache.containsKey(-1));
        }

    @Test
    public void testDeleteNull()
        {
        m_cache.put(2, null);
        Response response = createEntryResource(m_cache, 2, Person.class).delete();

        assertEquals(404 /* NOT FOUND */, response.getStatus());
        assertFalse(m_cache.containsKey(2));
        }

    @Test
    public void testPutJson()
        throws JAXBException
        {
        String        jsonPerson = "{ \"@type\": \".Person\", \"name\": \"Ivan\", \"address\": {\"city\": \"Santiago\"} }";
        EntryResource resource   = createEntryResource(m_cache, 2, Person.class);
        Response      response   = resource.put(HEADERS_JSON, new ByteArrayInputStream(jsonPerson.getBytes()));

        assertEquals(200 /* OK */, response.getStatus());
        assertEquals("Ivan", ((Person) m_cache.get(2)).getName());
        assertEquals("Santiago", ((Person) m_cache.get(2)).getAddress().getCity());
        }


    @Test
    public void testPutXml()
        throws JAXBException
        {
        String        xmlPerson = "<person><name>Ivan</name><address><city>Belgrade</city></address></person>";
        EntryResource resource  = createEntryResource(m_cache, 2, PortablePerson.class);

        Response      response  = resource.put(HEADERS_XML, new ByteArrayInputStream(xmlPerson.getBytes()));

        assertEquals(200 /* OK */, response.getStatus());
        assertEquals("Ivan", ((Person) m_cache.get(2)).getName());
        assertEquals("Belgrade", ((Person) m_cache.get(2)).getAddress().getCity());
        }

    @Test
    public void testPutJsonConflict()
        throws JAXBException
        {
        VersionablePerson person = new VersionablePerson("Ivan", new Date(78, 3, 25));
        person.setAddress(new Address("N/A", "Belgrade", "SRB", "11000"));
        person.incrementVersion();
        m_cache.put(2, person);

        String        jsonPerson = "{ \"@type\": \".VersionablePerson\", \"name\": \"Ivan\", \"address\": {\"city\": \"Santiago\"} }";
        EntryResource resource   = createEntryResource(m_cache, 2, VersionablePerson.class);
        Response      response   = resource.put(HEADERS_JSON, new ByteArrayInputStream(jsonPerson.getBytes()));

        assertEquals(409 /* Conflict */, response.getStatus());
        assertEquals("Belgrade", ((Person) m_cache.get(2)).getAddress().getCity());
        }

    // ----- helpers --------------------------------------------------------

    protected static Request createRequest(final String sMethod, final String sEtag)
        {
        return new Request()
            {
            public String getMethod()
                {
                return sMethod;
                }

            public Variant selectVariant(List<Variant> variants)
                    throws IllegalArgumentException
                {
                return null;
                }

            public Response.ResponseBuilder evaluatePreconditions(EntityTag entityTag)
                {
                if (sEtag == null)
                    {
                    return null;
                    }
                EntityTag eTag = new EntityTag(sEtag);
                return eTag.equals(entityTag) ? Response.notModified(eTag) : null;
                }

            public Response.ResponseBuilder evaluatePreconditions(Date date)
                {
                return null;
                }

            public Response.ResponseBuilder evaluatePreconditions(Date date,
                                                                  EntityTag entityTag)
                {
                return null;
                }

            public Response.ResponseBuilder evaluatePreconditions()
                {
                return null;
                }
            };
        }

    protected static HttpHeaders createHttpHeaders(final MediaType mediaType)
        {
        return new HttpHeaders()
            {
            @Override
            public List<String> getRequestHeader(String s)
                {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

            @Override
            public MultivaluedMap<String, String> getRequestHeaders()
                {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

            @Override
            public List<MediaType> getAcceptableMediaTypes()
                {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

            @Override
            public List<Locale> getAcceptableLanguages()
                {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

            @Override
            public MediaType getMediaType()
                {
                return mediaType;
                }

            @Override
            public Locale getLanguage()
                {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

            @Override
            public Map<String, Cookie> getCookies()
                {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

            @Override
            public int getLength()
                {
                return 0;
                }

            @Override
            public Date getDate()
                {
                return null;
                }

            @Override
            public String getHeaderString(String s)
                {
                return null;    //To change body of implemented methods use File | Settings | File Templates.
                }
            };
        }

    // ---- helper methods --------------------------------------------------
    
    protected EntryResource createEntryResource(NamedCache cache, Object oKey, Class clzValue)
        {
        EntryResource resource = new EntryResource(cache, oKey, clzValue);

        resource.m_processorRegistry  = new ProcessorRegistry();
        resource.m_marshallerRegistry = new MarshallerRegistry();

        return resource;
        }


    // ----- constants ------------------------------------------------------

    private static final Request     GET_REQUEST  = createRequest("GET", null);
    private static final HttpHeaders HEADERS_JSON = createHttpHeaders(MediaType.APPLICATION_JSON_TYPE);
    private static final HttpHeaders HEADERS_XML  = createHttpHeaders(MediaType.APPLICATION_XML_TYPE);
    }
