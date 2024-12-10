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
* The DCONST variable-size op pushes a double-precision floating point
* constant onto the stack.
* <p><code><pre>
* JASM op         :  DCONST    (0xe8)
* JVM byte code(s):  DCONST_0  (0x0e)
*                    DCONST_1  (0x0f)
*                    LDC2_W    (0x14????)
* Details         :
* </pre></code>
*
* @version 0.50, 06/11/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Dconst extends Op implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param dfl  the double value
    */
    public Dconst(double dfl)
        {
        this(new DoubleConstant(dfl));
        }

    /**
    * Construct the op.
    *
    * @param constant  the DoubleConstant to push
    */
    public Dconst(DoubleConstant constant)
        {
        super(DCONST);
        m_constant = constant;

        if (constant == null)
            {
            throw new IllegalArgumentException(CLASS + ":  Constant must not be null!");
            }
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the byte code.
    *
    * @return a string describing the byte code
    */
    public String toString()
        {
        return format(null, getName() + ' ' + m_constant.format(), null);
        }

    /**
    * Produce a human-readable string describing the byte code.
    *
    * @return a string describing the byte code
    */
    public String toJasm()
        {
        return getName() + ' ' + m_constant.format();
        }


    // ----- VMStructure operations -----------------------------------------

    /**
    * The pre-assembly step collects the necessary entries for the constant
    * pool.  During this step, all constants used by this VM structure and
    * any sub-structures are registered with (but not yet bound by position
    * in) the constant pool.
    *
    * @param pool  the constant pool for the class which needs to be
    *              populated with the constants required to build this
    *              VM structure
    */
    protected void preassemble(ConstantPool pool)
        {
        // 0 and 1 are optimizable
        DoubleConstant constant = m_constant;
        double n = constant.getValue();
        if (n != 0.0 && n != 1.0)
            {
            pool.registerConstant(constant);
            }
        }

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
        DoubleConstant constant = m_constant;
        double n = constant.getValue();

        // 0 and 1 are optimizable
        if (n == 0.0 || n == 1.0)
            {
            stream.writeByte(DCONST_0 + (int)n);
            }
        else
            {
            stream.writeByte(LDC2_W);
            stream.writeShort(pool.findConstant(constant));
            }
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
        double n = m_constant.getValue();
        setSize(n == 0.0 || n == 1.0 ? 1 : 3);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Dconst";

    /**
    * The double-precision floating point constant loaded by this op.
    */
    private DoubleConstant m_constant;
    }
