/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;



/**
* Represents the field of a class/interface.
*
* @version 0.50, 05/14/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class FieldConstant extends RefConstant implements Constants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Constructor used internally by the Constant class.
    */
    protected FieldConstant()
        {
        super(CONSTANT_FIELDREF);
        }

    /**
    * Construct a constant which specifies a class/interface field.
    *
    * @param sClass  the class name
    * @param sName   the field name
    * @param sType   the field type
    */
    public FieldConstant(String sClass, String sName, String sType)
        {
        super(CONSTANT_FIELDREF, sClass, sName, sType);
        }

    /**
    * Construct a constant which references the passed constants.
    *
    * @param constantClz  the referenced Class constant which contains the
    *                     name of the class
    * @param constantSig  the referenced Signature constant which contains
    *                     the type/name information for the field
    */
    public FieldConstant(ClassConstant constantClz, SignatureConstant constantSig)
        {
        super(CONSTANT_FIELDREF, constantClz, constantSig);
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the constant.
    *
    * @return a string describing the constant
    */
    public String toString()
        {
        return "(Field)->" + super.toString();
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

        sType = Field.toTypeString(sType);
        int of = sType.lastIndexOf('.');
        if (of != -1)
            {
            sType = sType.substring(of + 1);
            }
        sType = sType.replace('$', '.');

        // format as:
        //      class.name (field-type)
        return  sClass + '.' + sName + " (" + sType + ')';
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the JVM variable type (I, F, L, D, or A) for the passed JVM
    * type signature.
    *
    * @param sType  the JVM type signature
    *
    * @return  the JVM variable type
    */
    public static char getVariableType(String sType)
        {
        // check for intrinsic types (1-character reserved type names)
        if (sType.length() == 1)
            {
            char ch = sType.charAt(0);
            switch (ch)
                {
                case 'Z':   // boolean
                case 'B':   // byte
                case 'C':   // char
                case 'S':   // short
                case 'I':   // int
                    return 'I';

                case 'J':   // long
                    return 'L';

                case 'F':   // float
                    return 'F';

                case 'D':   // double
                    return 'D';

                case 'V':   // void (only for return values)
                    return 0x00;

                default:
                    throw new IllegalStateException(CLASS + "Illegal type=" + ch);
                }
            }
        else
            {
            // reference type
            return 'A';
            }
        }

    /**
    * Get the JVM variable type (I, F, L, D, or A) that can store the field
    * value.
    */
    public char getVariableType()
        {
        char chType = m_chType;
        if (chType == 0x00)
            {
            m_chType = chType = getVariableType(getType());
            }
        return chType;
        }

    /**
    * Get the variable size (number of words used to store the value in a
    * local variable or on the stack) for the field.
    *
    * @return  the number of words used by the variable type
    */
    public int getVariableSize()
        {
        return OpDeclare.getWidth(getVariableType());
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "FieldConstant";

    /**
    * Cached type info.
    */
    private char m_chType;
    }
