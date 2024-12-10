/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


import java.io.IOException;
import java.io.DataOutput;


/**
* The ACONST variable-size op pushes a reference constant onto the stack.
* <p><code><pre>
* JASM op         :  ACONST      (0xe9)
* JVM byte code(s):  ACONST_NULL (0x01)
*                    LDC         (0x12??)
*                    LDC_W       (0x13????)
* Details         :  The only non-null reference constants are of the string
*                    or class type (StringConstant or ClassConstant).
*                    This usage of ClassConstant was introduced in JDK 1.5
*                    (Classfile version 49.0).
* </pre></code>
*
* @version 0.50, 06/11/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Aconst extends Op implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    */
    public Aconst()
        {
        super(ACONST);
        }

    /**
    * Construct the op.
    *
    * @param s  a reference constant to push; a String or null
    */
    public Aconst(String s)
        {
        this(s == null ? null : new StringConstant(s));
        }

    /**
    * Construct the op.
    *
    * @param constant  a reference constant to push; a ClassConstant or null
    */
    public Aconst(ClassConstant constant)
        {
        super(ACONST);
        m_constant = constant;
        }

    /**
    * Construct the op.
    *
    * @param constant  a reference constant to push; a StringConstant or null
    */
    public Aconst(StringConstant constant)
        {
        super(ACONST);
        m_constant = constant;
        }

    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the byte code.
    *
    * @return a string describing the byte code
    */
    public String toString()
        {
        String sConst = (m_constant == null ? "null" : m_constant.format());
        return format(null, getName() + ' ' + sConst, null);
        }

    /**
    * Produce a human-readable string describing the byte code.
    *
    * @return a string describing the byte code
    */
    public String toJasm()
        {
        String sConst = (m_constant == null ? "null" : m_constant.format());
        return getName() + ' ' + sConst;
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
        Constant constant = m_constant;
        if (constant != null)
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
        Constant constant = m_constant;
        if (constant == null)
            {
            stream.writeByte(ACONST_NULL);
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
        Constant constant = m_constant;
        if (constant == null)
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
    private static final String CLASS = "Aconst";

    /**
    * The constant loaded by this op, or null.
    */
    private Constant m_constant;
    }
