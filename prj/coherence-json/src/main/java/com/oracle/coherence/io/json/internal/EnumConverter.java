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

import com.oracle.coherence.io.json.genson.reflect.TypeUtil;

import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

/**
 * A converter of enums.
 *
 * @param <T>  the type of the enum
 *
 * @since 20.06
 */
public class EnumConverter<T extends Enum<T>>
        implements Converter<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create an enum converter.
     *
     * @param enumClass  the type of the enum
     */
    protected EnumConverter(Class<T> enumClass)
        {
        this.f_clzEnum = enumClass;
        f_mapDeserializationNames = new HashMap<>();

        Arrays.stream(enumClass.getFields())
                .filter(Field::isEnumConstant)
                .forEach(this::acceptField);
        }

    // ---- Converter interface ---------------------------------------------

    @Override
    public void serialize(T obj, ObjectWriter writer, Context ctx)
        {
        writer.beginObject().writeString("enum", obj.name()).endObject();
        }

    @Override
    public T deserialize(ObjectReader reader, Context ctx)
        {
        String name = null;

        reader.beginObject();
        while (reader.hasNext())
            {
            reader.next();
            if ("enum".equals(reader.name()))
                {
                name = reader.valueAsString();
                }
            }
        reader.endObject();

        T value = f_mapDeserializationNames.get(name);
        if (value == null)
            {
            throw new JsonBindingException("No enum constant " + f_clzEnum.getCanonicalName() + "." + name);
            }

        return value;
        }

    /**
     * Helper method to allow making a field reflection call within a Java stream.
     *
     * @param f  the reflection {@link Field}
     */
    @SuppressWarnings("unchecked")
    private void acceptField(Field f)
        {
        try
            {
            f_mapDeserializationNames.put(f.getName(), (T) f.get(null));
            }
        catch (IllegalAccessException e)
            {
            throw new JsonBindingException("Failed to get enum value " + f.getName(), e);
            }
        }

    // ----- inner class: Factory -------------------------------------------

    /**
     * A factory to create {@code EnumConverter} instances.
     */
    public static class Factory
            implements com.oracle.coherence.io.json.genson.Factory<Converter<? extends Enum<?>>>
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct a new {@code Factory} instance.
         */
        protected Factory()
            {
            }

        // ----- Factory interface ------------------------------------------

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public Converter<Enum<?>> create(Type type, Genson genson)
            {
            Class<?> rawClass = TypeUtil.getRawClass(type);
            return rawClass.isEnum() || Enum.class.isAssignableFrom(rawClass)
                   ? new EnumConverter(rawClass)
                   : null;
            }

        // ----- constants --------------------------------------------------

        /**
         * The singleton factory instance.
         */
        public static final Factory INSTANCE = new Factory();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The enum type.
     */
    protected final Class<T> f_clzEnum;

    /**
     * Mapping of a logical name (used for serialization purposes) to the proper enum value.
     */
    protected final Map<String, T> f_mapDeserializationNames;
    }

