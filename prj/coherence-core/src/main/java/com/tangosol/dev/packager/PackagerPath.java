/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import com.tangosol.util.Base;

import java.io.Serializable;


/**
* This class represents a packager pathname for a PackagerEntry.
* It is designed to be usable as a hash-key or as a key in a sorted
* collection.
*/
public abstract class PackagerPath
        extends    Base
        implements Comparable, Serializable
    {
    /**
    * Construct a PackagerPath
    */
    protected PackagerPath()
        {
        }

    /**
    * Return the pathname as a String, using forward slashes to delimit
    * logical directory levels.
    */
    public String toString()
        {
        return(getPathName());
        }

    /**
    * Return the pathname as a String, using forward slashes to delimit
    * logical directory levels.
    */
    public abstract String getPathName();

    /**
    * Two PackagerPaths are considered to be equivalent if they have
    * equivalent pathnames.
    */
    public boolean equals(Object obj)
        {
        if (obj instanceof PackagerPath)
            {
            return(((PackagerPath)obj).getPathName().equals(getPathName()));
            }
        else
            {
            return(false);
            }
        }

    /**
    * Two PackagerPaths have the same hashcode if they have
    * equivalent pathnames.
    */
    public int hashCode()
        {
        return(getPathName().hashCode());
        }

    /**
    * Order PackagerPaths based on the ordering of their pathnames.
    */
    public int compareTo(Object obj)
        {
        PackagerPath that = (PackagerPath)obj;
        return(this.getPathName().compareTo(that.getPathName()));
        }
    }

