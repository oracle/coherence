/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.util.NullImplementation;

import java.io.IOException;

import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;


/**
* PofSerializer implementation that can serialize and deserialize a
* {@link Subject} to/from a POF stream.
* <p>
* As is the case with Java serialization of a Subject, only the Principals
* associated with the Subject are serialized. Furthermore, a deserialized
* Subject is marked as read-only.
*
* @author jh  2008.08.12
*
* @see PrincipalPofSerializer
*/
public class SubjectPofSerializer
        implements PofSerializer
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public SubjectPofSerializer()
        {
        }


    // ----- PofSerializer interface ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public void serialize(PofWriter out, Object o)
            throws IOException
        {
        Subject subject = (Subject) o;

        out.writeCollection(0, subject.getPrincipals());
        out.writeRemainder(null);
        }

    /**
    * {@inheritDoc}
    */
    public Object deserialize(PofReader in)
            throws IOException
        {
        Set set = (Set) in.readCollection(0, new HashSet());

        Object o = new Subject(false, set, NullImplementation.getSet(),
                NullImplementation.getSet());
        in.registerIdentity(o);
        in.readRemainder();

        return o;
        }
    }
