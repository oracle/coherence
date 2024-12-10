/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import  java.io.ByteArrayOutputStream;
import  java.io.File;
import  java.io.InputStream;
import  java.io.IOException;

import  java.util.Properties;


/**
* This kind of PackagerEntry is specified as a Component entry
* and is a wrapper around Component.Dev.Packager.Entry component
*/
public class PackagerComponentEntry
        extends PackagerClassEntry
    {
    /**
    * Create a new PackagerComponentEntry as an uninitialized Java Bean.
    */
    public PackagerComponentEntry()
        {
        this(null);
        }

    /**
    * Create a new PackagerComponentEntry for the specified CDEntry
    * (passed in as an PackagerEntry interface)
    */
    public PackagerComponentEntry(PackagerEntry entry)
        {
        super(entry == null ? null : entry.getClass().getName());
        setCDEntry(entry);
        }

    public PackagerEntry getCDEntry()
        {
        return entry;
        }

    public void setCDEntry(PackagerEntry entry)
        {
        this.entry = entry;
        }

    /**
    * Return a readable representation of the PackagerComponentEntry.
    */
    public String toString()
        {
        return String.valueOf(entry);
        }

    /**
    * Returns a PackagerPath for the PackagerEntry.
    */
    public PackagerPath getPath()
        {
        return entry.getPath();
        }

    /**
    * Returns the PackagerEntry as binary data suitable for packager.
    * The specified ClassLoader is not used for this entry type.
    */
    public byte[] getData(ClassLoader classLoader)
            throws PackagerEntryNotFoundException
        {
        return entry.getData(null);
        }

    /**
    * Return the modification time of the class file, if the class is loaded
    * from a normal file in the classpath.  Otherwise, returns the current
    * time.
    */
    public long getModificationTime()
        {
        long modificationTime = entry.getModificationTime();

        return modificationTime == -1 ? super.getModificationTime() : modificationTime;
        }

    /**
    * Returns a comment string for the PackagerComponentEntry.
    */
    public String getComment()
        {
        return entry.getComment();
        }

    /**
    * Returns the map of attribute names to attribute values
    * for examination by the Packager.
    */
    public Properties getAttributes()
        {
        return entry.getAttributes();
        }

    /**
    * Returns true if this entry has to be "secured" in a package
    */
    public boolean isSecured()
        {
        return entry.isSecured();
        }


    // ----- data members ---------------------------------------------------

    private PackagerEntry  entry;
    }
