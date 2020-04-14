/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.metrics;


import com.tangosol.net.metrics.MBeanMetric;

import java.util.Objects;


/**
 * A base class for Coherence metrics.
 *
 * @author jk  2019.06.21
 * @since 12.2.1.4
 */
public abstract class BaseMBeanMetric
        implements MBeanMetric
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a metric.
     *
     * @param identifier    this metrics identifier
     * @param sMBeanName    the MBean name
     * @param sDescription  the metric description
     */
    protected BaseMBeanMetric(Identifier identifier, String sMBeanName, String sDescription)
        {
        f_identifier   = Objects.requireNonNull(identifier);
        f_sMBean       = sMBeanName;
        f_sDescription = sDescription;
        }

    // ---- MBeanMetric interface -------------------------------------------

    @Override
    public Identifier getIdentifier()
        {
        return f_identifier;
        }

    @Override
    public String getMBeanName()
        {
        return f_sMBean;
        }

    @Override
    public String getDescription()
        {
        return f_sDescription;
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        BaseMBeanMetric that = (BaseMBeanMetric) o;
        return f_identifier.equals(that.f_identifier);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_identifier);
        }

    @Override
    public String toString()
        {
        return getClass().getSimpleName() + "{" +
               "identifier=" + f_identifier +
               ", mBeanName='" + f_sMBean + '\'' +
               ", description='" + f_sDescription + '\'' +
               ", value=" + getValue() +
               '}';
        }

    // ----- data members ---------------------------------------------------

    /**
     * This metrics identifier.
     */
    private final Identifier f_identifier;

    /**
     * The MBean name.
     */
    private final String f_sMBean;

    /**
     * The optional description text for this metric.
     */
    private final String f_sDescription;
    }
