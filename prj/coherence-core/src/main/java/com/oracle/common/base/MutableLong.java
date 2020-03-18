/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;

/**
 * A Long like class which supports mutation.
 *
 * @author mf  2014.03.20
 * @deprecated use {@link com.oracle.coherence.common.base.MutableLong} instead
 */
@Deprecated
public class MutableLong
        extends com.oracle.coherence.common.base.MutableLong
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

    /**
     * Return a ThreadLocal of MutableLong.
     *
     * @return a ThreadLocal of MutableLong
     */
    public static ThreadLocal<com.oracle.coherence.common.base.MutableLong> createThreadLocal()
        {
        return ThreadLocal.withInitial(MutableLong::new);
        }
    }
