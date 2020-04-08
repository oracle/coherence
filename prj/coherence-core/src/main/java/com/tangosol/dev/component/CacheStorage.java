/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import com.tangosol.net.cache.LocalCache;

import com.tangosol.util.ErrorList;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Iterator;


/**
* CacheStorage is a caching layer built on top of the resolve/extract logic.
*
* @version 1.00, 08/28/98
* @author  Cameron Purdy
*/
public class CacheStorage
        extends Extractor
    {
    // ----- constructors ---------------------------------------------------

    public CacheStorage(Storage storage)
        {
        super(storage);
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
    public synchronized Component loadComponent(String sName, boolean fReadOnly, ErrorList errlist)
            throws ComponentException
        {
        Component cd;

        // force the re-loading of the component if we are
        // going to be editing it
        if (fReadOnly && m_tblComponent.containsKey(sName))
            {
            cd = (Component) m_tblComponent.get(sName);
            }
        else
            {
            // load from the actual storage
            cd = super.loadComponent(sName, true, errlist);

            // store in the cache (even if it is null)
            m_tblComponent.put(sName, cd);

            /*
            if (sName.length() > 0 && !Component.isQualifiedNameLegal(sName))
                {
                m_tblSignature.put(sName, cd);
                }
            */
            }

        if (cd != null && !fReadOnly)
            {
            try
                {
                cd = (Component) cd.clone();
                cd.setModifiable(true);
                }
            catch (CloneNotSupportedException e)
                {
                throw new ComponentException(e.toString());
                }
            }

        return cd;
        }

    /**
    * Store the specified Component.
    *
    * @param cd       the Component Definition
    * @param errlist  the ErrorList object to log any derivation/modification
    *                 errors to
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public synchronized void storeComponent(Component cd, ErrorList errlist)
            throws ComponentException
        {
        super.storeComponent(cd, errlist);

        // we will no put the component in the cache right away
        // to force the "resolve" next time this component is
        // loaded since the result may differ...
        String sName = cd.getQualifiedName();

        flushComponentCache(sName);
        if (cd.isSignature())
            {
            flushSignatureCache(sName, cd.isInterface());
            }
        }

    /**
    * Remove the specified Component.
    *
    * @param sName fully qualified Component Definition name
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public synchronized void removeComponent(String sName)
            throws ComponentException
        {
        super.removeComponent(sName);

        flushComponentCache(sName);

        if (!Component.isQualifiedNameLegal(sName))
            {
            // assume the interface to do an extra check
            flushSignatureCache(sName, true);
            }
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
    public synchronized Component loadSignature(String sName)
            throws ComponentException
        {
        // check cache
        Component cdJCS = null;
        if (m_tblSignature.containsKey(sName))
            {
            cdJCS = (Component) m_tblSignature.get(sName);
            }
        else
            {
            cdJCS = super.loadSignature(sName);

            // store it in the cache (even if it is null)
            m_tblSignature.put(sName, cdJCS);
            }

        if (cdJCS != null)
            {
            cdJCS.setModifiable(false);
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
    public synchronized void storeSignature(Component cdJCS)
            throws ComponentException
        {
        super.storeSignature(cdJCS);

        flushSignatureCache(cdJCS.getName(), cdJCS.isInterface());
        }


    // ----- Cache support --------------------------------------------------

    /**
    * Determine which cached entries are impacted by the updated jcs
    * and remove them from the cache.
    *
    * We have to walk each cached entry's class hierarchy to determine
    * if the cached class has as it's super the updated class.  Since
    * we will need impacted entires still in the cache to determine if
    * any of that entries sub-classes are impacted, we will collect a
    * list of cache entries to be removed at the end of checking all
    * cached entries.
    *
    * @param sName fully qualified component name
    */
    private void flushComponentCache(String sName)
        {
        // remove all the impacted components from the cache
        for (Iterator iter = m_tblComponent.keySet().iterator(); iter.hasNext(); )
            {
            String    sCached  = (String) iter.next();
            Component cdCached = (Component) m_tblComponent.get(sCached);

            if (cdCached != null && cdCached.isImpactedBy(sName))
                {
                iter.remove();
                }
            }

        m_tblComponent.remove(sName);
        }


    /**
    * Determine which cached entries are impacted by the updated jcs
    * and remove them from the cache.
    *
    * We have to walk each cached entry's class hierarchy to determine
    * if the cached class has as it's super the updated class.  Since
    * we will need impacted entires still in the cache to determine if
    * any of that entries sub-classes are impacted, we will collect a
    * list of cache entries to be removed at the end of checking all
    * cached entries.
    *
    * @param sName fully qualified JCS name
    * @param fInterface if true then extra processing is done for checking
    *                   the impacted interfaces
    */
    private void flushSignatureCache(String sName, boolean fInterface)
            throws ComponentException
        {
        HashMap mapImpacted = new HashMap();

        // Create a current list of cached entry's key values.  We might
        // end up adding to the cache while resolving supers, so we create
        // an independant list.
        LinkedList listCached = new LinkedList(m_tblSignature.keySet());
        for (Iterator iter = listCached.iterator(); iter.hasNext(); )
            {
            collectImpactedJavaClassSignatures(
                sName, fInterface, (String) iter.next(), mapImpacted);
            }

        // remove all of the impacted java class signatures
        for (Iterator iter = mapImpacted.keySet().iterator(); iter.hasNext();)
            {
            String sImpacted = (String) iter.next();
            m_tblSignature.remove(sImpacted);
            }

        m_tblSignature.remove(sName);
        }

    private boolean collectImpactedJavaClassSignatures(String sModified,
                    boolean fInterface, String sJcs, HashMap mapImpacted)
            throws ComponentException
        {
        // Check if this JCS is the modified one
        if (sJcs.equals(sModified))
            {
            return true;
            }

        // Unfortunately, we *MUST* force the JCS to be reloaded if it has
        // been GC'ed, otherwise it could get reloaded later on while
        // resolving supers and in fact this entry should have been marked
        // as impacted.
        Component jcs = loadSignature(sJcs);
        // check if this is a cache entry that doesn't exist anymore
        if (jcs == null)
            {
            return true;
            }

        // Get the name of this jcs's super
        String sSuper = jcs.getSuperName();
        // Check if it has a super
        if (sSuper.length() != 0)
            {
            // Check if the super is impacted
            if (collectImpactedJavaClassSignatures(sModified, fInterface, sSuper, mapImpacted))
                {
                // Our super was impacted, therefore we are impacted
                mapImpacted.put(sJcs, null);
                return true;
                }
            }

        // Check if any of the implemented interfaces were impacted
        if (fInterface)
            {
            String[] asImplements = jcs.getImplements();
            for (int i = 0; i < asImplements.length; ++i)
                {
                // Check if this implemented interface was impacted
                if (collectImpactedJavaClassSignatures(sModified, fInterface, asImplements[i], mapImpacted))
                    {
                    // Our interface was impacted, therefore we are impacted
                    mapImpacted.put(sJcs, null);
                    return true;
                    }
                }
            }

        // Looks like we are *not* impacted by the change.
        return false;
        }


    // ----- Classes --------------------------------------------------------
    // No need to cache these for now.

    // ----- Java -----------------------------------------------------------
    // No need to cache these for now.

    // ----- Resources ------------------------------------------------------
    // No need to cache these for now.


    // ----- miscellaneous --------------------------------------------------

    /**
    * Flush the cache.
    */
    public void flush()
        {
        m_tblComponent.clear();
        m_tblSignature.clear();
        }

    /**
    * Provide a short human-readable description of the trait.
    *
    * @return a human-readable description of this trait
    */
    public String toString()
        {
        String sExtractor = super.toString();
        return CLASS + sExtractor.substring(sExtractor.indexOf('('));
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "CacheStorage";

    /**
    * Cache:  Component structures.
    */
    private Map m_tblComponent = new LocalCache(128, 0);
    /**
    * Cache:  Java Class Signature structures.
    */
    private Map m_tblSignature = new LocalCache(512, 0);
    }
