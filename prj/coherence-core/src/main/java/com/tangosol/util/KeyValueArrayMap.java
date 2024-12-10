/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.Iterator;


/**
 * KeyValueArrayMap is a Map implementation backed by an array of keys, and an
 * array of the associated values.
 * <p>
 * This implementation:
 * <ul>
 *   <li>does not support updates or removals</li>
 *   <li>does not ensure the uniqueness of keys (this is the caller's responsibility)</li>
 *   <li>is not thread-safe</li>
 * </ul>
 *
 * @author rhl 2011.12.20
 * @since  Coherence 12.1.2
 */
public class KeyValueArrayMap
        extends AbstractKeyBasedMap
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a KeyValueArrayMap backed by the specified key and value arrays.
     * The specified arrays must be non-null and of equal length.
     *
     * @param aoKey    the array of keys
     * @param aoValue  the array of values
     */
    public KeyValueArrayMap(Object[] aoKey, Object[] aoValue)
        {
        this(aoKey, 0, aoValue, 0, aoKey.length);
        }

    /**
     * Construct a KeyValueArrayMap backed by ranges of the specified key and
     * value arrays.
     *
     * @param aoKey    the array of keys
     * @param iKey     the index of the first key
     * @param aoValue  the array of values
     * @param iValue   the index of the first value
     * @param cSize    the number of entries
     */
    public KeyValueArrayMap(Object[] aoKey, int iKey, Object[] aoValue, int iValue, int cSize)
        {
        // intentional NPE if aoKey or aoValue are null
        if (iKey + cSize > aoKey.length || iValue + cSize > aoValue.length)
            {
            throw new ArrayIndexOutOfBoundsException();
            }

        m_aoKey   = aoKey;
        m_aoValue = aoValue;
        m_iKey    = iKey;
        m_iValue  = iValue;
        m_cSize   = cSize;
        }


    // ----- AbstractKeyBasedMap methods ------------------------------------

    /**
     * {@inheritDoc}
     */
    public Object get(Object oKey)
        {
        Object[] aoKey = m_aoKey;
        int      iKey  = m_iKey;
        for (int i = 0, c = m_cSize; i < c; i++)
            {
            if (Base.equals(aoKey[iKey + i], oKey))
                {
                return m_aoValue[m_iValue + i];
                }
            }

        return null;
        }

    /**
     * {@inheritDoc}
     */
    protected Iterator iterateKeys()
        {
        return new SimpleEnumerator(m_aoKey, m_iKey, m_cSize);
        }

    /**
     * {@inheritDoc}
     */
    public int size()
        {
        return m_cSize;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The array containing map keys.
     */
    protected Object[] m_aoKey;

    /**
     * The array containing map values.
     */
    protected Object[] m_aoValue;

    /**
     * The index into the key array of the first key.
     */
    protected int      m_iKey;

    /**
     * The index into the value array of the first value.
     */
    protected int      m_iValue;

    /**
     * The number of map entries.
     */
    protected int      m_cSize;
    }


