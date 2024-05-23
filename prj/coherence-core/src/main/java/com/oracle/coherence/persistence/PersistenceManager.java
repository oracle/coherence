/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.persistence;

import com.oracle.coherence.common.base.Collector;

import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WrapperBufferInput;
import com.tangosol.io.WrapperBufferOutput;
import com.tangosol.io.WriteBuffer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A PersistenceManager is responsible for managing the collection, location,
 * and lifecycle of persistent key-value stores.
 *
 * @param <R>  the type of a raw, environment specific object representation
 *
 * @author rhl/gg/jh/mf/hr 2012.06.12
 */
public interface PersistenceManager<R>
    {
    /**
     * Return the name of this manager.
     *
     * @return the name of this manager
     */
    public String getName();

    /**
     * Create a {@link PersistentStore} associated with the specified
     * identifier.
     * <p>
     * Creation of a store suggests its registration but has no usage
     * until it transitions into a state of open. The implementation may
     * choose to forgo any resource allocation until the caller
     * {@link #open opens} the same identifier.
     *
     * @param sId  a unique identifier of the store
     *
     * @return a persistent store
     */
    public PersistentStore createStore(String sId);

    /**
     * Open or create a {@link PersistentStore} associated with the specified
     * identifier and based on the provided {@link PersistentStore store}.
     * <p>
     * Upon a new store being created the provided store should be used as the
     * basis for the new store such that the extents and associated data is
     * available in the returned store. This provides an opportunity for an
     * implementation to optimize initializing the new store based upon knowledge
     * of the storage mechanics.
     *
     * @param sId        a unique identifier for the store
     * @param storeFrom  the PersistenceStore the new store should be based upon
     *
     * @return a PersistentStore associated with the specified identifier
     *
     * @throws ConcurrentAccessException if the specified store is
     *         unavailable for exclusive access
     *
     * @throws FatalAccessException if the specified store could not be
     *         opened due to a non-recoverable issue with the store
     *
     * @throws PersistenceException if the specified store could not be
     *         opened due to a potentially recoverable issue with the store
     *
     * @throws IllegalArgumentException if the specified identifier is invalid
     */
    public PersistentStore<R> open(String sId, PersistentStore<R> storeFrom);

    /**
     * Open or create a {@link PersistentStore} associated with the specified
     * identifier and based on the provided {@link PersistentStore store}.
     * <p>
     * Upon a new store being created the provided store should be used as the
     * basis for the new store such that the extents and associated data is
     * available in the returned store. This provides an opportunity for an
     * implementation to optimize initializing the new store based upon knowledge
     * of the storage mechanics.
     * <p>
     * Providing a {@link Collector} allows the open operation to be performed
     * asynchronously. This may be desirable when the calling thread can not
     * be blocked on IO operations that are required when creating a new store
     * based on an old store ({@code storeFrom}). Open is only non-blocking when
     * both an old store <b>and</b> a Collector are provided. Upon completion
     * of an asynchronous open request the provided Collector is called with
     * either a String (GUID) or an AsyncPersistenceException, thus notifying
     * the collector of success of failure respectively.
     * <p>
     * Note: the behavior of a returned store that has not been opened is undefined.
     *
     * @param sId        a unique identifier for the store
     * @param storeFrom  the PersistenceStore the new store should be based upon
     * @param collector  the Collector to notify once the store has been opened
     *                   or failed to open; the collector will either receive
     *                   a String (GUID) or an AsyncPersistenceException
     *
     * @return a PersistentStore associated with the specified identifier
     *
     * @throws ConcurrentAccessException if the specified store is
     *         unavailable for exclusive access
     *
     * @throws FatalAccessException if the specified store could not be
     *         opened due to a non-recoverable issue with the store
     *
     * @throws PersistenceException if the specified store could not be
     *         opened due to a potentially recoverable issue with the store
     *
     * @throws IllegalArgumentException if the specified identifier is invalid
     */
    public PersistentStore<R> open(String sId, PersistentStore<R> storeFrom, Collector<Object> collector);

    /**
     * Close the associated PersistentStore and release exclusive access to
     * the associated resources.
     *
     * @param sId  a unique identifier of the store to close
     *
     * @throws IllegalArgumentException if the specified identifier is invalid
     */
    public void close(String sId);

    /**
     * Remove the PersistentStore associated with the specified identifier.
     *
     * @param sId    a unique identifier of the store to remove
     * @param fSafe  if true, remove the store by moving it to a restorable
     *               location (if possible) rather than deleting it
     *
     * @return true if the store was successfully removed, false otherwise
     *
     * @throws IllegalArgumentException if the specified identifier is invalid
     */
    public boolean delete(String sId, boolean fSafe);

    /**
     * Return a list of the PersistentStoreInfo known to this manager.
     *
     * @return a list of the known PersistentStoreInfo
     *
     * @since 24.09
     */
    public PersistentStoreInfo[] listStoreInfo();

    /**
     * Return true if the specified directory is empty.
     *
     * @return true if the specified directory is empty
     *
     * @since 24.09
     */
    public boolean isEmpty(String sId);

    /**
     * Return the identifiers of the PersistentStores known to this manager.
     *
     * @return a list of the known store identifiers
     */
    public default boolean contains(String sGUID)
        {
        for (PersistentStoreInfo storeCurrent : listStoreInfo())
            {
            if (storeCurrent.getId().equals(sGUID))
                {
                return true;
                }
            }
        return false;
        }

    /**
     * Return the identifiers of PersistentStores that are currently open.
     *
     * @return a list of the open store identifiers
     */
    public String[] listOpen();

    /**
     * Read the PersistenceStore associated with the specified identifier
     * from the given input stream, making it available to this manager.
     *
     * @param sId  a unique identifier of the store to read
     * @param in   the stream to read from
     *
     * @throws IOException if an error occurred while reading from the stream
     *
     * @throws PersistenceException if an error occurred while writing to the
     *         specified store
     *
     * @throws IllegalArgumentException if the specified identifier is invalid
     */
    public default void read(String sId, InputStream in)
            throws IOException
        {
        ReadBuffer.BufferInput bufIn = in instanceof ReadBuffer.BufferInput
                ? (ReadBuffer.BufferInput) in
                : new WrapperBufferInput(in instanceof DataInputStream
                        ? (DataInputStream) in : new DataInputStream(in));

        read(sId, bufIn);
        }

    /**
     * Read the PersistenceStore associated with the specified identifier
     * from the given input stream, making it available to this manager.
     *
     * @param sId  a unique identifier of the store to read
     * @param in   the stream to read from
     *
     * @throws IOException if an error occurred while reading from the stream
     *
     * @throws PersistenceException if an error occurred while writing to the
     *         specified store
     *
     * @throws IllegalArgumentException if the specified identifier is invalid
     */
    public void read(String sId, ReadBuffer.BufferInput in)
            throws IOException;

    /**
     * Write the PersistentStore associated with the specified identifier to
     * the given output stream.
     *
     * @param sId  a unique identifier of the store to write
     * @param out  the stream to write to
     *
     * @throws IOException if an error occurred while writing to the stream
     *
     * @throws PersistenceException if an error occurred while reading from
     *         the specified store
     *
     * @throws IllegalArgumentException if the specified identifier is invalid
     */
    public default void write(String sId, OutputStream out)
            throws IOException
        {
        WriteBuffer.BufferOutput bufOut = out instanceof WriteBuffer.BufferOutput
                ? (WriteBuffer.BufferOutput) out
                : new WrapperBufferOutput(out instanceof DataOutputStream
                        ? (DataOutputStream) out : new DataOutputStream(out));

        write(sId, bufOut);
        }

    /**
     * Write the PersistentStore associated with the specified identifier to
     * the given output buffer.
     *
     * @param sId  a unique identifier of the store to write
     * @param out  the output buffer to write to
     *
     * @throws IOException if an error occurred while writing to the stream
     *
     * @throws PersistenceException if an error occurred while reading from
     *         the specified store
     *
     * @throws IllegalArgumentException if the specified identifier is invalid
     */
    public void write(String sId, WriteBuffer.BufferOutput out) throws IOException;

    /**
     * Copy the PersistentStore associated with the specified identifier to
     * the configured safe area.
     *
     * @throws IllegalArgumentException if the specified identifier is invalid
     */
    public default void writeSafe(String sId)
        {
        }

    /**
     * Release all resources held by this manager. Note that the behavior
     * of all other methods on this manager is undefined after this method
     * is called.
     */
    public void release();

    /**
     * Return an instance of {@link PersistenceTools} allowing offline operations
     * to be performed against the associated PersistenceManager and appropriate
     * {@link PersistentStore}.
     *
     * @return a PersistenceTools implementation
     */
    public PersistenceTools getPersistenceTools();

    /**
     * Perform any necessary maintenance of the underlying environment.
     */
    public default void maintainEnvironment()
        {
        }
    }
