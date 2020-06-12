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

import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

import com.tangosol.util.NullImplementation;

/**
 * A {@link Genson} {@link Converter} for {@link NullImplementation.NullSet}.
 *
 * @since 14.1.2
 */
public class NullSetConverter
        implements Converter<NullImplementation.NullSet>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a new {@code NullSetConverter}.
     */
    protected NullSetConverter()
        {
        }

    // ----- Converter interface --------------------------------------------

    @Override
    public NullImplementation.NullSet deserialize(ObjectReader reader, Context ctx)
        {
        return (NullImplementation.NullSet) NullImplementation.getSet();
        }

    @Override
    public void serialize(NullImplementation.NullSet object, ObjectWriter writer, Context ctx)
        {
        writer.beginObject().endObject();
        }

    // ----- constants ------------------------------------------------------

    /**
     * Singleton {@code NullSetConverter} instance.
     */
    public static final NullSetConverter INSTANCE = new NullSetConverter();
    }
