/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import com.tangosol.coherence.reporter.ReportBatch;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlHelper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for DefaultReporterDependencies (management-config/reporter element).
 *
 * @author DER  2011.07.18
 */
public class ReportBatchDefaultDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default values.
     */
    @Test
    public void testDefaultNoConfig()
        {
        ReportBatch.DefaultDependencies repDeps = new ReportBatch.DefaultDependencies();

        repDeps.validate();
        System.out.println("ReportBatchDefaultDependenciesTest.testDefaultNoConfig:");
        System.out.println(repDeps.toString());

        // Reporter Dependencies
        assertEquals(repDeps.getConfigFile(), "reports/report-group.xml");
        assertEquals(repDeps.getDateFormat(), "EEE MMM dd HH:mm:ss zzz yyyy");
        assertEquals(repDeps.getTimeZone(),   "");
        assertFalse(repDeps.isAutoStart());
        assertFalse(repDeps.isDistributed());

        // test the clone logic
        ReportBatch.DefaultDependencies repDeps2 = new ReportBatch.DefaultDependencies(repDeps);
        assertCloneEquals(repDeps, repDeps2);
        }

    /**
     * Test the reporter values.
     */
    @Test
    public void testReporterDependencies1()
        {
        String xmlString = " <reporter>"
                + "<autostart>true</autostart>"
                + "<distributed>true</distributed>"
                + "<timezone>America/Los_Angeles</timezone>"
                + "<timeformat>EEE MMM dd yyyy HH:mm:ss zzz</timeformat>"
                + "</reporter>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        ReportBatch.DefaultDependencies deps =
            LegacyXmlReporterHelper.fromXml(xml, new ReportBatch.DefaultDependencies());

        deps.validate();
        System.out.println("ReportBatchDefaultDependenciesTest.testReporterDependencies1:");
        System.out.println(deps.toString());

        assertEquals(deps.getDateFormat(), "EEE MMM dd yyyy HH:mm:ss zzz");
        assertEquals(deps.getTimeZone(),   "America/Los_Angeles");
        assertTrue(deps.isAutoStart());
        assertTrue(deps.isDistributed());
        }

    /**
     * Test the reporter values.
     */
    @Test
    public void testReporterDependencies2()
        {
        String xmlString = " <reporter>"
                + "<autostart>true</autostart>"
                + "<distributed>true</distributed>"
                + "<timezone>America/Los_Angeles</timezone>"
                + "<timeformat>EEE MMM dd yyyy HH:mm:ss zzz</timeformat>"
                + "</reporter>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        ReportBatch.DefaultDependencies deps =
            LegacyXmlReporterHelper.fromXml(xml, new ReportBatch.DefaultDependencies());

        deps.validate();

        assertEquals(deps.getDateFormat(), "EEE MMM dd yyyy HH:mm:ss zzz");
        assertEquals(deps.getTimeZone(),   "America/Los_Angeles");
        assertTrue(deps.isAutoStart());
        assertTrue(deps.isDistributed());
        }

    /**
     * Test the reporter values.
     */
    @Test
    public void testReporterDependencies3()
        {
        String xmlString = " <reporter>"
                + "<autostart>false</autostart>"
                + "<distributed>false</distributed>"
                + "</reporter>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        ReportBatch.DefaultDependencies deps =
            LegacyXmlReporterHelper.fromXml(xml, new ReportBatch.DefaultDependencies());

        deps.validate();

        assertFalse(deps.isAutoStart());
        assertFalse(deps.isDistributed());
        }

    /**
     * Test the reporter values.
     */
    @Test
    public void testReporterDependencies4()
        {
        String xmlString = " <reporter>"
                + "<autostart>garbage</autostart>"
                + "<distributed>garbage</distributed>"
                + "</reporter>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        ReportBatch.DefaultDependencies deps =
            LegacyXmlReporterHelper.fromXml(xml, new ReportBatch.DefaultDependencies());

        deps.validate();
        // Will use default when value is not a correct boolean
        assertFalse(deps.isAutoStart());
        assertFalse(deps.isDistributed());
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the two ReporterDependencies are equal.
     *
     * @param deps1  the first ReporterDependencies object
     * @param deps2  the second ReporterDependencies object
     */
    protected void assertCloneEquals(ReportBatch.DefaultDependencies deps1, ReportBatch.DefaultDependencies deps2)
        {
        assertEquals(deps1.getConfigFile(), deps2.getConfigFile());
        assertEquals(deps1.getDateFormat(), deps2.getDateFormat());
        assertEquals(deps1.getTimeZone(),   deps2.getTimeZone());
        assertEquals(deps1.isAutoStart(),   deps2.isAutoStart());
        assertEquals(deps1.isDistributed(), deps2.isDistributed());
        }
    }
