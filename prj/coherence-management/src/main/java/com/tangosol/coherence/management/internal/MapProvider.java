/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.tangosol.net.management.MapJsonBodyHandler;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A {@link MessageBodyWriter} and {@link MessageBodyReader} for {@link Map}
 * or {@link LinkedHashMap}.
 * <p>
 * When converting json to a {@link Map} this class will produce instances
 * of {@link LinkedHashMap}.
 *
 * @author jk  2019.05.31
 */
@Provider
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
public class MapProvider
        implements MessageBodyWriter<Map<?, ?>>, MessageBodyReader<Map<?, ?>>
    {
    // ----- MessageBodyWriter methods --------------------------------------

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
        {
        return Map.class.equals(type) || LinkedHashMap.class.equals(type);
        }

    @Override
    public long getSize(Map<?, ?> map, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
        {
        return 0;
        }

    @Override
    @SuppressWarnings("unchecked")
    public void writeTo(Map<?, ?>                      map,
                        Class<?>                       type,
                        Type                           genericType,
                        Annotation[]                   annotations,
                        MediaType                      mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream                   entityStream) throws IOException, WebApplicationException
        {
        f_handler.write((Map<String, Object>) map, entityStream);
        }

    // ----- MessageBodyReader methods --------------------------------------

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
        {
        return Map.class.equals(type) || LinkedHashMap.class.equals(type);
        }

    @Override
    public Map<?, ?> readFrom(Class<Map<?, ?>>               type,
                              Type                           genericType,
                              Annotation[]                   annotations,
                              MediaType                      mediaType,
                              MultivaluedMap<String, String> httpHeaders,
                              InputStream                    entityStream) throws IOException, WebApplicationException
        {
        return f_handler.readMap(entityStream);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The {@link ObjectMapper} to use to marshall {@link Map}s to and from json.
     */
    private static final MapJsonBodyHandler f_handler = MapJsonBodyHandler.ensureMapJsonBodyHandler();
    }
