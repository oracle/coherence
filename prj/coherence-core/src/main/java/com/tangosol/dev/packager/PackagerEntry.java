/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import java.util.Properties;


/**
* This interface represents a Component.Dev.Packager.Entry component as
* seen by the Packager
*
* @version 0.1
*/
public interface PackagerEntry
    {
    /**
    * Returns a PackagerPath for this entry.
    */
    public PackagerPath getPath();

    /**
    * Returns the binary data for this entry.
    *
    * @throws  PackagerEntryNotFoundException if an error occurs getting the data
    */
    public byte[] getData(ClassLoader classLoader)
        throws PackagerEntryNotFoundException;

    /**
    * Returns the time when the entry was last modified.
    */
    public long getModificationTime();

    /**
    * Returns a comment for the PackagerEntry.
    */
    public String getComment();

    /**
    * Returns the map of attribute names to attribute values
    * for examination by the Packager.
    */
    public Properties getAttributes();

    /**
    * Returns true if this entry has to be "secured" in a package
    */
    public boolean isSecured();
    }
