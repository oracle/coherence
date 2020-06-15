/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.internal;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
* OptimisticRemove is an EntryProcessor that performs an {@link
* com.tangosol.util.InvocableMap.Entry#remove(boolean)} Entry.remove}
* operation if the entry exists and the version identifier associated with
* the session is equal to a specified version identifier. If the entry does
* not already exist, no action is taken.
*
* @author jh  2008.12.16
* @since Coherence 3.5 renamed from OptimisticBinaryRemove
*/
public class SessionOptimisticRemove
        extends AbstractProcessor
        implements ExternalizableLite, PortableObject
    {


    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public SessionOptimisticRemove()
        {
        }

     /**
     * Create a new OptimisticRemove that will remove an entry iff the entry
     * already exists and the version identifier associated with the current
     * value is equal to the specified version identifier.
     *
     * @param nVersion  the expected version identifier; must be >= 0
     */
     public SessionOptimisticRemove(int nVersion)
         {
         this(nVersion,false);
         }
     
     /**
     * Create a new OptimisticRemove that will remove an entry iff the entry
     * already exists and the version identifier associated with the current
     * value is equal to the specified version identifier.
     *
     * @param nVersion  the expected version identifier; must be >= 0
     * @param fSynthetic specifies whether the remove operation is synthetic
     */
     public SessionOptimisticRemove(int nVersion, boolean fSynthetic)
         {
         if (nVersion < 0)
             {
             throw new IllegalArgumentException();
             }

         m_nVersion   = nVersion;
         m_fSynthetic = fSynthetic;
         }


    // ----- EntryProcessor interface ---------------------------------------

    /**
    * Process a Map.Entry object.
    *
    * @param entry  the Entry to process
    *
    * @return the current version identifier (>0) if the remove succeeded or
    *         0 if the entry did not exist; otherwise, the negated value of
    *         the current version identifier (<0)
    */
    public Object process(InvocableMap.Entry entry)
        {
        int nVersionRet = 0;

        if (entry.isPresent())
            {
            // determine the current version identifier
            Binary binSession  = ((BinaryEntry) entry).getBinaryValue();
            int    nVersionOld = SessionOptimisticPut.extractVersion(binSession);
            int    nVersionExp = m_nVersion;
            if (nVersionOld == nVersionExp)
                {
                entry.remove(m_fSynthetic);
                nVersionRet = nVersionOld;
                }
            else
                {
                nVersionRet = -nVersionOld;
                }
            }

        return Integer.valueOf(nVersionRet);
        }


    // ----- ExternalizableLite ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_nVersion   = ExternalizableHelper.readInt(in);
        try 
            {
            m_fSynthetic = ExternalizableHelper.readInt(in) == 0 ? true : false;        
            }
        catch(IOException ex)
            {
            // this block has been left blank intentionally, the try catch block was created so that
            // requests sent by older clients do not break the serialization
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeInt(out, m_nVersion);
        ExternalizableHelper.writeInt(out, m_fSynthetic ? 0 : 1);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_nVersion   = in.readInt(0);
        m_fSynthetic = in.readBoolean(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeInt(0, m_nVersion);
        out.writeBoolean(1, m_fSynthetic);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The expected version identifier.
    */
    public int m_nVersion;
    
    /**
    * Whether the removal must be synthetic or not
    */
    protected boolean m_fSynthetic = false;
    }
