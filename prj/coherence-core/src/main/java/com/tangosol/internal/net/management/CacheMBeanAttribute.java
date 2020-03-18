/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import java.util.stream.Collector;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

/**
 * CacheMBeanAttribute defines the attributes that should be aggregated with
 * a collector other than the default list collector.
 * <p>
 * There are 3 levels in which the collector for an attribute can be determined.
 * These are specified below in order of precedence:
 * <ol>
 *     <li><b>MBean Attribute</b> - An enum of type CacheMBeanAttribute for
 *     Cache and Storage MBeans with the collector to use being programmatically set.
 *     Note: this allows the greatest flexibility in defining the Collector to be used.</li>
 *     <li><b>Model</b> - The Model attribute has a descriptor annotation that
 *     specifies the collector that should be used and is the primary means to
 *     specify the collector if one of the standard collectors suffice.</li>
 *     <li><b>Default</b> - The list collector is used if no other collector is
 *     derived.</li>
 * </ol>
 *
 * @author hr  2016.07.21
 * @since 12.2.1.4.0
 */
@SuppressWarnings("unchecked")
public enum CacheMBeanAttribute
    implements MBeanAttribute
    {
        MemoryUnits(groupingBy(o -> o, counting())), // {false -> m, true -> n}
        ;
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a CacheMBeanAttribute with the provided {@link Collector}.
     *
     * @param fVisible   whether the attribute should be returned as a part
     *                   of a response
     */
    CacheMBeanAttribute(boolean fVisible)
        {
        this(null, NullCollector.INSTANCE(), fVisible);
        }

    /**
     * Construct a CacheMBeanAttribute with the provided {@link Collector}.
     *
     * @param collector  a Collector to use for this attribute
     */
    CacheMBeanAttribute(Collector collector)
        {
        this(null, collector);
        }

    /**
     * Construct a CacheMBeanAttribute with the provided name and {@link Collector}.
     * <p>
     * The name provides an ability to override the service attribute name.
     *
     * @param sName      the name to use as a part of the returned response
     * @param collector  the Collector implementation to use
     */
    CacheMBeanAttribute(String sName, Collector collector)
        {
        this(sName, collector, true);
        }

    /**
     * Construct a CacheMBeanAttribute with the provided name, a {@link Collector}
     * and whether the attribute is visible.
     * <p>
     * The name provides an ability to override the service attribute name.
     *
     * @param sName      the name to use as a part of the returned response
     * @param collector  the Collector implementation to use
     * @param fVisible   whether the attribute should be returned as a part
     *                   of a response
     */
    CacheMBeanAttribute(String sName, Collector collector, boolean fVisible)
        {
        f_collector = collector;
        f_sName     = sName;
        f_fVisible  = fVisible;
        }

    // ----- MBeanAttribute interface ---------------------------------------

    @Override
    public Collector collector()
        {
        return f_collector;
        }

    @Override
    public String description()
        {
        return f_sName == null ? name() : f_sName;
        }

    @Override
    public boolean isVisible()
        {
        return f_fVisible;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The Collector to use to aggregate this attribute.
     */
    protected final Collector f_collector;

    /**
     * The name to use instead of this enum's name, or null.
     */
    protected final String    f_sName;

    /**
     * Whether this attribute should be returned as a part of a response.
     */
    protected final boolean   f_fVisible;
    }
