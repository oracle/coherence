/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.cdi;

import com.oracle.coherence.cdi.AnnotationLiteral;

import javax.enterprise.util.Nonbinding;

import javax.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A qualifier annotation used when injecting Coherence resource to specify
 * the latch count.
 *
 * @author as, lh  2021.11.29
 * @since 20.12
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Count
    {
    /**
     * The latch count
     *
     * @return the latch count
     */
    @Nonbinding int value() default 1;

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link Count} annotation.
     */
    class Literal
            extends AnnotationLiteral<Count>
            implements Count
        {
        /**
         * Construct {@link Literal} instance.
         *
         * @param nCount  the count
         */
        private Literal(int nCount)
            {
            m_nCount = nCount;
            }

        /**
         * Create a {@link Literal}.
         *
         * @param nCount  the latch count
         *
         * @return a {@link Literal} with the specified value
         */
        public static Literal of(int nCount)
            {
            return new Literal(nCount);
            }

        /**
         * The count for the countdown latch.
         *
         * @return the latch count
         */
        public int value()
            {
            return m_nCount;
            }

        // ---- data members ------------------------------------------------

        /**
         * The latch count.
         */
        private final int m_nCount;
        }
    }
