/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.cdi;

import com.oracle.coherence.cdi.AnnotationLiteral;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.enterprise.util.Nonbinding;

import javax.inject.Qualifier;


/**
 * A qualifier annotation used when injecting Coherence resource to specify
 * the permits count.
 *
 * @author Vaso Putica  2021.12.01
 * @since 21.12
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Permits
    {
    /**
     * The permits count
     *
     * @return the permits count
     */
    @Nonbinding int value() default 0;

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link Permits} annotation.
     */
    class Literal
            extends AnnotationLiteral<Permits>
            implements Permits
        {
        /**
         * Construct {@link Literal} instance.
         *
         * @param sPermits  the permits count
         */
        private Literal(int sPermits)
            {
            m_sPermits = sPermits;
            }

        /**
         * Create a {@link Literal}.
         *
         * @param sPermits  the permits count
         *
         * @return a {@link Literal} with the specified value
         */
        public static Literal of(int sPermits)
            {
            return new Literal(sPermits);
            }

        /**
         * The permits count for the semaphore.
         *
         * @return the permits count
         */
        public int value()
            {
            return m_sPermits;
            }

        // ---- data members ------------------------------------------------

        /**
         * The permits count.
         */
        private final int m_sPermits;
        }
    }
