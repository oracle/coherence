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
* The ICONST variable-size op pushes an integer constant onto the stack.
* <p><code><pre>
* JASM op         :  ICONST    (0xe5)
* JVM byte code(s):  ICONST_M1 (0x02)
*                    ICONST_0  (0x03)
*                    ICONST_1  (0x04)
*                    ICONST_2  (0x05)
*                    ICONST_3  (0x06)
*                    ICONST_4  (0x07)
*                    ICONST_5  (0x08)
*                    BIPUSH    (0x10??)
*                    SIPUSH    (0x11????)
*                    LDC       (0x12??)
*                    LDC_W     (0x13????)
* Details         :
* </pre></code>
*
* @version 0.50, 06/11/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Iconst extends Op implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param n  the int value
    */
    public Iconst(int n)
        {
        this(new IntConstant(n));
        }

    /**
    * Construct the op.
    *
    * @param constant  the IntConstant to push
    */
    public Iconst(IntConstant constant)
        {
        super(ICONST);
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
        // the range of "short" is optimizable using SIPUSH
        IntConstant constant = m_constant;
        int n = constant.getValue();
        if (n < Short.MIN_VALUE || n > Short.MAX_VALUE)
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
        IntConstant constant = m_constant;
        int n = constant.getValue();

        // -1 through 5 are optimized to 1-byte codes
        if (n >= -1 && n <= 5)
            {
            stream.writeByte(ICONST_0 + n);
            }
        // the range of "byte" is optimizable using BIPUSH
        else if (n >= Byte.MIN_VALUE && n <= Byte.MAX_VALUE)
            {
            stream.writeByte(BIPUSH);
            stream.writeByte(n);
            }
        // the range of "short" is optimizable using SIPUSH
        else if (n >= Short.MIN_VALUE && n <= Short.MAX_VALUE)
            {
            stream.writeByte(SIPUSH);
            stream.writeShort(n);
            }
        // otherwise use the constant pool
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
        IntConstant constant = m_constant;
        int n = constant.getValue();

        // -1 through 5 are optimized to 1-byte codes
        if (n >= -1 && n <= 5)
            {
            setSize(1);
            }
        // the range of "byte" is optimizable using BIPUSH
        else if (n >= Byte.MIN_VALUE && n <= Byte.MAX_VALUE)
            {
            setSize(2);
            }
        // the range of "short" is optimizable using SIPUSH
        else if (n >= Short.MIN_VALUE && n <= Short.MAX_VALUE)
            {
            setSize(3);
            }
        // otherwise use the constant pool
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
    private static final String CLASS = "Iconst";

    /**
    * The integer constant loaded by this op.
    */
    private IntConstant m_constant;
    }
