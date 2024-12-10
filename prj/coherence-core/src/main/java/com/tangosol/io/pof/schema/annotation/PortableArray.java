/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.schema.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Marks an array field in a {@link PortableType} as serializable.
 *
 * @author as  2013.04.23
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PortableArray
    {
    /**
     * Type version this property was introduced in.
     *
     * @return type version this property was introduced in
     */
    int since() default 0;

    /**
     * The name that can be used to reference property.
     * <p>
     * If not specified it will default to field name.
     *
     * @return  the name that can be used to reference property
     */
    String name() default "";

    /**
     * Property order.
     * <p>
     * If not specified, it will be determined based on the combination of the
     * type version property was introduced in and the alphabetical order of
     * property names within a type version.
     *
     * @return property order
     */
    int order() default Integer.MAX_VALUE;

    /**
     * The class of the array elements.
     * <p>
     * If specified, it will be used to write the array into a POF stream using
     * uniform encoding.
     *
     * @return element class
     */
    Class<?> elementClass() default Object.class;

    /**
     * The flag specifying that a primitive array should be encoded using raw
     * encoding.
     * <p>
     * The raw encoding will typically perform better, but it may not be portable,
     * so it should only be used if serialization format portability is not required.
     *
     * @return whether the array should use raw encoding
     *
     * @since 24.09
     */
    boolean useRawEncoding() default false;
    }
