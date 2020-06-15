/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


import com.tangosol.dev.component.DataType;

import java.util.Enumeration;


/**
* The TypeInfo interface represents the information about a "type", which
* can be any of:
* <ol>
* <li>Java class
* <li>Java interface
* <li>Component
* </ol>
* method of a class.
*
* @version 1.00, 01/12/99
* @author  Cameron Purdy
*/
public interface TypeInfo
        extends Info
    {
    /**
    * Get the package.
    *
    * @return the package containing this type
    */
    PackageInfo getPackageInfo();

    /**
    * Determine if this type is an interface type.
    *
    * @return true if this is an interface
    */
    boolean isInterface();

    /**
    * Determine if this type is throwable.
    *
    * @return true if this type can be thrown
    */
    boolean isThrowable();

    /**
    * Determine the super type of this type.
    *
    * @return the super class/component of this type or null if none
    */
    TypeInfo getSuperInfo();

    /**
    * Enumerate all interfaces implemented by this type.  If this type is
    * an interface, this method enumerates the interfaces that are extended
    * by this interface, both directly and indirectly.
    *
    * @return an enumeration of DataType objects which represent
    *         interfaces implemented by this type
    */
    Enumeration interfaceTypes();

    /**
    * Test if the specified interface is implemented by this type.
    *
    * @param dt  the interface type
    *
    * @return true if this type implements the specified interface
    */
    boolean isInterface(DataType dt);

    /**
    * Enumerate all fields on this type.
    *
    * @return an enumeration of field names
    */
    Enumeration fieldNames();

    /**
    * Get the info for a field that exists on this type.
    *
    * @param sField  the name of the field
    *
    * @return the FieldInfo for the field or null
    */
    FieldInfo getFieldInfo(String sField);

    /**
    * Enumerate all unique method names on this type.
    *
    * @return an enumeration of method names
    */
    Enumeration methodNames();

    /**
    * Enumerate parameter lists for all methods on this type that have a
    * specified name and number of parameters.
    *
    * @param sMethod  the method name
    * @param cParams  the number of parameters (or -1 for any number)
    *
    * @return an enumeration of DataType arrays for the specified method name
    */
    Enumeration paramTypes(String sMethod, int cParams);

    /**
    * Get the info for a method that exists on this type.
    *
    * @param sMethod   the name of the method
    * @param adtParam  an array of DataType for each parameter (0..n-1)
    *
    * @return the MethodInfo for the method
    */
    MethodInfo getMethodInfo(String sMethod, DataType[] adtParam);

    /**
    * Determine the parent for this type.  A parent type is only applicable
    * for components.
    *
    * @return the TypeInfo for the parent or null if there is no parent
    */
    TypeInfo getParentInfo();

    /**
    * Enumerate all children of this type.
    *
    * @return an enumeration of child names
    */
    Enumeration childNames();

    /**
    * Get a child of this type.  A child type is only applicable for
    * components.
    *
    * @param sChild  the name of the child
    *
    * @return the TypeInfo for the child
    */
    TypeInfo getChildInfo(String sChild);
    }
