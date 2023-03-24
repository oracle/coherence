/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package config;


import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.RequestTimeoutException;

import com.tangosol.util.Base;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * COH-26992 : Add system property for overriding cluster publisher
 * delivery timeout
 */
public class ClusterConfigOverrideTest
    {

    @After
    public void cleanup()
        {
        Coherence.closeAll();
        System.clearProperty("coherence.publisher.delivery.timeout");
        System.clearProperty("coherence.wka");
        }

    @Test
    public void shouldOverridePublisherDeliveryTimeout()
        {
        System.setProperty("coherence.publisher.delivery.timeout",
                           String.valueOf(PUBLISHER_DELIVERY_TIMEOUT));
        // invalid wka should result into publisher delivery timeout to kick in
        // based on the timeout value set using "coherence.publisher.delivery.timeout"
        System.setProperty("coherence.wka", "1.2.3.4");

        long ldt1 = Base.getSafeTimeMillis();
        try
            {
            CacheFactory.ensureCluster();

            fail("Exception should have been thrown!");
            }
        catch (Exception e)
            {
            assertTrue(e instanceof RequestTimeoutException);

            long ldt2 = Base.getSafeTimeMillis();
            assertTrue(String.format("Should have failed after configured timeout of %s instead got %s", PUBLISHER_DELIVERY_TIMEOUT, ldt2 - ldt1),
                       ldt2 < (ldt1 + (PUBLISHER_DELIVERY_TIMEOUT * 2)));
            }
        }

    // ----- data members ---------------------------------------------

    private static final long PUBLISHER_DELIVERY_TIMEOUT = 10000;
    }
