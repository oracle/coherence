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
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.LinkedHashMap;

/**
 * Converter for values of type {@link Instant}.
 */
class InstantConverter
        extends BaseTemporalAccessorConverter<Instant> {
    InstantConverter(DateTimeConverterOptions options) {
        super(options, new InstantTimestampHandler(options), Instant::from);
    }

    private static class InstantTimestampHandler
            extends TimestampHandler<Instant> {
        private static final LinkedHashMap<String, TemporalField> INSTANT_TEMPORAL_FIELDS = new LinkedHashMap<>();

        static {
            INSTANT_TEMPORAL_FIELDS.put("second", ChronoField.INSTANT_SECONDS);
            INSTANT_TEMPORAL_FIELDS.put("nano", ChronoField.NANO_OF_SECOND);
        }

        private InstantTimestampHandler(DateTimeConverterOptions options) {
            super(DateTimeUtil::instantToMillis,
                  DateTimeUtil::instantFromMillis,
                  DateTimeUtil::instantToNanos,
                  DateTimeUtil::instantFromNanos,
                  INSTANT_TEMPORAL_FIELDS, Instant::now);
        }
    }
}
