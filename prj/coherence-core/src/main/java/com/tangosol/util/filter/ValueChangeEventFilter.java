/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* Filter which evaluates the content of a MapEvent values based on the
* specified value extractor.  This filter evaluates to true only for update
* events that change the value of an extracted attribute.
* <p>
* Example: a filter that evaluates to true if there is an update to an
* Employee object that changes a value of the LastName property.
* <pre>
*   new ValueChangeEventFilter("LastName");
* </pre>
*
* @see MapEventFilter
*
* @author gg 2003.09.30
* @since Coherence 2.3
*/
public class ValueChangeEventFilter<V, E>
    implements Filter<MapEvent<?, V>>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ValueChangeEventFilter()
        {
        }

    /**
    * Construct a ValueChangeEventFilter that evaluates MapEvent values
    * based on the specified extractor.
    *
    * @param extractor  ValueExtractor to extract MapEvent values
    */
    public ValueChangeEventFilter(ValueExtractor<? super V, ? extends E> extractor)
        {
        m_extractor = extractor;
        }

    /**
    * Construct a ValueChangeEventFilter that evaluates MapEvent values
    * based on the specified method name.
    *
    * @param sMethod  the name of the method to invoke via reflection
    */
    public ValueChangeEventFilter(String sMethod)
        {
        this(sMethod.indexOf('.') < 0
             ? new ReflectionExtractor<>(sMethod)
             : new ChainedExtractor<>(sMethod));
        }


    // ----- Filter interface -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluate(MapEvent<?, V> event)
        {
        return event.getId() == MapEvent.ENTRY_UPDATED &&
                !Base.equals(
                        InvocableMapHelper.extractFromEntry(m_extractor, event.getOldEntry()),
                        InvocableMapHelper.extractFromEntry(m_extractor, event.getNewEntry()));
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Extract value from a target object.
     *
     * @param target  the object to extract the value from
     *
     * @return the extracted value
     */
    protected E extract(V target)
        {
        return m_extractor.extract(target);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the ValueChangeEventFilter with another object to determine equality.
    *
    * @return true iff this ValueChangeEventFilter and the passed object are
    *         equivalent filters
    */
    public boolean equals(Object o)
        {
        if (o instanceof ValueChangeEventFilter)
            {
            ValueChangeEventFilter that = (ValueChangeEventFilter) o;
            return Base.equals(this.m_extractor, that.m_extractor);
            }

        return false;
        }

    /**
    * Determine a hash value for the ValueChangeEventFilter object according to the
    * general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this ValueChangeEventFilter object
    */
    public int hashCode()
        {
        return Base.hashCode(m_extractor);
        }

    /**
    * Return a human-readable description for this Filter.
    *
    * @return a String description of the Filter
    */
    public String toString()
        {
        return "ValueChangeEventFilter(extractor=" + m_extractor + ')';
        }


    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_extractor = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_extractor);
        }


    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_extractor = in.readObject(0);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_extractor);
        }

    // ---- data members ----------------------------------------------------

    @JsonbProperty("extractor")
    protected ValueExtractor<? super V, ? extends E> m_extractor;
    }
