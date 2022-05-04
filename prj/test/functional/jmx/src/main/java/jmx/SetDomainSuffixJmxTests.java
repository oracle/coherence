/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package jmx;


import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.Registry;
import com.oracle.coherence.testing.AbstractFunctionalTest;
import java.io.Serializable;
import java.util.Properties;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
* Validate management-config.domain-name-suffix and corresponding system property coherence.domain.name.suffix.
*
* @author jf 2022.02.09
*/
public class SetDomainSuffixJmxTests
        extends AbstractFunctionalTest implements Serializable
    {

    // ----- constructors ---------------------------------------------------

    public SetDomainSuffixJmxTests()
        {
        super(FILE_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.role", "main");
        System.setProperty("coherence.log.level", "3");

        // we will control the startup manually
        }

    @AfterClass
    public static void _shutdown()
        {
        // we will control the shutdown manually
        }

    @After
    public void cleanup()
        {
        AbstractFunctionalTest._shutdown();
        }

    // ----- test methods ---------------------------------------------------

    /**
    * Validate setting operational override management-config.domain-name-suffix.
    */
    @Test
    public void testSetDomainSuffixByOverrideFile()
            throws JMException, InterruptedException
        {
        Properties propsMain = new Properties();

        propsMain.put("coherence.management", "local-only");
        propsMain.put("coherence.management.remote", "false");
        propsMain.put("coherence.override", FILE_OPERATIONAL_OVERIDE);
        System.getProperties().putAll(propsMain);

        AbstractFunctionalTest._startup();
        try
            {
            Cluster cluster = CacheFactory.getCluster();

            assertTrue(cluster.isRunning());
            assertEquals("cluster already exists", 1, cluster.getMemberSet().size());

            Registry registry = cluster.getManagement();

            assertTrue("JMX is disabled", registry != null);

            MBeanServer serverJMX     = MBeanHelper.findMBeanServer();
            String      sDomain       = registry.getDomainName();
            String      sDomainSuffix = DOMAIN_NAME_SUFFIX.length() == 0
                                            ? ""
                                            : "@" +  DOMAIN_NAME_SUFFIX;

            assertEquals("Coherence" + sDomainSuffix, sDomain);

            String sCluster = sDomain + ":" + Registry.CLUSTER_TYPE;
            assertTrue("Should be registered: " + sCluster, serverJMX.isRegistered(new ObjectName(sCluster)));

            String sNode1 = sDomain + ":" + Registry.NODE_TYPE + ",nodeId=" + cluster.getLocalMember().getId();
            assertTrue("Should be registered: " + sNode1, serverJMX.isRegistered(new ObjectName(sNode1)));
            }
        finally
            {
            System.clearProperty("coherence.override");
            AbstractFunctionalTest._shutdown();
            }
        }

    /**
    * Validate setting system property coherence.domain.name.suffix only.
    */
    @Test
    public void testSetDomainNameSuffixBySystemProperty()
            throws JMException, InterruptedException
        {
        String sDomainNameSuffix = DOMAIN_NAME_SUFFIX + "SysProp";

        Properties propsMain = new Properties();
        propsMain.put("coherence.management", "local-only");
        propsMain.put("coherence.management.remote", "true");
        propsMain.put("coherence.domain.name.suffix", sDomainNameSuffix);
        System.getProperties().putAll(propsMain);

        AbstractFunctionalTest._startup();
        try
            {
            Cluster cluster = CacheFactory.getCluster();
            assertTrue(cluster.isRunning());
            assertEquals("cluster already exists", 1, cluster.getMemberSet().size());

            Registry registry = cluster.getManagement();
            assertTrue("JMX is disabled", registry != null);

            MBeanServer serverJMX = MBeanHelper.findMBeanServer();
            String sDomain        = registry.getDomainName();
            String sDomainSuffix  = sDomainNameSuffix.length() == 0
                                        ? ""
                                        : "@" + sDomainNameSuffix;
            assertEquals("Coherence" + sDomainSuffix, sDomain);
            String sCluster = sDomain + ":" + Registry.CLUSTER_TYPE;
            assertTrue("Should be registered: " + sCluster, serverJMX.isRegistered(new ObjectName(sCluster)));

            String sNode1 = sDomain + ":" + Registry.NODE_TYPE + ",nodeId=" + cluster.getLocalMember().getId();
            assertTrue("Should be registered: " + sNode1, serverJMX.isRegistered(new ObjectName(sNode1)));
            }
        finally
            {
            System.clearProperty("coherence.domain.name.suffix");
            AbstractFunctionalTest._shutdown();
            }
        }

    /**
     * Validate setting system property coherence.domain.name.suffix only.
     */
    @Test
    public void testSetDomainNameSuffixByOverrideAndSystemProperty()
            throws JMException, InterruptedException
        {
        String sDomainNameSuffix = DOMAIN_NAME_SUFFIX + "OverrideAndSysProp";

        Properties propsMain = new Properties();
        propsMain.put("coherence.management", "local-only");
        propsMain.put("coherence.management.remote", "true");
        propsMain.put("coherence.override", FILE_OPERATIONAL_OVERIDE);
        propsMain.put("test.domain.name.suffix", sDomainNameSuffix);
        System.getProperties().putAll(propsMain);

        AbstractFunctionalTest._startup();
        try
            {
            Cluster cluster = CacheFactory.getCluster();
            assertTrue(cluster.isRunning());
            assertEquals("cluster already exists", 1, cluster.getMemberSet().size());

            Registry registry = cluster.getManagement();
            assertTrue("JMX is disabled", registry != null);

            MBeanServer serverJMX = MBeanHelper.findMBeanServer();
            String sDomain        = registry.getDomainName();
            String sDomainSuffix  = sDomainNameSuffix.length() == 0
                                        ? ""
                                        : "@" + sDomainNameSuffix;
            assertEquals("Coherence" + sDomainSuffix, sDomain);
            String sCluster = sDomain + ":" + Registry.CLUSTER_TYPE;
            assertTrue("Should be registered: " + sCluster, serverJMX.isRegistered(new ObjectName(sCluster)));

            String sNode1 = sDomain + ":" + Registry.NODE_TYPE + ",nodeId=" + cluster.getLocalMember().getId();
            assertTrue("Should be registered: " + sNode1, serverJMX.isRegistered(new ObjectName(sNode1)));
            }
        finally
            {
            System.clearProperty("test.domain.name.suffix");
            System.clearProperty("coherence.override");
            AbstractFunctionalTest._shutdown();
            }
        }

    /**
     * Validate default domain is "Coherence".
     */
    @Test
    public void testDefaultDomain()
            throws JMException, InterruptedException
        {
        Properties propsMain = new Properties();
        propsMain.put("coherence.management", "local-only");
        propsMain.put("coherence.management.remote", "true");
        System.getProperties().putAll(propsMain);

        AbstractFunctionalTest._startup();
        try
            {
            Cluster cluster = CacheFactory.getCluster();
            assertTrue(cluster.isRunning());
            assertEquals("cluster already exists", 1, cluster.getMemberSet().size());

            Registry registry = cluster.getManagement();
            assertTrue("JMX is disabled", registry != null);

            MBeanServer serverJMX = MBeanHelper.findMBeanServer();
            String      sDomain   = registry.getDomainName();
            assertEquals("should be default Coherence domain name: Coherence", "Coherence", sDomain);
            String sCluster = sDomain + ":" + Registry.CLUSTER_TYPE;
            assertTrue("Should be registered: " + sCluster, serverJMX.isRegistered(new ObjectName(sCluster)));

            String sNode1 = sDomain + ":" + Registry.NODE_TYPE + ",nodeId=" + cluster.getLocalMember().getId();
            assertTrue("Should be registered: " + sNode1, serverJMX.isRegistered(new ObjectName(sNode1)));
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            }
        }

    /**
     * Operational override file for test. Defines new elememt management-config.domain.name.suffix.
     */
    public static String FILE_OPERATIONAL_OVERIDE = "setdomainprefix-coherence-override.xml";

    /**
     * Domain name suffix in operational configuration file
     */
    public static final String DOMAIN_NAME_SUFFIX = "SetDomainSuffixJmxTests";

    /**
     * The cache configuration file.
     */
    public final static String FILE_CFG_CACHE = "cache-config.xml";
    }
