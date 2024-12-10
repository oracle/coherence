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

import com.tangosol.util.Tree;


/**
* Represents a Java Virtual Machine "inner class" attribute which contains
* relationship information tying together an inner and outer class.
* <p>
* The InnerClasses Attribute is defined by the JDK 1.1 documentation as:
* <p>
* <code><pre>
*   InnerClasses_attribute
*       {
*       u2 attribute_name_index;
*       u4 attribute_length;
*       u2 number_of_classes;
*           {
*           u2 inner_class_info_index;   // CONSTANT_Class_info index
*           u2 outer_class_info_index;   // CONSTANT_Class_info index
*           u2 inner_name_index;         // CONSTANT_Utf8_info index
*           u2 inner_class_access_flags; // access_flags bitmask
*           } classes[number_of_classes]
*       }
* </pre></code>
*
* @version 0.50, 05/18/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class InnerClassesAttribute extends Attribute implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an inner classes attribute.
    *
    * @param context  the JVM structure containing the attribute
    */
    protected InnerClassesAttribute(VMStructure context)
        {
        super(context, ATTR_INNERCLASSES);
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
        Tree tbl = m_tblInner;
        tbl.clear();

        stream.readInt();
        int c = stream.readUnsignedShort();
        for (int i = 0; i < c; ++i)
            {
            InnerClass inner = new InnerClass();
            inner.disassemble(stream, pool);
            tbl.put(inner.getIdentity(), inner);
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

        for (Enumeration enmr = m_tblInner.elements(); enmr.hasMoreElements(); )
            {
            ((InnerClass) enmr.nextElement()).preassemble(pool);
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
        Tree tbl = m_tblInner;
        int c = tbl.getSize();

        stream.writeShort(pool.findConstant(super.getNameConstant()));
        stream.writeInt(2 + c * 8);

        stream.writeShort(c);
        for (Enumeration enmr = m_tblInner.elements(); enmr.hasMoreElements(); )
            {
            ((InnerClass) enmr.nextElement()).assemble(stream, pool);
            }
        }

    /**
    * Determine if the attribute has been modified.
    *
    * @return true if the attribute has been modified
    */
    public boolean isModified()
        {
        if (m_fModified)
            {
            return true;
            }

        for (Enumeration enmr = m_tblInner.elements(); enmr.hasMoreElements(); )
            {
            if (((InnerClass) enmr.nextElement()).isModified())
                {
                return true;
                }
            }

        return false;
        }

    /**
    * Reset the modified state of the VM structure.
    */
    protected void resetModified()
        {
        for (Enumeration enmr = m_tblInner.elements(); enmr.hasMoreElements(); )
            {
            ((InnerClass) enmr.nextElement()).resetModified();
            }

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
        return super.getName() + ' ' + m_tblInner.toString();
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
            InnerClassesAttribute that = (InnerClassesAttribute) obj;
            return this            == that
                || this.getClass() == that.getClass()
                && this.m_tblInner.equals(that.m_tblInner);
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
    * Access an inner class information structure.
    *
    * @param sName  the inner class name
    *
    * @return the specified inner class information structure or null
    */
    public InnerClass getInnerClass(String sName)
        {
        return (InnerClass) m_tblInner.get(sName);
        }

    /**
    * Add an inner class information structure.
    *
    * @param sName  the inner class encoded name
    *
    * @return  the new inner class information structure
    */
    public InnerClass addInnerClass(String sName)
        {
        InnerClass inner = new InnerClass(new ClassConstant(sName));
        m_tblInner.put(inner.getIdentity(), inner);
        m_fModified = true;
        return inner;
        }

    /**
    * Remove an inner class information structure.
    *
    * @param sName  the inner class encoded name
    */
    public void removeInnerClass(String sName)
        {
        m_tblInner.remove(sName);
        m_fModified = true;
        }

    /**
    * Access the set of encoded inner class names.
    *
    * @return an enumeration of InnerClass identities
    */
    public Enumeration getInnerClassNames()
        {
        return m_tblInner.keys();
        }

    /**
    * Access the set of inner class information structures.
    *
    * @return an enumeration of InnerClass instances (not inner class names)
    */
    public Enumeration getInnerClasses()
        {
        return m_tblInner.elements();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "InnerClassesAttribute";

    /**
    * Set of inner class information.
    */
    private Tree m_tblInner = new Tree();

    /**
    * Tracks modifications to the inner classes attribute.
    */
    private boolean m_fModified;
    }
