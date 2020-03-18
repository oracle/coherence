/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.run.xml;

import java.util.UnknownFormatConversionException;

/**
 * A {@link QualifiedName} is a useful class to separately capture the xml namespace prefix
 * and local name of xml elements and attributes as a single object (instead of having to parse them
 * out of {@link String}s all the time).
 *
 * For example, the xmlName "movie:definition" has the namespace prefix "movie"
 * and the local name "definition".  If there is no namespace prefix declared, the prefix is
 * always returned as "".
 *
 * @author bko 2011.06.22
 */
public class QualifiedName
    {

    // ----- constructor(s) -------------------------------------------------

    /**
     * Standard Constructor.
     *
     * @param xmlName The name of an xml element/attribute from which to create a qualified name.
     *                Must be of the format "prefix:name" or simply "name" (in which case the prefix is
     *                considered "")
     *
     * @throws UnknownFormatConversionException When the specified xmlName is invalid (contains mulitple :'s)
     */
    public QualifiedName(String xmlName)
            throws UnknownFormatConversionException
        {
        String[] parts = xmlName.trim().split(":");

        if (parts.length == 1)
            {
            this.m_sPrefix    = "";
            this.m_sLocalName = xmlName.trim();
            }
        else if (parts.length == 2)
            {
            if (parts[0].equals("xmlns"))
                {
                // there is a special case for the xmlns element.
                // It could be xmlns:foo="uri" or xmlns="uri".
                // The element is actually xmlns and the prefix
                // is either foo or "".
                // In this case the prefix follows the element
                this.m_sLocalName = parts[0].trim();
                this.m_sPrefix    = parts[1].trim();
                }
            else
                {
                this.m_sPrefix    = parts[0].trim();
                this.m_sLocalName = parts[1].trim();
                }
            }
        else
            {
            throw new UnknownFormatConversionException(String.format("The specified xmlName [%s] can't be parsed into a QualifiedName",
                    xmlName));
            }
        }

    /**
     * Standard Constructor.
     *
     * @param xmlElement An {@link XmlElement} from which to return the {@link QualifiedName}
     *
     * @throws UnknownFormatConversionException When the specified xmlElement is invalid (contains mulitple :'s)
     */
    public QualifiedName(XmlElement xmlElement)
            throws UnknownFormatConversionException
        {
        this(xmlElement.getName());
        }

    /**
     * Standard Constructor (using a specified {@link QualifiedName} for the prefix).
     *
     * @param qualifiedName The {@link QualifiedName} on which to base the new {@link QualifiedName}.
     * @param localName     The local name for the new {@link QualifiedName}.
     */
    public QualifiedName(QualifiedName qualifiedName, String localName)
        {
        this(qualifiedName.getPrefix(), localName);
        }

    /**
     * Standard Constructor.
     *
     * @param prefix The xmlns prefix for the {@link QualifiedName}
     * @param localName The localname for the {@link QualifiedName}
     */
    public QualifiedName(String prefix, String localName)
        {
        this.m_sPrefix    = prefix.trim();
        this.m_sLocalName = localName.trim();
        }

    // ----- Object interface -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other)
        {
        return (other != null) && (other instanceof QualifiedName) && ((QualifiedName) other).m_sPrefix.equals(m_sPrefix)
               && ((QualifiedName) other).m_sLocalName.equals(m_sLocalName);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
        {
        final int prime  = 31;
        int       result = 1;

        result = prime * result + ((m_sLocalName == null)
                                   ? 0
                                   : m_sLocalName.hashCode());
        result = prime * result + ((m_sPrefix == null)
                                   ? 0
                                   : m_sPrefix.hashCode());

        return result;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return getName();
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the xml prefix of the {@link QualifiedName}.
     *
     * @return Returns "" if the name is not qualified with a namespace prefix
     */
    public String getPrefix()
        {
        return m_sPrefix;
        }

    /**
     * Returns the local name of the {@link QualifiedName}.
     *
     * @return Returns the local part of a qualified name.
     */
    public String getLocalName()
        {
        return m_sLocalName;
        }

    /**
     * Returns the entire qualified name, including the prefix and local name.
     *
     * @return A string containing the entire qualified name, including prefix
     *         and local name.
     */
    public String getName()
        {
        return hasPrefix()
               ? String.format("%s:%s", getPrefix(), getLocalName())
               : getLocalName();
        }

    /**
     * Returns boolean based on if the {@link QualifiedName} has a namespace prefix.
     *
     * @return <code>true</code> If the {@link QualifiedName} has an xmlns prefix
     */
    public boolean hasPrefix()
        {
        return m_sPrefix.length() > 0;
        }

    // ----- data fields ----------------------------------------------------

    /**
     * The localName of a {@link QualifiedName} is the name of the xml element/attribute with in
     * its namespace.
     */
    private String m_sLocalName;

    /**
     * The prefix of a {@link QualifiedName} is usually the namespace to which it belongs.
     */
    private String m_sPrefix;
    }
