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
* The DUP2 simple op duplicates the top two words (a long or double) of
* the stack.
* <p><code><pre>
* JASM op         :  DUP2  (0x5c)
* JVM byte code(s):  DUP2  (0x5c)
* Details         :
* </pre></code>
*
* @version 0.50, 06/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Dup2 extends Op implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    */
    public Dup2()
        {
        super(DUP2);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Dup2";
    }
