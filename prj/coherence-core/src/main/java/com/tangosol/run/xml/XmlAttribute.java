/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.run.xml;

/**
 * An {@link XmlAttribute} represents an Xml Attribute, defined within an
 * {@link XmlElement}.
 *
 * @author bko 2011.06.14
 */
public interface XmlAttribute
    {

    /**
     * Get the name of the {@link XmlAttribute}
     * <p>
     * Note: This is the full string of the {@link XmlAttribute} name. For
     * namespace support use {@link #getQualifiedName()}
     *
     * @return The {@link XmlAttribute} name.
     */
    public String getName();

    /**
     * Get the {@link QualifiedName} of the {@link XmlAttribute}.
     *
     * @return The {@link QualifiedName}.
     */
    public QualifiedName getQualifiedName();

    /**
     * Get the {@link XmlElement} in which the {@link XmlAttribute} is defined.
     *
     * @return An {@link XmlElement}
     */
    public XmlElement getXmlElement();

    /**
     * Get the {@link XmlValue} defined by thge {@link XmlAttribute}.
     *
     * @return An {@link XmlValue}
     */
    public XmlValue getXmlValue();
    }
