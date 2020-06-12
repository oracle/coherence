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


import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.annotation.HandleBeanView;
import com.oracle.coherence.io.json.genson.annotation.HandleClassMetadata;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

/**
 * Base converter for serializing/deserializing a {@link TemporalAccessor} type.
 */
@HandleClassMetadata
@HandleBeanView
abstract class BaseTemporalAccessorConverter<T extends TemporalAccessor>
        implements Converter<T> {
    private DateTimeConverterOptions options;

    private TimestampHandler<T> timestampHandler;

    private TemporalQuery<T> query;

    BaseTemporalAccessorConverter(DateTimeConverterOptions options,
                                  TimestampHandler<T> timestampHandler,
                                  TemporalQuery<T> query) {
        this.options = options;
        this.timestampHandler = timestampHandler;
        this.query = query;
    }

    @Override
    public void serialize(T object, ObjectWriter writer, Context ctx) {
        if (options.isAsTimestamp()) {
            TimestampFormat timestampFormat = options.getTimestampFormat();

            switch (timestampFormat) {
            case MILLIS:
            case NANOS:
                timestampHandler.writeNumericTimestamp(object, writer, timestampFormat);
                break;
            case OBJECT:
                timestampHandler.writeObjectTimestamp(object, writer);
                break;
            case ARRAY:
                timestampHandler.writeArrayTimestamp(object, writer);
                break;
            default:
            }
        }
        else {
            writer.writeValue(options.getDateTimeFormatter().format(object));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T deserialize(ObjectReader reader, Context ctx) {
        T obj = null;
        if (options.isAsTimestamp()) {
            TimestampFormat timestampFormat = options.getTimestampFormat();
            switch (timestampFormat) {
            case MILLIS:
            case NANOS:
                obj = timestampHandler.readNumericTimestamp(reader, options.getTimestampFormat());
                break;
            case OBJECT:
                obj = timestampHandler.readObjectTimestamp(reader);
                break;
            case ARRAY:
                obj = timestampHandler.readArrayTimestamp(reader);
                break;
            default:
            }
        }
        else {
            obj = options.getDateTimeFormatter().parse(reader.valueAsString(), query);
            if (obj instanceof OffsetDateTime) {
                obj = (T) DateTimeUtil.correctOffset((OffsetDateTime) obj, options.getZoneId());
            }
        }

        return obj;
    }
}
