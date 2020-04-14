/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


/**
* This kind of PackagerPath is defined in terms of a file pathname.
*/
public class FilePackagerPath
        extends PackagerPath
    {
    /**
    * Construct a FilePackagerPath.
    */
    public FilePackagerPath()
        {
        this(null);
        }

    /**
    * Construct a FilePackagerPath to model the specified file pathname.
    */
    public FilePackagerPath(String filePathName)
        {
        setPathName(filePathName);
        }

    /**
    * Return the pathname modelled by the FilePackagerPath.
    */
    public String getPathName()
        {
        return(filePathName);
        }

    /**
    * Set the pathname modelled by the FilePackagerPath.
    */
    public void setPathName(String filePathName)
        {
        this.filePathName = filePathName;
        }

    private String filePathName;
    }
