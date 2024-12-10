/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Unit tests for DefaultConnectorDependencies (management-config element).
 *
 * @author DER  2011.07.18
 */
public class DefaultConnectorDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default values.
     */
    @Test
    public void testDefaultNoConfig()
        {
        DefaultConnectorDependencies connDeps = new DefaultConnectorDependencies();

        connDeps.validate();
        System.out.println("DefaultConnectorDependenciesTest.testDefaultNoConfig:");
        System.out.println(connDeps.toString());

        assertEquals(connDeps.getRefreshRequestTimeoutMillis(), 250);
        assertEquals(connDeps.getRefreshPolicy(), ConnectorDependencies.REFRESH_EXPIRED);
        assertEquals(connDeps.getRefreshTimeoutMillis(), 1000);

        // test the clone logic
        DefaultConnectorDependencies deps2 = new DefaultConnectorDependencies(connDeps);
        assertCloneEquals(connDeps, deps2);
        }

    /**
     * Test the ConnectorDependencies.
     */
    @Test
    public void testConnectorDependencies1()
        {
        String xmlString = "<management-config>"
                + " <refresh-policy>refresh-behind</refresh-policy>"
                + " <refresh-expiry>2000ms</refresh-expiry>"
                + " <refresh-timeout>250s</refresh-timeout>"
                + "</management-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        DefaultConnectorDependencies deps =
            LegacyXmlConnectorHelper.fromXml(xml, new DefaultConnectorDependencies());

        assertEquals(deps.getRefreshPolicy(), ConnectorDependencies.REFRESH_BEHIND);
        assertEquals(deps.getRefreshTimeoutMillis(), 2000);
        assertEquals(deps.getRefreshRequestTimeoutMillis(), 250000);
        }

    /**
     * Test the ConnectorDependencies.
     */
    @Test
    public void testConnectorDependencies2()
        {
        String xmlString = "<management-config>"
                + " <refresh-policy>refresh-expired</refresh-policy>"
                + " <refresh-expiry>3000</refresh-expiry>"
                + " <refresh-timeout>1h</refresh-timeout>"
                + "</management-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        DefaultConnectorDependencies deps =
            LegacyXmlConnectorHelper.fromXml(xml, new DefaultConnectorDependencies());

        assertEquals(deps.getRefreshPolicy(), ConnectorDependencies.REFRESH_EXPIRED);
        assertEquals(deps.getRefreshTimeoutMillis(), 3000);
        assertEquals(deps.getRefreshRequestTimeoutMillis(), 3600000);
        }

    /**
     * Test the ConnectorDependencies with an invalid refresh policy.
     */
    @Test
    public void testConnectorInvalidRefreshPolicy()
        {
        String xmlString = "<management-config>"
                + " <refresh-policy>garbage</refresh-policy>"
                + " <refresh-expiry>1h</refresh-expiry>"
                + " <refresh-timeout>100</refresh-timeout>"
                + "</management-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        try
            {
            DefaultConnectorDependencies deps =
                LegacyXmlConnectorHelper.fromXml(xml, new DefaultConnectorDependencies());
            fail("Expected an IllegalArgumentException because of invalid refresh-policy");
            }
        catch (Exception e)
            {
            if (!(e instanceof IllegalArgumentException))
                {
                fail("Expected an IllegalArgumentException.");
                }
            }
        }

    /**
     * Test the ConnectorDependencies with an invalid refresh policy.
     */
    @Test
    public void testConnectorInvalidRefreshExpiry()
        {
        String xmlString = "<management-config>"
                + " <refresh-policy>refresh-expired</refresh-policy>"
                + " <refresh-expiry>garbage</refresh-expiry>"
                + " <refresh-timeout>100</refresh-timeout>"
                + "</management-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        try
            {
            DefaultConnectorDependencies deps =
                LegacyXmlConnectorHelper.fromXml(xml, new DefaultConnectorDependencies());

            fail("Expected an NumberFormatException because of invalid time");
            }
        catch (Exception e)
            {
            if (!(e.getCause() instanceof java.lang.NumberFormatException))
                {
                fail("Expected a NumberFormatException.");
                }
            }
        }

    /**
     * Test the ConnectorDependencies with an invalid refresh policy.
     */
    @Test
    public void testConnectorInvalidRefreshTimeout()
        {
        String xmlString = "<management-config>"
                + " <refresh-policy>refresh-expired</refresh-policy>"
                + " <refresh-expiry>1s</refresh-expiry>"
                + " <refresh-timeout>garbage</refresh-timeout>"
                + "</management-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        try
            {
            DefaultConnectorDependencies deps =
                LegacyXmlConnectorHelper.fromXml(xml, new DefaultConnectorDependencies());

            fail("Expected an NumberFormatException because of invalid time");
            }
        catch (Exception e)
            {
            if (!(e.getCause() instanceof java.lang.NumberFormatException))
                {
                fail("Expected a NumberFormatException.");
                }
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the two ConnectorDependencies are equal.
     *
     * @param deps1  the first ConnectorDependencies object
     * @param deps2  the second ConnectorDependencies object
     */
    protected void assertCloneEquals(ConnectorDependencies deps1, ConnectorDependencies deps2)
        {
        assertEquals(deps1.getRefreshPolicy(),               deps2.getRefreshPolicy());
        assertEquals(deps1.getRefreshRequestTimeoutMillis(), deps2.getRefreshRequestTimeoutMillis());
        assertEquals(deps1.getRefreshTimeoutMillis(),        deps2.getRefreshTimeoutMillis());
        }
    }
