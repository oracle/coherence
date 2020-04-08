/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for JsonMap.
 *
 * @author as  2015.08.25
 */
public class JsonMapTest
    {
    public static final String JSON_STRING = "{\"name\":\"Aleks\",\"age\":41}";

    @Test
    public void testJsonMarshalling() throws Exception
        {
        ObjectMapper mapper = new ObjectMapper();

        JsonMap map = new JsonMap();
        map.put("name", "Aleks");
        map.put("age", 41);

        assertEquals(JSON_STRING, mapper.writeValueAsString(map));
        }

    @Test
    public void testJsonUnmarshalling() throws Exception
        {
        ObjectMapper mapper = new ObjectMapper();

        JsonMap map = mapper.readValue(JSON_STRING, JsonMap.class);

        assertEquals("Aleks", map.get("name"));
        assertEquals(41, map.get("age"));
        }

    @Test
    public void testDefaultSerialization() throws Exception
        {
        JsonMap map = new JsonMap();
        map.put("name", "Aleks");
        map.put("age", 41);

        assertEquals(map, roundTrip(map, new DefaultSerializer()));
        }

    @Test
    public void testPofSerialization() throws Exception
        {
        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(1, JsonMap.class, new JsonMap.Serializer());

        JsonMap map = new JsonMap();
        map.put("name", "Aleks");
        map.put("age", 41);

        assertEquals(map, roundTrip(map, ctx));
        }

    private Object roundTrip(JsonMap map, Serializer serializer)
        {
        return ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(map, serializer), serializer);
        }
    }
