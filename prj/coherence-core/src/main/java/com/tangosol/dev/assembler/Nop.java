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
* The NOP simple op does nothing.
* <p><code><pre>
* JASM op         :  NOP (0x00)
* JVM byte code(s):  NOP (0x00)
* Details         :
* </pre></code>
*
* @version 0.50, 06/11/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Nop extends Op implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    */
    public Nop()
        {
        super(NOP);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Nop";
    }
