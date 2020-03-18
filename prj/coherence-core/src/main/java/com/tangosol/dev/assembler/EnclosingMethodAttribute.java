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
* Represents a Java Virtual Machine "EnclosingMethod" attribute which
* specifies the enclosing method of a local or anonymous class.
*
* <p>
* The EnclosingMethod Attribute is defined by the JDK 1.5 documentation as:
* <p>
* <code><pre>
*   EnclosingMethod_attribute
*       {
*       u2 attribute_name_index;
*       u4 attribute_length; (=4)
*       u2 class_index;
*       u2 method_index;
*       }
* </pre></code>
*
* @author  rhl 2008.09.23
*/
public class EnclosingMethodAttribute extends Attribute implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a enclosing method attribute.
    *
    * @param context  the JVM structure containing the attribute
    */
    protected EnclosingMethodAttribute(VMStructure context)
        {
        super(context, ATTR_ENCMETHOD);
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
        m_methEnclosing = new MethodConstant();
        m_methEnclosing.disassemble(stream, pool);
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
        m_methEnclosing.preassemble(pool);
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
        stream.writeInt(4);  /* class_index, method_index */
        stream.writeShort(pool.findConstant(m_methEnclosing.getClassConstant()));
        stream.writeShort(pool.findConstant(m_methEnclosing.getSignatureConstant()));
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
        return super.getName() + '=' + m_methEnclosing;
        }

    
    // ----- accessors ------------------------------------------------------

    /**
    * Determine the enclosing method.
    *
    * @return the enclosing method
    */
    public MethodConstant getEnclosingMethod()
        {
        return m_methEnclosing;
        }

    /**
    * Set the enclosing method.
    *
    * @param methEnclosing the enclosing method
    */
    public void setEnclosingMethod(MethodConstant methEnclosing)
        {
        m_methEnclosing = methEnclosing;
        m_fModified     = true;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The enclosing method.
    */
    private MethodConstant m_methEnclosing;
        
    /**
    * Has the attribute been modified?
    */
    private boolean        m_fModified;
    }
