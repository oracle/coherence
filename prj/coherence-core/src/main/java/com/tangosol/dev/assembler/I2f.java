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
* The I2F simple op converts the integer on the top of the stack to a
* float.
* <p><code><pre>
* JASM op         :  I2F  (0x86)
* JVM byte code(s):  I2F  (0x86)
* Details         :
* </pre></code>
*
* @version 0.50, 06/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class I2f extends Op implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    */
    public I2f()
        {
        super(I2F);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "I2f";
    }
