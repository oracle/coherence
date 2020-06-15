/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.metrics.internal;


import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;


/**
 * A {@code javax.ws.rs.ext.MessageBodyWriter} implementation that
 * uses {@link MetricsFormatter} to write a set of metrics directly
 * to the response stream.
 *
 * @author as  2019.06.29
 * @since 12.2.1.4.0
 */
@Provider
public class MetricsWriter
        implements MessageBodyWriter<MetricsFormatter>
    {
    @Override
    public boolean isWriteable(Class<?> cls, Type type, Annotation[] annotations, MediaType mediaType)
        {
        return MetricsFormatter.class.isAssignableFrom(cls);
        }

    @Override
    public long getSize(MetricsFormatter formatter, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType)
        {
        return -1;
        }

    @Override
    public void writeTo(MetricsFormatter formatter, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap<String, Object> multivaluedMap, OutputStream out)
            throws IOException, WebApplicationException
        {
        try (Writer writer = new OutputStreamWriter(out))
            {
            formatter.writeMetrics(writer);
            }
        }
    }
