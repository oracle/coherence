/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.engarde;


import java.io.InputStream;
import java.io.IOException;

import java.util.Enumeration;


/**
* This interface represents an object that is capable of reading
* a content of Java application.  The application could exist as
* a directory structure or a java archive.
*
* @see com.tangosol.engarde.DirectoryStorage
* @see com.tangosol.engarde.JarStorage
*
* @version 1.00 10/08/01
* @author gg
*/
public interface ApplicationReader
    {
    /**
    * Return an input stream for reading the content of the specified
    * application entry.
    *
    * @param  entry the application entry
    *
    * @return an input stream for reading the content of the specified
    *         application entry or null if not found
    * 
    * @exception IOException if an I/O error has occurred
    */
    public InputStream getInputStream(ApplicationEntry entry)
        throws IOException;

    /**
    * Return an ApplicationReader that represents an application containted
    * within this application as a specified application entry
    *
    * @param  sName name of the application entry
    *
    * @return an ApplicationReader object for reading the content of 
    *         the contained application or null if not found
    * 
    * @exception IOException if an I/O error has occurred
    */
    public ApplicationReader extractApplication(String sName)
        throws IOException;


    /**
    * Return an ApplicationEntry object for the given name
    *
    * @param sName  name of the application entry
    *
    * @return ApplicationEntry object for the given entry name or
    *         null if not found
    */
    public ApplicationEntry getEntry(String sName);

    /**
    * Return an enumeration of the application entries
    *
    * @return Enumeration of ApplicationEntry objects
    */
    public Enumeration entries();


    /**
    * Close the application reader and release all used resources
    */
    public void close();
    }
