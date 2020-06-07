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
 * A {@link ExtractorBinding} annotation representing a {@link
 * com.tangosol.util.extractor.UniversalExtractor}.
 *
 * @author Jonathan Knight  2019.10.25
 */
@Inherited
@ExtractorBinding
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(PropertyExtractor.Extractors.class)
public @interface PropertyExtractor
    {
    /**
     * Returns the a method or property name to use when creating a {@link
     * com.tangosol.util.extractor.UniversalExtractor}.
     * <p>
     * If the value does not end in {@code "()"} the value is assumed to be a
     * property name. If the value is prefixed with one of the accessor prefixes
     * {@code "get"} or {@code "is"} and ends in {@code "()"} this extractor is
     * a property extractor. Otherwise, if the value just ends in {@code "()"}
     * this value is considered a method name.
     *
     * @return the value used for the where clause when creating a {@link
     * com.tangosol.util.extractor.UniversalExtractor}
     */
    @Nonbinding String value();

    // ---- inner interface: Extractors -------------------------------------

    /**
     * A holder for the repeatable {@link PropertyExtractor} annotation.
     */
    @Inherited
    @ExtractorBinding
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface Extractors
        {
        @Nonbinding PropertyExtractor[] value();

        // ---- inner class: Literal ----------------------------------------

        /**
         * An annotation literal for the {@link Extractors} annotation.
         */
        class Literal
                extends AnnotationLiteral<Extractors>
                implements Extractors
            {
            /**
             * Construct {@code Literal} instance.
             *
             * @param aExtractors  the extractors
             */
            private Literal(PropertyExtractor... aExtractors)
                {
                f_aExtractors = aExtractors;
                }

            /**
             * Create an {@link Extractors.Literal}.
             *
             * @param aExtractors  the extractors
             *
             * @return an {@link Extractors.Literal} containing the specified
             * extractors
             */
            public static Literal of(PropertyExtractor... aExtractors)
                {
                return new Literal(aExtractors);
                }

            /**
             * Obtain the extractor annotations contained in this annotation.
             *
             * @return the extractor annotations contained in this annotation
             */
            public PropertyExtractor[] value()
                {
                return f_aExtractors;
                }

            // ---- data members --------------------------------------------

            /**
             * The extractors array for this literal.
             */
            private final PropertyExtractor[] f_aExtractors;
            }
        }

    // ---- inner class: Literal ----------------------------------------

    /**
     * An annotation literal for the {@link PropertyExtractor} annotation.
     */
    class Literal
            extends AnnotationLiteral<PropertyExtractor>
            implements PropertyExtractor
        {
        /**
         * Construct {@code Literal} instance.
         *
         * @param sPropertyName  the name of the property to extract
         */
        private Literal(String sPropertyName)
            {
            f_sPropertyName = sPropertyName;
            }

        /**
         * Create a {@link PropertyExtractor.Literal}.
         *
         * @param sPropertyName  the name of the property to extract
         *
         * @return a {@link PropertyExtractor.Literal} with the specified value
         */
        public static Literal of(String sPropertyName)
            {
            return new Literal(sPropertyName);
            }

        /**
         * The name of the property to extract.
         *
         * @return the name of the property to extract
         */
        public String value()
            {
            return f_sPropertyName;
            }

        // ---- data members --------------------------------------------

        /**
         * The name of the property to extract.
         */
        private final String f_sPropertyName;
        }
    }
