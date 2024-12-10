/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

/**
 * A Long like class which supports mutation.
 *
 * @author mf  2014.03.20
 */
public class MutableLong
        extends Number
        implements Comparable<MutableLong>
    {
    /**
     * Construct a MutableLong with a zero initial value.
     */
    public MutableLong()
        {
        }

    /**
     * Construct a MutableLong with the specified value.
     *
     * @param lValue  the initial value
     */
    public MutableLong(long lValue)
        {
        m_lValue = lValue;
        }


    // ----- MutableLong interface ------------------------------------------

    /**
     * Update the value
     *
     * @param lValue  the new value
     *
     * @return this object
     */
    public MutableLong set(long lValue)
        {
        m_lValue = lValue;
        return this;
        }

    /**
     * Return the current value.
     *
     * @return the value
     */
    public long get()
        {
        return m_lValue;
        }

    /**
     * Increment the long and return the new value.
     *
     * @return the new value
     */
    public long incrementAndGet()
        {
        return ++m_lValue;
        }

    /**
     * Decrement the long and return the new value.
     *
     * @return the new value
     */
    public long decrementAndGet()
        {
        return --m_lValue;
        }

    /**
     * Return a ThreadLocal of MutableLong.
     *
     * @return a ThreadLocal of MutableLong
     */
    public static ThreadLocal<MutableLong> createThreadLocal()
        {
        return ThreadLocal.withInitial(MutableLong::new);
        }

    // ----- Number interface -----------------------------------------------

    @Override
    public int intValue()
        {
        return (int) m_lValue;
        }

    @Override
    public long longValue()
        {
        return m_lValue;
        }

    @Override
    public float floatValue()
        {
        return m_lValue;
        }

    @Override
    public double doubleValue()
        {
        return m_lValue;
        }


    // ----- Comparable interface -------------------------------------------

    @Override
    public int compareTo(MutableLong that)
        {
        long lDelta = this.m_lValue - that.m_lValue;
        return lDelta < 0 ? -1 : lDelta == 0 ? 0 : 1;
        }


    // ----- Object interface -----------------------------------------------

    @Override
    public int hashCode()
        {
        return (int)(m_lValue ^ (m_lValue >>> 32)); // same as Long.hashCode
        }

    @Override
    public boolean equals(Object that)
        {
        return that == this || that instanceof MutableLong && ((MutableLong) that).m_lValue == m_lValue;
        }

    @Override
    public String toString()
        {
        return String.valueOf(m_lValue);
        }


    // ----- data members ---------------------------------------------------

    /**
     * The value.
     */
    protected long m_lValue;
    }
