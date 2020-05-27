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


import java.time.OffsetTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.LinkedHashMap;

/**
 * Converter for values of type {@link OffsetTime}.
 */
class OffsetTimeConverter
        extends BaseTemporalAccessorConverter<OffsetTime> {
    OffsetTimeConverter(DateTimeConverterOptions options) {
        super(options, new OffsetTimeTimestampHandler(options), OffsetTime::from);
    }

    private static class OffsetTimeTimestampHandler
            extends TimestampHandler<OffsetTime> {
        private static final LinkedHashMap<String, TemporalField> OFFSET_TIME_TEMPORAL_FIELDS = new LinkedHashMap<>();

        static {
            OFFSET_TIME_TEMPORAL_FIELDS.put("hour", ChronoField.HOUR_OF_DAY);
            OFFSET_TIME_TEMPORAL_FIELDS.put("minute", ChronoField.MINUTE_OF_HOUR);
            OFFSET_TIME_TEMPORAL_FIELDS.put("second", ChronoField.SECOND_OF_MINUTE);
            OFFSET_TIME_TEMPORAL_FIELDS.put("nano", ChronoField.NANO_OF_SECOND);
            OFFSET_TIME_TEMPORAL_FIELDS.put("offsetSeconds", ChronoField.OFFSET_SECONDS);
        }

        private OffsetTimeTimestampHandler(DateTimeConverterOptions options) {
            super(null, null, null, null,
                  OFFSET_TIME_TEMPORAL_FIELDS, OffsetTime::now);
        }
    }
}
