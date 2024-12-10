/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Arrays;

import javax.json.bind.annotation.JsonbProperty;


/**
* Ownership is a light-weight data structure that contains a partition
* ownership information.
*
* @author gg 2008.06.29
* @since Coherence 3.4
*/
public class Ownership
        extends    ExternalizableHelper
        implements ExternalizableLite, PortableObject, Cloneable
    {
    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public Ownership()
        {
        }

    /**
    * Construct an empty Ownership object with a given backup count.
    *
    * @param cBackups  the number of backups
    */
    public Ownership(int cBackups)
        {
        m_aiOwner = new int[1 + cBackups];
        }

    /**
    * Copy constructor.
    *
    * @param owners  the Ownership object to copy from
    */
    public Ownership(Ownership owners)
        {
        this(owners.getBackupCount());

        setOwners(owners);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the primary owner id for this partition.
    *
    * @return the primary owner id for this partition
    */
    public int getPrimaryOwner()
        {
        return m_aiOwner[0];
        }

    /**
    * Specify a new primary owner id for this partition.
    *
    * @param iOwner  the new primary owner id for this partition
    */
    public void setPrimaryOwner(int iOwner)
        {
        m_aiOwner[0] = iOwner;
        }

    /**
    * Return the owner id for the specified storage for this partition.
    *
    * @param iStore  the storage index (zero for primary)
    *
    * @return the owner id or zero if there is no owner
    */
    public int getOwner(int iStore)
        {
        int[] aiOwner = m_aiOwner;
        return iStore < aiOwner.length ? aiOwner[iStore] : 0;
        }

    /**
    * Return an array containing owner ids for this partition.
    *
    * @return an array containing the owner ids for this partition
    */
    public int[] getOwners()
        {
        return getOwners(null);
        }

    /**
    * Return an array containing owner ids for this partition.
    *
    * @param aiOwner  the array into which the owner ids are to be stored, if
    *                 it is big enough; otherwise a new array will be allocated
    *                 and returned
    *
    * @return an array containing the owner ids for this partition
    */
    public int[] getOwners(int[] aiOwner)
        {
        int[] aiOwnerThis = m_aiOwner;
        int   cLen        = aiOwnerThis.length;

        if (aiOwner != null && aiOwner.length >= cLen)
            {
            System.arraycopy(aiOwnerThis, 0, aiOwner, 0, cLen);
            return aiOwner;
            }
        else
            {
            return aiOwnerThis.clone();
            }
        }

    /**
    * Specify a new owner id for the specified storage for this partition.
    *
    * @param iStore  the storage index (zero for primary)
    * @param iOwner  the new primary owner id or zero if there is no owner
    */
    public void setOwner(int iStore, int iOwner)
        {
        m_aiOwner[iStore] = iOwner;
        }

    /**
    * Set the owner ids for this partition according to the specified
    * array of member-ids.
    *
    * @param aiOwners  the array of member ids, keyed by storage index
    *
    * @throws IllegalArgumentException iff the backup count of the owners
    *         array differs from this
    */
    public void setOwners(int[] aiOwners)
        {
        int[] aiOwnersThis = m_aiOwner;
        int   cStoresThis  = aiOwnersThis.length;
        if (cStoresThis != aiOwners.length)
            {
            throw new IllegalArgumentException("Incompatible backup count");
            }

        System.arraycopy(aiOwners, 0, aiOwnersThis, 0, cStoresThis);
        }

    /**
    * Set the owner ids for this partition according to the specified
    * Ownership.
    *
    * @param owners  the new ownership
    *
    * @throws IllegalArgumentException iff the backup count of the new
    *         ownership differs from this
    */
    public void setOwners(Ownership owners)
        {
        setOwners(owners.m_aiOwner);
        }

    /**
    * Return the backup count for this Ownership object.
    *
    * @return the backup count
    */
    public int getBackupCount()
        {
        return m_aiOwner.length - 1;
        }

    /**
     * Return a human-readable description of this Ownership.
     *
     * @return a human-readable description
     */
    public String getDescription()
        {
        StringBuilder sb      = new StringBuilder("Owners=");
        int[]         aiOwner = m_aiOwner;

        for (int i = 0, c = aiOwner.length; i < c; ++i)
            {
            if (i > 0)
                {
                sb.append(", ");
                }
            sb.append(aiOwner[i]);
            }

        return sb.toString();
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        int cStores = readInt(in);
        azzert(cStores < 1024, "Unexpected partition backup count.");

        int[] aiOwner = new int[cStores];

        for (int i = 0; i < cStores; i++)
            {
            aiOwner[i] = readInt(in);
            }

        m_aiOwner = aiOwner;
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        int[] aiOwner = m_aiOwner;
        int   cStores = aiOwner.length;

        writeInt(out, cStores);
        for (int i = 0; i < cStores; i++)
            {
            writeInt(out, aiOwner[i]);
            }
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_aiOwner = in.readIntArray(0);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeIntArray(0, m_aiOwner);
        }


    // ----- Cloneable interface --------------------------------------------

    /**
    * Create a clone of this Ownership.
    *
    * @return a clone of the Ownership object
    */
    public Object clone()
        {
        try
            {
            Ownership that = (Ownership) super.clone();

            // deep-clone the ownership array
            that.m_aiOwner = (int[]) this.m_aiOwner.clone();

            return that;
            }
        catch (CloneNotSupportedException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Determine the hash code for this Ownership object.
    *
    * @return the hash code for this Ownership object
    */
    public int hashCode()
        {
        return Arrays.hashCode(m_aiOwner);
        }

    /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @param o  the object to test for equality
    *
    * @return <code>true</code> if this object is the same as the given one;
    *         <code>false</code> otherwise.
    */
    public boolean equals(Object o)
        {
        if (o instanceof Ownership)
            {
            if (o == this)
                {
                return true;
                }

            Ownership that = (Ownership) o;
            return equalsDeep(this.m_aiOwner, that.m_aiOwner);
            }

        return false;
        }

    /**
    * Returns a string representation of this Ownership.
    *
    * @return a string representation of this Ownership
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) + "{" + getDescription() + "}";
        }


    // ----- data fields ----------------------------------------------------

    /**
    * An array of member ids representing the partition ownership.
    */
    @JsonbProperty("ownership")
    protected int[] m_aiOwner;
    }