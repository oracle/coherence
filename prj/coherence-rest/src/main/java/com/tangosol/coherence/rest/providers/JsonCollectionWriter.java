/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.providers;

import com.tangosol.coherence.rest.io.Marshaller;
import com.tangosol.coherence.rest.io.MarshallerRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import java.lang.annotation.Annotation;

import java.lang.reflect.Type;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Provider responsible for converting Java collections to a JSON formatted
 * stream.
 *
 * @author vp 2011.06.29
 */
@Provider
@Produces(APPLICATION_JSON)
public class JsonCollectionWriter
        implements MessageBodyWriter<Object>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public JsonCollectionWriter()
        {
        }

    /**
     * Construct a JsonCollectionWriter instance.
     *
     * @param registry  marshaller registry to use
     */
    public JsonCollectionWriter(MarshallerRegistry registry)
        {
        m_marshallerRegistry = registry;
        }

    // ----- MessageBodyWriter interface ------------------------------------

    @Override
    public boolean isWriteable(Class<?> clz, Type type, Annotation[] aAnnotation,
            MediaType mediaType)
        {
        return Collection.class.isAssignableFrom(clz);
        }

    @Override
    public long getSize(Object col, Class<?> clz, Type type, Annotation[] aAnnotation,
            MediaType mediaType)
        {
        return -1;
        }

    @Override
    public void writeTo(Object oCol, Class<?> clz, Type type, Annotation[] aAnnotation,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream stream)
            throws IOException, WebApplicationException
        {
        MarshallerRegistry registry = m_marshallerRegistry;
        if (registry == null)
            {
            throw new IllegalStateException("MarshallerRegistry not configured");
            }

        EntryWriter entryWriter = new EntryWriter(registry);
        PrintStream out         = new PrintStream(stream);
        out.print("[");

        boolean first = true;
        for (Object o : (Collection) oCol)
            {
            if (o != null)
                {
                if (first)
                    {
                    first = false;
                    }
                else
                    {
                    out.print(',');
                    }

                if (o instanceof Map.Entry)
                    {
                    entryWriter.writeTo(o, o.getClass(), null, null, mediaType, null, stream);
                    }
                else
                    {
                    Marshaller marshaller = registry.getMarshaller(o.getClass(), mediaType);
                    marshaller.marshalAsFragment(o, stream, httpHeaders);
                    }
                }
            }

        out.print("]");
        }

    // ---- data members ----------------------------------------------------

    /**
     * Marshaller registry to obtain element marshallers from.
     */
    @Inject
    protected MarshallerRegistry m_marshallerRegistry;
    }
