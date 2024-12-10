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
* The INSTANCEOF op compares the class of the reference on the stack to the
* specified ClassConstant.
* <p><code><pre>
* JASM op         :  INSTANCEOF    (0xc1)
* JVM byte code(s):  INSTANCEOF    (0xc1)
* Details         :
* </pre></code>
*
* @version 0.50, 06/15/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Instanceof extends OpConst implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param constant  the ClassConstant to test a cast to
    */
    public Instanceof(ClassConstant constant)
        {
        super(INSTANCEOF, constant);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Instanceof";
    }
