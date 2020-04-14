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
* The ALOAD variable-size op pushes a reference variable (one word) onto the
* stack.
* <p><code><pre>
* JASM op         :  ALOAD    (0x19)
* JVM byte code(s):  ALOAD    (0x19)
*                    ALOAD_0  (0x2a)
*                    ALOAD_1  (0x2b)
*                    ALOAD_2  (0x2c)
*                    ALOAD_3  (0x2d)
* Details         :
* </pre></code>
*
* @version 0.50, 06/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Aload extends OpLoad implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param var  the variable to push
    */
    public Aload(Avar var)
        {
        super(ALOAD, var);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Aload";
    }
