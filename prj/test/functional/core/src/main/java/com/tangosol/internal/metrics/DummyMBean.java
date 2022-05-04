/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.metrics;

import com.tangosol.net.management.annotation.MetricsTag;
import com.tangosol.net.management.annotation.MetricsValue;

/**
 * @author jk  2019.06.25
 */
public interface DummyMBean
    {
    @MetricsValue("custom_value")
    long getValueOne();

    @MetricsValue
    long getValueTwo();

    long getValueThree();

    @MetricsTag("custom_tag")
    String getTagValueOne();

    @MetricsTag
    String getTagValueTwo();
    }
