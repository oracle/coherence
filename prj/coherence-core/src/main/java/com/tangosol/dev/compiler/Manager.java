/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.ClassConstant;
import com.tangosol.dev.assembler.FieldConstant;
import com.tangosol.dev.assembler.MethodConstant;
import com.tangosol.dev.assembler.Field;
import com.tangosol.dev.assembler.Method;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;


/**
* This interface represents the process which is managing the class
* compilation.  It answers several questions for the context which
* require information that only the class compilation process has.
*
* @version 1.00, 01/28/98
* @author  Cameron Purdy
*/
public interface Manager
    {
    /**
    * Create a synthetic field required by the context.
    *
    * @return a new synthetic field
    */
    Field addSyntheticField(DataType dt);

    /**
    * Create a synthetic method required by the context.
    *
    * @param adt  an array of data types, [0] for the return type and
    *             [1..n] for the parameters
    *
    * @return a new synthetic field
    */
    Method addSyntheticMethod(DataType[] adt);

    /**
    * Determine the class constant for the specified type.
    *
    * @param dtClass  the class/component data type
    *
    * @return the class constant to use
    */
    ClassConstant resolveClass(DataType dtClass);

    /**
    * Determine the field constant for the specified field.
    *
    * If the field referred to by the method constant exists, then the
    * manager returns it unchanged.  If the field does not exist, the
    * manager determines what field constant should be used instead.  This
    * is used primarily to find what derivation level a field exists at.
    *
    * @param dtClass  the data type that the field exists on (or a subclass
    *                 thereof)
    * @param dtField  the type of the field (can be ignored)
    * @param sName    the name of the field
    *
    * @return  the field constant to use
    */
    FieldConstant resolveField(DataType dtClass, DataType dtField, String sName);

    /**
    * Determine the method constant for the specified method.
    *
    * @param dtClass   the data type that the method exists on (or a subclass
    *                  thereof)
    * @param dtReturn  return value data type (can be ignored)
    * @param sName     the method name
    * @param adtParam  array of parameter data types
    *
    * @return  the method constant to call or null if the method call must
    *          be inlined
    */
    MethodConstant resolveMethod(DataType dtClass, DataType dtReturn, String sName, DataType[] adtParam);

    /**
    * Determine if the specified method could be inlined.
    *
    * If the method referred to by the method constant can be inlined,
    * return the field constant that inlines it.  Naming conventions
    * and design patterns are based on the Java beans specification.
    *
    * @param dtClass   the data type that the method exists on (or a subclass
    *                  thereof)
    * @param dtReturn  return value data type (can be ignored)
    * @param sName     the method name
    * @param adtParam  array of parameter data types
    *
    * @return  the field constant to use instead of calling the method
    *          or null if the method cannot be inlined
    */
    FieldConstant inlineMethod(DataType dtClass, DataType dtReturn, String sName, DataType[] adtParam);

    /**
    * Obtain the manager's current error list to log an error to.
    *
    * @return  an instance of com.tangosol.util.ErrorList to use
    *          to log an error to
    */
    ErrorList getErrorList();
    }
