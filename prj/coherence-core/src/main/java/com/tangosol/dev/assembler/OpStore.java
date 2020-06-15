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
* This abstract class implements ISTORE, LSTORE, FSTORE, DSTORE, and ASTORE.
* Additionally, the pseudo op RSTORE is supported, assembling to ASTORE.
*
* @version 0.50, 06/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public abstract class OpStore extends Op implements Constants, OpVariable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param iOp  the op value
    * @param var  the variable to store
    */
    public OpStore(int iOp, OpDeclare var)
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

        // RSTORE is not a real op -- return address actually uses ASTORE
        if (iOp == RSTORE)
            {
            iOp = ASTORE;
            }

        // 0..3 are optimized to a byte code
        if (n <= 3)
            {
            // luckily this is just math ... the ops are symmetrical
            stream.writeByte((iOp - ISTORE) * 4 + ISTORE_0 + n);
            }
        // up to 0xFF uses the normal store
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
        // up to 0xFF uses the normal store
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
    * Determine the variable type stored by this op.
    *
    * @return the variable type stored by the op
    *         ('I', 'L', 'F', 'D', 'A', or 'R')
    */
    public char getType()
        {
        int iOp = super.getValue();
        return iOp == RSTORE ? 'R' : TYPES[iOp - ISTORE];
        }

    /**
    * Determine the variable affected by this op.
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
    private static final String CLASS = "OpStore";

    /**
    * Variable types.
    */
    private static final char[] TYPES = {'I','L','F','D','A','R'};

    /**
    * The variable stored by this op.
    */
    private OpDeclare m_var;
    }
