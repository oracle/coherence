/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.preprocessor;

import com.tangosol.coherence.config.xml.preprocessor.SchemeRefPreprocessor;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.DocumentElementPreprocessor;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Unit tests for {@link SchemeRefPreprocessor}s.
 *
 * @author bo  2011.07.29
 */
public class SchemeRefPreprocessorTest
    {
//  THIS TEST IS COMMENTED OUT UNTIL <null-scheme-ref/> is resovled correctly    
//     /**
//      * Ensure the {@link SchemeRefPreprocessor} fails to resolve a <scheme-ref> when the referenced scheme is undefined.
//      *
//      * @throws ConfigurationException
//      */
//     @Test(expected = ConfigurationException.class)
//     public void testFailingToResolveUndefinedScheme()
//             throws ConfigurationException
//         {
//         String sXml = "<cache-config><caching-schemes>"
//                       + "<my-scheme><scheme-ref>some-undefined-scheme</scheme-ref></my-scheme>"
//                       + "</caching-schemes></cache-config>";
//         XmlElement xml = XmlHelper.loadXml(sXml);
//
//         Dependencies dep =
//             new DocumentProcessor.DefaultDependencies().addElementPreprocessor(SchemeRefPreprocessor.INSTANCE);
//
//         DefaultProcessingContext ctx = new DefaultProcessingContext(dep);
//
//         ctx.preprocess(xml);
//         }

    /**
     * Ensure the {@link SchemeRefPreprocessor} fails to resolve a <scheme-ref> when the referenced scheme is empty.
     *
     * @throws ConfigurationException
     */
    @Test(expected = ConfigurationException.class)
    public void testFailingToResolveEmptyScheme()
            throws ConfigurationException
        {
        String sXml = "<cache-config><caching-schemes>" + "<my-scheme><scheme-ref></scheme-ref></my-scheme>"
                      + "</caching-schemes></cache-config>";
        XmlElement xml = XmlHelper.loadXml(sXml);

        DocumentElementPreprocessor dep =
            new DocumentElementPreprocessor().addElementPreprocessor(SchemeRefPreprocessor.INSTANCE);

        dep.preprocess(null, xml);
        }

    /**
     * Ensure the {@link SchemeRefPreprocessor} fails to resolve a <scheme-ref> when the referenced scheme is of a different type.
     *
     * @throws ConfigurationException
     */
    @Test(expected = ConfigurationException.class)
    public void testFailingToResolveDifferentScheme()
            throws ConfigurationException
        {
        String sXml = "<cache-config><caching-schemes>"
                      + "<my-scheme><scheme-name>one</scheme-name><scheme-ref>two</scheme-ref></my-scheme>"
                      + "<other-scheme><scheme-name>two</scheme-name></other-scheme>"
                      + "</caching-schemes></cache-config>";
        XmlElement xml = XmlHelper.loadXml(sXml);

        DocumentElementPreprocessor dep =
            new DocumentElementPreprocessor().addElementPreprocessor(SchemeRefPreprocessor.INSTANCE);

        dep.preprocess(null, xml);
        }

    /**
     * Ensure the {@link SchemeRefPreprocessor} fails to resolve a scheme that references itself.
     *
     * @throws ConfigurationException
     */
    @Test(expected = ConfigurationException.class)
    public void testFailingToResolveSelfReferencingScheme()
            throws ConfigurationException
        {
        String sXml = "<cache-config><caching-schemes>"
                      + "<my-scheme><scheme-name>one</scheme-name><scheme-ref>one</scheme-ref></my-scheme>"
                      + "</caching-schemes></cache-config>";
        XmlElement xml = XmlHelper.loadXml(sXml);

        DocumentElementPreprocessor dep =
            new DocumentElementPreprocessor().addElementPreprocessor(SchemeRefPreprocessor.INSTANCE);

        dep.preprocess(null, xml);
        }

    /**
     * Ensure the {@link SchemeRefPreprocessor} fails to resolve a scheme that references itself transitively.
     *
     * @throws ConfigurationException
     */
    @Test(expected = ConfigurationException.class)
    public void testFailingToResolveCyclicReferences()
            throws ConfigurationException
        {
        String sXml = "<cache-config><caching-schemes>"
                      + "<my-scheme><scheme-name>one</scheme-name><scheme-ref>two</scheme-ref></my-scheme>"
                      + "<my-scheme><scheme-name>two</scheme-name><scheme-ref>one</scheme-ref></my-scheme>"
                      + "</caching-schemes></cache-config>";
        XmlElement xml = XmlHelper.loadXml(sXml);

        DocumentElementPreprocessor dep =
            new DocumentElementPreprocessor().addElementPreprocessor(SchemeRefPreprocessor.INSTANCE);

        dep.preprocess(null, xml);
        }

    /**
     * Ensure the {@link SchemeRefPreprocessor} resolves a simple <scheme-ref> (non-transitive).
     *
     * @throws ConfigurationException
     */
    @Test
    public void testResolveSimpleSchemeReferences()
            throws ConfigurationException
        {
        String sXml =
            "<cache-config><caching-schemes>"
            + "<my-scheme><scheme-name>one</scheme-name><scheme-ref>two</scheme-ref><base-element>one</base-element><one-element/></my-scheme>"
            + "<my-scheme><scheme-name>two</scheme-name><base-element>two</base-element><two-element/></my-scheme>"
            + "</caching-schemes></cache-config>";
        XmlElement xml = XmlHelper.loadXml(sXml);

        DocumentElementPreprocessor dep =
            new DocumentElementPreprocessor().addElementPreprocessor(SchemeRefPreprocessor.INSTANCE);

        dep.preprocess(null, xml);

        XmlElement xmlOne = SchemeRefPreprocessor.INSTANCE.findCachingScheme("one", xml);

        Assert.assertNotNull(xmlOne);
        Assert.assertNull(xmlOne.getElement("scheme-ref"));
        Assert.assertEquals("one", xmlOne.getElement("base-element").getString());
        Assert.assertNotNull(xmlOne.getElement("one-element"));
        Assert.assertNotNull(xmlOne.getElement("two-element"));
        }

    /**
     * Ensure the {@link SchemeRefPreprocessor} resolves transitive <scheme-ref>s.
     *
     * @throws ConfigurationException
     */
    @Test
    public void testResolveTransitiveSchemeReferences()
            throws ConfigurationException
        {
        String sXml =
            "<cache-config><caching-schemes>"
            + "<my-scheme><scheme-name>one</scheme-name><scheme-ref>two</scheme-ref><base-element>one</base-element><one-element/></my-scheme>"
            + "<my-scheme><scheme-name>two</scheme-name><scheme-ref>three</scheme-ref><base-element>two</base-element><two-element/></my-scheme>"
            + "<my-scheme><scheme-name>three</scheme-name><base-element>three</base-element><three-element/></my-scheme>"
            + "</caching-schemes></cache-config>";
        XmlElement xml = XmlHelper.loadXml(sXml);

        DocumentElementPreprocessor dep =
            new DocumentElementPreprocessor().addElementPreprocessor(SchemeRefPreprocessor.INSTANCE);

        dep.preprocess(null, xml);

        XmlElement xmlOne = SchemeRefPreprocessor.INSTANCE.findCachingScheme("one", xml);

        Assert.assertNotNull(xmlOne);
        Assert.assertNull(xmlOne.getElement("scheme-ref"));
        Assert.assertEquals("one", xmlOne.getElement("base-element").getString());
        Assert.assertNotNull(xmlOne.getElement("one-element"));
        Assert.assertNotNull(xmlOne.getElement("two-element"));
        Assert.assertNotNull(xmlOne.getElement("three-element"));

        XmlElement xmlTwo = SchemeRefPreprocessor.INSTANCE.findCachingScheme("two", xml);

        Assert.assertNotNull(xmlTwo);
        Assert.assertNull(xmlTwo.getElement("scheme-ref"));
        Assert.assertEquals("two", xmlTwo.getElement("base-element").getString());
        Assert.assertNull(xmlTwo.getElement("one-element"));
        Assert.assertNotNull(xmlTwo.getElement("two-element"));
        Assert.assertNotNull(xmlTwo.getElement("three-element"));

        XmlElement xmlThree = SchemeRefPreprocessor.INSTANCE.findCachingScheme("three", xml);

        Assert.assertNotNull(xmlThree);
        Assert.assertNull(xmlThree.getElement("scheme-ref"));
        Assert.assertEquals("three", xmlThree.getElement("base-element").getString());
        Assert.assertNull(xmlThree.getElement("one-element"));
        Assert.assertNull(xmlThree.getElement("two-element"));
        Assert.assertNotNull(xmlTwo.getElement("three-element"));
        }
    }
