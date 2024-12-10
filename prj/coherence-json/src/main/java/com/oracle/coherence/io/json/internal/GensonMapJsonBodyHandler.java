/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.GensonBuilder;

import com.tangosol.net.management.MapJsonBodyHandler;

import java.io.InputStream;
import java.io.OutputStream;

import java.util.Map;

/**
 * An implementation of {@link MapJsonBodyHandler} that uses
 * Genson to serialize and deserialze json maps.
 *
 * @author Jonathan Knight 2022.02.10
 * @since 22.06
 */
public class GensonMapJsonBodyHandler
        implements MapJsonBodyHandler
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Must have a default constructor as this class is loaded
     * via the {@link java.util.ServiceLoader}.
     */
    public GensonMapJsonBodyHandler()
        {
        }

    // ----- MapJsonBodyHandler methods -------------------------------------
    @Override
    public void write(Map<String, Object> body, OutputStream out)
        {
        s_genson.serialize(body, out);
        }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> readMap(InputStream in)
        {
        return s_genson.deserialize(in, Map.class);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The {@link Genson} instance to use.
     */
    public static final Genson s_genson = new GensonBuilder().create();
    }
