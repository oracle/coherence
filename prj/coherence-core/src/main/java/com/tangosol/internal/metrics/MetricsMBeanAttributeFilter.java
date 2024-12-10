/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.metrics;

import com.tangosol.net.management.annotation.MetricsTag;
import com.tangosol.net.management.annotation.MetricsValue;
import com.tangosol.util.Filter;

import javax.management.MBeanAttributeInfo;
import java.io.Serializable;

/**
 * A {@link Filter} used by Coherence metrics to filter MBean attributes.
 * <p>
 * This class is located in Coherence Core so that it is always on the classpath
 * regardless of which member is the management senior.
 *
 * @author jk  2019.05.13
 *
 * @see MetricsValue
 * @see com.tangosol.net.management.annotation.MetricsTag
 */
public class MetricsMBeanAttributeFilter
        implements Filter<MBeanAttributeInfo>, Serializable
    {
    // ----- Filter methods ---------------------------------------------

    /**
     * Filter to determine if Coherence MBean attribute is appropriate to map to metrics.
     *
     * @param attrInfo MBean attribute information
     *
     * @return true if this coherence MBean attribute should be mapped to metric.
     */
    public boolean evaluate(MBeanAttributeInfo attrInfo)
        {
        Object[] listoMetricsFieldValues = attrInfo.getDescriptor().getFieldValues(MetricsTag.DESCRIPTOR_KEY, MetricsValue.DESCRIPTOR_KEY);

        if (listoMetricsFieldValues != null)
            {
            for (Object oValue : listoMetricsFieldValues)
                {
                if (oValue != null && !((String)oValue).isEmpty())
                    {
                    return true;
                    }
                }
            }

        return false;
        }

    // ----- constants --------------------------------------------------

    private static final long serialVersionUID = -1L;
    }
