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


import java.time.YearMonth;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.util.LinkedHashMap;

/**
 * Converter for values of type {@link YearMonth}.
 */
class YearMonthConverter
        extends BaseTemporalAccessorConverter<YearMonth> {
    private static final YearMonth EPOCH_YEAR_MONTH = YearMonth.of(1970, 1);

    YearMonthConverter(DateTimeConverterOptions options) {
        super(options, new YearMonthTimestampHandler(options), YearMonth::from);
    }

    private static class YearMonthTimestampHandler
            extends TimestampHandler<YearMonth> {
        private static final LinkedHashMap<String, TemporalField> YEAR_MONTH_TEMPORAL_FIELDS = new LinkedHashMap<>();

        static {
            YEAR_MONTH_TEMPORAL_FIELDS.put("year", ChronoField.YEAR);
            YEAR_MONTH_TEMPORAL_FIELDS.put("month", ChronoField.MONTH_OF_YEAR);
        }

        private YearMonthTimestampHandler(DateTimeConverterOptions options) {
            super(YearMonthConverter::getEpochMonth, YearMonthConverter::fromEpochMonth,
                  YearMonthConverter::getEpochMonth, YearMonthConverter::fromEpochMonth,
                  YEAR_MONTH_TEMPORAL_FIELDS, YearMonth::now);
        }
    }

    private static long getEpochMonth(YearMonth yearMonth) {
        return EPOCH_YEAR_MONTH.until(yearMonth, ChronoUnit.MONTHS);
    }

    private static YearMonth fromEpochMonth(long months) {
        return EPOCH_YEAR_MONTH.plus(months, ChronoUnit.MONTHS);
    }
}
