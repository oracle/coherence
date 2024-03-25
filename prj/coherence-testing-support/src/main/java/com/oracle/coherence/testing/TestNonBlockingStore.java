/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing;

import com.oracle.coherence.common.base.Blocking;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.cache.NonBlockingEntryStore;
import com.tangosol.net.cache.StoreObserver;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ObservableHashMap;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.processor.AbstractProcessor;
import com.tangosol.util.processor.ConditionalPut;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public class TestNonBlockingStore<K, V>
        extends AbstractTestStore
        implements NonBlockingEntryStore<K, V>
    {
    /**
     * Default constructor.
     */
    public TestNonBlockingStore()
        {
        this(new ObservableHashMap());
        }

    /**
     * Create a new TestBinaryCacheStore that will use the NamedCache with the
     * specified name to store items.
     *
     * @param sName  the name of the NamedCache to use for storage
     */
    public TestNonBlockingStore(String sName)
        {
        this(CacheFactory.getCache(sName));
        }

    /**
     * Create a new TestBinaryCacheStore that will use the specified
     * ObservableMap to store items.
     *
     * @param mapStorage  the ObservableMap used for the underlying storage
     */
    public TestNonBlockingStore(ObservableMap mapStorage)
        {
        super(mapStorage);
        }

    public ExecutorService ensureExecutorService()
        {
        if (m_executorService == null)
            {
            m_executorService = Executors.newFixedThreadPool(200);
            }

        return m_executorService;
        }

    /**
     * Load the value from the underlying store and update the specified entry
     * by calling the <tt>onNext</tt> method of the provided
     * {@link StoreObserver} object, or <tt>onError</tt> if the store operation
     * failed.
     * <p>
     * If the NonBlockingEntryStore is capable of loading Binary values, it
     * should update the entry using the {#link BinaryEntry.updateBinaryValue}
     * API.
     *
     * @param binEntry  an entry that needs to be updated with the loaded value
     * @param observer  {@link StoreObserver} provided to caller to notify
     */
    @Override
    public void load(BinaryEntry<K, V> binEntry, StoreObserver<K, V> observer)
        {
        log(isVerboseLoad(), "TestNonBlockingStore load key: " + binEntry.getBinaryKey());

        Object oKey = binEntry.getKey();

        logMethodInvocation("load");

        if (getStorageMap().size() == 0)
            {
            try
                {
                // test close();
                if (oKey instanceof String)
                    {
                    String sKey = (String) oKey;
                    if (oKey.equals("IllegalState"))
                        {
                        ensureExecutorService().submit(() ->
                               {
                               // force close() to return before onNext
                               delay(2000);

                               try
                                   {
                                   observer.onNext(binEntry);
                                   }
                               catch(IllegalStateException ise)
                                   {
                                   getStorageMap().put(oKey, "IllegalStateException");
                                   }
                               });
                        }
                    }
                }
            catch (Exception e)
                {
                e.printStackTrace();
                }
            finally
                {
                observer.onComplete();
                }
            }

        ensureExecutorService().submit(() ->
                {
                delay(getDurationLoad());

                try
                   {
                   checkForFailure(getFailureKeyLoad(), oKey);

                   Object oValue = getStorageMap().get(oKey);
                   if (oValue != null)
                       {
                       binEntry.setValue((V) getStorageMap().get(oKey));
                       if (m_cExpiryMillis != Long.MIN_VALUE)
                           {
                           binEntry.expire(m_cExpiryMillis);
                           }
                       }
                   observer.onNext(binEntry);
                   }
                catch (RuntimeException re)
                   {
                   observer.onError(binEntry, re);
                   }
                finally
                   {
                   observer.onComplete();
                   }
                });
        }

    /**
     * Load the values from the underlying store and update the specified entries
     * by calling the <tt>onNext</tt> method of the provided
     * {@link StoreObserver} object, or <tt>onError</tt> if the store operation
     * failed.
     * <p>
     * If the NonBlockingEntryStore is capable of loading Binary values, it
     * should update the entry using the {#link BinaryEntry.updateBinaryValue}
     * API.
     *
     * @param setBinEntries  a set of entries that needs to be updated with the
     *                       loaded values
     * @param observer       {@link StoreObserver} provided to caller to notify
     *
     * @throws UnsupportedOperationException  if this implementation or the
     *         underlying store is read-only
     */
    @Override
    public void loadAll(Set<? extends BinaryEntry<K, V>> setBinEntries, StoreObserver<K, V> observer)
        {
        log(isVerboseLoadAll(), "loadAll[BinaryEntry](" + setBinEntries + ")");
        logMethodInvocation("loadAll");

        delay(getDurationLoadAll());

        AtomicInteger cEntries = new AtomicInteger(setBinEntries.size());

        for (Iterator iter = setBinEntries.iterator(); iter.hasNext(); )
            {
            BinaryEntry binEntry = (BinaryEntry) iter.next();
            Object oKey = binEntry.getKey();

            Object oValue = getStorageMap().get(oKey);
            if (oValue instanceof String)
                {
                if (((String) oValue).startsWith("Exception"))
                    {
                    observer.onError(binEntry, new RuntimeException("Test loadAll exception handling"));
                    observer.onComplete();
                    return;
                    }
                }

            ensureExecutorService().submit(() ->
                       {
                       try
                           {
                           checkForFailure(getFailureKeyLoadAll(), oKey);

                           if (oValue != null)
                               {
                               binEntry.setValue(oValue);
                               if (m_cExpiryMillis != Long.MIN_VALUE)
                                   {
                                   binEntry.expire(m_cExpiryMillis);
                                   }
                               }
                           observer.onNext(binEntry);
                           }
                       catch (Exception e)
                           {
                           observer.onError(binEntry, e);
                           }
                       synchronized (cEntries)
                           {
                           cEntries.decrementAndGet();
                           cEntries.notify();
                           }
                       });
            }

        // Wait for all threads to complete
        synchronized (cEntries)
            {
            while(cEntries.get() != 0)
                {
                try
                    {
                    Blocking.wait(cEntries);
                    }
                catch (Exception e)
                    {
                    e.printStackTrace();
                    }
                }
            }

        observer.onComplete();

        return;
        }

    /**
     * Store the specified entry in the underlying store, in an asynchronous
     * fashion. This method is intended to support both the entry creation and
     * value update upon invocation of the <tt>onNext</tt> method of the
     * provided {@link StoreObserver}. An error during an underlying store
     * operation, or custom logic, should invoke <tt>onError</tt> instead.
     * <p>
     * If the store operation changes the entry's value, a best effort will be
     * made to place the changed value back into the corresponding backing map
     * (for asynchronous store operations a concurrent backing map modification
     * can make it impossible).
     *
     * @param binEntry  the entry to be stored
     * @param observer  {@link StoreObserver} provided to caller to notify
     *
     * @throws UnsupportedOperationException  if this implementation or the
     *         underlying store is read-only
     */
    @Override
    public void store(BinaryEntry<K, V> binEntry, StoreObserver<K, V> observer)
        {
        log(isVerboseStore(), "TestNonBlockingStore store");

        Object oKey   = binEntry.getKey();
        Object oValue = binEntry.getValue();

        logMethodInvocation("store");

        ensureExecutorService().submit(() ->
                {
                if (oValue instanceof String)
                    {
                    if (((String) oValue).startsWith("DontOwn"))
                        {
                        delay(20000);
                        }
                    }

                delay(getDurationStore());
                getStorageMap().put(oKey, oValue);
                getProcessor().process(binEntry);
                delay(200);
                observer.onNext(binEntry);
                });
        }

    /**
     * Asynchronously store the entries in the specified set in the underlying
     * store. This method is intended to support both the entry creation and
     * value update upon invocation of the <tt>onNext</tt> method of the
     * provided {@link StoreObserver}. An error during an underlying store
     * operation, or custom logic, should invoke <tt>onError</tt> instead.
     * <p>
     * {@link StoreObserver#onNext} or {@link StoreObserver#onError} affects
     * individual entries in the specified set.
     * <p>
     * If the storeAll operation changes some entries' values, a best effort will
     * be made to place the changed values back into the corresponding backing
     * map (for asynchronous store operations concurrent backing map modifications
     * can make it impossible).
     *
     * @param setBinEntries  the set of entries to be stored
     * @param observer       {@link StoreObserver} provided to caller to notify
     *
     * @throws UnsupportedOperationException  if this implementation or the
     *         underlying store is read-only
     */
    @Override
    public void storeAll(Set<? extends BinaryEntry<K, V>> setBinEntries, StoreObserver<K, V> observer)
        {
        log(isVerboseStoreAll(), "TestNonBlockingStore storeAll for " + setBinEntries);
        logMethodInvocation("storeAll");

        boolean fRemove    = true;

        try
            {
            for (Iterator iter = setBinEntries.iterator(); iter.hasNext(); )
                {
                BinaryEntry binEntry = (BinaryEntry) iter.next();
                Object      oKey     = binEntry.getKey();
                Object      oValue   = binEntry.getValue();

                getProcessor().process(binEntry);

                if (oValue instanceof String)
                    {
                    String sValue = (String) oValue;
                    if (sValue.startsWith("Exception"))
                        {
                        observer.onError(binEntry, new RuntimeException("Test storeAll exception handling"));
                        return;
                        }
                    if (sValue.startsWith("Partial"))
                        {
                        int nIteration = Integer.valueOf(sValue.substring("Partial".length()));

                        if (nIteration >= 0 && nIteration < 10)
                            {
                            continue;
                            }
                        }
                    }

                ensureExecutorService().submit(() ->
                        {
                        delay(getDurationStore());
                        try
                           {
                           checkForFailure(getFailureKeyStoreAll(), oKey);

                           getStorageMap().put(oKey, oValue);
                           synchronized (iter)
                               {
                               observer.onNext(binEntry);
                               }
                           }
                        catch (Exception e)
                           {
                           observer.onError(binEntry, e);
                           }
                        });

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
        catch (Exception e)
            {
            e.printStackTrace();
            }
        finally
            {
            observer.onComplete();
            }
        }


    /**
     * Remove the specified entry from the underlying store.
     *
     * @param binEntry  the entry to be removed from the store
     *
     * @throws UnsupportedOperationException  if this implementation or the
     *         underlying store is read-only
     */
    @Override
    public void erase(BinaryEntry<K, V> binEntry)
        {
        Object oKey = binEntry.getKey();

        log(isVerboseErase(), "erase[BinaryEntry](" + oKey + ")");
        logMethodInvocation("erase");

        delay(getDurationErase());

        checkForFailure(getFailureKeyErase(), oKey);
        getStorageMap().remove(oKey);
        }

    /**
     * Remove the specified entries from the underlying store.
     * <p>
     * If this operation fails (by throwing an exception) after a partial
     * success, the convention is that entries which have been erased
     * successfully are to be removed from the specified set, indicating that
     * the erase operation for the entries left in the collection has failed or
     * has not been attempted.
     *
     * @param setBinEntries  the set entries to be removed from the store
     *
     * @throws UnsupportedOperationException  if this implementation or the
     *         underlying store is read-only
     */
    @Override
    public void eraseAll(Set<? extends BinaryEntry<K, V>> setBinEntries)
        {
        log(isVerboseEraseAll(), "eraseAll[BinaryEntry](" + setBinEntries + ")");
        logMethodInvocation("eraseAll");

        delay(getDurationEraseAll());

        Map mapStorage = getStorageMap();
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

    // ----- helpers ---------------------------------------------------

    /**
     * Set the store value.
     *
     * @param oStoreValue  the store value; null is determined to be no action
     */
    public void setStoreValue(Object oStoreValue)
        {
        log("setStoreValue called, setting to " + oStoreValue);
        m_processor = oStoreValue == null ? NullImplementation.getEntryProcessor()
                                          : new ConditionalPut(AlwaysFilter.INSTANCE, oStoreValue);
        }

    /**
     * Set the EntryProcessor that will be executed for each entry in a store
     * or storeAll request.
     *
     * @param processor  the EntryProcessor that will be executed for each entry
     *                   in a store or storeAll request
     */
    public void setProcessor(InvocableMap.EntryProcessor processor)
        {
        m_processor = processor;
        }

    /**
     * Return the EntryProcessor that will be executed for each entry in a store
     * or storeAll request.
     *
     * @return the EntryProcessor that will be executed for each entry in a store
     *         or storeAll request
     */
    public InvocableMap.EntryProcessor getProcessor()
        {
        return m_processor;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An EntryProcessor that reverts the binary of the BinaryEntry to its
     * original value.
     */
    public static final InvocableMap.EntryProcessor REVERTING_PROCESSOR = new AbstractProcessor()
        {
        public Object process(InvocableMap.Entry entry)
            {
            BinaryEntry binEntry = (BinaryEntry) entry;

            binEntry.setValue(binEntry.getOriginalValue());

            return null;
            }
        };

    /**
     * An EntryProcessor that removes the entry.
     */
    public static final InvocableMap.EntryProcessor REMOVING_PROCESSOR = new AbstractProcessor()
        {
        public Object process(InvocableMap.Entry entry)
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

    private ExecutorService m_executorService;

    /**
     * The EntryProcessor to execute against each entry for each store or storeAll
     * request.
     */
    protected InvocableMap.EntryProcessor m_processor = NullImplementation.getEntryProcessor();
    }
