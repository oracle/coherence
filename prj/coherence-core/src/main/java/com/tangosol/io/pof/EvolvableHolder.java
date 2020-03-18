/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import com.tangosol.io.Evolvable;

import com.tangosol.io.SimpleEvolvable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Storage for evolvable classes.
 *
 * @author as  2013.07.25
 * @since  12.2.1
 */
public class EvolvableHolder
    {
    /**
     * Return an {@link Evolvable} for the specified type id.
     *
     * @param idType  type identifier
     *
     * @return  an Evolvable instance
     */
    public Evolvable get(Integer idType)
        {
        return m_mapEvolvable.computeIfAbsent(idType, key -> new SimpleEvolvable(0));
        }

    /**
     * Return type identifiers for all the Evolvables within this holder.
     *
     * @return  type identifiers for all the Evolvables within this holder
     */
    public Set<Integer> getTypeIds()
        {
        return m_mapEvolvable.keySet();
        }

    /**
     * Return <code>true</code> if this holder is empty.
     *
     * @return  <code>true</code> if this holder is empty, <code>false</code> otherwise
     */
    public boolean isEmpty()
        {
        return m_mapEvolvable.isEmpty();
        }

    // ---- data members ----------------------------------------------------

    /**
     * Map of Evolvable objects, keyed by type id.
     */
    protected Map<Integer, Evolvable> m_mapEvolvable = new HashMap<>();
    }
