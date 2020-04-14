/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.schema.annotation;


import com.tangosol.io.pof.DateMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Marks a date field in a {@link PortableType} as serializable.
 *
 * @author as  2013.04.23
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PortableDate
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
     * Serialization mode for date/time value.
     *
     * @return serialization mode for date/time value
     */
    DateMode mode() default DateMode.DATE_TIME;

    /**
     * Flag specifying whether to include timezone information when serializing
     * annotated field.
     *
     * @return whether to include timezone information
     */
    boolean includeTimezone() default false;
    }
