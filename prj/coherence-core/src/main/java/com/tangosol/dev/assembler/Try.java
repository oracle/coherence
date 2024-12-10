/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;

import java.util.Vector;
import java.util.Enumeration;


/**
* The TRY pseudo op marks the start of a guarded section.
* <p><code><pre>
* JASM op         :  TRY (0xfe)
* JVM byte code(s):  n/a
* Details         :
* </pre></code>
*
* @version 0.50, 06/18/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Try extends Op implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    */
    public Try()
        {
        super(TRY);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * (Internal) Adds a Catch to the Try.
    *
    * @param op  the Catch op
    */
    protected void addCatch(Catch op)
        {
        Catch[] acatchOld = m_acatch;
        int     ccatchOld = acatchOld.length;
        int     ccatchNew = ccatchOld + 1;
        Catch[] acatchNew = new Catch[ccatchNew];
        System.arraycopy(acatchOld, 0, acatchNew, 0, ccatchOld);
        acatchNew[ccatchOld] = op;
        m_acatch = acatchNew;
        }

    /**
    * Get the Catch ops associated with the Try.
    *
    * @return an array of Catch ops
    */
    public Catch[] getCatches()
        {
        return m_acatch;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Try";

    /**
    * An empty array of catches.
    */
    private static final Catch[] EMPTY = new Catch[0];

    /**
    * An array of Catch ops for the Try op.
    */
    private Catch[] m_acatch = EMPTY;
    }
