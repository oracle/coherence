/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import java.io.IOException;

import java.util.HashSet;
import java.util.Set;


/**
* PofSerializer implementation that can serialize and deserialize a Set.
*
* @author jh  2010.09.23
*/
public class SetSerializer
        implements PofSerializer
    {
    /**
    * {@inheritDoc}
    */
    public Object deserialize(PofReader in)
            throws IOException
        {
        Set set = new HashSet();
        in.registerIdentity(set);
        in.readCollection(0, set);
        in.readRemainder();
        return set;
        }

    /**
    * {@inheritDoc}
    */
    public void serialize(PofWriter out, Object o)
            throws IOException
        {
        out.writeCollection(0, (Set) o);
        out.writeRemainder(null);
        }
    }