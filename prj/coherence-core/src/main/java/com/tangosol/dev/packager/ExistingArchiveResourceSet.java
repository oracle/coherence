/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import com.tangosol.util.Base;

import java.io.IOException;

import java.util.zip.ZipFile;


/**
* This kind of ResourceSet is used to model existing Zip files and Jar files
* that are expected to be available exactly as is in an application's
* runtime environment.
*/
public class ExistingArchiveResourceSet
        extends ResourceSet
    {
    /**
    * Construct an ExistingArchiveResourceSet to model the specified
    * zip-file archive.
    */
    public ExistingArchiveResourceSet(String zipFilePathName)
            throws IOException
        {
        this.zipFilePathName = zipFilePathName;
        getZipFile();
        }

    /**
    * Construct an ExistingArchiveResourceSet to model the specified
    * zip-file archive.
    */
    public ExistingArchiveResourceSet(ZipFile zipFile)
        {
        setZipFile(zipFile);
        }

    /**
    * Indicate whether or not an entry with the specified path is a member
    * of the ResourceSet.
    */
    public boolean containsKey(PackagerPath path)
        {
        String pathName = path.getPathName();
        try
            {
            return(getZipFile().getEntry(pathName) != null);
            }
        catch (IOException ioe)
            {
            throw(new UnexpectedPackagerException(ioe));
            }
        }

    /**
    * Return the zip-file modelled by the ExistingArchiveResourceSet.
    */
    public ZipFile getZipFile()
            throws IOException
        {
        if (zipFile == null && zipFilePathName != null)
            {
            zipFile = new ZipFile(zipFilePathName);
            }

        return(zipFile);
        }

    /**
    * Set the zip-file modelled by the ExistingArchiveResourceSet.
    */
    public void setZipFile(ZipFile zipFile)
        {
        this.zipFile = zipFile;
        this.zipFilePathName = ((zipFile == null) ? null : zipFile.getName());
        }


    ///
    ///  Unit test
    ///  args:  archive-file entry-name ...
    ///
    public static void main(String args[])
        {
        try
            {
            ExistingArchiveResourceSet archive =
                    new ExistingArchiveResourceSet(args[0]);
            for (int i = 1; i < args.length; i++)
                {
                Base.out(archive.containsKey(new FilePackagerPath(args[i])) + "\t" + args[i]);
                }
            }
        catch (Exception e)
            {
            Base.out(e);
            }
        }


    private transient     ZipFile         zipFile;
    private               String          zipFilePathName;
    }
