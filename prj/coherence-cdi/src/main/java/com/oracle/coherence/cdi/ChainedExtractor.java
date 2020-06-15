/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;

/**
 * A {@link ExtractorBinding} annotation representing a
 * {@link com.tangosol.util.extractor.ChainedExtractor}.
 *
 * @author Jonathan Knight  2019.10.25
 * @since 20.06
 */
@Inherited
@ExtractorBinding
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ChainedExtractor.Extractors.class)
public @interface ChainedExtractor
    {
    /**
     * Returns the a method or property name to use when creating a {@link
     * com.tangosol.util.extractor.ChainedExtractor}.
     * <p>
     * If the value does not end in {@code "()"} the value is assumed to be a
     * property name. If the value is prefixed with one of the accessor prefixes
     * {@code "get"} or {@code "is"} and ends in {@code "()"} this extractor is
     * a property extractor. Otherwise, if the value just ends in {@code "()"}
     * this value is considered a method name.
     *
     * @return the value used for the where clause when creating a {@link
     * com.tangosol.util.extractor.ChainedExtractor}
     */
    @Nonbinding String[] value();

    // ---- inner interface: Extractors -------------------------------------

    /**
     * A holder for the repeatable {@link ChainedExtractor} annotation.
     */
    @Inherited
    @ExtractorBinding
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface Extractors
        {
        /**
         * An array of {@link ChainedExtractor}s.
         *
         * @return an array of {@link ChainedExtractor}s
         */
        @Nonbinding ChainedExtractor[] value();

        // ---- inner class: Literal ----------------------------------------

        /**
         * An annotation literal for the {@link Extractors} annotation.
         */
        class Literal
                extends AnnotationLiteral<Extractors>
                implements Extractors
            {
            private Literal(ChainedExtractor... aExtractors)
                {
                f_aExtractors = aExtractors;
                }

            /**
             * Create an {@link Extractors.Literal}.
             *
             * @param extractors the extractors
             *
             * @return an {@link Extractors.Literal} containing the specified
             * extractors
             */
            public static Literal of(ChainedExtractor... extractors)
                {
                return new Literal(extractors);
                }

            /**
             * The extractor annotations contained in this annotation.
             *
             * @return the extractor annotations contained in this annotation
             */
            public ChainedExtractor[] value()
                {
                return f_aExtractors;
                }

            // ---- data members --------------------------------------------

            /**
             * The extractors value for this literal.
             */
            private final ChainedExtractor[] f_aExtractors;
            }
        }

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link ChainedExtractor} annotation.
     */
    class Literal
            extends AnnotationLiteral<ChainedExtractor>
            implements ChainedExtractor
        {
        /**
         * Construct {@code Literal} instance.
         *
         * @param asFields  the value used to create the extractor
         */
        private Literal(String[] asFields)
            {
            this.f_asFields = asFields;
            }

        /**
         * Create a {@link ChainedExtractor.Literal}.
         *
         * @param asFields  the value used to create the extractor
         *
         * @return a {@link ChainedExtractor.Literal} with the specified value
         */
        public static Literal of(String... asFields)
            {
            return new Literal(asFields);
            }

        /**
         * The value used for the where clause when creating a {@link
         * com.tangosol.util.extractor.ChainedExtractor}.
         *
         * @return the value used for the where clause when creating a {@link
         * com.tangosol.util.extractor.ChainedExtractor}
         */
        public String[] value()
            {
            return f_asFields;
            }

        // ---- data members ------------------------------------------------

        /**
         * The extractor value for this literal.
         */
        private final String[] f_asFields;
        }
    }
