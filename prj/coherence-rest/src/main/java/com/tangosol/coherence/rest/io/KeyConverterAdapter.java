/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.io;

import com.tangosol.coherence.rest.KeyConverter;

import com.tangosol.util.Base;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Marshaller that marshalls objects using a cache's key converter.
 *
 * @author vp 2011.07.26
 */
public class KeyConverterAdapter
        implements Marshaller<Object>
    {

    // ---- constructors ----------------------------------------------------

    /**
     * Construct KeyConverterAdapter instance.
     *
     * @param converter  key converter used to marshall key objects
     */
    public KeyConverterAdapter(KeyConverter converter)
        {
        if (converter == null)
            {
            throw new IllegalArgumentException("null converter");
            }
        m_keyConverter = converter;
        }

    // ---- Marshaller implementation ---------------------------------------

    @Override
    public void marshal(Object oValue, OutputStream out, MultivaluedMap<String, Object> httpHeaders)
        throws IOException
        {
        PrintStream stream = new PrintStream(out);
        stream.print(m_keyConverter.toString(oValue));
        stream.flush();
        }

    @Override
    public Object unmarshal(InputStream in, MediaType mediaType)
        throws IOException
        {
        String sInput = Base.read(new InputStreamReader(in));
        return m_keyConverter.fromString(sInput);
        }

    // ---- data members ----------------------------------------------------

    /**
     * Key converter.
     */
    private final KeyConverter m_keyConverter;
    }