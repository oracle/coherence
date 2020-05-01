package guardian;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import common.AbstractFunctionalTest;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

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
import java.io.IOException;

import java.util.Properties;

import static org.hamcrest.core.Is.is;

public class COH20930Tests
        extends AbstractFunctionalTest
    {
    public COH20930Tests()

        {
        super(FILE_CFG_CACHE);
        }

    @After
    public void cleanup()
        {
        stopAllApplications();
        CacheFactory.shutdown();
        }

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

    public static class TestProcessor extends AbstractProcessor
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

    public static String FILE_CFG_CACHE = "coherence-cache-config.xml";
    }