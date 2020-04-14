/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.ConcurrentModificationException;
import java.util.Map;

/**
* Map with additional concurrency features.
 *
 * @param <K>  the type of the map entry keys
 * @param <V>  the type of the map entry values
*
* @author gg  2001.12.16
*/
public interface ConcurrentMap<K, V>
        extends Map<K, V>
    {
    /**
    * Attempt to lock the specified item within the specified period of time.
    * <p>
    * The item doesn't have to exist to be <i>locked</i>. While the item is
    * locked there is known to be a <i>lock holder</i> which has an exclusive
    * right to modify (calling put and remove methods) that item.
    * <p>
    * Lock holder is an abstract concept that depends on the ConcurrentMap
    * implementation. For example, holder could be a cluster member or
    * a thread (or both).
    * <p>
    * Locking strategy may vary for concrete implementations as well. Lock
    * could have an expiration time (this lock is sometimes called a "lease")
    * or be held indefinitely (until the lock holder terminates).
    * <p>
    * Some implementations may allow the entire map to be locked. If the map is
    * locked in such a way, then only a lock holder is allowed to perform
    * any of the "put" or "remove" operations.
    * Pass the special constant {@link #LOCK_ALL} as the <i>oKey</i> parameter
    * to indicate the map lock.
    *
    * @param oKey   key being locked
    * @param cWait  the number of milliseconds to continue trying to obtain
    *               a lock; pass zero to return immediately; pass -1 to block
    *               the calling thread until the lock could be obtained
    *
    * @return true if the item was successfully locked within the
    *              specified time; false otherwise
    */
    public boolean lock(Object oKey, long cWait);

    /**
    * Attempt to lock the specified item and return immediately.
    * <p>
    * This method behaves exactly as if it simply performs the call
    * <tt>lock(oKey, 0)</tt>.
    *
    * @param oKey key being locked
    *
    * @return true if the item was successfully locked; false otherwise
    */
    public boolean lock(Object oKey);

    /**
    * Unlock the specified item. The item doesn't have to exist to be
    * <i>unlocked</i>. If the item is currently locked, only
    * the <i>holder</i> of the lock could successfully unlock it.
    *
    * @param oKey key being unlocked
    *
    * @return true if the item was successfully unlocked; false otherwise
    */
    public boolean unlock(Object oKey);


    // ---- Map interface ---------------------------------------------------

    /**
    * Returns the number of key-value mappings in this map. Note that
    * this number does not include the items that were <i>locked</i>
    * but didn't have corresponding map entries.
    *
    * @return the number of key-value mappings in this map
    */
    public int size();

    /**
    * Returns <tt>true</tt> if this map contains no key-value mappings.
    * Note that the map could have some items locked and be empty
    * at the same time.
    *
    * @return <tt>true</tt> if this map contains no key-value mappings
    */
    public boolean isEmpty();

    /**
    * Returns <tt>true</tt> if this map contains a mapping for the specified
    * key.
    *
    * @param key key whose presence in this map is to be tested
    *
    * @return <tt>true</tt> if this map contains a mapping for the specified
    *         key
    *
    * @throws ClassCastException if the key is of an inappropriate type for
    *         this map
    * @throws NullPointerException if the key is <tt>null</tt> and this map
    *         does not not permit <tt>null</tt> keys
    */
    public boolean containsKey(Object key);

    /**
    * Returns <tt>true</tt> if this map maps one or more keys to the
    * specified value.  More formally, returns <tt>true</tt> if and only if
    * this map contains at least one mapping to a value <tt>v</tt> such that
    * <tt>(value==null ? v==null : value.equals(v))</tt>.  This operation
    * will probably require time linear in the map size for most
    * implementations of the <tt>ConcurrentMap</tt> interface.
    *
    * @param value value whose presence in this map is to be tested
    *
    * @return <tt>true</tt> if this map maps one or more keys to the
    *         specified value
    */
    public boolean containsValue(Object value);

    /**
    * Returns the value to which this map maps the specified key.  Returns
    * <tt>null</tt> if the map contains no mapping for this key.  A return
    * value of <tt>null</tt> does not <i>necessarily</i> indicate that the
    * map contains no mapping for the key; it's also possible that the map
    * explicitly maps the key to <tt>null</tt>.  The <tt>containsKey</tt>
    * operation may be used to distinguish these two cases.
    *
    * @param key key whose associated value is to be returned
    *
    * @return the value to which this map maps the specified key, or
    *         <tt>null</tt> if the map contains no mapping for this key
    *
    * @throws ClassCastException if the key is of an inappropriate type for
    *         this map
    * @throws NullPointerException key is <tt>null</tt> and this map does not
    *         not permit <tt>null</tt> keys
    *
    * @see #containsKey(Object)
    */
    public V get(Object key);

    // Modification Operations

    /**
    * Associates the specified value with the specified key in this map
    * (optional operation). If the map previously contained a mapping for
    * this key, the old value is replaced.
    * <p>
    * Some implementations will attempt to obtain a lock for the key (if
    * necessary) before proceeding with the put operation. For such
    * implementations, the specified item has to be either already locked or
    * able to be locked for this operation to succeed.
    *
    * @param key key with which the specified value is to be associated
    * @param value value to be associated with the specified key
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for key.  A <tt>null</tt> return can
    *         also indicate that the map previously associated <tt>null</tt>
    *         with the specified key, if the implementation supports
    *         <tt>null</tt> values
    *
    * @throws IllegalArgumentException if the value cannot be stored
    *             in the map (i.e. not serializable)
    * @throws ConcurrentModificationException if the lock could not be
    *             successfully obtained for the specified key
    * @throws ClassCastException if the class of the specified key or value
    *             prevents it from being stored in this map
    * @throws NullPointerException this map does not permit <tt>null</tt>
    *             keys or values, and the specified key or value is
    *             <tt>null</tt>
    */
    public V put(K key, V value);

    /**
    * Removes the mapping for this key from this map if present (optional
    * operation).
    * <p>
    * Some implementations will attempt to obtain a lock for the key (if
    * necessary) before proceeding with the remove operation. For such
    * implementations, the specified item has to be either already locked or
    * able to be locked for this operation to succeed.
    *
    * @param key key whose mapping is to be removed from the map
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for key.  A <tt>null</tt> return can
    *         also indicate that the map previously associated <tt>null</tt>
    *         with the specified key, if the implementation supports
    *         <tt>null</tt> values
    *  
    * @throws ConcurrentModificationException if the lock could not be
    *         successfully obtained for the specified key
    */
    public V remove(Object key);


    // Bulk Operations

    /**
    * Copies all of the mappings from the specified map to this map
    * (optional operation).  These mappings will replace any mappings that
    * this map had for any of the keys currently in the specified map.
    *
    * @param map Mappings to be stored in this map
    *
    * @throws ClassCastException if the class of a key or value in the
    *            specified map prevents it from being stored in this map
    *
    * @throws ConcurrentModificationException if the lock could not be
    *            successfully obtained for some key
    * @throws NullPointerException this map does not permit <tt>null</tt>
    *            keys or values, and the specified key or value is
    *            <tt>null</tt>
    */
    public void putAll(Map<? extends K, ? extends V> map);

    /**
    * Removes all mappings from this map.
    * <p>
    * Some implementations will attempt to lock the entire map (if necessary)
    * before proceeding with the clear operation. For such implementations, the
    * entire map has to be either already locked or able to be locked for this
    * operation to succeed.
    *
    * @throws ConcurrentModificationException if a lock could not be
    *            successfully obtained for some key
    */
    public void clear();

    // View Operations
    /*
    The following methods have the same semantics as the Map's methods

    public Set keySet();
    public Set entrySet();
    public Collection values();
    public Set entrySet();
    */

    // ---- constants -------------------------------------------------------

    /**
    * Special key value indicating an intent to lock the entire map.
    */
    public static final Object LOCK_ALL = new Externalizable()
        {
        public int hashCode()
            {
            throw noimpl();
            }
        public boolean equals(Object o)
            {
            throw noimpl();
            }
        public void readExternal(ObjectInput in)
            {
            noimpl();
            }
        public void writeExternal(ObjectOutput out)
            {
            noimpl();
            }
        public RuntimeException noimpl()
            {
            throw new UnsupportedOperationException("LOCK_ALL is not implemented for this cache");
            }
        };
    }