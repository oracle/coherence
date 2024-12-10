/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.oracle.coherence.common.collections.AbstractStableIterator;

import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.util.AbstractKeyBasedMap;
import com.tangosol.util.ConverterCollections;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;


/**
* A backing Map implementation that delegates all operations to a CacheStore.
* <p>
* Note: The clear() method is explicitly <b>not</b> implemented. Items can
* be removed individually, but the suggested usage is to indicate item
* deletion by state change, such as by setting a persistent "item-deleted"
* flag. There are two reasons for this:
* <ol>
* <li>The CacheHandler for the backing Map may use the clear() method as part
* of setup and tear-down.</li>
* <li>Deletions are not replicatable to new members, since there is no record
* of the deletions having occurred. To be certain that new members are aware
* of items having been deleted, the "evidence" of the deletion must be
* recorded (i.e. as part of the "deleted" item's state).</li>
* </ol>
*
* @author cp  2006.09.06
*
* @since Coherence 3.2
*/
public class CacheStoreMap
        extends ConverterCollections.ConverterMap
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a read-only CacheStoreMap.
    *
    * @param ctx     the context provided by the CacheService which is using
    *                this backing map
    * @param loader  the CacheLoader to delegate read operations to
    */
    public CacheStoreMap(BackingMapManagerContext ctx, IterableCacheLoader loader)
        {
        this(ctx, CacheLoaderCacheStore.wrapCacheLoader(loader));
        }

    /**
    * Construct a read/write CacheStoreMap.
    *
    * @param ctx    the context provided by the CacheService which is using
    *               this backing map
    * @param store  the CacheStore to delegate read and write operations to
    */
    public CacheStoreMap(BackingMapManagerContext ctx, CacheStore store)
        {
        this(ctx, store, false);
        }

    /**
    * Construct a read/write CacheStoreMap.
    *
    * @param ctx     the context provided by the CacheService which is using
    *                this backing map
    * @param store   the CacheStore to delegate read and write operations to
    * @param fBlind  pass true to optimize put() and remove() by allowing
    *                them to skip the loading of old values
    */
    public CacheStoreMap(BackingMapManagerContext ctx, CacheStore store, boolean fBlind)
        {
        super(new ReadWriteMap(store, fBlind),
                ctx.getKeyToInternalConverter(), ctx.getKeyFromInternalConverter(),
                ctx.getValueToInternalConverter(), ctx.getValueFromInternalConverter());

        if (!(store instanceof IterableCacheLoader))
            {
            throw new IllegalArgumentException(
                    "CacheStore must implement IterableCacheLoader");
            }
        }


    // ----- inner class: Read/Write Map ------------------------------------

    /**
    * A Map implementation that delegates straight through to a CacheStore.
    */
    public static class ReadWriteMap
            extends AbstractKeyBasedMap
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a read/write CacheStoreMap.
        *
        * @param store   the CacheStore to delegate read and write operations
        *                to
        * @param fBlind  pass true to optimize put() and remove() by allowing
        *                them to skip the loading of old values
        */
        public ReadWriteMap(CacheStore store, boolean fBlind)
            {
            m_store  = store;
            m_fBlind = fBlind;
            }


        // ----- AbstractKeyBasedMap methods ----------------------------

        /**
        * Clear is explicitly <b>not</b> implemented.
        */
        public void clear()
            {
            }

        /**
        * {@inheritDoc}
        */
        public boolean containsKey(Object oKey)
            {
            return m_store.load(oKey) != null;
            }

        /**
        * {@inheritDoc}
        */
        public Object get(Object oKey)
            {
            return m_store.load(oKey);
            }

        /**
        * {@inheritDoc}
        */
        public Map getAll(Collection colKeys)
            {
            return m_store.loadAll(colKeys);
            }

        /**
        * {@inheritDoc}
        */
        protected Iterator iterateKeys()
            {
            return new AbstractStableIterator()
                {
                protected void advance()
                    {
                    Iterator iter = m_iter;
                    if (iter.hasNext())
                        {
                        setNext(iter.next());
                        }
                    }

                protected void remove(Object oPrev)
                    {
                    ReadWriteMap.this.remove(oPrev);
                    }

                Iterator m_iter = ((IterableCacheLoader) m_store).keys();
                };
            }

        /**
        * {@inheritDoc}
        */
        public Object put(Object oKey, Object oValue)
            {
            CacheStore store = m_store;
            Object     oOrig = isBlindPutAllowed() ? null : store.load(oKey);
            try
                {
                if (!m_fStoreUnsupported)
                    {
                    m_store.store(oKey, oValue);
                    }
                }
            catch (UnsupportedOperationException e)
                {
                m_fStoreUnsupported = true;
                }
            return oOrig;
            }

        /**
        * {@inheritDoc}
        */
        public void putAll(Map map)
            {
            try
                {
                m_store.storeAll(map);
                }
            catch (UnsupportedOperationException e)
                {
                super.putAll(map);
                }
            }

        /**
        * {@inheritDoc}
        */
        public Object remove(Object oKey)
            {
            CacheStore store  = m_store;
            boolean    fBlind = isBlindRemoveAllowed();
            Object     oOrig  = fBlind ? null : store.load(oKey);
            if (fBlind || oOrig != null)
                {
                try
                    {
                    if (!m_fEraseUnsupported)
                        {
                        m_store.erase(oKey);
                        }
                    }
                catch (UnsupportedOperationException e)
                    {
                    m_fEraseUnsupported = true;
                    }
                }
            return oOrig;
            }

        /**
        * {@inheritDoc}
        */
        protected boolean removeBlind(Object oKey)
            {
            CacheStore store   = m_store;
            boolean    fExists = store.load(oKey) != null;
            if (fExists)
                {
                try
                    {
                    if (!m_fEraseUnsupported)
                        {
                        m_store.erase(oKey);
                        }
                    }
                catch (UnsupportedOperationException e)
                    {
                    m_fEraseUnsupported = true;
                    }
                }
            return fExists;
            }


        // ----- internal operations ------------------------------------

        /**
        * Determine if the "blind put" optimization is possible.
        *
        * @return true if put() can return null, regardless of the presence
        *         of an old value
        */
        protected boolean isBlindPutAllowed()
            {
            return m_fBlind;
            }

        /**
        * Determine if the "blind remove" optimization is possible.
        *
        * @return true if remove() can return null, regardless of the
        *         presence of an old value
        */
        protected boolean isBlindRemoveAllowed()
            {
            return m_fBlind;
            }

        /**
        * Determine if the CacheStore has been determined to be read-only for
        * store() operations.
        *
        * @return true if any store() operations have failed due to UOE
        */
        protected boolean isStoreUnsupported()
            {
            return m_fStoreUnsupported;
            }

        /**
        * Determine if the CacheStore has been determined to be read-only for
        * erase() operations.
        *
        * @return true if any store() operations have failed due to UOE
        */
        protected boolean isEraseUnsupported()
            {
            return m_fEraseUnsupported;
            }


        // ----- data members -------------------------------------------

        /**
        * The CacheStore to delegate all Map operations to.
        */
        protected CacheStore m_store;

        /**
        * True means that put() and remove() can return null, regardless of
        * the presence of an old value.
        */
        protected boolean m_fBlind;

        /**
        * True means that a call to the CacheStore store() method is assumed
        * to be unsupported.
        */
        protected boolean m_fStoreUnsupported;

        /**
        * True means that a call to the CacheStore erase() method is assumed
        * to be unsupported.
        */
        protected boolean m_fEraseUnsupported;
        }
    }
