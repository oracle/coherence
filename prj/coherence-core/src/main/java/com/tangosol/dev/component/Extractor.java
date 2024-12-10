/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import com.tangosol.dev.assembler.ClassFile;

import com.tangosol.util.ErrorList;
import com.tangosol.util.StringTable;

import java.io.IOException;


/**
* A Component Definition Extractor is responsible for storing only the
* derivation for all non-root Component Definitions.
*
* (The complexities associated with modification are isolated to the
* repository type handler for Component Definitions.)
*
* @version 1.00, 02/04/98
* @author  Cameron Purdy
*/
public class Extractor
        extends    Resolver
        implements Storage
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a Extractor given a Storage.
    *
    * @param storage  the Storage object to use to load resolved or derived
    *                 Component Definitions (plus JCS's); note that
    *                 this loader must <B>NOT</B> return modifications
    */
    public Extractor(Storage storage)
        {
        this(storage, null);
        }

    /**
    * Construct a Extractor given a Storage and a package name for
    * relocatable classes.
    *
    * @param storage  the Storage object to use to load resolved or derived
    *                 Component Definitions (plus JCS's); note that
    *                 this loader must <B>NOT</B> return modifications
    * @param sPkg     the package name to use for relocatable classes
    */
    public Extractor(Storage storage, String sPkg)
        {
        super(storage, sPkg);

        m_storage = storage;
        }


    // ----- Storage interface ----------------------------------------------

    /**
    * Store the specified Component as a derivation (unless it is the root).
    *
    * @param cd       the Component Definition
    * @param errlist  the ErrorList object to log any derivation/modification
    *                 errors to
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public void storeComponent(Component cd, ErrorList errlist)
            throws ComponentException
        {
        if (Trait.DEBUG)
            {
            out();
            out("***Extractor*** Component to store:");
            cd.dump();
            }

        if (cd.getMode() != RESOLVED)
            {
            throw new ComponentException(CLASS + ".storeComponent:  " +
                "Invalid mode " + cd + " (" + cd.getMode() + ")");
            }

        if (cd.getQualifiedName().length() > 0)
            {
            // gg: 2001.4.25 allow JCS Interfaces to be resolved
            Component cdSuper = cd.isInterface()
                        ? new Component(null, Component.SIGNATURE, BLANK)
                        : loadComponent(cd.getSuperName(), true, errlist);

            azzert(cdSuper != null, "Failed to load the super component: \"" +
                cd.getSuperName() + '"');

            if (Trait.DEBUG)
                {
                out();
                out("***Extractor*** Super before extract:");
                cdSuper.dump();
                }

            cd = cd.extract(cdSuper, this, errlist);
            if (Trait.DEBUG)
                {
                out();
                out("***Extractor*** Component after extract:");
                cd.dump();
                }
            }

        m_storage.storeComponent(cd, errlist);
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
        m_storage.removeComponent(sName);
        }

    /**
    * Store the specified generated Java Class Signature by delegating to the
    * Extractor's storage.
    *
    * @param signature  the Java Class Signature
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public void storeSignature(Component signature)
            throws ComponentException
        {
        // The only resolved signature is "java.lang.Object"
        // (see Component(ClassFile) constructor)
        boolean fResolvable = signature.getSuperName().length() == 0
            && !signature.isInterface();

        switch (signature.getMode())
            {
            case RESOLVED:
                if (!fResolvable)
                    {
                    throw new ComponentException(CLASS + ".storeSignature:  " +
                        "Invalid mode " + signature + " (" + signature.getMode() + ")");
                    }
                break;

            case DERIVATION:
                if (fResolvable)
                    {
                    throw new ComponentException(CLASS + ".storeSignature:  " +
                        "Invalid mode " + signature + " (" + signature.getMode() + ")");
                    }
                break;

            default:
                throw new ComponentException(CLASS + ".storeSignature:  " +
                    "Invalid mode " + signature + " (" + signature.getMode() + ")");
            }

        m_storage.storeSignature(signature);
        }

    /**
    * Store the specified generated Java Class along with its listing
    * by delegating to the Extractor's storage.
    *
    * @param clz       the generated Class structure to store
    * @param sListing  (optional) the java listing of the class
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public void storeClass(ClassFile clz, String sListing)
            throws ComponentException
        {
        m_storage.storeClass(clz, sListing);
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
        m_storage.storeResourceSignature(sName, abData);
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
        m_storage.removeResourceSignature(sName);
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
        m_storage.storeResource(sName, abData);
        }

    // ----- component management -------------------------------------------

    /**
    * Return a StringTable which contains the names of Component Definitions
    * (CD) that derive from the specified Component Definition

    * @param  sComponent  the qualified CD name
    * @param  fQualify    if set to true, return fully qualified CD names;
    *                     otherwise -- non-qualified names
    *
    * @return StringTable object with Component Definition names as keys
    */
    public StringTable getSubComponents(String sComponent, boolean fQualify)
        {
        return m_storage.getSubComponents(sComponent, fQualify);
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
        return m_storage.getPackageComponents(sPackage, fQualify);
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
        return m_storage.getComponentPackages(sPackage, fQualify, fSubs);
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
        return m_storage.getPackageSignatures(sPackage, fQualify);
        }


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
    public StringTable getSignaturePackages(String sPackage, boolean fQualify, boolean fSubs)
        {
        return m_storage.getSignaturePackages(sPackage, fQualify, fSubs);
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
        return m_storage.getPackageResources(sPackage, fQualify);
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
        return m_storage.getResourcePackages(sPackage, fQualify, fSubs);
        }


    /**
    * Provide a short human-readable description of the trait.
    *
    * @return a human-readable description of this trait
    */
    public String toString()
        {
        return CLASS + '(' + m_storage + ')';
        }

    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Extractor";

    /**
    * The Storage which the Extractor uses to store CD's, and JCS's.
    */
    private Storage m_storage;
    }

