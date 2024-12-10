/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


/**
* The ParamInfo interface represents the information about a method
* parameter.
*
* @version 1.00, 01/13/99
* @author  Cameron Purdy
*/
public interface ParamInfo
        extends Info
    {
    /**
    * Determine the method that contains this parameter.
    *
    * @return the MethodInfo containing the parameter
    */
    MethodInfo getMethodInfo();
    }
