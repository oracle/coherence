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
* This abstract class implements the field operators (GETSTATIC, PUTSTATIC,
* GETFIELD, and PUTFIELD), the method operators (INVOKEVIRTUAL,
* INVOKESPECIAL, INVOKESTATIC, and INVOKEINTERFACE), NEW, ANEWARRAY,
* CHECKCAST, INSTANCEOF, and MULTIANEWARRAY.
*
* @version 0.50, 06/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public abstract class OpConst extends Op implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param iOp       the op value
    * @param constant  the Constant referenced by the op
    */
    public OpConst(int iOp, Constant constant)
        {
        super(iOp);
        m_constant = constant;

        if (constant == null)
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Constant must not be null!");
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
        pool.registerConstant(m_constant);
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
        int iOp    = super.getValue();
        int iConst = pool.findConstant(m_constant);

        stream.writeByte(iOp);
        stream.writeShort(iConst);
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
        // op + 2-byte constant index
        setSize(3);
        }


    // ----- accessors ------------------------------------------------------

    public Constant getConstant()
        {
        return m_constant;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "OpConst";

    /**
    * The Constant referenced by this op.
    */
    private Constant m_constant;
    }
