package com.sun.tools.visualvm.modules.coherence;

import java.net.UnknownHostException;

import org.junit.BeforeClass;

/**
 * Tests for aggregation usage via REST in the plugin.
 *
 * @author shyaradh 12.19.2017
 *
 * @since Coherence 12.3.1
 */
public class RestAggregatedDataRetrieverTests
            extends AbstractDataRetrieverTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Set properties to specifically use the reporter.
     */
    @BeforeClass
    public static void _startup()
            throws UnknownHostException
        {
        System.setProperty("tangosol.coherence.override", "test-cluster-coherence-override.xml");
        System.setProperty("tangosol.coherence.cacheconfig", "test-cluster-cache-config.xml");
        System.setProperty("com.oracle.coherence.jvisualvm.refreshtime", "1");
        startupCacheServers(true);
        }
    }
