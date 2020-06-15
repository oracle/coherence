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
* Represents the name and type of a Java Virtual Machine method or field.
* This constant type references two UtfConstant instances.
*
* @version 0.50, 05/13/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class SignatureConstant extends Constant implements Constants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Constructor used internally by the Constant class.
    */
    protected SignatureConstant()
        {
        super(CONSTANT_NAMEANDTYPE);
        }

    /**
    * Construct a constant whose values are java strings containing the name
    * of a Java method or field and the method signature or field type.  This
    * constructor is a helper which creates UTF constants from the passed
    * strings.
    *
    * @param sName  the method/field name
    * @param sType  the method signature/field type
    */
    public SignatureConstant(String sName, String sType)
        {
        this(new UtfConstant(sName), new UtfConstant(sType.replace('.', '/')));
        }

    /**
    * Construct a Signature constant which references the passed UTF constant.
    *
    * @param constantName  the referenced UTF constant which contains the
    *                      name of the field/method
    * @param constantType  the referenced UTF constant which contains the
    *                      field type/method signature
    */
    public SignatureConstant(UtfConstant constantName, UtfConstant constantType)
        {
        this();

        if (constantName == null || constantType == null)
            {
            throw new IllegalArgumentException(CLASS + ":  Values cannot be null!");
            }

        m_utfName = constantName;
        m_utfType = constantType;
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
        m_iRefName = stream.readUnsignedShort();
        m_iRefType = stream.readUnsignedShort();
        }

    /**
    * Resolve referenced constants.
    *
    * @param pool  the constant pool containing any constant referenced by
    *              this constant (i.e. referenced by index)
    */
    protected void postdisassemble(ConstantPool pool)
        {
        UtfConstant constName = m_utfName;
        UtfConstant constType = m_utfType;

        if (constName == null || constType == null)
            {
            constName = m_utfName = (UtfConstant) pool.getConstant(m_iRefName);
            constType = m_utfType = (UtfConstant) pool.getConstant(m_iRefType);

            constName.postdisassemble(pool);
            constType.postdisassemble(pool);
            }
        }

    /**
    * Register referenced constants.
    *
    * @param pool  the constant pool to register referenced constants with
    */
    protected void preassemble(ConstantPool pool)
        {
        pool.registerConstant(m_utfName);
        pool.registerConstant(m_utfType);
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
        stream.writeShort(pool.findConstant(m_utfName));
        stream.writeShort(pool.findConstant(m_utfType));
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
        SignatureConstant that = (SignatureConstant) obj;
        int nResult = this.m_utfName.compareTo(that.m_utfName);
        if (nResult == 0)
            {
            nResult = this.m_utfType.compareTo(that.m_utfType);
            }
        return nResult;
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the constant.
    *
    * @return a string describing the constant
    */
    public String toString()
        {
        return "(Signature)->"
                + (m_utfName == null ? "[" + m_iRefName + ']' : m_utfName.toString())
                + ", "
                + (m_utfType == null ? "[" + m_iRefType + ']' : m_utfType.toString());
        }

    /**
    * Format the constant as it would appear in JASM code.
    *
    * @return the constant as it would appear in JASM code
    */
    public String format()
        {
        String sName = m_utfName.format();
        String sType = m_utfType.format();
        if (sType.charAt(0) == '(')
            {
            return sName + sType;
            }
        else
            {
            return sName;
            }
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
            SignatureConstant that = (SignatureConstant) obj;
            return this            == that
                || this.getClass() == that.getClass()
                && this.m_utfName.equals(that.m_utfName)
                && this.m_utfType.equals(that.m_utfType);
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
    * Get the name portion of the constant as a string.
    *
    * @return  the field/method name
    */
    public String getName()
        {
        return m_utfName.getValue();
        }

    /**
    * Get the type portion of the constant as a string.
    *
    * @return  the field type/method signature
    */
    public String getType()
        {
        return m_utfType.getValue();
        }

    /**
    * Get the UTF constant which holds the field/method name.
    *
    * @return  the UTF constant which contains the name
    */
    public UtfConstant getNameConstant()
        {
        return m_utfName;
        }

    /**
    * Get the UTF constant which holds the field type/method signature.
    *
    * @return  the UTF constant which contains the type
    */
    public UtfConstant getTypeConstant()
        {
        return m_utfType;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "SignatureConstant";

    /**
    * The UTF constant referenced by this Signature constant containing the
    * field/method name.
    */
    private UtfConstant m_utfName;

    /**
    * The UTF constant referenced by this Signature constant containing the
    * field type/method signature.
    */
    private UtfConstant m_utfType;

    /**
    * Class pool index of the "name" UTF constant.
    * <p>
    * If this has been disassembled (previous to the "postdisassemble"
    * invocation), the reference to the UTF constant is still by index, as
    * it was in the persistent .class structure.
    */
    private int m_iRefName;

    /**
    * Class pool index of the "type" UTF constant.
    * <p>
    * If this has been disassembled (previous to the "postdisassemble"
    * invocation), the reference to the UTF constant is still by index, as
    * it was in the persistent .class structure.
    */
    private int m_iRefType;
    }
