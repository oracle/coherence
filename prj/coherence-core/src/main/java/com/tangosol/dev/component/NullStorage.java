/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import java.io.IOException;

import com.tangosol.dev.assembler.ClassFile;

import com.tangosol.util.ErrorList;
import com.tangosol.util.StringTable;


/**
* NullStorage is a Storage that does nothing.
*
* @version 1.00, 03/14/2001
* @author  gg
*/
public class NullStorage
        extends BaseStorage
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a null storage object.
    */
    public NullStorage()
        {
        }


    // ----- Components -----------------------------------------------------

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
            throws ComponentException
        {
        return null;
        }

    /**
    * Store the specified Component.
    *
    * @param cd       the Component Definition to be stored
    * @param errlist  the ErrorList object to log any derivation/modification
    *                 errors to
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public void storeComponent(Component cd, ErrorList errlist)
            throws ComponentException
        {
        throw new UnsupportedOperationException("Read-only storage: " + this);
        }


    // ----- Java Class Signatures ------------------------------------------

    /**
    * Load the specified Class Signature.
    *
    * @param sName    qualified Java Class Signature (JCS) name
    *
    * @return the specified Class Signature
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public Component loadSignature(String sName)
            throws ComponentException
        {
        return null;
        }

    /**
    * Store the specified generated Java Class Signature.
    *
    * @param cdJCS  the Java Class Signature
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public void storeSignature(Component cdJCS)
            throws ComponentException
        {
        throw new UnsupportedOperationException("Read-only storage: " + this);
        }

    /**
    * Remove the specified Component.
    *
    * @param sName fully qualified Component Definition name
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public void removeComponent(String sName)
            throws ComponentException
        {
        throw new UnsupportedOperationException("Read-only storage: " + this);
        }


    // ----- Classes --------------------------------------------------------

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
            throws ComponentException
        {
        return null;
        }

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
            throws ComponentException
        {
        return null;
        }

    /**
    * Store the specified generated Java Class along with its listing
    *
    * @param clz       the generated Class structure to store
    * @param sListing  (optional) the java listing of the class
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public void storeClass(ClassFile clz, String sListing)
            throws ComponentException
        {
        throw new UnsupportedOperationException("Read-only storage: " + this);
        }


    // ----- Java -------------------------------------------------------------

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
            throws IOException
        {
        return null;
        }


    // ----- Resources --------------------------------------------------------

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
            throws IOException
        {
        return null;
        }

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
            throws IOException
        {
        return null;
        }

    /**
    * Store the specified Resource Signature.
    *
    * @param sName  fully qualified resource name
    * @param abData the specified Resource Signature as a byte array
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public void storeResourceSignature(String sName, byte[] abData)
            throws IOException
        {
        throw new UnsupportedOperationException("Read-only storage: " + this);
        }

    /**
    * Remove the specified Resource Signature
    *
    * @param sName fully qualified resource name
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public void removeResourceSignature(String sName)
            throws IOException
        {
        throw new UnsupportedOperationException("Read-only storage: " + this);
        }

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
            throws IOException
        {
        return null;
        }

    /**
    * Store the specified resource.
    *
    * @param sName  fully qualified resource name
    * @param abData the specified resource as a byte array
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public void storeResource(String sName, byte[] abData)
            throws IOException
        {
        throw new UnsupportedOperationException("Read-only storage: " + this);
        }


    // ---- component managment -----------------------------------------------

    /**
    * Return a StringTable which contains the names of Component Definitions
    * (CD) that derive from the specified Component Definition
    *
    * @param  sComponent  the qualified CD name
    * @param  fQualify    if set to true, return fully qualified CD names;
    *                     otherwise -- non-qualified names
    *
    * @return StringTable object with Component Definition names as keys
    */
    public StringTable getSubComponents(String sComponent, boolean fQualify)
        {
        return new StringTable();
        }

    /**
    * Return a StringTable which contains the names of Component Definitions
    * (CD) that belong to the specified package
    *
    * @param  sPackage  the qualified package name
    * @param  fQualify  if set to true, return fully qualified CD names;
    *                   otherwise -- non-qualified names
    *
    * @return StringTable object with CD names as keys
    */
    public StringTable getPackageComponents(String sPackage, boolean fQualify)
        {
        return new StringTable();
        }

    /**
    * Return a StringTable which contains the names of sub-packages in
    * the specified component package
    *
    * @param sPackage   the qualified package name; pass null to retrieve
    *                   the top level component packages
    * @param fQualify   if set to true, return fully qualified package names;
    *                   otherwise -- non-qualified names
    * @param fSubs      if set to true, returns the entire tree of sub-packages
    *
    * @return StringTable object with package names as keys
    */
    public StringTable getComponentPackages(String sPackage, boolean fQualify, boolean fSubs)
        {
        return new StringTable();
        }

    /**
    * Return a StringTable which contains the names of Java Class Signature
    * (JCS) names that belong to the specified java class package
    * (i.e. "javax.swing")
    *
    * @param sPackage   the qualified package name
    * @param fQualify   if set to true, return fully qualified JCS names;
    *                   otherwise -- non-qualified names
    *
    * @return StringTable object with JCS names as keys
    */
    public StringTable getPackageSignatures(String sPackage, boolean fQualify)
        {
        return new StringTable();
        }

    /**
    * Return a StringTable which contains the names of sub-packages in
    * the specified java class package
    * (i.e. "javax.swing" is a sub-package of "javax" package)
    *
    * @param sPackage   the qualified package name; pass null to retrieve
    *                   the top level java class packages
    * @param fQualify   if set to true, return fully qualified JCS names;
    *                   otherwise -- non-qualified names
    * @param fSubs      if set to true, returns the entire tree of sub-packages
    *
    * @return StringTable object with package names as keys
    */
    public StringTable getSignaturePackages(String sPackage, boolean fQualify, boolean fSubs)
        {
        return new StringTable();
        }

    /**
    * Return a StringTable which contains the names of resource
    * names that belong to the specified package
    * (i.e. "img/tde")
    *
    * @param sPackage   the qualified package name
    * @param fQualify   if set to true, return fully qualified resource names;
    *                   otherwise -- non-qualified names
    *
    * @return StringTable object with resource names as keys
    */
    public StringTable getPackageResources(String sPackage, boolean fQualify)
        {
        return new StringTable();
        }


    /**
    * Return a StringTable which contains the names of sub-packages in
    * the specified resource package
    * (i.e. "img/tde" is a sub-package of "img" package)
    *
    * @param sPackage   the qualified package name; pass null to retrieve
    *                   the top level resource packages
    * @param fQualify   if set to true, return fully qualified package names;
    *                   otherwise -- non-qualified names
    * @param fSubs      if set to true, returns the entire tree of sub-packages
    *
    * @return StringTable object with package names as keys
    */
    public StringTable getResourcePackages(String sPackage, boolean fQualify, boolean fSubs)
        {
        return new StringTable();
        }

    /**
    * Provide a short human-readable description of the trait.
    *
    * @return a human-readable description of this trait
    */
    public String toString()
        {
        return CLASS;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "NullStorage";
    }
