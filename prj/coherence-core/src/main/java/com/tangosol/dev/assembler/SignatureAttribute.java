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
* Represents a Java Virtual Machine "Signature" attribute which
* specifies the class/method/field signature.
*
* <p>
* The Signature Attribute is defined by the JDK 1.5 documentation as:
* <p>
* <code><pre>
*   Signature_attribute
*       {
*       u2 attribute_name_index;
*       u4 attribute_length; (=2)
*       u2 signature_index;
*       }
* </pre></code>
*
* @author  rhl 2008.09.23
*/
public class SignatureAttribute extends Attribute implements Constants
    {

    // ----- constructors ---------------------------------------------------

    /**
    * Construct a signature attribute.
    *
    * @param context  the JVM structure containing the attribute
    */
    protected SignatureAttribute(VMStructure context)
        {
        super(context, ATTR_SIGNATURE);
        }


    // ----- VMStructure operations -----------------------------------------

    /**
    * The disassembly process reads the structure from the passed input
    * stream and uses the constant pool to dereference any constant
    * references.
    *
    * @param stream  the stream implementing java.io.DataInput from which
    *                to read the assembled VM structure
    * @param pool    the constant pool for the class which contains any
    *                constants referenced by this VM structure
    */
    protected void disassemble(DataInput stream, ConstantPool pool)
            throws IOException
        {
        stream.readInt();
        m_utfSignature =
            (UtfConstant) pool.getConstant(stream.readUnsignedShort());
        }

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
        pool.registerConstant(super.getNameConstant());
        pool.registerConstant(m_utfSignature);
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
        stream.writeShort(pool.findConstant(super.getNameConstant()));
        stream.writeInt(2);
        stream.writeShort(pool.findConstant(m_utfSignature));
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the attribute.
    *
    * @return a string describing the attribute
    */
    public String toString()
        {
        return super.getName() + '=' + m_utfSignature;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the signature.
    *
    * @return the signature
    */
    public String getSignature()
        {
        return m_utfSignature.getValue();
        }

    /**
    * Set the signature.
    *
    * @param sSignature the signature
    */
    public void setSignature(String sSignature)
        {
        m_utfSignature = new UtfConstant(sSignature);
        m_fModified    = true;
        }

    /**
    * Get the constant holding the signature.
    *
    * @return the signature constant
    */
    public UtfConstant getSignatureConstant()
        {
        return m_utfSignature;
        }

    /**
    * Determine if the attribute has been modified.
    *
    * @return true if the attribute has been modified
    */
    public boolean isModified()
        {
        return m_fModified;
        }

    /**
    * Reset the modified state of the VM structure.
    * <p>
    * This method must be overridden by sub-classes which do not maintain
    * the attribute as binary.
    */
    protected void resetModified()
        {
        m_fModified = false;
        }
    

    // ----- data members ---------------------------------------------------

    /**
    * The signature.
    */
    private UtfConstant m_utfSignature;
        
    /**
    * Has the attribute been modified?
    */
    private boolean     m_fModified;
    }
