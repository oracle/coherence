/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.LiteMap;
import com.tangosol.util.Versionable;

import java.io.IOException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A simple wrapper around {@link LiteMap} that preserves the order of elements
 * and adds support for POF serialization without fidelity loss in order to
 * support JSON pass-through.
 *
 * @author as  2015.08.25
 */
public class JsonMap
        extends LiteMap<String, Object>
        implements Versionable<Integer>
    {
    // ---- constructors ----------------------------------------------------

    /**
    * Construct a JsonMap.
    */
    public JsonMap()
        {
        }

    /**
    * Construct a JsonMap with the same mappings as the given map.
    *
    * @param map the map whose mappings are to be placed in this map.
    */
    public JsonMap(Map<String, ?> map)
        {
        super(map);
        }

    // ---- LiteMap methods -------------------------------------------------

    @Override
    protected Map<String, Object> instantiateMap()
        {
        // we'll use LinkedHashMap in delegation mode in order to preserve
        // the order of attributes in a JSON document
        return new LinkedHashMap<>();
        }

    @Override
    @JsonAnySetter
    public Object put(String key, Object value)
        {
        return super.put(key, value);
        }

    // ---- Versionable interface -------------------------------------------

    @Override
    public Integer getVersionIndicator()
        {
        return (Integer) get("@version");
        }

    @Override
    public void incrementVersion()
        {
        computeIfPresent("@version", (k, v) -> ((int) v) + 1);
        }

    // ---- inner class: Serializer -----------------------------------------

    /**
     * POF serializer for JsonMap.
     */
    public static class Serializer
            implements PofSerializer<JsonMap>
        {
        @Override
        public void serialize(PofWriter out, JsonMap value) throws IOException
            {
            out.writeMap(0, value);
            out.writeRemainder(null);
            }

        @Override
        public JsonMap deserialize(PofReader in) throws IOException
            {
            try
                {
                JsonMap map = (JsonMap) in.getPofContext().getClass(in.getUserTypeId()).newInstance();
                in.readMap(0, map);
                in.readRemainder();

                return map;
                }
            catch (Exception e)
                {
                throw new IOException(e);
                }
            }
        }

    private static final long serialVersionUID = 336675295275632710L;
    }
