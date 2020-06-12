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
import com.oracle.coherence.io.json.genson.Wrapper;

import com.oracle.coherence.io.json.genson.convert.ChainedFactory;

import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

import com.tangosol.io.Serializer;

import com.tangosol.util.ExternalizableHelper;

import java.lang.reflect.Type;

/**
 * A converter that performs pre- and post-processing of objects during
 * serialization, in order to handle lambdas and objects that implement
 * {@link com.tangosol.io.SerializationSupport} and/or
 * {@link com.tangosol.io.SerializerAware} interface.
 *
 * @author Aleksandar Seovic  2018.05.30
 * @since 14.1.2
 */
public class SerializationSupportConverter
        extends Wrapper<Converter<Object>>
        implements Converter<Object>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new {@code SerializationSupportConverter}.
     *
     * @param nextConverter  the {@link Converter} to wrap
     * @param serializer     the {@link Serializer} to use
     */
    protected SerializationSupportConverter(Converter<Object> nextConverter, Serializer serializer)
        {
        super(nextConverter);

        this.f_serializer = serializer;
        }

    // ----- Converter interface --------------------------------------------

    @Override
    public void serialize(Object object, ObjectWriter writer, Context ctx)
            throws Exception
        {
        object = ExternalizableHelper.replace(object);
        wrapped.serialize(object, writer, ctx);
        }

    @Override
    public Object deserialize(ObjectReader reader, Context ctx)
            throws Exception
        {
        Object object = wrapped.deserialize(reader, ctx);
        return ExternalizableHelper.realize(object, f_serializer);
        }

    // ----- inner class: Factory -------------------------------------------

    /**
     * A factory to create {@link SerializationSupportConverter} instances.
     */
    public static class Factory
            extends ChainedFactory
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link Factory}.
         *
         * @param serializer  the {@link Serializer} to use
         */
        public Factory(Serializer serializer)
            {
            this.f_serializer = serializer;
            }

        // ----- ChainedFactory methods -------------------------------------

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        protected Converter<?> create(Type type, Genson genson, Converter<?> nextConverter)
            {
            return new SerializationSupportConverter((Converter) nextConverter, f_serializer);
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Serializer}.
         */
        protected final Serializer f_serializer;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Serializer}.
     */
    protected final Serializer f_serializer;
    }
