/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import  java.io.File;


/**
* This kind of PackagerPath is defined in terms of a directory pathname.
*/
public class DirectoryPackagerPath
        extends FilePackagerPath
    {
    /**
    * Construct a DirectoryPackagerPath.
    */
    public DirectoryPackagerPath()
        {
        super(null);
        }

    /**
    * Construct a DirectoryPackagerPath with the specified directory pathname.
    */
    public DirectoryPackagerPath(String directoryPathName)
        {
        super(null);

        String dirName = directoryPathName;

        if (File.separatorChar != '/')
            {
            dirName = directoryPathName.replace(File.separatorChar, '/');
            }

        if (!dirName.endsWith("/"))
            {
            dirName = dirName + "/";
            }

        setPathName(dirName);
        }
    }
