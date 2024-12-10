/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.preprocessor;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.DocumentElementPreprocessor.ElementPreprocessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlElement;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link SchemeRefPreprocessor} is an {@link ElementPreprocessor} that resolves declarations of
 * &lt;scheme-ref&gt; as required by Coherence.
 *
 * @author bo  2011.08.03
 * @since Coherence 12.1.2
 */
public class SchemeRefPreprocessor implements ElementPreprocessor
    {
    // ----- ElementPreprocessor methods ------------------------------------

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean preprocess(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        XmlElement xmlSchemeRef   = element.getElement("scheme-ref");

        if (xmlSchemeRef == null)
            {
            return false;
            }
        else
            {
            String sSchemeName = xmlSchemeRef.getString().trim();

            if (sSchemeName.isEmpty())
                {
                throw new ConfigurationException(String.format("The referenced scheme in %s is blank.", element),
                    "Please ensure that the referenced scheme name is declared in the configuration");
                }
            else
                {
                // find the <*-scheme> with in the <caching-schemes> element that has the required scheme name
                XmlElement xmlScheme = findCachingScheme(sSchemeName, element);

                if (xmlScheme == null)
                    {
                    throw new ConfigurationException(String.format(
                            "The scheme-ref [%s] mentioned in cache configuration does not exist.", sSchemeName),
                            String.format("Please ensure that the scheme-ref [%s] is declared in the configuration.", sSchemeName));
                    }

                if (xmlScheme.getName().equals(element.getName()))
                    {
                    // ensure we don't have a cyclic reference (by ensuring we're not referencing ourself)
                    if (element == xmlScheme)
                        {
                        throw new ConfigurationException(String.format(
                            "Discovered a cyclic reference to scheme [%s] in %s.", xmlSchemeRef.getString(),
                            element), "Please ensure that the referenced scheme won't eventually reference itself.");
                        }

                    // remove the <scheme-ref> from element
                    List<XmlElement> listElements = (element.getElementList());

                    listElements.remove(xmlSchemeRef);

                    // perform the merge by adding the children of the referenced scheme into this element
                    // iff they don't already exist in the element
                    boolean fChanged = false;

                    for (XmlElement xmlSchemeChild : ((List<XmlElement>) xmlScheme.getElementList()))
                        {
                        if (element.getElement(xmlSchemeChild.getName()) == null)
                            {
                            listElements.add(xmlSchemeChild);
                            fChanged = true;
                            }
                        }

                    // if we changed the element we need to re-visit it (this allows for chained <scheme-ref>)
                    return fChanged;
                    }
                else
                    {
                    throw new ConfigurationException(String.format(
                        "The referenced scheme [%s] in %s is a different type of scheme.", xmlSchemeRef.getString(),
                        element), "Please ensure that the referenced scheme is the same type");
                    }
                }
            }
        }

    // ----- SchemeRefPreprocessor methods ----------------------------------

    /**
     * Obtains the {@link XmlElement} that contains a &lt;scheme-name&gt; definition for the specified sSchemeName with in
     * the provided {@link XmlElement}, or <code>null</code> if not found.
     *
     * @param sSchemeName  The scheme name to locate
     * @param element      The {@link XmlElement} to search
     *
     * @return the caching scheme xml element
     */
    @SuppressWarnings("unchecked")
    public XmlElement findCachingScheme(String sSchemeName, XmlElement element)
        {
        XmlElement       xmlSchemes        = element.getRoot().getElement("schemes");
        XmlElement       xmlCachingSchemes = element.getRoot().getElement("caching-schemes");
        List<XmlElement> elements          = new ArrayList<>();

        if (xmlSchemes != null)
            {
            elements.addAll(xmlSchemes.getElementList());
            }

        if (xmlCachingSchemes != null)
            {
            elements.addAll(xmlCachingSchemes.getElementList());
            }

        for (XmlElement xml : elements)
            {
            if (xml.getSafeElement("scheme-name").getString().trim().equals(sSchemeName))
                {
                return xml;
                }
            }

        return null;
        }

    // ----- constants ------------------------------------------------------

    /**
     * This singleton instance of the {@link SchemeRefPreprocessor}.
     */
    public static final SchemeRefPreprocessor INSTANCE = new SchemeRefPreprocessor();
    }
