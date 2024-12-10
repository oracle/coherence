/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* SimplePartitionKey is a trivial {@link
* KeyPartitioningStrategy.PartitionAwareKey PartitionAwareKey} implementation.
* <p>
* This key could also be used in conjunction with {@link
* com.tangosol.util.CompositeKey CompositeKey} explicitly associate a partition
* with custom keys. For example:
* <pre>
* new CompositeKey(SimplePartitionKey.getPartitionKey(nPartition), oKeyNatural);
* </pre>
*
* @author rhl 2011.02.10
* @since  Coherence 3.7
*/
public class SimplePartitionKey
        implements KeyPartitioningStrategy.PartitionAwareKey,
                   ExternalizableLite, PortableObject
    {
    /**
    * Default constructor is defined for serialization purposes only.  The
    * {@link #getPartitionKey} factory method should be used to obtain a
    * SimplePartitionKey.
    */
    public SimplePartitionKey()
        {
        }

    /**
    * Construct a SimplePartitionKey representing the specified partition.
    *
    * @param nPartition  the partition to create a key for
    */
    protected SimplePartitionKey(int nPartition)
        {
        m_nPartition = nPartition;
        }


    // ----- Factory methods ------------------------------------------------

    /**
    * Factory method for a SimplePartitionKey.
    *
    * @param nPartition  the partition to create a key for
    *
    * @return a SimplePartitionKey for the specified partition
    */
    public static SimplePartitionKey getPartitionKey(int nPartition)
        {
        return new SimplePartitionKey(nPartition);
        }


    // ----- PartitionAwareKey interface ------------------------------------

    /**
    * {@inheritDoc}
    */
    public int getPartitionId()
        {
        return m_nPartition;
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_nPartition = ExternalizableHelper.readInt(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeInt(out, m_nPartition);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_nPartition = in.readInt(0);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeInt(0, m_nPartition);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean equals(Object o)
        {
        return o instanceof SimplePartitionKey &&
            ((SimplePartitionKey) o).m_nPartition == m_nPartition;
        }

    /**
    * {@inheritDoc}
    */
    public int hashCode()
        {
        return m_nPartition;
        }

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "SimplePartitionKey(" + m_nPartition + ")";
        }


    // ----- data fields ----------------------------------------------------

    /**
    * The partition id.
    */
    @JsonbProperty("partition")
    protected int m_nPartition;
    }