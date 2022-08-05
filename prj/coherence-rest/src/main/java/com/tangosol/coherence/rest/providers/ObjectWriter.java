/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.providers;

import com.tangosol.coherence.rest.io.Marshaller;
import com.tangosol.coherence.rest.io.MarshallerRegistry;

import com.tangosol.util.SubList;

import java.io.IOException;
import java.io.OutputStream;

import java.lang.annotation.Annotation;

import java.lang.reflect.Type;

import jakarta.inject.Inject;

import jakarta.ws.rs.WebApplicationException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

/**
 * Provider responsible for marshalling Java objects.
 *
 * @author as  2011.07.15
 */
@Provider
public class ObjectWriter
        implements MessageBodyWriter<Object>
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public ObjectWriter()
        {
        }

    /**
     * Construct an ObjectWriter instance.
     *
     * @param registry  marshaller registry to use
     */
    public ObjectWriter(MarshallerRegistry registry)
        {
        m_marshallerRegistry = registry;
        }

    // ----- MessageBodyWriter interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean isWriteable(Class<?> clz, Type type, Annotation[] aAnnotation, MediaType mediaType)
        {
        // When SubList contains 1 element, some how this provider is selected.  Return false
        // to let the XmlCollectionWriter to handle it.
        if (SubList.class.isAssignableFrom(clz))
            {
            return false;
            }
        return m_marshallerRegistry.getMarshaller(clz, mediaType) != null;
        }

    /**
     * {@inheritDoc}
     */
    public long getSize(Object obj, Class<?> clz, Type type, Annotation[] aAnnotation,
            MediaType mediaType)
        {
        return -1;
        }

    /**
     * {@inheritDoc}
     */
    public void writeTo(Object obj, Class<?> clz, Type type, Annotation[] aAnnotation,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream stream)
            throws IOException, WebApplicationException
        {
        Marshaller marshaller = m_marshallerRegistry.getMarshaller(obj == null ? clz : obj.getClass(), mediaType);
        marshaller.marshal(obj, stream, httpHeaders);
        }

    // ----- data members ---------------------------------------------------

    /**
     * Marshaller registry to obtain marshallers from.
     */
    @Inject
    private MarshallerRegistry m_marshallerRegistry;
    }
