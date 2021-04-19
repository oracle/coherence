/*
 * Copyright (c) 2000, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.nearcaching;

import com.tangosol.net.Coherence;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SimpleNearCachingExample} using a near cache that expires
 * after 8 seconds.
 *
 * @author Tim Middleton 2021.04.01
 */
// tag::test[]
public class SimpleExpiringNearCachingExampleTest {
    
    @Test
    public void testExpiringNearCache() throws Exception {
        System.setProperty("coherence.log.level", "3");
        SimpleNearCachingExample example = new SimpleNearCachingExample("expiring-cache-all", "all");
        example.runExample();
        
        Coherence coherence = Coherence.getInstance();
        if (coherence != null) {
            coherence.close();
        }
    }
}
// end::test[]
