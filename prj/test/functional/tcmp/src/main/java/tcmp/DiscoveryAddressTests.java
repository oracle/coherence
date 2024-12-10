/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package tcmp;


import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.hamcrest.Matchers;

import org.junit.Test;

/**
 * Test for unicast-listener/discovery-address setting
 */
public class DiscoveryAddressTests
        extends AbstractFunctionalTest
    {
    // ----- test methods ---------------------------------------------------

    /**
     * Basic test with multicast using default system property.
     *
     * @since 12.2.1.4.10
     */
    @Test
    public void testClusterDefault()
        {
        testCluster("");
        }

    /**
     * Basic test with multicast using system property defined in the override.
     */
    @Test
    public void testClusterOverride()
        {
        testCluster("test.unicast.coherence.discovery.address");
        }

    private void testCluster(String sProperty)
        {
        final LocalPlatform         platform     = LocalPlatform.get();
        final AvailablePortIterator ports        = platform.getAvailablePorts();
        final int                   nClusterPort = ports.next();
        String                      sServer1;
        String                      sServer2;

        if (sProperty == null || sProperty.length() == 0)
            {
            sServer1 = "DefaultServer1";
            sServer2 = "DefaultServer2";
            System.setProperty("coherence.discovery.address", platform.getLoopbackAddress().getHostAddress());
            }
        else
            {
            sServer1 = "OverrideServer1";
            sServer2 = "OverrideServer2";
            System.setProperty(sProperty, platform.getLoopbackAddress().getHostAddress());
            System.setProperty("coherence.override", "coherence-discovery-override.xml");
            }
        System.setProperty("test.multicast.address", AbstractFunctionalTest.generateUniqueAddress(true));
        System.setProperty("test.multicast.port", String.valueOf(nClusterPort));

        AbstractFunctionalTest._startup();

        NamedCache cache = CacheFactory.getCache("disco");

        try (CoherenceClusterMember member1 = startCacheServer(sServer1, "Discovery", "");
             CoherenceClusterMember member2 = startCacheServer(sServer2, "Discovery", ""))
            {
            Eventually.assertDeferred(() -> cache.getCacheService().getCluster().getMemberSet().size(),
                    Matchers.is(3));
            }
        }
    }
