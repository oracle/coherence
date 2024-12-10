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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;


/**
* Represents a Java Virtual Machine Annotation structure in
* "RuntimeVisibleAnnotations", "RuntimeInvsibleAnnotations",
* "RuntimeVisibleParameterAnnotations",
* "RuntimeInvisibleParameterAnnotations", and "AnnotationDefault"
* attributes.
*
* <p>
* The Annotation structure is defined by the JDK 1.5 documentation as:
* <p>
* <code><pre>
*   Annotation
*       {
*       u2 type_index; 
*       u2 num_element_value_pairs; 
*           {
*           u2 element_name_index; 
*           element_value value; 
*           } element_value_pairs[num_element_value_pairs] 
*       }
*
*   element_value
*       {
*       u1 tag;
*       union
*           {
*           u2 const_value_index; 
*               { 
*               u2 type_name_index; 
*               u2 const_name_index; 
*               } enum_const_value; 
*           u2 class_info_index; 
*           annotation annotation_value; 
*               { 
*               u2 num_values; 
*               element_value values[num_values]; 
*               } array_value; 
*           } value; 
*       }
* </pre></code>
*
* @author rhl 2008.09.23
*/
public class Annotation
        extends VMStructure
        implements Constants
    {
    // ----- VMStructure operations -----------------------------------------

    /**
    * Read the constant information from the stream.  Since constants can be
    * inter-related, the dependencies are not derefenced until all constants
    * are disassembled; at that point, the constants are resolved using the
    * postdisassemble method.
    *
    * @param stream  the stream implementing java.io.DataInput from which
    *                to read the constant information
    * @param pool    the constant pool for the class which does not yet
    *                contain the constants referenced by this constant
    */
    protected void disassemble(DataInput stream, ConstantPool pool)
            throws IOException
        {
        m_utfType = (UtfConstant) pool.getConstant(stream.readUnsignedShort());
        int cElementValue = stream.readUnsignedShort();
        for (int i = 0; i < cElementValue; i++)
            {
            UtfConstant          utfElementName;
            AbstractElementValue elementValue;

            utfElementName = (UtfConstant) pool.getConstant(stream.readUnsignedShort());
            elementValue   = AbstractElementValue.loadElementValue(stream, pool);

            m_mapElementValue.put(utfElementName, elementValue);
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
        pool.registerConstant(m_utfType);

        for (Iterator iter = m_mapElementValue.entrySet().iterator();
             iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();
            ((UtfConstant) entry.getKey()).preassemble(pool);
            ((AbstractElementValue) entry.getValue()).preassemble(pool);            
            }
        }
    
    /**
    * The assembly process assembles and writes the constant to the passed
    * output stream.
    *
    * @param stream  the stream implementing java.io.DataOutput to which to
    *                write the assembled constant
    * @param pool    the constant pool for the class which by this point
    *                contains the entire set of constants required to build
    *                this VM structure
    */
    protected void assemble(DataOutput stream, ConstantPool pool)
            throws IOException
        {
        stream.writeShort(pool.findConstant(m_utfType));
        stream.writeShort(m_mapElementValue.size());
        for (Iterator iter = m_mapElementValue.entrySet().iterator();
             iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();
            stream.writeShort(pool.findConstant((UtfConstant) entry.getKey()));
            ((AbstractElementValue) entry.getValue()).assemble(stream, pool);            
            }
        }

    /**
    * Determine if the attribute has been modified.
    *
    * @return true if the attribute has been modified
    */
    public boolean isModified()
        {
        for (Iterator iter = m_mapElementValue.values().iterator();
             iter.hasNext(); )
            {
            if (((AbstractElementValue) iter.next()).isModified())
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


    // ----- AbstractAnnotation operations ----------------------------------

    /**
    * Get the assembled size in bytes of this annotation structure.
    */
    public int getSize()
        {
        int cBytes = 0;

        cBytes += 2;     /* type_index */
        cBytes += 2;     /* num_element_value_pairs */        

        for (Iterator iter = m_mapElementValue.values().iterator();
             iter.hasNext(); )
            {
            cBytes += 2; /* element_name_index */
            cBytes += ((AbstractElementValue) iter.next()).getSize();
            }
        return cBytes;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the type of this annotation.
    */
    public UtfConstant getAnnotationType()
        {
        return m_utfType;
        }

    public void setAnnotationType(UtfConstant utfType)
        {
        m_utfType   = utfType;
        m_fModified = true;
        }
    
    /**
    * Set an element value in this annotation structure.
    */
    public void setElementValue(UtfConstant utfElementName,
                                AbstractElementValue elementValue)
        {
        m_mapElementValue.put(utfElementName, elementValue);
        m_fModified = true;
        }

    /**
    * Get the element value associated with the element name in this
    * annotation structure, or null if the element does not exist.
    */
    public AbstractElementValue getElementValue(UtfConstant utfElementName)
        {
        return (AbstractElementValue) m_mapElementValue.get(utfElementName);
        }
    
    /**
    * Get an Iterator of the element names in this annotation structure.
    */
    public Iterator getElementNames()
        {
        return m_mapElementValue.keySet().iterator();
        }
    
    /**
    * Clear the element values.
    */
    public void clearElementValues()
        {
        m_mapElementValue.clear();
        m_fModified = true;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Annotation";
    
    /**
    * The type of the annotation.
    */
    private UtfConstant m_utfType;

    /**
    * The element-values.
    */
    private HashMap     m_mapElementValue = new HashMap();
    
    /**
    * Has the annotation been modified?
    */
    private boolean     m_fModified;    
    

    // ----- inner class: AbstractElementValue ------------------------------

    /**
    * Represents an element_value structure used by annotation-related
    * attributes.
    */
    public static abstract class AbstractElementValue
            extends VMStructure implements Constants
        {
        // ----- constructors -----------------------------------------------

        protected AbstractElementValue(char cTag)
            {
            m_cTag = cTag;
            }

        protected static AbstractElementValue loadElementValue(DataInput stream,
                                                               ConstantPool pool)
                throws IOException
            {
            AbstractElementValue elementValue = null;
            char                 cTag         = (char) stream.readByte();

            switch (cTag)
                {
                case TAGTYPE_BYTE:
                case TAGTYPE_CHAR:
                case TAGTYPE_DOUBLE:
                case TAGTYPE_FLOAT:
                case TAGTYPE_INT:
                case TAGTYPE_LONG:
                case TAGTYPE_SHORT:
                case TAGTYPE_BOOLEAN:
                case TAGTYPE_STRING:
                    {
                    /* primitive or string value */
                    elementValue = new ConstantElementValue(cTag);
                    break;
                    }
                case TAGTYPE_ENUM:
                    {
                    /* enum value */
                    elementValue = new EnumElementValue();
                    break;
                    }                    
                case TAGTYPE_CLASS:
                    {
                    /* class value */
                    elementValue = new ClassElementValue();
                    break;
                    }                    
                case TAGTYPE_ANNOTATION:
                    {
                    /* annotation value */
                    elementValue = new AnnotationElementValue();
                    break;
                    }                    
                case TAGTYPE_ARRAY:
                    {
                    /* array value */
                    elementValue = new ArrayElementValue();
                    break;
                    }
                default:
                    {
                    throw new IllegalArgumentException(CLASS + ".loadElementValue: unknown ElementValue tag type " + cTag);
                    }
                }
            elementValue.disassemble(stream, pool);
            return elementValue;
            }

         
        // ----- VMStructure operations -------------------------------------

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
            stream.writeByte((byte) m_cTag);
            }

        
        // ----- accessors --------------------------------------------------

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
        */
        protected void resetModified()
            {
            m_fModified = false;
            }

        /**
        * Get the assembled size in bytes of this element value structure.
        */        
        protected int getSize()
            {
            return 1;  /* tag */
            }
        
        // ----- constants --------------------------------------------------

        /* byte element type */
        public static final char TAGTYPE_BYTE         = 'B';
        
        /* char element type */        
        public static final char TAGTYPE_CHAR         = 'C';
        
        /* double element type */        
        public static final char TAGTYPE_DOUBLE       = 'D';
        
        /* float element type */
        public static final char TAGTYPE_FLOAT        = 'F';

        /* int element type */
        public static final char TAGTYPE_INT          = 'I';

        /* long element type */
        public static final char TAGTYPE_LONG         = 'J';

        /* short element type */
        public static final char TAGTYPE_SHORT        = 'S';

        /* boolean element type */
        public static final char TAGTYPE_BOOLEAN      = 'Z';

        /* String element type */
        public static final char TAGTYPE_STRING       = 's';

        /* enum constant element type */
        public static final char TAGTYPE_ENUM         = 'e';

        /* class element type */
        public static final char TAGTYPE_CLASS        = 'c';

        /* annotation element type */
        public static final char TAGTYPE_ANNOTATION   = '@';

        /* array element type */
        public static final char TAGTYPE_ARRAY        = '[';        
        
        // ----- data members -----------------------------------------------

        /**
        * Tracks modification to this object.
        */
        protected boolean m_fModified;
        
        /**
        * The ElementValue type.
        */
        private   char    m_cTag;
        }

    /**
    * Represents a constant element value in an annotation structure.
    */
    public static class ConstantElementValue extends AbstractElementValue
        {

        // ----- constructors -----------------------------------------------

        /**
        * Construct a ConstantElementValue object.  Used during disassembly.
        */
        protected ConstantElementValue(char cType)
            {
            super(cType);
            }

        /**
        * Construct a ConstantElementValue object.
        */
        public ConstantElementValue(char cType, Constant constValue)
            {
            super(cType);
            m_constValue = constValue;
            }

        // ----- accessors --------------------------------------------------

        /**
        * Get the constant value
        */
        public Constant getConstantValue()
            {
            return m_constValue;
            }

        /**
        * Set the constant value
        */
        public void setConstantValue(Constant constValue)
            {
            m_constValue = constValue;
            m_fModified  = true;
            }

        /**
        * Get the assembled size in bytes of this element value structure.
        */        
        protected int getSize()
            {
            return 2 + super.getSize();  /* const_value_index */
            }
        
        // ----- VMStructure operations -------------------------------------

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
            m_constValue = (Constant) pool.getConstant(stream.readUnsignedShort());
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
            pool.registerConstant(m_constValue);
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
            super.assemble(stream, pool);
            stream.writeShort(pool.findConstant(m_constValue));
            }

        // ----- data members -----------------------------------------------

        /**
        * The constant value.
        */
        private Constant m_constValue;
        }

    /**
    * Represents a class element value in an annotation structure.
    */
    public static class ClassElementValue extends AbstractElementValue
        {

        // ----- constructors -----------------------------------------------

        /**
        * Construct a ClassElementValue object.  Used during disassembly.
        */
        protected ClassElementValue()
            {
            super(TAGTYPE_CLASS);
            }

        /**
        * Construct a ClassElementValue object.
        */
        public ClassElementValue(UtfConstant utfClassType)
            {
            super(TAGTYPE_CLASS);
            m_utfClassType = utfClassType;
            }

        // ----- accessors --------------------------------------------------

        /**
        * Get the class type
        */
        public UtfConstant getClassType()
            {
            return m_utfClassType;
            }

        /**
        * Set the class type
        */
        public void setClassType(UtfConstant utfClassType)
            {
            m_utfClassType = utfClassType;
            m_fModified    = true;
            }

        /**
        * Get the assembled size in bytes of this element value structure.
        */        
        protected int getSize()
            {
            return 2 + super.getSize();  /* class_info_index */
            }
        
        // ----- VMStructure operations -------------------------------------

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
            m_utfClassType = (UtfConstant) pool.getConstant(stream.readUnsignedShort());
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
            pool.registerConstant(m_utfClassType);
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
            super.assemble(stream, pool);
            stream.writeShort(pool.findConstant(m_utfClassType));
            }

        // ----- data members -----------------------------------------------

        /**
        * The class type .
        */
        private UtfConstant m_utfClassType;
        }


    /**
    * Represents an enum value in an annotation structure.
    */
    public static class EnumElementValue extends AbstractElementValue
        {

        // ----- constructors -----------------------------------------------

        /**
        * Construct a EnumElementValue object.  Used during disassembly.
        */
        protected EnumElementValue()
            {
            super(TAGTYPE_ENUM);
            }

        /**
        * Construct a EnumElementValue object.
        */
        public EnumElementValue(UtfConstant utfEnumName, UtfConstant utfEnumType)
            {
            super(TAGTYPE_ENUM);
            m_utfEnumName = utfEnumName;
            m_utfEnumType = utfEnumType;
            }

        // ----- accessors --------------------------------------------------

        /**
        * Get the enum name.
        */
        public UtfConstant getEnumName()
            {
            return m_utfEnumName;
            }

        /**
        * Set the enum name.
        */
        public void setEnumName(UtfConstant utfEnumName)
            {
            m_utfEnumName = utfEnumName;
            m_fModified   = true;
            }

        /**
        * Get the enum type.
        */
        public UtfConstant getEnumType()
            {
            return m_utfEnumType;
            }

        /**
        * Set the enum type.
        */
        public void setEnumType(UtfConstant utfEnumType)
            {
            m_utfEnumType = utfEnumType;
            m_fModified   = true;
            }

        /**
        * Get the assembled size in bytes of this element value structure.
        */        
        protected int getSize()
            {
            return 4 + super.getSize();  /* type_name_index, const_name_index */
            }        
        
        // ----- VMStructure operations -------------------------------------

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
            m_utfEnumType = (UtfConstant) pool.getConstant(stream.readUnsignedShort());
            m_utfEnumName = (UtfConstant) pool.getConstant(stream.readUnsignedShort());
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
            pool.registerConstant(m_utfEnumType);
            pool.registerConstant(m_utfEnumName);
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
            super.assemble(stream, pool);
            stream.writeShort(pool.findConstant(m_utfEnumType));
            stream.writeShort(pool.findConstant(m_utfEnumName));
            }

        // ----- data members -----------------------------------------------

        /**
        * The enum name.
        */
        private UtfConstant m_utfEnumName;

        /**
        * The enum type.
        */        
        private UtfConstant m_utfEnumType;        
        }

    /**
    * Represents an annotation element value in an annotation structure.
    */
    public static class AnnotationElementValue extends AbstractElementValue
        {

        // ----- constructors -----------------------------------------------

        /**
        * Construct a AnnotationElementValue object.  Used during disassembly.
        */
        protected AnnotationElementValue()
            {
            super(TAGTYPE_ANNOTATION);
            }

        /**
        * Construct a AnnotationElementValue object.
        */
        public AnnotationElementValue(Annotation annotationValue)
            {
            super(TAGTYPE_ANNOTATION);
            m_annotationValue = annotationValue;
            }

        // ----- accessors --------------------------------------------------

        /**
        * Get the annotation.
        */
        public Annotation getAnnotation()
            {
            return m_annotationValue;
            }

        /**
        * Set the annotation.
        */
        public void setAnnotation(Annotation annotationValue)
            {
            m_annotationValue = annotationValue;
            m_fModified       = true;
            }

        /**
        * Get the assembled size in bytes of this element value structure.
        */        
        protected int getSize()
            {
            return m_annotationValue.getSize() + super.getSize();
            }

        /**
        * Determine if the attribute has been modified.
        *
        * @return true if the attribute has been modified
        */
        public boolean isModified()
            {
            return super.isModified() || m_annotationValue.isModified();
            }
        
        // ----- VMStructure operations -------------------------------------

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
            m_annotationValue = new Annotation();
            m_annotationValue.disassemble(stream, pool);
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
            m_annotationValue.preassemble(pool);
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
            super.assemble(stream, pool);
            m_annotationValue.assemble(stream, pool);
            }

        // ----- data members -----------------------------------------------

        /**
        * The annotation value.
        */        
        private Annotation m_annotationValue;
        }


    /**
    * Represents an array element value in an annotation structure.
    */
    public static class ArrayElementValue extends AbstractElementValue
        {

        // ----- constructors -----------------------------------------------

        /**
        * Construct a ArrayElementValue object.  Used during disassembly.
        */
        protected ArrayElementValue()
            {
            super(TAGTYPE_ARRAY);
            }

        /**
        * Construct a ArrayElementValue object.
        */
        public ArrayElementValue(List listElement)
            {
            super(TAGTYPE_ARRAY);
            m_listElement = new Vector(listElement);
            }

        // ----- accessors --------------------------------------------------

        /**
        * Get the list of elements.
        */
        public Iterator getElements()
            {
            return m_listElement.iterator();
            }

        /**
        * Add elementValue to the list of elements.
        */
        public void add(AbstractElementValue elementValue)
            {
            m_listElement.addElement(elementValue);
            m_fModified = true;
            }

        /**
        * Clear the list of elements.
        */
        public void clear()
            {
            m_listElement.clear();
            m_fModified = true;
            }        

        /**
        * Set the list of elements.
        */
        public void setElements(List listElement)
            {
            m_listElement.clear();
            m_listElement.addAll(listElement);
            m_fModified = true;
            }

        /**
        * Get the assembled size in bytes of this element value structure.
        */        
        protected int getSize()
            {
            int cBytes = super.getSize();
            cBytes += 2;  /* num_values */
            for (Iterator iter = m_listElement.iterator(); iter.hasNext();)
                {
                cBytes += ((AbstractElementValue) iter.next()).getSize();
                }
            return cBytes;
            }

        /**
        * Determine if the attribute has been modified.
        *
        * @return true if the attribute has been modified
        */
        public boolean isModified()
            {
            for (Iterator iter = m_listElement.iterator(); iter.hasNext();)
                {
                if (((AbstractElementValue) iter.next()).isModified())
                    {
                    return true;
                    }
                }
            return super.isModified();
            }        
        
        // ----- VMStructure operations -------------------------------------

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
            int cElement = stream.readUnsignedShort();
            for (int i = 0; i < cElement; i++)
                {
                AbstractElementValue elementValue =
                    AbstractElementValue.loadElementValue(stream, pool);
                m_listElement.addElement(elementValue);
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
            for (Iterator iter = m_listElement.iterator(); iter.hasNext();)
                {
                ((AbstractElementValue) iter.next()).preassemble(pool);
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
            super.assemble(stream, pool);

            stream.writeShort(m_listElement.size());
            for (Iterator iter = m_listElement.iterator(); iter.hasNext();)
                {
                ((AbstractElementValue) iter.next()).assemble(stream, pool);
                }
            }

        // ----- data members -----------------------------------------------

        /**
        * The list of element values.
        */        
        private Vector m_listElement = new Vector();
        }
    }