/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;

import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

import java.math.RoundingMode;

/**
 * A {@link Converter} for {@link RoundingMode} objects.
 *
 * @author rl  2023.1.10
 * @since 22.06.2
 */
public class RoundingModeConverter
        implements Converter<RoundingMode>
    {
    // ----- constructors ----------------------------------------------------

    /**
     * Private constructor; use {@link #INSTANCE}.
     */
    private RoundingModeConverter()
        {
        }

    // ----- Converter interface ---------------------------------------------

    @Override
    public void serialize(RoundingMode roundingMode, ObjectWriter writer, Context ctx)
            throws Exception
        {
        writer.writeValue(roundingMode.name());
        }

    @Override
    public RoundingMode deserialize(ObjectReader reader, Context ctx)
            throws Exception
        {
        return RoundingMode.valueOf(reader.valueAsString());
        }

    // ----- constants ---------------------------------------------------

    /**
     * Singleton {@code RoundingModeConverter}.
     */
    public static final RoundingModeConverter INSTANCE = new RoundingModeConverter();
    }
