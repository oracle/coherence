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
* SimplePofValue represents POF values which do not contain children (e.g.
* numeric values, strings, etc.)
*
* @author as  2009.02.12
* @since Coherence 3.5
*/
public class SimplePofValue
        extends AbstractPofValue
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a SimplePofValue instance wrapping the supplied buffer.
    *
    * @param valueParent   parent value within the POF stream
    * @param bufValue  buffer containing POF representation of this value
    * @param ctx       POF context to use when reading or writing properties
    * @param of        offset of this value from the beginning of POF stream
    * @param nType     POF type identifier for this value
    */
    public SimplePofValue(PofValue valueParent, ReadBuffer bufValue,
                          PofContext ctx, int of, int nType)
        {
        super(valueParent, bufValue, ctx, of, nType);
        }


    // ----- PofValue interface ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public PofValue getChild(int nIndex)
        {
        // this is a bit opportunistic fix for COH-6330; if the underlying
        // stream does not have any type information we may return null
        // rather than throwing (e.g. the writer called writeInt(0));
        // given the design of the POF stream, that's the best we can do here
        if (getValue() == null)
            {
            return null;
            }
        throw new PofNavigationException("getChild() method cannot be invoked"
                + " on the SimplePofValue instance.");
        }
    }
