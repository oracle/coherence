/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.preprocessor;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.DocumentElementPreprocessor.ElementPreprocessor;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A {@link OperationalDefaultsPreprocessor} is an {@link ElementPreprocessor} that introduces
 * (via cloning) default xml content for xml elements where the said content is missing.
 * <p>
 * Ultimately this {@link ElementPreprocessor} is designed to perform pre-processing of Coherence cache-config.xml
 * files to inject appropriate Operational Config elements defined separately in tangosol-coherence.*.xml files.
 *
 * @author bo  2012.01.10
 * @since Coherence 12.1.2
 */
public class OperationalDefaultsPreprocessor
        implements ElementPreprocessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link OperationalDefaultsPreprocessor}.
     */
    public OperationalDefaultsPreprocessor()
        {
        m_listDefaultsDefinitions = new ArrayList<OperationalDefaultsPreprocessor.DefaultsDefinition>();
        }

    // ----- ElementPreprocessor methods ------------------------------------

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean preprocess(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        // add missing defaults to the current element (where required)
        for (DefaultsDefinition defaultDefn : m_listDefaultsDefinitions)
            {
            // does the default definition path match the current element (if so we may have to clone in defaults)
            if (defaultDefn.matches(element))
                {
                // ensure each of the children of the default are in the element and clone/add those that aren't
                XmlElement       defaults            = defaultDefn.getDefaultsElement();

                List<XmlElement> listDefaultChildren = (List<XmlElement>) defaults.getElementList();

                for (XmlElement defaultChild : listDefaultChildren)
                    {
                    // when the required child is missing, clone and add one from the defaults
                    if (element.getElement(defaultChild.getName()) == null)
                        {
                        XmlElement child = (XmlElement) defaultChild.clone();

                        element.getElementList().add(child);
                        }
                    }
                }
            }

        return false;
        }

    // ----- OperationalDefaultsPreprocessor methods -----------------------------------

    /**
     * Defines that elements matching the specified path must contain the child elements defined by the default element.
     * If not the missing children must be cloned from the default element into the element matching the path during
     * pre-processing.
     * <p>
     * Paths used by this method are based those defined by the {@link XmlHelper#findElement(XmlElement, String)}
     * method, with the exception that ".." is not supported.
     * <p>
     * For example 1: The following specifies that the elements matching the absolute path
     * "/caching-schemes/distributed-scheme" must contain the children defined by the xmlDistributedSchemeDefaults
     * element.  If not, they must be cloned into place.
     * <p>
     * <code>addDefaultsDefinition("/caching-schemes/distributed-scheme", xmlDistributedSchemeDefaults);</code>
     * <p>
     * For example 2: The following specifies that the elements matching the relative path
     * "distributed-scheme" must contain the children defined by the xmlDistributedSchemeDefaults element.
     * If not, they must be cloned into place.
     * <p>
     * <code>addDefaultsDefinition("distributed-scheme", xmlDistributedSchemeDefaults);</code>
     *
     * @param sPath            The path of elements requiring the defaults
     * @param defaultsElement  The xml element containing the required defaults for the specified path
     */
    public void addDefaultsDefinition(String sPath, XmlElement defaultsElement)
        {
        m_listDefaultsDefinitions.add(new DefaultsDefinition(sPath, defaultsElement));
        }

    // ----- DefaultsDefinition class ---------------------------------------

    /**
     * A {@link DefaultsDefinition} captures the definition of defaults for a specific path
     */
    private static class DefaultsDefinition
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link DefaultsDefinition}.
         *
         * @param sPath            The path of elements requiring the defaults
         * @param defaultsElement  The xml element containing the required defaults for the specified path
         */
        public DefaultsDefinition(String sPath, XmlElement defaultsElement)
            {
            m_listRequiredElementParentNames = new ArrayList<String>(10);
            sPath                            = sPath.trim();

            // for absolute paths, we add a "/" as the absolute parent
            if (sPath.startsWith("/"))
                {
                m_listRequiredElementParentNames.add("/");
                }

            // tokenize the parent element path into parent names
            StringTokenizer tokenizer = new StringTokenizer(sPath, "/");

            for (; tokenizer.hasMoreTokens(); )
                {
                m_listRequiredElementParentNames.add(tokenizer.nextToken());
                }

            m_xmlDefaultsElement = defaultsElement;
            }

        // ----- DefaultsDefinition methods ---------------------------------

        /**
         * Obtains the {@link XmlElement} containing the required default (child) elements.
         *
         * @return the default {@link XmlElement}
         */
        public XmlElement getDefaultsElement()
            {
            return m_xmlDefaultsElement;
            }

        /**
         * Determines if the specified {@link XmlElement} matches the path specified by the {@link DefaultsDefinition}.
         *
         * @param element  the {@link XmlElement} for comparison.
         *
         * @return if the {@link XmlElement} matches the path
         */
        public boolean matches(XmlElement element)
            {
            // ensure that the specified element has the same path as that defined by the mapping
            // (assume it does to start)
            boolean fPathMatches = true;

            for (int i = m_listRequiredElementParentNames.size() - 1; i >= 0 && fPathMatches; i--)
                {
                String sParentName = m_listRequiredElementParentNames.get(i);

                if (element != null && sParentName.equals("/"))
                    {
                    fPathMatches = element.getParent() == null;
                    }
                else if (element != null && element.getName().equals(sParentName))
                    {
                    element = element.getParent();
                    }
                else
                    {
                    fPathMatches = false;
                    }
                }

            return fPathMatches;
            }

        // ----- data members ---------------------------------------------------

        /**
         * The parent element names of the required element.
         */
        private ArrayList<String> m_listRequiredElementParentNames;

        /**
         * The parent element containing the defaults to be cloned into the above specified parent
         */
        private XmlElement m_xmlDefaultsElement;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link DefaultsDefinition}s defined for the {@link OperationalDefaultsPreprocessor}.
     */
    private ArrayList<DefaultsDefinition> m_listDefaultsDefinitions;
    }
