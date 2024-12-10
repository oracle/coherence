/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


/**
* The FieldInfo interface represents the information about a field of a
* class.
*
* @version 1.00, 09/15/98
* @author  Cameron Purdy
*/
public interface FieldInfo
        extends MemberInfo
    {
    /**
    * Determine the value of a constant field.  The return value uses the
    * standard object wrappers, such as Integer for int types.
    *
    * @return the constant value of the field
    */
    Object getValue();

    /**
    * Determine if the field cannot be accessed directly but rather must be
    * accessed via a method.
    *
    * @return true if the field must be accessed via an accessor method
    */
    boolean isViaAccessor();

    /**
    * Get the "getter" accessor for the field if the field is accessed via
    * an accessor.
    *
    * @return the method information for the getter or null if non-existent
    */
    MethodInfo getGetterInfo();

    /**
    * Get the "setter" accessor for the field if the field is accessed via
    * an accessor.
    *
    * @return the method information for the setter or null if non-existent
    */
    MethodInfo getSetterInfo();

    /**
    * Get the "indexed getter" accessor for the field if the field is
    * accessed via an accessor.
    *
    * @return the method information for the indexed getter or null if
    *         non-existent
    */
    MethodInfo getIndexedGetterInfo();

    /**
    * Get the "indexed setter" accessor for the field if the field is
    * accessed via an accessor.
    *
    * @return the method information for the indexed setter or null if
    *         non-existent
    */
    MethodInfo getIndexedSetterInfo();
    }
