/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;


/**
* Represents Java Virtual Machine "inner class" information, part of the
* "InnerClasses" attribute.
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
public class InnerClass extends VMStructure implements Constants, Comparable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor; typically used before disassembly.
    */
    protected InnerClass()
        {
        }

    /**
    * Initializing constructor.
    *
    * @param clzInner  inner class constant
    */
    protected InnerClass(ClassConstant clzInner)
        {
        m_clzInner = clzInner;
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
        m_clzInner = (ClassConstant) pool.getConstant(stream.readUnsignedShort());
        m_clzOuter = (ClassConstant) pool.getConstant(stream.readUnsignedShort());
        m_utfInner = (UtfConstant  ) pool.getConstant(stream.readUnsignedShort());
        m_flags.disassemble(stream, pool);
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
        pool.registerConstant(m_clzInner);
        pool.registerConstant(m_clzOuter);
        pool.registerConstant(m_utfInner);
        m_flags.preassemble(pool);
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
        stream.writeShort(pool.findConstant(m_clzInner));
        stream.writeShort(pool.findConstant(m_clzOuter));
        stream.writeShort(pool.findConstant(m_utfInner));
        m_flags.assemble(stream, pool);
        }

    /**
    * Determine the identity of the VM structure (if applicable).
    *
    * @return  the string identity of the VM structure
    */
    public String getIdentity()
        {
        return m_clzInner.getValue();
        }

    /**
    * Determine if the attribute has been modified.
    *
    * @return true if the attribute has been modified
    */
    public boolean isModified()
        {
        return m_fModified || m_flags.isModified();
        }

    /**
    * Reset the modified state of the VM structure.
    */
    protected void resetModified()
        {
        m_flags.resetModified();
        m_fModified = false;
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
        InnerClass that = (InnerClass) obj;
        return this.m_clzInner.compareTo(that.m_clzInner);
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the attribute.
    *
    * @return a string describing the attribute
    */
    public String toString()
        {
        return "(" + CLASS + ")->" + m_clzInner.toString();
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
            InnerClass that = (InnerClass) obj;
            return  this            == that
                ||  this.getClass() == that.getClass()
                &&  this.m_clzInner.equals(that.m_clzInner)
                && (this.m_clzOuter == null ? that.m_clzOuter == null
                                            : this.m_clzOuter.equals(that.m_clzOuter))
                && (this.m_utfInner == null ? that.m_utfInner == null
                                            : this.m_utfInner.equals(that.m_utfInner))
                &&  this.m_flags   .equals(that.m_flags);
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
    * Access the inner class "encoded" name.
    *
    * @return the encoded inner class name
    */
    public String getInnerClass()
        {
        return m_clzInner.getValue();
        }

    /**
    * Access the inner class "encoded" name.
    *
    * @return the encoded inner class constant
    */
    protected ClassConstant getInnerClassConstant()
        {
        return m_clzInner;
        }

    /**
    * Access the "defining scope" outer class.
    *
    * @return the outer class name or null
    */
    public String getOuterClass()
        {
        return (m_clzOuter != null ? m_clzOuter.getValue() : null);
        }

    /**
    * Access the "defining scope" outer class.
    *
    * @return the outer class constant or null
    */
    protected ClassConstant getOuterClassConstant()
        {
        return m_clzOuter;
        }

    /**
    * Set the "defining scope" outer class.
    *
    * @param  sOuter  the outer class name or null
    */
    public void setOuterClass(String sOuter)
        {
        m_clzOuter = (sOuter != null ? new ClassConstant(sOuter) : null);
        }

    /**
    * Set the "defining scope" outer class.
    *
    * @param clzOuter  the outer class constant or null
    */
    protected void setOuterClassConstant(ClassConstant clzOuter)
        {
        m_clzOuter = clzOuter;
        }

    /**
    * Access the declared inner class name.
    *
    * @return the inner class simple name or null
    */
    public String getInnerName()
        {
        return (m_utfInner != null ? m_utfInner.getValue() : null);
        }

    /**
    * Access the declared inner class name.
    *
    * @return the inner class simple name constant or null
    */
    protected UtfConstant getInnerNameConstant()
        {
        return m_utfInner;
        }

    /**
    * Set the declared inner class name.
    *
    * @param sInner  the inner class simple name or null
    */
    public void setInnerName(String sInner)
        {
        m_utfInner = (sInner != null ? new UtfConstant(sInner) : null);
        }

    /**
    * Set the declared inner class name.
    *
    * @param utfInner  the inner class simple name constant or null
    */
    protected void setInnerNameConstant(UtfConstant utfInner)
        {
        m_utfInner = utfInner;
        }

    /**
    * Get the access flags for the inner class as they were declared in
    * source; the access flags are typically modified by the compiler to
    * follow the accessibility rules for inner classes as defined by the
    * JDK 1.1.
    *
    * @return the declared access flags for the inner class
    */
    public AccessFlags getAccessFlags()
        {
        return m_flags;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "InnerClass";

    /**
    * Encoded name (i.e. assigned by compiler) of the inner class.
    */
    private ClassConstant m_clzInner;

    /**
    * "Defining scope" of the inner class.
    */
    private ClassConstant m_clzOuter;

    /**
    * Inner class name (as declared in the source).
    */
    private UtfConstant   m_utfInner;

    /**
    * Access flags for the inner class (as declared in the source).
    */
    private AccessFlags   m_flags = new AccessFlags();

    /**
    * Tracks if the inner class information has been modified.
    */
    private boolean       m_fModified;
    }
