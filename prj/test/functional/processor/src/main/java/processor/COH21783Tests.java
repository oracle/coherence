/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package processor;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.MapTriggerListener;

import com.oracle.coherence.testing.AbstractRollingRestartTest;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Test;

import com.tangosol.net.internal.RemoveEntryProcessor;

import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.FilterTrigger;
import com.tangosol.util.filter.LessFilter;

import java.util.HashMap;
import java.util.Map;

public class COH21783Tests extends AbstractRollingRestartTest
    {
    /**
     * Default constructor.
     */
    public COH21783Tests()
        {
        super(s_sCacheConfig);
        }

    // ----- lifecycle -----------------------------------------------------

    @After
    public void cleanup()
        {
        stopAllApplications();
        CacheFactory.shutdown();
        }

    // ----- accessors -----------------------------------------------------

    @Override
    public String getCacheConfigPath()
        {
        return s_sCacheConfig;
        }

    @Override
    public String getBuildPath()
        {
        return s_sBuild;
        }

    @Override
    public String getProjectName()
        {
        return s_sProject;
        }

    // ----- test method -------------------------------------------------

    @Test
    public void testRemove() throws Exception
        {
        final NamedCache cache    = getNamedCache("dist-std-test");
        final int        cKeys    = 20;
        final int        cServers = 2;
        final Map        map      = new HashMap();

        for (int i = 0; i < cKeys; i++)
            {
            map.put(i, i);
            }

        MemberHandler memberHandler = new MemberHandler(
                CacheFactory.ensureCluster(), getServerPrefix() + "-",
                /*fExternalKill*/true, /*fGraceful*/false);

        final boolean[] afExiting = new boolean[1];

        Thread thdTest = null;
        try
            {
            // setup, start the initial cache servers
            for (int i = 0; i < cServers; i++)
                {
                memberHandler.addServer();
                }

            MapTriggerListener listener = new MapTriggerListener(m_triggerRemove);
            cache.addMapListener(listener);

            final DistributedCacheService service = (DistributedCacheService) cache.getCacheService();
            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(cServers));

            // start the client test
            // thread
            final Object[] aoException = new Object[1];

            thdTest = new Thread(() ->
            {
            InvocableMap.EntryProcessor processor = new RemoveEntryProcessor(true);
            try
                {
                while (!afExiting[0])
                    {
                    for (int i = 0; i < cKeys; i++)
                        {
                        cache.putAll(map);
                        if (i < 10)
                            {
                            cache.invoke(i, processor);
                            }

                        assertFalse("Entry should have been removed!", cache.containsKey(i));
                        }

                    Base.sleep(10);
                    }
                }
            catch (Exception e)
                {
                aoException[0] = e;
                throw Base.ensureRuntimeException(e);
                }
            });
            thdTest.start();

            // perform the rolling restart
            doRollingRestart(memberHandler, 25,
                    new Runnable()
                        {
                        public void run()
                            {
                            Base.sleep(5000);
                            waitForNodeSafe(service);
                            Base.sleep(5000);
                            }
                        });

            afExiting[0] = true;
            thdTest.join();

            assertNull(aoException[0]);
            }
        finally
            {
            afExiting[0] = true;
            if (thdTest != null)
                {
                try
                    {
                    thdTest.join();
                    }
                catch (InterruptedException ignored) {}
                }

            memberHandler.dispose();
            Cluster cluster = CacheFactory.getCluster();
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
            }
        }

    public final static String s_sCacheConfig = "coherence-cache-config.xml";

    /**
     * The project name.
     */
    public final static String s_sProject     = "processor";

    /**
     * The path to the Ant build script.
     */
    public final static String s_sBuild       = "build.xml";

    /**
     * Return the prefix to use for the server names.
     *
     * @return the server name prefix
     */
    public String getServerPrefix()
        {
        return "COH21783";
        }

    protected MapTrigger m_triggerRemove =
            new FilterTrigger(new LessFilter(IdentityExtractor.INSTANCE, Integer.valueOf(10)), FilterTrigger.ACTION_REMOVE);
    }
