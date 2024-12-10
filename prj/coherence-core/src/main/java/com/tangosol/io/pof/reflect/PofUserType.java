/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.reflect;


import com.tangosol.io.ReadBuffer;

import com.tangosol.io.pof.PofContext;


/**
* PofUserType is {@link PofValue} implementation for user types.
*
* @author as  2009.02.12
* @since Coherence 3.5
*/
public class PofUserType
        extends PofSparseArray
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a PofUserType instance wrapping the supplied buffer.
    *
    * @param valueParent parent value within the POF stream
    * @param bufValue    buffer containing POF representation of this value
    * @param ctx         POF context to use when reading or writing properties
    * @param of          offset of this value from the beginning of POF stream
    * @param nType       POF type identifier for this value
    * @param ofChildren  offset of the first child element within this value
    * @param nVersion    data version of this value
    */
    public PofUserType(PofValue valueParent, ReadBuffer bufValue, PofContext ctx,
            int of, int nType, int ofChildren, int nVersion)
        {
        super(valueParent, bufValue, ctx, of, nType, ofChildren);

        m_nVersion = nVersion;
        }


    // ----- public API -----------------------------------------------------

    /**
    * Return the data version of this value.
    *
    * @return the data version of this value
    */
    public int getVersion()
        {
        return m_nVersion;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The data version of this value.
    */
    protected int m_nVersion;
    }
