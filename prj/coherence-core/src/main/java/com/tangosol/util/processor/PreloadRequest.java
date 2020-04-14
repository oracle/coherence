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

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.NullImplementation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Map;
import java.util.Set;


/**
* PreloadRequest is a simple EntryProcessor that performs a
* {@link com.tangosol.util.InvocableMap.Entry#getValue() Entry.getValue}
* call. No results are reported back to the caller.
* <p>
* The PreloadRequest process provides a means to "pre-load" an entry or a
* collection of entries into the cache using the cache loader without
* incurring the cost of sending the value(s) over the network. If the
* corresponding entry (or entries) already exists in the cache, or if the
* cache does not have a loader, then invoking this EntryProcessor has no
* effect.
*
* @author gg 2006.04.28
* @since Coherence 3.2
*/
public class PreloadRequest<K, V>
        extends    AbstractProcessor<K, V, V>
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the PreloadRequest processor.
    */
    public PreloadRequest()
        {
        }


    // ----- EntryProcessor interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public V process(InvocableMap.Entry<K, V> entry)
        {
        if (!entry.isPresent())
            {
            if (entry instanceof BinaryEntry)
                {
                ((BinaryEntry) entry).getBinaryValue();
                }
            else
                {
                entry.getValue();
                }
            }
        return null;
        }

    /**
    * {@inheritDoc}
    */
    public Map<K, V> processAll(Set<? extends InvocableMap.Entry<K, V>> setEntries)
        {
        // TODO: an alternative approach could be as follows:
        /*
        CacheMap mapBacking = null;
        Set      setKeys    = new HashSet(setEntries.size());
        for (Iterator iter = setEntries.iterator(); iter.hasNext();)
            {
            InvocableMap.Entry entry = (InvocableMap.Entry) iter.next();
            if (entry instanceof BinaryEntry)
                {
                BinaryEntry entryBin = (BinaryEntry) entry;
                if (mapBacking == null)
                    {
                    Map map = entryBin.getBackingMap();
                    if (map instanceof CacheMap)
                        {
                        mapBacking = (CacheMap) map;
                        }
                    else
                        {
                        break;
                        }
                    }
                setKeys.add(entryBin.getBinaryKey());
                }
            else
                {
                mapBacking = null;
                break;
                }
            }

         if (mapBacking == null)
            {
            super.processAll(setEntries);
            }
         else
            {
            mapBacking.getAll(setKeys);
            }
         */

        super.processAll(setEntries);

        return NullImplementation.getMap();
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the PreloadRequest with another object to determine equality.
    *
    * @return true iff this PreloadRequest and the passed object are
    *         equivalent PreloadRequest objects
    */
    public boolean equals(Object o)
        {
        return o instanceof PreloadRequest;
        }

    /**
    * Determine a hash value for the PreloadRequest object according to the
    * general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this ConditionalPut object
    */
    public int hashCode()
        {
        return 3;
        }

    /**
    * Return a human-readable description for this PreloadRequest processor.
    *
    * @return a String description of the PreloadRequest processor
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass());
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        }


    // ----- constants ------------------------------------------------------

    /**
    * An instance of the PreloadRequest processor.
    */
    public static final PreloadRequest INSTANCE = new PreloadRequest();

    /**
    * Return an instance of the PreloadRequest processor.
    *
    * @param <K>  the key type
    * @param <V>  the value type
    *
    * @return a PreloadRequest
    */
    public static <K, V> PreloadRequest<K, V> INSTANCE()
        {
        return INSTANCE;
        }
    }
