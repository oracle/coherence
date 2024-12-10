/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
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
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.management.MapJsonBodyHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A {@link MapJsonBodyHandler} implementation that will use Jackson
 * to process json, if Jackson is on the classpath.
 *
 * @author Jonathan Knight  2020.02.23
 * @since 22.06
 */
public class JacksonMapJsonBodyHandler
        implements MapJsonBodyHandler
    {
    @Override
    public void write(Map<String, Object> body, OutputStream out)
        {
        ensureDelegate().write(body, out);
        }

    @Override
    public Map<String, Object> readMap(InputStream in)
        {
        return ensureDelegate().readMap(in);
        }

    @Override
    public boolean isEnabled()
        {
        return ensureDelegate().isEnabled();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create the delegate {@link MapJsonBodyHandler} to use.
     * <p>
     * If Jackson is on the classpath then a {@link JacksonDelegate} will be
     * returned, otherwise a {@link DisabledDelegate} will be returned.
     *
     * @return the delegate {@link MapJsonBodyHandler} to use
     */
    private MapJsonBodyHandler ensureDelegate()
        {
        if (m_delegate == null)
            {
            synchronized (this)
                {
                if (m_delegate == null)
                    {
                    try
                        {
                        m_delegate = new JacksonDelegate();
                        }
                    catch (Throwable t)
                        {
                        Logger.finest("Disabling " + getClass() + " due to " + t.getMessage());
                        m_delegate = new DisabledDelegate();
                        }
                    }
                }
            }
        return m_delegate;
        }

    // ----- inner class: JacksonDelegate -----------------------------------

    /**
     * A {@link MapJsonBodyHandler} implementation that will use Jackson
     * to process json, if Jackson is on the classpath.
     */
    private static class JacksonDelegate
            implements MapJsonBodyHandler
        {
        // ----- constructors -----------------------------------------------

        public JacksonDelegate()
            {
            f_mapper = createDefaultMapper();
            }

        // ----- MapJsonBodyHandler methods ---------------------------------
        @Override
        public void write(Map<String, Object> body, OutputStream out)
            {
            try
                {
                f_mapper.writeValue(out, body);
                }
            catch (IOException e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            }

        @SuppressWarnings("unchecked")
        @Override
        public Map<String, Object> readMap(InputStream in)
            {
            try
                {
                return f_mapper.readValue(in, LinkedHashMap.class);
                }
            catch (IOException e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            }

        // ----- helper methods ---------------------------------------------

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

        // ----- data members -----------------------------------------------

        /**
         * The Jackson {@link ObjectMapper} to use.
         */
        private final ObjectMapper f_mapper;
        }

    // ----- inner class: DisabledDelegate ----------------------------------

    /**
     * A {@link MapJsonBodyHandler} that is disabled.
     */
    private static class DisabledDelegate
            implements MapJsonBodyHandler
        {
        @Override
        public boolean isEnabled()
            {
            return false;
            }

        @Override
        public void write(Map<String, Object> body, OutputStream out)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public Map<String, Object> readMap(InputStream in)
            {
            throw new UnsupportedOperationException();
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The delegate {@link MapJsonBodyHandler} to use.
     */
    private volatile MapJsonBodyHandler m_delegate;
    }
