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
* The CHECKCAST op compares the class of the reference on the stack to the
* specified ClassConstant.
* <p><code><pre>
* JASM op         :  CHECKCAST    (0xc0)
* JVM byte code(s):  CHECKCAST    (0xc0)
* Details         :
* </pre></code>
*
* @version 0.50, 06/15/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Checkcast extends OpConst implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param constant  the ClassConstant to cast to
    */
    public Checkcast(ClassConstant constant)
        {
        super(CHECKCAST, constant);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Checkcast";
    }
