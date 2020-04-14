/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.lang.reflect.Array;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents a set of named properties and allows the extraction of those
 * properties from a target object.
 * <p>
 * Each extracted property is returned as a {@link PartialObject} instance,
 * which is an instance of a dynamically generated class containing only
 * properties defined in this PropertySet.
 *
 * @author as  2010.10.11
 */
public class PropertySet<T>
        implements Iterable<PropertySpec>, ValueExtractor<T, Object>,
                   ExternalizableLite, PortableObject
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public PropertySet()
        {
        m_setProperties = new TreeSet<>();
        }

    /**
     * Construct a PropertySet instance.
     *
     * @param aProperties  an array of property specifications
     */
    public PropertySet(PropertySpec... aProperties)
        {
        m_setProperties = new TreeSet<>(Arrays.asList(aProperties));
        }

    /**
     * Construct a PropertySet instance.
     *
     * @param setProperties  a set of property specifications
     */
    public PropertySet(Set<PropertySpec> setProperties)
        {
        m_setProperties = new TreeSet<>(setProperties);
        }

    // ----- factory methods ------------------------------------------------

    /**
     * Create a PropertySet from its textual representation.
     *
     * @param sPropertySet  property set descriptor
     *
     * @return property set instance
     */
    public static PropertySet fromString(String sPropertySet)
        {
        if (sPropertySet == null)
            {
            throw new IllegalArgumentException("null property set descriptor");
            }
        sPropertySet = sPropertySet.trim();

        StringBuilder sbName  = new StringBuilder();
        PropertySet   psOuter = new PropertySet();
        PropertySet   psInner = null;

        int ich = 0;
        int cch = sPropertySet.length();
        while (ich < cch)
            {
            char c = sPropertySet.charAt(ich++);
            if (c == ',')
                {
                // terminate property
                psOuter.add(new PropertySpec(sbName.toString(), psInner));
                sbName  = new StringBuilder();
                psInner = null;
                }
            else if (c == ':')
                {
                // property has inner property list
                ich++; // skip first open parenthesis

                StringBuilder sbInner = new StringBuilder();
                for (int cParen = 1; cParen > 0 && ich < cch; )
                    {
                    char cc = sPropertySet.charAt(ich++);
                    if (cc == ')')
                        {
                        cParen--;
                        }
                    else if (cc == '(')
                        {
                        cParen++;
                        }
                    if (cParen > 0)
                        {
                        sbInner.append(cc);
                        }
                    }

                psInner = fromString(sbInner.toString());
                }
            else if (c != ' ')
                {
                sbName.append(c);
                }
            }

        if (sbName.length() > 0)
            {
            psOuter.add(new PropertySpec(sbName.toString(), psInner));
            }

        return psOuter;
        }

    // ----- public methods -------------------------------------------------

    /**
     * Add a property to this property set.
     *
     * @param property  property to add
     */
    public void add(PropertySpec property)
        {
        m_setProperties.add(property);
        }

    /**
     * Extract a collection of partial objects.
     *
     * @param colSource  collection of source objects
     *
     * @return a collections of partial objects extracted from a source
     *         collection based on this property set
     */
    public Collection<Object> extract(Collection<? extends T> colSource)
        {
        if (colSource == null)
            {
            throw new IllegalArgumentException("null source collection");
            }

        Collection<Object> colValues = new ArrayList<>(colSource.size());
        for (T o : colSource)
            {
            colValues.add(extract(o));
            }

        return colValues;
        }

    /**
     * Extract an array of partial objects.
     *
     * @param aSource  an array of source objects
     *
     * @return an array of partial objects extracted from a source array
     *         based on this property set
     */
    public Object[] extract(Object[] aSource)
        {
        if (aSource == null)
            {
            throw new IllegalArgumentException("null source array");
            }

        int cSource = aSource.length;
        if (cSource == 0)
            {
            return new PartialObject[0];
            }

        Class clzElementType = PartialObject.getPartialClass(
                aSource[0].getClass(), this);

        Object[] aValues = (PartialObject[]) Array.newInstance(
                clzElementType, cSource);

        for (int i = 0; i < cSource; ++i)
            {
            aValues[i] = extract((T) aSource[i]);
            }

        return aValues;
        }

    /**
     * Extract a partial object from a source object.
     *
     * @param source  the source object
     *
     * @return partial object extracted from a source object based on this
     *         property set
     */
    public Object extract(T source)
        {
        if (source == null)
            {
            throw new IllegalArgumentException("null source object");
            }

        SortedSet<PropertySpec> setProperties = m_setProperties;

        Map<String, Object> mapProps =
                new LinkedHashMap<>(setProperties.size());

        for (PropertySpec property : setProperties)
            {
            String sName  = property.getName();
            Object oValue = property.getValue(source);

            if (oValue != null && property.getPropertySet() != null)
                {
                if (oValue.getClass().isArray())
                    {
                    oValue = property.getPropertySet().extract((Object[]) oValue);
                    }
                else if (oValue instanceof Collection)
                    {
                    Object[] aValues = new ArrayList((Collection) oValue).toArray();
                    oValue = property.getPropertySet().extract(aValues);
                    }
                else
                    {
                    oValue = property.getPropertySet().extract(oValue);
                    }

                property.setPartialClass(oValue.getClass());
                }

            mapProps.put(sName, oValue);
            }

        return source instanceof Map
               ? mapProps
               : PartialObject.create(source.getClass(), this, mapProps);
        }

    // ----- Iterable implementation ----------------------------------------

    /**
     * Return an iterator for this property set.
     *
     * @return iterator over the set of properties
     */
    public Iterator<PropertySpec> iterator()
        {
        return m_setProperties.iterator();
        }

    // ----- ExternalizableList implementation ------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        ExternalizableHelper.readCollection(in, m_setProperties, null);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeCollection(out, m_setProperties);
        }

    // ----- PortableObject implementation ----------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader)
            throws IOException
        {
        reader.readCollection(0, m_setProperties);
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeCollection(0, m_setProperties, PropertySpec.class);
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
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }

        PropertySet that = (PropertySet) o;
        return m_setProperties.equals(that.m_setProperties);
        }

    /**
     * Return hash code for this object.
     *
     * @return hash code
     */
    public int hashCode()
        {
        return m_setProperties.hashCode();
        }

    /**
     * Return string representation of this object.
     *
     * @return this object as a String
     */
    public String toString()
        {
        StringBuilder sb     = new StringBuilder();
        boolean       fFirst = true;

        for (PropertySpec p : this)
            {
            if (!fFirst)
                {
                sb.append(',');
                }
            sb.append(p);
            fFirst = false;
            }

        return sb.toString();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return the first property in this PropertySet.
     *
     * @return first property in this PropertySet
     */
    protected PropertySpec first()
        {
        SortedSet<PropertySpec> setProperties = m_setProperties;
        if (setProperties.size() > 0)
            {
            return setProperties.first();
            }
        return null;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the internal property set.
     *
     * @return internal property set
     */
    protected SortedSet<PropertySpec> getProperties()
        {
        return m_setProperties;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Property set.
     */
    protected final SortedSet<PropertySpec> m_setProperties;
    }
