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
* Storage represents the ability to read and write Component Definitions,
* and Java Class Signatures.
*
* @version 1.00, 02/04/98
* @author  Cameron Purdy
*/
public interface Storage
        extends Loader
    {
    /**
    * Store the specified Component.
    *
    * @param cd       the Component Definition
    * @param errlist  the ErrorList object to log any derivation/modification
    *                 errors to
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public void storeComponent(Component cd, ErrorList errlist)
            throws ComponentException;

    /**
    * Remove the specified Component.
    *
    * @param sName fully qualified Component Definition name
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public void removeComponent(String sName)
            throws ComponentException;

    /**
    * Store the specified generated Java Class Signature.
    *
    * @param signature  the Java Class Signature
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public void storeSignature(Component signature)
            throws ComponentException;

    /**
    * Store the specified generated Java Class along with its listing
    *
    * @param clz       the generated Class structure to store
    * @param sListing  (optional) the java listing of the class
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public void storeClass(ClassFile clz, String sListing)
            throws ComponentException;

    /**
    * Store the specified Resource Signature.
    *
    * @param sName  fully qualified resource name
    * @param abData the specified Resource Signature as a byte array
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public void storeResourceSignature(String sName, byte[] abData)
            throws IOException;

    /**
    * Store the specified resource.
    *
    * @param sName  fully qualified resource name
    * @param abData the specified resource as a byte array
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public void storeResource(String sName, byte[] abData)
            throws IOException;

    /**
    * Remove the specified Resource Signature
    *
    * @param sName fully qualified resource name
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public void removeResourceSignature(String sName)
            throws IOException;

    // ---- component management -------------------------------------------

    /**
    * Return a StringTable which contains the names of Component Definitions
    * (CD) that derive from the specified Component Definition

    * @param  sComponent  the qualified CD name
    * @param  fQualify    if set to true, return fully qualified CD names;
    *                     otherwise -- non-qualified names
    *
    * @return StringTable object with Component Definition names as keys
    */
    public StringTable getSubComponents(String sComponent, boolean fQualify);


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
    public StringTable getPackageComponents(String sPackage, boolean fQualify);


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
    public StringTable getComponentPackages(String sPackage, boolean fQualify, boolean fSubs);


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
    public StringTable getPackageSignatures(String sPackage, boolean fQualify);


    /**
    * Return a StringTable which contains the names of sub-packages in
    * the specified java class package
    * (i.e. "javax.swing" is a sub-package of "javax" package)
    *
    * @param sPackage   the qualified package name; pass null to retrieve
    *                   the top level java class packages
    * @param fQualify   if set to true, return fully qualified package names;
    *                   otherwise -- non-qualified names
    * @param fSubs      if set to true, returns the entire tree of sub-packages
    *
    * @return StringTable object with package names as keys
    */
    public StringTable getSignaturePackages(String sPackage, boolean fQualify, boolean fSubs);

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
    public StringTable getPackageResources(String sPackage, boolean fQualify);


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
    public StringTable getResourcePackages(String sPackage, boolean fQualify, boolean fSubs);
    }
