/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.engarde;


import java.io.IOException;


/**
* This interface represents an object that is capable of writing out
* a content of Java application.  The application could be stored as
* a directory structure or a java archive.
*
* @see com.tangosol.engarde.DirectoryStorage
* @see com.tangosol.engarde.JarStorage
*
* @version 1.00 10/08/01
* @author gg
*/
public interface ApplicationWriter
    {
    /**
    * Create a new application entry with a given name and prepare to start
    * writing the entry data.  Close the current entry if still active.
    *
    * @param sName the application entry name
    *
    * @return a newly created AppplicationEntry
    *
    * @exception  IOException if an I/O error has occurred
    */
    public ApplicationEntry createEntry(String sName)
        throws IOException;

    /**
    * Create a new application entry with all attributes of the specified
    * entry and prepare to start writing the entry data.
    * Close the current entry if still active.
    *
    * @param entry an application entry
    *
    * @return a newly created AppplicationEntry
    *
    * @exception  IOException if an I/O error has occurred
    */
    public ApplicationEntry createEntry(ApplicationEntry entry)
        throws IOException;

    /**
    * Write an array of bytes to the current application entry data.
    * This method will block until all the bytes are written.
    *
    * @param abData the data to be written
    * @param of     the offset into the array
    * @param cb     the number of bytes to write
    * 
    * @exception  IOException if an I/O error has occurred
    */
    public void writeEntryData(byte[] abData, int of, int cb)
        throws IOException;

    /**
    * Close the current application entry.
    *
    * @exception  IOException if an I/O error has occurred
    */
    public void closeEntry()
        throws IOException;

    /**
    * Close the application writer and release all used resources
    */
    public void close();
    }
