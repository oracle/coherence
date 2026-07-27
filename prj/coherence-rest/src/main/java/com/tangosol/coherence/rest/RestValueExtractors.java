/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.tangosol.coherence.rest.util.PropertySet;

import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.ValueExtractor;

import java.util.Map;
import java.util.Objects;

/**
 * Local REST extractors used after query execution returns cache entries.
 *
 * @author Vaso Putica  2026.05.11
 * @since 26.04
 */
@SuppressWarnings({"rawtypes", "unchecked"})
final class RestValueExtractors
    {
    private RestValueExtractors()
        {
        }

    /**
     * Return an extractor for values, optionally projected by the supplied
     * property set.
     *
     * @param propertySet  optional projection properties
     *
     * @return the value extractor
     */
    static ValueExtractor<Map.Entry, ?> valueExtractor(PropertySet propertySet)
        {
        return propertySet == null ? ENTRY_VALUE_EXTRACTOR : ENTRY_VALUE_EXTRACTOR.andThen(propertySet);
        }

    /**
     * Return an extractor for entries, optionally projecting the value side.
     *
     * @param propertySet  optional projection properties
     *
     * @return the entry extractor
     */
    static ValueExtractor<Map.Entry, ?> entryExtractor(PropertySet propertySet)
        {
        return propertySet == null
               ? ValueExtractor.identity()
               : new EntryProjectionExtractor(propertySet);
        }

    /**
     * Extracts a Map.Entry value without relying on a dynamic method-reference
     * extractor.
     */
    private enum EntryValueExtractor
            implements ValueExtractor<Map.Entry, Object>
        {
        INSTANCE;

        @Override
        public Object extract(Map.Entry entry)
            {
            return entry.getValue();
            }
        }

    /**
     * Extracts a Map.Entry while projecting its value.
     */
    private static final class EntryProjectionExtractor
            implements ValueExtractor<Map.Entry, SimpleMapEntry>
        {
        private EntryProjectionExtractor(PropertySet propertySet)
            {
            f_propertySet = propertySet;
            }

        @Override
        public SimpleMapEntry extract(Map.Entry entry)
            {
            return new SimpleMapEntry<>(entry.getKey(), f_propertySet.extract(entry.getValue()));
            }

        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (!(o instanceof EntryProjectionExtractor))
                {
                return false;
                }
            EntryProjectionExtractor that = (EntryProjectionExtractor) o;
            return Objects.equals(f_propertySet, that.f_propertySet);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(f_propertySet);
            }

        private final PropertySet f_propertySet;
        }

    /**
     * Concrete value extractor used by REST projection aliases.
     * <p>
     * Composing a projection with {@code Map.Entry::getValue} can force
     * hardened mode to evaluate a dynamic lambda before the generated partial
     * object is serialized
     */
    private static final ValueExtractor<Map.Entry, Object> ENTRY_VALUE_EXTRACTOR = EntryValueExtractor.INSTANCE;
    }
