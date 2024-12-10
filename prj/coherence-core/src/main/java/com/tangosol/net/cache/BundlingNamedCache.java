/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.net.NamedCache;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
* Bundling NamedCache implementation.
*
* @see AbstractBundler
* @author gg 2007.02.13
* @since Coherence 3.3
*/
public class BundlingNamedCache
        extends WrapperNamedCache
    {
    /**
    * Construct a BundlingNamedCache based on the specified NamedCache.
    *
    * @param cache  the NamedCache that will be wrapped by this BundlingNamedCache
    */
    public BundlingNamedCache(NamedCache cache)
        {
        super(cache, null, null);
        }


    // ----- initiators ------------------------------------------------------

    /**
    * Configure the bundler for the "get" operations. If the bundler does not
    * exist and bundling is enabled, it will be instantiated.
    *
    * @param cBundleThreshold  the bundle size threshold; pass zero to disable
    *                          "get" operation bundling
    *
    * @return the "get" bundler or null if bundling is disabled
    */
    public synchronized AbstractBundler ensureGetBundler(int cBundleThreshold)
        {
        if (cBundleThreshold > 0)
            {
            GetBundler bundler = m_getBundler;
            if (bundler == null)
                {
                m_getBundler = bundler = new GetBundler();
                }
            bundler.setSizeThreshold(cBundleThreshold);
            return bundler;
            }
        else
            {
            return m_getBundler = null;
            }
        }

    /**
    * Configure the bundler for the "put" operations. If the bundler does not
    * exist and bundling is enabled, it will be instantiated.
    *
    * @param cBundleThreshold  the bundle size threshold; pass zero to disable
    *                          "put" operation bundling
    *
    * @return the "put" bundler or null if bundling is disabled
    */
    public synchronized AbstractBundler ensurePutBundler(int cBundleThreshold)
        {
        if (cBundleThreshold > 0)
            {
            PutBundler bundler = m_putBundler;
            if (bundler == null)
                {
                m_putBundler = bundler = new PutBundler();
                }
            bundler.setSizeThreshold(cBundleThreshold);
            return bundler;
            }
        else
            {
            return m_putBundler = null;
            }
        }

    /**
    * Configure the bundler for the "remove" operations. If the bundler does not
    * exist and bundling is enabled, it will be instantiated.
    *
    * @param cBundleThreshold  the bundle size threshold; pass zero to disable
    *                          "remove" operation bundling
    *
    * @return the "remove" bundler or null if bundling is disabled
    */
    public synchronized AbstractBundler ensureRemoveBundler(int cBundleThreshold)
        {
        if (cBundleThreshold > 0)
            {
            RemoveBundler bundler = m_removeBundler;
            if (bundler == null)
                {
                m_removeBundler = bundler = new RemoveBundler();
                }
            bundler.setSizeThreshold(cBundleThreshold);
            return bundler;
            }
        else
            {
            return m_removeBundler = null;
            }
        }


    // ----- accessors -------------------------------------------------------

    /**
    * Obtain the bundler for the "get" operations.
    *
    * @return the "get" bundler
    */
    public AbstractBundler getGetBundler()
        {
        return m_getBundler;
        }

    /**
    * Obtain the bundler for the "put" operations.
    *
    * @return the "put" bundler
    */
    public AbstractBundler getPutBundler()
        {
        return m_putBundler;
        }

    /**
    * Obtain the bundler for the "remove" operations.
    *
    * @return the "remove" bundler
    */
    public AbstractBundler getRemoveBundler()
        {
        return m_removeBundler;
        }

    // ----- various bundleable NamedCache methods ---------------------------

    /**
    * {@inheritDoc}
    */
    public Object get(Object oKey)
        {
        GetBundler bundler = m_getBundler;
        return bundler == null ?
                super.get(oKey) : bundler.process(oKey);
        }

    /**
    * {@inheritDoc}
    */
    public Map getAll(Collection colKeys)
        {
        GetBundler bundler = m_getBundler;
        return bundler == null ?
                super.getAll(colKeys) : bundler.processAll(colKeys);
        }

    /**
    * {@inheritDoc}
    * <p>
    * <b>Note:</b> this method always returns null.
    */
    public Object put(Object oKey, Object oValue)
        {
        PutBundler bundler = m_putBundler;
        if (bundler == null)
            {
            super.putAll(Collections.singletonMap(oKey, oValue));
            }
        else
            {
            bundler.process(oKey, oValue);
            }
        return null;
        }

    /**
    * {@inheritDoc}
    */
    public void putAll(Map map)
        {
        PutBundler bundler = m_putBundler;
        if (bundler == null)
            {
            super.putAll(map);
            }
        else
            {
            bundler.processAll(map);
            }
        }

    /**
    * {@inheritDoc}
    * <p>
    * <b>Note:</b> this method always returns null.
    */
    public Object remove(Object oKey)
        {
        RemoveBundler bundler = m_removeBundler;
        if (bundler == null)
            {
            super.remove(oKey);
            }
        else
            {
            bundler.process(oKey);
            }
        return null;
        }


    // ----- inner classes ---------------------------------------------------

    protected class GetBundler
            extends AbstractKeyBundler
        {
        /**
        * A pass through the underlying getAll operation.
        */
        protected Map bundle(Collection colKeys)
            {
            return BundlingNamedCache.super.getAll(colKeys);
            }

        /**
        * A pass through the underlying get operation.
        */
        protected Object unbundle(Object oKey)
            {
            return BundlingNamedCache.super.get(oKey);
            }
        }

    protected class PutBundler
            extends AbstractEntryBundler
        {
        /**
        * A pass through the underlying putAll() operation.
        */
        protected void bundle(Map map)
            {
            BundlingNamedCache.super.putAll(map);
            }
        }

    protected class RemoveBundler
            extends AbstractKeyBundler
        {
        /**
        * A pass through the underlying keySet().removeAll() operation.
        */
        protected Map bundle(Collection colKeys)
            {
            BundlingNamedCache.this.keySet().removeAll(colKeys);
            return null;
            }

        /**
        * A pass through the underlying remove() operation.
        */
        protected Object unbundle(Object oKey)
            {
            BundlingNamedCache.this.keySet().remove(oKey);
            return null;
            }
        }

    // ----- data fields -----------------------------------------------------

    /**
    * The bundler for get() operations.
    */
    private GetBundler m_getBundler;

    /**
    * The bundler for put() operations.
    */
    private PutBundler m_putBundler;

    /**
    * The bundler for remove() operations.
    */
    private RemoveBundler m_removeBundler;
    }