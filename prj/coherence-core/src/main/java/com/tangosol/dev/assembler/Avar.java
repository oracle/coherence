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
* The AVAR pseudo-op declares a reference variable.  The variable can
* optionally be named.
* <p><code><pre>
* JASM op         :  AVAR  (0xf0)
* JVM byte code(s):  n/a
* Details         :
* </pre></code>
*
* @version 0.50, 06/18/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Avar extends OpDeclare implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    */
    public Avar()
        {
        super(AVAR, null, null);
        }

    /**
    * Construct the op.
    *
    * @param sName  the name of the variable
    */
    public Avar(String sName)
        {
        super(AVAR, sName, null);
        }

    /**
    * Construct the op.
    *
    * @param sName  the name of the variable
    * @param sSig   the signature of the reference type
    *               ('L' + class.replace('.', '/') + ';')
    */
    public Avar(String sName, String sSig)
        {
        super(AVAR, sName, sSig);
        }

    /**
    * Construct the op.  Used by disassembler.
    *
    * @param iVar   the variable index
    */
    protected Avar(int iVar)
        {
        super(AVAR, null, null, iVar);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Avar";
    }
