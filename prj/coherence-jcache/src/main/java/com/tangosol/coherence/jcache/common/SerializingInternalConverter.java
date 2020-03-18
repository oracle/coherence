/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import com.tangosol.io.Serializer;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

/**
 * An {@link InternalConverter} that converts values to and from their
 * Coherence-based serialized {@link Binary} representation.
 *
 * @param <T>  the type of value to convert
 *
 * @author jf  2013.10.24
 * @since Coherence 12.1.3
 */
public class SerializingInternalConverter<T>
        implements InternalConverter<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link SerializingInternalConverter}.
     *
     * @param serializer   the {@link Serializer} to use for conversion
     */
    public SerializingInternalConverter(Serializer serializer)
        {
        m_serializer = serializer;
        }

    // ----- InternalConverter methods -----------------------------

    @Override
    public Object toInternal(T value)
        {
        if (value == null)
            {
            return null;
            }
        else
            {
            return ExternalizableHelper.toBinary(value, m_serializer);
            }
        }

    @Override
    public T fromInternal(Object object)
        {
        if (object == null)
            {
            return null;
            }
        else
            {
            return (T) ExternalizableHelper.fromBinary((Binary) object, m_serializer);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Serializer} to use for serializing and deserializing objects.
     */
    private final Serializer m_serializer;
    }
