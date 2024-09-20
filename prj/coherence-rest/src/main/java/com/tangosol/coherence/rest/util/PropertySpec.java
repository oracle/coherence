/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util;

import com.tangosol.coherence.dslquery.UniversalExtractorBuilder;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ValueExtractor;

import java.io.IOException;
import java.io.Serializable;

import static com.tangosol.util.extractor.AbstractExtractor.VALUE;

/**
 * Specification for a single property.
 *
 * @author as  2010.10.11
 */
public class PropertySpec
        implements Comparable<PropertySpec>, Serializable, PortableObject
    {

    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor (necessary for deserialization).
     */
    public PropertySpec()
        {
        }

    /**
     * Construct PropertySpec instance.
     *
     * @param sName  property name
     */
    public PropertySpec(String sName)
        {
        this(sName, null);
        }

    /**
     * Construct PropertySpec instance.
     *
     * @param sName        property name
     * @param propertySet  a set of nested properties
     */
    public PropertySpec(String sName, PropertySet propertySet)
        {
        m_sName       = sName;
        m_propertySet = propertySet;
        }

    // ---- factory methods -------------------------------------------------

    /**
     * Create a PropertySpec from its textual representation.
     *
     * @param sSpec  property specification as a string
     *
     * @return PropertySpec instance
     */
    public static PropertySpec fromString(String sSpec)
        {
        return PropertySet.fromString(sSpec).first();
        }

    // ---- methods ---------------------------------------------------------

    /**
     * Extract the value of this property from a target object.
     *
     * @param oTarget  object to extract the value from
     *
     * @return extracted property value, or <tt>null</tt> if specified
     *         property cannot be accessed
     */
    public Object getValue(Object oTarget)
        {
        try
            {
            return getExtractor().extract(oTarget);
            }
        catch (Exception e)
            {
            return null;
            }
        }

    // ----- Comparable implementation --------------------------------------

    /**
     * Compare this object to another.
     *
     * @param o  object to compare this object with
     *
     * @return negative integer if this object is less than a specified
     *         object, zero if they are equal, or positive integer if this
     *         object is greater than a specified object
     */
    public int compareTo(PropertySpec o)
        {
        return m_sName.compareTo(o.m_sName);
        }

    // ----- PortableObject implementation ----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader reader)
            throws IOException
        {
        m_sName       = reader.readString(0);
        m_propertySet = (PropertySet) reader.readObject(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeString(0, m_sName);
        writer.writeObject(1, m_propertySet);
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Test equality of this object with another.
     *
     * @param o  object to compare this object with
     *
     * @return <tt>true</tt> if this object is equal to the specified object
     */
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || !Base.equals(getClass(), o.getClass()))
            {
            return false;
            }

        PropertySpec that = (PropertySpec) o;
        return Base.equals(m_sName,       that.m_sName) &&
               Base.equals(m_propertySet, that.m_propertySet);
        }

    /**
     * Return hash code for this object.
     *
     * @return hash code
     */
    public int hashCode()
        {
        PropertySet propertySet = m_propertySet;

        int nResult = m_sName.hashCode();
        nResult = 31 * nResult + (propertySet == null ? 0 : propertySet.hashCode());
        return nResult;
        }

    /**
     * Return string representation of this object.
     *
     * @return this object as a String
     */
    public String toString()
        {
        return m_sName + (isPartial() ? ":(" + m_propertySet + ')' : "");
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the property name.
     *
     * @return the property name
     */
    public String getName()
        {
        return m_sName;
        }

    /**
     * Return the set of nested properties.
     *
     * @return nested properties
     */
    public PropertySet getPropertySet()
        {
        return m_propertySet;
        }

    /**
     * Return whether this is a partial property or not.
     *
     * @return <tt>true</tt> if this is a partial property, <tt>false</tt>
     *         otherwise
     */
    public boolean isPartial()
        {
        return m_propertySet != null;
        }

    /**
     * Return the partial class for this property.
     *
     * @return partial class for this property, or <tt>null</tt> if this is
     *         not a partial property
     */
    public Class getPartialClass()
        {
        return m_clzPartialClass;
        }

    /**
     * Set the partial class for this property.
     *
     * @param clzPartialClass  class of this property
     */
    public void setPartialClass(Class clzPartialClass)
        {
        m_clzPartialClass = clzPartialClass;
        }

    /**
     * Return extractor for this property.
     *
     * @return an extractor
     */
    protected ValueExtractor getExtractor()
        {
        ValueExtractor extractor = m_extractor;
        if (extractor == null)
            {
            m_extractor = extractor = new UniversalExtractorBuilder().realize("", VALUE, m_sName);
            }
        return extractor;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Property name.
     */
    protected String m_sName;

    /**
     * A set of nested properties.
     */
    protected PropertySet m_propertySet;

    /**
     * Property class (only for partial properties).
     */
    protected transient Class m_clzPartialClass;

    /**
     * An extractor that can extract value of this property from a target
     * object.
     */
    protected transient ValueExtractor m_extractor;
    }
