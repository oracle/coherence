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
* The INVOKESTATIC op invokes a class (static) method.
* <p><code><pre>
* JASM op         :  INVOKESTATIC    (0xb8)
* JVM byte code(s):  INVOKESTATIC    (0xb8)
* Details         :
* </pre></code>
*
* @version 0.50, 06/15/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Invokestatic extends OpConst implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    * <p>
    * As of JDK8 (52.0) InvokeSpecial may call an interface method to support
    * default methods.
    *
    * @param constant  the MethodConstant
    */
    public Invokestatic(MethodConstant constant)
        {
        super(INVOKESTATIC, constant);
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
        // pops parameters
        // pushes return value
        MethodConstant constant = (MethodConstant) getConstant();
        return -constant.getTotalParameterSize() + constant.getVariableSize(0);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Invokestatic";
    }
