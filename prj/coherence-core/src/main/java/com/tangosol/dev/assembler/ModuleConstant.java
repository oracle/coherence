/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.assembler;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * ModuleConstant represents a module.
 * <p>
 * The Module Constant was defined by JDK 9 under bytecode version 53.0. The
 * structure is defined as:
 * <p>
 * <code><pre>
 * CONSTANT_Module_info
 *     {
 *     u1 tag;
 *     u2 name_index;
 *     }
 * </pre></code>
 *
 * @author Aleks Seovic  2022.04.09
 */
public class ModuleConstant
        extends Constant
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ModuleConstant (tag = 19).
     */
    protected ModuleConstant()
        {
        super(CONSTANT_MODULE);
        }

    // ----- Constant methods -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected void disassemble(DataInput stream, ConstantPool pool)
            throws IOException
        {
        m_nNameIndex = stream.readUnsignedShort();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void postdisassemble(ConstantPool pool)
        {
        m_name = (UtfConstant) pool.getConstant(m_nNameIndex);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void preassemble(ConstantPool pool)
        {
        pool.registerConstant(m_name);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void assemble(DataOutput stream, ConstantPool pool)
            throws IOException
        {
        super.assemble(stream, pool);

        stream.writeShort(pool.findConstant(m_name));
        }

    // ----- Comparable methods ---------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Object obj)
        {
        return obj instanceof ModuleConstant
               ? m_name.compareTo(((ModuleConstant) obj).m_name)
               : 1;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return "(Module)->[name = " + m_name + "]";
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String format()
        {
        return m_name.format();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj)
        {
        return obj instanceof ModuleConstant
               && m_name.equals(((ModuleConstant) obj).m_name);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the module name.
     *
     * @return the module name
     */
    public UtfConstant getName()
        {
        return m_name;
        }

    /**
     * Sets the module name.
     *
     * @param name the module name
     */
    public void setName(UtfConstant name)
        {
        m_name = name;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The ConstantPool index of the module name.
     */
    private int m_nNameIndex;

    /**
     * The Constant that contains module name.
     */
    private UtfConstant m_name;
    }