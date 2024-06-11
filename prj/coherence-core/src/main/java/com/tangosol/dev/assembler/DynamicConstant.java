/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.assembler;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * DynamicConstant represents a dynamically computed constant,
 * an arbitrary primitive or reference value produced by the invocation
 * of a dynamic call site.
 * <p>
 * The Dynamic Constant was defined by JDK 11 under bytecode
 * version {@code 55.0}. The structure is defined as:
 * <p>
 * <pre>
 * {@code
 * CONSTANT_InvokeDynamic_info
 *     {
 *     u1 tag;
 *     u2 bootstrap_method_attr_index;
 *     u2 name_and_type_index;
 *     }
 * }
 * </pre>
 *
 * @author rl  2024.06.10
 */
public class DynamicConstant
        extends Constant
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an InvokeDynamicConstant instance (tag = 18).
     */
    protected DynamicConstant()
        {
        this(0, null);
        }

    /**
     * Construct an InvokeDynamicConstant instance (tag = 18).
     *
     * @param nBootstrapMethodIndex an index into the BootstrapMethods
     *                              ClassFile attribute
     * @param methodNameDesc        {@link SignatureConstant} representing a
     *                              method name and signature
     */
    public DynamicConstant(int nBootstrapMethodIndex, SignatureConstant methodNameDesc)
        {
        super(CONSTANT_DYNAMIC);

        m_nBootstrapIndex = nBootstrapMethodIndex;
        m_methodNameDesc = methodNameDesc;
        }

    // ----- Constant methods -----------------------------------------------

    @Override
    protected void disassemble(DataInput stream, ConstantPool pool) throws
                                                                    IOException
        {
        m_nBootstrapIndex      = stream.readUnsignedShort();
        m_nMethodNameDescIndex = stream.readUnsignedShort();
        }

    @Override
    protected void postdisassemble(ConstantPool pool)
        {
        m_methodNameDesc = (SignatureConstant) pool.getConstant(m_nMethodNameDescIndex);
        }

    @Override
    protected void preassemble(ConstantPool pool)
        {
        pool.registerConstant(m_methodNameDesc);
        }

    @Override
    protected void assemble(DataOutput stream, ConstantPool pool)
            throws IOException
        {
        super.assemble(stream, pool);

        stream.writeShort(m_nBootstrapIndex);
        stream.writeShort(pool.findConstant(m_methodNameDesc));
        }

    // ----- Comparable methods ---------------------------------------------

    @Override
    public int compareTo(Object obj)
        {
        return obj instanceof DynamicConstant
               ? ((DynamicConstant) obj).m_nBootstrapIndex - m_nBootstrapIndex
               : 1;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "(Dynamic)->[bootstrap_method_attr_index = " + m_nBootstrapIndex + ", "
               + "method_name_desc = " + m_methodNameDesc + "]";
        }

    @Override
    public String format()
        {
        return m_methodNameDesc.format();
        }

    @Override
    public boolean equals(Object obj)
        {
        return obj instanceof DynamicConstant
               && m_nBootstrapIndex == ((DynamicConstant) obj).m_nBootstrapIndex
               && m_methodNameDesc.equals(((DynamicConstant) obj).m_methodNameDesc);
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
     * @param nBootstrapMethodIndex an index into the BootstrapMethods
     *                              ClassFile Attribute
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
     * @param methodNameAndDescription method name and signature represented
     *                                 by a SignatureConstant
     */
    public void setMethodNameAndDescription(SignatureConstant methodNameAndDescription)
        {
        m_methodNameDesc = methodNameAndDescription;
        }

    // ----- data members ---------------------------------------------------

    /**
     * An index into the BootstrapMethods ClassFile Attribute.
     */
    private int m_nBootstrapIndex;

    /**
     * A ConstantPool index of the {@link SignatureConstant} describing the
     * method name and signature.
     */
    private int m_nMethodNameDescIndex;

    /**
     * The method name and signature represented by a SignatureConstant
     */
    private SignatureConstant m_methodNameDesc;
    }
