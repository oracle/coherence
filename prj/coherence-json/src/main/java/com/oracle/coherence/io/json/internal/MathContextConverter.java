/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.JsonBindingException;

import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

import java.math.MathContext;
import java.math.RoundingMode;


/**
 * A {@link Converter} for {@link MathContext} objects.
 *
 * @author rl  2023.1.10
 * @since 22.06.2
 */
public class MathContextConverter
        implements Converter<MathContext>
    {
    // ----- constructors ----------------------------------------------------

    /**
     * Private constructor; use {@link #INSTANCE}.
     */
    private MathContextConverter()
        {
        }

    // ----- Converter interface ---------------------------------------------

    @Override
    public void serialize(MathContext mathContext, ObjectWriter writer, Context ctx)
            throws Exception
        {
        writer.beginObject();
        writer.writeName("precision");
        writer.writeValue(mathContext.getPrecision());
        writer.writeName("roundingMode");
        RoundingModeConverter.INSTANCE.serialize(mathContext.getRoundingMode(), writer, ctx);
        writer.endObject();
        }

    @Override
    public MathContext deserialize(ObjectReader reader, Context ctx)
            throws Exception
        {
        int          precision = -1;
        RoundingMode roundMode = null;

        reader.beginObject();
        while (reader.hasNext())
            {
            reader.next();
            String name = reader.name();
            if ("precision".equals(name))
                {
                precision = reader.valueAsInt();
                if (precision < 0)
                    {
                    throw new JsonBindingException("Unable to deserialize java.math.MathContext;"
                                                   + " invalid value [" + precision + "] for precision");
                    }
                }
            else if ("roundingMode".equals(name))
                {
                roundMode = RoundingModeConverter.INSTANCE.deserialize(reader, ctx);
                }
            }
        reader.endObject();

        if (precision == -1 && roundMode == null)
            {
            throw new JsonBindingException("Unable to deserialize java.math.MathContext; no attributes present in object");
            }
        else if (precision == -1)
            {
            throw new JsonBindingException("Unable to deserialize java.math.MathContext; roundMode attribute is"
                                           + " present, but the precision attribute is not");
            }
        else if (roundMode == null)
            {
            return new MathContext(precision);
            }
        else
            {
            return new MathContext(precision, roundMode);
            }
        }

    // ----- constants ---------------------------------------------------

    /**
     * Singleton {@code MatchContextConverter}.
     */
    public static final MathContextConverter INSTANCE = new MathContextConverter();
    }
