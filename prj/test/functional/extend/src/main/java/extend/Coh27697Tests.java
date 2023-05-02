/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.coherence.component.net.extend.RemoteNamedCache;

import com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory;

import com.tangosol.coherence.component.util.SafeNamedCache;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.NearCache;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test for {@code COH-27697 - Add isReady() to NamedMap interface}.
 *
 * @since 14.1.1.2206.5
 * @author rl 4.27.2023
 */
@RunWith(Parameterized.class)
public class Coh27697Tests
        extends AbstractExtendTest
    {
    // ----- constructors ---------------------------------------------------

    public Coh27697Tests(String sCacheName)
        {
        super(sCacheName, CLIENT_CONFIG_XML);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Start the storage and proxy members necessary for the test.
     */
    @Before
    public void startup()
        {
        startStorage(true);
        startProxy(true);
        }

    /**
     * Stop all storage and proxy members used by the test.
     */
    @After
    public void shutdown()
        {
        stopCacheServer(STORAGE_NAME);
        stopCacheServer(PROXY_NAME);
        }

    // ----- test methods ---------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void testIsReady()
        {
        NamedCache<String, String> cache = getNamedCache();

        // ensure isActive() and isReady return true
        assertThat(cache.isActive(), is(true));
        assertThat(cache.isReady(), is(true));

        // gracefully stop storage node
        stopCacheServer(STORAGE_NAME, true);

        // ensure isActive() still returns true as the service is still running
        // ensure isReady() returns false as there is no storage
        assertThat(cache.isActive(), is(true));
        assertThat(cache.isReady(), is(false));

        int cNum = 0;
        while (cNum++ < 10)
            {
            // start storage; don't wait for server start to exercise API
            startStorage(false);

            // ensure isActive() and isReady return true
            assertThat(cache.isActive(), is(true));
            Eventually.assertDeferred(cache::isReady, is(true));

            // stop storage; don't wait for server termination to exercise API
            stopCacheServer(STORAGE_NAME, false);

            assertThat(cache.isActive(), is(true));
            Eventually.assertDeferred(cache::isReady, is(false));
            }

        stopCacheServer(STORAGE_NAME, true);
        stopCacheServer(PROXY_NAME, true);

        // ensure isActive() and isReady return false
        assertThat(cache.isActive(), is(false));
        assertThat(cache.isReady(), is(false));
        }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test(expected = UnsupportedOperationException.class)
    public void shouldThrowUSOEWhenOldProtocolUsed()
        {
        NamedCache<String, String> cache       = getNamedCache();
        RemoteNamedCache           remoteCache;

        if (cache instanceof SafeNamedCache)
            {
            remoteCache = (RemoteNamedCache) ((SafeNamedCache) cache).getNamedCache();
            }
        else if (cache instanceof NearCache)
            {
            NamedCache c = ((NearCache) cache).getBackCache();
            if (c instanceof SafeNamedCache)
                {
                remoteCache = (RemoteNamedCache) ((SafeNamedCache) c).getNamedCache();
                }
            else
                {
                remoteCache = (RemoteNamedCache) c;
                }
            }
        else
            {
            throw new UnsupportedOperationException("ignored");
            }

        NamedCacheFactory factory  = (NamedCacheFactory) remoteCache.getChannel().getMessageFactory();
        int               nVersion = factory.getVersion();

        factory.setVersion(10);
        try
            {
            cache.isReady(); // should throw
            }
        finally
            {
            factory.setVersion(nVersion);
            }
        }

    // ----- helper methods -------------------------------------------------


    /**
     * Start the storage member.
     */
    @SuppressWarnings("resource")
    protected void startStorage(boolean fGraceful)
        {
        Properties props = new Properties();

        props.setProperty("test.server.distributed.localstorage", "true");
        props.setProperty("test.extend.enabled", "false");

        startCacheServer(STORAGE_NAME, PROJECT_NAME, SERVER_CONFIG_XML, props, fGraceful);
        }

    /**
     * Start the proxy member.
     */
    @SuppressWarnings("resource")
    protected void startProxy(boolean fGraceful)
        {
        Properties props = new Properties();

        props.setProperty("test.server.distributed.localstorage", "false");
        props.setProperty("test.extend.enabled", "true");

        startCacheServerWithProxy(PROXY_NAME,
                PROJECT_NAME, "build.xml", SERVER_CONFIG_XML, props, fGraceful);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name of the storage node.
     */
    protected static final String STORAGE_NAME = "cache-coh27697-tests-storage";

    /**
     * The name of the storage node.
     */
    protected static final String PROXY_NAME = "cache-coh27697-tests-proxy";

    /**
     * The project name.
     */
    protected static final String PROJECT_NAME = "extend";

    /**
     * The cache server configuration file.
     */
    protected static final String SERVER_CONFIG_XML = "server-cache-config.xml";

    /**
     * The client configuration file.
     */
    protected static final String CLIENT_CONFIG_XML = "client-cache-config.xml";

    /**
     * Test parameters to validate both direct and near cache calls.
     *
     * @return  the test parameters
     */
    @Parameterized.Parameters
    public static Collection<Object[]> data()
        {
        return Arrays.asList(new Object[][]
            {
                {
                "dist-extend-direct"
                },
                {
                "dist-extend-near-all"
                }
            });
        }
    }
