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
* The FLOAD variable-size op pushes a float variable (one word) onto the
* stack.
* <p><code><pre>
* JASM op         :  FLOAD    (0x17)
* JVM byte code(s):  FLOAD    (0x17)
*                    FLOAD_0  (0x22)
*                    FLOAD_1  (0x23)
*                    FLOAD_2  (0x24)
*                    FLOAD_3  (0x25)
* Details         :
* </pre></code>
*
* @version 0.50, 06/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Fload extends OpLoad implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param var  the variable to push
    */
    public Fload(Fvar var)
        {
        super(FLOAD, var);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Fload";
    }
