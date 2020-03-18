/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import  java.io.IOException;
import  java.io.ObjectInputStream;


/**
* This kind of PackagerPath is defined in terms of a Java Package.
*/
public class JavaPackagePackagerPath
        extends PackagerPath
    {
    /**
    * Construct a JavaPackagePackagerPath.
    */
    public JavaPackagePackagerPath()
        {
        this(null);
        }

    /**
    * Construct a JavaPackagePackagerPath to model the specified Java package.
    */
    public JavaPackagePackagerPath(String javaPackageName)
        {
        setJavaPackageName(javaPackageName);
        }

    /**
    * Return the pathname as a String, using forward slashes to delimit
    * logical directory levels.  The pathname for a JavaPackagePackagerPath
    * ends with a trailing forward slash.
    */
    public String getPathName()
        {
        String javaPackagePathName = javaPackageName.replace('.', '/');
        String packageDirectoryPathName  = ( javaPackagePathName + '/' );
        return(packageDirectoryPathName);
        }

    /**
    * Return the name of the Java package modelled by the
    * JavaPackagePackagerPath.
    */
    public String getJavaPackageName()
        {
        return(javaPackageName);
        }

    /**
    * Set the Java package modelled by the JavaPackagePackagerPath.
    */
    public void setJavaPackageName(String javaPackageName)
        {
        this.javaPackageName = javaPackageName;
        }

    private               String  javaPackageName;
    }
