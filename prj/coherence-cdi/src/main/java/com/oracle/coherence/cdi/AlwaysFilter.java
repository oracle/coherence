/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.enterprise.util.AnnotationLiteral;

/**
 * A {@link com.oracle.coherence.cdi.FilterBinding} annotation representing an
 * {@link com.tangosol.util.filter.AlwaysFilter}.
 *
 * @author Jonathan Knight  2019.10.24
 * @since 20.06
 */
@FilterBinding
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface AlwaysFilter
    {
    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link com.oracle.coherence.cdi.AlwaysFilter}
     * annotation.
     */
    class Literal
            extends AnnotationLiteral<AlwaysFilter>
            implements AlwaysFilter
        {
        /**
         * Construct {@code Literal} instance.
         */
        private Literal()
            {
            }

        // ---- constants ---------------------------------------------------

        /**
         * A {@link com.oracle.coherence.cdi.AlwaysFilter.Literal} instance.
         */
        public static final Literal INSTANCE = new Literal();
        }
    }
