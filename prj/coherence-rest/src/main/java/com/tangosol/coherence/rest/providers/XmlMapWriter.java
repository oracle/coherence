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
import java.util.Set;

import javax.inject.Inject;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.MediaType.APPLICATION_XML;

/**
 * Provider responsible for converting Java maps to an XML formatted stream.
 *
 * @author vp 2011.07.18
 */
@Provider
@Produces(APPLICATION_XML)
public class XmlMapWriter
        implements MessageBodyWriter<Map>
    {
    // ----- MessageBodyWriter interface ------------------------------------

    @Override
    public boolean isWriteable(Class<?> clz, Type type, Annotation[] aAnnotations,
            MediaType mediaType)
        {
        return Map.class.isAssignableFrom(clz);
        }

    @Override
    public long getSize(Map map, Class<?> aClass, Type type, Annotation[] aAnnotations,
            MediaType mediaType)
        {
        return -1;
        }

    @Override
    public void writeTo(Map map, Class<?> clz, Type type, Annotation[] aAnnotation,
            MediaType mediaType,MultivaluedMap<String, Object> httpHeaders,
            OutputStream stream)
            throws IOException, WebApplicationException
        {
        MarshallerRegistry registry = m_marshallerRegistry;
        if (registry == null)
            {
            throw new IllegalStateException("MarshallerRegistry not configured");
            }

        PrintStream out = new PrintStream(stream);
        out.print("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        out.print("<map>");

        for (Map.Entry entry : (Set<Map.Entry>) map.entrySet())
            {
            Object oKey   = entry.getKey();
            Object oValue = entry.getValue();

            out.print("<entry><key>");
            Marshaller marshaller = registry.getMarshaller(oKey.getClass(), mediaType);
            marshaller.marshalAsFragment(oKey, stream, httpHeaders);

            out.print("</key><value>");
            marshaller = registry.getMarshaller(oValue.getClass(), mediaType);
            marshaller.marshalAsFragment(oValue, stream, httpHeaders);

            out.print("</value></entry>");
            }

        out.print("</map>");
        }

    // ---- data members ----------------------------------------------------

    /**
     * Marshaller registry to obtain element marshallers from.
     */
    @Inject
    protected MarshallerRegistry m_marshallerRegistry;
    }
