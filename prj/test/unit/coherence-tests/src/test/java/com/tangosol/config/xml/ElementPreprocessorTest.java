/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.DocumentElementPreprocessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.DocumentElementPreprocessor.ElementPreprocessor;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import junit.framework.Assert;

import org.junit.Test;

import java.util.List;

/**
 * Unit tests for {@link DocumentElementPreprocessor} {@link ElementPreprocessor}.
 *
 * @author bo  2011.07.29
 */
public class ElementPreprocessorTest
    {
    /**
     * Ensure that we can pre-process a simple {@link XmlDocument} and
     * convert all content to uppercase.
     *
     * @throws ConfigurationException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSimpleElementVisitor()
            throws ConfigurationException
        {
        DocumentElementPreprocessor dep = new DocumentElementPreprocessor();

        dep.addElementPreprocessor(new ElementPreprocessor()
            {
            @Override
            public boolean preprocess(ProcessingContext context, XmlElement element)
                    throws ConfigurationException
                {
                if (element.isContent())
                    {
                    String sContext = element.getString().toUpperCase();

                    element.setString(sContext);
                    }

                return false;
                }


            });

        String     sXml = "<element><element>a</element><element>b</element></element>";
        XmlElement xml  = XmlHelper.loadXml(sXml);

        dep.preprocess(null, xml);

        List<XmlElement> lstElement = (List<XmlElement>) xml.getElementList();

        Assert.assertEquals("A", lstElement.get(0).getString());
        Assert.assertEquals("B", lstElement.get(1).getString());
        }
    }
