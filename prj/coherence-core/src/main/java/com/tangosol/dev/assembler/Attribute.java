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

import java.util.Arrays;


/**
* Represents a Java Virtual Machine Attribute structure as defined by the
* Java Virtual Machine (JVM) Specification.
*
* <code><pre>
*   attribute_info
*       {
*       u2 attribute_name_index;
*       u4 attribute_length;
*       u1 info[attribute_length];
*       }
* </pre></code>
*
* @version 0.50, 05/14/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Attribute extends VMStructure implements Constants, Comparable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct with a name.
    *
    * @param context  the JVM structure containing the attribute
    * @param sAttr    the attribute name.
    */
    protected Attribute(VMStructure context, String sAttr)
        {
        this(context, new UtfConstant(sAttr));
        }

    /**
    * Construct with a name.
    *
    * @param context  the JVM structure containing the attribute
    * @param utf      the constant containing the attribute name.
    */
    protected Attribute(VMStructure context, UtfConstant utf)
        {
        m_utf     = utf;
        m_ab      = NO_BYTES;
        m_abOrig  = NO_BYTES;
        m_context = context;
        }


    // ----- Attribute operations -------------------------------------------

    /**
    * Based on the name of the attribute, which is encountered first in the
    * stream, construct the correct attribute class and disassemble it.
    *
    * @param context  the VM structure which contains this attribute; this
    *                 is needed since different attributes are valid only
    *                 within certain contexts
    * @param stream   the stream implementing java.io.DataInput from which
    *                 to read the assembled VM structure
    * @param pool     the constant pool for the class which contains any
    *                 constants referenced by this VM structure
    */
    protected static Attribute loadAttribute(VMStructure context, DataInput stream, ConstantPool pool)
            throws IOException
        {
        UtfConstant utf   = (UtfConstant) pool.getConstant(stream.readUnsignedShort());
        String      sAttr = utf.getValue();
        Attribute   attr  = null;

        // determine if the attribute is known
        if (context instanceof Method)
            {
            if (sAttr.equals(ATTR_CODE))
                {
                attr = new CodeAttribute(context);
                }
            else if (sAttr.equals(ATTR_EXCEPTIONS))
                {
                attr = new ExceptionsAttribute(context);
                }
            else if (sAttr.equals(ATTR_DEPRECATED))
                {
                attr = new DeprecatedAttribute(context);
                }
            else if (sAttr.equals(ATTR_SYNTHETIC))
                {
                attr = new SyntheticAttribute(context);
                }
            else if (sAttr.equals(ATTR_RTVISANNOT))
                {
                attr = new RuntimeVisibleAnnotationsAttribute(context);
                }
            else if (sAttr.equals(ATTR_RTINVISANNOT))
                {
                attr = new RuntimeInvisibleAnnotationsAttribute(context);
                }
            else if (sAttr.equals(ATTR_RTVISPARAMANNOT))
                {
                attr = new RuntimeVisibleParameterAnnotationsAttribute(context);
                }
            else if (sAttr.equals(ATTR_RTINVISPARAMANNOT))
                {
                attr = new RuntimeInvisibleParameterAnnotationsAttribute(context);
                }
            else if (sAttr.equals(ATTR_ANNOTDEFAULT))
                {
                attr = new AnnotationDefaultAttribute(context);
                }
            else if (sAttr.equals(ATTR_SIGNATURE))
                {
                attr = new SignatureAttribute(context);
                }
            else if (sAttr.equals(ATTR_RTVISTANNOT))
                {
                attr = new RuntimeVisibleTypeAnnotationsAttribute(context);
                }
            else if (sAttr.equals(ATTR_RTINVISTANNOT))
                {
                attr = new RuntimeInvisibleTypeAnnotationsAttribute(context);
                }
            else if (sAttr.equals(ATTR_METHODPARAMS))
                {
                attr = new MethodParametersAttribute(context);
                }
            }
        else if (context instanceof CodeAttribute)
            {
            if (sAttr.equals(ATTR_LINENUMBERS))
                {
                attr = new LineNumberTableAttribute(context);
                }
            else if (sAttr.equals(ATTR_VARIABLES))
                {
                attr = new LocalVariableTableAttribute(context);
                }
            else if (sAttr.equals(ATTR_VARIABLETYPES))
                {
                attr = new LocalVariableTypeTableAttribute(context);
                }
            else if (sAttr.equals(ATTR_STACKMAPTABLE))
                {
                attr = new StackMapTableAttribute(context);
                }
            else if (sAttr.equals(ATTR_RTVISTANNOT))
                {
                attr = new RuntimeVisibleTypeAnnotationsAttribute(context);
                }
            else if (sAttr.equals(ATTR_RTINVISTANNOT))
                {
                attr = new RuntimeInvisibleTypeAnnotationsAttribute(context);
                }
            }
        else if (context instanceof Field)
            {
            if (sAttr.equals(ATTR_CONSTANT))
                {
                attr = new ConstantValueAttribute(context);
                }
            else if (sAttr.equals(ATTR_DEPRECATED))
                {
                attr = new DeprecatedAttribute(context);
                }
            else if (sAttr.equals(ATTR_SYNTHETIC))
                {
                attr = new SyntheticAttribute(context);
                }
            else if (sAttr.equals(ATTR_RTVISANNOT))
                {
                attr = new RuntimeVisibleAnnotationsAttribute(context);
                }
            else if (sAttr.equals(ATTR_RTINVISANNOT))
                {
                attr = new RuntimeInvisibleAnnotationsAttribute(context);
                }
            else if (sAttr.equals(ATTR_SIGNATURE))
                {
                attr = new SignatureAttribute(context);
                }
            else if (sAttr.equals(ATTR_RTVISTANNOT))
                {
                attr = new RuntimeVisibleTypeAnnotationsAttribute(context);
                }
            else if (sAttr.equals(ATTR_RTINVISTANNOT))
                {
                attr = new RuntimeInvisibleTypeAnnotationsAttribute(context);
                }
            }
        else if (context instanceof ClassFile)
            {
            if (sAttr.equals(ATTR_FILENAME))
                {
                attr = new SourceFileAttribute(context);
                }
            else if (sAttr.equals(ATTR_DEPRECATED))
                {
                attr = new DeprecatedAttribute(context);
                }
            else if (sAttr.equals(ATTR_SYNTHETIC))
                {
                attr = new SyntheticAttribute(context);
                }
            else if (sAttr.equals(ATTR_INNERCLASSES))
                {
                attr = new InnerClassesAttribute(context);
                }
            else if (sAttr.equals(ATTR_ENCMETHOD))
                {
                attr = new EnclosingMethodAttribute(context);
                }
            else if (sAttr.equals(ATTR_RTVISANNOT))
                {
                attr = new RuntimeVisibleAnnotationsAttribute(context);
                }
            else if (sAttr.equals(ATTR_RTINVISANNOT))
                {
                attr = new RuntimeInvisibleAnnotationsAttribute(context);
                }
            else if (sAttr.equals(ATTR_BOOTSTRAPMETHODS))
                {
                attr = new BootstrapMethodsAttribute(context);
                }
            else if (sAttr.equals(ATTR_RTVISTANNOT))
                {
                attr = new RuntimeVisibleTypeAnnotationsAttribute(context);
                }
            else if (sAttr.equals(ATTR_RTINVISTANNOT))
                {
                attr = new RuntimeInvisibleTypeAnnotationsAttribute(context);
                }
            }

        // unknown attribute
        if (attr == null)
            {
            attr = new Attribute(context, utf);
            pool.setOrderImportant(true);
            }

        attr.disassemble(stream, pool);
        return attr;
        }


    // ----- VMStructure operations -----------------------------------------

    /**
    * The disassembly process reads the structure from the passed input
    * stream and uses the constant pool to dereference any constant
    * references.
    * <p>
    * This method must be overridden or supplemented by sub-classes which
    * do not maintain the attribute as binary.
    *
    * @param stream  the stream implementing java.io.DataInput from which
    *                to read the assembled VM structure
    * @param pool    the constant pool for the class which contains any
    *                constants referenced by this VM structure
    */
    protected void disassemble(DataInput stream, ConstantPool pool)
            throws IOException
        {
        int    cb = stream.readInt();
        byte[] ab = new byte[cb];
        stream.readFully(ab);
        m_ab = ab;
        }

    /**
    * The pre-assembly step collects the necessary entries for the constant
    * pool.  During this step, all constants used by this VM structure and
    * any sub-structures are registered with (but not yet bound by position
    * in) the constant pool.
    * <p>
    * This method must be supplemented by sub-classes which reference any
    * additional constants.
    *
    * @param pool  the constant pool for the class which needs to be
    *              populated with the constants required to build this
    *              VM structure
    */
    protected void preassemble(ConstantPool pool)
        {
        pool.registerConstant(m_utf);
        }

    /**
    * The assembly process assembles and writes the structure to the passed
    * output stream, resolving any dependencies using the passed constant
    * pool.
    * <p>
    * This method must be overridden by sub-classes which do not maintain
    * the attribute as binary.
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
        stream.writeShort(pool.findConstant(m_utf));
        stream.writeInt(m_ab.length);
        stream.write(m_ab);
        }

    /**
    * Determine the identity of the VM structure (if applicable).
    *
    * @return  the string identity of the VM structure
    */
    public String getIdentity()
        {
        return m_utf.getValue();
        }

    /**
    * Determine if the VM structure (or any contained VM structure) has been
    * modified by comparing the binary portion of the attribute.
    * <p>
    * This method must be overridden by sub-classes which do not maintain
    * the attribute as binary.
    *
    * @return true if the VM structure has been modified
    */
    public boolean isModified()
        {
        return m_ab != m_abOrig && !Arrays.equals(m_ab, m_abOrig);
        }

    /**
    * Reset the modified state of the VM structure.
    * <p>
    * This method must be overridden by sub-classes which do not maintain
    * the attribute as binary.
    */
    protected void resetModified()
        {
        m_abOrig = m_ab;
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
        Attribute that = (Attribute) obj;
        return this.m_utf.compareTo(that.m_utf);
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the attribute.
    *
    * @return a string describing the attribute
    */
    public String toString()
        {
        return m_utf.getValue();
        }

    /**
    * Compare this object to another object for equality.
    * <p>
    * This method must be overridden by sub-classes which do not maintain
    * the attribute as binary.
    *
    * @param obj  the other object to compare to this
    *
    * @return true if this object equals that object
    */
    public boolean equals(Object obj)
        {
        try
            {
            Attribute that = (Attribute) obj;
            return this            == that
                || this.getClass() == that.getClass()
                && this.m_utf.equals(that.m_utf)
                && Arrays.equals(this.getBytes(), that.getBytes());
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
    * Determine the context of the attribute.
    *
    * @return the JVM structure containing this attribute
    */
    protected VMStructure getContext()
        {
        return m_context;
        }

    /**
    * Determine the attribute's name, which is immutable.
    *
    * @return the attribute name
    */
    public String getName()
        {
        return m_utf.getValue();
        }

    /**
    * Get the attribute's name constant.
    *
    * @return the attribute name constant
    */
    public UtfConstant getNameConstant()
        {
        return m_utf;
        }

    /**
    * Get the binary portion of the attribute.  If the binary is unavailable,
    * for example, if a sub-class is unable to produce the binary without the
    * constant pool, then null is returned.  This typically would occur in an
    * attribute that references constants, since the attribute would require
    * the constant pool for building the binary.
    *
    * @return the binary portion of the attribute
    */
    public byte[] getBytes()
        {
        return m_ab;
        }

    /**
    * Set the binary portion of the attribute.  Pass null to clear the binary
    * portion.
    */
    public void setBytes(byte[] ab)
        {
        m_ab = (ab == null ? NO_BYTES : ab);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Attribute";

    /**
    * An empty attribute.
    */
    private static final byte[] NO_BYTES = new byte[0];

    /**
    * The virtual machine context.
    */
    private VMStructure m_context;

    /**
    * Attribute name.
    */
    private UtfConstant m_utf;

    /**
    * Attribute contents.
    */
    private byte[] m_ab;

    /**
    * Original attribute contents.
    */
    private byte[] m_abOrig;
    }
