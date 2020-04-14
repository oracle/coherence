/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.AbstractMap;
import java.util.Set;


/**
* A trivial Map implementation that is based on a specified set of entries.
* This implementation does not support inserts or updates. Additionally, since
* almost every operation is implemented by iterating over the underlying entry
* set, this class is meant to be used only for iterations or small sets of data.
*
* @since Coherence 3.6
*/
public class EntrySetMap
        extends AbstractMap
    {
    public EntrySetMap(Set setEntries)
        {
        m_setEntries = setEntries;
        }

    public Set entrySet()
        {
        return m_setEntries;
        }

    /**
    * The underlying set of Entry objects.
    */
    protected Set m_setEntries;
    }
