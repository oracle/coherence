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
* Represents the name of a Java Virtual Machine class.  This constant type
* references a UtfConstant.
*
* @version 0.50, 05/13/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class ClassConstant extends Constant implements Constants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Constructor used internally by the Constant class.
    */
    protected ClassConstant()
        {
        super(CONSTANT_CLASS);
        }

    /**
    * Construct a constant whose value is a java string containing the name
    * of a Java class.  This constructor is a helper which creates a UTF
    * constant from the passed string.
    *
    * @param sClass  the class name
    */
    public ClassConstant(String sClass)
        {
        this(new UtfConstant(sClass.replace('.', '/')));
        }

    /**
    * Construct a Class constant which references the passed UTF constant.
    *
    * @param constant  the referenced UTF constant which contains the value
    *                  of the Class constant
    */
    public ClassConstant(UtfConstant constant)
        {
        this();

        if (constant == null)
            {
            throw new IllegalArgumentException(CLASS + ":  Value cannot be null!");
            }

        m_utf = constant;
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
        m_iRef = stream.readUnsignedShort();
        }

    /**
    * Resolve referenced constants.
    *
    * @param pool  the constant pool containing any constant referenced by
    *              this constant (i.e. referenced by index)
    */
    protected void postdisassemble(ConstantPool pool)
        {
        UtfConstant constUtf = m_utf;
        if (constUtf == null)
            {
            constUtf = m_utf = (UtfConstant) pool.getConstant(m_iRef);

            constUtf.postdisassemble(pool);
            }
        }

    /**
    * Register referenced constants.
    *
    * @param pool  the constant pool to register referenced constants with
    */
    protected void preassemble(ConstantPool pool)
        {
        pool.registerConstant(m_utf);
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
        stream.writeShort(pool.findConstant(m_utf));
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
        ClassConstant that = (ClassConstant) obj;
        return this.m_utf.compareTo(that.m_utf);
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the constant.
    *
    * @return a string describing the constant
    */
    public String toString()
        {
        return "(Class)->" + (m_utf == null ? "[" + m_iRef + ']' : m_utf.toString());
        }

    /**
    * Format the constant as it would appear in JASM code.
    *
    * @return the constant as it would appear in JASM code
    */
    public String format()
        {
        String sClass = m_utf.format();
        return sClass.replace('/', '.');
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
            ClassConstant that = (ClassConstant) obj;
            return this            == that
                || this.getClass() == that.getClass()
                && this.m_utf.equals(that.m_utf);
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
    * Get the class name as it appears in Java source.
    *
    * @return  the class name
    */
    public String getJavaName()
        {
        return m_utf.getValue().replace('/', '.');
        }

    /**
    * Get the string value (class name) of the constant.
    *
    * @return  the class name
    */
    public String getValue()
        {
        return m_utf.getValue();
        }

    /**
    * Get the UTF constant which holds the class name.
    *
    * @return  the UTF constant referenced from this constant
    */
    public UtfConstant getValueConstant()
        {
        return m_utf;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "ClassConstant";

    /**
    * The UTF constant referenced by this Class constant.
    */
    private UtfConstant m_utf;

    /**
    * If this has been disassembled (previous to the "postdisassemble"
    * invocation), the reference to the UTF constant is still by index, as
    * it was in the persistent .class structure.
    */
    private int m_iRef;
    }
