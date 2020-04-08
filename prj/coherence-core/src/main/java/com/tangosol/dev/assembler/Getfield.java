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
* The GETFIELD op accesses an object (instance) field.
* <p><code><pre>
* JASM op         :  GETFIELD    (0xb4)
* JVM byte code(s):  GETFIELD    (0xb4)
* Details         :
* </pre></code>
*
* @version 0.50, 06/15/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Getfield extends OpConst implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param constant  the FieldConstant
    */
    public Getfield(FieldConstant constant)
        {
        super(GETFIELD, constant);
        }


    // ----- Op operations --------------------------------------------------

    /**
    * Returns the effect of the byte code on the height of the stack.
    *
    * @return the number of words pushed (if positive) or popped (if
    *         negative) from the stack by the op
    */
    public int getStackChange()
        {
        // pops object reference
        // pushes value
        return -1 + ((FieldConstant) getConstant()).getVariableSize();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Getfield";
    }
