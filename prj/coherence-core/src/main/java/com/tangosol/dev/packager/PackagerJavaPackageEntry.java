/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import  java.io.File;
import  java.io.IOException;
import  java.io.ObjectInputStream;


/**
* This kind of PackagerEntry is for a Java package.
*/
public class PackagerJavaPackageEntry
        extends PackagerBaseEntry
    {
    /**
    * Construct a PackagerJavaPackageEntry.
    */
    public PackagerJavaPackageEntry()
        {
        }

    /**
    * Construct a PackagerJavaPackageEntry to model the specified Java package.
    */
    public PackagerJavaPackageEntry(String javaPackageName)
        {
        setJavaPackageName(javaPackageName);
        }

    /**
    * Returns a PackagerPath for the PackagerEntry.
    */
    public PackagerPath getPath()
        {
        return new JavaPackagePackagerPath(getJavaPackageName());
        }

    /**
    * Returns the PackagerEntry as binary data suitable for packager.
    */
    public byte[] getData(ClassLoader classLoader)
            throws PackagerEntryNotFoundException, UnexpectedPackagerException
        {
        return EMPTY_DATA;
        }

    /**
    * Return the Java package modelled by the PackagerJavaPackageEntry.
    */
    public String getJavaPackageName()
        {
        return javaPackageName;
        }

    /**
    * Set the Java package modelled by the PackagerJavaPackageEntry.
    */
    public void setJavaPackageName(String javaPackageName)
        {
        this.javaPackageName = javaPackageName;
        }

    /**
    * Return a comment String for the PackagerJavaPackageEntry.
    */
    public String getComment()
        {
        return null;
        }

    /**
    * Return the modification time of the package's directory.
    */
    public long getModificationTime()
        {
        long modificationTime = -1L;

        File packageDirectory = findEntryFile();
        if (packageDirectory != null)
            {
            modificationTime = packageDirectory.lastModified();
            }

        return modificationTime == -1 ?
            super.getModificationTime() : modificationTime;
        }

    public String toString()
        {
        return "package " + javaPackageName;
        }


    // ----- data members ---------------------------------------------------

    private               String  javaPackageName;
    private static byte[] EMPTY_DATA = new byte[0];
    }
