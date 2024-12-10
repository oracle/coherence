/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import com.tangosol.coherence.reporter.ReportBatch;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Filter;
import com.tangosol.util.Resources;
import com.tangosol.util.WrapperException;

import java.net.URL;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for DefaultGatewayDependencies (management-config element).
 *
 * @author DER  2011.07.18
 */
public class DefaultGatewayDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default values.
     */
    @Test
    public void testDefaultNoConfig()
        {
        DefaultConnectorDependencies connDeps = new DefaultConnectorDependencies();


        DefaultCustomMBeanDependencies customDeps = new DefaultCustomMBeanDependencies();
        List<CustomMBeanDependencies> listCustomMBeansDependencies = new ArrayList();
        listCustomMBeansDependencies.add(customDeps);

        ReportBatch.DefaultDependencies repDeps = new ReportBatch.DefaultDependencies();
        DefaultGatewayDependencies gatewayDeps = new DefaultGatewayDependencies();
        gatewayDeps.setConnectorDependencies(connDeps);
        gatewayDeps.setCustomMBeanDependencies(listCustomMBeansDependencies);
        gatewayDeps.setReporterDependencies(repDeps);

        gatewayDeps.validate();
        System.out.println("DefaultGatewayDependenciesTest.testDefaultNoConfig:");
        System.out.println(gatewayDeps.toString());

        assertEquals(gatewayDeps.getManagedNodes(), "none");
        assertTrue(gatewayDeps.isAllowRemoteManagement());
        assertFalse(gatewayDeps.isReadOnly());
        // Connector Dependencies
        assertEquals(gatewayDeps.getConnectorDependencies().getRefreshRequestTimeoutMillis(), 250);
        assertEquals(gatewayDeps.getConnectorDependencies().getRefreshPolicy(), ConnectorDependencies.REFRESH_EXPIRED);
        assertEquals(gatewayDeps.getConnectorDependencies().getRefreshTimeoutMillis(), 1000);
        // Reporter Dependencies
        assertEquals(gatewayDeps.getReporterDependencies().getConfigFile(), "reports/report-group.xml");
        assertEquals(gatewayDeps.getReporterDependencies().getDateFormat(), "EEE MMM dd HH:mm:ss zzz yyyy");
        assertEquals(gatewayDeps.getReporterDependencies().getTimeZone(),   "");
        assertFalse(gatewayDeps.getReporterDependencies().isAutoStart());
        assertFalse(gatewayDeps.getReporterDependencies().isDistributed());
        // Custom MBean Dependencies
        for (CustomMBeanDependencies cmbd : gatewayDeps.getCustomMBeanDependencies())
            {
            DefaultCustomMBeanDependencies deps = (DefaultCustomMBeanDependencies) cmbd;

            assertFalse(deps.isLocalOnly());
            assertFalse(deps.isEnabled());
            assertFalse(deps.isExtendLifecycle());
            }

        // test the clone logic
        GatewayDependencies gatewayDeps2 = new DefaultGatewayDependencies(gatewayDeps);
        assertCloneEquals(gatewayDeps, gatewayDeps2);
        }

    /**
     * Test the values set from the default operational config file.
     *
     * @throws Exception general exception
     */
    @Test
    public void testDefaultOperConfig()
            throws Exception
        {
        URL         url          = Resources.findFileOrResource("tangosol-coherence.xml", null);
        XmlDocument xmlCoherence = XmlHelper.loadXml(url);
        XmlHelper.replaceSystemProperties(xmlCoherence, "system-property");
        XmlElement xmlManagement = xmlCoherence.getSafeElement("management-config");

        DefaultGatewayDependencies gatewayDeps =
            LegacyXmlGatewayHelper.fromXml(xmlManagement, new DefaultGatewayDependencies());

        gatewayDeps.validate();
        System.out.println("DefaultGatewayDependenciesTest.testDefaultConfig:");
        System.out.println(gatewayDeps.toString());

        assertEquals("dynamic", gatewayDeps.getManagedNodes());
        assertTrue(gatewayDeps.isAllowRemoteManagement());
        assertFalse(gatewayDeps.isReadOnly());

        assertTrue(gatewayDeps.getFilter() instanceof Filter);

        // Connector Dependencies
        assertEquals(gatewayDeps.getConnectorDependencies().getRefreshRequestTimeoutMillis(), 250);
        assertEquals(gatewayDeps.getConnectorDependencies().getRefreshPolicy(), ConnectorDependencies.REFRESH_AHEAD);
        assertEquals(gatewayDeps.getConnectorDependencies().getRefreshTimeoutMillis(), 1000);
        // Reporter Dependencies
        assertEquals(gatewayDeps.getReporterDependencies().getConfigFile(), "reports/report-group.xml");
        assertEquals(gatewayDeps.getReporterDependencies().getDateFormat(), "EEE MMM dd HH:mm:ss zzz yyyy");
        assertEquals(gatewayDeps.getReporterDependencies().getTimeZone(),   "");
        assertFalse(gatewayDeps.getReporterDependencies().isAutoStart());
        assertFalse(gatewayDeps.getReporterDependencies().isDistributed());
        // Custom MBean Dependencies
        for (CustomMBeanDependencies cmbd : gatewayDeps.getCustomMBeanDependencies())
            {
            DefaultCustomMBeanDependencies deps = (DefaultCustomMBeanDependencies) cmbd;

            if (deps.getMBeanName().equals("type=Platform"))
                {
                assertFalse(deps.isLocalOnly());
                assertTrue(deps.isEnabled());
                assertFalse(deps.isExtendLifecycle());
                assertEquals(deps.getMBeanQuery(), "java.lang:*");
                }
            }
        }

    /**
     * Test the various gatewayDependencies settings.
     */
    @Test
    public void testGatewayDependencies1()
        {
        String xmlString = "<management-config>"
                + " <managed-nodes>local-only</managed-nodes>"
                + " <default-domain-name>myDefaultDomain</default-domain-name>"
                + " <allow-remote-management>false</allow-remote-management>"
                + " <read-only>false</read-only>"
                + "</management-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        DefaultGatewayDependencies deps =
            LegacyXmlGatewayHelper.fromXml(xml, new DefaultGatewayDependencies());

        assertEquals(deps.getManagedNodes(), "local-only");
        assertEquals(deps.getDefaultDomain(), "myDefaultDomain");
        assertFalse(deps.isAllowRemoteManagement());
        assertFalse(deps.isReadOnly());
        }

    /**
     * Test the various gatewayDependencies settings.
     */
    @Test
    public void testGatewayDependencies2()
        {
        String xmlString = "<management-config>"
                + " <managed-nodes>remote-only</managed-nodes>"
                + " <allow-remote-management>true</allow-remote-management>"
                + " <read-only>true</read-only>"
                + "</management-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        DefaultGatewayDependencies deps =
            LegacyXmlGatewayHelper.fromXml(xml, new DefaultGatewayDependencies());

        assertEquals(deps.getManagedNodes(), "remote-only");
        assertTrue(deps.isAllowRemoteManagement());
        assertTrue(deps.isReadOnly());
        }

    /**
     * Test the reporter values.
     */
    @Test
    public void testGatewayDependenciesReporter()
        {
        String xmlString = "<management-config>"
                + " <reporter>"
                + "<autostart>true</autostart>"
                + "<distributed>true</distributed>"
                + "<timezone>America/Los_Angeles</timezone>"
                + "<timeformat>EEE MMM dd yyyy HH:mm:ss zzz</timeformat>"
                + "</reporter>"
                + "</management-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        DefaultGatewayDependencies deps =
            LegacyXmlGatewayHelper.fromXml(xml, new DefaultGatewayDependencies());

        // Reporter Dependencies
        assertEquals(deps.getReporterDependencies().getDateFormat(), "EEE MMM dd yyyy HH:mm:ss zzz");
        assertEquals(deps.getReporterDependencies().getTimeZone(),   "America/Los_Angeles");
        assertTrue(deps.getReporterDependencies().isAutoStart());
        assertTrue(deps.getReporterDependencies().isDistributed());
        }

    /**
     * Test the various gatewayDependencies settings.
     */
    @Test
    public void testGatewayDependencies3()
        {
        String xmlString = "<management-config>"
                + " <managed-nodes>all</managed-nodes>"
                + "</management-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        DefaultGatewayDependencies deps =
            LegacyXmlGatewayHelper.fromXml(xml, new DefaultGatewayDependencies());

        assertEquals(deps.getManagedNodes(), "all");
        }

    /**
     * Test an custom MBeans.
     */
    @Test
    public void testGatewayCustomMBeans()
        {
        String xmlString = "<management-config>"
            + "<mbeans>"
            + "<mbean id=\"1\">"
            + "<mbean-query>java.lang:*</mbean-query>"
            + "<mbean-name>customMBean1</mbean-name>"
            + "<mbean-server-domain>test1ServerDomain</mbean-server-domain>"
            + "<enabled>true</enabled>"
            + "<local-only>true</local-only>"
            + "<extend-lifecycle>true</extend-lifecycle>"
            + "</mbean>"
            + "<mbean id=\"2\">"
            + "<mbean-class>com.tangosol.net.management.customMBeanClass</mbean-class>"
            + "<mbean-name>customMBean2</mbean-name>"
            + "<mbean-server-domain>test2ServerDomain</mbean-server-domain>"
            + "<enabled>false</enabled>"
            + "<local-only>false</local-only>"
            + "<extend-lifecycle>false</extend-lifecycle>"
            + "</mbean>"
            + "<mbean id=\"3\">"
            + "<mbean-factory>com.tangosol.net.management.customMBeanFactory</mbean-factory>"
            + "<mbean-accessor>accessorMethod</mbean-accessor>"
            + "<mbean-name>customMBean3</mbean-name>"
            + "<mbean-server-domain>test3ServerDomain</mbean-server-domain>"
            + "<enabled>false</enabled>"
            + "<local-only>true</local-only>"
            + "<extend-lifecycle>false</extend-lifecycle>"
            + "</mbean>"
            + "</mbeans>"
            + "</management-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        DefaultGatewayDependencies deps =
            LegacyXmlGatewayHelper.fromXml(xml, new DefaultGatewayDependencies());

        // Custom MBean Dependencies
        for (CustomMBeanDependencies cmbd : deps.getCustomMBeanDependencies())
            {
            DefaultCustomMBeanDependencies dcmbd = (DefaultCustomMBeanDependencies) cmbd;

            if (dcmbd.getMBeanName().equals("customMBean1"))
                {
                assertTrue(dcmbd.isEnabled());
                assertTrue(dcmbd.isLocalOnly());
                assertTrue(dcmbd.isExtendLifecycle());
                assertEquals(dcmbd.getMBeanQuery(), "java.lang:*");
                assertEquals(dcmbd.getMBeanServerDomain(), "test1ServerDomain");
                }
            else if (dcmbd.getMBeanName().equals("customMBean2"))
                {
                assertFalse(dcmbd.isEnabled());
                assertFalse(dcmbd.isLocalOnly());
                assertFalse(dcmbd.isExtendLifecycle());
                assertEquals(dcmbd.getMBeanClass(), "com.tangosol.net.management.customMBeanClass");
                assertEquals(dcmbd.getMBeanServerDomain(), "test2ServerDomain");
                }
            else if (dcmbd.getMBeanName().equals("customMBean3"))
                {
                assertFalse(dcmbd.isEnabled());
                assertTrue(dcmbd.isLocalOnly());
                assertFalse(dcmbd.isExtendLifecycle());
                assertEquals(dcmbd.getMBeanFactory(), "com.tangosol.net.management.customMBeanFactory");
                assertEquals(dcmbd.getMBeanAccessor(), "accessorMethod");
                assertEquals(dcmbd.getMBeanServerDomain(), "test3ServerDomain");
                }
            }
        }

    /**
     * Test an invalid MBean Filter.
     */
    @Test
    public void testGatewayInvalidMBeanFilter()
        {
        String xmlString = "<management-config>"
            + "<mbean-filter>"
            + "<class-name>com.tangosol.net.management.badFilter</class-name>"
            + "</mbean-filter>"
            + "</management-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        try
            {
            DefaultGatewayDependencies deps =
                LegacyXmlGatewayHelper.fromXml(xml, new DefaultGatewayDependencies());
            fail("Expected error instantiating MBean Filter");
            }
        catch (Exception e)
            {
            if (!(e instanceof IllegalArgumentException))
                {
                fail("Expected an IllegalArgumentException while instantiating a MBean Filter.");
                }
            }
        }

    /**
     * Test an invalid server factory.
     */
    @Test
    public void testGatewayInvalidServerFactory()
        {
        String xmlString = "<management-config>"
            + "<server-factory>"
            + "<class-name>tangosol.coherence.management.badServerfactory</class-name>"
            + "</server-factory>"
            + "</management-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        try
            {
            DefaultGatewayDependencies deps =
                LegacyXmlGatewayHelper.fromXml(xml, new DefaultGatewayDependencies());
            fail("Expected error instantiating server factory");
            }
        catch (Exception e)
            {
            if (!(e instanceof WrapperException))
                {
                fail("Expected an exception while instantiating a server factory.");
                }
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the two GatewayDependencies are equal.
     *
     * @param deps1  the first GatewayDependencies object
     * @param deps2  the second GatewayDependencies object
     */
    protected void assertCloneEquals(GatewayDependencies deps1, GatewayDependencies deps2)
        {
        assertEquals(deps1.isAllowRemoteManagement(),       deps2.isAllowRemoteManagement());
        assertEquals(deps1.getConnectorDependencies(),      deps2.getConnectorDependencies());
        assertEquals(deps1.getCustomMBeanDependencies(),    deps2.getCustomMBeanDependencies());
        assertEquals(deps1.getFilter(),                     deps2.getFilter());
        assertEquals(deps1.getManagedNodes(),               deps2.getManagedNodes());
        assertEquals(deps1.isReadOnly(),                    deps2.isReadOnly());
        assertEquals(deps1.getReporterDependencies(),       deps2.getReporterDependencies());
        assertEquals(deps1.getServer(),                     deps2.getServer());
        }
    }
