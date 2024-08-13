/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal;


import com.tangosol.internal.metrics.MetricsFormatter;

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
 * @since 14.1.2.0
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
