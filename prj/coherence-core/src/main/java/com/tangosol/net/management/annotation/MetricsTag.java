/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.management.annotation;

import javax.management.DescriptorKey;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The MetricsTag descriptor annotation adds a <code>metrics.tag</code> field
 * and its value to the {@link javax.management.MBeanAttributeInfo#getDescriptor() descriptor}
 * for a method on an MBean. The existence of this annotation on
 * an MBean attribute indicates that the MBean attribute is mapped to
 * a metrics tag with the metrics tag name being {@link #value()}.
 * By default, an MBean attribute is not mapped to a {@link MetricsValue metrics value} or
 * metrics tag.
 * <p>
 * This annotation is intended to be put on {@link javax.management.MBeanAttributeInfo#getType()
 * MBean attribute type's} containing metainfo about all the metrics.values associated with an MBean.
 *
 * @author jf  9.27.2018
 * @since 12.2.1.4.0
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MetricsTag
    {
    /**
     * Returns the metrics name, expected to be in snake case, for the MBean
     * attribute.  If value is <code>_default</code>, a snake case
     * metric name is generated from the {@link javax.management.MBeanAttributeInfo#getName()
     * MBean attribute name}.
     * <p>
     * To allow short form assignments of single member annotations, "value" must
     * be used (Java convention).
     *
     * @return a snake case metrics name for MBean attribute or "_default"
     */
    @DescriptorKey(DESCRIPTOR_KEY)
    String value() default DEFAULT;

    String DEFAULT = "_default";

    String DESCRIPTOR_KEY = "metrics.tag";
    }
