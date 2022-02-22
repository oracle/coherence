/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.streams;

import com.tangosol.net.Coherence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static com.oracle.coherence.guides.streams.StreamsExample.STORAGE_ENABLED;

/**
 * Tests for {@link StreamsExample}.
 *
 * @author Tim Middleton 2022.02.16
 */
public class StreamsTest {
    
    @Test
    public void testExample() {
        System.setProperty(STORAGE_ENABLED, "true");
        System.setProperty("coherence.log.level", "3");
        StreamsExample example = new StreamsExample();
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
