/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.run.xml;

/**
 * A {@link SimpleAttribute} provides a basic implementation of an {@link XmlAttribute}.
 *
 * @author bko 2011.06.14
 */
public class SimpleAttribute implements XmlAttribute
    {

    // ----- Constructors ---------------------------------------------------

    /**
     * Standard Constructor.
     *
     * @param xmlElement The {@link XmlElement} in which the {@link XmlAttribute} is defined.
     * @param sName      The name of the {@link XmlAttribute}
     * @param xmlValue   The value of the {@link XmlElement} represented as an {@link XmlValue}.
     */
    public SimpleAttribute(XmlElement xmlElement, String sName, XmlValue xmlValue)
        {
        m_xmlElement    = xmlElement;
        m_qualifiedName = new QualifiedName(sName);
        m_xmlValue      = xmlValue;
        }

    // ----- XmlAttribute Interface -----------------------------------------

    /**
     * Return the name of the {@link XmlAttribute}.
     *
     * @return A {@link String}
     */
    public String getName()
        {
        return m_qualifiedName.getName();
        }

    /**
     * Return the {@link QualifiedName} of the {@link XmlAttribute}.
     *
     * @return A {@link QualifiedName}
     */
    public QualifiedName getQualifiedName()
        {
        return m_qualifiedName;
        }

    /**
     * Return the {@link XmlElement} in which the {@link XmlAttribute} is defined.
     *
     * @return An {@link XmlElement}
     */
    public XmlElement getXmlElement()
        {
        return m_xmlElement;
        }

    /**
     * Return the {@link XmlValue} defined by the {@link XmlAttribute}.
     *
     * @return An {@link XmlValue}
     */
    public XmlValue getXmlValue()
        {
        return m_xmlValue;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link QualifiedName} of the {@link XmlAttribute}.
     */
    private QualifiedName m_qualifiedName;

    /**
     * The {@link XmlElement} in which the {@link XmlAttribute} is defined.
     */
    private XmlElement m_xmlElement;

    /**
     * The {@link XmlValue} of the {@link XmlAttribute}.
     */
    private XmlValue m_xmlValue;
    }
