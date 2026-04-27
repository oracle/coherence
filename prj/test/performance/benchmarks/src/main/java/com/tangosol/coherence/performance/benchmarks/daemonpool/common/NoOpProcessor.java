/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.daemonpool.common;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.Serializable;

/**
 * Minimal service-side workload used to validate benchmark wiring.
 *
 * @author Aleks Seovic  2026.04.25
 * @since 26.04
 */
public class NoOpProcessor
        extends AbstractProcessor<Integer, Integer, Integer>
        implements Serializable
    {
    @Override
    public Integer process(InvocableMap.Entry<Integer, Integer> entry)
        {
        Integer nValue = entry.getValue();
        return nValue == null ? -1 : nValue;
        }

    public static final NoOpProcessor INSTANCE = new NoOpProcessor();
    }
