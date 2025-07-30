/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.net.cache.CacheMap;

import javax.enterprise.util.Nonbinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Get the value from cache if present, invoke the method and cache the result
 * otherwise.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheGet
    {
    /**
     * The number of milliseconds until the cache entry expires.
     *
     * @return the number of milliseconds until the cache entry expires
     */
    @Nonbinding long ttl() default CacheMap.EXPIRY_DEFAULT;

    /**
     * An annotation literal for the {@link CacheGet} annotation.
     */
    class Literal
            extends AnnotationLiteral<CacheGet>
            implements CacheGet
        {
        public static final Literal INSTANCE = new Literal();

        /**
         * Construct {@link Literal} instance.
         */
        private Literal(long nTtl)
            {
            m_nTtl = nTtl;
            }

        /**
         * Construct {@link Literal} instance.
         */
        public Literal()
            {
            m_nTtl = CacheMap.EXPIRY_DEFAULT;
            }

        /**
         * Create a {@link Literal}.
         *
         * @param nTtl  the number of milliseconds until the cache entry expires
         *
         * @return a {@link Literal} with the specified value
         */
        public static Literal of(long nTtl)
            {
            return new Literal(nTtl);
            }

        /**
         * The number of milliseconds until the cache entry expires.
         *
         * @return the number of milliseconds until the cache entry expires.
         */
        @Override
        public long ttl()
            {
            return m_nTtl;
            }

        // ---- data members ------------------------------------------------

        /**
         * The number of milliseconds until the cache entry expires.
         */
        private final long m_nTtl;
        }
    }
