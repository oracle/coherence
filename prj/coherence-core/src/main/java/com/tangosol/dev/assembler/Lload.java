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
* The LLOAD variable-size op pushes a long variable (two words) onto the
* stack.
* <p><code><pre>
* JASM op         :  LLOAD    (0x16)
* JVM byte code(s):  LLOAD    (0x16)
*                    LLOAD_0  (0x1e)
*                    LLOAD_1  (0x1f)
*                    LLOAD_2  (0x20)
*                    LLOAD_3  (0x21)
* Details         :
* </pre></code>
*
* @version 0.50, 06/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Lload extends OpLoad implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param var  the variable to push
    */
    public Lload(Lvar var)
        {
        super(LLOAD, var);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Lload";
    }
