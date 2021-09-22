/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package tcmp;


import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import common.AbstractFunctionalTest;

import java.util.Properties;

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
     * Basic test with multicast
     */
    @Test
    public void testCluster()
        {
        final LocalPlatform         platform     = LocalPlatform.get();
        final AvailablePortIterator ports        = platform.getAvailablePorts();
        final int                   nClusterPort = ports.next();

        System.setProperty("test.unicast.coherence.discovery.address", platform.getLoopbackAddress().getHostAddress());
        System.setProperty("test.multicast.address", AbstractFunctionalTest.generateUniqueAddress(true));
        System.setProperty("test.multicast.port", String.valueOf(nClusterPort));

        AbstractFunctionalTest._startup();

        NamedCache cache = CacheFactory.getCache("disco");

        startCacheServer("server1", "Discovery", "");
        startCacheServer("server2", "Discovery", "");

        Eventually.assertDeferred(() -> cache.getCacheService().getCluster().getMemberSet().size(),
                                  Matchers.is(3));
        }
    }
