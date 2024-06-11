/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.assembler;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
* InvokeDynamicConstant represents a method invocation to a 'free' method
* bound at runtime by a CallSite as prescribed by the invokedynamic feature.
* <p>
* The InvokeDynamic Constant was defined by JDK 1.7 under bytecode
* version 51.0. The structure is defined as:
* <p>
* <code><pre>
* CONSTANT_InvokeDynamic_info
*     {
*     u1 tag;
*     u2 bootstrap_method_attr_index;
*     u2 name_and_type_index;
*     }
* </pre></code>
*
* @author hr  2012.08.06
*/
public class InvokeDynamicConstant
    extends Constant
    {

    // ----- constructors ---------------------------------------------------

    /**
    * Construct an InvokeDynamicConstant instance (tag = 18).
    */
    protected InvokeDynamicConstant()
        {
        this(0, null);
        }

    /**
    * Construct an InvokeDynamicConstant instance (tag = 18).
    *
    * @param nBootstrapMethodIndex  an index into the BootstrapMethods
    *                               ClassFile attribute
    * @param methodNameDesc         {@link SignatureConstant} representing a
    *                               method name and signature
    */
    public InvokeDynamicConstant(int nBootstrapMethodIndex, SignatureConstant methodNameDesc)
        {
        super(CONSTANT_INVOKEDYNAMIC);

        m_nBootstrapIndex = nBootstrapMethodIndex;
        m_methodNameDesc  = methodNameDesc;
        }

    // ----- Constant methods -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    protected void disassemble(DataInput stream, ConstantPool pool) throws IOException
        {
        m_nBootstrapIndex      = stream.readUnsignedShort();
        m_nMethodNameDescIndex = stream.readUnsignedShort();
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected void postdisassemble(ConstantPool pool)
        {
        m_methodNameDesc = (SignatureConstant) pool.getConstant(m_nMethodNameDescIndex);
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected void preassemble(ConstantPool pool)
        {
        pool.registerConstant(m_methodNameDesc);
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected void assemble(DataOutput stream, ConstantPool pool) throws IOException
        {
        super.assemble(stream, pool);

        stream.writeShort(m_nBootstrapIndex);
        stream.writeShort(pool.findConstant(m_methodNameDesc));
        }

    // ----- Comparable methods ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public int compareTo(Object obj)
        {
        return obj instanceof InvokeDynamicConstant
                ? ((InvokeDynamicConstant) obj).m_nBootstrapIndex - m_nBootstrapIndex
                : 1;
        }

    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public String toString()
        {
        return "(InvokeDynamic)->[bootstrap_method_attr_index = " + m_nBootstrapIndex + ", "
                + "method_name_desc = " + m_methodNameDesc + "]";
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public String format()
        {
        return m_methodNameDesc.format();
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean equals(Object obj)
        {
        return obj instanceof InvokeDynamicConstant
                ? m_nBootstrapIndex == ((InvokeDynamicConstant) obj).m_nBootstrapIndex
                    && m_methodNameDesc.equals(((InvokeDynamicConstant) obj).m_methodNameDesc)
                : false;
        }

    // ----- accessors ------------------------------------------------------

    /**
    * Returns an index into the BootstrapMethods ClassFile Attribute
    * referring to a BootstrapMethod instance.
    *
    * @return an index into the BootstrapMethods ClassFile Attribute
    */
    public int getBootstrapMethodIndex()
        {
        return m_nBootstrapIndex;
        }

    /**
    * Sets an index into the BootstrapMethods ClassFile Attribute
    * referring to a BootstrapMethod instance.
    *
    * @param nBootstrapMethodIndex  an index into the BootstrapMethods
    *                               ClassFile Attribute
    */
    public void setBootstrapMethodIndex(int nBootstrapMethodIndex)
        {
        m_nBootstrapIndex = nBootstrapMethodIndex;
        }

    /**
    * Returns the method name and signature represented by a
    * {@link SignatureConstant}. In version 51.0 this constant has no
    * runtime effect on the linking procedure induced by an invokedynamic
    * instruction.
    *
    * @return method name and signature represented by a SignatureConstant
    */
    public SignatureConstant getMethodNameAndDescription()
        {
        return m_methodNameDesc;
        }

    /**
    * Sets the method name and signature represented by a
    * {@link SignatureConstant}. In version 51.0 this constant has no
    * runtime effect on the linking procedure induced by an invokedynamic
    * instruction.
    *
    * @param methodNameAndDescription  method name and signature represented
    *                                  by a SignatureConstant
    */
    public void setMethodNameAndDescription(SignatureConstant methodNameAndDescription)
        {
        m_methodNameDesc = methodNameAndDescription;
        }

    // ----- data members ---------------------------------------------------

    /**
    * An index into the BootstrapMethods ClassFile Attribute.
    */
    private int               m_nBootstrapIndex;

    /**
    * A ConstantPool index of the {@link SignatureConstant} describing the
    * method name and signature.
    */
    private int               m_nMethodNameDescIndex;

    /**
    * The method name and signature represented by a SignatureConstant
    */
    private SignatureConstant m_methodNameDesc;
    }
