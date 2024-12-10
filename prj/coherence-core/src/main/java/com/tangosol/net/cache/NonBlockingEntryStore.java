/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;

import com.tangosol.util.BinaryEntry;

import java.util.Set;

/**
 * NonBlockingEntryStore provides a means to integrate Coherence with an underlying
 * data source that offers a non-blocking API.
 * <p>
 * The methods on this interface a called based on a get, getAll, put, putAll, remove
 * or removeAll respectively. Similar to {@link BinaryEntryStore}, the methods on this
 * interface receive a BinaryEntry allowing them to avoid deserialization, if possible,
 * for the key, value and original value by using the {@code get*Binary*} equivalent
 * methods.
 * <p>
 * The expectation is for implementations to execute non-blocking APIs that will complete
 * at some point in the future. Once the operation completes the implementation can
 * notify the provided {@link StoreObserver} via {@link StoreObserver#onNext onNext} or
 * {@link StoreObserver#onError onError} passing the <b>same</b> BinaryEntry that was
 * provided which correlates to the successful or unsuccessful operation. Additionally,
 * the {@link StoreObserver} offers an {@link StoreObserver#onComplete onComplete} method to allow
 * an implementation to suggest to Coherence that no further processing will occur for the
 * relevant operation. Below is an example implementation:
 * <pre><code>
 *   public void loadAll(Set<? extends BinaryEntry<K, V>> setBinEntries, StoreObserver<K, V> observer)
 *       {
 *       SomeReactiveResource resource;
 *       for (BinaryEntry<K, V> binEntry : setBinEntries)
 *           {
 *           CompletableFuture<V> future = resource.get(binEntry.getKey);
 *           future.whenComplete((value, exception) ->
 *               {
 *               if (exception == null)
 *                   {
 *                   binEntry.setValue(value);
 *                   observer.onNext(binEntry))
 *                   }
 *               else
 *                   {
 *                   observer.onError(binEntry, exception))
 *                   if (isTerminal(exception))
 *                       {
 *                       // no futher processing will be possible as resource
 *                       // is terminally unavailable and assume futures will
 *                       // not be fired
 *                       observer.onComplete();
 *                       }
 *                   }
 *               }
 *           }
 *       }
 * </code></pre>
 * Some additional notes on calling {@link StoreObserver#onComplete()}:
 * <ul>
 *   <li>The StoreObserver instance will throw a IllegalStateExcpetion on
 *       any future calls to {@link StoreObserver#onNext onNext} or {@link StoreObserver#onError onError}.</li>
 *   <li>Any unprocessed entires will have their decorations removed thus store
 *       will not be called on failover</li>
 * </ul>
 *
 * @since 21.06
 * @author mg/hr/as
 * @see StoreObserver
 */
public interface NonBlockingEntryStore<K, V>
    {
    /**
     * Load the value from the underlying store, update the provided entry and
     * call the {@link StoreObserver#onNext onNext} method of the provided
     * {@link StoreObserver} object, or {@link StoreObserver#onError onError} if the store operation
     * failed.
     *
     * @param binEntry  an entry that needs to be updated with the loaded value
     * @param observer  {@link StoreObserver} provided to caller to notify
     */
    public void load(BinaryEntry<K, V> binEntry, StoreObserver<K, V> observer);

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
    public void loadAll(Set<? extends BinaryEntry<K, V>> setBinEntries, StoreObserver<K, V> observer);

    /**
     * Store the specified entry in the underlying store, in an asynchronous
     * fashion. This method will be called for inserts and updates. Once
     * successfully or unsuccessfully stored this implementation should
     * call {@link StoreObserver#onNext onNext} or {@link StoreObserver#onError
     * onError} respectively.
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
    public void store(BinaryEntry<K, V> binEntry, StoreObserver<K, V> observer);

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
    public void storeAll(Set<? extends BinaryEntry<K, V>> setBinEntries, StoreObserver<K, V> observer);

    /**
     * Remove the specified entry from the underlying store.
     *
     * @param binEntry  the entry to be removed from the store
     *
     * @throws UnsupportedOperationException  if this implementation or the
     *         underlying store is read-only
     */
    public void erase(BinaryEntry<K, V> binEntry);

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
    public void eraseAll(Set<? extends BinaryEntry<K, V>> setBinEntries);
    }
