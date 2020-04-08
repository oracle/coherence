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
* Represents a Java Virtual Machine "SourceFile" Attribute which specifies
* the file name from which the Java .class was compiled.
*
* @version 0.50, 05/15/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class SourceFileAttribute extends Attribute implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a source file attribute.
    *
    * @param context  the JVM structure containing the attribute
    */
    protected SourceFileAttribute(VMStructure context)
        {
        super(context, ATTR_FILENAME);
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
        m_utf = (UtfConstant) pool.getConstant(stream.readUnsignedShort());
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
        pool.registerConstant(m_utf);
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
        stream.writeShort(pool.findConstant(m_utf));
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


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the attribute.
    *
    * @return a string describing the attribute
    */
    public String toString()
        {
        return super.getName() + '=' + m_utf.getValue();
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
            SourceFileAttribute that = (SourceFileAttribute) obj;
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
    * Determine the source file name.
    *
    * @return the file name
    */
    public String getSourceFile()
        {
        return m_utf.getValue();
        }

    /**
    * Set the source file name.
    *
    * @param sName the file name
    */
    public void setSourceFile(String sName)
        {
        m_utf = new UtfConstant(sName);
        m_fModified = true;
        }

    /**
    * Get the constant holding the source file name.
    *
    * @return the source file constant
    */
    public UtfConstant getSourceFileConstant()
        {
        return m_utf;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "SourceFileAttribute";

    /**
    * The name of the source file.
    */
    private UtfConstant m_utf;

    /**
    * Has the attribute been modified?
    */
    private boolean m_fModified;
    }
