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
* The IINC variable-size op increments a local integer variable by a short
* value.
* <p><code><pre>
* JASM op         :  IINC       (0x84)
* JVM byte code(s):  IINC       (0x84????)
*                    WIDE IINC  (0xC484????????)
* Details         :
* </pre></code>
*
* @version 0.50, 06/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Iinc extends Op implements Constants, OpVariable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param var   the integer variable
    * @param sInc  the increment
    */
    public Iinc(Ivar var, short sInc)
        {
        super(IINC);
        m_var  = var;
        m_sInc = sInc;

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
        return format(null, getName() + ' ' + m_var.format() + ", " + m_sInc, null);
        }

    /**
    * Produce a human-readable string describing the byte code.
    *
    * @return a string describing the byte code
    */
    public String toJasm()
        {
        return getName() + ' ' + m_var.format() + ", " + m_sInc;
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
        int n = m_var.getSlot();
        int i = m_sInc;

        if (n > 0xFF || i < Byte.MIN_VALUE || i > Byte.MAX_VALUE)
            {
            stream.writeByte(WIDE);
            stream.writeByte(IINC);
            stream.writeShort(n);
            stream.writeShort(i);
            }
        else
            {
            stream.writeByte(IINC);
            stream.writeByte(n);
            stream.writeByte(i);
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
        int i = m_sInc;
        if (m_var.getSlot() > 0xFF || i < Byte.MIN_VALUE || i > Byte.MAX_VALUE)
            {
            setSize(6);
            }
        else
            {
            setSize(3);
            }
        }


    // ----- accessors ------------------------------------------------------

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
    private static final String CLASS = "Iinc";

    /**
    * The integer variable to increment.
    */
    private Ivar m_var;

    /**
    * The integer increment.
    */
    private short m_sInc;
    }
