/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


import java.util.Enumeration;


/**
* The MethodInfo interface represents the information about a method
* of a class.
*
* @version 1.00, 09/15/98
* @author  Cameron Purdy
*/
public interface MethodInfo
        extends MemberInfo
    {
    /**
    * Get the number of method parameters.
    *
    * @return the number of method parameters
    */
    int getParamCount();

    /**
    * Get information about a specified parameter.
    *
    * @param i  a 0-based parameter index
    *
    * @return the specified parameter info
    */
    ParamInfo getParamInfo(int i);

    /**
    * Get an enumeration of exceptions throwable by the method.
    *
    * @return an enumeration of DataType for each declared exception
    */
    Enumeration exceptionTypes();

    /**
    * If the method (e.g. simple accessor) is inlineable, this method returns
    * the field information that must be referenced by the inlined code.
    *
    * @return the field info to use when inlining or null if inlining is not
    *         possible
    */
    FieldInfo getFieldInfo();
    }
