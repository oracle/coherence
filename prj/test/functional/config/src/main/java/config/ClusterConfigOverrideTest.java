/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package config;


import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
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

        Cluster cluster = null;
        long    ldt1    = 0L;

        try
            {
            ldt1 = Base.getSafeTimeMillis();

            cluster = CacheFactory.ensureCluster();

            fail("Exception should have been thrown!");
            }
        catch (Exception e)
            {
            long ldt2 = Base.getSafeTimeMillis();

            assertTrue(e instanceof RequestTimeoutException);

            long ldiff = ldt2 - ldt1;

            assertTrue(String.format("Should have failed after configured timeout of %s millis instead got %s millis",
                       PUBLISHER_DELIVERY_TIMEOUT, ldiff), ldiff < PUBLISHER_DELIVERY_TIMEOUT * 3);
            }
        finally
            {
            if (cluster != null)
                {
                cluster.shutdown();
                }
            }
        }

    // ----- data members ---------------------------------------------

    private static final long PUBLISHER_DELIVERY_TIMEOUT = 10000L;
    }
