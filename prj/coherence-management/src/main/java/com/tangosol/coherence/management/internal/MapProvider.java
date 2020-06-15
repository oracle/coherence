/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
        implements MessageBodyWriter<Map>, MessageBodyReader<Map>
    {
    // ----- MessageBodyWriter methods --------------------------------------

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
        {
        return Map.class.equals(type) || LinkedHashMap.class.equals(type);
        }

    @Override
    public long getSize(Map map, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
        {
        return 0;
        }

    @Override
    public void writeTo(Map                            map,
                        Class<?>                       type,
                        Type                           genericType,
                        Annotation[]                   annotations,
                        MediaType                      mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream                   entityStream) throws IOException, WebApplicationException
        {
        marshaller.writeValue(entityStream, map);
        }

    // ----- MessageBodyReader methods --------------------------------------

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
        {
        return Map.class.equals(type) || LinkedHashMap.class.equals(type);
        }

    @Override
    public Map readFrom(Class<Map>                     type,
                        Type                           genericType,
                        Annotation[]                   annotations,
                        MediaType                      mediaType,
                        MultivaluedMap<String, String> httpHeaders,
                        InputStream                    entityStream) throws IOException, WebApplicationException
        {
        return marshaller.readValue(entityStream, LinkedHashMap.class);
        }

    // ----- helper methods -------------------------------------------------

    private static ObjectMapper createDefaultMapper()
        {
        ObjectMapper mapper = new ObjectMapper();

        mapper.configure(MapperFeature.AUTO_DETECT_FIELDS, false);
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        mapper.configure(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME, true);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);
        mapper.configure(MapperFeature.USE_GETTERS_AS_SETTERS, false);
        mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);

        return mapper;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link ObjectMapper} to use to marshall {@link Map}s to and from json.
     */
    private static ObjectMapper marshaller = createDefaultMapper();
    }
