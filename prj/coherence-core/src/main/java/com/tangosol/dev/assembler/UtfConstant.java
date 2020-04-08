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
* Represent a Java Virtual Machine character string constant.  Several
* constant types represent a string value, but this constant type actually
* has (as opposed to "references") a string value stored in the .class
* structure in UTF-8 format.
*
* @version 0.50, 05/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class UtfConstant extends Constant implements Constants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Constructor used internally by the Constant class.
    */
    protected UtfConstant()
        {
        super(CONSTANT_UTF8);
        }

    /**
    * Construct a constant whose value is a java string.
    *
    * @param sText  the java string
    */
    public UtfConstant(String sText)
        {
        this();

        if (sText == null)
            {
            throw new IllegalArgumentException(CLASS + ":  Value cannot be null!");
            }

        m_sText = sText;
        }


    // ----- VMStructure operations -----------------------------------------

    /**
    * Read the constant information from the stream.  Since constants can be
    * inter-related, the dependencies are not derefenced until all constants
    * are disassembled; at that point, the constants are resolved using the
    * postdisassemble method.
    *
    * @param stream  the stream implementing java.io.DataInput from which
    *                to read the constant information
    * @param pool    the constant pool for the class which does not yet
    *                contain the constants referenced by this constant
    */
    protected void disassemble(DataInput stream, ConstantPool pool)
            throws IOException
        {
        m_sText = stream.readUTF();
        }

    /**
    * The assembly process assembles and writes the constant to the passed
    * output stream.
    *
    * @param stream  the stream implementing java.io.DataOutput to which to
    *                write the assembled constant
    * @param pool    the constant pool for the class which by this point
    *                contains the entire set of constants required to build
    *                this VM structure
    */
    protected void assemble(DataOutput stream, ConstantPool pool)
            throws IOException
        {
        super.assemble(stream, pool);
        stream.writeUTF(m_sText);
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
        UtfConstant that = (UtfConstant) obj;
        return this.m_sText.compareTo(that.m_sText);
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the constant.
    *
    * @return a string describing the constant
    */
    public String toString()
        {
        return "(Utf) " + toQuotedStringEscape(m_sText);
        }

    /**
    * Format the constant as it would appear in JASM code.
    *
    * @return the constant as it would appear in JASM code
    */
    public String format()
        {
        return m_sText;
        }

    /**
    * Compare this object to another object for equality.
    *
    * @param obj  the other object to compare to this
    *
    * @return true if this object equals that object
    */
    public boolean equals(Object obj)
        {
        try
            {
            UtfConstant that = (UtfConstant) obj;
            return this            == that
                || this.getClass() == that.getClass()
                && this.m_sText.equals(that.m_sText);
            }
        catch (NullPointerException e)
            {
            // obj is null
            return false;
            }
        catch (ClassCastException e)
            {
            // obj is not of this class
            return false;
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the string value of the constant.
    *
    * @return  the constant's string value
    */
    public String getValue()
        {
        return m_sText;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "UtfConstant";

    /**
    * The constant string value.
    */
    private String m_sText;
    }
