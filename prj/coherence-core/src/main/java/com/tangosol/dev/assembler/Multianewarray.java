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
* The MULTIANEWARRAY op creates an multi-dimensioned array of references of
* a type specified by the ClassConstant.
* <p><code><pre>
* JASM op         :  MULTIANEWARRAY    (0xc5)
* JVM byte code(s):  MULTIANEWARRAY    (0xc5)
* Details         :
* </pre></code>
*
* @version 0.50, 06/15/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Multianewarray extends OpConst implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param constant  the ClassConstant
    * @param cDims     the number of dimensions
    */
    public Multianewarray(ClassConstant constant, int cDims)
        {
        super(MULTIANEWARRAY, constant);
        m_cDims = cDims;

        if (cDims < 0x01 || cDims > 0xFF)
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Dimensions must be in the range 0x01..0xFF!");
            }
        }


    // ----- VMStructure operations -----------------------------------------

    /**
    * The assembly process assembles and writes the structure to the passed
    * output stream, resolving any dependencies using the passed constant
    * pool.
    *
    * @param stream  the stream implementing java.io.DataOutput to which to
    *                write the assembled VM structure
    * @param pool    the constant pool for the class which by this point
    *                contains the entire set of constants required to build
    *                this VM structure
    */
    protected void assemble(DataOutput stream, ConstantPool pool)
            throws IOException
        {
        ClassConstant constant = (ClassConstant) super.getConstant();
        stream.writeByte(MULTIANEWARRAY);
        stream.writeShort(pool.findConstant(constant));
        stream.writeByte(m_cDims);
        }


    // ----- Op operations --------------------------------------------------

    /**
    * Calculate and set the size of the assembled op based on the offset of
    * the op and the constant pool which is passed.
    *
    * @param pool    the constant pool for the class which by this point
    *                contains the entire set of constants required to build
    *                this VM structure
    */
    protected void calculateSize(ConstantPool pool)
        {
        setSize(4);
        }

    /**
    * Returns the effect of the byte code on the height of the stack.
    *
    * @return the number of words pushed (if positive) or popped (if
    *         negative) from the stack by the op
    */
    public int getStackChange()
        {
        // int size of each dimension is popped
        // resulting array is pushed
        return 1 - m_cDims;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Multianewarray";

    /**
    * The number of dimensions to allocate.
    */
    private int m_cDims;
    }
