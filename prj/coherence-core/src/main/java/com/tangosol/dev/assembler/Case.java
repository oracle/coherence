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
* The CASE op implements a particular switch case.  The CASE op is part of a
* SWITCH, TABLESWITCH, or LOOKUPSWITCH op; although it is implemented as a
* separate op, it does not produce any code on its own; that is the
* responsibility of the various switch ops.
* <p><code><pre>
* JASM op         :  CASE    (0xfd)
* JVM byte code(s):  n/a
* Details         :  Must immediately follow one of the SWITCH variants.
* </pre></code>
*
* @version 0.50, 06/16/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Case extends OpBranch implements Constants, Comparable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param iCase  the integer case value
    * @param label   the label to branch to
    */
    public Case(int iCase, Label label)
        {
        super(CASE, label);
        m_iCase = iCase;
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the byte code.
    *
    * @return a string describing the byte code
    */
    public String toString()
        {
        String sName  = getName();
        String sCase  = String.valueOf(m_iCase);
        String sLabel = getLabel().format();
        return format(null, sName + ' ' + sCase + ' ' + sLabel, null);
        }

    /**
    * Produce a human-readable string describing the byte code.
    *
    * @return a string describing the byte code
    */
    public String toJasm()
        {
        String sName  = getName();
        String sCase  = String.valueOf(m_iCase);
        String sLabel = String.valueOf(getLabel().getOffset());
        return sName + ' ' + sCase + ": goto " + sLabel;
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
        setSize(0);
        }


    // ----- Comparable operations ------------------------------------------

    /**
    * Compares this Object with the specified Object for order.  Returns a
    * negative integer, zero, or a positive integer as this Object is less
    * than, equal to, or greater than the given Object.
    *
    * @param   obj the <code>Object</code> to be compared.
    *
    * @return  a negative integer, zero, or a positive integer as this Object
    *          is less than, equal to, or greater than the given Object.
    *
    * @exception ClassCastException the specified Object's type prevents it
    *            from being compared to this Object.
    */
    public int compareTo(Object obj)
        {
        Case that = (Case) obj;

        int nThis = this.m_iCase;
        int nThat = that.m_iCase;

        return (nThis < nThat ? -1 : (nThis > nThat ? +1 : 0));
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Access the integer case value.
    *
    * @return the integer value of this case
    */
    public int getCase()
        {
        return m_iCase;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Case";

    /**
    * The integer value of this case.
    */
    private int m_iCase;
    }
