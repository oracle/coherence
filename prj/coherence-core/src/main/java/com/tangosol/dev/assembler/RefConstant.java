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
* Represents the class, name and type of a Java Virtual Machine method or
* field.  This constant type supports the byte code get/put operations for
* fields and the invoke operations for methods.
* <p>
* This constant type references a ClassConstant and a SignatureConstant.
*
* @version 0.50, 05/13/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public abstract class RefConstant extends Constant implements Constants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Constructor used internally by the Constant class.
    */
    protected RefConstant(int nTag)
        {
        super(nTag);
        }

    /**
    * Construct a constant which specifies a class/interface field/method.
    * This constructor is a helper which creates the necessary constants
    * from the passed strings.
    *
    * @param sClass  the class name
    * @param sName   the method/field name
    * @param sType   the method signature/field type
    */
    protected RefConstant(int nTag, String sClass, String sName, String sType)
        {
        this(nTag, new ClassConstant(sClass), new SignatureConstant(sName, sType));
        }

    /**
    * Construct a constant which references the passed constants.
    *
    * @param constantClz  the referenced Class constant which contains the
    *                     name of the class
    * @param constantSig  the referenced Signature constant which contains
    *                     the type/name information for the field/method
    */
    protected RefConstant(int nTag, ClassConstant constantClz, SignatureConstant constantSig)
        {
        this(nTag);

        if (constantClz == null || constantSig == null)
            {
            throw new IllegalArgumentException(CLASS + ":  Values cannot be null!");
            }

        m_clz = constantClz;
        m_sig = constantSig;
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
        m_iRefClz = stream.readUnsignedShort();
        m_iRefSig = stream.readUnsignedShort();
        }

    /**
    * Resolve referenced constants.
    *
    * @param pool  the constant pool containing any constant referenced by
    *              this constant (i.e. referenced by index)
    */
    protected void postdisassemble(ConstantPool pool)
        {
        ClassConstant     constClz = m_clz;
        SignatureConstant constSig = m_sig;

        if (constClz == null || constSig == null)
            {
            constClz = m_clz = (ClassConstant)     pool.getConstant(m_iRefClz);
            constSig = m_sig = (SignatureConstant) pool.getConstant(m_iRefSig);

            // post disassemble dependent
            constClz.postdisassemble(pool);
            constSig.postdisassemble(pool);
            }
        }

    /**
    * Register referenced constants.
    *
    * @param pool  the constant pool to register referenced constants with
    */
    protected void preassemble(ConstantPool pool)
        {
        pool.registerConstant(m_clz);
        pool.registerConstant(m_sig);
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
        stream.writeShort(pool.findConstant(m_clz));
        stream.writeShort(pool.findConstant(m_sig));
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
        RefConstant that = (RefConstant) obj;
        int nResult = this.m_clz.compareTo(that.m_clz);
        if (nResult == 0)
            {
            nResult = this.m_sig.compareTo(that.m_sig);
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
        return (m_clz == null ? "[" + m_iRefClz + ']' : m_clz.toString()) + ", " + 
               (m_sig == null ? "[" + m_iRefSig + ']' : m_sig.toString());
        }

    /**
    * Format the constant as it would appear in JASM code.
    *
    * @return the constant as it would appear in JASM code
    */
    public String format()
        {
        return m_clz.format() + '.' + m_sig.format();
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
            RefConstant that = (RefConstant) obj;
            return this            == that
                || this.getClass() == that.getClass()
                && this.m_clz.equals(that.m_clz)
                && this.m_sig.equals(that.m_sig);
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
    * Get the name of the class (or interface).
    *
    * @return  the class name
    */
    public String getClassName()
        {
        return m_clz.getValue();
        }

    /**
    * Get the name of the field/method.
    *
    * @return  the field/method name
    */
    public String getName()
        {
        return m_sig.getName();
        }

    /**
    * Get the type of the field or signature of the method.
    *
    * @return  the field type/method signature
    */
    public String getType()
        {
        return m_sig.getType();
        }

    /**
    * Get the Class constant which holds the class name.
    *
    * @return  the Class constant which contains the class name
    */
    public ClassConstant getClassConstant()
        {
        return m_clz;
        }

    /**
    * Get the signature constant which holds the type and name of the
    * field/method reference.
    *
    * @return  the signature constant which contains the type/name
    */
    public SignatureConstant getSignatureConstant()
        {
        return m_sig;
        }

    /**
    * Get the UTF constant which holds the field/method name.
    *
    * @return  the UTF constant which contains the name
    */
    public UtfConstant getNameConstant()
        {
        return m_sig.getNameConstant();
        }

    /**
    * Get the UTF constant which holds the field type/method signature.
    *
    * @return  the UTF constant which contains the type
    */
    public UtfConstant getTypeConstant()
        {
        return m_sig.getTypeConstant();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "RefConstant";

    /**
    * The class constant representing the class/interface containing the
    * referenced field/method.
    */
    private ClassConstant m_clz;

    /**
    * The type/name information for the field/method.
    */
    private SignatureConstant m_sig;

    /**
    * Class pool index of the class constant.
    * <p>
    * If this has been disassembled (previous to the "postdisassemble"
    * invocation), the reference to the UTF constant is still by index, as
    * it was in the persistent .class structure.
    */
    private int m_iRefClz;

    /**
    * Class pool index of the signature constant.
    * <p>
    * If this has been disassembled (previous to the "postdisassemble"
    * invocation), the reference to the UTF constant is still by index, as
    * it was in the persistent .class structure.
    */
    private int m_iRefSig;
    }
