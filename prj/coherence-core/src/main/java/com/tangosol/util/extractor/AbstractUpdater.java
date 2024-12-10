/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;


import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueUpdater;

import java.io.Serializable;

import java.util.Map;


/**
* Abstract base for ValueUpdater implementations.
* <p>
* Starting with Coherence 3.6, when used to update information stored in a Map,
* subclasses have the additional ability to operate against the Map.Entry
* instead of just the value. This allows an updater implementation to update a
* desired value using all available information on the corresponding Map.Entry
* object and is intended to be used in advanced custom scenarios, when
* application code needs to look at both key and value at the same time or can
* make some very specific assumptions regarding to the implementation details of
* the underlying Entry object (e.g. {@link com.tangosol.util.BinaryEntry}).
* To maintain full backwards compatibility, the default behavior remains to
* update the Value property of the Map.Entry.
* <p>
* <b>Note:</b> subclasses are responsible for POF and/or Lite serialization of
* the updater.
*
* @author gg 2009.09.11
* @since Coherence 3.6
*/
public abstract class AbstractUpdater<K, V, U>
        extends    ExternalizableHelper
        implements ValueUpdater<Object, U>, Serializable
    {
    // ----- ValueUpdater interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void update(Object oTarget, U oValue)
        {
        throw new UnsupportedOperationException();
        }


    // ----- subclassing support --------------------------------------------

    /**
    * Update the state of the passed entry using the passed value.
    * <p>
    * By overriding this method, an updater implementation is able to update
    * the entry's value using all available information on the corresponding
    * Map.Entry object and is intended to be used in advanced custom scenarios,
    * when application code needs to look at both key and value at the same time
    * or can make some very specific assumptions regarding to the implementation
    * details of the underlying Entry object.
    *
    * @param entry   the Entry object whose value is to be updated
    * @param oValue  the new value to update the entry with;  for intrinsic
    *                types, the specified value is expected to be a standard
    *                wrapper type in the same manner that reflection works
    *                (e.g. an int value would be passed as a java.lang.Integer)
    */
    public void updateEntry(Map.Entry<K, V> entry, U oValue)
        {
        // the code below is identical to the bottom part of the
        // com.tangosol.util.InvocableMapHelper#updateEntry(), but I couldn't
        // figure out a neat way to coalesce these small blocks of code... (GG)

        V oTarget = entry.getValue();

        update(oTarget, oValue);

        if (entry instanceof InvocableMap.Entry)
            {
            ((InvocableMap.Entry<K, V>) entry).setValue(oTarget, false);
            }
        else
            {
            entry.setValue(oTarget);
            }
        }
    }
