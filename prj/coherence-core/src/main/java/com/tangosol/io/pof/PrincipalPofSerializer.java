/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import java.io.IOException;

import java.security.Principal;


/**
* PofSerializer implementation that can serialize and deserialize a
* {@link Principal} to/from a POF stream.
* <p>
* The PrincipalPofSerializer can serialize any Principal implementation to a
* POF stream; however, the Principals returned during deserialization are
* always instances of the {@link PofPrincipal} class.
*
* @author jh  2008.08.12
*/
public class PrincipalPofSerializer
        implements PofSerializer
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public PrincipalPofSerializer()
        {
        }


    // ----- PofSerializer implementation -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void serialize(PofWriter out, Object o)
            throws IOException
        {
        Principal principal = (Principal) o;

        out.writeString(0, principal.getName());
        out.writeRemainder(null);
        }

    /**
    * {@inheritDoc}
    */
    public Object deserialize(PofReader in)
            throws IOException
        {
        Object o = new PofPrincipal(in.readString(0));
        in.registerIdentity(o);
        in.readRemainder();

        return o;
        }
    }
