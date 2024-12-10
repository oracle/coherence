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
* The INVOKEVIRTUAL op invokes a virtual method using a reference.
* <p><code><pre>
* JASM op         :  INVOKEVIRTUAL    (0xb6)
* JVM byte code(s):  INVOKEVIRTUAL    (0xb6)
* Details         :
* </pre></code>
*
* @version 0.50, 06/15/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Invokevirtual extends OpConst implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param constant  the MethodConstant
    */
    public Invokevirtual(MethodConstant constant)
        {
        super(INVOKEVIRTUAL, constant);
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
        // pops reference and parameters
        // pushes return value
        MethodConstant constant = (MethodConstant) getConstant();
        return -1 - constant.getTotalParameterSize() + constant.getVariableSize(0);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Invokevirtual";
    }
