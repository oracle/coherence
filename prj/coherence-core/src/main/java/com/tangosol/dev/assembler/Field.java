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
* Represents a Java Virtual Machine Field structure as defined by the Java
* Virtual Machine (JVM) Specification.
* <p>
* The structure is defined by the Java Virtual Machine Specification as:
* <p>
* <code><pre>
*   field_info
*       {
*       u2 access_flags;
*       u2 name_index;
*       u2 descriptor_index;
*       u2 attributes_count;
*       attribute_info attributes[attributes_count];
*       }
* </pre></code>
*
* @version 0.50, 05/14/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Field extends VMStructure implements Constants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a field structure.
    */
    protected Field()
        {
        }

    /**
    * Construct a field structure.
    *
    * @param sName  the field name
    * @param sType  the field type
    */
    protected Field(String sName, String sType)
        {
        this(new UtfConstant(sName), new UtfConstant(sType.replace('.', '/')));
        }

    /**
    * Construct a field which references the passed UTF constant.
    *
    * @param constantName  the referenced UTF constant which contains the
    *                      name of the field
    * @param constantType  the referenced UTF constant which contains the
    *                      field type
    */
    protected Field(UtfConstant constantName, UtfConstant constantType)
        {
        if (constantName == null || constantType == null)
            {
            throw new IllegalArgumentException(CLASS + ":  Values cannot be null!");
            }

        m_utfName = constantName;
        m_utfType = constantType;
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
        // access flags
        m_flags.disassemble(stream, pool);

        // name and type
        m_utfName = (UtfConstant) pool.getConstant(stream.readUnsignedShort());
        m_utfType = (UtfConstant) pool.getConstant(stream.readUnsignedShort());

        // attributes
        m_tblAttribute.clear();
        int cAttr = stream.readUnsignedShort();
        for (int i = 0; i < cAttr; ++i)
            {
            Attribute attr = Attribute.loadAttribute(this, stream, pool);
            m_tblAttribute.put(attr.getIdentity(), attr);
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
        pool.registerConstant(m_utfName);
        pool.registerConstant(m_utfType);

        m_flags.preassemble(pool);

        Enumeration enmr = m_tblAttribute.elements();
        while (enmr.hasMoreElements())
            {
            ((Attribute) enmr.nextElement()).preassemble(pool);
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
        m_flags.assemble(stream, pool);

        stream.writeShort(pool.findConstant(m_utfName));
        stream.writeShort(pool.findConstant(m_utfType));

        stream.writeShort(m_tblAttribute.getSize());
        Enumeration enmr = m_tblAttribute.elements();
        while (enmr.hasMoreElements())
            {
            ((Attribute) enmr.nextElement()).assemble(stream, pool);
            }
        }

    /**
    * Determine the identity of the VM structure (if applicable).
    *
    * @return  the string identity of the VM structure
    */
    public String getIdentity()
        {
        return m_utfName.getValue();
        }

    /**
    * Determine if the VM structure (or any contained VM structure) has been
    * modified.
    *
    * @return true if the VM structure has been modified
    */
    public boolean isModified()
        {
        if (m_fModified || m_flags.isModified())
            {
            return true;
            }

        Enumeration enmr = m_tblAttribute.elements();
        while (enmr.hasMoreElements())
            {
            Attribute attr = (Attribute) enmr.nextElement();
            if (attr.isModified())
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
        m_flags.resetModified();

        Enumeration enmr = m_tblAttribute.elements();
        while (enmr.hasMoreElements())
            {
            ((Attribute) enmr.nextElement()).resetModified();
            }

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
        Field that = (Field) obj;
        int nResult = this.m_utfName.compareTo(that.m_utfName);
        if (nResult == 0)
            {
            nResult = this.m_utfType.compareTo(that.m_utfType);
            }
        return nResult;
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the field.
    *
    * @return a string describing the field
    */
    public String toString()
        {
        String sMods = m_flags.toString(ACC_FIELD);
        String sType = getTypeString();
        String sName = m_utfName.getValue();

        StringBuffer sb = new StringBuffer();
        if (sMods.length() > 0)
            {
            sb.append(sMods)
              .append(' ');
            }

        sb.append(sType)
          .append(' ')
          .append(sName);

        return sb.toString();
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
            Field that = (Field) obj;
            return this            == that
                || this.getClass() == that.getClass()
                && this.m_utfName     .equals(that.m_utfName     )
                && this.m_utfType     .equals(that.m_utfType     )
                && this.m_flags       .equals(that.m_flags       )
                && this.m_tblAttribute.equals(that.m_tblAttribute);
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


    // ----- Field operations -----------------------------------------------

    /**
    * Provide a Java source representation of a JVM type signature.
    *
    * @param sSig the JVM type signature
    *
    * @return the Java type name as found in Java source code
    */
    public static String toTypeString(String sSig)
        {
        switch (sSig.charAt(0))
            {
            case 'V':
                return "void";
            case 'Z':
                return "boolean";
            case 'B':
                return "byte";
            case 'C':
                return "char";
            case 'S':
                return "short";
            case 'I':
                return "int";
            case 'J':
                return "long";
            case 'F':
                return "float";
            case 'D':
                return "double";

            case 'L':
                return sSig.substring(1, sSig.indexOf(';')).replace('/', '.');

            case '[':
                int of = 0;
                while (isDecimal(sSig.charAt(++of)))
                    {}
                return toTypeString(sSig.substring(of)) + '[' + sSig.substring(1, of) + ']';

            default:
                throw new IllegalArgumentException("JVM Type Signature cannot start with '" + sSig.charAt(0) + "'");
            }
        }

    /**
    * Provide a boxed version of the given primitive in binary format.
    *
    * @param sSig  the JVM type signature
    *
    * @return the boxed version of the given primitive in binary format
    */
    public static String toBoxedType(String sSig)
        {
        String sBoxedType = "java/lang/";
        switch (sSig.charAt(0))
            {
            case 'V':
                sBoxedType += "Void";
                break;
            case 'Z':
                sBoxedType += "Boolean";
                break;
            case 'B':
                sBoxedType += "Byte";
                break;
            case 'C':
                sBoxedType += "Character";
                break;
            case 'S':
                sBoxedType += "Short";
                break;
            case 'I':
                sBoxedType += "Integer";
                break;
            case 'J':
                sBoxedType += "Long";
                break;
            case 'F':
                sBoxedType += "Float";
                break;
            case 'D':
                sBoxedType += "Double";
                break;
            case 'L':
                if (sSig.startsWith("Ljava/lang/"))
                    {
                    return sSig.substring(1, sSig.length() - 1);
                    }
            case '[':
                // reference and array types are quietly unsupported
                sBoxedType = null;
                break;
            default:
                throw new IllegalArgumentException("JVM Type Signature cannot start with '" + sSig.charAt(0) + "'");
            }
        return sBoxedType;
        }

    /**
    * Provide a boxed version of the given primitive in binary format.
    *
    * @param sSig  the JVM type signature
    *
    * @return the boxed version of the given primitive in binary format
    */
    public static char fromBoxedType(String sSig)
        {
        if (sSig.startsWith("java/lang/"))
            {
            switch (sSig.substring(10))
                {
                case "Void":
                    return 'V';
                case "Boolean":
                    return 'Z';
                case "Byte":
                    return 'B';
                case "Character":
                    return 'C';
                case "Short":
                    return 'S';
                case "Integer":
                    return 'I';
                case "Long":
                    return 'J';
                case "Float":
                    return 'F';
                case "Double":
                    return 'D';
                }
            }
        return 0; // ascii null
        }


    // ----- accessors:  name and type --------------------------------------

    /**
    * Get the name of the field as a string.
    *
    * @return  the field name
    */
    public String getName()
        {
        return m_utfName.getValue();
        }

    /**
    * Get the type of the field as a string.
    *
    * @return  the field type
    */
    public String getType()
        {
        return m_utfType.getValue();
        }

    /**
    * Get the type of the field as a string as it appears in Java source.
    *
    * @return  the field type string
    */
    public String getTypeString()
        {
        return toTypeString(m_utfType.getValue());
        }

    /**
    * Get the UTF constant which holds the field name.
    *
    * @return  the UTF constant which contains the name
    */
    public UtfConstant getNameConstant()
        {
        return m_utfName;
        }

    /**
    * Get the UTF constant which holds the field type.
    *
    * @return  the UTF constant which contains the type
    */
    public UtfConstant getTypeConstant()
        {
        return m_utfType;
        }


    // ----- accessor:  access ----------------------------------------------

    /**
    * Get the field accessibility value.
    *
    * @return one of ACC_PUBLIC, ACC_PROTECTED, ACC_PRIVATE, or ACC_PACKAGE
    */
    public int getAccess()
        {
        return m_flags.getAccess();
        }

    /**
    * Set the field accessibility value.
    *
    * @param nAccess  should be one of ACC_PUBLIC, ACC_PROTECTED,
    *                 ACC_PRIVATE, or ACC_PACKAGE
    */
    public void setAccess(int nAccess)
        {
        m_flags.setAccess(nAccess);
        }

    /**
    * Determine if the accessibility is public.
    *
    * @return true if the accessibility is public
    */
    public boolean isPublic()
        {
        return m_flags.isPublic();
        }

    /**
    * Set the accessibility to public.
    */
    public void setPublic()
        {
        m_flags.setPublic();
        }

    /**
    * Determine if the accessibility is protected.
    *
    * @return true if the accessibility is protected
    */
    public boolean isProtected()
        {
        return m_flags.isProtected();
        }

    /**
    * Set the accessibility to protected.
    */
    public void setProtected()
        {
        m_flags.setProtected();
        }

    /**
    * Determine if the accessibility is package private.
    *
    * @return true if the accessibility is package private
    */
    public boolean isPackage()
        {
        return m_flags.isPackage();
        }

    /**
    * Set the accessibility to package private.
    */
    public void setPackage()
        {
        m_flags.setPackage();
        }

    /**
    * Determine if the accessibility is private.
    *
    * @return true if the accessibility is private
    */
    public boolean isPrivate()
        {
        return m_flags.isPrivate();
        }

    /**
    * Set the accessibility to private.
    */
    public void setPrivate()
        {
        m_flags.setPrivate();
        }


    // ----- accessor:  static -------------------------------------------

    /**
    * Determine if the Static attribute is set.
    *
    * @return true if Static
    */
    public boolean isStatic()
        {
        return m_flags.isStatic();
        }

    /**
    * Set the Static attribute.
    *
    * @param fStatic  true to set to Static, false otherwise
    */
    public void setStatic(boolean fStatic)
        {
        m_flags.setStatic(fStatic);
        }


    // ----- accessor:  final -------------------------------------------

    /**
    * Determine if the Final attribute is set.
    *
    * @return true if Final
    */
    public boolean isFinal()
        {
        return m_flags.isFinal();
        }

    /**
    * Set the Final attribute.
    *
    * @param fFinal  true to set to Final, false otherwise
    */
    public void setFinal(boolean fFinal)
        {
        m_flags.setFinal(fFinal);
        }


    // ----- accessor:  volatile -------------------------------------------

    /**
    * Determine if the Volatile attribute is set.
    *
    * @return true if Volatile
    */
    public boolean isVolatile()
        {
        return m_flags.isVolatile();
        }

    /**
    * Set the Volatile attribute.
    *
    * @param fVolatile  true to set to Volatile, false otherwise
    */
    public void setVolatile(boolean fVolatile)
        {
        m_flags.setVolatile(fVolatile);
        }


    // ----- accessor:  transient -------------------------------------------

    /**
    * Determine if the Transient attribute is set.
    *
    * @return true if Transient
    */
    public boolean isTransient()
        {
        return m_flags.isTransient();
        }

    /**
    * Set the Transient attribute.
    *
    * @param fTransient  true to set to Transient, false otherwise
    */
    public void setTransient(boolean fTransient)
        {
        m_flags.setTransient(fTransient);
        }

        
    // ----- accessor:  enum ------------------------------------------------

    /**
    * Determine if the Enum attribute is set.
    *
    * @return true if Enum
    */
    public boolean isEnum()
        {
        return m_flags.isEnum();
        }

    /**
    * Set the Enum attribute.
    *
    * @param fEnum  true to set to Enum, false otherwise
    */
    public void setEnum(boolean fEnum)
        {
        m_flags.setEnum(fEnum);
        }
        

    // ----- accessor:  attribute -------------------------------------------

    /**
    * Access a Java .class attribute structure.
    *
    * @param sName  the attribute name
    *
    * @return the specified attribute or null if the attribute does not exist
    */
    public Attribute getAttribute(String sName)
        {
        return (Attribute) m_tblAttribute.get(sName);
        }

    /**
    * Add a Java .class attribute structure.
    *
    * @param sName  the attribute name
    *
    * @return  the new attribute
    */
    public Attribute addAttribute(String sName)
        {
        Attribute attribute;
        if (sName.equals(ATTR_CONSTANT))
            {
            attribute = new ConstantValueAttribute(this);
            }
        else if (sName.equals(ATTR_DEPRECATED))
            {
            attribute = new DeprecatedAttribute(this);
            }
        else if (sName.equals(ATTR_SYNTHETIC))
            {
            attribute = new SyntheticAttribute(this);
            }
        else if (sName.equals(ATTR_RTVISANNOT))
            {
            attribute = new RuntimeVisibleAnnotationsAttribute(this);
            }
        else if (sName.equals(ATTR_RTINVISANNOT))
            {
            attribute = new RuntimeInvisibleAnnotationsAttribute(this);
            }
        else if (sName.equals(ATTR_RTVISTANNOT))
            {
            attribute = new RuntimeVisibleTypeAnnotationsAttribute(this);
            }
        else if (sName.equals(ATTR_RTINVISTANNOT))
            {
            attribute = new RuntimeInvisibleTypeAnnotationsAttribute(this);
            }
        else
            {
            attribute = new Attribute(this, sName);
            }

        m_tblAttribute.put(attribute.getIdentity(), attribute);
        m_fModified = true;

        return attribute;
        }

    /**
    * Remove a attribute.
    *
    * @param sName  the attribute name
    */
    public void removeAttribute(String sName)
        {
        m_tblAttribute.remove(sName);
        m_fModified = true;
        }

    /**
    * Access the set of attributes.
    *
    * @return an enumeration of attributes (not attribute names)
    */
    public Enumeration getAttributes()
        {
        return m_tblAttribute.elements();
        }


    // ----- accessor:  attribute helpers -----------------------------------

    /**
    * Determine if the field is a constant.
    *
    * @return true if the field is final and initialized to a constant.
    */
    public boolean isConstant()
        {
        return isFinal() && m_tblAttribute.contains(ATTR_CONSTANT);
        }

    /**
    * Determine the constant value of the field.
    *
    * @return the constant value or null if none
    */
    public Constant getConstantValue()
        {
        ConstantValueAttribute attr =
                (ConstantValueAttribute) m_tblAttribute.get(ATTR_CONSTANT);
        return (attr == null ? null : attr.getConstant());
        }

    /**
    * Set the constant value.
    *
    * @param constant  the constant value
    */
    public void setConstantValue(Constant constant)
        {
        // It is quite rare but possible to have a final instance constant
        // so we don't force setStatic(true);
        setFinal(true);
        ConstantValueAttribute attr =
            (ConstantValueAttribute) addAttribute(ATTR_CONSTANT);
        attr.setConstant(constant);
        }

    /**
    * Determine if the field is deprecated.
    *
    * @return true if deprecated, false otherwise
    */
    public boolean isDeprecated()
        {
        return m_tblAttribute.contains(ATTR_DEPRECATED);
        }

    /**
    * Toggle if the field is deprecated.
    *
    * @param  fDeprecated  pass true to deprecate, false otherwise
    */
    public void setDeprecated(boolean fDeprecated)
        {
        if (fDeprecated)
            {
            addAttribute(ATTR_DEPRECATED);
            }
        else
            {
            removeAttribute(ATTR_DEPRECATED);
            }
        }

    /**
    * Determine if the field is synthetic.
    *
    * @return true if synthetic, false otherwise
    */
    public boolean isSynthetic()
        {
        return m_tblAttribute.contains(ATTR_SYNTHETIC);
        }

    /**
    * Toggle if the field is synthetic.
    *
    * @param  fSynthetic  pass true to set synthetic, false otherwise
    */
    public void setSynthetic(boolean fSynthetic)
        {
        if (fSynthetic)
            {
            addAttribute(ATTR_SYNTHETIC);
            }
        else
            {
            removeAttribute(ATTR_SYNTHETIC);
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Field";

    /**
    * Access flags applicable to a field.
    */
    private static final int ACC_FIELD  = AccessFlags.ACC_PUBLIC       |
                                          AccessFlags.ACC_PRIVATE      |
                                          AccessFlags.ACC_PROTECTED    |
                                          AccessFlags.ACC_STATIC       |
                                          AccessFlags.ACC_FINAL        |
                                          AccessFlags.ACC_VOLATILE     |
                                          AccessFlags.ACC_TRANSIENT    |
                                          AccessFlags.ACC_SYNTHETIC    |
                                          AccessFlags.ACC_ENUM;

    /**
    * The name of the field.
    */
    private UtfConstant m_utfName;

    /**
    * The type of the field.
    */
    private UtfConstant m_utfType;

    /**
    * The AccessFlags structure contained in the field.
    */
    private AccessFlags m_flags = new AccessFlags();

    /**
    * The Attribute structures contained in the field.
    */
    private StringTable m_tblAttribute = new StringTable();

    /**
    * Tracks changes to the field.
    */
    private boolean m_fModified;
    }
