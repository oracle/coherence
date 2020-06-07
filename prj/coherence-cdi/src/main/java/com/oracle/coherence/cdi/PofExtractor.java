/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.io.pof.generator.PortableTypeGenerator;
import com.tangosol.io.pof.schema.annotation.PortableType;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;

/**
 * A {@link ExtractorBinding} annotation representing a {@link
 * com.tangosol.util.extractor.PofExtractor}.
 * <p>
 * This annotation can be used to define an extractor that extracts and attribute
 * from a POF stream based on an array of integer property indices, in which
 * case the type is optional, or a property path based on serialized field names
 * concatenated using period (ie. {@code address.city}, in which case {@link
 * #type()} attribute must be set as well.
 * <p>
 * The latter approach can only be used if the specified type is annotated with a
 * {@link PortableType @PortableType} annotation and has been instrumented using
 * {@link PortableTypeGenerator} (typically via {@code pof-maven-plugin}).
 * <p>
 * Either {@link #index()} or {@link #path()} must be specified within this
 * annotation in order for it to be valid.
 *
 * @author Jonathan Knight  2019.10.25
 * @author Aleks Seovic  2020.06.06
 */
@Inherited
@ExtractorBinding
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(PofExtractor.Extractors.class)
public @interface PofExtractor
    {
    /**
     * Returns an array of POF indexes to use to extract the value.
     *
     * @return an array of POF indexes to use to extract the value
     */
    @Nonbinding int[] index() default {};

    /**
     * Returns a property path to use to extract the value.
     * <p>
     * This attribute can only be used in combination with the {@link #type()}
     * attribute, and only if the specified type is annotated with a
     * {@link PortableType @PortableType} annotation and instrumented using
     * {@link PortableTypeGenerator}.
     *
     * @return a property path to use to extract the value
     */
    @Nonbinding String path() default "";

    /**
     * Returns the root type to extract property from.
     *
     * @return the root type to extract property from
     */
    @Nonbinding Class<?> type() default Object.class;

    // ---- inner interface: Extractors -------------------------------------
    
    /**
     * A holder for the repeatable {@link PofExtractor} annotation.
     */
    @Inherited
    @ExtractorBinding
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
         * Construct {@code Literal} instance.
         *
         * @param clzType  the root type to extract property from
         * @param anIndex  an array of POF indexes to use to extract the value
         * @param sPath    a property path to use to extract the value
         */
        private Literal(Class<?> clzType, int[] anIndex, String sPath)
            {
            f_clzType = clzType;
            f_anIndex = anIndex;
            f_sPath   = sPath;
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
            return new Literal(Object.class, value, "");
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
            return new Literal(type, value, "");
            }

       /**
         * Create a {@link PofExtractor.Literal}.
         *
         * @param sPath  the POF indexes to use to extract the value
         *
         * @return a {@link PofExtractor.Literal} with the specified value
         */
        public static Literal of(Class<?> type, String sPath)
            {
            return new Literal(type, new int[] {}, sPath);
            }

        /**
         * The POF indexes to use to extract a value.
         *
         * @return the POF indexes to use to extract a value
         */
        public int[] index()
            {
            return f_anIndex;
            }

        /**
        * Returns a property path to use to extract the value.
        * <p>
        * This attribute can only be used in combination with the {@link #type()}
        * attribute, and only if the specified type is annotated with a
        * {@link PortableType @PortableType} annotation and instrumented using
        * {@link PortableTypeGenerator}.
        *
        * @return a property path to use to extract the value
        */
        public String path()
            {
            return f_sPath;
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
        private final int[] f_anIndex;

        /**
         * The property path to use to extract the value
         */
        private final String f_sPath;

        /**
         * The type being extracted.
         */
        private final Class<?> f_clzType;
        }
    }
