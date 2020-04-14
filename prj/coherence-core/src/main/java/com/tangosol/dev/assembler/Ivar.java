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


/**
* The IVAR pseudo-op declares an integer variable.  The variable can
* optionally be named.
* <p><code><pre>
* JASM op         :  IVAR  (0xec)
* JVM byte code(s):  n/a
* Details         :
* </pre></code>
*
* @version 0.50, 06/18/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Ivar extends OpDeclare implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    */
    public Ivar()
        {
        super(IVAR, null, null);
        }

    /**
    * Construct the op.
    *
    * @param sName  the name of the variable
    */
    public Ivar(String sName)
        {
        super(IVAR, sName, null);
        }

    /**
    * Construct the op.
    *
    * @param sName  the name of the variable
    * @param sSig   the signature of the reference type
    *               ('Z', 'B', 'C', 'S', or 'I')
    */
    public Ivar(String sName, String sSig)
        {
        super(IVAR, sName, sSig);
        }

    /**
    * Construct the op.  Used by disassembler.
    *
    * @param iVar   the variable index
    */
    protected Ivar(int iVar)
        {
        super(IVAR, null, null, iVar);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Ivar";
    }
