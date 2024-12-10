/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.transformer;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapEventTransformer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
* SemiLiteEventTransformer is a special purpose MapEventTransformer
* implementation that removes an OldValue from the MapEvent object for the
* purpose of reducing the amount of data that has to be sent over the network
* to event consumers.
* <p>
* Usage example:
* <pre>
*    cache.addMapListener(listener, new MapEventTransformerFilter(null,
*        SemiLiteEventTransformer.INSTANCE), false);
* </pre>

* @author gg/jh  2008.05.01
* @since Coherence 3.4
*/
public class SemiLiteEventTransformer<K, V>
        extends ExternalizableHelper
        implements MapEventTransformer<K, V, V>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Deserialization constructor.
    */
    public SemiLiteEventTransformer()
        {
        }

    // ----- MapEventTransformer methods ------------------------------------

    /**
    * Remove an old value from the specified MapEvent.
    *
    * @return modified MapEvent object that does not contain the old value
    */
    public MapEvent<K, V> transform(MapEvent<K, V> event)
        {
        if (event instanceof ConverterCollections.ConverterMapEvent)
            {
            ((ConverterCollections.ConverterMapEvent) event).setOldValue(null);
            return event;
            }
        else
            {
            return new MapEvent(event.getMap(), event.getId(),
                event.getKey(), null, event.getNewValue());
            }
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        }

    // ----- Object methods -------------------------------------------------

    /**
    * Compare the SemiLiteEventTransformer with another object to determine
    * equality.
    *
    * @return true iff this SemiLiteEventTransformer and the passed object are
    *         equivalent
    */
    public boolean equals(Object o)
        {
        return o instanceof SemiLiteEventTransformer;
        }

    /**
    * Determine a hash value for the SemiLiteEventTransformer object according to
    * the general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this object
    */
    public int hashCode()
        {
        return 79;
        }

    /**
    * Provide a human-readable representation of this object.
    *
    * @return a String whose contents represent the value of this object
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) + "@" + hashCode();
        }


    // ----- constants ------------------------------------------------------

    /**
    * The SemiLiteEventTransformer singleton.
    */
    public static final SemiLiteEventTransformer INSTANCE =
        new SemiLiteEventTransformer();
    }
