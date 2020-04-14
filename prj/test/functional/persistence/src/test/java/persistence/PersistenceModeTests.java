/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.TypeAssertion;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;

import com.tangosol.util.Base;

import common.AbstractFunctionalTest;

import org.hamcrest.core.Is;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import java.util.Properties;
import java.util.Set;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static persistence.AbstractConfigurationPersistenceTests.getProjectName;

/**
 * Functional persistence configuration tests on persistence mode property using BerkeleyDB.
 *
 * @author lh  2016.11.30
 */
public class PersistenceModeTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public PersistenceModeTests()
        {
        super("persistence-bdb-cache-config.xml");
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        try
            {
            File fileActive   = FileHelper.createTempDir();
            File fileSnapshot = FileHelper.createTempDir();
            File fileTrash    = FileHelper.createTempDir();
            Properties props  = System.getProperties();

            props.setProperty("coherence.distributed.localstorage", "true");
            props.setProperty("test.partition-count", "11");
            props.setProperty("coherence.distributed.persistence.active.dir", fileActive.getAbsolutePath());
            props.setProperty("coherence.distributed.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
            props.setProperty("coherence.distributed.persistence.trash.dir", fileTrash.getAbsolutePath());
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Initialize each test
     * <p>
     * This method starts the Coherence cluster, if it isn't already running.
     */
    @Before
    public void _initTest()
        {
        setupProps();

        startCluster();
        }

    /**
     * Clean up after each test.
     * <p/>
     * This method stops the Coherence cluster.
     */
    @After
    public void _cleanTest()
        {
        stopAllApplications();
        CacheFactory.shutdown();

        // don't use the out() method, as it will restart the cluster
        System.out.println(createMessageHeader() + " <<<<<<< Stopped cluster");
        }

    // ----- tests ----------------------------------------------------------

    /**
     * Test default persistence mode property.
     */
    @Test
    public void testDefaultModeProperty()
        {
        testPersistenceModeProperty("testDefaultModeProperty", null, null);
        }

    /**
     * Test persistence mode property for "coherence.distributed.persistence-mode".
     */
    @Test
    public void testModePropertyWithHyphen()
        {
        testPersistenceModeProperty("testModePropertyWithHyphen", "coherence.distributed.persistence-mode", "active");
        }

    /**
     * Test persistence mode property for "coherence.distributed.persistence.mode".
     */
    @Test
    public void testModePropertyWithDot()
        {
        testPersistenceModeProperty("testModePropertyWithDot", "coherence.distributed.persistence.mode", "active");
        }

    private void testPersistenceModeProperty(String sServer, String sProperty, String sMode)
        {
        Properties props = new Properties();
        props.setProperty("test.server.distributed.localstorage", String.valueOf(false));
        props.setProperty("coherence.management", "all");
        props.setProperty("com.sun.management.jmxremote", "true");

        NamedCache<Integer, String> cache = null;
        try
            {
            if (sProperty == null || sMode == null)
                {
                System.clearProperty("coherence.distributed.persistence.mode");
                System.clearProperty("coherence.distributed.persistence-mode");
                sMode = "on-demand";
                }
            else
                {
                System.setProperty(sProperty, sMode);
                }
            AbstractFunctionalTest.setupProps();

            startCacheServer(sServer, getProjectName(), getCacheConfigPath(), props, false);

            AbstractFunctionalTest.startCluster();

            waitForBalanced((CacheService) getFactory().ensureService("DistributedCachePersistenceDefault"));

            cache = getFactory().ensureTypedCache("default-persistent", null, TypeAssertion.withRawTypes());

            CacheService service = cache.getCacheService();
            Member       member  = null;
            if (service instanceof DistributedCacheService)
                {
                Set<Member> setMembers = ((DistributedCacheService) service).getOwnershipEnabledMembers();

                for (Member item : setMembers)
                    {
                    member = item;
                    }
                }

            Registry registry = CacheFactory.getCluster().getManagement();
            MBeanServerProxy mbsProxy = registry.getMBeanServerProxy();

            String sServiceName = cache.getCacheService().getInfo().getServiceName();
            String sMBean       = registry.ensureGlobalName(
                    Registry.SERVICE_TYPE + ",name=" + sServiceName, member);

            Eventually.assertThat(invoking(mbsProxy).isMBeanRegistered(sMBean), Is.is(true));
            Assert.assertEquals(mbsProxy.getAttribute(sMBean, "PersistenceMode"), sMode);
            }
        finally
            {
            cache.destroy();
            stopCacheServer(sServer);
            }
        }
    }
