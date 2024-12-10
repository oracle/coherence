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
* The PUTSTATIC op stores a class (static) field.
* <p><code><pre>
* JASM op         :  PUTSTATIC    (0xb3)
* JVM byte code(s):  PUTSTATIC    (0xb3)
* Details         :
* </pre></code>
*
* @version 0.50, 06/15/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Putstatic extends OpConst implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param constant  the FieldConstant
    */
    public Putstatic(FieldConstant constant)
        {
        super(PUTSTATIC, constant);
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
        // pops value
        return -((FieldConstant) getConstant()).getVariableSize();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Putstatic";
    }
