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

import java.util.Enumeration;

import com.tangosol.util.StringTable;


/**
* Represents a Java Virtual Machine "exceptions" Attribute which declares
* exceptions that may be thrown by a method.
*
* @version 0.50, 05/15/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class ExceptionsAttribute extends Attribute implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an exceptions attribute.
    *
    * @param context  the JVM structure containing the attribute
    */
    protected ExceptionsAttribute(VMStructure context)
        {
        super(context, ATTR_EXCEPTIONS);
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
        int c = stream.readUnsignedShort();
        for (int i = 0; i < c; ++i)
            {
            ClassConstant clz = (ClassConstant) pool.getConstant(stream.readUnsignedShort());
            m_tblException.put(clz.getValue(), clz);
            }
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

        Enumeration enmr = m_tblException.elements();
        while (enmr.hasMoreElements())
            {
            pool.registerConstant((ClassConstant) enmr.nextElement());
            }
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
        stream.writeInt(2 + m_tblException.getSize() * 2);
        stream.writeShort(m_tblException.getSize());
        Enumeration enmr = m_tblException.elements();
        while (enmr.hasMoreElements())
            {
            ClassConstant clz = (ClassConstant) enmr.nextElement();
            stream.writeShort(pool.findConstant(clz));
            }
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
    *
    * This method must be overridden by sub-classes which do not maintain
    * the attribute as binary.
    */
    protected void resetModified()
        {
        m_fModified = false;
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the attribute.
    *
    * @return a string describing the attribute
    */
    public String toString()
        {
        return "Exceptions:  " + m_tblException.toString();
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
            ExceptionsAttribute that = (ExceptionsAttribute) obj;
            return this            == that
                || this.getClass() == that.getClass()
                && this.m_tblException.equals(that.m_tblException);
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


    // ----- accessor:  exception -------------------------------------------

    /**
    * Add an exception.
    *
    * @param sClz  the class name of the exception
    */
    public void addException(String sClz)
        {
        m_tblException.put(sClz, new ClassConstant(sClz));
        m_fModified = true;
        }

    /**
    * Remove an exception.
    *
    * @param sClz  the class name of the exception
    */
    public void removeException(String sClz)
        {
        m_tblException.remove(sClz);
        m_fModified = true;
        }

    /**
    * Access the set of exceptions.
    *
    * @return an enumeration of exception class names
    */
    public Enumeration getExceptions()
        {
        return m_tblException.keys();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "ExceptionsAttribute";

    /**
    * The constant value.
    */
    private StringTable m_tblException = new StringTable();

    /**
    * Has the attribute been modified?
    */
    private boolean m_fModified;
    }
