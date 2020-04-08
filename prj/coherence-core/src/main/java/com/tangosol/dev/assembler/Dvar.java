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
* The DVAR pseudo-op declares a double variable.  The variable can
* optionally be named.
* <p><code><pre>
* JASM op         :  DVAR  (0xef)
* JVM byte code(s):  n/a
* Details         :
* </pre></code>
*
* @version 0.50, 06/18/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Dvar extends OpDeclare implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    */
    public Dvar()
        {
        super(DVAR, null, null);
        }

    /**
    * Construct the op.
    *
    * @param sName  the name of the variable
    */
    public Dvar(String sName)
        {
        super(DVAR, sName, null);
        }

    /**
    * Construct the op.  Used by disassembler.
    *
    * @param iVar   the variable index
    */
    protected Dvar(int iVar)
        {
        super(DVAR, null, null, iVar);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Dvar";
    }
