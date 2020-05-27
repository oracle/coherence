/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json.internal;

import java.lang.reflect.Type;

import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.Genson;

import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

/**
 * A {@link Converter} for {@link Class} instances.
 *
 * @since 14.1.2
 */
public class ClassConverter
        implements Converter<Class<?>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new {@code ClassConverter}.
     */
    protected ClassConverter()
        {
        }

    // ----- Converter interface --------------------------------------------

    @Override
    public void serialize(Class<?> object, ObjectWriter writer, Context ctx) throws Exception
        {
        writer.writeValue(object.getCanonicalName());
        }

    @Override
    public Class<?> deserialize(ObjectReader reader, Context ctx) throws Exception
        {
        return Thread.currentThread().getContextClassLoader().loadClass(reader.valueAsString());
        }

    // ----- inner class: Factory -------------------------------------------

    /**
     * A factory for creating {@code ClassConverter}s.
     */
    public static class Factory
            implements com.oracle.coherence.io.json.genson.Factory<Converter<Class<?>>>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code Factory}.
         */
        protected Factory()
            {
            }

        // ----- Factory interface ------------------------------------------

        @Override
        public Converter<Class<?>> create(Type type, Genson genson)
            {
            return ClassConverter.INSTANCE;
            }

        // ----- constants --------------------------------------------------

        /**
         * The singleton instance of {@code Factory}.
         */
        public static final Factory INSTANCE = new Factory();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The singleton instance for {@code ClassConverter}.
     */
    public static final ClassConverter INSTANCE = new ClassConverter();
    }
