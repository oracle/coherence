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
* The LVAR pseudo-op declares a long variable.  The variable can
* optionally be named.
* <p><code><pre>
* JASM op         :  LVAR  (0xed)
* JVM byte code(s):  n/a
* Details         :
* </pre></code>
*
* @version 0.50, 06/18/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Lvar extends OpDeclare implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    */
    public Lvar()
        {
        super(LVAR, null, null);
        }

    /**
    * Construct the op.
    *
    * @param sName  the name of the variable
    */
    public Lvar(String sName)
        {
        super(LVAR, sName, null);
        }

    /**
    * Construct the op.  Used by disassembler.
    *
    * @param iVar   the variable index
    */
    protected Lvar(int iVar)
        {
        super(LVAR, null, null, iVar);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Lvar";
    }
