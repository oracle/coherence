package com.sun.tools.visualvm.modules.coherence;


import org.junit.BeforeClass;

import java.net.UnknownHostException;


/**
 * Run DataRetriever tests using Reporter API where available.
 */
public class ReporterDataRetrieverTests
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
        startupCacheServers(false);
        }
    }
