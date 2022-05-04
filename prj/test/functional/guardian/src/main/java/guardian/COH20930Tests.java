/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package guardian;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.io.FileHelper;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Converter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.File;

import java.util.Properties;

import org.junit.After;
import org.junit.Test;

import static org.hamcrest.core.Is.is;

/**
 * A functional test to validate that the interrupted flag from guardian timeout
 * is cleared so it does not affect subsequent operations.
 *
 * Test setup is as follows:
 * Two distributed services,  NumberDistributed and ItemDistributed,  both has 30s
 * task timeout and 60s request timeout. EP from NumberService make calls to ItemService
 * which should be hang as ItemService is suspended.  As the result, the invoke thread
 * should be interrupted by guardian in ~30s. Since there are changes made in EP on
 * other entries, persistChanges should be called following guardian timeout. BDB should
 * not see interrupted flag on the thread and should not throw.
 *
 * @author bbc  2021.11.10
 */
public class COH20930Tests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    public COH20930Tests()
        {
        super(FILE_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    @After
    public void cleanup()
        {
        stopAllApplications();
        CacheFactory.shutdown();
        }

    // ----- tests ----------------------------------------------------------

    @Test
    public void testCOH20930() throws Exception
        {
        File fileBaseDir = FileHelper.createTempDir();

        System.setProperty("coherence.distributed.persistence.mode", "active");
        System.setProperty("coherence.distributed.persistence.base.dir", fileBaseDir.getAbsolutePath());

        AbstractFunctionalTest._startup();

        Properties props = new Properties();
        props.putAll(System.getProperties());

        CoherenceClusterMember clusterMember = startCacheServer("testCOH20930-1", "guardian", FILE_CFG_CACHE);

        Eventually.assertThat(clusterMember.getClusterSize(), is(2));

        NamedCache cacheIterm  = getNamedCache("item");
        NamedCache cacheNumber = getNamedCache("number");

        for (int i=0; i<5; i++)
            {
            cacheIterm.put(i,  i);
            cacheNumber.put(i, i);
            }

        DistributedCacheService serviceIterm   = (DistributedCacheService) cacheIterm.getCacheService();
        DistributedCacheService serviceNumber  = (DistributedCacheService) cacheNumber.getCacheService();
        String                  sServiceNumber = serviceNumber.getInfo().getServiceName();
        Cluster                 cluster        = serviceIterm.getCluster();

        cluster.suspendService(sServiceNumber);

        try
            {
            TestProcessor tp = new TestProcessor();
            cacheIterm.invoke(1, tp);
            }
        catch (Exception e)
            {
            Base.log("Invoke failed with " + e.getMessage());
            }
        finally
            {
            Eventually.assertThat(serviceIterm.getInfo().getServiceMembers().size(), is(2));
            cleanup();
            }
        }

    // ----- inner-class: TestProcessor -------------------------------------

    public class TestProcessor extends AbstractProcessor
        {
        @Override
        public Object process(InvocableMap.Entry entry)
            {
            BinaryEntry       binEntry = (BinaryEntry) entry;
            BackingMapContext ctx      =  binEntry.getBackingMapContext();
            Converter         covDown  = binEntry.getContext().getKeyToInternalConverter();

            ctx.getBackingMap().remove(covDown.convert(Integer.valueOf(2)));
            ctx.getBackingMap().remove(covDown.convert(Integer.valueOf(3)));

            NamedCache cache = CacheFactory.getCache("number");
            cache.get(1);
            return null;
            }
        }

    // ----- constants ------------------------------------------------------

    public static String FILE_CFG_CACHE = "coherence-cache-config.xml";
    }