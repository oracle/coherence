/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.cache.KeyAssociation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* Key class that consists of a primary and secondary component. Two instances
* of CompositeKey are considered to be equal iff both the primary and
* secondary components of the two instances are considered equal.
* Additionally, the hash code of a CompositeKey takes into the consideration
* the hash codes of its two components. Finally, the CompositeKey class
* implements KeyAssociation by returning the primary component.
*
* @param <P>  the type of the primary component
* @param <S>  the type of the secondary component
*
* @author jh  2008.12.11
*/
public class CompositeKey<P, S>
        extends ExternalizableHelper
        implements KeyAssociation<P>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public CompositeKey()
        {
        }

    /**
    * Create a new CompositeKey that consists of the given primary and
    * secondary components.
    *
    * @param primary    the primary key component; must not be null. This is
    *                   also the host key returned by the KeyAssociation
    *                   implementation
    * @param secondary  the secondary key component; must not be null
    */
    public CompositeKey(P primary, S secondary)
        {
        if (primary == null || secondary == null)
            {
            throw new IllegalArgumentException();
            }

        m_primary   = primary;
        m_secondary = secondary;
        }


    // ----- KeyAssociation interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public P getAssociatedKey()
        {
        return m_primary;
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_primary   = readObject(in);
        m_secondary = readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        writeObject(out, m_primary);
        writeObject(out, m_secondary);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_primary   = in.readObject(0);
        m_secondary = in.readObject(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_primary);
        out.writeObject(1, m_secondary);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Returns a hash code value for this object.
    *
    * @return a hash code value for this object.
    */
    public int hashCode()
        {
        int nHash = m_nHash;

        if (nHash == 0)
            {
            nHash = m_nHash = m_primary.hashCode() ^ m_secondary.hashCode();
            }
        return nHash;
        }

    /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @param o  the reference object with which to compare.
    *
    * @return <code>true</code> if this object is the same as the obj
    *         argument; <code>false</code> otherwise.
    */
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }

        if (o instanceof CompositeKey)
            {
            CompositeKey that = (CompositeKey) o;
            return this.m_primary.equals(that.m_primary) &&
                   this.m_secondary.equals(that.m_secondary);
            }

        return false;
        }

    /**
    * Returns a string representation of the object.
    *
    * @return a string representation of the object.
    */
    public String toString()
        {
        return m_primary + ":" + m_secondary;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the primary key component.
    *
    * @return  the primary key component
    */
    public P getPrimaryKey()
        {
        return m_primary;
        }

    /**
    * Return the secondary key component.
    *
    * @return  the secondary key component
    */
    public S getSecondaryKey()
        {
        return m_secondary;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The primary key component.
    */
    @JsonbProperty("primary")
    private P m_primary;

    /**
    * The secondary key component.
    */
    @JsonbProperty("secondary")
    private S m_secondary;

    /**
    * Cached hash code.
    */
    private transient int m_nHash;
    }
