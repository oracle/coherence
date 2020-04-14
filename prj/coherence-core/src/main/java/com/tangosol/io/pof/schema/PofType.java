/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.schema;

import com.oracle.coherence.common.schema.AbstractCanonicalType;
import com.oracle.coherence.common.schema.ExtensibleProperty;
import com.oracle.coherence.common.schema.ExtensibleType;
import com.oracle.coherence.common.schema.PropertyAware;

import java.util.Collections;
import java.util.List;

/**
 * Representation of POF type metadata.
 *
 * @author as  2013.06.21
 */
public class PofType
        extends AbstractCanonicalType<PofProperty>
        implements PropertyAware
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code PofType} instance.
     *
     * @param parent  the type to add POF metadata to
     */
    public PofType(ExtensibleType parent)
        {
        super(parent);
        }

    // ---- accessors -------------------------------------------------------

    /**
     * Return the type ID.
     *
     * @return the type ID
     */
    public int getId()
        {
        return m_nId;
        }

    /**
     * Set the type ID.
     *
     * @param nId  the type ID
     */
    public void setId(int nId)
        {
        m_nId = nId;
        }

    /**
     * Return the type version.
     *
     * @return the type version
     */
    public int getVersion()
        {
        return m_nVersion;
        }

    /**
     * Set the type version.
     *
     * @param nVersion  the type version
     */
    public void setVersion(int nVersion)
        {
        m_nVersion = nVersion;
        }

    /**
     * Return the property with the specified name.
     *
     * @param propertyName  property name
     *
     * @return the property with the specified name
     */
    public PofProperty getProperty(String propertyName)
        {
        return super.getProperty(propertyName);
        }

    /**
     * Return all properties.
     *
     * @return all properties
     */
    public List<PofProperty> getProperties()
        {
        List<PofProperty> properties = super.getProperties();
        Collections.sort(properties);
        return properties;
        }

    // ---- PropertyAware interface -----------------------------------------

    @Override
    public void propertyAdded(ExtensibleProperty property)
        {
        }

    // ---- data members ----------------------------------------------------

    /**
     * The type ID.
     */
    private int m_nId;

    /**
     * The type version.
     */
    private int m_nVersion;
    }
