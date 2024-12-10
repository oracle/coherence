/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


import java.io.IOException;
import java.io.DataOutput;

import java.util.HashSet;


/**
* The RET op returns from a JSR (internal subroutine).
* <p><code><pre>
* JASM op         :  RET       (0xa9??)
* JVM byte code(s):  RET       (0xa9??)
*                    WIDE RET  (0xc4a9????)
* Details         :
* </pre></code>
*
* @version 0.50, 06/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Ret extends Op implements Constants, OpVariable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param var  the Variable to push
    */
    public Ret(Rvar var)
        {
        super(RET);
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
        int n = m_var.getSlot();

        // up to 0xFF uses the normal ret
        if (n <= 0xFF)
            {
            stream.writeByte(RET);
            stream.writeByte(n);
            }
        // otherwise use the WIDE modifier
        else
            {
            stream.writeByte(WIDE);
            stream.writeByte(RET);
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
        setSize(m_var.getSlot() <= 0xFF ? 2 : 4);
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


    // ----- for definite assignment

    /**
    * Get list of variables in-scope and definitely-assigned at the RET.
    *
    * @return  the set of in-scope assigned vars
    */
    protected HashSet getVariables()
        {
        return m_setVars;
        }

    /**
    * Set list of variables in-scope and definitely-assigned at the RET.
    *
    * @param setVars  the set of in-scope assigned vars
    */
    protected void setVariables(HashSet setVars)
        {
        m_setVars = setVars;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Ret";

    /**
    * The variable loaded by this op.
    */
    private Rvar m_var;

    /**
    * What variables are currently in scope and have a value when the RET
    * is encountered.
    */
    private HashSet m_setVars;
    }
