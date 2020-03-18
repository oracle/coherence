/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.oracle.coherence.common.collections.InflatableMap;

import com.tangosol.io.ExternalizableLite;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.Map;

/**
* An implementation of java.util.Map that is optimal (in terms of both size
* and speed) for very small sets of data but still works excellently with
* large sets of data.  This implementation is not thread-safe.
* <p>
* The LiteMap implementation switches at runtime between several different
* sub-implementations for storing the Map of objects, described here:
* <ol>
* <li>"empty map" - a map that contains no data;
* <li>"single entry" - a reference directly to a single map entry
* <li>"Object[]" - a reference to an array of entries; the item limit for
*     this implementation is determined by the THRESHOLD constant;
* <li>"delegation" - for more than THRESHOLD items, a map is created to
*     delegate the map management to; sub-classes can override the default
*     delegation class (java.util.HashMap) by overriding the factory method
*     instantiateMap.
* </ol>
* <p>
* The LiteMap implementation supports the null key value.
*
* @author cp 06/29/99
*/
public class LiteMap<K, V>
        extends InflatableMap<K, V>
        implements ExternalizableLite
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a LiteMap.
    */
    public LiteMap()
        {
        }

    /**
    * Construct a LiteMap with the same mappings as the given map.
    *
    * @param map the map whose mappings are to be placed in this map.
    */
    public LiteMap(Map<? extends K, ? extends V> map)
        {
        putAll(map);
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        if (!isEmpty())
            {
            throw new NotActiveException();
            }

        int c = ExternalizableHelper.readInt(in);
        if (c == 0)
            {
            return;
            }

        boolean fLite = in.readBoolean();
        ObjectInput inObj = fLite
                ? null
                : ExternalizableHelper.getObjectInput(in, null);

        // either pick the "delegation" model up front and read entries
        // directly into the delegatee map, or just add the entries one by
        // one to this Map (which results in no wasted allocations)
        Map<K, V> map;
        if (c > THRESHOLD)
            {
            map = instantiateMap();
            m_nImpl = I_OTHER;
            m_oContents = map;
            }
        else
            {
            map = this;
            }

        for (int i = 0; i < c; ++i)
            {
            K key;
            V value;
            if (fLite)
                {
                key   = (K) ExternalizableHelper.readObject(in);
                value = (V) ExternalizableHelper.readObject(in);
                }
            else
                {
                try
                    {
                    key   = (K) inObj.readObject();
                    value = (V) inObj.readObject();
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new IOException("readObject failed: " + e
                            + "\n" + Base.getStackTrace(e));
                    }
                }

            map.put(key, value);
            }
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void writeExternal(DataOutput out)
            throws IOException
        {
        int               c      = size();
        Map.Entry<K, V>[] aEntry = (Map.Entry[]) entrySet().toArray(new Map.Entry[c]);
        ExternalizableHelper.writeInt(out, c);
        if (c == 0)
            {
            return;
            }

        // scan through the contents searching for anything that cannot be
        // streamed to a DataOutput (i.e. anything that requires Java Object
        // serialization); note that the toArray() also resolves concerns
        // related to the synchronization of the data structure itself during
        // serialization
        boolean fLite  = true;
        final int FMT_OBJ_SER = ExternalizableHelper.FMT_OBJ_SER;
        for (int i = 0; i < c; ++i)
            {
            Map.Entry<K, V> entry = aEntry[i];
            if (ExternalizableHelper.getStreamFormat(entry.getKey()) == FMT_OBJ_SER ||
                ExternalizableHelper.getStreamFormat(entry.getValue()) == FMT_OBJ_SER)
                {
                fLite = false;
                break;
                }
            }
        out.writeBoolean(fLite);

        ObjectOutput outObj = fLite
                ? null
                : ExternalizableHelper.getObjectOutput(out);

        for (int i = 0; i < c; ++i)
            {
            Map.Entry<K, V> entry = aEntry[i];
            K key   = entry.getKey();
            V value = entry.getValue();
            if (fLite)
                {
                ExternalizableHelper.writeObject(out, key);
                ExternalizableHelper.writeObject(out, value);
                }
            else
                {
                outObj.writeObject(key);
                outObj.writeObject(value);
                }
            }

        if (outObj != null)
            {
            outObj.close();
            }
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = -4530874198920262848L;
    }
