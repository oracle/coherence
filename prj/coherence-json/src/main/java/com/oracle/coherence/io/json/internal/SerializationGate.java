/*
 * Copyright (c) 2022, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.io.json.internal;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.DefaultObjectInputFilter;
import com.tangosol.io.internal.SerializationTelemetry;
import com.tangosol.internal.util.CoherenceMode;

import java.io.ObjectInputFilter;
import java.util.Set;

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

    /**
     * Determine whether JSON class metadata is a valid materialization target.
     *
     * @param sMetadata  the JSON metadata value, which may be an alias or class
     *                   name
     * @param clz        the resolved class
     *
     * @return {@code true} if the class metadata may be materialized
     */
    public static boolean isValidClassMetadata(String sMetadata, Class<?> clz)
        {
        return isValidClassMetadata(sMetadata, clz, Object.class);
        }

    /**
     * Determine whether JSON class metadata is a valid materialization target.
     *
     * @param sMetadata  the JSON metadata value, which may be an alias or class
     *                   name
     * @param clz        the resolved class
     * @param clzTarget  the static target class being deserialized
     *
     * @return {@code true} if the class metadata may be materialized
     */
    public static boolean isValidClassMetadata(String sMetadata, Class<?> clz, Class<?> clzTarget)
        {
        return isValidClassMetadata(sMetadata, clz, clzTarget, 0);
        }

    /**
     * Determine whether JSON class metadata is a valid materialization target.
     *
     * @param sMetadata  the JSON metadata value, which may be an alias or class
     *                   name
     * @param clz        the resolved class
     * @param clzTarget  the static target class being deserialized
     * @param nDepth     the nested JSON class metadata depth
     *
     * @return {@code true} if the class metadata may be materialized
     */
    public static boolean isValidClassMetadata(String sMetadata, Class<?> clz, Class<?> clzTarget, int nDepth)
        {
        if (isDeniedMetadata(sMetadata, clz) && shouldRejectDeniedMetadata(clzTarget, nDepth))
            {
            SerializationTelemetry.recordFilterCheck("rejected", "json-class-metadata-rejected", clz, null);
            return false;
            }

        return isValid(clz);
        }

    /**
     * Enter a JSON class metadata deserialization scope.
     *
     * @return the previous metadata depth
     */
    public static int enterJsonClassMetadata()
        {
        int nDepth = JSON_CLASS_METADATA_DEPTH.get();
        JSON_CLASS_METADATA_DEPTH.set(nDepth + 1);
        return nDepth;
        }

    /**
     * Exit a JSON class metadata deserialization scope.
     */
    public static void exitJsonClassMetadata()
        {
        int nDepth = JSON_CLASS_METADATA_DEPTH.get();
        if (nDepth <= 1)
            {
            JSON_CLASS_METADATA_DEPTH.remove();
            }
        else
            {
            JSON_CLASS_METADATA_DEPTH.set(nDepth - 1);
            }
        }

    /**
     * Return {@code true} if JSON class metadata names a known executable
     * materialization gadget.
     *
     * @param sMetadata  the JSON metadata value
     * @param clz        the resolved class
     *
     * @return {@code true} if the metadata should be rejected
     */
    static boolean isDeniedMetadata(String sMetadata, Class<?> clz)
        {
        String sClass = clz == null ? null : clz.getName();
        return (sMetadata != null && DENY_METADATA.contains(sMetadata))
               || (sClass != null && DENY_METADATA.contains(sClass));
        }

    /**
     * Return {@code true} if denied JSON metadata must be rejected at this
     * materialization site.
     *
     * @param clzTarget  the static target class being deserialized
     * @param nDepth     the nested JSON class metadata depth
     *
     * @return {@code true} if the denied metadata must be rejected
     */
    private static boolean shouldRejectDeniedMetadata(Class<?> clzTarget, int nDepth)
        {
        if (CoherenceMode.isLegacy())
            {
            return false;
            }

        return SerializationRole.current() == SerializationRole.GRPC
               || (nDepth == 0 && (clzTarget == null || clzTarget == Object.class));
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
     * JSON class metadata values that materialize executable state before a
     * route-specific install gate can review the object.
     */
    private static final Set<String> DENY_METADATA = Set.of(
            "com.tangosol.coherence.config.builder.InstanceBuilder",
            "com.tangosol.coherence.config.builder.StaticFactoryInstanceBuilder",
            "com.tangosol.internal.util.invoke.ClassDefinition",
            "com.tangosol.internal.util.invoke.RemoteConstructor",
            "com.tangosol.internal.util.invoke.RemotableSupport",
            "com.tangosol.util.aggregator.ScriptAggregator",
            "com.tangosol.util.extractor.ReflectionExtractor",
            "com.tangosol.util.extractor.ReflectionUpdater",
            "com.tangosol.util.extractor.ScriptValueExtractor",
            "com.tangosol.util.filter.ScriptFilter",
            "com.tangosol.util.processor.MethodInvocationProcessor",
            "com.tangosol.util.processor.ScriptProcessor",
            "coherence.config.builder.InstanceBuilder",
            "coherence.config.builder.StaticFactoryInstanceBuilder",
            "extractor.ReflectionExtractor",
            "extractor.ReflectionUpdater",
            "filter.ScriptFilter",
            "internal.util.invoke.ClassDefinition",
            "internal.util.invoke.RemoteConstructor",
            "processor.MethodInvocationProcessor",
            "processor.ScriptProcessor",
            "util.aggregator.ScriptAggregator",
            "util.extractor.ReflectionExtractor",
            "util.extractor.ReflectionUpdater",
            "util.extractor.ScriptValueExtractor",
            "util.filter.ScriptFilter",
            "util.processor.MethodInvocationProcessor",
            "util.processor.ScriptProcessor");

    /**
     * The current nested JSON class metadata depth.
     */
    private static final ThreadLocal<Integer> JSON_CLASS_METADATA_DEPTH = ThreadLocal.withInitial(() -> 0);

    }
