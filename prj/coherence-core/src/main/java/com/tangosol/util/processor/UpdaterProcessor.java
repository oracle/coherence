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

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueUpdater;

import com.tangosol.util.extractor.CompositeUpdater;
import com.tangosol.util.extractor.ReflectionUpdater;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* UpdaterProcessor is an EntryProcessor implementations that updates an
* attribute of an object cached in an InvocableMap. A common usage pattern
* is:
* <pre>
*   cache.invoke(oKey, new UpdaterProcessor(updater, value));
* </pre>
* which is functionally equivalent to the following operation:
* <pre>
*   V target = cache.get(key);
*   updater.update(target, value);
*   cache.put(key, target);
* </pre>
* The major difference is that for clustered caches using the UpdaterProcessor
* allows avoiding explicit concurrency control and could significantly reduce
* the amount of network traffic.
*
* @author gg 2006.07.25
*/
public class UpdaterProcessor<K, V, T>
        extends    AbstractProcessor<K, V, Boolean>
        implements ExternalizableLite, PortableObject
    {
    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public UpdaterProcessor()
        {
        }

    /**
    * Construct an UpdaterProcessor based on the specified ValueUpdater.
    *
    * @param updater  a ValueUpdater object; passing null will simpy replace
    *                 the entry's value with the specified one instead of
    *                 updating it
    * @param value  the value to update the target entry with
    */
    public UpdaterProcessor(ValueUpdater<V, T> updater, T value)
        {
        m_updater = updater;
        m_value = value;
        }

    /**
    * Construct an UpdaterProcessor for a given method name. The method
    * must have a single parameter of a Java type compatible with the
    * specified value type.
    *
    * @param sMethod  a method name to make a
    *                 {@link com.tangosol.util.extractor.ReflectionUpdater}
    *                 for; this parameter can also be a dot-delimited sequence
    *                 of method names which would result in using a
    *                 {@link com.tangosol.util.extractor.CompositeUpdater}
     * @param value  the value to update the target entry with
    */
    public UpdaterProcessor(String sMethod, T value)
        {
        azzert(sMethod != null && sMethod.length() != 0, "Invalid method name");

        m_updater = sMethod.indexOf('.') < 0 ?
            new ReflectionUpdater(sMethod) :
            (ValueUpdater) new CompositeUpdater(sMethod);
        m_value = value;
        }


    // ----- ValueUpdater interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public Boolean process(InvocableMap.Entry<K, V> entry)
        {
        ValueUpdater<V, T> updater = m_updater;
        if (updater == null)
            {
            //NOTE: a possibly unsafe cast from T to V
            entry.setValue((V) m_value, false);
            }
        else if (entry.isPresent())
            {
            entry.update(updater, m_value);
            }
        else
            {
            return Boolean.FALSE;
            }
        return Boolean.TRUE;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the UpdaterProcessor with another object to determine equality.
    *
    * @return true iff this UpdaterProcessor and the passed object are
    *         equivalent UpdaterProcessor
    */
    public boolean equals(Object o)
        {
        if (o instanceof UpdaterProcessor)
            {
            UpdaterProcessor that = (UpdaterProcessor) o;
            return equals(this.m_updater, that.m_updater) &&
                   equals(this.m_value,  that.m_value);
            }

        return false;
        }

    /**
    * Determine a hash value for the UpdaterProcessor object according to the
    * general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this UpdaterProcessor object
    */
    public int hashCode()
        {
        return m_updater.hashCode();
        }

    /**
    * Return a human-readable description for this UpdaterProcessor.
    *
    * @return a String description of the UpdaterProcessor
    */
    public String toString()
        {
        return "UpdaterProcessor(" + m_updater + ", " + m_value + ')';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_updater = (ValueUpdater) ExternalizableHelper.readObject(in);
        m_value = (T) ExternalizableHelper.readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_updater);
        ExternalizableHelper.writeObject(out, m_value);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_updater = (ValueUpdater) in.readObject(0);
        m_value = (T) in.readObject(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_updater);
        out.writeObject(1, m_value);
        }


    // ----- data fields ----------------------------------------------------

    /**
    * The underlying ValueUpdater.
    */
    @JsonbProperty("updater")
    protected ValueUpdater<V, T> m_updater;

    /**
    * A value to update the entry's value with.
    */
    @JsonbProperty("value")
    protected T m_value;
    }