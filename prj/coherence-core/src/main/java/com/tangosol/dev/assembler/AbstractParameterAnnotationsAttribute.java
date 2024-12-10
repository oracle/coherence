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
import java.util.List;
import java.util.Vector;


/**
* Represents a Java Virtual Machine parameter annotations attribute
* ("RuntimeVisibleParameterAnnotations" or
*  "RuntimeInvisibleParameterAnnotations")
*
* <p>
* The parameter annotation attributes are defined by the JDK 1.5
* documentation as:
* <p>
* <code><pre>
*   {RuntimeVisibleParameterAnnotations | RuntimeInvisibleParameterAnnotations}_attribute
*       {
*       u2 attribute_name_index;
*       u4 attribute_length;
*       u1 num_parameters;
*           {
*           u2 num_annotations;
*           annotation annotations[num_annotations]
*           } parameter_annotations[num_parameters]
*       }
* </pre></code>
*
* @author  rhl 2008.09.23
*/
public abstract class AbstractParameterAnnotationsAttribute
        extends Attribute
        implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a parameter annotations attribute.
    *
    * @param context  the JVM structure containing the attribute
    */
    protected AbstractParameterAnnotationsAttribute(VMStructure context,
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
        int cParam = stream.readUnsignedByte();
        for (int i = 0; i < cParam; i++)
            {
            Parameter parameter = new Parameter();
            parameter.disassemble(stream, pool);
            m_listParameter.addElement(parameter);
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
        for (Iterator iter = m_listParameter.iterator();
             iter.hasNext(); )
            {
            ((Parameter) iter.next()).preassemble(pool);
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

        int cBytes = 1;
        for (Iterator iter = m_listParameter.iterator(); iter.hasNext(); )
            {
            cBytes += ((Parameter) iter.next()).getSize();
            }
        stream.writeInt(cBytes);
        stream.writeByte((byte) m_listParameter.size());
        for (Iterator iter = m_listParameter.iterator(); iter.hasNext(); )
            {
            ((Parameter) iter.next()).assemble(stream, pool);
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
        for(Iterator iter = getParameters(); iter.hasNext(); )
            {
            sb.append("\n  ").append(iter.next());
            }
        sb.append("\n}");        
        return sb.toString();
        }

    // ----- accessors ------------------------------------------------------


    /**
    * Get an Iterator of the parameters in this attribute.
    *
    * @return the parameters
    */
    public Iterator getParameters()
        {
        return m_listParameter.iterator();
        }

    /**
    * Add an annotation to this attribute.
    *
    * @param annotation  the annotation
    */
    public void addParameter(Parameter parameter)
        {
        m_listParameter.addElement(parameter);
        m_fModified    = true;
        }

    /**
    * Clear the parameters in this attribute.
    */
    public void clearParameters()
        {
        m_listParameter.clear();
        m_fModified    = true;
        }

    /**
    * Determine if the attribute has been modified.
    *
    * @return true if the attribute has been modified
    */
    public boolean isModified()
        {
        for (Iterator iter = m_listParameter.iterator(); iter.hasNext(); )
            {
            if (((Parameter) iter.next()).isModified())
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


    // ----- data members ---------------------------------------------------

    /**
    * The parameters.
    */
    private Vector m_listParameter = new Vector();
        
    /**
    * Has the attribute been modified?
    */
    private boolean m_fModified;

    
    // ----- inner classes --------------------------------------------------
    
    /**
    * Represents "Parameter" structure of the
    * "RuntimeVisibleParameterAnnotations" and
    * "RuntimeInvisibleParameterAnnotations" attributes.
    */
    public static class Parameter extends VMStructure implements Constants
        {

        // ----- constructors -----------------------------------------------        

        /**
        * Construct a Parameter object.
        */
        protected Parameter()
            {
            }


        // ----- VMStructure operations -------------------------------------

        /**
        * The disassembly process reads the structure from the passed
        * input stream and uses the constant pool to dereference any
        * constant references.
        *
        * @param stream  the stream implementing java.io.DataInput from which
        *                to read the assembled VM structure
        * @param pool    the constant pool for the class which contains any
        *                constants referenced by this VM structure
        */
        protected void disassemble(DataInput stream, ConstantPool pool)
                throws IOException
            {
            int cAnnotation = stream.readUnsignedShort();
            for (int i = 0; i < cAnnotation; i++)
                {
                Annotation annotation = new Annotation();
                annotation.disassemble(stream, pool);
                m_listAnnotation.addElement(annotation);
                }
            }

        /**
        * The pre-assembly step collects the necessary entries for the
        * constant pool.  During this step, all constants used by this
        * VM structure and any sub-structures are registered with (but
        * not yet bound by position in) the constant pool.
        *
        * @param pool  the constant pool for the class which needs to be
        *              populated with the constants required to build this
        *              VM structure
        */
        protected void preassemble(ConstantPool pool)
            {
            for (Iterator iter = m_listAnnotation.iterator();
                 iter.hasNext(); )
                {
                ((Annotation) iter.next()).preassemble(pool);
                }
            }

        /**
        * The assembly process assembles and writes the structure to
        * the passed output stream, resolving any dependencies using
        * the passed constant pool.
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
            stream.writeShort(m_listAnnotation.size());
            for (Iterator iter = m_listAnnotation.iterator(); iter.hasNext(); )
                {
                ((Annotation) iter.next()).assemble(stream, pool);
                }
            }

        // ----- Object operations ------------------------------------------
        
        /**
        * Produce a human-readable string describing the attribute.
        *
        * @return a string describing the attribute
        */
        public String toString()
            {
            StringBuffer sb = new StringBuffer();
            
            sb.append("  ").append("Parameter");
            sb.append("\n  {");
            for(Iterator iter = getAnnotations(); iter.hasNext(); )
                {
                sb.append("\n   ").append(iter.next());
                }
            sb.append("\n  }");        
            return sb.toString();
            }
        
        // ----- accessors --------------------------------------------------


        /**
        * Get an Iterator of the annotations in this parameter.
        *
        * @return the annotations
        */
        public Iterator getAnnotations()
            {
            return m_listAnnotation.iterator();
            }
        
        /**
        * Add an annotation to this parameter.
        *
        * @param annotation  the annotation
        */
        public void addAnnotation(Annotation annotation)
            {
            m_listAnnotation.addElement(annotation);
            m_fModified = true;
            }
        
        /**
        * Clear the annotations in this parameter.
        */
        public void clearAnnotations()
            {
            m_listAnnotation.clear();
            m_fModified = true;
            }

        /**
        * Determine if the parameter has been modified.
        *
        * @return true if the parameter has been modified
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
        * Get the assembled size in bytes of this parameter structure.
        */
        protected int getSize()
            {
            int cBytes = 2;  /* num_annotations */
            for (Iterator iter = m_listAnnotation.iterator(); iter.hasNext(); )
                {
                cBytes += ((Annotation) iter.next()).getSize();
                }
            return cBytes;
            }
        
        // ----- data members -----------------------------------------------
        /**
        * The annotations.
        */
        private Vector m_listAnnotation = new Vector();
        
        /**
        * Has the attribute been modified?
        */
        private boolean m_fModified;

        }
    }
