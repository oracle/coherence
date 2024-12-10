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


import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import com.oracle.coherence.io.json.genson.datetime.annotation.JsonTimestampFormat;
import com.oracle.coherence.io.json.genson.datetime.annotation.JsonZoneId;

import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.GensonBuilder;
import com.oracle.coherence.io.json.genson.annotation.JsonDateFormat;
import com.oracle.coherence.io.json.genson.convert.ContextualFactory;
import com.oracle.coherence.io.json.genson.ext.GensonBundle;
import com.oracle.coherence.io.json.genson.reflect.BeanProperty;

/**
 * Provides support for Java 8 Date & Time API (JSR 310).
 */
public class JavaDateTimeBundle
        extends GensonBundle {
    private ZoneId zoneId = ZoneId.systemDefault();

    private Map<Class<?>, ConverterGenerator> converterGenerators = new HashMap<>();
    private Map<Class<? extends TemporalAccessor>, TimestampFormat> temporalAccessorTimestampFormats = new HashMap<>();
    private Map<Class<? extends TemporalAmount>, TimestampFormat> temporalAmountTimestampFormats = new HashMap<>();
    private Map<Class<?>, DateTimeFormatter> formatters = new HashMap<>();

    {
        registerConverterGenerator(Instant.class, InstantConverter::new);
        registerConverterGenerator(ZonedDateTime.class, ZonedDateTimeConverter::new);
        registerConverterGenerator(OffsetDateTime.class, OffsetDateTimeConverter::new);
        registerConverterGenerator(LocalDateTime.class, LocalDateTimeConverter::new);
        registerConverterGenerator(LocalDate.class, LocalDateConverter::new);
        registerConverterGenerator(LocalTime.class, LocalTimeConverter::new);
        registerConverterGenerator(Year.class, YearConverter::new);
        registerConverterGenerator(YearMonth.class, YearMonthConverter::new);
        registerConverterGenerator(MonthDay.class, MonthDayConverter::new);
        registerConverterGenerator(OffsetTime.class, OffsetTimeConverter::new);
        registerConverterGenerator(Period.class, TemporalAmountConverter::period);
        registerConverterGenerator(Duration.class, TemporalAmountConverter::duration);
    }

    {
        temporalAccessorTimestampFormats.put(Instant.class, TimestampFormat.OBJECT);
        temporalAccessorTimestampFormats.put(ZonedDateTime.class, TimestampFormat.OBJECT);
        temporalAccessorTimestampFormats.put(OffsetDateTime.class, TimestampFormat.OBJECT);
        temporalAccessorTimestampFormats.put(LocalDateTime.class, TimestampFormat.OBJECT);
        temporalAccessorTimestampFormats.put(LocalDate.class, TimestampFormat.OBJECT);
        temporalAccessorTimestampFormats.put(LocalTime.class, TimestampFormat.OBJECT);
        temporalAccessorTimestampFormats.put(Year.class, TimestampFormat.OBJECT);
        temporalAccessorTimestampFormats.put(YearMonth.class, TimestampFormat.OBJECT);
        temporalAccessorTimestampFormats.put(MonthDay.class, TimestampFormat.OBJECT);
        temporalAccessorTimestampFormats.put(OffsetTime.class, TimestampFormat.OBJECT);
    }

    {
        temporalAmountTimestampFormats.put(Period.class, TimestampFormat.OBJECT);
        temporalAmountTimestampFormats.put(Duration.class, TimestampFormat.OBJECT);
    }

    {
        formatters.put(Instant.class, DateTimeFormatter.ISO_INSTANT);
        formatters.put(ZonedDateTime.class, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        formatters.put(OffsetDateTime.class, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        formatters.put(LocalDateTime.class, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        formatters.put(LocalDate.class, DateTimeFormatter.ISO_LOCAL_DATE);
        formatters.put(LocalTime.class, DateTimeFormatter.ISO_LOCAL_TIME);
        formatters.put(OffsetTime.class, DateTimeFormatter.ISO_OFFSET_TIME);
        formatters.put(Year.class, DateTimeFormatter.ofPattern("uuuu"));
        formatters.put(YearMonth.class, DateTimeFormatter.ofPattern("uuuu-MM"));
        formatters.put(MonthDay.class, DateTimeFormatter.ofPattern("MM-dd"));
    }

    @Override
    public void configure(GensonBuilder builder) {
        boolean asTimestamp = builder.isDateAsTimestamp();
        builder.withContextualFactory(new JavaDateTimeContextualFactory(asTimestamp));
        for (Map.Entry<Class<?>, ConverterGenerator> converterGeneratorEntry : converterGenerators.entrySet()) {
            Class<?> clazz = converterGeneratorEntry.getKey();
            ConverterGenerator<?> converterGenerator = converterGeneratorEntry.getValue();
            DateTimeFormatter formatter = formatters.get(clazz);
            DateTimeConverterOptions options = new DateTimeConverterOptions(clazz,
                                                                            formatter,
                                                                            asTimestamp,
                                                                            getDefaultTimestampFormat(clazz),
                                                                            zoneId);
            Converter converter = converterGenerator.createConverter(options);
            builder.withConverters(converter);
        }
        builder.withConverter(new ZoneOffsetConverter(), ZoneOffset.class);
        builder.withConverterFactory(new ZoneIdConverter.Factory());
    }

    private TimestampFormat getDefaultTimestampFormat(Class<?> clazz) {
        if (TemporalAmount.class.isAssignableFrom(clazz)) {
            return temporalAmountTimestampFormats.get(clazz);
        }
        else {
            return temporalAccessorTimestampFormats.get(clazz);
        }
    }

    private <T> void registerConverterGenerator(Class<T> clazz,
                                                Function<DateTimeConverterOptions, Converter<T>> converterGeneratorFunction) {
        ConverterGenerator converterGenerator = new ConverterGenerator(converterGeneratorFunction);
        converterGenerators.put(clazz, converterGenerator);
    }

    /**
     * Sets the {@link DateTimeFormatter} to use when parsing/formatting a value of the provided class.
     * @param clazz the class to format
     * @param formatter the formatter for the class
     * @return this {@link JavaDateTimeBundle}
     */
    public JavaDateTimeBundle setFormatter(Class<? extends TemporalAccessor> clazz, DateTimeFormatter formatter) {
        formatters.put(clazz, formatter);
        return this;
    }

    /**
     * The {@link TimestampFormat} to use when serializing/deserializing a {@link TemporalAccessor} value of the provided class.
     * <p>
     * This only applies if {@link GensonBuilder#isDateAsTimestamp()} is true or if {@link JsonDateFormat#asTimeInMillis()}
     * is true.
     *
     * @param clazz  the class to format
     * @param format the format for the class
     * @return this {@link JavaDateTimeBundle}
     */
    public JavaDateTimeBundle setTemporalAccessorTimestampFormat(Class<? extends TemporalAccessor> clazz,
                                                                 TimestampFormat format) {
        temporalAccessorTimestampFormats.put(clazz, format);
        return this;
    }

    /**
     * The {@link TimestampFormat} to use when serializing/deserializing a {@link TemporalAmount} value of the provided class.
     * <p>
     * Use this method to set timestamp format for {@link Duration} or {@link Period}.
     *
     * @param clazz  the class to format
     * @param format the format for the class
     * @return this {@link JavaDateTimeBundle}
     */
    public JavaDateTimeBundle setTemporalAmountTimestampFormat(Class<? extends TemporalAmount> clazz,
                                                               TimestampFormat format) {
        temporalAmountTimestampFormats.put(clazz, format);
        return this;
    }

    /**
     * The default zoneId to use when parsing DateTime type objects.
     * <p>
     * By default, this will be set to the value returned by {@link ZoneId#systemDefault()}.
     *
     * @param zoneId the Zone ID
     * @return this {@link JavaDateTimeBundle}
     */
    public JavaDateTimeBundle setZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
        return this;
    }

    private class JavaDateTimeContextualFactory
            implements ContextualFactory {
        private boolean timestampByDefault;

        private JavaDateTimeContextualFactory(boolean timestampByDefault) {
            this.timestampByDefault = timestampByDefault;
        }

        @Override
        public Converter create(BeanProperty property, Genson genson) {
            if (hasRelevantAnnotation(property)) {
                Class<?> rawClass = property.getRawClass();
                for (Map.Entry<Class<?>, ConverterGenerator> supportedType : converterGenerators.entrySet()) {
                    Class<?> clazz = supportedType.getKey();
                    ConverterGenerator generator = supportedType.getValue();
                    if (clazz.isAssignableFrom(rawClass)) {
                        DateTimeConverterOptions options = createOptions(property, clazz);
                        return generator.createConverter(options);
                    }
                }
            }

            return null;
        }

        private DateTimeConverterOptions createOptions(BeanProperty property, Class<?> clazz) {
            JsonDateFormat jsonDateFormat = property.getAnnotation(JsonDateFormat.class);
            DateTimeFormatter formatter = getFormatter(jsonDateFormat, formatters.get(clazz));
            boolean asTimestamp = jsonDateFormat == null ? timestampByDefault : jsonDateFormat.asTimeInMillis();
            ZoneId zoneId = getZoneId(property);
            TimestampFormat timestampFormat = getTimestampFormat(property, getDefaultTimestampFormat(clazz));
            return new DateTimeConverterOptions(clazz, formatter, asTimestamp, timestampFormat, zoneId);
        }

        private DateTimeFormatter getFormatter(JsonDateFormat ann, DateTimeFormatter defaultFormatter) {
            if (ann == null || ann.value().isEmpty()) {
                return defaultFormatter;
            }
            else {
                Locale locale = ann.lang().isEmpty() ? Locale.getDefault() : new Locale(ann.lang());
                return DateTimeFormatter.ofPattern(ann.value()).withLocale(locale);
            }
        }

        private TimestampFormat getTimestampFormat(BeanProperty property, TimestampFormat defaultTimestampFormat) {
            JsonTimestampFormat ann = property.getAnnotation(JsonTimestampFormat.class);
            return ann == null ? defaultTimestampFormat : ann.value();
        }

        private ZoneId getZoneId(BeanProperty property) {
            JsonZoneId ann = property.getAnnotation(JsonZoneId.class);
            return ann == null || ann.value().isEmpty() ? zoneId : ZoneId.of(ann.value());
        }

        private boolean hasRelevantAnnotation(BeanProperty property) {
            JsonDateFormat formatAnn = property.getAnnotation(JsonDateFormat.class);
            JsonZoneId zoneIdAnn = property.getAnnotation(JsonZoneId.class);
            JsonTimestampFormat timestampFormatAnn = property.getAnnotation(JsonTimestampFormat.class);
            return formatAnn != null || zoneIdAnn != null || timestampFormatAnn != null;
        }

    }

    private class ConverterGenerator<T> {
        private Function<DateTimeConverterOptions, Converter<T>> converterGeneratorFunction;

        ConverterGenerator(Function<DateTimeConverterOptions, Converter<T>> converterGeneratorFunction) {
            this.converterGeneratorFunction = converterGeneratorFunction;
        }

        Converter<T> createConverter(DateTimeConverterOptions options) {
            return converterGeneratorFunction.apply(options);
        }
    }
}
