/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.providers;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.tangosol.coherence.config.Config;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * Provider responsible for converting Java object to/from JSON/XML.
 *
 * @author lh 2014.07.18
 *
 * @since Coherence 12.2.1
 */
@Provider
public class JacksonMapperProvider 
    implements ContextResolver<ObjectMapper>
    {
    // ----- constructors ---------------------------------------------------

    public JacksonMapperProvider()
        {
        }

    // ----- ContextResolver interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    public ObjectMapper getContext(Class<?> type)
        {
            return m_defaultObjectMapper;
        }

    public static ObjectMapper getObjectMapper()
        {
            return m_defaultObjectMapper;
        }

    private static ObjectMapper createDefaultMapper()
        {
        final ObjectMapper mapper = new ObjectMapper();

        JaxbAnnotationModule module = new JaxbAnnotationModule();
        mapper.registerModule(module);

        mapper.configure(SerializationFeature.INDENT_OUTPUT,
                         Config.getBoolean(com.tangosol.coherence.rest.io.Marshaller.FORMAT_OUTPUT));
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
    // ---- data members ----------------------------------------------------
    
    static final ObjectMapper m_defaultObjectMapper = createDefaultMapper();
    }
