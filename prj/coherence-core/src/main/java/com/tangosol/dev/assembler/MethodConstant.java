/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;



/**
* Represents the method of a class.
*
* @version 0.50, 05/14/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class MethodConstant extends RefConstant implements Constants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Constructor used internally by the InterfaceConstant class.
    */
    protected MethodConstant(int nTag)
        {
        super(nTag);
        }

    /**
    * Constructor used internally by the InterfaceConstant class.
    */
    protected MethodConstant(int nTag, String sClass, String sName, String sType)
        {
        super(nTag, sClass, sName, sType);
        }

    /**
    * Constructor used internally by the InterfaceConstant class.
    */
    protected MethodConstant(int nTag, ClassConstant constantClz, SignatureConstant constantSig)
        {
        super(nTag, constantClz, constantSig);
        }

    /**
    * Constructor used internally by the Constant class.
    */
    protected MethodConstant()
        {
        super(CONSTANT_METHODREF);
        }

    /**
    * Construct a constant which specifies a class method.
    *
    * @param sClass  the class name
    * @param sName   the method name
    * @param sType   the method signature
    */
    public MethodConstant(String sClass, String sName, String sType)
        {
        super(CONSTANT_METHODREF, sClass, sName, sType);
        }

    /**
    * Construct a constant which references the passed constants.
    *
    * @param constantClz  the referenced Class constant which contains the
    *                     name of the class
    * @param constantSig  the referenced Signature constant which contains
    *                     the name and signature of the method
    */
    public MethodConstant(ClassConstant constantClz, SignatureConstant constantSig)
        {
        super(CONSTANT_METHODREF, constantClz, constantSig);
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the constant.
    *
    * @return a string describing the constant
    */
    public String toString()
        {
        return "(Method)->" + super.toString();
        }

    /**
    * Format the constant as it would appear in JASM code.
    *
    * @return the constant as it would appear in JASM code
    */
    public String format()
        {
        String sClass = getClassConstant().format();
        if (sClass.startsWith("java"))
            {
            int of = sClass.lastIndexOf('.');
            if (of != -1)
                {
                sClass = sClass.substring(of + 1);
                }
            }
        sClass = sClass.replace('$', '.');

        SignatureConstant sig = getSignatureConstant();
        String sName = sig.getName();
        String sType = sig.getType();

        String[] asType = Method.toTypeStrings(sType);
        for (int i = 0, c = asType.length; i < c; ++i)
            {
            sType = asType[i];
            int of = sType.lastIndexOf('.');
            if (of != -1)
                {
                sType = sType.substring(of + 1);
                }
            sType = sType.replace('$', '.');
            asType[i] = sType;
            }

        // format as:
        //      return-type class.name(param-types)

        StringBuffer sb = new StringBuffer();
        sb.append(asType[0])
          .append(' ')
          .append(sClass)
          .append('.')
          .append(sName)
          .append('(');

        for (int i = 1, c = asType.length; i < c; ++i)
            {
            if (i > 1)
                {
                sb.append(',');
                }

            sb.append(asType[i]);
            }

        sb.append(')');
        return sb.toString();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the Java signature types of the parameters.
    *
    * @return an array of JVM type signatures, where [0] is the return
    *         type and [1]..[c] are the parameter types
    */
    public String[] getTypes()
        {
        String[] asType = m_asType;
        if (asType == null)
            {
            m_asType = asType = Method.toTypes(getType());
            }
        return asType;
        }

    /**
    * Get the variable types of the parameters.
    *
    * @return an array of JVM variables types (I, F, L, D, A, R) or 0x00 for
    *         void, where [0] is the return type and [1]..[c] are the
    *         parameter types
    */
    public char[] getVariableType()
        {
        char[] achType = m_achType;
        if (achType == null)
            {
            String[] asType = getTypes();
            int      cTypes = asType.length;

            achType = new char[cTypes];
            for (int i = 0; i < cTypes; ++i)
                {
                achType[i] = FieldConstant.getVariableType(asType[i]);
                }

            m_achType = achType;
            }

        return achType;
        }

    /**
    * Get the variable types of the specified parameter.
    *
    * @param i  [0] for the return type and [1]..[c] for the parameter types
    *
    * @return the JVM variable type (I, F, L, D, A, R) or 0x00 for void
    */
    public char getVariableType(int i)
        {
        return getVariableType()[i];
        }

    /**
    * Determine if the referenced method is void.
    *
    * @return true if void
    */
    public boolean isVoid()
        {
        return (getVariableType(0) == 0x00);
        }

    /**
    * Get the variable size of the specified parameter.
    *
    * @param i  [0] for the return type and [1]..[c] for the parameter types
    *
    * @return the number of words on the stack used by the parameter
    */
    public int getVariableSize(int i)
        {
        if (i == 0 && isVoid())
            {
            return 0;
            }

        return OpDeclare.getWidth(getVariableType(i));
        }

    /**
    * Get the net stack change in words of pushing all of the parameters.
    *
    * @return the number of parameter words pushed onto the stack in order
    *         to invoke the method
    */
    public int getTotalParameterSize()
        {
        char[] achType = getVariableType();
        int    cTypes  = achType.length;
        int    cWords  = 0;
        for (int i = 1; i < cTypes; ++i)
            {
            cWords += OpDeclare.getWidth(achType[i]);
            }

        return cWords;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "MethodConstant";

    /**
    * Cached type info:  JVM signatures.
    */
    private String[] m_asType;

    /**
    * Cached type info:  variable types.
    */
    private char[] m_achType;
    }