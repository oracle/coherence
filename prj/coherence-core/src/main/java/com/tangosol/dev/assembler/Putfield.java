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
* The PUTFIELD op stores an object (instance) field.
* <p><code><pre>
* JASM op         :  PUTFIELD    (0xb5)
* JVM byte code(s):  PUTFIELD    (0xb5)
* Details         :
* </pre></code>
*
* @version 0.50, 06/15/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Putfield extends OpConst implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param constant  the FieldConstant
    */
    public Putfield(FieldConstant constant)
        {
        super(PUTFIELD, constant);
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
        // pops reference, value
        return -1 - ((FieldConstant) getConstant()).getVariableSize();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Putfield";
    }
