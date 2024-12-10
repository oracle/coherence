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

/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Copyright 2011-2014 Genson - Cepoi Eugen
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.oracle.coherence.io.json.genson.datetime;


import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

/**
 * Base class to handle the parsing/serializing of different timestamp formats.
 */
abstract class TimestampHandler<T extends TemporalAccessor> {
    private Function<T, Long> toMillis;

    private Function<Long, T> fromMillis;

    private Function<T, Long> toNanos;

    private Function<Long, T> fromNanos;

    private LinkedHashMap<String, TemporalField> temporalFields;

    private Supplier<T> instanceProvider;

    TimestampHandler(Function<T, Long> toMillis,
                     Function<Long, T> fromMillis,
                     Function<T, Long> toNanos,
                     Function<Long, T> fromNanos,
                     LinkedHashMap<String, TemporalField> temporalFields,
                     Supplier<T> instanceProvider) {
        this.toMillis = toMillis;
        this.fromMillis = fromMillis;
        this.toNanos = toNanos;
        this.fromNanos = fromNanos;
        this.temporalFields = temporalFields;
        this.instanceProvider = instanceProvider;
    }

    void writeNumericTimestamp(T object, ObjectWriter writer, TimestampFormat timestampFormat) {
        if (timestampFormat == TimestampFormat.MILLIS) {
            writer.writeValue(toMillis.apply(object));
        }
        else {
            writer.writeValue(toNanos.apply(object));
        }
    }

    void writeObjectTimestamp(T object, ObjectWriter writer) {
        writer.beginObject();
        writeFieldsAsObject(object, writer);
        writer.endObject();
    }

    protected void writeFieldsAsObject(T object, ObjectWriter writer) {
        for (Map.Entry<String, TemporalField> temporalFieldEntry : temporalFields.entrySet()) {
            String jsonName = temporalFieldEntry.getKey();
            TemporalField field = temporalFieldEntry.getValue();
            long value = object.getLong(field);
            writer.writeName(jsonName);
            writer.writeValue(value);
        }
    }

    void writeArrayTimestamp(T object, ObjectWriter writer) {
        writer.beginArray();
        writeFieldsAsArray(object, writer);
        writer.endArray();
    }

    protected void writeFieldsAsArray(T object, ObjectWriter writer) {
        for (TemporalField field : temporalFields.values()) {
            long value = object.getLong(field);
            writer.writeValue(value);
        }
    }

    final T readNumericTimestamp(ObjectReader reader, TimestampFormat timestampFormat) {
        long value = reader.valueAsLong();
        Function<Long, T> numberToInstance = timestampFormat == TimestampFormat.MILLIS ? fromMillis : fromNanos;
        if (numberToInstance == null) {
            throw new IllegalArgumentException("Timestamp format not supported");
        }
        return numberToInstance.apply(value);
    }

    final T readArrayTimestamp(ObjectReader reader) {
        reader.beginArray();
        T obj = readFieldsFromArray(instanceProvider, reader);
        reader.endArray();
        return obj;
    }

    @SuppressWarnings("unchecked")
    protected T readFieldsFromArray(Supplier<T> instanceProvider, ObjectReader reader) {
        Temporal obj = (Temporal) instanceProvider.get();

        for (TemporalField temporalField : temporalFields.values()) {
            if (reader.hasNext()) {
                reader.next();
                long value = reader.valueAsLong();
                obj = obj.with(temporalField, value);
            }
        }

        return (T) obj;
    }

    final T readObjectTimestamp(ObjectReader reader) {
        reader.beginObject();
        T obj = readFieldsFromObject(instanceProvider, reader);
        reader.endObject();
        return obj;
    }

    protected T readFieldsFromObject(Supplier<T> instanceProvider, ObjectReader reader) {
        T obj = instanceProvider.get();
        while (reader.hasNext()) {
            reader.next();
            obj = readFieldFromObject(obj, reader);
        }
        return obj;
    }

    @SuppressWarnings("unchecked")
    protected T readFieldFromObject(T obj, ObjectReader reader) {
        Temporal objTemporal = (Temporal) obj;
        String jsonName = reader.name();
        TemporalField field = temporalFields.get(jsonName);
        long value = reader.valueAsLong();
        return (T) objTemporal.with(field, value);
    }
}
