/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.io;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.tangosol.coherence.rest.providers.JacksonMapperProvider;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Jackson-based marshaller that marshals object to/from JSON.
 *
 * @author as  2011.07.13
 */
public class JacksonJsonMarshaller<T>
        extends AbstractMarshaller<T>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct JacksonJsonMarshaller instance.
     *
     * @param clzRoot  class of the root object this marshaller is for
     */
    public JacksonJsonMarshaller(Class<T> clzRoot)
        {
        super(clzRoot);

        m_objectMapper = JacksonMapperProvider.getObjectMapper();
        }

    // ---- Marshaller implementation ---------------------------------------

    @Override
    public void marshal(T value, OutputStream out, MultivaluedMap<String, Object> httpHeaders) throws IOException
        {
        m_objectMapper.writeValue(out, value);
        }

    @Override
    public T unmarshal(InputStream in, MediaType mediaType) throws IOException
        {
        return m_objectMapper.readValue(in, getRootClass());
        }

    // ---- data members ----------------------------------------------------

    /**
     * Object mapper to use for marshalling.
     */
    private final ObjectMapper m_objectMapper;
    }
