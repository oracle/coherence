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


/**
 * Marks field in a {@link PortableType} as serializable.
 *
 * @author as  2013.04.23
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Portable
    {
    /**
     * Type version this property was introduced in.
     *
     * @return  type version this property was introduced in
     */
    int since() default 0;

    /**
     * The name that can be used to reference property.
     * <p>
     * If not specified it will default to field name, with first letter
     * converted to uppercase.
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
     * @return  property order
     */
    int order() default Integer.MAX_VALUE;

    /**
     * Property class.
     * <p>
     * This attribute is only meaningful if the property is
     * of a collection type, in which case it might be better to use one of the
     * more specific annotations, such as {@link PortableSet},
     * {@link PortableList} or {@link PortableMap}.
     *
     * @return  property class
     */
    Class<?> clazz() default Object.class;

    /**
     * Factory class for the property.
     * <p>
     * This attribute allows you to specify a {@link Factory} implementation
     * that should be used to create property instance during deserialization.
     * It is typically used to better control deserialization of collections and
     * maps.
     * <p>
     * This attribute is only meaningful if the property is
     * of a collection type, in which case it might be better to use one of the
     * more specific annotations, such as {@link PortableSet},
     * {@link PortableList} or {@link PortableMap}.
     *
     * @return factory class for the attribute
     */
    Class<? extends Factory> factory() default Factory.class;
    }
