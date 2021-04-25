/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An object that represents a fragment of another object.
 *
 * @param <T>  the type of the object this fragment represents
 *
 * @author Aleks Seovic  2021.02.22
 * @since 21.06
 */
public class Fragment<T>
        implements ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    @SuppressWarnings("unused")
    public Fragment()
        {
        }

    /**
     * Construct {@code Fragment} instance.
     *
     * @param mapAttr  a map of extracted attribute values, keyed by canonical
     *                 name or extracted attribute position (if the name is {@code null})
     */
    public Fragment(Map<String, Object> mapAttr)
        {
        m_mapAttr = mapAttr;
        }

    // ---- public API ------------------------------------------------------

    /**
     * Get the value of the attribute extracted by the specified extractor.
     *
     * @param extractor  an extractor that was used to extract an attribute
     * @param <E>        the type of extracted attribute
     *
     * @return the value of the attribute extracted by the specified extractor
     */
    public <E> E get(ValueExtractor<? super T, ? extends E> extractor)
        {
        return get(ValueExtractor.of(extractor).getCanonicalName());
        }

    /**
     * Get the value of the attribute with the specified name or positional index.
     *
     * @param sName  the name of the attribute, or a positional index in the
     *               {@code $N} format
     * @param <E>    the type of extracted attribute
     *
     * @return the value of the specified attribute
     */
    @SuppressWarnings("unchecked")
    public <E> E get(String sName)
        {
        return (E) m_mapAttr.get(sName);
        }

    /**
     * Get the nested fragment extracted from the specified attribute.
     *
     * @param extractor  an attribute to extract a nested fragment from
     * @param <E>        the type of attribute to extract a nested fragment from
     *
     * @return the fragment extracted from the specified attribute
     */
    public <E> Fragment<E> getFragment(ValueExtractor<? super T, ? extends E> extractor)
        {
        return getFragment(ValueExtractor.of(extractor).getCanonicalName());
        }

    /**
     * Get the nested fragment extracted from the specified attribute.
     *
     * @param sName  an attribute to extract a nested fragment from
     * @param <E>    the type of attribute to extract a nested fragment from
     *
     * @return the fragment extracted from the specified attribute
     */
    @SuppressWarnings("unchecked")
    public <E> Fragment<E> getFragment(String sName)
        {
        return (Fragment<E>) m_mapAttr.get(sName);
        }

    /**
     * Convert this {@code Fragment} into a {@code Map}.
     * <p/>
     * Any nested fragments within this fragment will be recursively converted
     * into the map as well.
     *
     * @return a {@code Map} with property names as keys, and the property
     *         values as values
     */
    public Map<String, Object> toMap()
        {
        Map<String, Object> map = new HashMap<>(m_mapAttr.size());
        for (Map.Entry<String, ?> entry : m_mapAttr.entrySet())
            {
            Object oValue = entry.getValue();
            map.put(entry.getKey(), oValue instanceof Fragment
                                    ? ((Fragment<?>) oValue).toMap()
                                    : oValue);
            }
        return map;
        }

    // ---- Object methods --------------------------------------------------

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
        Fragment<?> fragment = (Fragment<?>) o;
        return m_mapAttr.equals(fragment.m_mapAttr);
        }

    public int hashCode()
        {
        return Objects.hash(m_mapAttr);
        }

    public String toString()
        {
        return "Fragment{" + m_mapAttr + '}';
        }

    // ---- ExternalizableLite interface ------------------------------------

    public void readExternal(DataInput in) throws IOException
        {
        ExternalizableHelper.readMap(in, m_mapAttr, null);
        }

    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeMap(out, m_mapAttr);
        }

    // ---- PortableObject interface ----------------------------------------

    public void readExternal(PofReader in) throws IOException
        {
        in.readMap(0, m_mapAttr);
        }

    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeMap(0, m_mapAttr, String.class);
        }

    // ---- data members ----------------------------------------------------

    /**
     * A map of extracted attribute values, keyed by canonical name or
     * extracted attribute position (if the name is {@code null}).
     */
    private Map<String, ?> m_mapAttr = new HashMap<>();
    }
