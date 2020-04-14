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
* The ILOAD variable-size op pushes an integer variable (one word) onto the
* stack.
* <p><code><pre>
* JASM op         :  ILOAD    (0x15)
* JVM byte code(s):  ILOAD    (0x15)
*                    ILOAD_0  (0x1a)
*                    ILOAD_1  (0x1b)
*                    ILOAD_2  (0x1c)
*                    ILOAD_3  (0x1d)
* Details         :
* </pre></code>
*
* @version 0.50, 06/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Iload extends OpLoad implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param var  the variable to push
    */
    public Iload(Ivar var)
        {
        super(ILOAD, var);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Iload";
    }
