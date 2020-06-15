/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;



/**
* Represents the method of an interface.
*
* @version 0.50, 05/14/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class InterfaceConstant extends MethodConstant implements Constants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Constructor used internally by the Constant class.
    */
    protected InterfaceConstant()
        {
        super(CONSTANT_INTERFACEMETHODREF);
        }

    /**
    * Construct a constant which specifies an interface method.
    *
    * @param sClass  the interface name
    * @param sName   the method name
    * @param sType   the method signature
    */
    public InterfaceConstant(String sClass, String sName, String sType)
        {
        super(CONSTANT_INTERFACEMETHODREF, sClass, sName, sType);
        }

    /**
    * Construct a constant which references the passed constants.
    *
    * @param constantClz  the referenced Class constant which contains the
    *                     name of the interface
    * @param constantSig  the referenced Signature constant which contains
    *                     the name and signature of the method
    */
    public InterfaceConstant(ClassConstant constantClz, SignatureConstant constantSig)
        {
        super(CONSTANT_INTERFACEMETHODREF, constantClz, constantSig);
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the constant.
    *
    * @return a string describing the constant
    */
    public String toString()
        {
        return "(Interface)->" + super.toString();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "InterfaceConstant";
    }
