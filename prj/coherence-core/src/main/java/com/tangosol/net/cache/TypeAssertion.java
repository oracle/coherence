/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.cache;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.util.Options;

import com.tangosol.coherence.config.CacheMapping;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

/**
 * Defines a {@link com.tangosol.net.NamedCache.Option} for asserting the type
 * of keys and values used with a {@link NamedCache}.
 *
 * @param <K>  the type of the cache entry keys
 * @param <V>  the type of the cache entry values
 *
 * @author bko  2015.06.15
 *
 * @since Coherence 12.2.1
 */
public interface TypeAssertion<K, V> extends NamedCache.Option
    {
    /**
     * Asserts the type compatibility of a named cache given the
     * {@link CacheMapping} that defines the cache.
     *
     * @param sCacheName    the name of the cache
     * @param cacheMapping  the {@link CacheMapping}
     * @param fLog          when true, log warning if typing mismatch between {@link CacheMapping} configuration
     *                      and this {@link TypeAssertion}; specifically, one uses raw types and the other
     *                      asserts specific types.
     *
     * @return false if warning logged about the assertion, true otherwise.
     *
     * @throws IllegalArgumentException  when types used with the {@link TypeAssertion}
     *                                   are illegal according to the configuration
     */
    boolean assertTypeSafety(String sCacheName, CacheMapping cacheMapping, boolean fLog)
            throws IllegalArgumentException;

    /**
     * Asserts the type compatibility of a named cache given the
     * {@link CacheMapping} that defines the cache.
     *
     * @param sCacheName    the name of the cache
     * @param cacheMapping  the {@link CacheMapping}
     *
     * @return false if warning logged about the assertion, true otherwise.
     *
     * @throws IllegalArgumentException  when types used with the {@link TypeAssertion}
     *                                   are illegal according to the configuration
     */
    default boolean assertTypeSafety(String sCacheName, CacheMapping cacheMapping)
            throws IllegalArgumentException
        {
        return assertTypeSafety(sCacheName, cacheMapping, true);
        }

    // ----- Helper methods ---------------------------------------------

    /**
     * Obtains a {@link TypeAssertion} that asserts the specified types are
     * configured for a cache.
     *
     * @param <K>       the key type
     * @param <V>       the value type
     * @param clsKey    the desired key type
     * @param clsValue  the desired value type
     *
     * @return a {@link TypeAssertion}
     */
    static <K, V> TypeAssertion<K, V> withTypes(final Class<K> clsKey, final Class<V> clsValue)
        {
        return new WithTypesAssertion<K, V>(clsKey, clsValue);
        }

    /**
     * Obtains a {@link TypeAssertion} that allows NamedCaches to be acquired
     * and assigned to a raw NamedCache reference.  A debug log message will be
     * raised for caches that have been configured with specific types.
     *
     * @return a {@link TypeAssertion}
     */
    @Options.Default
    static TypeAssertion withRawTypes()
        {
        return WITH_RAW_TYPES;
        }

    /**
     * Obtains a {@link TypeAssertion} that allows NamedCaches to be acquired
     * <strong>without</strong> type-checking, warnings or log messages.
     *
     * @param <K>       the key type
     * @param <V>       the value type
     *
     * @return a {@link TypeAssertion}
     */
    static <K, V> TypeAssertion<K, V> withoutTypeChecking()
        {
        return WITHOUT_TYPE_CHECKING;
        }

    // ----- inner class WithTypesAssertion ---------------------------------------------------------------------------

    static class WithTypesAssertion<K, V>
            implements TypeAssertion<K, V>
        {
        // ----- constructors -----------------------------------------------------------------------------------------

        /**
         * Constructs {@link com.tangosol.net.cache.TypeAssertion.WithTypesAssertion}
         *
         * @param keyClass   non-null key type
         * @param valueClass non-null value type
         */
        public WithTypesAssertion(Class<K> keyClass, Class<V> valueClass)
            {
            if (keyClass == null || valueClass == null)
                {
                throw new IllegalArgumentException(keyClass == null ? "keyClass" : " valueClass" +
                                                   " parameter must be non-null" );
                }

            m_sKeyClassName   = keyClass.getName();
            m_sValueClassName = valueClass.getName();
            }

        // ----- TypeAssertion interface ------------------------------------------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean assertTypeSafety(String sCacheName, CacheMapping cacheMapping, boolean fLog)
                throws IllegalArgumentException
            {
            if (cacheMapping.usesRawTypes())
                {
                if (fLog)
                    {
                    Logger.info("The cache \"" + sCacheName + "\" is configured without key and/or value types "
                            + "but the application is requesting NamedCache<" + getKeyClassName() + ","
                            + getValueClassName() + ">");
                    return false;
                    }
                }
            else
                {
                // ensure that the specified types match the cache mapping types
                if (!getKeyClassName().equals(cacheMapping.getKeyClassName())
                    || !getValueClassName().equals(cacheMapping.getValueClassName()))
                    {
                    throw new IllegalArgumentException("The cache mapping for \"" + sCacheName
                                                       + "\" has been configured as NamedCache<"
                                                       + cacheMapping.getKeyClassName() + ","
                                                       + cacheMapping.getValueClassName()
                                                       + ">, but the application is requesting NamedCache<"
                                                       + getKeyClassName() + "," + getValueClassName() + ">");
                    }
                }
            return true;
            }

        // ----- WithTypesAssertion methods ---------------------------------------------------------------------------

        /**
         * Get Key ClassName
         *
         * @return key class name
         */
        public String getKeyClassName()
            {
            return m_sKeyClassName;
            }

        /**
         * Get Value ClassName
         *
         * @return Value class name
         */
        public String getValueClassName()
            {
            return m_sValueClassName;
            }

        // ----- Object methods ---------------------------------------------------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }

            if (o instanceof WithTypesAssertion)
                {
                WithTypesAssertion a = (WithTypesAssertion) o;

                return m_sKeyClassName.equals(a.getKeyClassName()) && m_sValueClassName.equals(a.getValueClassName());
                }
            else
                {
                return false;
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode()
            {
            return m_sKeyClassName.hashCode() + m_sValueClassName.hashCode();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            return "WithTypesAssertion" + "_" + m_sKeyClassName + "_" + m_sValueClassName;
            }

        // ----- data members -----------------------------------------------------------------------------------------

        /**
         * Key class name.
         */
        final private String m_sKeyClassName;

        /**
         * Value class name
         */
        final private String m_sValueClassName;
        }

    // ----- constants --------------------------------------------------

    /**
     * When used no type checking will occur and no warnings will be generated.
     */
    final TypeAssertion WITHOUT_TYPE_CHECKING = new TypeAssertion()
        {
        @Override
        public boolean assertTypeSafety(String sCacheName, CacheMapping cacheMapping, boolean fLog)
                throws IllegalArgumentException
            {
            // NOTE: completely by-passes all type-checking and warnings
            return true;
            }

        @Override
        public String toString()
            {
            return "WITHOUT_TYPE_CHECKING";
            }
        };

    /**
     * When used warnings will be issued where types are configured but not used.
     */
    final TypeAssertion WITH_RAW_TYPES = new TypeAssertion()
        {
        @Override
        public boolean assertTypeSafety(String sCacheName, CacheMapping cacheMapping, boolean fLog)
                throws IllegalArgumentException
            {
            if (!cacheMapping.usesRawTypes() && fLog)
                {
                CacheFactory
                    .log("The cache \"" + sCacheName + "\" has been configured as NamedCache<"
                            + cacheMapping.getKeyClassName() + "," + cacheMapping.getValueClassName()
                            + "> but the application is requesting the cache using raw types", CacheFactory.LOG_INFO);
                return false;
                }
            return true;
            }

        @Override
        public String toString()
            {
            return "WITH_RAW_TYPES";
            }
        };
    }
