/*
 * Copyright (c) 2000, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.sun.tools.visualvm.modules.coherence;

import org.junit.BeforeClass;

import java.net.UnknownHostException;

/**
 * Run DataRetriever tests using JMX API.
 */
public class JMXDataRetrieverTests
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
        startupCacheServers(false);
        }
    }
