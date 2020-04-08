/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import com.tangosol.dev.assembler.ClassFile;

import com.tangosol.util.Base;
import com.tangosol.util.ErrorList;
import com.tangosol.util.StringTable;
import com.tangosol.util.WrapperException;

import java.io.IOException;

import java.util.Arrays;

/**
* ChainStorage is a Storage implementation backed by two other Storage
* implementations.
* In the case of "overriding" chain, the delta is used for first-chance
* reads and all writes.  The base is used when an item is not present in the delta.
* In the case of "modifying" chain, the delta is used to read and store the
* component modifications.
*
* @version 1.00, 04/29/99
* @author  Cameron Purdy
*/
public class ChainStorage
        extends    Base
        implements Storage
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a chained storage object backed by two storage objects.
    *
    * @param base      the base (secondary) storage
    * @param delta     the delta (primary)  storage
    */
    public ChainStorage(Storage base, Storage delta)
        {
        this(base, delta, false);
        }

    /**
    * Create a chained storage object backed by two storage objects.
    *
    * @param base      the base (secondary) storage
    * @param delta     the delta (primary)  storage
    * @param fOverride true if the delta "overrides" the base;
    *                  false if the delta "modifies" the base
    */
    public ChainStorage(Storage base, Storage delta, boolean fOverride)
        {
        if (base == null || delta == null)
            {
            throw new IllegalArgumentException(CLASS +
                    "Storage implementations must be non-null!");
            }

        m_base      = base;
        m_delta     = delta;
        m_fOverride = fOverride;
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
        if (Trait.DEBUG)
            {
            out();
            out("***ChainStorage*** loading: " + sName + " @" + toString());
            }

        Component cdBase  = m_base .loadComponent(sName, true, errlist);
        Component cdDelta = m_delta.loadComponent(sName, true, errlist);
        Component cdResult;

        if (m_fOverride || cdBase == null)
            {
            cdResult = cdDelta == null ? cdBase : cdDelta;
            }
        else
            {
            if (cdDelta == null)
                {
                cdDelta = (Component) cdBase.getNullDerivedTrait(null, MODIFICATION);
                }

            int nModeBase  = cdBase .getMode();
            int nModeDelta = cdDelta.getMode();
            switch (nModeDelta)
                {
                case DERIVATION:
                    if (nModeBase == RESOLVED)
                        {
                        break;
                        }
                    if (nModeBase == DERIVATION)
                        {
                        // both base and delta exist, meaning that
                        // a) the component [derivation] exists in more than one storage.
                        // b) there is a diamond shaped tree of storages
                        //                   S0
                        //                S1    S2
                        //                   S3
                        // and the component originates in S0.
                        //
                        // For Signatures we will ignore the collision; for the Components
                        // we will ignore the collision if the components are equal and
                        // issue a warning otherwise
                        if (cdDelta.isComponent() && !cdDelta.equals(cdBase))
                            {
                            // there is at least one scenario where the components
                            // appear to be unequal because finalizeResolve has not
                            // yet been issued; verify that is _not_ the case
                            Component cdBaseClone;
                            Component cdDeltaClone;
                            try
                                {
                                cdBaseClone  = (Component) cdBase .clone();
                                cdDeltaClone = (Component) cdDelta.clone();
                                }
                            catch (CloneNotSupportedException e)
                                {
                                // never happens for global components
                                throw new WrapperException(e);
                                }

                            cdBaseClone .finalizeResolve(this, null);
                            cdDeltaClone.finalizeResolve(this, null);
                            if (!cdDeltaClone.equals(cdBaseClone))
                                {
                                // TODO: use cdDelta.logError
                                String sMsg = "During resolution it was necessary to discard " +
                                    "the base Derivation information for \"" + sName +
                                    "\" from " + getRightmostStorage(m_base) + " in favor of " +
                                    "Derivation information from " + getRightmostStorage(m_delta);
                                if (errlist == null)
                                    {
                                    out(sMsg);
                                    }
                                else
                                    {
                                    errlist.addWarning(sMsg);
                                    }
                                }
                            }

                        cdBase = null;
                        break;
                        }
                    // fall through
                case RESOLVED:
                    // the only resolved components are "Root" or "java.lang.Object"
                    if (nModeBase == RESOLVED)
                        {
                        cdBase = null;
                        break;
                        }
                    // TODO: cdDelta.logError()
                    String sMsg = "During resolution it was necessary to discard " +
                        "the base Derivation information for \"" + sName +
                        "\" from " + getRightmostStorage(m_base) + " in favor of " +
                        (nModeDelta == DERIVATION ? "Derivation" : "Modification") +
                        "  information from " + getRightmostStorage(m_delta);
                    if (errlist == null)
                        {
                        out(sMsg);
                        }
                    else
                        {
                        errlist.addWarning(sMsg);
                        }
                    cdBase = null;
                }

            if (Trait.DEBUG)
                {
                out();
                out("***ChainStorage*** Component Base before resolve:");
                cdBase.dump();
                out();
                out("***ChainStorage*** Component Delta before resolve:");
                cdDelta.dump();
                }

            cdResult = cdBase == null ? cdDelta : cdBase.resolve(cdDelta, this, errlist);

            if (Trait.DEBUG)
                {
                out();
                out("***ChainStorage*** Component Result after resolve:");
                cdResult.dump();
                }
            }

        if (cdResult != null)
            {
            cdResult.setModifiable(!fReadOnly);
            }

        return cdResult;
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
        if (Trait.DEBUG)
            {
            out();
            out("***ChainStorage*** storing: " + cd + " @" + toString());
            }

        String    sName  = cd.getQualifiedName();
        Component cdBase = m_base.loadComponent(sName, true, errlist);
        Component cdDelta;

        if (m_fOverride || cdBase == null)
            {
            cdDelta = cd;

            if (cdBase != null)
                {
                // Delta storage is on override, meaning that it is
                // a persistent storage and no other extracts will follow
                // However, since persistent storage is responsible
                // for calling finalizeExtrace itself, we will do it
                // on a clone instead of the actual component

                try
                    {
                    cd = (Component) cd.clone();
                    }
                catch (CloneNotSupportedException e) {}

                cd.finalizeExtract(this, errlist);

                if (cd.equals(cdBase))
                    {
                    try
                        {
                        m_delta.removeComponent(sName);
                        return;
                        }
                    catch (ComponentException e)
                        {
                        }
                    }
                }
            }
        else
            {
            if (Trait.DEBUG)
                {
                out();
                out("***ChainStorage*** Component Base before extract:");
                cdBase.dump();
                out();
                out("***ChainStorage*** Component Passed before extract:");
                cd.dump();
                }

            cdDelta = cd.extract(cdBase, this, errlist);
            if (Trait.DEBUG)
                {
                out();
                out("***ChainStorage*** Component Delta after extract:");
                cdDelta.dump();
                }
            }

        m_delta.storeComponent(cdDelta, errlist);
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
        m_delta.removeComponent(sName);
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
        Component cdJCS = m_delta.loadSignature(sName);
        if (cdJCS == null)
            {
            cdJCS = m_base.loadSignature(sName);
            }
        return cdJCS;
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
        String    sName     = cdJCS.getQualifiedName();
        Component cdJCSBase = null;
        try
            {
            cdJCSBase = m_base.loadSignature(sName);
            }
        catch (ComponentException e)
            {
            }

        if (cdJCSBase != null && cdJCSBase.equals(cdJCS))
            {
            // Signatures are persisted exremely rarely (in-house testing only),
            // so there is no reason to complicate the API with "removeSignature".
            // Just make sure that unnecessary duplicate doesn't get created
            try
                {
                if (m_delta.loadSignature(sName) == null)
                    {
                    // Don't create a duplicate
                    return;
                    }
                }
            catch (ComponentException e)
                {
                }
            }

        m_delta.storeSignature(cdJCS);
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
        ClassFile clz = m_delta.loadOriginalClass(sName);
        return    clz == null ? m_base.loadOriginalClass(sName) : clz;
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
        ClassFile clz = m_delta.loadClass(sName);
        return    clz == null ? m_base.loadClass(sName) : clz;
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
        String    sName   = clz.getName();
        ClassFile clzBase = null;
        try
            {
            clzBase = m_base.loadClass(sName);
            }
        catch (ComponentException e)
            {
            }

        if (clzBase != null && clzBase.equals(clz))
            {
            // TODO: remove from the delta storage

            try
                {
                if (m_delta.loadClass(sName) == null)
                    {
                    // Don't create a new duplicate
                    return;
                    }
                }
            catch (ComponentException e)
                {
                }
            }

        m_delta.storeClass(clz, sListing);
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
        String sScript = m_delta.loadJava(sName);
        return sScript == null ? m_base.loadJava(sName) : sScript;
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
        byte[] ab = m_delta.loadOriginalResource(sName);
        return ab == null ? m_base.loadOriginalResource(sName) : ab;
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
        byte[] abBase  = m_base .loadResourceSignature(sName);
        byte[] abDelta = m_delta.loadResourceSignature(sName);
        byte[] abResult;

        if (m_fOverride || abBase == null)
            {
            abResult = abDelta == null ? abBase : abDelta;
            }
        else
            {
            abResult = getResourceHandler(sName).resolve(abBase, abDelta);
            }
        return abResult;
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
        byte[] abBase = m_base.loadResourceSignature(sName);
        byte[] abDelta;

        if (m_fOverride || abBase == null)
            {
            if (Arrays.equals(abData, abBase))
                {
                try
                    {
                    m_delta.removeResourceSignature(sName);
                    return;
                    }
                catch (IOException e)
                    {
                    }
                }
            abDelta = abData;
            }
        else
            {
            abDelta = getResourceHandler(sName).extract(abData, abBase);
            }

        m_delta.storeResourceSignature(sName, abDelta);
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
        m_delta.removeResourceSignature(sName);
        }

    /**
    * Load the generated resource.
    *
    * @param sName  fully qualified resource name
    *
    * @return the specified resource as a byte array
    *
    * @exception IOException  if an unrecoverable error occurs
    *
    */
    public byte[] loadResource(String sName)
            throws IOException
        {
        byte[] ab = m_delta.loadResource(sName);
        return ab == null ? m_base.loadResource(sName) : ab;
        }

    /**
    * Store the specified resource.
    *
    * @param sName  fully qualified resource name
    * @param abData the specified resource as a byte array
    *
    * @exception IOException  if an unrecoverable error occurs
    *
    */
    public void storeResource(String sName, byte[] abData)
            throws IOException
        {
        // We need to store the resource regardless of the base
        // since it's going to be used by an external tool

        /*
        byte[] abBase = null;
        try
            {
            abBase = m_base.loadResource(sName);
            }
        catch (IOException e)
            {
            }

        if (abBase != null && Arrays.equals(abBase, abData))
            {
            // TODO: remove from the delta storage

            try
                {
                if (m_delta.loadResource(sName) == null)
                    {
                    // Don't create a new duplicate
                    return;
                    }
                }
            catch (IOException e)
                {
                }
            }
        */
        m_delta.storeResource(sName, abData);
        }

    // ---- component management --------------------------------------------

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
        StringTable tblBase  = m_base .getSubComponents(sComponent, fQualify);
        StringTable tblDelta = m_delta.getSubComponents(sComponent, fQualify);
        return merge(tblBase, tblDelta);
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
        StringTable tblBase  = m_base .getPackageComponents(sPackage, fQualify);
        StringTable tblDelta = m_delta.getPackageComponents(sPackage, fQualify);
        return merge(tblBase, tblDelta);
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
        if (fSubs)
            {
            fQualify = true;
            }

        StringTable tblBase  = m_base .getComponentPackages(sPackage, fQualify, fSubs);
        StringTable tblDelta = m_delta.getComponentPackages(sPackage, fQualify, fSubs);
        return merge(tblBase, tblDelta);
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
        StringTable tblBase  = m_base .getPackageSignatures(sPackage, fQualify);
        StringTable tblDelta = m_delta.getPackageSignatures(sPackage, fQualify);
        return merge(tblBase, tblDelta);
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
        if (fSubs)
            {
            fQualify = true;
            }

        StringTable tblBase  = m_base .getSignaturePackages(sPackage, fQualify, fSubs);
        StringTable tblDelta = m_delta.getSignaturePackages(sPackage, fQualify, fSubs);
        return merge(tblBase, tblDelta);
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
        StringTable tblBase  = m_base .getPackageResources(sPackage, fQualify);
        StringTable tblDelta = m_delta.getPackageResources(sPackage, fQualify);
        return merge(tblBase, tblDelta);
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
        if (fSubs)
            {
            fQualify = true;
            }

        StringTable tblBase  = m_base .getResourcePackages(sPackage, fQualify, fSubs);
        StringTable tblDelta = m_delta.getResourcePackages(sPackage, fQualify, fSubs);
        return merge(tblBase, tblDelta);
        }

    /**
    * Provide a short human-readable description of the trait.
    *
    * @return a human-readable description of this trait
    */
    public String toString()
        {
        return CLASS +
            '(' + m_base + (m_fOverride ? "," : ":") + m_delta + ')';
        }

    // ----- helpers

    protected StringTable merge(StringTable tblBase, StringTable tblDelta)
        {
        // since the StringTables carry the locator objects, we have to ensure
        // that the delta locators override the base locators

        if (tblDelta.isEmpty())
            {
            return tblBase;
            }
        if (tblBase.isEmpty())
            {
            return tblDelta;
            }
        else if (tblBase.getSize() > tblDelta.getSize())
            {
            tblBase.putAll(tblDelta);
            return tblBase;
            }
        else
            {
            tblDelta.addAll(tblBase);
            return tblDelta;
            }
        }


    /**
    * Return the rightmost terminal storage in a tree of storages
    * startin at the specified storage
    */
    private Storage getRightmostStorage(Storage storage)
        {
        if (storage instanceof ChainStorage)
            {
            ChainStorage chain = (ChainStorage) storage;

            storage = chain.getRightmostStorage(chain.m_delta);
            }
        return storage;
        }

    // ---- resource resolution support ------------------------------------

    public interface ResourceHandler
        {
        /**
        * Resolve the resource using the specified base and delta
        */
        public byte[] resolve(byte[] abBase, byte[] abDelta)
                throws IOException;

        /**
        * Extract the delta for a resource using the resulting resource and the base
        */
        public byte[] extract(byte[] abResult, byte[] abBase)
                throws IOException;
        }

    public class SimpleHandler implements ResourceHandler
        {
        public byte[] resolve(byte[] abBase, byte[] abDelta)
            {
            return abDelta == null ? abBase : abDelta;
            }
        public byte[] extract(byte[] abResult, byte[] abBase)
            {
            return Arrays.equals(abResult, abBase) ? null : abResult;
            }
        }

    /**
    * Return a ResourceHandler for the specified resource name
    */
    protected ResourceHandler getResourceHandler(String sName)
        {
        return new SimpleHandler();
        }

    // ---- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "ChainStorage";

    /**
    * The storage which provides the base component definitions to be modified.
    */
    private Storage m_base;

    /**
    * The storage which provided the delta component definitions.
    */
    private Storage m_delta;

    /**
    * The flag that specifies whether this chain is an "override" or
    * a "supplement" (i.e. "version" versus "customization")
    */
    private boolean m_fOverride;
    }
