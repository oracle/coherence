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
* The ISTORE variable-size op stores an integer variable.
* <p><code><pre>
* JASM op         :  ISTORE    (0x36)
* JVM byte code(s):  ISTORE    (0x36)
*                    ISTORE_0  (0x3b)
*                    ISTORE_1  (0x3c)
*                    ISTORE_2  (0x3d)
*                    ISTORE_3  (0x3e)
* Details         :
* </pre></code>
*
* @version 0.50, 06/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Istore extends OpStore implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param var  the variable to pop
    */
    public Istore(Ivar var)
        {
        super(ISTORE, var);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Istore";
    }
