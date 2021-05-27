/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.topic;

import com.tangosol.util.Binary;

/**
 * A {@link NamedTopic.ElementCalculator} that calculates size of an element based on the
 * size of the serialized binary value.
 *
 * @author Jonathan Knight  2021.05.17
 * @since 21.06
 */
public class BinaryElementCalculator
        implements NamedTopic.ElementCalculator
    {
    @Override
    public int calculateUnits(Binary binElement)
        {
        return binElement != null ? binElement.length() : 0;
        }

    // ----- constants ------------------------------------------------------

    /**
     * A singleton instance of {@link BinaryElementCalculator}.
     */
    public static final BinaryElementCalculator INSTANCE = new BinaryElementCalculator();
    }
