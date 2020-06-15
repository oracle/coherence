/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.Serializer;

import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

import com.tangosol.util.Versionable;

/**
 * {@link Serializer} for {@link Versionable} that delegates the serialization to another {@link Converter}
 * based on the raw type as {@link Versionable} itself has no serializable properties.
 *
 * @since 20.06
 */
@SuppressWarnings("rawtypes")
public class VersionableSerializer
        implements Serializer<Versionable>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a new {@code VersionableSerializer}.
     */
    protected VersionableSerializer()
        {
        }

    // ----- Serializer interface -------------------------------------------

    @Override
    public void serialize(Versionable object, ObjectWriter writer, Context ctx) throws Exception
        {
        ctx.genson.serialize(object, object.getClass(), writer, ctx);
        }

    // ----- constants ------------------------------------------------------

    /**
     * Singleton {@code VersionableSerializer} instance.
     */
    public static final VersionableSerializer INSTANCE = new VersionableSerializer();
    }
