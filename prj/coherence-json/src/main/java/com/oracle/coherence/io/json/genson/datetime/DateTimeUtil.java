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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

class DateTimeUtil {
    private static final long THOUSAND = 1_000L;

    private static final long MILLION = 1_000_000L;

    private static final long BILLION = 1_000_000_000L;

    /**
     * Private constructor for utility class.
     */
    private DateTimeUtil() {
    }

    /**
     * Default parsing values to apply to {@link DateTimeFormatter} objects.
     *
     * <p>This allows for partial parsing of types that expect additional fields.
     * For example, a value of '2011-11'02' can be parsed to a {@link java.time.ZonedDateTime} by having the time portion
     * defaulted to '00:00:00'</p>
     */
    private static final DateTimeFormatter DEFAULTS =
            new DateTimeFormatterBuilder()
                    .parseDefaulting(ChronoField.YEAR, 2000)
                    .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
                    .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                    .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                    .toFormatter();

    /**
     * Applies parse defaulting to the passed in {@link DateTimeFormatter} and sets the default {@link ZoneId}
     * to the value provided.
     */
    static DateTimeFormatter createFormatterWithDefaults(DateTimeFormatter formatter, ZoneId zoneId) {
        //Trying to apply zoneId and zoneOffset defaults to ISO_INSTANT causes parsing to throw exceptions
        //Since instants are in UTC zone and always have all required fields, we can return it as is
        if (formatter == DateTimeFormatter.ISO_INSTANT) {
            return formatter;
        }
        else {
            DateTimeFormatterBuilder formatterWithDefaultsBuilder = new DateTimeFormatterBuilder().append(formatter)
                    .append(DEFAULTS);
            formatterWithDefaultsBuilder
                    .parseDefaulting(ChronoField.OFFSET_SECONDS, (long) OffsetDateTime.now(zoneId).getOffset().getTotalSeconds());
            return formatterWithDefaultsBuilder.toFormatter().withZone(zoneId);
        }
    }

    static long getNanos(long seconds, long nanoAdjustment) {
        return seconds * BILLION + nanoAdjustment;
    }

    static long getMillis(long seconds, long nanoAdjustment) {
        return (seconds * THOUSAND) + (nanoAdjustment / MILLION);
    }

    static long getSecondsFromMillis(long millis) {
        return millis / THOUSAND;
    }

    static long getNanosFromMillis(long millis) {
        return (millis % THOUSAND) * MILLION;
    }

    static Instant instantFromMillis(long millis) {
        return Instant.ofEpochMilli(millis);
    }

    static Instant instantFromNanos(long nanos) {
        long seconds = nanos / BILLION;
        long adjustment = nanos % BILLION;
        return Instant.ofEpochSecond(seconds, adjustment);
    }

    static Long instantToMillis(Instant instant) {
        return instant.toEpochMilli();
    }

    static Long instantToNanos(Instant instant) {
        return DateTimeUtil.getNanos(instant.getEpochSecond(), instant.getNano());
    }

    static OffsetDateTime correctOffset(OffsetDateTime value, ZoneId zoneId) {
        Instant instant = value.toLocalDateTime().atZone(zoneId).toInstant();
        return OffsetDateTime.ofInstant(instant, zoneId);
    }
}
