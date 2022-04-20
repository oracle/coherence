/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.preprocessor;

import com.tangosol.coherence.config.xml.preprocessor.OperationalDefaultsPreprocessor;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.DocumentElementPreprocessor;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Unit tests for {@link OperationalDefaultsPreprocessor}s.
 *
 * @author bo  2012.01.10
 */
public class OperationalDefaultsPreprocessorTest
    {
    /**
     * Ensure a {@link OperationalDefaultsPreprocessor} resolves a simply declared, missing default.
     *
     * @throws ConfigurationException should a problem occur processing defaults
     */
    @Test
    public void testSingleDefaultResolution()
            throws ConfigurationException
        {
        String                          sDefaults = "<defaults>" + "<one>one</one>" + "</defaults>";

        XmlElement                      defaults  = XmlHelper.loadXml(sDefaults);

        String                          sXml      = "<root></root>";
        XmlElement                      xml       = XmlHelper.loadXml(sXml);

        OperationalDefaultsPreprocessor odpp      = new OperationalDefaultsPreprocessor();

        odpp.addDefaultsDefinition("/", defaults);

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor().addElementPreprocessor(odpp);

        dep.preprocess(null, xml);

        Assert.assertTrue(XmlHelper.equalsElement(defaults.findElement("/one"), xml.findElement("/one")));
        }

    /**
     * Ensure a {@link OperationalDefaultsPreprocessor} resolves multiple missing defaults
     *
     * @throws ConfigurationException should a problem occur processing defaults
     */
    @Test
    public void testMultipleDefaultResolution()
            throws ConfigurationException
        {
        String sDefaults = "<defaults>" + "<one>one</one>" + "<two>two</two>" + "<three><four/></three>"
                           + "</defaults>";

        XmlElement                      defaults = XmlHelper.loadXml(sDefaults);

        String                          sXml     = "<root></root>";
        XmlElement                      xml      = XmlHelper.loadXml(sXml);

        OperationalDefaultsPreprocessor odpp     = new OperationalDefaultsPreprocessor();

        odpp.addDefaultsDefinition("/", defaults);

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor().addElementPreprocessor(odpp);

        dep.preprocess(null, xml);

        Assert.assertTrue(XmlHelper.equalsElement(defaults.findElement("/one"), xml.findElement("/one")));
        Assert.assertTrue(XmlHelper.equalsElement(defaults.findElement("/two"), xml.findElement("/two")));
        Assert.assertTrue(XmlHelper.equalsElement(defaults.findElement("/three"), xml.findElement("/three")));
        }

    /**
     * Ensure a {@link OperationalDefaultsPreprocessor} resolves skips existing definitions
     *
     * @throws ConfigurationException should a problem occur processing defaults
     */
    @Test
    public void testSkippingDefaultResolution()
            throws ConfigurationException
        {
        String sDefaults = "<defaults>" + "<one>one</one>" + "<two>two</two>" + "<three><four/></three>"
                           + "</defaults>";

        XmlElement                      defaults = XmlHelper.loadXml(sDefaults);

        String                          sXml     = "<root><one>1</one><two>2</two><three>3</three></root>";
        XmlElement                      xml      = XmlHelper.loadXml(sXml);

        OperationalDefaultsPreprocessor odpp     = new OperationalDefaultsPreprocessor();

        odpp.addDefaultsDefinition("/", defaults);

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor().addElementPreprocessor(odpp);

        dep.preprocess(null, xml);

        Assert.assertFalse(XmlHelper.equalsElement(defaults.findElement("/one"), xml.findElement("/one")));
        Assert.assertEquals("1", xml.findElement("/one").getString());

        Assert.assertFalse(XmlHelper.equalsElement(defaults.findElement("/two"), xml.findElement("/two")));
        Assert.assertEquals("2", xml.findElement("/two").getString());

        Assert.assertFalse(XmlHelper.equalsElement(defaults.findElement("/three"), xml.findElement("/three")));
        Assert.assertEquals("3", xml.findElement("/three").getString());
        }

    /**
     * Ensure a {@link OperationalDefaultsPreprocessor} resolves multiple missing defaults (using relative paths)
     *
     * @throws ConfigurationException should a problem occur processing defaults
     */
    @Test
    public void testMultipleRelativePathDefaultResolution()
            throws ConfigurationException
        {
        String sDefaults = "<defaults>" + "<one>one</one>" + "<two>two</two>" + "<three><four/></three>"
                           + "</defaults>";

        XmlElement                      defaults = XmlHelper.loadXml(sDefaults);

        String                          sXml     = "<root><child/><grand-child><child/></grand-child></root>";
        XmlElement                      xml      = XmlHelper.loadXml(sXml);

        OperationalDefaultsPreprocessor odpp     = new OperationalDefaultsPreprocessor();

        odpp.addDefaultsDefinition("child", defaults);

        DocumentElementPreprocessor dep = new DocumentElementPreprocessor().addElementPreprocessor(odpp);

        dep.preprocess(null, xml);

        System.out.println(xml);

        Assert.assertTrue(XmlHelper.equalsElement(defaults.findElement("/one"), xml.findElement("/child/one")));
        Assert.assertTrue(XmlHelper.equalsElement(defaults.findElement("/two"), xml.findElement("/child/two")));
        Assert.assertTrue(XmlHelper.equalsElement(defaults.findElement("/three"), xml.findElement("/child/three")));

        Assert.assertTrue(XmlHelper.equalsElement(defaults.findElement("/one"),
            xml.findElement("/grand-child/child/one")));
        Assert.assertTrue(XmlHelper.equalsElement(defaults.findElement("/two"),
            xml.findElement("/grand-child/child/two")));
        Assert.assertTrue(XmlHelper.equalsElement(defaults.findElement("/three"),
            xml.findElement("/grand-child/child/three")));
        }
    }
