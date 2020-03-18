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
* The FCONST variable-size op pushes a single-precision floating point
* constant onto the stack.
* <p><code><pre>
* JASM op         :  FCONST    (0xe7)
* JVM byte code(s):  FCONST_0  (0x0b)
*                    FCONST_1  (0x0c)
*                    FCONST_1  (0x0d)
*                    LDC       (0x12??)
*                    LDC_W     (0x13????)
* Details         :
* </pre></code>
*
* @version 0.50, 06/11/98, assembler/dis-assembler
* @version 0.51, 12/04/98, fix to calculateSize
* @author  Cameron Purdy
*/
public class Fconst extends Op implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param fl  the float value
    */
    public Fconst(float fl)
        {
        this(new FloatConstant(fl));
        }

    /**
    * Construct the op.
    *
    * @param constant  the FloatConstant to push
    */
    public Fconst(FloatConstant constant)
        {
        super(FCONST);
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
        // 0F, 1F and 2F are optimizable
        FloatConstant constant = m_constant;
        float n = constant.getValue();
        if (n != 0F && n != 1F && n != 2F)
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
        FloatConstant constant = m_constant;
        float n = constant.getValue();

        // 0F, 1F, and 2F are optimizable
        if (n == 0F || n == 1F || n == 2F)
            {
            stream.writeByte(FCONST_0 + (int)n);
            }
        else
            {
            int iConst = pool.findConstant(constant);
            if (iConst <= 0xFF)
                {
                stream.writeByte(LDC);
                stream.writeByte(iConst);
                }
            else
                {
                stream.writeByte(LDC_W);
                stream.writeShort(iConst);
                }
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
        FloatConstant constant = m_constant;
        float         fl       = constant.getValue();

        if (fl == 0F || fl == 1F || fl == 2F)
            {
            setSize(1);
            }
        else if (pool.findConstant(constant) <= 0xFF)
            {
            setSize(2);
            }
        else
            {
            setSize(3);
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Fconst";

    /**
    * The single-precision floating point constant loaded by this op.
    */
    private FloatConstant m_constant;
    }
