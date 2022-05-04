/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package jmx;

import com.tangosol.coherence.discovery.Discovery;
import com.tangosol.net.management.MBeanHelper;

import com.tangosol.util.Base;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.io.Serializable;

import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the DiscoveryMBean registered for EM Integration.
 *
 * @author dag 2012.09.124
 */
@SuppressWarnings("serial")
public class DiscoveryMBeanTests
        extends AbstractFunctionalTest implements Serializable
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor initializes the cache config
     */
    public DiscoveryMBeanTests()
        {
        super(FILE_CFG_CACHE);
       }

    // ----- test lifecycle -------------------------------------------------

    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.role", "main");
        System.setProperty("coherence.log.level", "3");
        System.setProperty("coherence.management", "all");
        System.setProperty("coherence.management.remote", "true");
        System.setProperty("coherence.distributed.localstorage", "true");

        AbstractFunctionalTest._startup();
        }

    @AfterClass
    public static void _shutdown()
        {
        AbstractFunctionalTest._shutdown();
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Test that the DiscoveryMBean is available and has the right attributes
     */
    @Test
    public void testDiscoveryMBean()
        {
        try 
            {
            Discovery  discovery = new Discovery();
            ObjectName nameMBean = getQueryName(discovery);
            // connect to local MBeanServer to retrieve info for the MBean
            MBeanServer serverJMX = MBeanHelper.findMBeanServer();
    
            // get the DiscoveryMBean
            Set<ObjectName> setObjectNames = serverJMX.queryNames(nameMBean, null);
            assertEquals(1, setObjectNames.size());
            
            String sAddOnName = (String) serverJMX.getAttribute(nameMBean, "AddOnName");
            assertEquals(sAddOnName, discovery.getAddOnName());
            String sAddOnDisplayName = (String) serverJMX.getAttribute(nameMBean, "AddOnDisplayName");
            assertEquals(sAddOnDisplayName, discovery.getAddOnDisplayName());
            String sEMDiscoveryPluginName = (String) serverJMX.getAttribute(nameMBean, "EMDiscoveryPluginName");
            assertEquals(sEMDiscoveryPluginName, discovery.getEMDiscoveryPluginName());
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Return the ObjectName used in a query to get the DiscoveryMBean
     *
     * @return the ObjectName
     */
    protected ObjectName getQueryName(Discovery discovery)
        {
        try
            {
            return new ObjectName("EMDomain:name=oracle.sysman.emas.CoherenceDiscovery,type=EMDiscoveryIntegration");
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * cache configuration file with the required schemes.
     */
    public final static String FILE_CFG_CACHE = "cache-config.xml";
    }