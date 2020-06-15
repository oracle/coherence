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

import java.util.Set;

import javax.inject.Inject;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.MediaType.APPLICATION_XML;

/**
 * Provider responsible for converting set of keys to a XML formatted
 * stream.
 *
 * @author si 2016.05.17
 */
@Provider
@Produces(APPLICATION_XML)
public class XmlKeysWriter
        implements MessageBodyWriter<Set>
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public XmlKeysWriter()
        {
        }

    /**
     * Construct an XmlKeysWriter instance.
     *
     * @param registry  marshaller registry to use
     */
    public XmlKeysWriter(MarshallerRegistry registry)
        {
        m_marshallerRegistry = registry;
        }

    // ----- MessageBodyWriter interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean isWriteable(Class<?> clz, Type type, Annotation[] aAnnotation,
            MediaType mediaType)
        {
        for (Annotation anno : aAnnotation)
            {
            if (Path.class.equals(anno.annotationType()) &&
                "keys".equals(((Path) anno).value()))
                {
                return true;
                }
            }
        return false;
        }

    /**
     * {@inheritDoc}
     */
    public long getSize(Set set, Class<?> clz, Type type, Annotation[] aAnnotation,
            MediaType mediaType)
        {
        return -1;
        }

    /**
     * {@inheritDoc}
     */
    public void writeTo(Set set, Class<?> clz, Type type, Annotation[] aAnnotation,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream stream)
            throws IOException, WebApplicationException
        {
        MarshallerRegistry registry = m_marshallerRegistry;
        if (registry == null)
            {
            throw new IllegalStateException("MarshallerRegistry not configured");
            }

        PrintStream out = new PrintStream(stream);
        out.print("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        out.print("<keys>");

        for (Object o : set)
            {
            if (o != null)
                {
                out.print("<key>");
                Marshaller marshaller = registry.getMarshaller(o.getClass(), mediaType);
                marshaller.marshalAsFragment(o, stream, httpHeaders);
                out.print("</key>");
                }
            }

        out.print("</keys>");
        }

    // ---- data members ----------------------------------------------------

    /**
     * Marshaller registry to obtain element marshallers from.
     */
    @Inject
    protected MarshallerRegistry m_marshallerRegistry;
    }
