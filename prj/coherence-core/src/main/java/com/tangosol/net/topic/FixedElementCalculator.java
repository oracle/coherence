/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.topic;

import com.tangosol.internal.net.topic.impl.paged.PagedTopic;

import com.tangosol.util.Binary;

/**
 * A fixed implementation of a {@link NamedTopic.ElementCalculator} that gives all elements a size of 1.
 *
 * @author Jonathan Knight  2021.05.17
 * @since 21.06
 */
public class FixedElementCalculator
        implements NamedTopic.ElementCalculator
    {
    @Override
    public int calculateUnits(Binary binElement)
        {
        return 1;
        }

    // ----- constants ------------------------------------------------------

    public static final FixedElementCalculator INSTANCE = new FixedElementCalculator();
    }
