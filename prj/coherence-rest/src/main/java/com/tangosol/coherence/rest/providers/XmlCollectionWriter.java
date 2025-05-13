/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
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

import jakarta.inject.Inject;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;

/**
 * Provider responsible for converting Java collections to a XML formatted
 * stream.
 *
 * @author vp 2011.06.29
 */
@Provider
@Produces(APPLICATION_XML)
public class XmlCollectionWriter
        implements MessageBodyWriter<Collection<?>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public XmlCollectionWriter()
        {
        }

    /**
     * Construct an XmlCollectionWriter instance.
     *
     * @param registry  marshaller registry to use
     */
    public XmlCollectionWriter(MarshallerRegistry registry)
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
    public long getSize(Collection<?> col, Class<?> clz, Type type, Annotation[] aAnnotation,
            MediaType mediaType)
        {
        return -1;
        }

    @Override
    public void writeTo(Collection<?> oCol, Class<?> clz, Type type, Annotation[] aAnnotation,
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

        out.print("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        out.print("<collection>");

        for (Object o : oCol)
            {
            if (o instanceof Map.Entry)
                {
                entryWriter.writeTo(o, o.getClass(), null, null, mediaType, null, stream);
                }
            else if (o != null)
                {
                Marshaller marshaller = registry.getMarshaller(o.getClass(), mediaType);
                marshaller.marshalAsFragment(o, stream, httpHeaders);
                }
            }

        out.print("</collection>");
        }

    // ---- data members ----------------------------------------------------

    /**
     * Marshaller registry to obtain element marshallers from.
     */
    @Inject
    protected MarshallerRegistry m_marshallerRegistry;
    }
