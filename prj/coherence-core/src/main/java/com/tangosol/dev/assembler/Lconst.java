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
* The LCONST variable-size op pushes a long integer constant onto the stack.
* <p><code><pre>
* JASM op         :  LCONST    (0xe6)
* JVM byte code(s):  LCONST_0  (0x09)
*                    LCONST_1  (0x0a)
*                    LDC2_W    (0x14????)
* Details         :
* </pre></code>
*
* @version 0.50, 06/11/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Lconst extends Op implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param l  the long value
    */
    public Lconst(long l)
        {
        this(new LongConstant(l));
        }

    /**
    * Construct the op.
    *
    * @param constant  the LongConstant to push
    */
    public Lconst(LongConstant constant)
        {
        super(LCONST);
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
        LongConstant constant = m_constant;
        long n = constant.getValue();
        if (n != 0L && n != 1L)
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
        LongConstant constant = m_constant;
        long n = constant.getValue();

        // 0L and 1L are optimizable
        if (n == 0L || n == 1L)
            {
            stream.writeByte(LCONST_0 + (int)n);
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
        long n = m_constant.getValue();
        setSize(n == 0L || n == 1L ? 1 : 3);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Lconst";

    /**
    * The long integer constant loaded by this op.
    */
    private LongConstant m_constant;
    }
