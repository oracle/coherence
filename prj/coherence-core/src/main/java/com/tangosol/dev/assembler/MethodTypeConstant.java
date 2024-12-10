/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.assembler;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
* MethodTypeConstant represents a method type.
* <p>
* The MethodType Constant was defined by JDK 1.7 under bytecode
* version 51.0. The structure is defined as:
* <p>
* <code><pre>
* CONSTANT_MethodType_info
*     {
*     u1 tag;
*     u2 descriptor_index;
*     }
* </pre></code>
*
* @author hr  2012.08.06
*/
public class MethodTypeConstant
        extends Constant
    {

    // ----- constructors ---------------------------------------------------

    /**
    * Construct a MethodTypeConstant (tag = 16).
    */
    protected MethodTypeConstant()
        {
        super(CONSTANT_METHODTYPE);
        }

    // ----- Constant methods -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    protected void disassemble(DataInput stream, ConstantPool pool) throws IOException
        {
        m_nDescriptorIndex = stream.readUnsignedShort();
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected void postdisassemble(ConstantPool pool)
        {
        m_descriptor = (UtfConstant) pool.getConstant(m_nDescriptorIndex);
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected void preassemble(ConstantPool pool)
        {
        pool.registerConstant(m_descriptor);
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected void assemble(DataOutput stream, ConstantPool pool) throws IOException
        {
        super.assemble(stream, pool);

        stream.writeShort(pool.findConstant(m_descriptor));
        }

    // ----- Comparable methods ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public int compareTo(Object obj)
        {
        return obj instanceof MethodTypeConstant
                ? m_descriptor.compareTo(((MethodTypeConstant) obj).m_descriptor)
                : 1;
        }

    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public String toString()
        {
        return "(MethodType)->[descriptor = " + m_descriptor + "]";
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public String format()
        {
        return m_descriptor.format();
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean equals(Object obj)
        {
        return obj instanceof MethodTypeConstant
                ? m_descriptor.equals(((MethodTypeConstant) obj).m_descriptor)
                : false;
        }

    // ----- accessors ------------------------------------------------------

    /**
    * Returns the method description.
    *
    * @return the method description
    */
    public UtfConstant getDescriptor()
        {
        return m_descriptor;
        }

    /**
    * Sets the method description.
    *
    * @return the method description
    */
    public void setDescriptor(UtfConstant descriptor)
        {
        m_descriptor = descriptor;
        }

    // ----- data members ---------------------------------------------------

    /**
    * The ConstantPool index of the method description.
    */
    private int         m_nDescriptorIndex;

    /**
    * The Constant that describes the method.
    */
    private UtfConstant m_descriptor;
    }