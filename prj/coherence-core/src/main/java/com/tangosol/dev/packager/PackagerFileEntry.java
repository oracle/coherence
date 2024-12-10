/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.List;
import java.util.LinkedList;


/**
* This kind of PackagerEntry is specified using a File pathname
* for the resource and the directory name that this resource
* should be packaged at.
*
* Example: if there is a file c:\MySite\index.html that should be packaged
* into a jar file as html\public\index.html, then creating
*     new PackagerFileEntry("c:\MySite\index.html", "html\public")
* could be used to accomplish the task.
*/
public class PackagerFileEntry
        extends    PackagerBaseEntry
        implements PackagerDependencyElement
    {
    /**
    * Create a new PackagerFileEntry as an uninitialized Java Bean.
    */
    public PackagerFileEntry()
        {
        super();
        }

    /**
    * Create a new PackagerFileEntry with the specified file name
    * and the specified package path
    */
    public PackagerFileEntry(String filePath, String packagedDirectoryPath)
        {
        super();

        setFilePath(filePath);
        setPackagedDirectoryPath(packagedDirectoryPath);
        }

    /**
    * Return the PackagerFileEntry's file-pathname.
    */
    public String getFilePath()
        {
        return filePath;
        }

    /**
    * Assign the PackagerFileEntry's file-pathname.
    */
    public void setFilePath(String filePath)
        {
        this.filePath = filePath;
        }

    /**
    * Return the directory name that this PackagerFileEntry
    * should be packaged at.
    */
    public String getPackagedDirectoryPath()
        {
        return packagedDirectoryPath;
        }

    /**
    * Assign the directory name that this PackagerFileEntry
    * should be packaged at.
    */
    public void setPackagedDirectoryPath(String directoryPath)
        {
        // make sure the directoryPath ends with '/'
        // we could hard code the '/' since the JarFile does as well
        directoryPath = directoryPath.replace('\\', '/');
        if (directoryPath.length() > 0 && !directoryPath.endsWith("/"))
            {
            directoryPath = directoryPath + "/";
            }
        this.packagedDirectoryPath = directoryPath;
        }

    /**
    * Returns a PackagerPath for the PackagerEntry.
    */
    public PackagerPath getPath()
        {
        File   entryFile     = getFile();
        String directoryPath = getPackagedDirectoryPath();

        return new FilePackagerPath(directoryPath + entryFile.getName());
        }

    /**
    * Returns a File that contains the PackagerEntry.
    */
    public File getFile()
        {
        return new File(getFilePath());
        }

    /**
    * Returns the PackagerEntry as binary data suitable for packager.
    */
    public byte[] getData(ClassLoader classLoader)
            throws PackagerEntryNotFoundException
        {
        File entryFile = getFile();
        if (!entryFile.exists())
            {
            throw new PackagerEntryNotFoundException(entryFile.toString());
            }

        try
            {
            return read(entryFile);
            }
        catch (IOException ioe)
            {
            throw new UnexpectedPackagerException(ioe);
            }
        }

    /**
    * Return a comment for the PackagerEntry.
    */
    public String getComment()
        {
        return null;
        }

    /**
    * Return a readable representation of the PackagerFileEntry.
    */
    public String toString()
        {
        return "FileEntry: " + getPath();
        }


    // PackagerDependencyElement interface
    /**
    * Return the immediate dependents of this PackagerDependencyElement
    * in the context of the specified ClassLoader.
    */
    public List getDependents(ClassLoader classLoader)
        {
        return null;
        }

    /**
    * Return the direct PackagerEntries of this PackagerDependencyElement.
    */
    public List getPackagerEntries()
        {
        LinkedList list = new LinkedList();
        list.add(this);
        return list;
        }


    // ----- data members ---------------------------------------------------

    private String filePath;
    private String packagedDirectoryPath;
    }
