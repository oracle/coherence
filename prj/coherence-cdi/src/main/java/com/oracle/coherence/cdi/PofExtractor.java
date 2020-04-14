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
 * A {@link ValueExtractorBinding} annotation representing a {@link
 * com.tangosol.util.extractor.PofExtractor}.
 *
 * @author Jonathan Knight  2019.10.25
 */
@Inherited
@ValueExtractorBinding
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(PofExtractor.Extractors.class)
public @interface PofExtractor
    {
    /**
     * Returns the POF indexes to use to extract the value.
     *
     * @return the POF indexes to use to extract the value
     */
    @Nonbinding int[] value();

    /**
     * Returns the type being extracted.
     *
     * @return the type being extracted
     */
    @Nonbinding Class<?> type() default Object.class;

    // ---- inner interface: Extractors -------------------------------------
    
    /**
     * A holder for the repeatable {@link PofExtractor} annotation.
     */
    @Inherited
    @ValueExtractorBinding
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface Extractors
        {
        @Nonbinding PofExtractor[] value();

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
             * @param aExtractors the extractors
             */
            private Literal(PofExtractor... aExtractors)
                {
                m_aExtractors = aExtractors;
                }

            /**
             * Create an {@link Extractors.Literal}.
             *
             * @param aExtractors the extractors
             *
             * @return an {@link Extractors.Literal} containing the specified
             * extractors
             */
            public static Literal of(PofExtractor... aExtractors)
                {
                return new Literal(aExtractors);
                }

            /**
             * The extractor annotations contained in this annotation.
             *
             * @return the extractor annotations contained in this annotation
             */
            public PofExtractor[] value()
                {
                return m_aExtractors;
                }

            // ---- data members --------------------------------------------
            
            /**
             * The extractors value for this literal.
             */
            private final PofExtractor[] m_aExtractors;
            }
        }

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link com.oracle.coherence.cdi.PofExtractor}
     * annotation.
     */
    class Literal
            extends AnnotationLiteral<PofExtractor>
            implements PofExtractor
        {
        /**
         *
         * @param clzType
         * @param anIndices
         */
        private Literal(Class<?> clzType, int[] anIndices)
            {
            f_clzType   = clzType;
            f_anIndices = anIndices;
            }

        /**
         * Create a {@link PofExtractor.Literal}.
         *
         * @param value the POF indexes to use to extract the value
         *
         * @return a {@link PofExtractor.Literal} with the specified value
         */
        public static Literal of(int... value)
            {
            return new Literal(Object.class, value);
            }

        /**
         * Create a {@link PofExtractor.Literal}.
         *
         * @param value the POF indexes to use to extract the value
         *
         * @return a {@link PofExtractor.Literal} with the specified value
         */
        public static Literal of(Class<?> type, int... value)
            {
            return new Literal(type, value);
            }

        /**
         * The POF indexes to use to extract a value.
         *
         * @return the POF indexes to use to extract a value
         */
        public int[] value()
            {
            return f_anIndices;
            }

        /**
         * The type being extracted.
         *
         * @return the type being extracted
         */
        public Class<?> type()
            {
            return f_clzType;
            }

        // ---- data members ------------------------------------------------

        /**
         * The POF indexes to use to extract the value.
         */
        private final int[] f_anIndices;

        /**
         * The type being extracted.
         */
        private final Class<?> f_clzType;
        }
    }
