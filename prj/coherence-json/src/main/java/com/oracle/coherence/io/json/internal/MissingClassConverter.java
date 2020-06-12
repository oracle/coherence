/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json.internal;


import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.JsonBindingException;
import com.oracle.coherence.io.json.genson.Wrapper;

import com.oracle.coherence.io.json.genson.convert.ChainedFactory;
import com.oracle.coherence.io.json.genson.convert.ClassMetadataConverter;

import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

import java.lang.reflect.Type;

/**
 * A converter that catches {@link ClassNotFoundException} thrown by the
 * {@link ClassMetadataConverter} and deserializes JSON into a default
 * object type.
 *
 * @author Aleks Seovic  2018.07.04
* @since 20.06
 */
public class MissingClassConverter
        extends Wrapper<Converter<Object>>
        implements Converter<Object>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a new {@code MissingClassConverter}.
     *
     * @param nextConverter  the converter to wrap
     */
    protected MissingClassConverter(Converter<Object> nextConverter)
        {
        super(nextConverter);
        }

    // ----- Converter interface --------------------------------------------

    @Override
    public void serialize(Object object, ObjectWriter writer, Context ctx)
            throws Exception
        {
        wrapped.serialize(object, writer, ctx);
        }

    @Override
    public Object deserialize(ObjectReader reader, Context ctx)
            throws Exception
        {
        try
            {
            return wrapped.deserialize(reader, ctx);
            }
        catch (JsonBindingException e)
            {
            if (e.getCause() instanceof ClassNotFoundException)
                {
                return JsonObjectConverter.INSTANCE.deserialize(reader, ctx);
                }
            throw e;
            }
        }

    // ----- inner class: Factory -------------------------------------------

    /**
     * A factory to create {@link MissingClassConverter} instances.
     */
    public static class Factory
            extends ChainedFactory
        {
        // ----- constructors -----------------------------------------------

        /**
         * Creates a new {@code Factory}.
         */
        protected Factory()
            {
            }

        // ----- Factory interface ------------------------------------------

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        protected Converter<?> create(Type type, Genson genson, Converter<?> nextConverter)
            {
            return new MissingClassConverter((Converter) nextConverter);
            }

        // ----- constants --------------------------------------------------

        /**
         * Singleton {@link Factory} instance.
         */
        public static final Factory INSTANCE = new Factory();
        }
    }
