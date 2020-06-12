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

import com.oracle.coherence.io.json.genson.stream.JsonWriter;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Converter} for {@link Throwable}s.
 *
 * @author Jonathan Knight  2018.11.28
* @since 20.06
 */
public class ThrowableConverter
        implements Converter<Throwable>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a new {@code ThrowableConverter}.
     */
    protected ThrowableConverter()
        {
        }

    // ----- Converter interface --------------------------------------------

    @Override
    public void serialize(Throwable throwable, ObjectWriter writer, Context context) throws Exception
        {
        // This is a horrible hack because the HandleMetadataAnnotation is fundamentally broken.
        ((JsonWriter) writer).clearMetadata();
        writer.beginObject();
        writer.writeMetadata("class", JsonPortableException.class.getCanonicalName());
        writer.writeString("name", throwable.getClass().getName());
        writer.writeString("message", throwable.getMessage());

        writer.writeName("stack").beginArray();
        for (StackTraceElement element : throwable.getStackTrace())
            {
            writer.writeValue(element.toString());
            }

        writer.endArray();
        writer.endObject();
        }

    @Override
    public Throwable deserialize(ObjectReader in, Context context) throws Exception
        {
        String name = JsonPortableException.class.getName();
        String message = null;
        List<String> stack = new ArrayList<>();

        in.beginObject();

        while (in.hasNext())
            {
            in.next();
            switch (in.name())
                {
                case "name":
                    name = in.valueAsString();
                    break;
                case "message":
                    message = in.valueAsString();
                    break;
                case "stack":
                    in.beginArray();
                    while (in.hasNext())
                        {
                        in.next();
                        stack.add(in.valueAsString());
                        }
                    in.endArray();
                    break;
                default:
                }
            }

        return new JsonPortableException(name, message, stack.toArray(new String[0]));
        }

    // ----- inner class: Factory -------------------------------------------

    /**
     * A {@link com.oracle.coherence.io.json.genson.Factory} for creating instances
     * of {@link ThrowableConverter}.
     */
    public static class Factory
            implements com.oracle.coherence.io.json.genson.Factory<Converter<? extends Throwable>>
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
        public Converter<? extends Throwable> create(Type type, Genson genson)
            {
            return ThrowableConverter.INSTANCE;
            }

        // ----- Constants --------------------------------------------------

        /**
         * A singleton {@code Factory} instance.
         */
        public static final Factory INSTANCE = new Factory();
        }

    // ----- constants ------------------------------------------------------

    /**
     * A singleton {@code ThrowableConverter} instance.
     */
    public static final ThrowableConverter INSTANCE = new ThrowableConverter();
    }
