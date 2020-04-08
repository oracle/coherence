/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import com.tangosol.util.Base;

import  java.io.Serializable;


/**
* This class is used to represent a named Java class by name only.
*/
public class ClassReference
        extends    Base
        implements Serializable
    {
    /**
    * Construct a ClassReference for the specified class name.
    */
    public ClassReference(String className)
        {
        if (className.startsWith("["))
            {
            int ofStart = 0;

            while (className.charAt(++ofStart) == '[') {}; // skip all '['

            if (className.charAt(ofStart) != 'L')
                {
                // intrinsic element type -- substitute with void to ignore
                className = "void";
                }
            else
                {
                int ofEnd = ++ofStart; // skip 'L'

                while (className.charAt(++ofEnd) != ';') {};   // skip till ';'

                className = className.substring(ofStart, ofEnd);
                }
            }
        this.className = className;
        }

    /**
    * Return the name of the referenced class.
    */
    public String getClassName()
        {
        return(className);
        }

    public int hashCode()
        {
        return(className.hashCode());
        }

    public boolean equals(Object obj)
        {
        if (obj instanceof ClassReference)
            {
            return(((ClassReference)obj).className.equals(className));
            }
        else
            {
            return(false);
            }
        }

    private final String className;
    }
