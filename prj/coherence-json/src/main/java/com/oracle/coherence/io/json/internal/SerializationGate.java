/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.common.collections.ConcurrentHashMap;

import java.io.ObjectInputFilter;

/**
 * Utility class to determine if a type is allowed to be serialized or
 * deserialized.  In order to do so, this class relies on using
 * the configuration defined by {@link ObjectInputFilter.Config#getSerialFilter()}.
 * The configuration returned is controlled by the {@code jdk.SerialFilter} system
 * property.
 *
 * See <a href="https://www.oracle.com/pls/topic/lookup?ctx=javase11&id=serialization_filter_guide">Serialization Filter Guide</a>
 * for details.
 *
 * @author rl  2022.4.19
 * @since 22.06
 */
public class SerializationGate
    {
    /**
     * Determines if the provided class is a valid target for serialization/
     * deserialization operations.
     *
     * @param clz  the type to validate
     *
     * @return {@code true} if the type may be serialized/deserialized,
     *         otherwise returns {@code false}
     */
    public static boolean isValid(Class<?> clz)
        {
        String sClass = clz.getName();

        return RESULT_CACHE.computeIfAbsent(sClass, s ->
            {
            ObjectInputFilter filter = ObjectInputFilter.Config.getSerialFilter();
            return filter == null || ObjectInputFilter.Config.getSerialFilter()
                                             .checkInput(new FilterInfo(clz)) != ObjectInputFilter.Status.REJECTED;
            });
        }

    // ----- inner class: FilterInfo ----------------------------------------

    /**
     * Simple {@link ObjectInputFilter.FilterInfo} info implementation.
     */
    public static class FilterInfo
            implements ObjectInputFilter.FilterInfo
        {
        // ----- constructors -----------------------------------------------

        public FilterInfo(Class<?> clz)
            {
            f_clz = clz;
            }

        // ----- ObjectInputFilter.FilterInfo methods -----------------------

        @Override
        public Class<?> serialClass()
            {
            return f_clz;
            }

        @Override
        public long arrayLength()
            {
            return -1L;
            }

        @Override
        public long depth()
            {
            return 1L;
            }

        @Override
        public long references()
            {
            return 0L;
            }

        @Override
        public long streamBytes()
            {
            return 0L;
            }

        // ----- data members -----------------------------------------------

        /**
         * The class to test.
         */
        private final Class<?> f_clz;
        }

    // ----- constants ------------------------------------------------------

    /**
     * A {@code Map} to cache the serialization filter result.
     */
    private static final ConcurrentHashMap<String, Boolean> RESULT_CACHE = new ConcurrentHashMap<>();
    }
