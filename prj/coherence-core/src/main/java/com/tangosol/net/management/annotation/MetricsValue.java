/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.management.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.management.DescriptorKey;
import javax.management.MBeanAttributeInfo;

/**
 * The MetricsValue descriptor annotation adds a metrics.value field
 * and its value to the {@link MBeanAttributeInfo#getDescriptor() descriptor}
 * for a method on an MBean. The existence of this annotation on
 * an MBean attribute indicates that the MBean attribute is mapped to
 * a metrics value with the metrics name being {@link #value()}.
 * By default, an MBean attribute is not mapped to a metrics value or
 * {@link MetricsTag}.
 * <p>
 * This annotation is only allowed to be put on {@link javax.management.MBeanAttributeInfo#getType()
 * MBean attribute type's} of numeric primitives or numeric classes deriving from
 * {@link Number}. Map non-numerics MBean attribute types to {@link MetricsTag}.
 *
 * @author jf  9.27.2018
 * @since 12.2.1.4.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MetricsValue
    {
    @DescriptorKey(DESCRIPTOR_KEY)
    String value() default DEFAULT;

    String DEFAULT = "_default";

    String DESCRIPTOR_KEY = "metrics.value";
    }
