/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


import com.tangosol.util.ClassHelper;
import com.tangosol.util.ErrorList;
import com.tangosol.util.Resources;


/**
* Compiler constants.
*
* @version 1.00, 2000.03.22
* @author  Cameron Purdy
*/
public interface Constants
        extends ErrorList.Constants
    {
    /**
    * The package resources.
    */
    public static final Resources RESOURCES =
            ClassHelper.getPackageResources("com.tangosol.dev.compiler.");


    // ----- error codes ----------------------------------------------------

    public static final String WARN_DEPRECATED    = "CMP-001";
    }
