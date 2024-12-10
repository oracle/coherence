/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.BinaryEntry;

import java.util.Set;


/**
* BinaryEntryStore is analogous to the {@link CacheStore}, but operates on
* {@link BinaryEntry} objects. Note that the BinaryEntry interface extends
* Map.Entry and provides dual access to the underlying data: in external
* (Object) format via the <tt>getKey/getValue/setValue</tt> methods or internal
* (Binary) format via <tt>getBinaryKey/getBinaryValue/updateBinaryValue</tt>
* methods. Additionally, for the purpose of the optimistic concurrency control,
* implementations could rely on the entry's "previous" values: in the external
* format using {@link BinaryEntry#getOriginalValue} and in the internal format
* using the {@link BinaryEntry#getOriginalBinaryValue} methods (for store
* operations, a value of null here would indicate an insert operation).
*
* @since Coherence 3.6
* @author gg 2009.09.25
*/
public interface BinaryEntryStore<K, V>
    {
    /**
    * Load the value from the underlying store and update the specified entry.
    * If the BinaryEntryStore is capable of loading Binary values, it should
    * update the entry using the {#link BinaryEntry.updateBinaryValue} API.
    *
    * @param binEntry  an entry that needs to be updated with the loaded value
    */
    public void load(BinaryEntry<K, V> binEntry);

    /**
    * Load the values from the underlying store and update the specified entries.
    * If the BinaryEntryStore is capable of loading Binary values, it should
    * update the entry using the {#link BinaryEntry.updateBinaryValue} API.
    *
    * @param setBinEntries  a set of entries that needs to be updated with the
    *                       loaded values
    */
    public void loadAll(Set<? extends BinaryEntry<K, V>> setBinEntries);

    /**
    * Store the specified entry in the underlying store. This method is intended
    * to support both the entry creation and value update.
    * <p>
    * If the store operation changes the entry's value, a best effort will be
    * made to place the changed value back into the corresponding backing map
    * (for asynchronous store operations a concurrent backing map modification
    * can make it impossible).
    *
    * @param binEntry  the entry to be stored
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void store(BinaryEntry<K, V> binEntry);

    /**
    * Store the entries in the specified set in the underlying store. This
    * method is intended to support both the entry creation and value update.
    * <p>
    * If this operation fails (by throwing an exception) after a partial
    * success, the convention is that entries which have been stored
    * successfully are to be removed from the specified set, indicating that the
    * store operation for the entries left in the map has failed or has not been
    * attempted.
    * <p>
    * If the storeAll operation changes some entries' values, a best effort will
    * be made to place the changed values back into the corresponding backing
    * map (for asynchronous store operations concurrent backing map modifications
    * can make it impossible).
    *
    * @param setBinEntries  the set of entries to be stored
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void storeAll(Set<? extends BinaryEntry<K, V>> setBinEntries);

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