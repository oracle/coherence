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
* The ANEWARRAY op creates an array of references of a type specified by the
* ClassConstant.
* <p><code><pre>
* JASM op         :  ANEWARRAY    (0xbd)
* JVM byte code(s):  ANEWARRAY    (0xbd)
* Details         :
* </pre></code>
*
* @version 0.50, 06/15/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Anewarray extends OpConst implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param constant  the ClassConstant
    */
    public Anewarray(ClassConstant constant)
        {
        super(ANEWARRAY, constant);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Anewarray";
    }
