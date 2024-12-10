/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.xml.preprocessor.SystemPropertyPreprocessor;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.SystemPropertyParameterResolver;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.oracle.coherence.testing.SystemPropertyResource;

import org.junit.Assert;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        ProcessingContext           ctx  = mock(ProcessingContext.class);

        when(ctx.getDefaultParameterResolver()).thenReturn(SystemPropertyParameterResolver.INSTANCE);

        DocumentElementPreprocessor dep  = new DocumentElementPreprocessor();

        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        dep.preprocess(ctx, xml);
        Assert.assertEquals("undefined", xml.getString());
        Assert.assertNull(xml.getAttribute("system-property"));

        System.setProperty("custom", "hello");
        xml = XmlHelper.loadXml(sXml);
        dep.preprocess(ctx, xml);

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
        ProcessingContext           ctx  = mock(ProcessingContext.class);

        when(ctx.getDefaultParameterResolver()).thenReturn(SystemPropertyParameterResolver.INSTANCE);

        DocumentElementPreprocessor dep  = new DocumentElementPreprocessor();

        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        dep.preprocess(ctx, xml);
        Assert.assertEquals("undefined", xml.getString());
        Assert.assertNull(xml.getAttribute("system-property"));

        try (SystemPropertyResource p = new SystemPropertyResource("tangosol.coherence.system.property", "backwardscompatibilehello"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(ctx, xml);
            Assert.assertEquals("backwardscompatibilehello", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }

        try (SystemPropertyResource p = new SystemPropertyResource("coherence.system.property", "goodbye"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(ctx, xml);
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
        ProcessingContext           ctx  = mock(ProcessingContext.class);

        when(ctx.getDefaultParameterResolver()).thenReturn(SystemPropertyParameterResolver.INSTANCE);

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor();

        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        dep.preprocess(ctx, xml);
        Assert.assertEquals("near-direct", xml.getString());
        Assert.assertNull(xml.getAttribute("system-property"));

        try (SystemPropertyResource p = new SystemPropertyResource("coherence.client", "remote"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(ctx, xml);
            Assert.assertEquals("near-remote", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }

        try (SystemPropertyResource p = new SystemPropertyResource("coherence.client", "direct"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(ctx, xml);
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
        ProcessingContext           ctx  = mock(ProcessingContext.class);

        when(ctx.getDefaultParameterResolver()).thenReturn(SystemPropertyParameterResolver.INSTANCE);

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor();

        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        dep.preprocess(ctx, xml);
        Assert.assertEquals("near-direct-default", xml.getString());
        Assert.assertNull(xml.getAttribute("system-property"));

        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.client", "remote");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.macro", "notdefault"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(ctx, xml);
            Assert.assertEquals("near-remote-notdefault", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }

        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.client", "direct");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.macro", "anotherValue"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(ctx, xml);
            Assert.assertEquals("near-direct-anotherValue", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }

        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.client", "remote");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.macro", "anotherValue");
             SystemPropertyResource p3 = new SystemPropertyResource("coherence.profile",
                "system-property-macro-replacement-${coherence.client direct}-${coherence.macro default}"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(ctx, xml);
            Assert.assertEquals("system-property-macro-replacement-remote-anotherValue", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }

        try (SystemPropertyResource p = new SystemPropertyResource("coherence.profile",
            "system-property-macro-replacement-${coherence.client direct}-${coherence.macro default}"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(ctx, xml);
            Assert.assertEquals("system-property-macro-replacement-direct-default", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }

        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.client", "remote");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.profile",
                "system-property-macro-replacement-${coherence.client direct}-${coherence.client altdefault}"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(ctx, xml);
            Assert.assertEquals("system-property-macro-replacement-remote-remote", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }

        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.profile",
            "system-property-macro-replacement-${coherence.client default}-${coherence.client altdefault}"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(ctx, xml);
            Assert.assertEquals("system-property-macro-replacement-default-altdefault", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }
        }

    /**
     * Regression test for COH-14214
     */
    @Test(timeout = 10000)
    public void testRecursiveMacroProcessing()
        {
        String sXml =
                "<element system-property=\"coherence.profile\">near-${coherence.client direct}-${coherence.macro default}</element>\n";
        XmlElement        xml = XmlHelper.loadXml(sXml);
        ProcessingContext ctx = mock(ProcessingContext.class);

        when(ctx.getDefaultParameterResolver()).thenReturn(SystemPropertyParameterResolver.INSTANCE);

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor();

        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.client", "${coherence.client remote}");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.macro", "notdefault"))
            {
            xml = XmlHelper.loadXml(sXml);
            dep.preprocess(ctx, xml);
            Assert.assertEquals("near-direct-notdefault", xml.getString());
            Assert.assertNull(xml.getAttribute("system-property"));
            }
        }

    /**
     * Another Regression test for COH-14214
     */
    @Test(timeout = 10000)
    public void testRecursiveMacroProcessingOfCycleOfMacros()
        {
        String            sXml = "<config><enable-pof-serialization system-property=\"tangosol.pof.enabled\">false</enable-pof-serialization></config>\n";
        XmlElement        xml  = XmlHelper.loadXml(sXml);
        ProcessingContext ctx  = mock(ProcessingContext.class);

        when(ctx.getDefaultParameterResolver()).thenReturn(SystemPropertyParameterResolver.INSTANCE);

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor();

        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.pof.enabled", "${alternate.pof.enabled}");
             SystemPropertyResource p2 = new SystemPropertyResource("alternate.pof.enabled", "${coherence.pof.enabled}"))
            {
            dep.preprocess(ctx, xml);
            //Assert.assertEquals("near-remote-notdefault", xml.getString());
            //Assert.assertNull(xml.getAttribute("system-property"));
            }

        try (SystemPropertyResource p1 = new SystemPropertyResource("tangosol.pof.enabled", "${tangosol.pof.enabled}"))
            {
            dep.preprocess(ctx, xml);
            }

        }

    /**
     * Regression test for reported failure in COH-14214
     */
    @Test
    public void testCOH14214()
        {
        String            sXml = "<config><enable-pof-serialization system-property=\"tangosol.pof.enabled\">false</enable-pof-serialization></config>\n";
        XmlElement        xml  = XmlHelper.loadXml(sXml);
        ProcessingContext ctx  = mock(ProcessingContext.class);

        when(ctx.getDefaultParameterResolver()).thenReturn(SystemPropertyParameterResolver.INSTANCE);

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor();

        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        try (SystemPropertyResource p1 = new SystemPropertyResource("tangosol.pof.enabled", "${tangosol.pof.enabled}"))
            {
            dep.preprocess(ctx, xml);
            }
        }

    @Test
    public void shouldUseContextValuesFirst()
        {
        String                  sXml = "<config><test system-property=\"coherence.test\">one</test></config>\n";
        XmlElement              xml  = XmlHelper.loadXml(sXml);
        ProcessingContext       ctx  = mock(ProcessingContext.class);
        ResolvableParameterList list = new ResolvableParameterList();

        list.add(new Parameter("coherence.test", "two"));

        when(ctx.getDefaultParameterResolver()).thenReturn(list);

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor();

        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.test", "three"))
            {
            dep.preprocess(ctx, xml);
            assertThat(xml.getElement("test").getString(), is("two"));
            }
        }

    }
