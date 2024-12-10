/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.io.BinaryStore;

import com.tangosol.util.AbstractKeyBasedMap;
import com.tangosol.util.AbstractKeySetBasedMap;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.LiteSet;
import com.tangosol.util.LongArray;
import com.tangosol.util.MapEvent;
import com.tangosol.util.SparseArray;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
* A version of SerializationMap that implements an LRU policy.
* <p>
* This implementation is partially thread safe. It assumes that multiple
* threads will not be accessing the same keys at the same time, nor would
* any other thread be accessing this cache while a clear() operation were
* going on, for example. In other words, this implementation assumes that
* access to this cache is either single-threaded or gated through an object
* like WrapperConcurrentMap.
* <p>
* The primary reason why SerializationCache is a sub-class of
* SerializationMap instead of combining their functionality is that
* SerializationMap represents a passive store, and thus does not implement
* the ObservableMap interface, while SerializationCache represents an
* active store, and thus clients of it would have to always handle potential
* events, even if it were not size-limited.
*
* @since Coherence 2.2
* @author cp  2003.05.28
*/
public class SerializationCache
        extends AbstractSerializationCache
        implements CacheMap, ConfigurableCacheMap
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a SerializationCache on top of a BinaryStore.
    *
    * @param store  the BinaryStore to use to write the serialized objects to
    * @param cMax   the maximum number of items to store in the binary store
    */
    public SerializationCache(BinaryStore store, int cMax)
        {
        this(store, cMax, null);
        }

    /**
    * Construct a SerializationCache on top of a BinaryStore.
    *
    * @param store  the BinaryStore to use to write the serialized objects to
    * @param cMax   the maximum number of items to store in the binary store
    * @param loader the ClassLoader to use for deserialization
    */
    public SerializationCache(BinaryStore store, int cMax, ClassLoader loader)
        {
        super(store, loader);
        setHighUnits(cMax);
        }

    /**
    * Construct a SerializationCache on top of a BinaryStore.
    *
    * @param store       the BinaryStore to use to write the serialized objects to
    * @param cMax        the maximum number of items to store in the binary store
    * @param fBinaryMap  true indicates that this map will only manage
    *                    binary keys and values
    * @since Coherence 2.4
    */
    public SerializationCache(BinaryStore store, int cMax, boolean fBinaryMap)
        {
        super(store, fBinaryMap);
        setHighUnits(cMax);
        }


    // ----- Map interface --------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public synchronized void clear()
        {
        try
            {
            super.clear();
            }
        finally
            {
            getLruArray().clear();
            getExpiryArray().clear();
            m_cCurUnits = 0L;
            }
        }

    /**
    * {@inheritDoc}
    */
    public boolean containsKey(Object oKey)
        {
        checkExpiry();

        boolean fContains = super.containsKey(oKey);
        if (fContains)
            {
            touch(oKey);
            }
        return fContains;
        }

    /**
    * {@inheritDoc}
    */
    public boolean containsValue(Object oValue)
        {
        checkExpiry();

        return super.containsValue(oValue);
        }

    /**
    * {@inheritDoc}
    */
    public Object get(Object oKey)
        {
        checkExpiry();

        touch(oKey);
        return super.get(oKey);
        }

    /**
    * {@inheritDoc}
    */
    public boolean isEmpty()
        {
        checkExpiry();

        return super.isEmpty();
        }

    /**
    * {@inheritDoc}
    */
    public Object put(Object oKey, Object oValue)
        {
        checkExpiry();

        Object oOrig = super.put(oKey, oValue);

        checkSize();
        return oOrig;
        }

    /**
    * {@inheritDoc}
    */
    public void putAll(Map map)
        {
        checkExpiry();

        super.putAll(map);
        checkSize();
        }

    /**
    * {@inheritDoc}
    */
    public Object remove(Object oKey)
        {
        checkExpiry();

        return super.remove(oKey);
        }

    /**
    * {@inheritDoc}
    */
    public int size()
        {
        checkExpiry();

        return super.size();
        }


    // ----- AbstractKeySetBasedMap methods ---------------------------------

    /**
    * {@inheritDoc}
    */
    protected boolean removeBlind(Object oKey)
        {
        checkExpiry();

        return super.removeBlind(oKey);
        }


    // ----- CacheMap interface ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        if (cMillis > Integer.MAX_VALUE)
            {
            throw new IllegalArgumentException("TTL value (" + cMillis
                    + ") is out of range (0.." + Integer.MAX_VALUE + ")");
            }

        Object oOrig = put(oKey, oValue);

        // if necessary change the expiry for the entry
        if (cMillis != EXPIRY_DEFAULT && cMillis != getExpiryDelay())
            {
            registerKey(oKey, null, null, (int) cMillis);
            }

        return oOrig;
        }


    // ----- ConfigurableCacheMap interface ---------------------------------

    /**
    * {@inheritDoc}
    */
    public int getUnits()
        {
        if (m_calculator == null)
            {
            return super.size();
            }

        return OldCache.toExternalUnits(m_cCurUnits, getUnitFactor());
        }

    /**
    * {@inheritDoc}
    */
    public int getHighUnits()
        {
        return OldCache.toExternalUnits(m_cMaxUnits, getUnitFactor());
        }

    /**
    * {@inheritDoc}
    */
    public void setHighUnits(int cMax)
        {
        long cUnits = OldCache.toInternalUnits(cMax, getUnitFactor());

        m_cMaxUnits   = cUnits;
        m_cPruneUnits = cUnits == Long.MAX_VALUE ? cUnits : (long) (OldCache.DEFAULT_PRUNE * cUnits);

        checkSize();
        }

    /**
    * {@inheritDoc}
    */
    public int getLowUnits()
        {
        return OldCache.toExternalUnits(m_cPruneUnits, getUnitFactor());
        }

    /**
    * {@inheritDoc}
    */
    public void setLowUnits(int cUnits)
        {
        m_cPruneUnits = OldCache.toInternalUnits(cUnits, getUnitFactor());
        }

    /**
    * {@inheritDoc}
    */
    public int getUnitFactor()
        {
        return m_nUnitFactor;
        }

    /**
    * {@inheritDoc}
    */
    public void setUnitFactor(int nFactor)
        {
        if (nFactor == m_nUnitFactor)
            {
            return;
            }

        if (nFactor < 1)
            {
            throw new IllegalArgumentException("unit factor must be >= 1");
            }

        if (m_cCurUnits > 0)
            {
            throw new IllegalStateException(
                    "unit factor cannot be set after the cache has been populated");
            }

        // only adjust the max units if there was no unit factor set previously
        if (m_nUnitFactor == 1 && m_cMaxUnits != Long.MAX_VALUE)
            {
            m_cMaxUnits *= nFactor;
            }

        m_nUnitFactor = nFactor;
        }

    /**
    * {@inheritDoc}
    */
    public EvictionPolicy getEvictionPolicy()
        {
        return InternalEvictionPolicy.INSTANCE;
        }

    /**
    * {@inheritDoc}
    */
    public void setEvictionPolicy(EvictionPolicy policy)
        {
        if (policy != null && policy != InternalEvictionPolicy.INSTANCE)
            {
            throw new IllegalArgumentException("unsupported eviction policy");
            }
        }

    /**
    * {@inheritDoc}
    */
    public void evict(Object oKey)
        {
        ConfigurableCacheMap.EvictionApprover apprvr = m_apprvrEvict;
        if ((apprvr == null || apprvr.isEvictable(getCacheEntry(oKey))) &&
            getKeyMap().containsKey(oKey))
            {
            if (hasListeners())
                {
                dispatchPendingEvent(oKey, MapEvent.ENTRY_DELETED, null, true);
                }
            getBinaryStore().erase(toBinary(oKey));
            unregisterKey(oKey);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void evictAll(Collection colKeys)
        {
        for (Iterator iter = colKeys.iterator(); iter.hasNext(); )
            {
            Object oKey = iter.next();
            evict(oKey);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void evict()
        {
        checkExpiry();
        checkSize();
        }

    /**
    * {@inheritDoc}
    */
    public int getExpiryDelay()
        {
        return m_cDefaultTTLMillis;
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void setExpiryDelay(int cMillis)
        {
        if (cMillis != m_cDefaultTTLMillis)
            {
            if (cMillis < 0)
                {
                cMillis = 0;
                }

            m_cDefaultTTLMillis = cMillis;
            }
        }

    /**
     * {@inheritDoc}
     */
    public long getNextExpiryTime()
        {
        LongArray arrayExpiry = getExpiryArray();
        return arrayExpiry.isEmpty() ? 0 : arrayExpiry.getFirstIndex();
        }

    /**
    * {@inheritDoc}
    */
    public EvictionApprover getEvictionApprover()
        {
        return m_apprvrEvict;
        }

    /**
    * {@inheritDoc}
    */
    public void setEvictionApprover(EvictionApprover approver)
        {
        m_apprvrEvict = approver;
        }

    /**
    * {@inheritDoc}
    */
    public ConfigurableCacheMap.Entry getCacheEntry(Object oKey)
        {
        return super.containsKey(oKey)
            ? (ConfigurableCacheMap.Entry) ((EntrySet) entrySet()).instantiateEntry(oKey, null)
            : null;
        }

    /**
    * {@inheritDoc}
    */
    public UnitCalculator getUnitCalculator()
        {
        UnitCalculator calculator = m_calculator;
        return calculator == null ? OldCache.InternalUnitCalculator.INSTANCE : calculator;
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void setUnitCalculator(UnitCalculator calculator)
        {
        if (calculator == OldCache.InternalUnitCalculator.INSTANCE)
            {
            calculator = null;
            }

        if (calculator != m_calculator)
            {
            // recalculate the current number of units
            if (calculator == null)
                {
                // use the "fixed" calculator, i.e. each entry is 1 unit
                m_cCurUnits = size();
                }
            else
                {
                // get a stable view of the current set of keys
                Map      mapAttr = getKeyMap();
                Object[] aoKey   = mapAttr.keySet().toArray();

                // recalc all of the per-entry unit figures and the total
                int cTotal = 0;
                BinaryStore store = getBinaryStore();
                for (int i = 0, c = aoKey.length; i < c; ++i)
                    {
                    Object          oKey     = aoKey[i];
                    EntryAttributes attr     = (EntryAttributes) mapAttr.get(oKey);
                    Binary          binKey   = toBinary(oKey);
                    Binary          binValue = store.load(binKey);
                    int             cUnits   = calculator.calculateUnits(binKey, binValue);

                    // update the entry
                    mapAttr.put(oKey, instantiateEntryAttributes(attr,
                            attr.getExpiryTime(), attr.getTouchCount(), cUnits));

                    // update the total
                    cTotal += cUnits;
                    }

                m_cCurUnits = cTotal;
                }

            m_calculator = calculator;
            }
        }


    // ----- SerializationCache methods -------------------------------------

    /**
    * Flush items that have expired.
    *
    * @deprecated use {@link #evict()}
    */
    public void flush()
        {
        checkExpiry();
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "SerializationCache {" + getDescription() + "}";
        }


    // ----- accessors ------------------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected synchronized void setBinaryStore(BinaryStore store)
        {
        BinaryStore storeOrig = getBinaryStore();
        if (store != storeOrig)
            {
            getLruArray().clear();
            getExpiryArray().clear();
            m_cCurUnits = 0L;

            super.setBinaryStore(store);
            }
        }

    /**
    * Determine the next value from the touch counter.
    *
    * @return the next value from the touch counter
    */
    protected long getTouchCounter()
        {
        long c = m_cTouches + 1;
        m_cTouches = c;
        return c;
        }

    /**
    * Get the LRU data structure. The key is a touch count, the value is the
    * Object key for the cache.
    *
    * @return the LRU data structure
    */
    protected LongArray getLruArray()
        {
        LongArray array = m_arrayLRU;
        if (array == null)
            {
            m_arrayLRU = array = new SparseArray();
            }
        return array;
        }

    /**
    * Get the Expiry data structure. The key is a date/time value, the value
    * is a set of Object keys for the cache that expire at that time.
    *
    * @return the Expiry data structure
    */
    protected LongArray getExpiryArray()
        {
        LongArray array = m_arrayExpiry;
        if (array == null)
            {
            m_arrayExpiry = array = new SparseArray();
            }
        return array;
        }

    /**
    * {@inheritDoc}
    */
    protected String getDescription()
        {
        return super.getDescription()
               + ", Units="          + getUnits()
               + ", HighUnits="      + getHighUnits()
               + ", UnitFactor="     + getUnitFactor()
               + ", EvictionPolicy=" + getEvictionPolicy().getName()
               + ", UnitCalculator=" + getUnitCalculator().getName()
               + ", ExpiryDelay="    + getExpiryDelay();
        }


    // ----- internal helpers -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected void registerKey(Object oKey, Binary binKey, Binary binValue)
        {
        registerKey(oKey, binKey, binValue, getExpiryDelay());
        }

    /**
    * Register a new key for the SerializationMap. This method maintains the
    * internal key Set for the SerializationMap.
    *
    * @param oKey      the key that has been added to the map
    * @param binKey    the binary form of the key (null if only TTL change)
    * @param binValue  the binary form of the value (null if only TTL change)
    * @param cMillis   the TTL value (in milliseconds) for the entry
    */
    protected synchronized void registerKey(Object oKey, Binary binKey, Binary binValue, int cMillis)
        {
        LongArray arrayLRU    = getLruArray();
        LongArray arrayExpiry = getExpiryArray();
        Map       mapKeys     = getKeyMap();

        // calculate units of storage for the new value
        int cOldUnits = -1;
        int cNewUnits = -1;
        if (binKey != null)
            {
            // a non-null binKey indicates the caller wants the units to be
            // adjusted, therefore load the value if not provided
            binValue = binValue == null ? getBinaryStore().load(binKey) : binValue;
            if (binValue != null)
                {
                UnitCalculator calculator = m_calculator;
                cNewUnits = calculator == null ? 1 : calculator.calculateUnits(binKey, binValue);
                }
            }

        // remove any previous LRU and expiry information
        EntryAttributes attr = (EntryAttributes) mapKeys.get(oKey);
        if (attr != null)
            {
            // remove old LRU touch count
            arrayLRU.remove(attr.getTouchCount());

            // remove old expiry
            long ldtExpires = attr.getExpiryTime();
            if (ldtExpires != 0L)
                {
                Set setKeys = (Set) arrayExpiry.get(ldtExpires);
                if (setKeys != null)
                    {
                    setKeys.remove(oKey);
                    if (setKeys.isEmpty())
                        {
                        arrayExpiry.remove(ldtExpires);
                        }
                    }
                }

            cOldUnits = attr.getUnits();
            if (cNewUnits < 0)
                {
                cNewUnits = cOldUnits;
                }
            }

        // register LRU touch count
        long nTouch = getTouchCounter();
        arrayLRU.set(nTouch, oKey);

        // register expiry
        long ldtExpires = 0L;
        if (cMillis > 0)
            {
            // resolution is 1/4 second (to more efficiently lump keys into
            // sets)
            ldtExpires = (getSafeTimeMillis() + cMillis) & ~0xFFL;

            Set setKeys = (Set) arrayExpiry.get(ldtExpires);
            if (setKeys == null)
                {
                setKeys = new LiteSet();
                arrayExpiry.set(ldtExpires, setKeys);
                }
            setKeys.add(oKey);
            }

        // register units
        if (cNewUnits != cOldUnits)
            {
            m_cCurUnits += Math.max(0, cNewUnits) - Math.max(0, cOldUnits);
            }

        // register the key
        attr = instantiateEntryAttributes(attr, ldtExpires, nTouch, cNewUnits);
        mapKeys.put(oKey, attr);
        }

    /**
    * Touch an object to pop it to the top of the LRU
    *
    * @param oKey  the key of the object to touch
    */
    protected void touch(Object oKey)
        {
        LongArray arrayLRU = getLruArray();
        Map       mapKeys  = getKeyMap();
        synchronized (this)
            {
            if (mapKeys.containsKey(oKey))
                {
                EntryAttributes attr = (EntryAttributes) mapKeys.get(oKey);
                if (attr != null)
                    {
                    // remove old LRU touch count
                    arrayLRU.remove(attr.getTouchCount());
                    }

                // register LRU touch count
                long nTouch = getTouchCounter();
                arrayLRU.set(nTouch, oKey);

                attr = instantiateEntryAttributes(attr, attr.getExpiryTime(),
                        nTouch, attr.getUnits());
                mapKeys.put(oKey, attr);
                }
            }
        }

    /**
    * Unregister a key from the SerializationMap. This method maintains the
    * internal key Set for the SerializationMap.
    *
    * @param oKey  the key that has been removed from the map
    */
    protected void unregisterKey(Object oKey)
        {
        LongArray arrayLRU    = getLruArray();
        LongArray arrayExpiry = getExpiryArray();
        Map       mapKeys     = getKeyMap();
        synchronized (this)
            {
            EntryAttributes attr = (EntryAttributes) mapKeys.remove(oKey);
            if (attr != null)
                {
                // remove old LRU info
                arrayLRU.remove(attr.getTouchCount());

                // remove old expiry info
                long ldtPrevExpires = attr.getExpiryTime();
                if (ldtPrevExpires != 0L)
                    {
                    Set setKeys = (Set) arrayExpiry.get(ldtPrevExpires);
                    if (setKeys != null)
                        {
                        setKeys.remove(oKey);
                        if (setKeys.isEmpty())
                            {
                            arrayExpiry.remove(ldtPrevExpires);
                            }
                        }
                    }

                // adjust units
                m_cCurUnits -= attr.getUnits();
                }
            }
        }

    /**
    * Make sure the size of the cache isn't too big.
    */
    protected void checkSize()
        {
        long cMax = m_cMaxUnits;
        if (cMax > 0 && cMax < m_cCurUnits && m_apprvrEvict != EvictionApprover.DISAPPROVER)
            {
            synchronized (this)
                {
                LongArray arrayLRU = getLruArray();
                while (m_cCurUnits > getLowUnits())
                    {
                    Object oKey = arrayLRU.get(arrayLRU.getFirstIndex());
                    if (oKey != null)
                        {
                        evict(oKey);
                        }
                    }
                }
            }
        }

    /**
    * Make sure that the cache does not contain expired items.
    */
    protected void checkExpiry()
        {
        // most of the time, expiry probably isn't even used
        LongArray arrayExpiry = getExpiryArray();
        if (arrayExpiry.isEmpty())
            {
            return;
            }

        // check if the current time indicates that some entries have expired
        long ldtCurrent = getSafeTimeMillis();
        if (ldtCurrent > arrayExpiry.getFirstIndex())
            {
            synchronized (this)
                {
                Set setEvict = null;
                for (LongArray.Iterator iter = arrayExpiry.iterator(); iter.hasNext(); )
                    {
                    Set setKeys = (Set) iter.next();
                    if (setKeys != null && ldtCurrent > iter.getIndex())
                        {
                        iter.remove();
                        if (setEvict == null)
                            {
                            setEvict = setKeys;
                            }
                        else
                            {
                            setEvict.addAll(setKeys);
                            }
                        }
                    else
                        {
                        break;
                        }
                    }
                if (setEvict != null)
                    {
                    evictAll(setEvict);
                    }
                }
            }
        }


    // ----- inner class: EntryAttributes -----------------------------------

    /**
    * Factory pattern: Construct an attribute holder for an entry.
    *
    * @param attrOrig    the previous attributes for this entry, if any
    * @param ldtExpires  the date/time at which the entry expires, or zero
    * @param nTouch      the touch counter assigned to the entry
    * @param cUnits      the number of storage units used by the entry
    *
    * @return the instantiated {@link EntryAttributes}
    */
    public EntryAttributes instantiateEntryAttributes(
            EntryAttributes attrOrig, long ldtExpires, long nTouch, int cUnits)
        {
        return new EntryAttributes(ldtExpires, nTouch, cUnits);
        }

    /**
    * A class that holds on to the expiry time and touch order for an entry.
    */
    protected class EntryAttributes
            extends Base
        {
        /**
        * Construct an attribute holder for an entry.
        *
        * @param ldtExpires  the date/time at which the entry expires, or
        *                    zero
        * @param nTouch      the touch counter assigned to the entry
        * @param cUnits      the number of storage units used by the entry
        */
        public EntryAttributes(long ldtExpires, long nTouch, int cUnits)
            {
            m_ldtExpires = ldtExpires;
            m_nTouch     = nTouch;
            m_cUnits     = cUnits;
            }

        /**
        * Determine the date/time at which the entry expires.
        *
        * @return the system time at which the corresponding entry expires,
        *         or zero if the entry never expires
        */
        public long getExpiryTime()
            {
            return m_ldtExpires;
            }

        /**
        * Determine the absolute order of the entry within in the LRU list.
        *
        * @return the touch counter assigned to the corresponding entry
        */
        public long getTouchCount()
            {
            return m_nTouch;
            }

        /**
        * Determine the number of units of storage used by the entry.
        *
        * @return the storage units used by the entry
        */
        public int getUnits()
            {
            return m_cUnits;
            }

        /**
        * The date/time at which the corresponding entry expires.
        */
        private long m_ldtExpires;

        /**
        * The touch counter for the entry. This is the absolute order of the
        * entry in the LRU list.
        */
        private long m_nTouch;

        /**
        * The number of units of storage used by the entry.
        */
        private int m_cUnits;
        }


    // ----- inner class: EntrySet ------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected Set instantiateEntrySet()
        {
        return new EntrySet();
        }

    /**
    * A set of entries backed by this map.
    */
    public class EntrySet
            extends AbstractKeySetBasedMap.EntrySet
        {
        // ----- inner class: Entry -------------------------------------

        /**
        * {@inheritDoc}
        */
        protected Map.Entry instantiateEntry(Object oKey, Object oValue)
            {
            return new Entry(oKey, oValue);
            }

        /**
        * A Cache Entry implementation.
        */
        public class Entry
                extends AbstractKeyBasedMap.EntrySet.Entry
                implements ConfigurableCacheMap.Entry
            {
            /**
            * Construct an Entry.
            *
            * @param oKey    the Entry key
            * @param oValue  the Entry value (optional)
            */
            public Entry(Object oKey, Object oValue)
                {
                super(oKey, oValue);
                }

            /**
            * {@inheritDoc}
            */
            public void touch()
                {
                SerializationCache.this.touch(getKey());
                }

            /**
            * {@inheritDoc}
            */
            public int getTouchCount()
                {
                EntryAttributes attr = getAttributes();
                return (int) Math.max(attr == null ? 0L : attr.getTouchCount(), Integer.MAX_VALUE);
                }

            /**
            * {@inheritDoc}
            */
            public long getLastTouchMillis()
                {
                // value is unknown
                return 0L;
                }

            /**
            * {@inheritDoc}
            */
            public long getExpiryMillis()
                {
                EntryAttributes attr = getAttributes();
                return attr == null ? 0L : attr.getExpiryTime();
                }

            /**
            * {@inheritDoc}
            */
            public void setExpiryMillis(long lMillis)
                {
                EntryAttributes attr = getAttributes();
                if (attr != null)
                    {
                    registerKey(getKey(), null, null, (int)
                        Math.min(Math.max(1L, lMillis - getSafeTimeMillis()), Integer.MAX_VALUE));
                    }
                }

            /**
            * {@inheritDoc}
            */
            public int getUnits()
                {
                EntryAttributes attr = getAttributes();
                return attr == null ? -1 : attr.getUnits();
                }

            /**
            * {@inheritDoc}
            */
            public void setUnits(int cUnits)
                {
                SerializationCache cache = SerializationCache.this;
                synchronized (cache)
                    {
                    EntryAttributes attr = getAttributes();
                    if (attr != null)
                        {
                        int cOld = attr.getUnits();
                        if (cOld >= 0 && cUnits >= 0 && cOld != cUnits)
                            {
                            // adjust the units of the entry
                            cache.getKeyMap().put(getKey(),
                                    cache.instantiateEntryAttributes(attr,
                                    attr.getExpiryTime(), getTouchCount(), cUnits));

                            // adjust total units
                            cache.m_cCurUnits += (cUnits - cOld);
                            }
                        }
                    }
                }

            /**
            * Obtain the cache attributes for this entry.
            *
            * @return the entry attributes for this entry
            */
            protected EntryAttributes getAttributes()
                {
                return (EntryAttributes) SerializationCache.this.getKeyMap().get(getKey());
                }
            }
        }


    // ----- inner class: InternalEvictionPolicy ----------------------------

    /**
    * The InternalEvictionPolicy represents a pluggable eviction policy for
    * the non-pluggable built-in (internal) eviction policies supported by
    * this cache implementation.
    */
    public static class InternalEvictionPolicy
            implements EvictionPolicy
        {
        /**
        * Default constructor.
        */
        private InternalEvictionPolicy()
            {
            }

        /**
        * {@inheritDoc}
        */
        public void entryTouched(ConfigurableCacheMap.Entry entry)
            {
            throw new UnsupportedOperationException();
            }

        /**
        * {@inheritDoc}
        */
        public void requestEviction(int cMaximum)
            {
            throw new UnsupportedOperationException();
            }

        /**
        * {@inheritDoc}
        */
        public String getName()
            {
            return "Internal-LRU";
            }

        static final InternalEvictionPolicy INSTANCE = new InternalEvictionPolicy();
        }


    // ----- data fields ----------------------------------------------------

    /**
    * Maximum number of units to manage in the cache.
    */
    protected long m_cMaxUnits;

    /**
    * Current number of units to in the cache.
    */
    protected long m_cCurUnits;

    /**
    * Default expiry for entries added to the cache. This is a TTL value,
    * expressed in milliseconds. Zero indicates no expiry. A positive value
    * indicates the number of milliseconds that an entry will be valid after
    * it is placed into the cache, assuming a specific TTL is not provided
    * with the entry.
    */
    private int m_cDefaultTTLMillis;

    /**
    * The number of units to prune the cache down to.
    */
    protected long m_cPruneUnits;

    /**
    * Touch counter (for LRU).
    */
    private long m_cTouches;

    /**
    * Array of keys, indexed by the touch counter.
    */
    private LongArray m_arrayLRU;

    /**
    * Array of set of keys, indexed by the time of expiry.
    */
    private LongArray m_arrayExpiry;

    /**
    * The unit calculator to use to limit size of the cache.
    */
    protected UnitCalculator m_calculator;

    /**
    * The unit factor.
    */
    protected int m_nUnitFactor = 1;

    /**
    * The EvictionApprover.
    */
    protected EvictionApprover m_apprvrEvict;
    }
