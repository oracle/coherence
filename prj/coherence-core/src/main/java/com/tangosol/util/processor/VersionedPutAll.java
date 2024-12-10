/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.processor;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.LiteMap;
import com.tangosol.util.Versionable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.json.bind.annotation.JsonbProperty;


/**
* VersionedPutAll is an EntryProcessor that assumes that entry values
* implement {@link com.tangosol.util.Versionable} interface and performs an
* {@link com.tangosol.util.InvocableMap.Entry#setValue(Object)
* Entry.setValue} operation only for entries whose versions match to versions
* of the corresponding current values. In case of the match, the
* VersionedPutAll will increment the version indicator before each value is
* updated.
*
* @see VersionedPut
* @author gg 2006.05.07
* @since Coherence 3.2
*/
public class VersionedPutAll<K, V extends Versionable>
        extends    AbstractProcessor<K, V, V>
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public VersionedPutAll()
        {
        }

    /**
    * Construct a VersionedPutAll processor that updates an entry with a new
    * value if and only if the version of the new value matches to the
    * version of the current entry's value (which must exist).
    * The result of the {@link #process} invocation does not return any
    * result.
    *
    * @param map a map of values to update entries with
    */
    public VersionedPutAll(Map<? extends K, ? extends V> map)
        {
        this(map, false, false);
        }

    /**
    * Construct a VersionedPutAll processor that updates an entry with a new
    * value if and only if the version of the new value matches to the
    * version of the current entry's value (which must exist). This processor
    * optionally returns a map of entries that have not been updated (the
    * versions did not match).
    *
    * @param map           a map of values to update entries with
    * @param fAllowInsert  specifies whether or not an insert should be
    *                      allowed (no currently existing value)
    * @param fReturn       specifies whether or not the processor should
    *                      return the entries that have not been updated
    */
    public VersionedPutAll(Map<? extends K, ? extends V> map, boolean fAllowInsert, boolean fReturn)
        {
        azzert(map != null, "Map is null");

        m_map     = new HashMap(map);
        m_fInsert = fAllowInsert;
        m_fReturn = fReturn;
        }


    // ----- EntryProcessor interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public V process(InvocableMap.Entry<K, V> entry)
        {
        Object oResult = processEntry(entry, m_map, m_fInsert, m_fReturn);
        return oResult == NO_RESULT ? null : (V) oResult;
        }

    /**
    * {@inheritDoc}
    */
    public Map<K, V> processAll(Set<? extends InvocableMap.Entry<K, V>> setEntries)
        {
        Map     mapResult = new LiteMap();
        Map     mapAll    = m_map;
        boolean fInsert   = m_fInsert;
        boolean fReturn   = m_fReturn;

        for (Iterator iter = setEntries.iterator(); iter.hasNext();)
            {
            InvocableMap.Entry entry = (InvocableMap.Entry) iter.next();

            Object oResult = processEntry(entry, mapAll, fInsert, fReturn);

            if (oResult != NO_RESULT)
                {
                mapResult.put(entry.getKey(), oResult);
                }
            }

        return mapResult;
        }


// ----- Object methods -------------------------------------------------

    /**
    * Compare the VersionedPutAll with another object to determine equality.
    *
    * @return true iff this VersionedPutAll and the passed object are
    *         equivalent VersionedPutAll
    */
    public boolean equals(Object o)
        {
        if (o instanceof VersionedPutAll)
            {
            VersionedPutAll that = (VersionedPutAll) o;
            return equals(this.m_map,       that.m_map)
                &&        this.m_fInsert == that.m_fInsert
                &&        this.m_fReturn == that.m_fReturn;
            }

        return false;
        }

    /**
    * Determine a hash value for the VersionedPutAll object according to the
    * general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this VersionedPutAll object
    */
    public int hashCode()
        {
        return m_map.hashCode() + (m_fInsert ? 1 : 2) + (m_fReturn ? 3 : 4);
        }

    /**
    * Return a human-readable description for this VersionedPutAll.
    *
    * @return a String description of the VersionedPutAll
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) +
            "{InsertAllowed="   + m_fInsert +
            ", ReturnRequired=" + m_fReturn +
            ", Map=" + m_map + '}';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        Map map = m_map = new HashMap();
        ExternalizableHelper.readMap(in, map, null);
        m_fInsert = in.readBoolean();
        m_fReturn = in.readBoolean();
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeMap(out, m_map);
        out.writeBoolean(m_fInsert);
        out.writeBoolean(m_fReturn);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_map     = in.readMap(0, new HashMap());
        m_fInsert = in.readBoolean(1);
        m_fReturn = in.readBoolean(2);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeMap(0, m_map);
        out.writeBoolean(1, m_fInsert);
        out.writeBoolean(2, m_fReturn);
        }


    // ----- helper methods -------------------------------------------------

    /**
    * Process the given entry.
    *
    * @param entry   entry the entry to be processed
    * @param mapAll  the map of new values
    * @param fInsert specifies whether or not an insert is allowed
    * @param fReturn specifies whether or not a return value is required
    *
    * @return the result of processing the entry; can be NO_RESULT
    */
    protected Object processEntry(InvocableMap.Entry<K, V> entry, Map<? extends K, ? extends V> mapAll,
                                  boolean fInsert,  boolean fReturn)
        {
        K oKey = entry.getKey();

        if (mapAll.containsKey(oKey))
            {
            V oValueCur = entry.getValue();
            V oValueNew = mapAll.get(oKey);
            boolean     fMatch;

            if (oValueCur == null)
                {
                fMatch = fInsert;
                }
            else
                {
                Comparable verCur = oValueCur.getVersionIndicator();
                Comparable verNew = oValueNew.getVersionIndicator();

                fMatch = verCur.compareTo(verNew) == 0;
                }

            if (fMatch)
                {
                oValueNew.incrementVersion();
                entry.setValue(oValueNew, false);
                return NO_RESULT;
                }
            return fReturn ? oValueCur : NO_RESULT;
            }

        return NO_RESULT;
        }


    // ----- constants ------------------------------------------------------

    /**
    * Used internally to differentiate between "no result" and a null result.
    * NO_RESULT should never be returned through the public APIs.
    * A value of NO_RESULT may be returned from processEntry() and will
    * never be included in the result map returned from processAll().
    */
    private static final Object NO_RESULT = new Object();


    // ----- data members ---------------------------------------------------

    /**
    * Specifies the map of new values.
    */
    @JsonbProperty("entries")
    protected Map<? extends K, ? extends V> m_map;

    /**
    * Specifies whether or not an insert is allowed.
    */
    @JsonbProperty("insert")
    protected boolean m_fInsert;

    /**
    * Specifies whether or not a return value is required.
    */
    @JsonbProperty("return")
    protected boolean m_fReturn;
    }