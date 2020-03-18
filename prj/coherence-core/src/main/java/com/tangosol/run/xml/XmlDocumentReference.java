/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.run.xml;


import java.net.URI;

/**
 * An {@link XmlDocumentReference} provides an mechanism to reference an {@link XmlDocument}.
 *
 * @author Brian Oliver
 */
public class XmlDocumentReference
    {

    /**
     * Standard Constructor.
     *
     * @param sXmlDocument A {@link String} representation of the {@link XmlDocument}.
     */
    public XmlDocumentReference(String sXmlDocument)
        {
        m_sXmlDocument   = sXmlDocument;
        m_uriXmlDocument = null;
        }

    /**
     * Standard Constructor.
     *
     * @param uriXmlDocument {@link URI} of the {@link XmlDocument}.
     */
    public XmlDocumentReference(URI uriXmlDocument)
        {
        m_sXmlDocument   = null;
        m_uriXmlDocument = uriXmlDocument;
        }

    /**
     * Retrieves a <strong>copy</strong> of the {@link XmlDocument} specified by the {@link XmlDocumentReference}.
     *
     * @param classLoader The {@link ClassLoader} to use for locating and loading the {@link XmlDocument} if necessary.
     * @return An {@link XmlDocument}.
     */
    public XmlDocument getXmlDocument(ClassLoader classLoader)
        {
        if (m_uriXmlDocument == null)
            {
            return XmlHelper.loadXml(m_sXmlDocument);
            }
        else
            {
            String location = m_uriXmlDocument.toString();

            return XmlHelper.loadFileOrResource(location, location, (classLoader == null)
                    ? this.getClass().getClassLoader()
                    : classLoader);
            }
        }

    /**
     * Retrieves a <strong>copy</strong> of the {@link XmlDocument} specified by the {@link XmlDocumentReference}.
     *
     * @return An {@link XmlDocument}.
     */
    public XmlDocument getXmlDocument()
        {
        return getXmlDocument(this.getClass().getClassLoader());
        }

    /**
     * A {@link String} containing the {@link XmlDocument}. This will be <code>null</code> if an
     * {@link URI} is used to initialize the {@link XmlDocumentReference}.
     */
    private String m_sXmlDocument;

    /**
     * The {@link URI} of the {@link XmlDocument}. This will be <code>null</code> if an
     * xml {@link String} is used to initialize the {@link XmlDocumentReference}.
     */
    private URI m_uriXmlDocument;
    }
