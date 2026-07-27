/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import java.util.concurrent.atomic.LongAdder;

/**
 * Serialization gate tuple counter implementation.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public class SerializationGateCounter
        implements SerializationFilterCheckMBean, SerializationFmtCheckMBean,
                   SerializationPofCheckMBean, SerializationLambdaBytecodeCheckMBean
    {
    /**
     * Increment the counter.
     */
    public void increment()
        {
        f_counter.increment();
        }

    @Override
    public long getCount()
        {
        return f_counter.sum();
        }

    // ----- data members ----------------------------------------------------

    /**
     * The tuple counter.
     */
    private final LongAdder f_counter = new LongAdder();
    }
