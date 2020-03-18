/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


import com.tangosol.util.Resources;


/**
* @version 1.00, 2000.03.22
*/
public class PackageResources
        extends Resources
        implements Constants
    {
    public Object[][] getContents()
        {
        return resources;
        }

    static final Object[][] resources =
        {
        {WARN_DEPRECATED, "Deprecated feature:  \"{0}\"."},
        };
    }
