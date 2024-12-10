/*
 * Copyright (c) 2000, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.aggregations;

import com.tangosol.net.Coherence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SimpleAggregationExample}.
 *
 * @author Tim Middleton 2021.04.01
 */
public class SimpleAggregationTest {
    
    @Test
    public void testExample() {
        SimpleAggregationExample.setStorageEnabled(true);
        System.setProperty("coherence.log.level", "3");
        SimpleAggregationExample example = new SimpleAggregationExample();
        example.populate();
        example.runExample();
    }

    @AfterAll
    public static void shutdownCoherence() {
        Coherence coherence = Coherence.getInstance();
        if (coherence != null) {
            coherence.close();
        }
    }
}
