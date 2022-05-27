/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package logging;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.filter.EqualsFilter;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import data.Person;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isIn;


/**
 * Functional test of the Unavailable Partition Events logging functionality.
 *
 * @author mg  2021.04.20
 */
public class UnavailableTimeTests
        extends AbstractFunctionalTest
{
    // ----- test lifecycle -------------------------------------------------

    @BeforeClass
    public static void _startup()
        {
        System.setProperty("test.log", "jdk");
        System.setProperty("test.log.level", "8");
        System.setProperty("test.log.name", "Unavailable");

        System.setProperty("coherence.override", "logging-coherence-override.xml");
        System.setProperty("coherence.cacheconfig", "logging-cache-config.xml");

        Logger rootLog = Logger.getLogger("");
        rootLog.setLevel( Level.ALL );

        Logger logger = m_logger = Logger.getLogger("Unavailable");
        logger.addHandler(m_logHandler = new LogHandler());
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Tests for logging of partition events
     */
    @Test
    public void testUnavailableTimeLogging()
        {
        LogHandler handler = m_logHandler;
        try
            {
            CacheFactory.shutdown();

            handler.m_enabled = true;

            System.setProperty("coherence.distributed.localstorage", "true");
            System.setProperty("coherence.distributed.partitioncount", "100");
            System.setProperty("coherence.distributed.partition.events", "log");

            AbstractFunctionalTest._startup();

            NamedCache<Integer, Person> cache = CacheFactory.getCache("dist-unavail-time");
            int nPartitions = ((PartitionedService) cache.getCacheService()).getPartitionCount();

            Eventually.assertDeferred(() -> handler.collectContaining("ASSIGN"), hasSize(1));

            // start second member to generate partition events
            Properties props = new Properties();

            // first clear global props
            System.clearProperty("test.log");
            System.clearProperty("test.log.level");
            System.clearProperty("test.log.name");

            props.put("coherence.log", "stdout");
            props.put("coherence.log.level", "8");
            props.put("coherence.distributed.partition.events", "log");

            String sServerName = "storage1";
            CoherenceClusterMember clusterMember1 = startCacheServer(sServerName, "UnavailableTimeLogging", "", props);
            waitForServer(clusterMember1);
            waitForBalanced(cache.getCacheService());

            Eventually.assertDeferred(() -> handler.collectContaining("PRIMARY_TRANSFER_OUT"), hasSize(nPartitions / 2));
            Eventually.assertDeferred(() -> handler.collectContaining("BACKUP_TRANSFER_OUT"), hasSize(nPartitions / 2));

            // generate index build messages
            for (int i = 0; i < 1000; i++)
                {
                cache.put(i, new Person("1234" + i, "John", "Doe", 1919, "5678", new String[]{"11", "22"}));
                }

            cache.addIndex(Person::getBirthYear);

            // ensure index is built
            Filter filter = new EqualsFilter<>(Person::getBirthYear, 1919);
            cache.entrySet(filter);

            Eventually.assertDeferred(() -> handler.collectContaining("INDEX_BUILD"), hasSize(nPartitions / 2));

            handler.flush();

            // check logs on other side, make sure primary_transfer_in kicked in
            List<String> lLogLines = null;
            try
                {
                lLogLines = Files.readAllLines(
                    Paths.get(System.getProperty("test.project.dir") +
                            File.separatorChar +
                            "target/test-output/functional" +
                            File.separatorChar +
                            sServerName +
                            ".out"));
                }
            catch (IOException ioe)
                {
                Assert.fail("Log file not found " + ioe);
                }
            assertThat(lLogLines.stream().filter(s -> s.contains("PRIMARY_TRANSFER_IN")).collect(Collectors.toList()), hasSize(nPartitions / 2));

            // stop second member and check that restore/index build gets triggered for partitions coming back
            stopCacheServer("storage1");
            // wait for server to stop
            Eventually.assertDeferred(() -> cache.getCacheService().getCluster().getMemberSet().size(),
                                      Matchers.is(1), within(5, TimeUnit.MINUTES));

            PartitionedService service = (PartitionedService) cache.getCacheService();
            Member             member  = service.getCluster().getLocalMember();
            // wait for re-distribution
            Eventually.assertDeferred(() -> service.getOwnedPartitions(member).cardinality(), is(nPartitions));

            Eventually.assertDeferred(() -> handler.collectContaining("RESTORE"), hasSize(nPartitions / 2));
            // sleep to ensure index build gets performed
            Base.sleep(3000L);
            Eventually.assertDeferred(() -> handler.collectContaining("INDEX_BUILD"), hasSize(nPartitions / 2));
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            handler.m_enabled = false;
            }
    }

    /**
     * Wait for an extended period compared with {@link AbstractFunctionalTest#waitForBalanced(CacheService)}
     * for the specified (partitioned) cache service to become "balanced".
     *
     * @param service   the partitioned cache to wait for balancing
     */
    public static void waitForBalanced(CacheService service)
        {
        SafeService      serviceSafe = (SafeService) service;
        PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();

        Eventually.assertDeferred(() -> serviceReal.calculateUnbalanced(), is(0), within(300, TimeUnit.SECONDS));
        }

    // ----- inner class: LogHandler ----------------------------------------

    /**
     * A jdk logging handler to capture log messages when enabled.
     */
    public static class LogHandler
            extends Handler
        {

        // ----- Handler methods --------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public void publish(LogRecord lr)
            {
            if (m_enabled)
                {
                m_listMessages.add(lr.getMessage());
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void flush()
            {
            m_listMessages.clear();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws SecurityException
            {
            m_listMessages.clear();
            }

        /**
         * Returns a list of log messages collected.
         *
         * @return a list of log messages collected
         */
        public List<String> collect()
            {
            return Collections.unmodifiableList(m_listMessages);
            }

        /**
         * Returns a list of log messages collected containing string.
         *
         * @return a list of log messages collected
         */
        public List<String> collectContaining(String sFilter)
            {
            synchronized (m_listMessages)
                {
                return Collections.unmodifiableList(m_listMessages.stream().filter(s -> s.contains(sFilter)).collect(Collectors.toList()));
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * Whether to collect log messages.
         */
        protected volatile boolean m_enabled = false;

        /**
         * The log messages collected.
         */
        protected List<String> m_listMessages = Collections.synchronizedList(new LinkedList<>());
        }

    // ----- data members ---------------------------------------------------

    /**
     * The sniffing log handler that can be enabled / disabled.
     */
    private static LogHandler       m_logHandler;

    /**
     * A reference to logger to ensure it is not gc'd as jdk only holds a
     * weak reference to the logger.
     */
    private static Logger           m_logger;
}