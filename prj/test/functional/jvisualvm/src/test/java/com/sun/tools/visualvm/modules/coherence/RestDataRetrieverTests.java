package com.sun.tools.visualvm.modules.coherence;

import java.net.UnknownHostException;

import org.junit.BeforeClass;

/**
 * Tests for data retrieval using REST.
 *
 * @author sr 12.19.2017
 *
 * @since Coherence 19.1.0.0.0
 */
public class RestDataRetrieverTests
        extends AbstractDataRetrieverTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Set properties to not use the reporter.
     */
    @BeforeClass
    public static void _startup()
            throws UnknownHostException
        {
        System.setProperty("tangosol.coherence.override", "test-cluster-coherence-override.xml");
        System.setProperty("tangosol.coherence.cacheconfig", "test-cluster-cache-config.xml");
        System.setProperty("com.oracle.coherence.jvisualvm.reporter.disabled", "true");
        System.setProperty("com.oracle.coherence.jvisualvm.refreshtime", "1");

        startupCacheServers(true);
        }
    }
