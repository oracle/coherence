/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.schema;

import com.oracle.coherence.common.schema.AbstractCanonicalProperty;
import com.oracle.coherence.common.schema.ExtensibleProperty;

/**
 * Representation of POF property metadata.
 *
 * @author as  2013.07.11
 */
public class PofProperty
        extends AbstractCanonicalProperty
        implements Comparable<PofProperty>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code PofProperty} instance.
     *
     * @param parent  the property to add POF metadata to
     */
    public PofProperty(ExtensibleProperty parent)
        {
        super(parent);
        }

    // ---- accessors -------------------------------------------------------

    /**
     * Return the property name.
     *
     * @return the property name
     */
    @Override
    public String getName()
        {
        return m_sName == null ? super.getName() : m_sName;
        }

    /**
     * Set the property name.
     *
     * @param name  property name
     */
    public void setName(String name)
        {
        m_sName = name;
        }

    /**
     * Return the class version the property was introduced in.
     *
     * @return the class version the property was introduced in
     */
    public int getSince()
        {
        return m_nSince;
        }

    /**
     * Set the class version the property was introduced in.
     *
     * @param since  the class version the property was introduced in
     */
    public void setSince(int since)
        {
        m_nSince = since;
        }

    /**
     * Return the property order in the POF stream.
     *
     * @return the property order in the POF stream
     */
    public int getOrder()
        {
        return m_nOrder;
        }

    /**
     * Set the property order in the POF stream.
     *
     * @param nOrder  the property order in the POF stream
     */
    public void setOrder(int nOrder)
        {
        m_nOrder = nOrder;
        }

    /**
     * Return the additional type info.
     *
     * @return the additional type info
     */
    public Object getInfo()
        {
        return m_info;
        }

    /**
     * Set the additional type info.
     *
     * @param info  the additional type info
     */
    public void setInfo(Object info)
        {
        m_info = info;
        }

    /**
     * Return whether this property represents an array.
     *
     * @return whether this property represents an array
     */
    public boolean isArray()
        {
        return m_info instanceof PofArray;
        }

    /**
     * Return whether this property represents a date/time value.
     *
     * @return whether this property represents a date/time value
     */
    public boolean isDate()
        {
        return m_info instanceof PofDate;
        }

    /**
     * Return whether this property represents a collection.
     *
     * @return whether this property represents a collection
     */
    public boolean isCollection()
        {
        return m_info instanceof PofCollection;
        }

    /**
     * Return whether this property represents a list.
     *
     * @return whether this property represents a list
     */
    public boolean isList()
        {
        return m_info instanceof PofList;
        }

    /**
     * Return whether this property represents a set.
     *
     * @return whether this property represents a set
     */
    public boolean isSet()
        {
        return m_info instanceof PofSet;
        }

    /**
     * Return whether this property represents a map.
     *
     * @return whether this property represents a map
     */
    public boolean isMap()
        {
        return m_info instanceof PofMap;
        }

    /**
     * Return the additional type info as {@link PofArray}.
     *
     * @return the additional type info as {@code PofArray}
     *
     * @throws IllegalStateException if this property does not represent a {@code PofArray}
     */
    public PofArray asArray()
        {
        if (isArray())
            {
            return (PofArray) m_info;
            }
        throw new IllegalStateException("not a PofArray");
        }

    /**
     * Return the additional type info as {@link PofDate}.
     *
     * @return the additional type info as {@code PofDate}
     *
     * @throws IllegalStateException if this property does not represent a {@code PofDate}
     */
    public PofDate asDate()
        {
        if (isDate())
            {
            return (PofDate) m_info;
            }
        throw new IllegalStateException("not a PofDate");
        }

    /**
     * Return the additional type info as {@link PofCollection}.
     *
     * @return the additional type info as {@code PofCollection}
     *
     * @throws IllegalStateException if this property does not represent a {@code PofCollection}
     */
    public PofCollection asCollection()
        {
        if (isCollection())
            {
            return (PofCollection) m_info;
            }
        throw new IllegalStateException("not a PofCollection");
        }

    /**
     * Return the additional type info as {@link PofList}.
     *
     * @return the additional type info as {@code PofList}
     *
     * @throws IllegalStateException if this property does not represent a {@code PofList}
     */
    public PofList asList()
        {
        if (isList())
            {
            return (PofList) m_info;
            }
        throw new IllegalStateException("not a PofList");
        }

    /**
     * Return the additional type info as {@link PofSet}.
     *
     * @return the additional type info as {@code PofSet}
     *
     * @throws IllegalStateException if this property does not represent a {@code PofSet}
     */
    public PofSet asSet()
        {
        if (isSet())
            {
            return (PofSet) m_info;
            }
        throw new IllegalStateException("not a PofSet");
        }

    /**
     * Return the additional type info as {@link PofMap}.
     *
     * @return the additional type info as {@code PofMap}
     *
     * @throws IllegalStateException if this property does not represent a {@code PofMap}
     */
    public PofMap asMap()
        {
        if (isMap())
            {
            return (PofMap) m_info;
            }
        throw new IllegalStateException("not a PofMap");
        }

    // ---- Comparable interface --------------------------------------------

    @Override
    public int compareTo(PofProperty other)
        {
        int cmp = Integer.compare(getSince(), other.getSince());
        if (cmp == 0)
            {
            cmp = Integer.compare(getOrder(), other.getOrder());
            }
        return cmp == 0
               ? getName().compareTo(other.getName())
               : cmp;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The property name.
     */
    private String m_sName;

    /**
     * The class version the property was introduced in.
     */
    private int m_nSince;

    /**
     * The property order in the POF stream.
     */
    private int m_nOrder;

    /**
     * The additional type info.
     */
    private Object m_info;
    }
