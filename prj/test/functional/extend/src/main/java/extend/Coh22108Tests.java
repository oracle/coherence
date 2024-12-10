/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.tangosol.coherence.component.net.extend.remoteService.RemoteCacheService;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator;

import com.tangosol.coherence.component.util.safeService.SafeCacheService;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import java.util.Set;

import java.util.function.Supplier;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertThat;

/**
 * Unit test for {@code COH-22108 - RFA: Memory Leak on TcpInitiator for TCP Extend clients}.
 *
 * @since 12.2.1.3
 * @author rl 10.7.2020
 */
public class Coh22108Tests
        extends AbstractExtendTest
    {
    // ----- constructors ---------------------------------------------------

    public Coh22108Tests()
        {
        super(CACHE_NAME, CLIENT_CONFIG_XML);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy(SERVER_NAME, SERVER_CONFIG_XML);
        }

    // ----- test methods ---------------------------------------------------

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testCoh22108()
        {
        NamedCache cache = getNamedCache();

        TcpInitiator initiator = (TcpInitiator)
                ((RemoteCacheService)
                         ((SafeCacheService) cache.getCacheService()).getRunningCacheService()).getInitiator();
        final Set setSockets = initiator.getCloseOnExit();

        Supplier<String> msg = () -> "Expected setSockets to be empty, but size was " + setSockets.size();

        assertThat(msg.get(), setSockets.isEmpty(), is(true));

        // stop the cache server and ensure sockets don't leak after failed connections
        stopCacheServer(SERVER_NAME, false);

        for (int i = 0; i < 10; i++)
            {
            try
                {
                cache.put("a", "b");
                }
            catch (Exception e)
                {
                CacheFactory.log("Exception raised: " + e.toString(), CacheFactory.LOG_INFO);
                }
            }

        assertThat(msg.get(), setSockets.isEmpty(), is(true));
        }

    // ----- constants ------------------------------------------------------

    /**
     * The cache server name.
     */
    protected static final String SERVER_NAME = "cache-coh22108-tests";

    /**
     * The cache server configuration file.
     */
    protected static final String SERVER_CONFIG_XML = "server-cache-config.xml";

    /**
     * The test cache name.
     */
    protected static final String CACHE_NAME = "dist-extend";

    /**
     * The client configuration file.
     */
    protected static final String CLIENT_CONFIG_XML = "client-cache-config-coh22108.xml";
    }
