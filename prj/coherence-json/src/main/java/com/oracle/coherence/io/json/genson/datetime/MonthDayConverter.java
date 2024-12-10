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


import java.time.MonthDay;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.oracle.coherence.io.json.genson.stream.ObjectReader;

/**
 * Converter for values of type {@link MonthDay}.
 */
class MonthDayConverter
        extends BaseTemporalAccessorConverter<MonthDay> {
    MonthDayConverter(DateTimeConverterOptions options) {
        super(options, new MonthDayTimestampHandler(options), MonthDay::from);
    }

    private static class MonthDayTimestampHandler
            extends TimestampHandler<MonthDay> {
        private static final LinkedHashMap<String, TemporalField> MONTH_DAY_TEMPORAL_FIELDS = new LinkedHashMap<>();

        static {
            MONTH_DAY_TEMPORAL_FIELDS.put("month", ChronoField.MONTH_OF_YEAR);
            MONTH_DAY_TEMPORAL_FIELDS.put("day", ChronoField.DAY_OF_MONTH);
        }

        MonthDayTimestampHandler(DateTimeConverterOptions options) {
            super(null, null, null, null,
                  MONTH_DAY_TEMPORAL_FIELDS, MonthDay::now);
        }

        @Override
        protected MonthDay readFieldsFromObject(Supplier<MonthDay> instanceProvider, ObjectReader reader) {
            Map<String, Integer> values = new HashMap<>();
            reader.next();
            values.put(reader.name(), reader.valueAsInt());
            reader.next();
            values.put(reader.name(), reader.valueAsInt());
            return MonthDay.of(values.get("month"), values.get("day"));
        }

        @Override
        protected MonthDay readFieldsFromArray(Supplier<MonthDay> instanceProvider, ObjectReader reader) {
            reader.next();
            int month = reader.valueAsInt();
            reader.next();
            int day = reader.valueAsInt();
            return MonthDay.of(month, day);
        }
    }
}
