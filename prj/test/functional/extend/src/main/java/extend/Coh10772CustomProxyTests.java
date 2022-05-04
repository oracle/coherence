/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.net.NamedCache;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertTrue;

/**
 * A collection of functional tests for Coherence*Extend clients that
 * use a custom proxy.
 *
 * @author prollman  2013.11.27
 *
 * @since @BUILDVERSION@
 */
public class Coh10772CustomProxyTests
        extends AbstractFunctionalTest
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public Coh10772CustomProxyTests()
        {
        super("client-cache-config-custom-proxy-coh10772.xml");
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember memberProxy = startCacheServer("Coh10772CustomProxyTests", "extend",
                                                "server-cache-config-custom-proxy-coh10772.xml");
        Eventually.assertThat(invoking(memberProxy).isServiceRunning("ExtendTcpCustomProxyServiceCoh10772"), is(true));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("Coh10772CustomProxyTests");
        }

    // ----- Extend client tests ---------------------------------------------


    /**
     * Test extend client receiving events when the proxy holds a handle
     * to the cache.
     */
    @Test
    public void testCacheEvents()
        {
        Listener   listener = new Listener();
        NamedCache cache    = getNamedCache("coh10772");

        cache.addMapListener(listener);

        cache.put(1, 1);
        assertTrue(1 == cache.size());

        Eventually.assertThat(invoking(listener).isInsertEventReceived(), is(true));

        cache.put(1, 2);
        assertTrue(1 == cache.size());

        Eventually.assertThat(invoking(listener).isUpdateEventReceived(), is(true));

        cache.remove(1);
        assertTrue(0 == cache.size());

        Eventually.assertThat(invoking(listener).isDeleteEventReceived(), is(true));
        }

    public static class Listener implements MapListener
        {
        final AtomicBoolean updateEventReceived = new AtomicBoolean();
        final AtomicBoolean insertEventReceived = new AtomicBoolean();
        final AtomicBoolean deleteEventReceived = new AtomicBoolean();

        @Override
        public void entryUpdated(MapEvent event)
            {
            updateEventReceived.set(true);
            }

        @Override
        public void entryInserted(MapEvent event)
            {
            insertEventReceived.set(true);
            }

        @Override
        public void entryDeleted(MapEvent event)
            {
            deleteEventReceived.set(true);
            }

        public boolean isUpdateEventReceived()
            {
            return updateEventReceived.get();
            }

        public boolean isInsertEventReceived()
            {
            return insertEventReceived.get();
            }

        public boolean isDeleteEventReceived()
            {
            return deleteEventReceived.get();
            }
        }
    }
