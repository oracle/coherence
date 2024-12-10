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

import java.util.Iterator;
import java.util.Vector;


/**
* Represents a Java Virtual Machine annotations attribute
* ("RuntimeVisibleAnnotations", "RuntimeInvisibleAnnotations",
* "RuntimeVisibleTypeAnnotations", "RuntimeInvisibleTypeAnnotations").
* <p>
* Note: the similarity at the attribute level between type and non-type annotations
* is significant, thus we use the same mechanism to both disassemble and assemble.
* The distinction is in the definition of the 'annotation' and 'type_annotation'
* structure that we allow to be polymorphically defined by sub classes.
* <p>
* The annotation attributes are defined by the JVM 5 spec and the type annotations
* by the JVM 8 spec as:
* <p>
* <code><pre>
*   {RuntimeVisibleAnnotations     | RuntimeInvisibleAnnotations}_attribute
*    RuntimeVisibleTypeAnnotations | RuntimeInvisibleTypeAnnotations}_attribute
*       {
*       u2 attribute_name_index;
*       u4 attribute_length;
*       u2 num_annotations;
*       (annotation | type_annotation) annotations[num_annotations]
*       }
* </pre></code>
*
* @author rhl 2008.09.23
*
* @see RuntimeVisibleAnnotationsAttribute
* @see RuntimeInvisibleAnnotationsAttribute
* @see RuntimeVisibleTypeAnnotationsAttribute
* @see RuntimeInvisibleTypeAnnotationsAttribute
*/
public abstract class AbstractAnnotationsAttribute
        extends Attribute
        implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an annotations attribute.
    *
    * @param context  the JVM structure containing the attribute
    */
    protected AbstractAnnotationsAttribute(VMStructure context,
                                           String sAttr)
        {
        super(context, sAttr);
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
        int cAnnotation = stream.readUnsignedShort();
        for (int i = 0; i < cAnnotation; i++)
            {
            Annotation annotation = instantiateAnnotation();
            annotation.disassemble(stream, pool);
            m_listAnnotation.addElement(annotation);
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
        for (Iterator iter = m_listAnnotation.iterator();
             iter.hasNext(); )
            {
            ((Annotation) iter.next()).preassemble(pool);
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

        int cBytes = 2;
        for (Iterator iter = m_listAnnotation.iterator(); iter.hasNext(); )
            {
            cBytes += ((Annotation) iter.next()).getSize();
            }

        stream.writeInt(cBytes);
        stream.writeShort(m_listAnnotation.size());
        for (Iterator iter = m_listAnnotation.iterator(); iter.hasNext(); )
            {
            ((Annotation) iter.next()).assemble(stream, pool);
            }
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the attribute.
    *
    * @return a string describing the attribute
    */
    public String toString()
        {
        StringBuffer sb = new StringBuffer();

        sb.append(getName() + " for "+ getContext().getIdentity());
        sb.append("\n{");
        for(Iterator iter = getAnnotations(); iter.hasNext(); )
            {
            sb.append("\n  ").append(iter.next());
            }
        sb.append("\n}");        
        return sb.toString();
        }

    // ----- accessors ------------------------------------------------------


    /**
    * Get an Iterator of the annotations in this attribute.
    *
    * @return the annotations
    */
    public Iterator getAnnotations()
        {
        return m_listAnnotation.iterator();
        }

    /**
    * Add an annotation to this attribute.
    *
    * @param annotation  the annotation
    */
    public void addAnnotation(Annotation annotation)
        {
        m_listAnnotation.addElement(annotation);
        m_fModified    = true;
        }

    /**
    * Clear the annotations in this attribute.
    */
    public void clearAnnotations()
        {
        m_listAnnotation.clear();
        m_fModified    = true;
        }

    /**
    * Determine if the attribute has been modified.
    *
    * @return true if the attribute has been modified
    */
    public boolean isModified()
        {
        for (Iterator iter = m_listAnnotation.iterator(); iter.hasNext(); )
            {
            if (((Annotation) iter.next()).isModified())
                {
                return true;
                }
            }
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

    /**
    * Return a new instance of an {@link Annotation}.
    *
    * @return  a new instance of an Annotation
    */
    protected Annotation instantiateAnnotation()
        {
        return new Annotation();
        }

    // ----- data members ---------------------------------------------------

    /**
    * The annotations.
    */
    private Vector m_listAnnotation = new Vector();
        
    /**
    * Has the attribute been modified?
    */
    private boolean m_fModified;
    }
