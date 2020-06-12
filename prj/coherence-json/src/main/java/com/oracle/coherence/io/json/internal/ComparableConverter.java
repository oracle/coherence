/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.Genson;

import com.oracle.coherence.io.json.genson.convert.DefaultConverters.NumberConverter;

import com.oracle.coherence.io.json.genson.reflect.TypeUtil;

import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;
import com.oracle.coherence.io.json.genson.stream.ValueType;

import java.lang.reflect.Type;

/**
 * A {@link Converter} for {@link Comparable} instances.  Specifically, this handles the case
 * where the raw value being processed is a JSON String, Number, or Boolean. If the type
 * being handled isn't any of those, allow other {@link Converter converters} to process the value.
 *
 * @since 14.1.2
 */
@SuppressWarnings("rawtypes")
public class ComparableConverter
        implements Converter<Comparable>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a new {@code ComparableConverter}.
     */
    protected ComparableConverter()
        {
        }

    // ----- Converter interface --------------------------------------------

    @Override
    public Comparable deserialize(final ObjectReader reader, final Context ctx)
            throws Exception
        {
        ValueType type = reader.getValueType();
        if (type == ValueType.STRING)
            {
            return reader.valueAsString();
            }
        else if (type == ValueType.INTEGER || type == ValueType.DOUBLE)
            {
            return (Comparable) NumberConverter.instance.deserialize(reader, ctx);
            }
        else if (reader.getValueType() == ValueType.BOOLEAN)
            {
            return reader.valueAsBoolean();
            }
        else
            {
            // this shouldn't happen
            throw new IllegalStateException();
            }
        }

    @Override
    public void serialize(final Comparable object, final ObjectWriter writer, final Context ctx)
            throws Exception
        {
        if (object instanceof String)
            {
            writer.writeValue(object.toString());
            }
        else if (object instanceof Number)
            {
            writer.writeValue((Number) object);
            }
        else if (object instanceof Boolean)
            {
            writer.writeValue((Boolean) object);
            }
        }

    /**
     * A factory for creating {@code ComparableConverter}s.
     */
    public static class Factory
            implements com.oracle.coherence.io.json.genson.Factory<Converter<Comparable>>
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
        public Converter<Comparable> create(Type type, Genson genson)
            {
            return ((TypeUtil.getRawClass(type) == Comparable.class)
                    ? ComparableConverter.INSTANCE
                    : null);
            }

        // ----- constants --------------------------------------------------

        /**
         * The singleton instance of the factory.
         */
        public static final Factory INSTANCE = new Factory();
        }

    // ----- constants ------------------------------------------------------

    /**
     * Singleton instance for {@code ComparableConverter}.
     */
    public static final ComparableConverter INSTANCE = new ComparableConverter();
    }
