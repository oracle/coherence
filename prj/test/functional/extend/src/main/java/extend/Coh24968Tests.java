/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.internal.net.NamedCacheDeactivationListener;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestPolicyException;

import com.tangosol.util.MapEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Test for {@code COH-24968 - RFA: Exception occurred while encoding a
 * AggregateAllRequest results in total disconnect for client}.
 *
 * @since 14.1.1.0.9
 * @author rl 3.15.2022
 */
@RunWith(Parameterized.class)
public class Coh24968Tests
        extends AbstractExtendTest
    {
    // ----- constructors ---------------------------------------------------

    public Coh24968Tests(String sCacheName)
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
        startStorage();
        startProxy();
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
    public void testCoh24968()
        {
        NamedCache<String, String> cache = getNamedCache();

        // run a PUT and a series of GETs to ensure a sane environment.
        runCleanOps(cache);

        // register a deactivation listener that will update the returned
        // AtomicBoolean once it has been called.
        AtomicBoolean deactivated  = registerDeactivationListener(cache);

        // kill the storage node and assert the named cache was not
        // explicitly destroyed by closing the channel
        stopCacheServer(STORAGE_NAME, false);

        // assert deactivation listener was invoked.
        Eventually.assertDeferred(deactivated::get, is(true));

        // Run a similar series of operations and ensure the expected
        // exception is raised
        runNoStorageNodeGets(cache);

        // re-start the storage member and ensure exceptions are no longer
        // thrown
        startStorage();

        // verify cache operations eventually return to running status
        runOpsUntilClean(cache);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Start the storage member.
     */
    protected void startStorage()
        {
        Properties props = new Properties();

        props.setProperty("test.server.distributed.localstorage", "true");
        props.setProperty("test.extend.enabled", "false");

        startCacheServer(STORAGE_NAME, PROJECT_NAME, SERVER_CONFIG_XML, props);
        }

    /**
     * Start the proxy member.
     */
    protected void startProxy()
        {
        Properties props = new Properties();

        props.setProperty("test.server.distributed.localstorage", "false");
        props.setProperty("test.extend.enabled", "true");

        startCacheServerWithProxy(PROXY_NAME, PROJECT_NAME, SERVER_CONFIG_XML, props);
        }

    /**
     * Register a {@link NamedCacheDeactivationListener} with the provided
     * cache returning an {@link AtomicBoolean} that will return {@code true}
     * once the listener has been invoked.
     *
     * @param cache  the {@link NamedCache} to register the listener against
     *
     * @return an {@link AtomicBoolean} that will return {@code true}
     *         nce the listener has been invoked
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected AtomicBoolean registerDeactivationListener(NamedCache<?, ?> cache)
        {
        AtomicBoolean deactivated = new AtomicBoolean();

        cache.addMapListener(new NamedCacheDeactivationListener()
            {
            @Override
            public void entryInserted(MapEvent evt)
                {
                }

            @Override
            public void entryUpdated(MapEvent evt)
                {
                }

            @Override
            public void entryDeleted(MapEvent evt)
                {
                deactivated.set(true);
                }
            });

        return deactivated;
        }

    /**
     * Perform a series of cache operations to ensure a sane test environment.
     *
     * @param cache  the {@link NamedCache} to perform operations against
     */
    protected void runOpsUntilClean(NamedCache<String, String> cache)
        {
        assertThat(cache.isEmpty(), is(true));

        cache.put("a", "a");

        for (int i = 0; i < 5000; i++)
            {
            try
                {
                cache.get("a");

                CacheFactory.log("Cluster servicing requests after " + i + " iterations.");

                return;
                }
            catch (Exception e)
                {
                assertThat(e, is(instanceOf(RequestPolicyException.class)));
                }

            sleep(100);
            }

        fail("Environment didn't return to a stable state; requests are still failing.");
        }

    /**
     * Perform a series of cache operations to we eventually return to a sane
     * configuration and operations don't fail.
     *
     * @param cache  the {@link NamedCache} to perform operations against
     */
    protected void runCleanOps(NamedCache<String, String> cache)
        {
        assertThat(cache.isEmpty(), is(true));

        cache.put("a", "a");

        for (int i = 0; i < 10; i++)
            {
            try
                {
                cache.get("a");
                }
            catch (Exception e)
                {
                fail(e.toString());
                }

            sleep(100);
            }
        }

    /**
     * Perform a series of cache operations once all storage nodes have
     * been removed from the cluster.  This ensures a {@link RequestPolicyException}
     * is thrown indicating no storage nodes exist.
     *
     * @param cache  the {@link NamedCache} to perform operations against
     */
    protected void runNoStorageNodeGets(NamedCache<?, ?> cache)
        {
        for (int i = 0; i < 10; i++)
            {
            try
                {
                cache.get("a");
                fail("Expected RequestPolicyException to be thrown");
                }
            catch (Exception e)
                {
                assertThat(e, is(instanceOf(RequestPolicyException.class)));
                }

            sleep(100);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name of the storage node.
     */
    protected static final String STORAGE_NAME = "cache-coh24968-tests-storage";

    /**
     * The name of the storage node.
     */
    protected static final String PROXY_NAME = "cache-coh24968-tests-proxy";

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
