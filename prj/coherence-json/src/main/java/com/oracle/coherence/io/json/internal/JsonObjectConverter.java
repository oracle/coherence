/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.io.json.JsonObject;

import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.GenericType;

import com.oracle.coherence.io.json.genson.annotation.HandleClassMetadata;

import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

import java.util.Map;

/**
 * A {@link Converter} for {@link JsonObject} instances.
 *
 * @since 14.1.2
 */
@HandleClassMetadata
public class JsonObjectConverter
        implements Converter<JsonObject>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a new {@code JsonObjectConverter}.
     */
    protected JsonObjectConverter()
        {
        }

    // ----- Converter interface --------------------------------------------

    @Override
    public void serialize(JsonObject obj, ObjectWriter writer, Context ctx) throws Exception
        {
        writer.beginObject();

        String className = obj.getClassName();
        if (className != null)
            {
            writer.writeMetadata("class", className);
            }
        if (obj.isVersioned())
            {
            writer.writeMetadata("version", (long) obj.getVersionIndicator());
            }

        for (Map.Entry<String, Object> entry : obj.entrySet())
            {
            writer.writeName(entry.getKey());
            ctx.genson.serialize(entry.getValue(), writer, ctx);
            }

        writer.endObject();
        }

    @Override
    public JsonObject deserialize(ObjectReader reader, Context ctx) throws Exception
        {
        JsonObject obj = new JsonObject();

        reader.nextObjectMetadata();
        if (reader.metadata().containsKey("class"))
            {
            obj.setClassName(reader.metadata("class"));
            }
        if (reader.metadata().containsKey("version"))
            {
            obj.setVersion(reader.metadataAsLong("version").intValue());
            }

        while (reader.hasNext())
            {
            reader.next();
            obj.put(reader.name(), ctx.genson.deserialize(TYPE, reader, ctx));
            }

        reader.endObject();
        return obj;
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link GenericType} for {@link Object}.
     */
    private static final GenericType<Object> TYPE = GenericType.of(Object.class);

    /**
     * The singleton instance for {@code JsonObjectConverter}.
     */
    public static final JsonObjectConverter INSTANCE = new JsonObjectConverter();
    }
