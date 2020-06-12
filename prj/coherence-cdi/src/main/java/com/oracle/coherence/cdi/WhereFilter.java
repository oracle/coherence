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
import javax.enterprise.util.Nonbinding;

/**
 * A {@link com.oracle.coherence.cdi.FilterBinding} annotation representing a
 * {@link com.tangosol.util.Filter} produced from a CohQL where clause.
 *
 * @author Jonathan Knight  2019.10.24
 * @since 20.06
 */
@Inherited
@FilterBinding
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface WhereFilter
    {
    /**
     * The CohQL query expression.
     *
     * @return the CohQL query expression
     */
    @Nonbinding String value();

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link WhereFilter} annotation.
     */
    class Literal
            extends AnnotationLiteral<WhereFilter>
            implements WhereFilter
        {
        /**
         * Construct {@code Literal} instance.
         *
         * @param sQuery  the CohQL query expression
         */
        private Literal(String sQuery)
            {
            this.f_sQuery = sQuery;
            }

        /**
         * Create a {@link WhereFilter.Literal}.
         *
         * @param sQuery  the CohQL query expression
         *
         * @return a {@link WhereFilter.Literal} with the specified CohQL query
         */
        public static Literal of(String sQuery)
            {
            return new Literal(sQuery);
            }

        /**
         * The CohQL query expression.
         *
         * @return the CohQL query expression
         */
        public String value()
            {
            return f_sQuery;
            }

        // ---- data members ------------------------------------------------

        /**
         * The CohQL query expression.
         */
        private final String f_sQuery;
        }
    }
