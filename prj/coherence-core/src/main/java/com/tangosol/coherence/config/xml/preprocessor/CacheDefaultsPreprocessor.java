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

import com.tangosol.util.Base;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A {@link CacheDefaultsPreprocessor} is an {@link ElementPreprocessor} that introduces (via cloning) default xml
 * content for xml elements where the said content is missing.
 * <p>
 * Ultimately this {@link ElementPreprocessor} is designed to perform pre-processing of Coherence Cache &lt;defaults&gt;
 * declarations, inserting them into the appropriate places in cache-config.xml documents.
 *
 * @see OperationalDefaultsPreprocessor
 *
 * @author bo  2011.12.16
 * @since Coherence 12.1.2
 */
public class CacheDefaultsPreprocessor
        implements ElementPreprocessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link CacheDefaultsPreprocessor} with a specific path to where default element content can be located.
     * <p>
     * Example: <code>new DefaultPreprocessor("/defaults");</code>
     *
     * @param sDefaultsParentPath  the absolute path to the {@link XmlElement} that contains (is the parent of)
     *                             the default elements
     */
    public CacheDefaultsPreprocessor(String sDefaultsParentPath)
        {
        Base.azzert(sDefaultsParentPath != null && sDefaultsParentPath.startsWith("/"));

        // ensure the path to the defaults element does not end with a /
        m_sDefaultsParentPath = sDefaultsParentPath.trim();

        if (m_sDefaultsParentPath.endsWith("/") && m_sDefaultsParentPath.length() > 1)
            {
            m_sDefaultsParentPath = m_sDefaultsParentPath.substring(0, m_sDefaultsParentPath.length() - 1);
            }

        m_listDefaultDefinitions = new ArrayList<CacheDefaultsPreprocessor.DefaultDefinition>();
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
        // is this element with in the default element?
        // (this ensures that we don't attempt to perform default substitutions with in default element)
        String sElementPath = getPathToRoot(element);

        if (!sElementPath.startsWith(m_sDefaultsParentPath))
            {
            List<XmlElement> listElementChildren = (List<XmlElement>) element.getElementList();

            // add missing defaults to the current element (where required)
            for (DefaultDefinition defaultDefn : m_listDefaultDefinitions)
                {
                // does the current element match the path specified by the default definition?
                if (defaultDefn.matches(element))
                    {
                    // when the required element doesn't exist, clone a default one into place
                    if (element.getElement(defaultDefn.getRequiredElementName()) == null)
                        {
                        // locate the default element to clone
                        XmlElement defaultElement = element.findElement(m_sDefaultsParentPath + "/"
                                                        + defaultDefn.getRequiredElementName());

                        if (defaultElement == null)
                            {
                            // could not find the default, so nothing should happen
                            }
                        else if (listElementChildren.isEmpty() && !element.getString().isEmpty())
                            {
                            // we can't add the required element to an element with simple content (but no children)
                            }
                        else
                            {
                            // clone and add the default into the current element
                            XmlElement requiredElement = (XmlElement) defaultElement.clone();

                            listElementChildren.add(requiredElement);
                            }
                        }
                    }
                }

            }

        return false;
        }

    // ----- CacheDefaultsPreprocessor methods ------------------------------

    /**
     * Adds a requirement that the specified "default" element must be added to (if not already defined) in the element
     * specified by the defined parent path.
     * <p>
     * Paths used by this method are based those defined by the {@link XmlHelper#findElement(XmlElement, String)}
     * method, with the exception that ".." is not supported.
     * <p>
     * For example 1: The following call specifies that the "serializer" should be cloned from the defined default
     * path if it doesn't exist in the "/caching-schemes/distributed-scheme" element.
     * <p>
     * <code>addDefaultDefinition("/caching-schemes/distributed-scheme", "serializer");</code>
     * <p>
     * For example 2: The following call specifies that the "serializer" should be cloned from the defined default
     * path if it doesn't exist in any "distributed-scheme" elements
     * <p>
     * <code>addDefaultDefinition("distributed-scheme", "serializer");</code>
     *
     * @param sRequiredElementParentPath  the path of the parent for the required element
     * @param sRequiredElementName        the name of the element that should be cloned (if it doesn't exist in the
     *                                    specified parent path) from the defined default element path
     */
    public void addDefaultDefinition(String sRequiredElementParentPath, String sRequiredElementName)
        {
        m_listDefaultDefinitions.add(new DefaultDefinition(sRequiredElementParentPath, sRequiredElementName));
        }

    /**
     * Obtains the path to the root element given the specified element, but does not include the root element
     * name so that the value returned can be used by {@link XmlHelper#findElement(XmlElement, String)}.
     *
     * @param element  the element from which to produce the path
     * @return the path to the root element (excluding the root element name)
     */
    private String getPathToRoot(XmlElement element)
        {
        if (element == null)
            {
            return null;
            }
        else
            {
            StringBuilder builder = new StringBuilder();

            while (element.getParent() != null)
                {
                builder.insert(0, "/" + element.getName());
                element = element.getParent();
                }

            return builder.length() == 0 ? "/" : builder.toString();
            }
        }

    // ----- DefaultMapping class -------------------------------------------

    /**
     * A {@link DefaultDefinition} captures the definition of a required default element.
     */
    private static class DefaultDefinition
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link DefaultDefinition}.
         *
         * @param sRequiredElementParentPath  the path of the parent for the required element
         * @param sRequiredElementName        the name of the element that should be cloned (if it doesn't exist in the
         *                                    specified parent path) from the default element path
         */
        public DefaultDefinition(String sRequiredElementParentPath, String sRequiredElementName)
            {
            m_listRequiredElementParentNames = new ArrayList<String>(10);
            sRequiredElementParentPath       = sRequiredElementParentPath.trim();

            // for absolute paths, we add a "/" as the absolute parent
            if (sRequiredElementParentPath.startsWith("/"))
                {
                m_listRequiredElementParentNames.add("/");
                }

            // tokenize the parent element path into parent names
            StringTokenizer tokenizer = new StringTokenizer(sRequiredElementParentPath, "/");

            for (; tokenizer.hasMoreTokens(); )
                {
                m_listRequiredElementParentNames.add(tokenizer.nextToken());
                }

            m_sRequiredElementName = sRequiredElementName.trim();
            }

        // ----- DefaultMapping methods -------------------------------------

        /**
         * Obtains the required element name.
         *
         * @return the required element name
         */
        public String getRequiredElementName()
            {
            return m_sRequiredElementName;
            }

        /**
         * Determines if the specified {@link XmlElement} matches the path specified by the {@link DefaultDefinition}.
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
         * The name of the potentially required, but potentially missing element
         * (this will be appended to the parent path).
         */
        private String m_sRequiredElementName;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The path to the parent element of all default elements.
     */
    private String m_sDefaultsParentPath;

    /**
     * The {@link DefaultDefinition}s defined for the {@link CacheDefaultsPreprocessor}.
     */
    private ArrayList<DefaultDefinition> m_listDefaultDefinitions;
    }
