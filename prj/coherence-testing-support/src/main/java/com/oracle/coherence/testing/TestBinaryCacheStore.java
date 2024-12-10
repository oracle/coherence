/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing;


import com.tangosol.net.CacheFactory;

import com.tangosol.net.cache.BinaryEntryStore;
import com.tangosol.net.cache.ReadWriteBackingMap;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ObservableHashMap;
import com.tangosol.util.ObservableMap;

import com.tangosol.util.filter.AlwaysFilter;

import com.tangosol.util.processor.AbstractProcessor;
import com.tangosol.util.processor.ConditionalPut;

import java.util.Set;
import java.util.Map;
import java.util.Iterator;


/**
* {@link BinaryEntryStore} implementation for testing {@link ReadWriteBackingMap}
* functionality.
*/
public class TestBinaryCacheStore
        extends AbstractTestStore
        implements BinaryEntryStore
    {
    /**
    * Default constructor.
    */
    public TestBinaryCacheStore()
        {
        this(new ObservableHashMap());
        }

    /**
    * Create a new TestBinaryCacheStore that will use the
    * specified binary entry expiry value.
    */
    public TestBinaryCacheStore(long expiry)
        {
        this(new ObservableHashMap());
        this.m_cExpiryMillis = expiry;
        }
    
    /**
    * Create a new TestBinaryCacheStore that will use the NamedCache with the
    * specified name to store items.
    *
    * @param sName  the name of the NamedCache to use for storage
    */
    public TestBinaryCacheStore(String sName)
        {
        this(CacheFactory.getCache(sName));
        }

    /**
    * Create a new TestBinaryCacheStore that will use the specified
    * ObservableMap to store items.
    *
    * @param mapStorage  the ObservableMap used for the underlying storage
    */
    public TestBinaryCacheStore(ObservableMap mapStorage)
        {
        super(mapStorage);
        }

    /**
    * {@inheritDoc}
    */
    public void load(BinaryEntry binEntry)
        {
        Object oKey = binEntry.getKey();

        log(isVerboseLoad(), "load[BinaryEntry](" + oKey + ")");
        logMethodInvocation("load");

        delay(getDurationLoad());

        checkForFailure(getFailureKeyLoad(), oKey);

        Object oValue = getStorageMap().get(oKey);
        if (oValue != null)
            {
            binEntry.setValue(getStorageMap().get(oKey));
            if (m_cExpiryMillis != Long.MIN_VALUE)
	            {
	            binEntry.expire(m_cExpiryMillis);
	            }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void loadAll(Set setBinEntries)
        {
        log(isVerboseLoadAll(), "loadAll[BinaryEntry](" + setBinEntries + ")");
        logMethodInvocation("loadAll");

        delay(getDurationLoadAll());

        for (Iterator iter = setBinEntries.iterator(); iter.hasNext(); )
            {
            BinaryEntry binEntry = (BinaryEntry) iter.next();

            Object oKey = binEntry.getKey();

            checkForFailure(getFailureKeyLoadAll(), oKey);

            Object oValue = getStorageMap().get(oKey);
            if (oValue != null)
                {
                binEntry.setValue(oValue);
                if (m_cExpiryMillis != Long.MIN_VALUE)
	                {
	                binEntry.expire(m_cExpiryMillis);
	                }
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void store(BinaryEntry binEntry)
        {
        Object oKey   = binEntry.getKey();
        Object oValue = binEntry.getValue();

        log(isVerboseStore(), "store[BinaryEntry](" + oKey + ", " + oValue + ")");
        logMethodInvocation("store");

        delay(getDurationStore());

        checkForFailure(getFailureKeyStore(), oKey);
        getStorageMap().put(oKey, oValue);

        if (m_cExpiryMillis != Long.MIN_VALUE)
            {
            binEntry.expire(m_cExpiryMillis);
            }

        getProcessor().process(binEntry);
        }

    /**
    * {@inheritDoc}
    */
    public void storeAll(Set setBinEntries)
        {
        log(isVerboseStoreAll(), "storeAll[BinaryEntry](" + setBinEntries + ")");
        logMethodInvocation("storeAll");

        delay(getDurationStoreAll());

        Map     mapStorage = getStorageMap();
        boolean fRemove    = true;

        for (Iterator iter = setBinEntries.iterator(); iter.hasNext(); )
            {
            BinaryEntry binEntry = (BinaryEntry) iter.next();
            Object      oKey     = binEntry.getKey();
            Object      oValue   = binEntry.getValue();

            checkForFailure(getFailureKeyStoreAll(), oKey);
            mapStorage.put(oKey, oValue);

            if (m_cExpiryMillis != Long.MIN_VALUE)
	            {
	            binEntry.expire(m_cExpiryMillis);
	            }

            getProcessor().process(binEntry);

            if (fRemove)
                {
                try
                    {
                    iter.remove();
                    }
                catch (UnsupportedOperationException e)
                    {
                    fRemove = false;
                    }
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void erase(BinaryEntry binEntry)
        {
        Object oKey = binEntry.getKey();

        log(isVerboseErase(), "erase[BinaryEntry](" + oKey + ")");
        logMethodInvocation("erase");

        delay(getDurationErase());

        checkForFailure(getFailureKeyErase(), oKey);
        getStorageMap().remove(oKey);
        }

    /**
    * {@inheritDoc}
    */
    public void eraseAll(Set setBinEntries)
        {
        log(isVerboseEraseAll(), "eraseAll[BinaryEntry](" + setBinEntries + ")");
        logMethodInvocation("eraseAll");

        delay(getDurationEraseAll());

        Map     mapStorage = getStorageMap();
        boolean fRemove    = true;

        for (Iterator iter = setBinEntries.iterator(); iter.hasNext(); )
            {
            BinaryEntry entry = (BinaryEntry) iter.next();

            Object oKey = entry.getKey();
            checkForFailure(getFailureKeyEraseAll(), oKey);
            mapStorage.remove(oKey);

            if (fRemove)
                {
                try
                    {
                    iter.remove();
                    }
                catch (UnsupportedOperationException e)
                    {
                    fRemove = false;
                    }
                }
            }
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Set the store value.
    *
    * @param oStoreValue  the store value; null is determined to be no action
    */
    public void setStoreValue(Object oStoreValue)
        {
        m_processor = oStoreValue == null ? NullImplementation.getEntryProcessor()
                                          : new ConditionalPut(AlwaysFilter.INSTANCE, oStoreValue);
        }

    /**
    * Return the EntryProcessor that will be executed for each entry in a store
    * or storeAll request.
    *
    * @return the EntryProcessor that will be executed for each entry in a store
    *         or storeAll request
    */
    public EntryProcessor getProcessor()
        {
        return m_processor;
        }

    /**
    * Set the EntryProcessor that will be executed for each entry in a store
    * or storeAll request.
    *
    * @param processor  the EntryProcessor that will be executed for each entry
    *                   in a store or storeAll request
    */
    public void setProcessor(EntryProcessor processor)
        {
        m_processor = processor;
        }

    // ----- inner-class: ExpireProcessor -----------------------------------

    /**
    * An EntryProcessor that expires each {@link Entry entry} with the given
    * expiry time.
    */
    public static class ExpireProcessor
            extends AbstractProcessor
        {
        // ----- constructors -----------------------------------------------

        /**
        * Default constructor.
        */
        public ExpireProcessor()
            {
            }

        /**
        * Construct an ExpireProcessor with the provided expiry time.
        *
        * @param cExpire  the expiry time to set for each entry
        */
        public ExpireProcessor(long cExpire)
            {
            m_cExpire = cExpire;
            }

        // ----- AbstractProcessor methods ----------------------------------

        @Override
        public Object process(Entry entry)
            {
            ((BinaryEntry) entry).expire(m_cExpire);

            return null;
            }

        // ----- data members -----------------------------------------------

        /**
        * The expire time to set for each entry.
        */
        protected long m_cExpire;
        }

    // ----- constants ------------------------------------------------------

    /**
    * An EntryProcessor that reverts the binary of the BinaryEntry to its
    * original value.
    */
    public static final EntryProcessor REVERTING_PROCESSOR = new AbstractProcessor()
        {
        public Object process(Entry entry)
            {
            BinaryEntry binEntry = (BinaryEntry) entry;

            binEntry.setValue(binEntry.getOriginalValue());

            return null;
            }
        };

    /**
    * An EntryProcessor that removes the entry.
    */
    public static final EntryProcessor REMOVING_PROCESSOR = new AbstractProcessor()
        {
        public Object process(Entry entry)
            {
            BinaryEntry binEntry = (BinaryEntry) entry;

            binEntry.remove(true);

            return null;
            }
        };

    // ----- data members ---------------------------------------------------

    /**
    * Expiry value in milliseconds.
    */
    protected long m_cExpiryMillis = Long.MIN_VALUE;

    /**
    * The EntryProcessor to execute against each entry for each store or storeAll
    * request.
    */
    protected EntryProcessor m_processor = NullImplementation.getEntryProcessor();
    }