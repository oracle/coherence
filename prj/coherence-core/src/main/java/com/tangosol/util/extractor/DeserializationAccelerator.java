/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.BackingMapContext;

import com.tangosol.util.ForwardOnlyMapIndex;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Comparator;
import java.util.Map;

import javax.json.bind.annotation.JsonbProperty;


/**
* DeserializationAccelerator is an {@link IndexAwareExtractor} implementation that
* is used to create a {@link ForwardOnlyMapIndex}, which in turn is used for
* deserialization optimization.
* <p>
* Below is an example how to use this feature to optimize deserialization for
* {@link com.tangosol.util.BinaryEntry#getValue BinaryEntry.getValue()} calls:
* <pre>
*   NamedCache cache = ...;
*   cache.addIndex(new DeserializationAccelerator(
*      IdentityExtractor.INSTANCE), false, null);
* </pre>
* There are two ways the DeserializationAccelerator could be used: <i>pro-active</i>
* (default) and <i>on-demand</i>. In the pro-active mode, the underlying
* {@link ForwardOnlyMapIndex} is populated every time the index is rebuilt or
* updated. In the on-demand mode, the {@link ForwardOnlyMapIndex} stores the
* values only when they are {@link ForwardOnlyMapIndex#get requested}.
*
* @author gg/hr/jh 2014.03.07
* @since Coherence 12.1.3
*/
public class DeserializationAccelerator
        extends AbstractExtractor
        implements IndexAwareExtractor, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the DeserializationAccelerator.
    */
    public DeserializationAccelerator()
        {
        this(null);
        }

    /**
    * Construct the DeserializationAccelerator.
    *
    * @param extractor  the extractor used by this extractor to create a
    *                   {@link ForwardOnlyMapIndex}; note that the created index
    *                   will be associated with this extractor in the given
    *                   index map; passing null is equivalent to using the
    *                   {@link IdentityExtractor}
    */
    public DeserializationAccelerator(ValueExtractor extractor)
        {
        this(extractor, false);
        }

    /**
    * Construct the DeserializationAccelerator.
    *
    * @param extractor  the extractor used by this extractor to create a
    *                   {@link ForwardOnlyMapIndex}; note that the created index
    *                   will be associated with this extractor in the given
    *                   index map; passing null is equivalent to using the
    *                   {@link IdentityExtractor}
    * @param fOnDemand  if true, the forward index will be created "on-demand"
    *                   as the values are attempted to be accessed; otherwise
    *                   the forward index is populated proactively (default)
    */
    public DeserializationAccelerator(ValueExtractor extractor, boolean fOnDemand)
        {
        m_extractor = extractor == null ? IdentityExtractor.INSTANCE : extractor;
        m_fLazy     = fOnDemand;
        }


    // ----- IndexAwareExtractor interface ----------------------------------

    /**
    * {@inheritDoc}
    */
    public MapIndex createIndex(boolean fOrdered, Comparator comparator,
            Map mapIndex, BackingMapContext ctx)
        {
        ValueExtractor extractor = m_extractor;
        MapIndex       index     = (MapIndex) mapIndex.get(extractor);

        if (index != null)
            {
            if (index.equals(this))
                {
                return null;
                }
            throw new IllegalArgumentException(
                    "Repetitive addIndex call for " + this);
            }

        ForwardOnlyMapIndex indexNew = new ForwardOnlyMapIndex(extractor, ctx, m_fLazy);

        mapIndex.put(extractor, indexNew);

        return indexNew;
        }

    /**
    * {@inheritDoc}
    */
    public MapIndex destroyIndex(Map mapIndex)
        {
        return (MapIndex) mapIndex.remove(m_extractor);
        }


    // ----- ValueExtractor interface ---------------------------------------

    /**
    * Using a DeserializationAccelerator to extract values in not supported.
    *
    * @throws UnsupportedOperationException always
    */
    public Object extract(Object oTarget)
        {
        throw new UnsupportedOperationException(
            "DeserializationAccelerator may not be used as an extractor.");
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_extractor = readObject(in);
        m_fLazy     = in.readBoolean();
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        writeObject(out, m_extractor);
        out.writeBoolean(m_fLazy);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_extractor = in.readObject(0);
        m_fLazy     = in.readBoolean(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_extractor);
        out.writeBoolean(1, m_fLazy);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean equals(Object o)
        {
        if (o != null && o.getClass() == getClass())
            {
            DeserializationAccelerator that = (DeserializationAccelerator) o;
            return equals(this.m_extractor, that.m_extractor) &&
                          this.m_fLazy == that.m_fLazy;
            }

        return false;
        }

    /**
    * {@inheritDoc}
    */
    public int hashCode()
        {
        return m_extractor.hashCode();
        }

    /**
    * Return a human-readable description for this DeserializationAccelerator.
    *
    * @return a String description of the DeserializationAccelerator
    */
    public String toString()
        {
        return "DeserializationAccelerator" +
            "(extractor=" + m_extractor + "; lazy=" + m_fLazy + ")";
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying extractor.
    */
    @JsonbProperty("extractor")
    protected ValueExtractor m_extractor;

    /**
    * The "lazy" flag.
    */
    @JsonbProperty("onDemand")
    protected boolean m_fLazy;
    }
