/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.management.annotation;

import com.tangosol.net.metrics.MBeanMetric;

import javax.management.DescriptorKey;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The MetricsScope descriptor annotation adds a metrics.scope field and its
 * value to the {@link javax.management.MBeanInfo#getDescriptor() descriptor}
 * for an MBean.
 *
 * @author jk  21.06.2019
 * @since 12.2.1.4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MetricsScope
    {
    @DescriptorKey(KEY)
    MBeanMetric.Scope value();

    /**
     * The MBean descriptor key.
     */
    String KEY = "metric.scope";

    /**
     * The full descriptor value to use for vendor scoped metrics.
     */
    String VENDOR = KEY + '=' + MBeanMetric.Scope.VENDOR.name();

    /**
     * The full descriptor value to use for vendor scoped metrics.
     */
    String APPLICATION = KEY + '=' + MBeanMetric.Scope.APPLICATION.name();

    /**
     * The full descriptor value to use for base scoped metrics.
     */
    String BASE = KEY + '=' + MBeanMetric.Scope.BASE.name();
    }
