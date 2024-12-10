/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


import com.tangosol.util.NullImplementation;
import com.tangosol.util.StringTable;

import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;

import java.util.Enumeration;
import java.util.Vector;


/**
* Represents a Java Virtual Machine Method structure as defined by the Java
* Virtual Machine (JVM) Specification.
*
* @version 0.50, 05/14/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Method extends VMStructure implements Constants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a method structure.  Used by ClassFile disassembly.
    *
    * @param sClass      the class name containing this method
    * @param fInterface  whether this method is defined on an interface
    */
    protected Method(String sClass, boolean fInterface)
        {
        m_sClass     = sClass;
        f_fInterface = fInterface;
        }

    /**
    * Construct a method structure.
    *
    * @param sName       the method name
    * @param sSig        the method signature
    * @param fInterface  whether this method is defined on an interface
    */
    protected Method(String sName, String sSig, boolean fInterface)
        {
        this(new UtfConstant(sName), new UtfConstant(sSig.replace('.','/')), fInterface);
        }

    /**
    * Construct a method which references the passed UTF constants.
    *
    * @param constantName  the referenced UTF constant which contains the
    *                      name of the method
    * @param constantSig   the referenced UTF constant which contains the
    *                      method signature
    * @param fInterface    whether this method is defined on an interface
    */
    protected Method(UtfConstant constantName, UtfConstant constantSig, boolean fInterface)
        {
        if (constantName == null || constantSig == null)
            {
            throw new IllegalArgumentException(CLASS + ":  Values cannot be null!");
            }

        m_utfName    = constantName;
        m_utfSig     = constantSig;
        f_fInterface = fInterface;
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

        // name and signature
        m_utfName = (UtfConstant) pool.getConstant(stream.readUnsignedShort());
        m_utfSig  = (UtfConstant) pool.getConstant(stream.readUnsignedShort());

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
        pool.registerConstant(m_utfSig );

        m_flags.preassemble(pool);

        Enumeration enmr = m_tblAttribute.elements();
        while (enmr.hasMoreElements())
            {
            Attribute attr = (Attribute) enmr.nextElement();
            try
                {
                attr.preassemble(pool);
                }
            catch (Throwable e)
                {
                if (attr.getName().equals(ATTR_CODE))
                    {
                    out("Code pre-assembly error in:  " + toString());
                    ((CodeAttribute) attr).print();
                    }

                if (e instanceof RuntimeException)
                    {
                    throw (RuntimeException) e;
                    }
                else
                    {
                    throw (Error) e;
                    }
                }
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
        stream.writeShort(pool.findConstant(m_utfSig ));

        stream.writeShort(m_tblAttribute.getSize());
        Enumeration enmr = m_tblAttribute.elements();
        while (enmr.hasMoreElements())
            {
            Attribute attr = (Attribute) enmr.nextElement();
            try
                {
                attr.assemble(stream, pool);
                }
            catch (Throwable e)
                {
                if (attr.getName().equals(ATTR_CODE))
                    {
                    out("Code assembly error in:  " + toString());
                    ((CodeAttribute) attr).print();
                    }

                if (e instanceof RuntimeException)
                    {
                    throw (RuntimeException) e;
                    }
                else
                    {
                    throw (Error) e;
                    }
                }
            }
        }

    /**
    * Determine the identity of the VM structure (if applicable).
    *
    * @return  the string identity of the VM structure
    */
    public String getIdentity()
        {
        return m_utfName.getValue() + m_utfSig.getValue();
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
        Method that = (Method) obj;
        int nResult = this.m_utfName.compareTo(that.m_utfName);
        if (nResult == 0)
            {
            nResult = this.m_utfSig.compareTo(that.m_utfSig);
            }
        return nResult;
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the method.
    *
    * @return a string describing the method
    */
    public String toString()
        {
        String   sMods  = m_flags.toString(ACC_METHOD);
        String   sName  = m_utfName.getValue();
        String[] asType = toTypeStrings(m_utfSig.getValue());
        int      cType  = asType.length;

        StringBuffer sb = new StringBuffer();
        if (sMods.length() > 0)
            {
            sb.append(sMods)
              .append(' ');
            }

        sb.append(asType[0])
          .append(' ')
          .append(sName)
          .append('(');

        for (int i = 1; i < cType; ++i)
            {
            if (i > 1)
                {
                sb.append(", ");
                }
            sb.append(asType[i]);
            }

        sb.append(')');
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
            Method that = (Method) obj;
            return this            == that
                || this.getClass() == that.getClass()
                && this.m_utfName     .equals(that.m_utfName     )
                && this.m_utfSig      .equals(that.m_utfSig      )
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


    // ----- Method operations ----------------------------------------------

    /**
    * Add a description (name and access flags) to a parameter at index
    * {@code iParam} in the method signature.
    * <p>
    * Access flags is restricted to the following bit values:
    * <ol>
    *   <li>{@link MethodParametersAttribute.MethodParameter#ACC_FINAL final}</li>
    *   <li>{@link MethodParametersAttribute.MethodParameter#ACC_SYNTHETIC synthetic}</li>
    *   <li>{@link MethodParametersAttribute.MethodParameter#ACC_MANDATED mandated}</li>
    * </ol>
    *
    * @param iParam  an index to the parameter in the method signature
    * @param sName   the name of the parameter
    * @param nFlags  the access flags for the parameter; bit-mask of the following
    *                bits: {@link MethodParametersAttribute.MethodParameter#ACC_FINAL final},
    *                {@link MethodParametersAttribute.MethodParameter#ACC_SYNTHETIC synthetic},
    *                {@link MethodParametersAttribute.MethodParameter#ACC_MANDATED mandated}
    *
    * @return whether the parameter description was added
    */
    public boolean addParameter(int iParam, String sName, int nFlags)
        {
        String[] asTypes = getTypes();
        int      cParams = asTypes.length - 1;
        if (iParam > cParams)
            {
            return false;
            }
        MethodParametersAttribute attrParams = ensureMethodParameters(cParams);

        return attrParams.addParameter(iParam, sName, nFlags);
        }

    /**
    * Parse the method signature into discrete return type and parameter
    * signatures as they would appear in Java source.
    *
    * @param sSig the JVM method signature
    *
    * @return an array of Java type strings, where [0] is the return
    *         type and [1]..[c] are the parameter types.
    */
    public static String[] toTypeStrings(String sSig)
        {
        String[] asType = toTypes(sSig);
        int      cTypes = asType.length;
        for (int i = 0; i < cTypes; ++i)
            {
            asType[i] = Field.toTypeString(asType[i]);
            }
        return asType;
        }

    /**
    * Parse the method signature into discrete return type and parameter
    * signatures as they appear in Java .class structures.
    *
    * @param sSig the JVM method signature
    *
    * @return an array of JVM type signatures, where [0] is the return
    *         type and [1]..[c] are the parameter types.
    */
    public static String[] toTypes(String sSig)
        {
        // check for start of signature
        char[] ach = sSig.toCharArray();
        if (ach[0] != '(')
            {
            throw new IllegalArgumentException("JVM Method Signature must start with '('");
            }

        // reserve the first element for the return value
        Vector vect = new Vector();
        vect.addElement(null);

        // parse parameter signatures
        int of = 1;
        while (ach[of] != ')')
            {
            int cch = getTypeLength(ach, of);
            vect.addElement(new String(ach, of, cch));
            of += cch;
            }

        // return value starts after the parameter-stop character
        // and runs to the end of the method signature
        ++of;
        vect.setElementAt(new String(ach, of, ach.length - of), 0);

        String[] asSig = new String[vect.size()];
        vect.copyInto(asSig);

        return asSig;
        }

    private static int getTypeLength(char[] ach, int of)
        {
        switch (ach[of])
            {
            case 'V':
            case 'Z':
            case 'B':
            case 'C':
            case 'S':
            case 'I':
            case 'J':
            case 'F':
            case 'D':
                return 1;

            case '[':
                {
                int cch = 1;
                while (isDecimal(ach[++of]))
                    {
                    ++cch;
                    }
                return cch + getTypeLength(ach, of);
                }

            case 'L':
                {
                int cch = 2;
                while (ach[++of] != ';')
                    {
                    ++cch;
                    }
                return cch;
                }

            default:
                throw new IllegalArgumentException("JVM Type Signature cannot start with '" + ach[of] + "'");
            }
        }



    // ----- accessors:  name and type --------------------------------------

    /**
    * Get the name of the method as a string.
    *
    * @return  the method name
    */
    public String getName()
        {
        return m_utfName.getValue();
        }

    /**
    * Get the signature of the method as a string.  (This is not called
    * "getSignature" because that could imply a "SignatureConstant", which
    * is itself both name and type.)
    *
    * @return  the method signature
    */
    public String getType()
        {
        return m_utfSig.getValue();
        }

    /**
    * Get the types of the method parameters and return value as they would
    * appear in JVM .class structures.
    *
    * @return  an array of JVM type signatures, [0] for the return value type
    *          and [1..n] for the parameter types
    */
    public String[] getTypes()
        {
        return toTypes(m_utfSig.getValue());
        }

    /**
    * Get the types of the method parameters and return value as they would
    * appear in Java source.
    *
    * @return  an array of strings, [0] for the return value type and [1..n]
    *          for the parameter types
    */
    public String[] getTypeStrings()
        {
        return toTypeStrings(m_utfSig.getValue());
        }

    /**
    * Get the names of the method parameters as they appear in source code.
    * This only works for code which has been assembled or disassembled,
    * not for code which has been created but not yet assembled.
    *
    * @return  an array of parameter names, [0] for the return value and
    *          [1..n] for the parameters; the return value name is always
    *          null and the other names are null if debugging information
    *          is not available
    */
    public String[] getNames()
        {
        String[] as = getTypes();
        int      c  = as.length;

        CodeAttribute code = (CodeAttribute) m_tblAttribute.get(ATTR_CODE);
        if (code != null)
            {
            LocalVariableTableAttribute vars =
                    (LocalVariableTableAttribute) code.getAttribute(ATTR_VARIABLES);
            if (vars != null)
                {
                int cwBase   = isStatic() ? 0 : 1;
                int cwParams = cwBase;
                for (int i = 1; i < c; ++i)
                    {
                    switch (as[i].charAt(0))
                        {
                        default:
                            cwParams += 1;
                            break;

                        case 'D':
                        case 'J':
                            cwParams += 2;
                            break;
                        }
                    }

                int[] aiSlotToParam = new int[cwParams];
                cwParams = cwBase;
                for (int i = 1; i < c; ++i)
                    {
                    // the slot number (cwParams) corresponds to the 1-based
                    // parameter number (i)
                    aiSlotToParam[cwParams] = i;
                    switch (as[i].charAt(0))
                        {
                        default:
                            cwParams += 1;
                            break;

                        case 'D':
                        case 'J':
                            cwParams += 2;
                            break;
                        }

                    // clear param type
                    as[i] = null;
                    }

                // clear return type
                as[0] = null;

                for (Enumeration enmr = vars.ranges(); enmr.hasMoreElements(); )
                    {
                    AbstractLocalVariableTableAttribute.Range range =
                        (AbstractLocalVariableTableAttribute.Range) enmr.nextElement();

                    if (range.getSlot() < cwParams)
                        {
                        int i = aiSlotToParam[range.getSlot()];
                        if (i > 0 && as[i] == null)
                            {
                            as[i] = range.getVariableName();
                            }
                        }
                    }

                return as;
                }
            }

        // clear names
        for (int i = 0; i < c; ++i)
            {
            as[i] = null;
            }

        return as;
        }

    /**
    * Get the UTF constant which holds the method name.
    *
    * @return  the UTF constant which contains the name
    */
    public UtfConstant getNameConstant()
        {
        return m_utfName;
        }

    /**
    * Get the UTF constant which holds the method signature.
    *
    * @return  the UTF constant which contains the signature
    */
    public UtfConstant getTypeConstant()
        {
        return m_utfSig;
        }


    // ----- accessor:  access ----------------------------------------------

    /**
    * Get the method accessibility value.
    *
    * @return one of ACC_PUBLIC, ACC_PROTECTED, ACC_PRIVATE, or ACC_PACKAGE
    */
    public int getAccess()
        {
        return m_flags.getAccess();
        }

    /**
    * Set the method accessibility value.
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


    // ----- accessor:  synchronized -------------------------------------------

    /**
    * Determine if the synchronized attribute is set.
    *
    * @return true if synchronized
    */
    public boolean isSynchronized()
        {
        return m_flags.isSynchronized();
        }

    /**
    * Set the synchronized attribute.
    *
    * @param fSynchronized  true to set to synchronized, false otherwise
    */
    public void setSynchronized(boolean fSynchronized)
        {
        m_flags.setSynchronized(fSynchronized);
        }


    // ----- accessor:  native -------------------------------------------

    /**
    * Determine if the native attribute is set.
    *
    * @return true if native
    */
    public boolean isNative()
        {
        return m_flags.isNative();
        }

    /**
    * Set the native attribute.
    *
    * @param fNative  true to set to native, false otherwise
    */
    public void setNative(boolean fNative)
        {
        m_flags.setNative(fNative);
        }


    // ----- accessor:  abstract -------------------------------------------

    /**
    * Determine if the abstract attribute is set.
    *
    * @return true if abstract
    */
    public boolean isAbstract()
        {
        return m_flags.isAbstract();
        }

    /**
    * Set the abstract attribute.
    *
    * @param fAbstract  true to set to abstract, false otherwise
    */
    public void setAbstract(boolean fAbstract)
        {
        m_flags.setAbstract(fAbstract);
        }
        
    // ----- accessor:  bridge ----------------------------------------------

    /**
    * Determine if the bridge attribute is set.
    *
    * @return true if bridge
    */
    public boolean isBridge()
        {
        return m_flags.isBridge();
        }

    /**
    * Set the bridge attribute.
    *
    * @param fBridge  true to set to bridge, false otherwise
    */
    public void setBridge(boolean fBridge)
        {
        m_flags.setBridge(fBridge);
        }

    // ----- accessor:  varargs ---------------------------------------------

    /**
    * Determine if the varargs attribute is set.
    *
    * @return true if varargs
    */
    public boolean isVarArgs()
        {
        return m_flags.isVarArgs();
        }

    /**
    * Set the varargs attribute.
    *
    * @param fVarArgs  true to set to varargs, false otherwise
    */
    public void setVarArgs(boolean fVarArgs)
        {
        m_flags.setVarArgs(fVarArgs);
        }


    // ----- accessor:  strict ----------------------------------------------

    /**
    * Determine if the strict attribute is set.
    *
    * @return true if strict
    */
    public boolean isStrict()
        {
        return m_flags.isStrict();
        }

    /**
    * Set the strict attribute.
    *
    * @param fStrict  true to set to strict, false otherwise
    */
    public void setStrict(boolean fStrict)
        {
        m_flags.setStrict(fStrict);
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
        if (sName.equals(ATTR_CODE))
            {
            attribute = new CodeAttribute(this);
            }
        else if (sName.equals(ATTR_EXCEPTIONS))
            {
            attribute = new ExceptionsAttribute(this);
            }
        else if (sName.equals(ATTR_DEPRECATED))
            {
            attribute = new DeprecatedAttribute(this);
            }
        else if (sName.equals(ATTR_SYNTHETIC))
            {
            attribute = new SyntheticAttribute(this);
            }
        else if (sName.equals(ATTR_SIGNATURE))
            {
            attribute = new SignatureAttribute(this);
            }
        else if (sName.equals(ATTR_RTVISANNOT))
            {
            attribute = new RuntimeVisibleAnnotationsAttribute(this);
            }
        else if (sName.equals(ATTR_RTINVISANNOT))
            {
            attribute = new RuntimeInvisibleAnnotationsAttribute(this);
            }
        else if (sName.equals(ATTR_RTVISPARAMANNOT))
            {
            attribute = new RuntimeVisibleParameterAnnotationsAttribute(this);
            }
        else if (sName.equals(ATTR_RTINVISPARAMANNOT))
            {
            attribute = new RuntimeInvisibleParameterAnnotationsAttribute(this);
            }
        else if (sName.equals(ATTR_RTVISTANNOT))
            {
            attribute = new RuntimeVisibleTypeAnnotationsAttribute(this);
            }
        else if (sName.equals(ATTR_RTINVISTANNOT))
            {
            attribute = new RuntimeInvisibleTypeAnnotationsAttribute(this);
            }
        else if (sName.equals(ATTR_METHODPARAMS))
            {
            attribute = new MethodParametersAttribute(this);
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
    * Get the code attribute.
    *
    * @return the code attribute, creating one if necessary
    */
    public CodeAttribute getCode()
        {
        CodeAttribute attr = (CodeAttribute) m_tblAttribute.get(ATTR_CODE);
        return (attr == null ? (CodeAttribute) addAttribute(ATTR_CODE) : attr);
        }

    /**
    * Determine if the method is deprecated.
    *
    * @return true if deprecated, false otherwise
    */
    public boolean isDeprecated()
        {
        return m_tblAttribute.contains(ATTR_DEPRECATED);
        }

    /**
    * Toggle if the method is deprecated.
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
    * Determine if the method is synthetic.
    *
    * @return true if synthetic, false otherwise
    */
    public boolean isSynthetic()
        {
        return m_tblAttribute.contains(ATTR_SYNTHETIC) ||
               m_flags.isSynthetic();
        }

    /**
    * Toggle if the method is synthetic.
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

    /**
    * Add an exception.
    *
    * @param sClz  the class name of the exception
    */
    public void addException(String sClz)
        {
        ExceptionsAttribute attr = (ExceptionsAttribute) getAttribute(ATTR_EXCEPTIONS);
        if (attr == null)
            {
            attr = (ExceptionsAttribute) addAttribute(ATTR_EXCEPTIONS);
            }
        attr.addException(sClz);
        }

    /**
    * Remove an exception.
    *
    * @param sClz  the class name of the exception
    */
    public void removeException(String sClz)
        {
        ExceptionsAttribute attr = (ExceptionsAttribute) m_tblAttribute.get(ATTR_EXCEPTIONS);
        if (attr != null)
            {
            attr.removeException(sClz);
            }
        }

    /**
    * Access the set of exceptions.
    *
    * @return an enumeration of exception class names
    */
    public Enumeration getExceptions()
        {
        ExceptionsAttribute attr = (ExceptionsAttribute) getAttribute(ATTR_EXCEPTIONS);
        return attr == null ? NullImplementation.getEnumeration() : attr.getExceptions();
        }


    // ----- helpers --------------------------------------------------------

    /**
    * The class name if this method is from disassembly.
    *
    * @return the class name as it was found in the constant pool
    */
    protected String getClassName()
        {
        return m_sClass;
        }

    /**
    * Ensure a {@link MethodParametersAttribute} exists as method_info
    * attribute.
    *
    * @param cParams  the number of parameters defined by this method's signature
    *
    * @return a MethodParametersAttribute linked to this Method
    */
    protected MethodParametersAttribute ensureMethodParameters(int cParams)
        {
        MethodParametersAttribute attrParams = (MethodParametersAttribute)
                getAttribute(ATTR_METHODPARAMS);
        if (attrParams == null)
            {
            attrParams = (MethodParametersAttribute) addAttribute(ATTR_METHODPARAMS);
            attrParams.setParameterCount(cParams);
            }
        return attrParams;
        }


    // ----- constants ------------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Method";

    /**
    * Access flags applicable to a method.
    */
    public static final int ACC_METHOD = AccessFlags.ACC_PUBLIC       |
                                         AccessFlags.ACC_PRIVATE      |
                                         AccessFlags.ACC_PROTECTED    |
                                         AccessFlags.ACC_STATIC       |
                                         AccessFlags.ACC_FINAL        |
                                         AccessFlags.ACC_SYNCHRONIZED |
                                         AccessFlags.ACC_BRIDGE       |
                                         AccessFlags.ACC_VARARGS      |        
                                         AccessFlags.ACC_NATIVE       |
                                         AccessFlags.ACC_ABSTRACT     |
                                         AccessFlags.ACC_STRICT       |
                                         AccessFlags.ACC_SYNTHETIC;


    // ----- data members ---------------------------------------------------

    /**
    * Whether this method is defined against an interface or a class.
    */
    private final boolean f_fInterface;

    /**
    * The name of the class if this method is the result of disassembly.
    */
    private String m_sClass;

    /**
    * The name of the method.
    */
    private UtfConstant m_utfName;

    /**
    * The signature of the method.
    */
    private UtfConstant m_utfSig;

    /**
    * The AccessFlags structure contained in the method.
    */
    private AccessFlags m_flags = new AccessFlags()
        {
        @Override
        protected void preassemble(ConstantPool pool)
            {
            if (f_fInterface)
                {
                if (isMaskSet(ACC_PROTECTED | ACC_FINAL | ACC_SYNCHRONIZED | ACC_NATIVE))
                    {
                    throw new IllegalStateException("Interface method " + this + " can not be "
                            + "protected, final, synchronized, or native");
                    }
                if (pool.getClassFile().getMajorVersion() < 52)
                    {
                    setPublic();
                    setAbstract(true);
                    }
                else if (!isPublic() && !isPrivate())
                    {
                    throw new IllegalStateException("Interface method " + this + " must be "
                            + "either public or private");
                    }
                }
            if (isAbstract() && isMaskSet(ACC_PRIVATE | ACC_STATIC | ACC_FINAL |
                    ACC_SYNCHRONIZED | ACC_NATIVE | ACC_STRICT))
                {
                throw new IllegalStateException("Abstract Method " + this + " can not be "
                        + "private, static, final, synchronized, native, or strict");
                }

            super.preassemble(pool);
            }
        };

    /**
    * The Attribute structures contained in the method.
    */
    private StringTable m_tblAttribute = new StringTable();

    /**
    * Tracks changes to the method.
    */
    private boolean m_fModified;
    }
