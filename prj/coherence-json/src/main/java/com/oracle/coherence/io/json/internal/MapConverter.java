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

import com.oracle.coherence.io.json.genson.annotation.HandleClassMetadata;

import com.oracle.coherence.io.json.genson.reflect.TypeUtil;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

import java.lang.reflect.Type;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Map converter.
 * <p>
 * <em>NOTE:</em> this {@link Converter} produces a serialization format that is incompatible
 * with other JSON parsers.
 *
 * @param <K> the type of the map keys
 * @param <V> the type of the map values
 *
 * @author Aleks Seovic  2018.05.30
* @since 20.06
 */
@HandleClassMetadata
public class MapConverter<K, V>
        implements Converter<Map<K, V>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new {@code MapConverter} using the provided {@link Converter converters}
     * to convert keys and values.
     *
     * @param convKey    the {@link Converter} for keys
     * @param convValue  the {@link Converter} for values
     */
    protected MapConverter(Converter<K> convKey, Converter<V> convValue)
        {
        this.f_convKey   = convKey;
        this.f_convValue = convValue;
        }

    // ----- Converter interfaces -------------------------------------------

    @Override
    public void serialize(Map<K, V> object, ObjectWriter writer, Context ctx)
            throws Exception
        {
        writer.beginObject();
        boolean isSorted  = object instanceof SortedMap;
        boolean isOrdered = object instanceof LinkedHashMap;
        if (isSorted)
            {
            writer.writeMetadata("sorted", true);
            }
        else if (isOrdered)
            {
            writer.writeMetadata("ordered", true);
            }

        writer.writeName("entries").beginArray();
        for (Map.Entry<K, V> entry : object.entrySet())
            {
            writer.beginObject().writeName("key");
            f_convKey.serialize(entry.getKey(), writer, ctx);
            writer.writeName("value");
            f_convValue.serialize(entry.getValue(), writer, ctx);
            writer.endObject();
            }
        writer.endArray().endObject();
        }

    @Override
    @SuppressWarnings("unchecked")
    public Map<K, V> deserialize(ObjectReader reader, Context ctx)
            throws Exception
        {
        Map<K, V> map;
        reader.nextObjectMetadata();
        boolean isSorted  = (Boolean) reader.metadata().getOrDefault("sorted", false);
        boolean isOrdered = (Boolean) reader.metadata().getOrDefault("ordered", false);
        String  className = (String) reader.metadata().getOrDefault("class", getMapTypeBasedOnFlags(isSorted,
                                                                                                    isOrdered));
        try
            {
            Class<? extends Map<K, V>> mapClass = (Class<? extends Map<K, V>>) ctx.genson.classFor(className);
            map = mapClass.getDeclaredConstructor().newInstance();
            }
        catch (Exception e)
            {
            map = isSorted
                  ? new TreeMap<>()
                  : isOrdered
                      ? new LinkedHashMap<>()
                      : new HashMap<>();
            }

        reader.next();
        if ("entries".equals(reader.name()))
            {
            reader.beginArray();
            while (reader.hasNext())
                {
                reader.next();
                reader.beginObject();
                K key   = null;
                V value = null;
                while (reader.hasNext())
                    {
                    reader.next();
                    if ("key".equals(reader.name()))
                        {
                        key = f_convKey.deserialize(reader, ctx);
                        }
                    else if ("value".equals(reader.name()))
                        {
                        value = f_convValue.deserialize(reader, ctx);
                        }
                    }
                map.put(key, value);
                reader.endObject();
                }
            reader.endArray();
            }
        reader.endObject();
        return map;
        }

    /**
     * Return the map type (as string) based on the provided flags.
     *
     * @param isSorted   flag indicating the map should be sorted
     * @param isOrdered  flag indicating the map should be ordered
     *
     * @return the map type (as string) based on the provided flags.
     */
    private static String getMapTypeBasedOnFlags(boolean isSorted, boolean isOrdered)
        {
        return isSorted
               ? TreeMap.class.getName()
               : isOrdered
                   ? LinkedHashMap.class.getName()
                   : HashMap.class.getName();
        }

    /**
     * A {@link MapConverter} factory.
     */
    public static class Factory
            implements com.oracle.coherence.io.json.genson.Factory<Converter<? extends Map<?, ?>>>
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
        public Converter<? extends Map<?, ?>> create(Type type, Genson genson)
            {
            Type expandedType = type;
            if (TypeUtil.getRawClass(type).getTypeParameters().length == 0)
                {
                expandedType = TypeUtil.expandType(TypeUtil.lookupGenericType(Map.class, TypeUtil.getRawClass(type)),
                                                   type);
                }

            Type keyType   = TypeUtil.typeOf(0, expandedType);
            Type valueType = TypeUtil.typeOf(1, expandedType);

            return new MapConverter(genson.provideConverter(keyType), genson.provideConverter(valueType));
            }

        // ----- constants --------------------------------------------------

        /**
         * Singleton {@code Factory} instance.
         */
        public static final Factory INSTANCE = new Factory();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Converter} for the {@link Map} keys.
     */
    protected final Converter<K> f_convKey;

    /**
     * The {@link Converter} for the {@link Map} values.
     */
    protected final Converter<V> f_convValue;
    }
