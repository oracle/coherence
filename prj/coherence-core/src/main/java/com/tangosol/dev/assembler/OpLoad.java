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
* This abstract class implements ILOAD, LLOAD, FLOAD, DLOAD, and ALOAD.
*
* @version 0.50, 06/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public abstract class OpLoad extends Op implements Constants, OpVariable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param iOp  the op value
    * @param var  the variable to push
    */
    public OpLoad(int iOp, OpDeclare var)
        {
        super(iOp);
        m_var = var;

        if (var == null)
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Variable must not be null!");
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
        return format(null, getName() + ' ' + m_var.format(), null);
        }

    /**
    * Produce a human-readable string describing the byte code.
    *
    * @return a string describing the byte code
    */
    public String toJasm()
        {
        return getName() + ' ' + m_var.format();
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
        int n   = m_var.getSlot();
        int iOp = super.getValue();

        // 0..3 are optimized to a byte code
        if (n <= 3)
            {
            // luckily this is just math ... the ops are symmetrical
            stream.writeByte((iOp - ILOAD) * 4 + ILOAD_0 + n);
            }
        // up to 0xFF uses the normal load
        else if (n <= 0xFF)
            {
            stream.writeByte(iOp);
            stream.writeByte(n);
            }
        // otherwise use the WIDE modifier
        else
            {
            stream.writeByte(WIDE);
            stream.writeByte(iOp);
            stream.writeShort(n);
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
        int n = m_var.getSlot();

        // 0..3 are optimized to a byte code
        if (n <= 3)
            {
            setSize(1);
            }
        // up to 0xFF uses the normal load
        else if (n <= 0xFF)
            {
            setSize(2);
            }
        // otherwise use the WIDE modifier
        else
            {
            setSize(4);
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the variable type loaded by this op.
    *
    * @return the variable type loaded by the op ('I', 'L', 'F', 'D', or 'A')
    */
    public char getType()
        {
        return TYPES[super.getValue() - ILOAD];
        }

    /**
    * Determine the variable loaded by this op.
    *
    * @return the variable
    */
    public OpDeclare getVariable()
        {
        return m_var;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "OpLoad";

    /**
    * Variable types.
    */
    private static final char[] TYPES = {'I','L','F','D','A'};

    /**
    * The variable loaded by this op.
    */
    private OpDeclare m_var;
    }
