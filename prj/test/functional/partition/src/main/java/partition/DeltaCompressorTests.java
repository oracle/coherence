/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package partition;


import static org.junit.Assert.*;

import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.net.NamedCache;

import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The purpose of this test is to verify that a proxy using the standard
 * compressor can join a distributed cache where 'backup-count-after-writebehind'
 * is set to 0, meaning that the compressor used for extracting and applying
 * will be a null implementation.
 * <p>
 * To achieve this we need to use two separate cache configuration files one
 * for the proxy and one for the storage node.
 *
 * @author coh 2012.01.19
 */
public class DeltaCompressorTests
        extends AbstractFunctionalTest
    {
    private static final String STORAGE_NODE_NAME = "storage-node";
    private static final String PROXY_NAME = "proxy";
    private static final String PROJECT_NAME = "partition";

    public DeltaCompressorTests()
        {
        super("client-cache-config.xml");
        }


    // ----- test lifecycle -------------------------------------------------

    @BeforeClass
    public static void _startup()
        {
        System.setProperty(OperationalOverride.PROPERTY,
                           "common/tangosol-coherence-override.xml");

        Properties proxyProperties = new Properties();
        proxyProperties.setProperty("test.server.distributed.localstorage", "false");
        proxyProperties.setProperty(OperationalOverride.PROPERTY,
                                    "common/tangosol-coherence-override.xml");

        startCacheServer(PROXY_NAME, PROJECT_NAME, "proxy-cache-config.xml", proxyProperties);
        startCacheServer(STORAGE_NODE_NAME, PROJECT_NAME, "rwbm-cache-config.xml");
        }

    @AfterClass
    public static void _shutdown()
        {
        AbstractFunctionalTest._shutdown();
        stopCacheServer(STORAGE_NODE_NAME);
        stopCacheServer(PROXY_NAME);
        }

    @Test
    public void test()
        {
        final NamedCache cache = getNamedCache("distrwbm-cache");

        assertNull(cache.put(1, 1));
        assertEquals(1, cache.get(1));

        assertTrue(cache.isActive());
        cache.release();
        assertFalse(cache.isActive());
        }

    }
