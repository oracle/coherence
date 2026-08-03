/*
 * Copyright (c) 2022, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.io.json.internal;

import com.tangosol.io.internal.DefaultObjectInputFilter;
import com.tangosol.io.internal.SerializationTelemetry;

import java.io.ObjectInputFilter;

/**
 * Utility class to determine if a type is allowed to be serialized or
 * deserialized. In order to do so, this class relies on the Coherence default
 * filter intersected with any configuration defined by
 * {@link ObjectInputFilter.Config#getSerialFilter()}.
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
        ObjectInputFilter filter = DefaultObjectInputFilter.create();
        boolean fAllowed = filter.checkInput(new FilterInfo(clz)) != ObjectInputFilter.Status.REJECTED;
        if (!fAllowed)
            {
            SerializationTelemetry.recordFilterCheck("rejected", "json-class-rejected", clz, null);
            }
        return fAllowed;
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

    }
