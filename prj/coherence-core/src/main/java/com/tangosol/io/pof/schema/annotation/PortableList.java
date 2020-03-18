/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.schema.annotation;


import com.oracle.coherence.common.base.Factory;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;


/**
 * Marks a list field in a {@link PortableType} as serializable.
 *
 * @author as  2013.04.23
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PortableList
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
     * The property order.
     * <p>
     * If not specified, it will be determined based on the combination of the
     * type version property was introduced in and the alphabetical order of
     * property names within a type version.
     *
     * @return property order
     */
    int order() default Integer.MAX_VALUE;

    /**
     * The class of the list elements.
     * <p>
     * If specified, it will be used to write the list into a POF stream using
     * uniform encoding.
     *
     * @return element class
     */
    Class<?> elementClass() default Object.class;

    /**
     * The class of the list that should be created during deserialization.
     *
     * @return the list class
     */
    Class<? extends List> clazz() default ArrayList.class;

    /**
     * Factory class for the attribute.
     * <p>
     * This attribute allows you to specify a {@link Factory} implementation
     * that should be used to create property instance during deserialization.
     * It is typically used to better control deserialization of collections and
     * maps.
     *
     * @return factory class for the attribute
     */
    Class<? extends Factory> factory() default Factory.class;
    }
