/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.coherence.config.xml.preprocessor.SystemPropertyPreprocessor;

import com.tangosol.config.ConfigurationException;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import common.SystemPropertyResource;

import org.junit.Assert;

import org.junit.Test;

/**
 * Unit tests for {@link SystemPropertyPreprocessor}s.
 *
 * @author bo  2011.07.29
 */
public class SystemPropertyPreprocessorTest
    {
    /**
     * Ensure the {@link SystemPropertyPreprocessor} replace system properties as expected.
     *
     * @throws ConfigurationException
     */
    @Test
    public void testSystemPropertyResourceSubstitutionVisitor()
            throws ConfigurationException
        {
        String                      sXml = "<element system-property=\"custom\">undefined</element>";
        XmlElement                  xml  = XmlHelper.loadXml(sXml);

        DocumentElementPreprocessor dep  = new DocumentElementPreprocessor();

        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        dep.preprocess(null, xml);
        Assert.assertEquals("undefined", xml.getString());
        Assert.assertNull(xml.getAttribute("system-property"));

        System.setProperty("custom", "hello");
        xml = XmlHelper.loadXml(sXml);
        dep.preprocess(null, xml);

        Assert.assertEquals("hello", xml.getString());
        Assert.assertNull(xml.getAttribute("system-property"));
        }

    /**
     * Ensure the {@link SystemPropertyPreprocessor} replace system properties as expected according to COH-12944.
     *
     * @throws ConfigurationException
     */
    @Test
    public void testCoherenceSystemPropertyResourceProcessing()
            throws ConfigurationException
        {
        String                      sXml = "<element system-property=\"coherence.system.property\">undefined</element>";
        XmlElement                  xml  = XmlHelper.loadXml(sXml);

        DocumentElementPreprocessor dep  = new DocumentElementPreprocessor();

        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        dep.preprocess(null, xml);
        Assert.assertEquals("undefined", xml.getString());
        Assert.assertNull(xml.getAttribute("system-property"));

        try (SystemPropertyResource p = new SystemPropertyResource("tangosol.coherence.system.property", "backwardscompatibilehello"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(null, xml);
            Assert.assertEquals("backwardscompatibilehello", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }

        try (SystemPropertyResource p = new SystemPropertyResource("coherence.system.property", "goodbye"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(null, xml);
            Assert.assertEquals("goodbye", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }
        }

    /**
     * Ensure the {@link SystemPropertyPreprocessor} performs macro expansion on element value.
     *
     * @throws ConfigurationException
     */
    @Test
    public void testSimpleMacroProcessing()
            throws ConfigurationException
        {
        String sXml =
            "<scheme-name system-property=\"coherence.profile\">near-${coherence.client direct}</scheme-name>\n";
        XmlElement                  xml = XmlHelper.loadXml(sXml);

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor();

        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        dep.preprocess(null, xml);
        Assert.assertEquals("near-direct", xml.getString());
        Assert.assertNull(xml.getAttribute("system-property"));

        try (SystemPropertyResource p = new SystemPropertyResource("coherence.client", "remote"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(null, xml);
            Assert.assertEquals("near-remote", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }

        try (SystemPropertyResource p = new SystemPropertyResource("coherence.client", "direct"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(null, xml);
            Assert.assertEquals("near-direct", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }
        }

    /**
     * Ensure the {@link SystemPropertyPreprocessor} replace system properties as expected according to COH-12944.
     *
     * @throws ConfigurationException
     */
    @Test
    public void testMacroProcessing()
            throws ConfigurationException
        {
        String sXml =
            "<element system-property=\"coherence.profile\">near-${coherence.client direct}-${coherence.macro default}</element>\n";
        XmlElement                  xml = XmlHelper.loadXml(sXml);

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor();

        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        dep.preprocess(null, xml);
        Assert.assertEquals("near-direct-default", xml.getString());
        Assert.assertNull(xml.getAttribute("system-property"));

        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.client", "remote");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.macro", "notdefault"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(null, xml);
            Assert.assertEquals("near-remote-notdefault", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }

        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.client", "direct");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.macro", "anotherValue"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(null, xml);
            Assert.assertEquals("near-direct-anotherValue", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }

        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.client", "remote");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.macro", "anotherValue");
             SystemPropertyResource p3 = new SystemPropertyResource("coherence.profile",
                "system-property-macro-replacement-${coherence.client direct}-${coherence.macro default}"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(null, xml);
            Assert.assertEquals("system-property-macro-replacement-remote-anotherValue", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }

        try (SystemPropertyResource p = new SystemPropertyResource("coherence.profile",
            "system-property-macro-replacement-${coherence.client direct}-${coherence.macro default}"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(null, xml);
            Assert.assertEquals("system-property-macro-replacement-direct-default", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }

        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.client", "remote");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.profile",
                "system-property-macro-replacement-${coherence.client direct}-${coherence.client altdefault}"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(null, xml);
            Assert.assertEquals("system-property-macro-replacement-remote-remote", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }

        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.profile",
            "system-property-macro-replacement-${coherence.client default}-${coherence.client altdefault}"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(null, xml);
            Assert.assertEquals("system-property-macro-replacement-default-altdefault", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }
        }

    /**
     * Regression test for COH-14214
     */
    @Test
    public void testRecursiveMacroProcessing()
        {

        String sXml =
                "<element system-property=\"coherence.profile\">near-${coherence.client direct}-${coherence.macro default}</element>\n";
        XmlElement xml = XmlHelper.loadXml(sXml);

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor();

        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.client", "${coherence.client remote}");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.macro", "notdefault"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(null, xml);
            Assert.assertEquals("near-direct-notdefault", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }
        }

    /**
     * Another Regression test for COH-14214
     */
    @Test
    public void testRecursiveMacroProcessingOfCycleOfMacros()
        {

        String sXml = "<config><enable-pof-serialization system-property=\"tangosol.pof.enabled\">false</enable-pof-serialization></config>\n";
        XmlElement xml = XmlHelper.loadXml(sXml);

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor();

        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.pof.enabled", "${alternate.pof.enabled}");
             SystemPropertyResource p2 = new SystemPropertyResource("alternate.pof.enabled", "${coherence.pof.enabled}"))
            {
            dep.preprocess(null, xml);
            //Assert.assertEquals("near-remote-notdefault", xml.getString());
            //Assert.assertNull(xml.getAttribute("system-property"));
            }

        try (SystemPropertyResource p1 = new SystemPropertyResource("tangosol.pof.enabled", "${tangosol.pof.enabled}"))
            {
            dep.preprocess(null, xml);
            }

        }

    /**
     * Regression test for reported failure in COH-14214
     */
    @Test
    public void testCOH14214()
        {

        String sXml = "<config><enable-pof-serialization system-property=\"tangosol.pof.enabled\">false</enable-pof-serialization></config>\n";
        XmlElement xml = XmlHelper.loadXml(sXml);

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor();

        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        try (SystemPropertyResource p1 = new SystemPropertyResource("tangosol.pof.enabled", "${tangosol.pof.enabled}"))
            {
            dep.preprocess(null, xml);
            }
        }
    }
