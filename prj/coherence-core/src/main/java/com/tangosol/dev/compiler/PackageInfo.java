/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


import java.util.Enumeration;


/**
* The PackageInfo interface represents the information about a package.
*
* @version 1.00, 01/12/99
* @author  Cameron Purdy
*/
public interface PackageInfo
        extends Info
    {
    /**
    * Get the containing package.
    *
    * @return the package containing this package or null if this is
    *         the root package
    */
    PackageInfo getPackageInfo();

    /**
    * Enumerate all sub-packages in this package.
    *
    * @return an enumeration of short (unqualified) sub-package names
    */
    Enumeration packageNames();

    /**
    * Get the specified sub-package.
    *
    * @param sPkg  the short package name
    *
    * @return the PackageInfo for the specified package or null
    */
    PackageInfo getPackageInfo(String sPkg);

    /**
    * Enumerate all type names in this package.  Inner classes are in the
    * form "<outer>$<inner>".
    *
    * @return an enumeration of short (unqualified) type names
    */
    Enumeration typeNames();

    /**
    * Get the specified type.
    *
    * @param sType  the short name of a type in this package
    *
    * @return the TypeInfo for the specified type or null
    */
    TypeInfo getTypeInfo(String sType);
    }
