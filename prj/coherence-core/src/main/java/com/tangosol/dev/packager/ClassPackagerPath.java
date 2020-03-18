/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


/**
*  This kind of PackagerPath is defined in terms of a class's package
*  and class name.
*/
public class ClassPackagerPath
        extends PackagerPath
    {
    /**
    *  Construct a ClassPackagerPath.
    */
    public ClassPackagerPath()
        {
        this("java.lang.Object");
        }

    /**
    *  Construct a ClassPackagerPath for the specified class.
    */
    public ClassPackagerPath(String packagedClassName)
        {
        setPackagedClassName(packagedClassName);
        }

    /**
    *  Return the pathname as a String, using forward slashes to delimit
    *  logical directory levels.
    */
    public String getPathName()
        {
        String classPathName = packagedClassName.replace('.', '/');

        // [gg] somehow a signature comes in!
        if (classPathName.startsWith("L") && classPathName.endsWith(";"))
            {
            classPathName = classPathName.substring(1, classPathName.length() - 1);
            }
        if (classPathName.length() == 1)
            {
            classPathName = "java/lang/Object";
            }

        return(classPathName + ".class");
        }

    /**
    *  Return the class modelled by the ClassPackagerPath.
    */
    public String getPackagedClassName()
        {
        return(packagedClassName);
        }

    /**
    *  Set the class modelled by the ClassPackagerPath.
    */
    public void setPackagedClassName(String packagedClassName)
        {
        this.packagedClassName = packagedClassName;
        }

    private String packagedClassName;
    }
