/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.management.annotation;

import javax.management.DescriptorKey;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The MetricsLabels descriptor annotation adds a <code>metrics.labels</code> field
 * and its value to the {@link javax.management.MBeanAttributeInfo#getDescriptor() descriptor}
 * for a method on an MBean. The existence of this annotation on
 * an MBean attribute indicates that the MBean attribute is mapped to
 * a metrics value with additional tags.
 * By default, an MBean attribute is not mapped to a {@link MetricsValue metrics value} or
 * metrics tag.
 * <p>
 * This annotation is intended to be put on {@link javax.management.MBeanAttributeInfo#getType()
 * MBean attribute type's} containing meta info about all the metrics.values associated with an MBean.
 *
 * @author Jonathan Knight  2020.10.14
 * @since 20.12
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MetricsLabels
    {
    /**
     * Returns the additional metric tags.
     * <p>
     * The value should be a String array of key/value pairs,
     * therefore there should be an even number of String values.
     *
     * @return the additional metric tags
     */
    @DescriptorKey(DESCRIPTOR_KEY)
    String[] value();

    /**
     * The MBean descriptor key that this annotation creates.
     */
    String DESCRIPTOR_KEY = "metric.labels";
    }
