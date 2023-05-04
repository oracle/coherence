/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package partition;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.component.util.safeService.safeCacheService.SafeDistributedCacheService;

import com.tangosol.io.DecoratedBinaryDeltaCompressor;
import com.tangosol.io.DecorationOnlyDeltaCompressor;
import com.tangosol.io.DeltaCompressor;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.management.MBeanHelper;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.hamcrest.Matcher;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;


/**
 * @author coh 2011.01.28
 * @since Coherence 3.7
 */
public class ConfigurationTests
        extends AbstractFunctionalTest
    {
    /**
     * Default constructor.
     */
    public ConfigurationTests()
        {
        super();
        }

    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.management", "all");
        AbstractFunctionalTest._startup();
        }

    /**
     * Test a default compressor.
     */
    @Test
    public void testCompressorDefault()
        {
        PartitionedCache cache      = getPartitionedCache(CacheFactory.getCache("compressor-default"));
        DeltaCompressor  compressor = cache.getBackupDeltaCompressor();

        assertEquals(DecorationOnlyDeltaCompressor.class, compressor.getClass());
        }

    /**
     * Test a standard compressor (non-POF).
     */
    @Test
    public void testCompressorStandard()
        {
        PartitionedCache cache = getPartitionedCache(CacheFactory.getCache("compressor-standard"));

        DeltaCompressor  compressor = cache.getBackupDeltaCompressor();

        assertEquals(DecoratedBinaryDeltaCompressor.class, compressor.getClass());
        assertTrue(compressor.toString().contains("com.tangosol.io.BinaryDeltaCompressor"));
        }

    /**
     * Test a standard compressor (POF).
     */
    @Test
    public void testCompressorStandardPof()
        {
        PartitionedCache cache = getPartitionedCache(CacheFactory.getCache("compressor-standard-pof"));
        DeltaCompressor  compressor = cache.getBackupDeltaCompressor();

        assertEquals(DecoratedBinaryDeltaCompressor.class, compressor.getClass());

        // COH-5528 workaround
        // assertTrue(compressor.toString().indexOf("com.tangosol.io.pof.PofDeltaCompressor") != -1);
        }

    /**
     * Test a custom compressor.
     */
    @Test
    public void testCompressorCustom()
        {
        PartitionedCache cache = getPartitionedCache(CacheFactory.getCache("compressor-custom"));

        assertEquals(MyCompressor.class, cache.getBackupDeltaCompressor().getClass());
        }

    /**
     * Ensure a misconfigured partition count is overridden by the senior member.
     */
    @Test
    public void testMisconfiguredPartitionCount()
        {
        try
            {
            startCacheServer("testMisconfiguredPartitionCount-1", "partition");

            NamedCache              cache         = getNamedCache("coh7495-partTest");
            DistributedCacheService cacheSvc      = (DistributedCacheService) cache.getCacheService();
            Member                  memberPrimary = findCacheServer("testMisconfiguredPartitionCount-1");
            ensureMember(cacheSvc, memberPrimary);

            System.setProperty("test.partition.count", "2011");
            startCacheServer("testMisconfiguredPartitionCount-2", "partition");
            Member memberSecondary = findCacheServer("testMisconfiguredPartitionCount-2");
            ensureMember(cacheSvc, memberSecondary);

            Eventually.assertThat(
                    invoking(findApplication("testMisconfiguredPartitionCount-1"))
                    .isServiceRunning("DistributedCache"), is(true));

            Eventually.assertThat(
                    invoking(findApplication("testMisconfiguredPartitionCount-2"))
                    .isServiceRunning("DistributedCache"), is(true));

            Eventually.assertThat(invoking(this).getPartitionCount(cacheSvc, memberSecondary), greaterThan(0));
            Eventually.assertThat(invoking(this).getPartitionCount(cacheSvc, memberSecondary), is(getPartitionCount(cacheSvc, memberPrimary)));
            }
        finally
            {
            System.clearProperty("test.partition.count");
            stopCacheServer("testMisconfiguredPartitionCount-1");
            stopCacheServer("testMisconfiguredPartitionCount-2");
            }
        }

    /**
     * Test two server distribution strategy for SE One with backup.
     * <p/>
     * For two servers the distribution strategy does not split the partitions;
     * instead the first server owns all the primary and the second - all the
     * backups. As soon as the third server starts, we go back to "normal"
     * distribution.
     */
    @Test
    public void testTwoServerSEOneBC1()
        {
        try
            {
            System.setProperty("coherence.distribution.2server", "true");

            startCacheServer("testTwoServerSEOneBC1-1", "partition");

            NamedCache              cache      = getNamedCache("dist-test");
            DistributedCacheService service    = (DistributedCacheService) cache.getCacheService();
            String                  sService   = service.getInfo().getServiceName();
            int                     cPartition = service.getPartitionCount();
            Member                  member1    = findCacheServer("testTwoServerSEOneBC1-1");
            String                  sPrimary   = "OwnedPartitionsPrimary";
            String                  sBackup    = "OwnedPartitionsBackup";

            Eventually.assertThat(
                    invoking(findApplication("testTwoServerSEOneBC1-1")).isServiceRunning(sService), is(true));

            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, member1, sPrimary), is(cPartition));

            startCacheServer("testTwoServerSEOneBC1-2", "partition");
            Member member2 = findCacheServer("testTwoServerSEOneBC1-2");

            Eventually.assertThat(invoking(findApplication("testTwoServerSEOneBC1-2")).isServiceRunning(sService), is(true));

            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, member1, sPrimary), is(cPartition));
            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, member1, sBackup), is(0));

            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, member2, sPrimary), is(0));
            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, member2, sBackup), is(cPartition));

            startCacheServer("testTwoServerSEOneBC1-3", "partition");
            Member member3 = findCacheServer("testTwoServerSEOneBC1-3");

            Eventually.assertThat(invoking(findApplication("testTwoServerSEOneBC1-3")).isServiceRunning(sService), is(true));

            int cFairShare = cPartition/3;

            Matcher matcher = isIn(Arrays.asList(cFairShare, cFairShare + 1));
            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, member1, sPrimary), matcher);
            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, member1, sBackup), matcher);

            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, member2, sPrimary), matcher);
            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, member2, sBackup), matcher);

            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, member3, sPrimary), matcher);
            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, member3, sBackup), matcher);

            stopCacheServer("testTwoServerSEOneBC1-3");

            cFairShare = cPartition/2;
            matcher = isIn(Arrays.asList(cFairShare, cFairShare + 1));
            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, member1, sPrimary), matcher);
            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, member1, sBackup), matcher);

            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, member2, sPrimary), matcher);
            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, member2, sBackup), matcher);
            }
        finally
            {
            stopCacheServer("testTwoServerSEOneBC1-1");
            stopCacheServer("testTwoServerSEOneBC1-2");
            stopCacheServer("testTwoServerSEOneBC1-3");

            System.clearProperty("coherence.distribution.2server");
            }
        }

    /**
     * Test two server distribution strategy for SE One without backup.
     * <p/>
     * If there is no backup, the new strategy behaves "normally" and
     * distributes the partitions equally across the members.
     */
    @Test
    public void testTwoServerSEOneBC0()
        {
        try
            {
            AbstractFunctionalTest._shutdown();

            Properties props = new Properties();
            props.put("test.distributed.backupcount", "0");
            props.put("coherence.distribution.2server", "true");

            System.getProperties().putAll(props);

            AbstractFunctionalTest._startup();

            startCacheServer("testTwoServerSEOneBC0-1", "partition", null, props);

            NamedCache              cache         = getNamedCache("dist-test");
            DistributedCacheService service       = (DistributedCacheService) cache.getCacheService();
            String                  sService      = service.getInfo().getServiceName();
            int                     cPartition    = service.getPartitionCount();
            Member                  memberPrimary = findCacheServer("testTwoServerSEOneBC0-1");

            String                  sPrimary      = "OwnedPartitionsPrimary";
            String                  sBackup       = "OwnedPartitionsBackup";

            Eventually.assertThat(invoking(findApplication("testTwoServerSEOneBC0-1")).isServiceRunning(sService), is(true));

            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, memberPrimary, sPrimary), is(cPartition));

            startCacheServer("testTwoServerSEOneBC0-2", "partition", null, props);
            Member memberSecondary = findCacheServer("testTwoServerSEOneBC0-2");

            Eventually.assertThat(invoking(findApplication("testTwoServerSEOneBC0-2")).isServiceRunning(sService), is(true));

            int     cFair   = cPartition / 2;
            Matcher matcher = isIn(Arrays.asList(cFair, cFair + 1));

            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, memberPrimary, sPrimary), matcher);
            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, memberPrimary, sBackup), is(0));

            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, memberSecondary, sPrimary), matcher);
            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, memberSecondary, sBackup), is(0));

            startCacheServer("testTwoServerSEOneBC0-3", "partition", null, props);
            Member memberThird = findCacheServer("testTwoServerSEOneBC0-3");

            Eventually.assertThat(invoking(findApplication("testTwoServerSEOneBC0-3")).isServiceRunning(sService), is(true));

            cFair   = cPartition / 3;
            matcher = isIn(Arrays.asList(cFair, cFair + 1));

            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, memberPrimary, sPrimary), matcher);
            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, memberPrimary, sBackup), is(0));

            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, memberSecondary, sPrimary), matcher);
            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, memberSecondary, sBackup), is(0));

            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, memberThird, sPrimary), matcher);
            Eventually.assertThat(invoking(this).getOwnedPartitionCount(service, memberThird, sBackup), is(0));
            }
        finally
            {
            stopCacheServer("testTwoServerSEOneBC0-1");
            stopCacheServer("testTwoServerSEOneBC0-2");
            stopCacheServer("testTwoServerSEOneBC0-3");

            AbstractFunctionalTest._shutdown();
            System.setProperty("test.distributed.backupcount", "1");
            System.clearProperty("coherence.distribution.2server");
            }
        }

    /**
     * Obtain the number of primary partitions for the given service
     * owned by the given member.
     *
     * @param service  the service for which to check partition ownership
     * @param member   the member to check partition ownership for
     *
     * @return the number of primary partitions owned by the given member for
     *         the given service
     */
    public int getPartitionCount(PartitionedService service, Member member)
        {
        return getOwnedPartitionCount(service, member, "PartitionsAll");
        }

    /**
     * Obtain the number of primary or backup partitions for the given service
     * owned by the given member.
     *
     * @param service         the service for which to check partition ownership
     * @param member          the member to check partition ownership for
     * @param sAttributeName  true for primary, false for backup
     *
     * @return the number of primary or backup partitions owned by the given member for
     *         the given service
     */
    public int getOwnedPartitionCount(PartitionedService service, Member member, String sAttributeName)
        {
        try
            {
            String      sName  = service.getInfo().getServiceName();
            int         nId    = member.getId();
            MBeanServer server = MBeanHelper.findMBeanServer();

            return (Integer) server.getAttribute(
                     new ObjectName(String.format("Coherence:type=Service,name=%s,nodeId=%s", sName, nId)), sAttributeName);
            }
        catch (Exception e)
            {
            CacheFactory.log(e);
            return -1;
            }
        }

    /**
     * Ensure that the member has joined the given service.
     *
     * @param service  the service for which to check membership
     * @param member   the member to check service membership for
     *
     */
    protected void ensureMember(CacheService service, Member member)
        {
        assertNotNull(member);
        Eventually.assertThat(invoking(service).getInfo().getServiceMembers().contains(member), is(true));
        }

    /**
     * Return the partitioned cache service for the specified named cache.
     *
     * @param cache  the named cache
     *
     * @return the partitioned cache service
     */
    private PartitionedCache getPartitionedCache(NamedCache cache)
        {
        assertTrue(cache.getCacheService() instanceof SafeDistributedCacheService);

        SafeDistributedCacheService safeService = (SafeDistributedCacheService) cache.getCacheService();
        assertTrue(safeService.getService() instanceof DistributedCacheService);

        DistributedCacheService service = (DistributedCacheService) safeService.getService();
        assertTrue(service instanceof PartitionedCache);

        return (PartitionedCache) service;
        }
    }
