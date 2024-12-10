/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.engarde;


/**
* This interface represents a Java application entry that is read
* by the ApplicationReader and written out by the ApplicationWriter
*
* @version	1.0 10/08/01
* @author	gg
*
* @see com.tangosol.engarde.DirectoryStorage.Entry
* @see com.tangosol.engarde.JarStorage.Entry
*/

public interface ApplicationEntry
    {
    /**
    * Return the name of the entry.
    *
    * @return the name of the entry
    */
    public String getName();

    /**
    * Return the modification time of the entry, or -1 if not specified.
    *
    * @return the modification time of the entry, or -1 if not specified
    */
    public long getTime();

    /**
    * Set the modification time of the entry.
    *
    * @param lTime the entry modification time in number of milliseconds
    *		 since the epoch
    */
    public void setTime(long lTime);

    /**
    * Return the [uncompressed] size of the entry data, or -1 if not known.
    *
    * @return the size of the entry data, or -1 if not known
    */
    public long getSize();

    /**
    * Set the [uncompressed] size of the entry data.
    *
    * @param lSize the size in bytes
    */
    public void setSize(long lSize);
    }
