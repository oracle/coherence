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


import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

class DateTimeConverterOptions {
    private final DateTimeFormatter dateTimeFormatter;

    private final boolean asTimestamp;

    private final TimestampFormat timestampFormat;

    private final ZoneId zoneId;

    /**
     * Options to use when creating a {@link com.oracle.coherence.io.json.genson.Converter} for a {@link java.time.temporal.TemporalAccessor} type.
     *
     * @param clazz             The class to which the converter applies
     * @param dateTimeFormatter The {@link DateTimeFormatter} to use for the {@link java.time.temporal.TemporalAccessor} type
     * @param asTimestamp       Whether values of the specified type should be serialized/deserialized as timestamps
     * @param timestampFormat   The {@link TimestampFormat} to use if asTimestamp is true
     * @param zoneId            The default {@link ZoneId} to use when parsing
     */
    DateTimeConverterOptions(Class<?> clazz,
                             DateTimeFormatter dateTimeFormatter,
                             boolean asTimestamp,
                             TimestampFormat timestampFormat,
                             ZoneId zoneId) {
        this.dateTimeFormatter = dateTimeFormatter == null
                ? null
                : DateTimeUtil.createFormatterWithDefaults(dateTimeFormatter, zoneId);
        this.asTimestamp = asTimestamp;
        this.timestampFormat = timestampFormat;
        // Instant should always be in UTC timezone
        this.zoneId = clazz == Instant.class ? ZoneId.of("UTC") : zoneId;
    }

    public DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }

    public boolean isAsTimestamp() {
        return asTimestamp;
    }

    public TimestampFormat getTimestampFormat() {
        return timestampFormat;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }
}
