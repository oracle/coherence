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
* The DLOAD variable-size op pushes a double variable (two words) onto the
* stack.
* <p><code><pre>
* JASM op         :  DLOAD    (0x18)
* JVM byte code(s):  DLOAD    (0x18)
*                    DLOAD_0  (0x26)
*                    DLOAD_1  (0x27)
*                    DLOAD_2  (0x28)
*                    DLOAD_3  (0x29)
* Details         :
* </pre></code>
*
* @version 0.50, 06/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Dload extends OpLoad implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param var  the variable to push
    */
    public Dload(Dvar var)
        {
        super(DLOAD, var);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Dload";
    }
