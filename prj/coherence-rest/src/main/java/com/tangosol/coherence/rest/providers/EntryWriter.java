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

import java.util.Map;

import javax.inject.Inject;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;


/**
 * Provider responsible for marshalling map entries.
 *
 * @author as  2015.07.22
 */
@Provider
@Produces({"application/xml", "application/json"})
public class EntryWriter
        implements MessageBodyWriter<Object>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public EntryWriter()
        {
        }

    /**
     * Construct an EntryWriter instance.
     *
     * @param registry  marshaller registry to use
     */
    public EntryWriter(MarshallerRegistry registry)
        {
        m_marshallerRegistry = registry;
        }

    // ----- MessageBodyWriter interface ------------------------------------

    @Override
    public boolean isWriteable(Class<?> clz, Type type, Annotation[] aAnnotation, MediaType mediaType)
        {
        return Map.Entry.class.isAssignableFrom(clz);
        }

    @Override
    public long getSize(Object entry, Class<?> clz, Type type, Annotation[] aAnnotation, MediaType mediaType)
        {
        return -1;
        }

    @SuppressWarnings("unchecked")
    @Override
    public void writeTo(Object oEntry, Class<?> clz, Type type, Annotation[] aAnnotation,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream stream)
            throws IOException, WebApplicationException
        {
        Map.Entry entry  = (Map.Entry) oEntry;
        Object    oKey   = entry.getKey();
        Object    oValue = entry.getValue();

        Marshaller keyMarshaller   = m_marshallerRegistry.getMarshaller(oKey.getClass(), mediaType);
        Marshaller valueMarshaller = m_marshallerRegistry.getMarshaller(oValue.getClass(), mediaType);

        PrintStream out = new PrintStream(stream);

        if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE))
            {
            out.print("{\"key\":");
            keyMarshaller.marshalAsFragment(oKey, stream, httpHeaders);
            out.print(",\"value\":");
            valueMarshaller.marshalAsFragment(oValue, stream, httpHeaders);
            out.print("}");
            }
        else if (mediaType.equals(MediaType.APPLICATION_XML_TYPE))
            {
            out.print("<entry><key>");
            keyMarshaller.marshalAsFragment(oKey, stream, httpHeaders);
            out.print("</key><value>");
            valueMarshaller.marshalAsFragment(oValue, stream, httpHeaders);
            out.print("</value></entry>");
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * Marshaller registry to obtain marshallers from.
     */
    @Inject
    private MarshallerRegistry m_marshallerRegistry;
    }
