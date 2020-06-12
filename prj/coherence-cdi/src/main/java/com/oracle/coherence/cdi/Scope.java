/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

/**
 * A qualifier annotation used when injecting Coherence resource to indicate
 * that those resource should be obtained from a specific {@link
 * com.tangosol.net.ConfigurableCacheFactory}.
 *
 * @author Jonathan Knight  2019.10.20
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Scope
    {
    /**
     * The scope name or URI used to identify a specific {@link
     * com.tangosol.net.ConfigurableCacheFactory}.
     *
     * @return the scope name or URI used to identify a specific
     *         {@link com.tangosol.net.ConfigurableCacheFactory}
     */
    @Nonbinding String value() default DEFAULT;

    /**
     * Predefined constant for system scope.
     */
    String DEFAULT = "";

    /**
     * Predefined constant for system scope.
     */
    String SYSTEM = "SYS";

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link Scope} annotation.
     */
    class Literal
            extends AnnotationLiteral<Scope>
            implements Scope
        {
        /**
         * Construct {@code Literal} instacne.
         *
         * @param sValue  the scope name or URI used to identify a specific
         *                {@link com.tangosol.net.ConfigurableCacheFactory}
         */
        private Literal(String sValue)
            {
            m_sValue = sValue;
            }

        /**
         * Create a {@link Scope.Literal}.
         *
         * @param sValue  the scope name or URI used to identify a specific
         *                {@link com.tangosol.net.ConfigurableCacheFactory}
         *
         * @return a {@link Scope.Literal} with the specified URI
         */
        public static Literal of(String sValue)
            {
            return new Literal(sValue);
            }

        /**
         * Obtain the name value.
         *
         * @return the name value
         */
        public String value()
            {
            return m_sValue;
            }

        // ---- data members ------------------------------------------------

        /**
         * The value for this literal.
         */
        private final String m_sValue;
        }
    }
