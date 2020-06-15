/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.io.Serializer;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;


/**
* Map.Entry that internally stores both key and value in a Binary format and
* uses an underlying Serializer to convert it to and from an Object view.
*
* @author as  2009.01.05
*
* @since Coherence 3.5
*/
public interface BinaryEntry<K, V>
        extends InvocableMap.Entry<K, V>
    {
    /**
    * Return a raw binary key for this entry.
    *
    * @return a raw binary key for this entry
    */
    public Binary getBinaryKey();

    /**
    * Return a raw binary value for this entry.
    *
    * @return a raw binary value for this entry; null if the value does not
    *         exist
    */
    public Binary getBinaryValue();

    /**
    * Return a {@link Serializer} that is used to serialize/deserialize this
    * entry.
    *
    * @return a {@link Serializer} that is used to serialize/deserialize this
    *         entry
    */
    public Serializer getSerializer();

    /**
    * Return the context this entry operates within.
    * <p>
    * Note: This method is a shortcut for the {@link
    * BackingMapContext#getManagerContext() getBackingMapContext.getManagerContext()}
    * call.
    * @return the BackingMapManagerContext for this entry
    */
    public BackingMapManagerContext getContext();

    /**
    * Update the binary value for this entry.
    * <p>
    * Passing a non-null binary is functionally equivalent to:
    * <pre>
    *   setValue(getContext().getValueFromInternalConverter().convert(binValue));
    * </pre>
    * Passing null value is functionally equivalent to removing the entry.
    * <pre>
    *   remove(false);
    * </pre>
    *
    * @param binValue  new binary value to be stored in this entry or null
    */
    public void updateBinaryValue(Binary binValue);

    /**
    * Update the binary value for this entry.
    * <p>
    * Passing a non-null binary is functionally equivalent to:
    * <pre>
    *   setValue(getContext().getValueFromInternalConverter().convert(binValue));
    * </pre>
    * Passing null value is functionally equivalent to removing the entry.
    * <pre>
    *   remove(false);
    * </pre>
    * This method will bypass any
    * {@link com.tangosol.net.cache.CacheStore CacheStore} or
    * {@link com.tangosol.net.cache.BinaryEntryStore BinaryEntryStore}
    * implementations, iff <tt>fSynthetic</tt> is true and the
    * backing map associated with this entry is a ReadWriteBackingMap.
    *
    * @param binValue    new binary value to be stored in this entry or null
    * @param fSynthetic  pass true only if the insertion into or
    *                    modification of the Map should be treated as a
    *                    synthetic event
    *
    * @since Coherence 12.1.2
    */
    public void updateBinaryValue(Binary binValue, boolean fSynthetic);

    /**
    * Return an original value for this entry.
    *
    * @return an original value for this entry
    *
    * @since Coherence 3.6
    */
    public V getOriginalValue();

    /**
    * Return a raw original binary value for this entry.
    *
    * @return a raw original binary value for this entry; null if the original
    *         value did not exist
    *
    * @since Coherence 3.6
    */
    public Binary getOriginalBinaryValue();

    /**
    * Obtain a reference to the backing map that this Entry corresponds to.
    * The returned Map should be used in a read-only manner.
    * <p>
    * Note: This method is a shortcut for the {@link
    * BackingMapContext#getBackingMap() getBackingMapContext().getBackingMap()}
    * call. As of Coherence 3.7, the returned type has been narrowed to
    * ObservableMap.
    *
    * @return the backing map reference; null if the entry does not have any
    *         backing map association
    *
    * @since Coherence 3.6
    *
    * @deprecated As of Coherence 12.1.3, replaced with
    *             {@link BackingMapContext#getBackingMapEntry}
    *             {@link BackingMapContext#getBackingMapEntry}
    */
    public ObservableMap<K, V> getBackingMap();

    /**
    * Obtain a reference to the {@link BackingMapContext backing map context}
    * for the cache that this Entry corresponds to.
    *
    * @return the corresponding BackingMapContext; null if the entry does not
    *         have any backing map association
    *
    * @since Coherence 3.7
    */
    public BackingMapContext getBackingMapContext();

    /**
    * Update the entry with the specified expiry delay.
    * <p>
    * Note: this method only has an effect only if the associated backing map
    *       implements the {@link com.tangosol.net.cache.CacheMap} interface
    *
    * @param cMillis  the number of milliseconds until the entry will expire;
    *                 pass {@link com.tangosol.net.cache.CacheMap#EXPIRY_DEFAULT
    *                CacheMap.EXPIRY_DEFAULT} to use the default expiry setting;
    *                pass {@link com.tangosol.net.cache.CacheMap#EXPIRY_NEVER
    *                CacheMap.EXPIRY_NEVER} to indicate that the entry should
    *                never expire
    *
    * @since Coherence 3.7
    */
    public void expire(long cMillis);

    /**
    * Return the number of milliseconds remaining before the specified entry
    * is scheduled to expire.  If the {@link #expire(long)} method has been called,
    * the returned value will reflect the requested expiry delay.  Otherwise if
    * the entry {@link #isPresent() exists}, the returned value will represent
    * the time remaining until the entry expires (or {@link
    * com.tangosol.net.cache.CacheMap#EXPIRY_NEVER CacheMap.EXPIRY_NEVER} if the
    * entry will never expire). If the entry does not exist, {@link
    * com.tangosol.net.cache.CacheMap#EXPIRY_DEFAULT CacheMap.EXPIRY_DEFAULT}
    * will be returned.
    * <p>
    * This method will make a <i>"best effort"</i> attempt to determine the
    * expiry time remaining.  In some cases, it may not be possible to determine
    * the expiry (e.g. the backing-map does not implement the {@link
    * com.tangosol.net.cache.CacheMap CacheMap} interface), in which case
    * {@link com.tangosol.net.cache.CacheMap#EXPIRY_DEFAULT CacheMap.EXPIRY_DEFAULT}
    * will be returned.
    *
    * @return the number of milliseconds remaining before the specified entry expires
    *
    * @since Coherence 12.1.3
    */
    public long getExpiry();

    /**
    * Check whether this BinaryEntry allows data modification operations.
    *
    * @return true iff the entry is "read-only"
    *
    * @since Coherence 3.7
    */
    public boolean isReadOnly();

    /**
    * Check whether this BinaryEntry is loaded by a "read-through" operation.
    *
    * @return true iff the entry is loaded by a "read-through" operation
    *
    * @since Coherence 12.2.1
    */
     default public boolean isValueLoaded()
         {
         return getBinaryValue() == getOriginalBinaryValue();
         }

    /**
    * Check whether the value of this BinaryEntry is changed.
    *
    * @return true iff the entry is changed
    *
    * @since Coherence 12.2.1.1
    */
    default public boolean isValueChanged()
        {
        return isValueUpdated() || isValueRemoved();
        }

    /**
    * Check whether the value of this BinaryEntry is updated.
    *
    * @return true iff the entry is updated
    *
    * @since Coherence 12.2.1.1
    */
    default public boolean isValueUpdated()
        {
        return getBinaryValue() != getOriginalBinaryValue();
        }

    /**
    * Check whether the value of this BinaryEntry is removed.
    *
    * @return true iff the entry is removed
    *
    * @since Coherence 12.2.1.1
    */
    default public boolean isValueRemoved()
        {
        return getBinaryValue() == null && getOriginalBinaryValue() != null;
        }


    // ----- InvocableMap.Entry interface -----------------------------------

    /**
    * {@inheritDoc}
    * <p>
    * As of Coherence 12.1.2, if <tt>fSynthetic</tt> is true and the
    * {@link #getBackingMap BackingMap} associated with this entry is a
    * ReadWriteBackingMap, this method will bypass the
    * {@link com.tangosol.net.cache.CacheStore CacheStore} or
    * {@link com.tangosol.net.cache.BinaryEntryStore BinaryEntryStore}.
    */
    public void setValue(V value, boolean fSynthetic);

    /**
    * {@inheritDoc}
    * <p>
    * As of Coherence 12.1.2, if <tt>fSynthetic</tt> is true and the
    * {@link #getBackingMap BackingMap} associated with this entry is a
    * ReadWriteBackingMap, this method will bypass the
    * {@link com.tangosol.net.cache.CacheStore CacheStore} or
    * {@link com.tangosol.net.cache.BinaryEntryStore BinaryEntryStore}.
    */
    public void remove(boolean fSynthetic);
    }
