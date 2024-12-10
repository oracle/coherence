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


import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.LinkedHashMap;

/**
 * Converter for values of type {@link LocalDateTime}.
 */
class LocalDateTimeConverter
        extends BaseTemporalAccessorConverter<LocalDateTime> {
    LocalDateTimeConverter(DateTimeConverterOptions options) {
        super(options, new LocalDateTimeTimestampHandler(options), LocalDateTime::from);
    }

    private static class LocalDateTimeTimestampHandler
            extends TimestampHandler<LocalDateTime> {
        private static final LinkedHashMap<String, TemporalField> LOCAL_DATE_TIME_TEMPORAL_FIELDS = new LinkedHashMap<>();

        static {
            LOCAL_DATE_TIME_TEMPORAL_FIELDS.put("year", ChronoField.YEAR);
            LOCAL_DATE_TIME_TEMPORAL_FIELDS.put("month", ChronoField.MONTH_OF_YEAR);
            LOCAL_DATE_TIME_TEMPORAL_FIELDS.put("day", ChronoField.DAY_OF_MONTH);
            LOCAL_DATE_TIME_TEMPORAL_FIELDS.put("hour", ChronoField.HOUR_OF_DAY);
            LOCAL_DATE_TIME_TEMPORAL_FIELDS.put("minute", ChronoField.MINUTE_OF_HOUR);
            LOCAL_DATE_TIME_TEMPORAL_FIELDS.put("second", ChronoField.SECOND_OF_MINUTE);
            LOCAL_DATE_TIME_TEMPORAL_FIELDS.put("nano", ChronoField.NANO_OF_SECOND);
        }

        private LocalDateTimeTimestampHandler(DateTimeConverterOptions options) {
            super(lt -> DateTimeUtil.instantToMillis(lt.atZone(options.getZoneId()).toInstant()),
                  millis -> LocalDateTime.ofInstant(DateTimeUtil.instantFromMillis(millis), options.getZoneId()),
                  lt -> DateTimeUtil.instantToNanos(lt.atZone(options.getZoneId()).toInstant()),
                  nanos -> LocalDateTime.ofInstant(DateTimeUtil.instantFromNanos(nanos), options.getZoneId()),
                  LOCAL_DATE_TIME_TEMPORAL_FIELDS, LocalDateTime::now);
        }
    }
}
