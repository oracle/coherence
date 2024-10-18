/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.ai.QueryResult;
import com.oracle.coherence.ai.search.SimpleQueryResult;
import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.GenericType;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

/**
 * A converter to serialize a {@link QueryResult} to json.
 */
@SuppressWarnings("rawtypes")
public class QueryResultConverter
        implements Converter<QueryResult>
    {
    @Override
    public void serialize(QueryResult result, ObjectWriter writer, Context ctx) throws Exception
        {
        writer.beginObject();
        writer.writeNumber("distance", result.getDistance());
        writer.writeName("key");
        ctx.genson.serialize(result.getKey(), writer, ctx);
        writer.writeName("value");
        ctx.genson.serialize(result.getValue(), writer, ctx);
        writer.endObject();
        }

    @Override
    public QueryResult deserialize(ObjectReader reader, Context ctx) throws Exception
        {
        double distance = 0.0d;
        Object key      = null;
        Object value    = null;

        reader.beginObject();
        while (reader.hasNext())
            {
            reader.next();
            switch (reader.name())
                {
                case "distance":
                    distance = reader.valueAsDouble();
                    break;
                case "key":
                    key = ctx.genson.deserialize(TYPE, reader, ctx);
                    break;
                case "value":
                    value = ctx.genson.deserialize(TYPE, reader, ctx);
                    break;
                }
            }
        reader.endObject();
        return new SimpleQueryResult<>(distance, key, value);
        }

    /**
     * {@link GenericType} for {@link Object}.
     */
    private static final GenericType<Object> TYPE = GenericType.of(Object.class);

    /**
     * The singleton instance for {@code QueryResultConverter}.
     */
    public static final QueryResultConverter INSTANCE = new QueryResultConverter();
    }
