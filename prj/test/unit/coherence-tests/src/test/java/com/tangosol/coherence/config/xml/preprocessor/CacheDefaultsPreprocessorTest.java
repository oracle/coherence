/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.preprocessor;

import com.tangosol.coherence.config.xml.preprocessor.CacheDefaultsPreprocessor;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.DocumentElementPreprocessor;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Unit tests for {@link CacheDefaultsPreprocessor}s.
 *
 * @author bo  2011.12.16
 */
public class CacheDefaultsPreprocessorTest
    {
    /**
     * Ensure a {@link CacheDefaultsPreprocessor} resolves a simply declared default.
     *
     * @throws ConfigurationException should a problem occur processing defaults
     */
    @Test
    public void testSimpleDefaultResolution()
            throws ConfigurationException
        {
        String sXml = "<cache-config>" + "<defaults>" + "<serializer>pof</serializer>" + "</defaults>"
                      + "</cache-config>";
        XmlElement                xml = XmlHelper.loadXml(sXml);

        CacheDefaultsPreprocessor dpp = new CacheDefaultsPreprocessor("/defaults");

        dpp.addDefaultDefinition("/", "serializer");

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor().addElementPreprocessor(dpp);

        dep.preprocess(null, xml);

        Assert.assertTrue(XmlHelper.equalsElement(xml.findElement("/defaults/serializer"),
            xml.findElement("/serializer")));
        }

    /**
     * Ensure a {@link CacheDefaultsPreprocessor} does not resolve an element that's already defined.
     *
     * @throws ConfigurationException should a problem occur processing defaults
     */
    @Test
    public void testSkippingDefaultResolution()
            throws ConfigurationException
        {
        String sXml = "<cache-config>" + "<defaults>" + "<serializer>pof</serializer>" + "</defaults>"
                      + "<serializer>custom</serializer>" + "</cache-config>";
        XmlElement                xml = XmlHelper.loadXml(sXml);

        CacheDefaultsPreprocessor dpp = new CacheDefaultsPreprocessor("/defaults");

        dpp.addDefaultDefinition("/", "serializer");

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor().addElementPreprocessor(dpp);

        dep.preprocess(null, xml);

        Assert.assertEquals("custom", xml.findElement("/serializer").getString());
        }

    /**
     * Ensure a {@link CacheDefaultsPreprocessor} resolves a multiple defaults using absolute paths of the same name.
     *
     * @throws ConfigurationException should a problem occur processing defaults
     */
    @Test
    public void testMultipleSingleAbsoluteDefaultResolution()
            throws ConfigurationException
        {
        String sXml = "<cache-config>" + "<defaults>" + "<serializer>pof</serializer>" + "</defaults>"
                      + "<caching-schemes>" + "<distributed-scheme><scheme-name>A</scheme-name></distributed-scheme>"
                      + "<replicated-scheme><scheme-name>A</scheme-name></replicated-scheme>" + "</caching-schemes>"
                      + "</cache-config>";
        XmlElement                xml = XmlHelper.loadXml(sXml);

        CacheDefaultsPreprocessor dpp = new CacheDefaultsPreprocessor("/defaults");

        dpp.addDefaultDefinition("/caching-schemes/distributed-scheme", "serializer");
        dpp.addDefaultDefinition("/caching-schemes/replicated-scheme", "serializer");

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor().addElementPreprocessor(dpp);

        dep.preprocess(null, xml);

        Assert.assertTrue(XmlHelper.equalsElement(xml.findElement("/caching-schemes/distributed-scheme/serializer"),
            xml.findElement("/defaults/serializer")));
        Assert.assertTrue(XmlHelper.equalsElement(xml.findElement("/caching-schemes/replicated-scheme/serializer"),
            xml.findElement("/defaults/serializer")));
        }

    /**
     * Ensure a {@link CacheDefaultsPreprocessor} resolves a multiple defaults using relative paths of the same name.
     *
     * @throws ConfigurationException should a problem occur processing defaults
     */
    @Test
    public void testMultipleSingleRelativeDefaultResolution()
            throws ConfigurationException
        {
        String sXml = "<cache-config>" + "<defaults>" + "<serializer>pof</serializer>" + "</defaults>"
                      + "<caching-schemes>" + "<distributed-scheme><scheme-name>A</scheme-name></distributed-scheme>"
                      + "<replicated-scheme><scheme-name>A</scheme-name></replicated-scheme>"
                      + "<distributed-scheme><scheme-name>B</scheme-name></distributed-scheme>" + "</caching-schemes>"
                      + "</cache-config>";
        XmlElement                xml = XmlHelper.loadXml(sXml);

        CacheDefaultsPreprocessor dpp = new CacheDefaultsPreprocessor("/defaults");

        dpp.addDefaultDefinition("distributed-scheme", "serializer");
        dpp.addDefaultDefinition("replicated-scheme", "serializer");

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor().addElementPreprocessor(dpp);

        dep.preprocess(null, xml);

        String sResult =
            "<cache-config>" + "<defaults>" + "<serializer>pof</serializer>" + "</defaults>" + "<caching-schemes>"
            + "<distributed-scheme><scheme-name>A</scheme-name><serializer>pof</serializer></distributed-scheme>"
            + "<replicated-scheme><scheme-name>A</scheme-name><serializer>pof</serializer></replicated-scheme>"
            + "<distributed-scheme><scheme-name>B</scheme-name><serializer>pof</serializer></distributed-scheme>"
            + "</caching-schemes>" + "</cache-config>";

        Assert.assertTrue(XmlHelper.equalsElement(xml, XmlHelper.loadXml(sResult)));
        }

    /**
     * Ensure that multiple different types of defaults are resolved, including those with nested elements.
     *
     * @throws ConfigurationException should a problem occur processing defaults
     */
    @Test
    public void testMultipleDefaultResolution()
            throws ConfigurationException
        {
        String sXml = "<cache-config>" + "<defaults>" + "<serializer>pof</serializer>"
                      + "<mode><option>normal</option></mode>" + "</defaults>" + "<caching-schemes>"
                      + "<distributed-scheme><scheme-name>A</scheme-name></distributed-scheme>"
                      + "<replicated-scheme><scheme-name>A</scheme-name></replicated-scheme>" + "</caching-schemes>"
                      + "</cache-config>";

        XmlElement                xml = XmlHelper.loadXml(sXml);

        CacheDefaultsPreprocessor dpp = new CacheDefaultsPreprocessor("/defaults");

        dpp.addDefaultDefinition("/caching-schemes/distributed-scheme", "serializer");
        dpp.addDefaultDefinition("/caching-schemes/replicated-scheme", "serializer");
        dpp.addDefaultDefinition("/caching-schemes/distributed-scheme", "mode");
        dpp.addDefaultDefinition("/caching-schemes/replicated-scheme", "mode");
        dpp.addDefaultDefinition("/", "mode");

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor().addElementPreprocessor(dpp);

        dep.preprocess(null, xml);

        XmlElement xmlSerializer = xml.findElement("/defaults/serializer");
        XmlElement xmlMode       = xml.findElement("/defaults/mode");

        Assert.assertTrue(XmlHelper.equalsElement(xml.findElement("/caching-schemes/distributed-scheme/serializer"),
            xmlSerializer));
        Assert.assertTrue(XmlHelper.equalsElement(xml.findElement("/caching-schemes/replicated-scheme/serializer"),
            xmlSerializer));
        Assert.assertTrue(XmlHelper.equalsElement(xml.findElement("/caching-schemes/distributed-scheme/mode"),
            xmlMode));
        Assert.assertTrue(XmlHelper.equalsElement(xml.findElement("/caching-schemes/replicated-scheme/mode"), xmlMode));
        Assert.assertTrue(XmlHelper.equalsElement(xml.findElement("/mode"), xmlMode));
        Assert.assertNull(xml.findElement("/caching-schemes/mode"));
        }

    /**
     * Ensure a {@link CacheDefaultsPreprocessor} resolves a simply declared default.
     *
     * @throws ConfigurationException should a problem occur processing defaults
     */
    @Test
    public void testCustomClassResolution()
            throws ConfigurationException
        {
        String sXml = "<cache-config>" + "<defaults><serializer><class-scheme>A</class-scheme></serializer></defaults>"
                      + "<caching-schemes><distributed-scheme><scheme-name>A</scheme-name></distributed-scheme>"
                      + "<replicated-scheme><scheme-name>A</scheme-name></replicated-scheme>"
                      + "<distributed-scheme><scheme-name>B</scheme-name></distributed-scheme></caching-schemes>"
                      + "</cache-config>";

        XmlElement                xml = XmlHelper.loadXml(sXml);

        CacheDefaultsPreprocessor dpp = new CacheDefaultsPreprocessor("/defaults");

        dpp.addDefaultDefinition("distributed-scheme", "serializer");
        dpp.addDefaultDefinition("replicated-scheme", "serializer");

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor().addElementPreprocessor(dpp);

        dep.preprocess(null, xml);

        String sResult =
            "<cache-config>" + "<defaults>" + "<serializer><class-scheme>A</class-scheme></serializer>" + "</defaults>"
            + "<caching-schemes>"
            + "<distributed-scheme><scheme-name>A</scheme-name><serializer><class-scheme>A</class-scheme></serializer></distributed-scheme>"
            + "<replicated-scheme><scheme-name>A</scheme-name><serializer><class-scheme>A</class-scheme></serializer></replicated-scheme>"
            + "<distributed-scheme><scheme-name>B</scheme-name><serializer><class-scheme>A</class-scheme></serializer></distributed-scheme>"
            + "</caching-schemes>" + "</cache-config>";

        Assert.assertTrue(XmlHelper.equalsElement(xml, XmlHelper.loadXml(sResult)));
        }
    }
