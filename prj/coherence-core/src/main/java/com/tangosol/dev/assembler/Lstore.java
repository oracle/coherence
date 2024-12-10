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
* The LSTORE variable-size op stores a long variable.
* <p><code><pre>
* JASM op         :  LSTORE    (0x37)
* JVM byte code(s):  LSTORE    (0x37)
*                    LSTORE_0  (0x3f)
*                    LSTORE_1  (0x40)
*                    LSTORE_2  (0x41)
*                    LSTORE_3  (0x42)
* Details         :
* </pre></code>
*
* @version 0.50, 06/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Lstore extends OpStore implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param var  the variable to pop
    */
    public Lstore(Lvar var)
        {
        super(LSTORE, var);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Lstore";
    }
