/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapEventTransformer;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* MapEventTransformerFilter is a generic multiplexing wrapper that combines two
* implementations: a Filter (most commonly a {@link MapEventFilter}) and a
* MapEventTransformer and is used to register event listeners that allow to
* change the content of a MapEvent.
*
* @see com.tangosol.util.transformer.SemiLiteEventTransformer
*
* @author gg/jh  2008.05.01
* @since Coherence 3.4
*/
public class MapEventTransformerFilter<T>
        extends    ExternalizableHelper
        implements Filter<T>, MapEventTransformer, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * No-argument constructor for lite deserialization.
    */
    public MapEventTransformerFilter()
        {
        }

    /**
    * Construct a MapEventTransformerFilter based on the specified Filter
    * and MapEventTransformer.
    *
    * @param filter       the underlying Filter (e.g. MapEventFilter) used to
    *                     evaluate original MapEvent objects (optional)
    * @param transformer  the underlying MapEventTransformer used to
    *                     transform original MapEvent objects
    */
    public MapEventTransformerFilter(Filter<T> filter, MapEventTransformer transformer)
        {
        azzert(transformer != null, "null transformer");

        m_filter      = filter;
        m_transformer = transformer;
        }


    // ----- Filter interface -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluate(T o)
        {
        if (!(o instanceof MapEvent))
            {
            throw new IllegalStateException(ClassHelper.getSimpleName(getClass())
                + " should not be used as a general purpose filter");
            }
        Filter<T> filter = m_filter;
        return filter == null || filter.evaluate(o);
        }


    // ----- MapEventTransformer methods ------------------------------------

    /**
    * Remove an old value from the specified MapEvent.
    *
    * @return modified MapEvent object that does not contain the old value
    */
    public MapEvent transform(MapEvent event)
        {
        return m_transformer.transform(event);
        }


    // ----- ExternalizableLite methods -------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_filter      = (Filter) readObject(in);
        m_transformer = (MapEventTransformer) readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        writeObject(out, m_filter);
        writeObject(out, m_transformer);
        }


    // ----- PortableObject methods -----------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_filter      = (Filter) in.readObject(0);
        m_transformer = (MapEventTransformer) in.readObject(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_filter);
        out.writeObject(1, m_transformer);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the MapEventTransformerFilter with another object to determine
    * equality.
    *
    * @return true iff this MapEventTransformerFilter and the passed object are
    *         equivalent
    */
    public boolean equals(Object o)
        {
        if (o instanceof MapEventTransformerFilter)
            {
            MapEventTransformerFilter that = (MapEventTransformerFilter) o;

            return equals(this.m_filter,      that.m_filter)
                && equals(this.m_transformer, that.m_transformer);
            }
        return false;
        }

    /**
    * Determine a hash value for the MapEventTransformerFilter object according
    * to the general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this object
    */
    public int hashCode()
        {
        Filter<T> filter = m_filter;
        return (m_filter == null ? 79 : filter.hashCode() ) + hashCode(m_transformer);
        }

    /**
    * Provide a human-readable representation of this object.
    *
    * @return a String whose contents represent the value of this object
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) + '(' + m_filter +
            ", " + m_transformer + ')';
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying Filter to evaluate MapEvents with.
    */
    @JsonbProperty("filter")
    private Filter<T> m_filter;

    /**
    * The underlying transformer.
    */
    @JsonbProperty("transformer")
    private MapEventTransformer m_transformer;
    }
