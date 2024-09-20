/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.persistence;

import com.oracle.coherence.common.base.Collector;

import com.tangosol.util.NullImplementation;

/**
 * PersistentStore represents a persistence facility to store and recover
 * key/value pairs. Each key-value pair is namespaced by a numeric "extent"
 * identifier and may be stored or removed in atomic units.
 * <p>
 * A PersistentStore implementation should be optimized for random writes and
 * sequential reads (e.g. iteration), as it is generally assumed to only be
 * read from during recovery operations. Additionally, all operations with
 * the exception of key and entry iteration may be called concurrently and
 * therefore must be thread-safe.
 *
 * @param <R>  the type of a raw, environment specific object representation
 *
 * @author rhl/gg/jh/mf/hr 2012.06.12
 */
public interface PersistentStore<R>
    {
    /**
     * Return the identifier of this store.
     *
     * @return the identifier that was used to open this store
     */
    public String getId();

    // ---- extent lifecycle ------------------------------------------------

    /**
     * Ensure that an extent with the given identifier exists in the
     * persistent store, returning true iff the extent was created.
     *
     * @param lExtentId  the identifier of the extent to ensure
     *
     * @return true iff the specified extent did not previously exist
     */
    public boolean ensureExtent(long lExtentId);

    /**
     * Delete the specified extent from the persistent store, ensuring that
     * any key-value mappings associated with the extent are no longer valid.
     * <p>
     * Removal of the key-value mappings associated with the extent from the
     * underlying storage is the responsibility of the implementation, and
     * may (for example) be performed immediately, asynchronously, or
     * deferred until space is required.
     *
     * @param lExtentId  the identifier of the extent to delete
     */
    public void deleteExtent(long lExtentId);

    /**
     * Truncate the specified extent from the persistent store, ensuring that
     * any key-value mappings associated with the extent are removed.
     * <p>
     * Removal of the key-value mappings associated with the extent from the
     * underlying storage is the responsibility of the implementation, and
     * may (for example) be performed immediately, asynchronously, or
     * deferred until space is required.
     *
     * @param lExtentId  the identifier of the extent to truncate
     */
    public void truncateExtent(long lExtentId);

    /**
     * Move the specified extent from the old extent id to the new extent id.
     * <p>
     * Upon control being returned the implementation guarantees that any data
     * data that used to reside against the old extent id is accessible from
     * new extent id using the {@link #load(long, Object) load} API. In addition,
     * calls to {@link #store(long, Object, Object, Object) store} are permitted
     * immediately after control is returned.
     *
     * @param lOldExtentId  the old extent identifier
     * @param lNewExtentId  the new extent identifier
     */
    public void moveExtent(long lOldExtentId, long lNewExtentId);

    /**
     * Return a list of the extent identifiers in the underlying store.
     *
     * @return a list of the extent identifiers in the underlying store
     */
    public long[] extents();

    /**
     * Suggest to this PersistentStore that the caller requires exclusive access
     * to this store until {@link AutoCloseable#close() close} is called on the
     * returned {@link AutoCloseable}.
     * <p>
     * Note: the caller <b>must</b> call {@link AutoCloseable#close() close} on
     *       the returned object
     *
     * @return an {@link AutoCloseable} object that <b>requires</b> close to be
     *         called on it when exclusive access is no longer needed
     */
    public default AutoCloseable exclusively()
        {
        return NullImplementation.getAutoCloseable();
        }

    // ----- store operations -----------------------------------------------

    /**
     * Return the value associated with the specified key, or null if the key
     * does not have an associated value in the underlying store.
     *
     * @param lExtentId  the extent identifier for the key
     * @param key        key whose associated value is to be returned
     *
     * @return the value associated with the specified key, or <tt>null</tt>
     *         if no value is available for that key
     *
     * @throws IllegalArgumentException if the specified extent does not exist
     *                                  or the key is invalid
     */
    public R load(long lExtentId, R key);

    /**
     * Return true if the specified extend identifer exist.
     *
     * @param lExtentId  the extent identifier
     *
     * @return true if the specified extend identifer exist
     *
     * @since 24.09
     */
    public boolean containsExtent(long lExtentId);

    /**
     * Store the specified value under the specific key in the underlying
     * store. This method is intended to support both key-value pair creation
     * and value update for a specific key.
     *
     * @param lExtentId  the extent identifier for the key-value pair
     * @param key        key to store the value under
     * @param value      value to be stored
     * @param oToken     optional token that represents a set of mutating
     *                   operations to be committed as an atomic unit; if
     *                   null, the given key-value pair will be committed to
     *                   the store automatically by this method
     *
     * @throws IllegalArgumentException if the specified extent does not exist,
     *                                  or if the key, value or token is invalid
     */
    public void store(long lExtentId, R key, R value, Object oToken);

    /**
     * Remove the specified key from the underlying store if present.
     *
     * @param lExtentId  the extent identifier for the key
     * @param key        key whose mapping is to be removed
     * @param oToken     optional token that represents a set of mutating
     *                   operations to be committed as an atomic unit; if
     *                   null, the removal of the given key will be committed
     *                   to the store automatically by this method
     *
     * @throws IllegalArgumentException if the specified extent does not exist,
     *                                  of if the key or the token is invalid
     */
    public void erase(long lExtentId, R key, Object oToken);

    /**
     * Iterate the key-value pairs in the persistent store, applying the
     * specified visitor to each key-value pair.
     *
     * @param visitor  the visitor to apply
     */
    public void iterate(Visitor<R> visitor);

    /**
     * Return true if the store is open.
     *
     * @return true if the store is open
     *
     * @since 24.09
     */
    public boolean isOpen();

    // ----- transaction demarcation ----------------------------------------

    /**
     * Begin a new sequence of mutating operations that should be committed
     * to the store as an atomic unit. The returned token should be passed to
     * all mutating operations that should be part of the atomic unit. Once
     * the sequence of operations have been performed, they must either be
     * {@link #commit(Object) committed} to the store or the atomic unit must
     * be {@link #abort(Object) aborted}.
     *
     * @return a token that represents the atomic unit
     */
    public Object begin();

    /**
     * Begin a new sequence of mutating operations that should be committed
     * to the store asynchronously as an atomic unit. The returned token
     * should be passed to all mutating operations that should be part of the
     * atomic unit. Once the sequence of operations have been performed, they
     * must either be {@link #commit(Object) committed} to the store or the
     * atomic unit must be {@link #abort(Object) aborted}.
     * <p>
     * If a collector is passed to this method, the specified receipt will be
     * added to it when the unit is committed. If the operation is {@link #abort
     * aborted} or an error occurs during the commit, an {@link
     * AsyncPersistenceException} that wraps the cause and
     * specified receipt will be added. Finally, the collector will be flushed.
     *
     * @param collector an optional collector
     * @param oReceipt  a receipt to be added to the collector (if any) when
     *                  the unit is committed
     *
     * @return a token representing the atomic unit that will be committed
     *         asynchronously
     */
    public Object begin(Collector<Object> collector, Object oReceipt);

    /**
     * Commit a sequence of mutating operations represented by the given
     * token as an atomic unit.
     *
     * @param oToken  a token that represents the atomic unit to commit
     *
     * @throws IllegalArgumentException if the token is invalid
     */
    public void commit(Object oToken);

    /**
     * Abort an atomic sequence of mutating operations.
     *
     * @param oToken  a token that represents the atomic unit to abort
     *
     * @throws IllegalArgumentException if the token is invalid
     */
    public void abort(Object oToken);

    // ----- inner interface: Visitor ---------------------------------------

    /**
     * The Visitor interface allows the "iteration" of the contents of a
     * persistent store in the style of the
     * <a href="http://en.wikipedia.org/wiki/Visitor_pattern">Visitor Pattern</a>.
     *
     * @param <R>  the type of a raw, environment specific object representation
     */
    public interface Visitor<R>
        {
        /**
         * Apply the visitor to the specified extent-scoped key-value pair.
         *
         * @param lExtentId  the extent identifier
         * @param key        the key
         * @param value      the value
         *
         * @return false to terminate the iteration
         */
        public boolean visit(long lExtentId, R key, R value);

        /**
         * Apply the visitor to the specified extent.
         *
         * @param lExtentId  the extent identifier
         *
         * @return false to terminate the iteration
         */
        public default boolean visitExtent(long lExtentId)
            {
            return true;
            }

        /**
         * Return a key this Visitor should start visiting from, or null to start
         * from either the start of the store or anywhere in the store.
         * <p>
         * PersistentStore implementations are open to not supporting the start
         * key.
         *
         * @return the key to start visiting from or null to start visiting from
         *         the start of the store
         */
        public default R visitFromKey()
            {
            return null;
            }
        }
    }