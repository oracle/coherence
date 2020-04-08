/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import com.tangosol.dev.assembler.ClassFile;

import com.tangosol.util.ErrorList;

import java.io.IOException;


/**
* Loader represents the ability to obtain Component Definitions,
* and Java Class Signatures.
*
* @version 1.00, 02/04/98
* @author  Cameron Purdy
*/
public interface Loader
        extends Constants
    {
    /**
    * Load the specified Component.
    *
    * @param sName      fully qualified Component Definition name
    * @param fReadOnly  true if the loaded component will be read-only
    * @param errlist    the ErrorList object to log any derivation/
    *                   modification errors to
    *
    * @return the specified Component Definition or null
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public Component loadComponent(String sName, boolean fReadOnly, ErrorList errlist)
            throws ComponentException;

    /**
    * Load the specified Class Signature.
    *
    * @param sName  qualified Java Class Signature (JCS) name
    *
    * @return the specified Class Signature
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public Component loadSignature(String sName)
            throws ComponentException;

    /**
    * Load the original (before any customization takes place) Java Class.
    *
    * @param sName  fully qualified Java Class name
    *
    * @return the specified Class structure
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public ClassFile loadOriginalClass(String sName)
            throws ComponentException;

    /**
    * Load the specified generated Java Class.
    *
    * @param sName  fully qualified Java Class name
    *
    * @return the specified Class structure
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public ClassFile loadClass(String sName)
            throws ComponentException;

    /**
    * Load the source code for the specified (original) Java class.
    *
    * @param sName  fully qualified Java Class name
    *
    * @return the specified Java source code as a String
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public String loadJava(String sName)
            throws IOException;

    /**
    * Load the original (before any customization takes place) resource.
    *
    * @param sName  fully qualified resource name
    *
    * @return the specified resource as a byte array
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public byte[] loadOriginalResource(String sName)
            throws IOException;

    /**
    * Load the Resource Signature.
    *
    * @param sName  fully qualified resource name
    *
    * @return the specified Resource Signature as a byte array
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public byte[] loadResourceSignature(String sName)
            throws IOException;

    /**
    * Load the generated resource.
    *
    * @param sName  fully qualified resource name
    *
    * @return the specified resource as a byte array
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public byte[] loadResource(String sName)
            throws IOException;
    }
