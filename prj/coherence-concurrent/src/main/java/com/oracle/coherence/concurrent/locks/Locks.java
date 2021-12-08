/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks;

import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;

import com.oracle.coherence.concurrent.locks.internal.ExclusiveLockHolder;
import com.oracle.coherence.concurrent.locks.internal.ReadWriteLockHolder;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Factory methods for various local and remote lock implementations.
 *
 * @author Aleks Seovic  2021.10.20
 * @since 21.12
 */
public class Locks
    {
    /**
     * Return a singleton instance of a local {@link ReentrantLock}
     * with a specified name.
     *
     * @param sName  the process-wide, unique name of the lock
     *
     * @return an instance of a local lock with a specified name
     */
    public static ReentrantLock localLock(String sName)
        {
        return f_mapExclusiveLocal.computeIfAbsent(sName, n -> new ReentrantLock());
        }

    /**
     * Return a singleton instance of a remote {@link RemoteLock}
     * with a specified name.
     *
     * @param sName  the cluster-wide, unique name of the lock
     *
     * @return an instance of a remote lock with a specified name
     */
    public static RemoteLock remoteLock(String sName)
        {
        return f_mapExclusive.computeIfAbsent(sName, n -> new RemoteLock(n, exclusiveLocksMap()));
        }

    /**
     * Return a singleton instance of a local {@link ReentrantReadWriteLock}
     * with a specified name.
     *
     * @param sName  the process-wide, unique name of the lock
     *
     * @return an instance of a local read/write lock with a specified name
     */
    public static ReentrantReadWriteLock localReadWriteLock(String sName)
        {
        return f_mapReadWriteLocal.computeIfAbsent(sName, n -> new ReentrantReadWriteLock());
        }

    /**
     * Return a singleton instance of a remote {@link RemoteReadWriteLock}
     * with a specified name.
     *
     * @param sName  the cluster-wide, unique name of the lock
     *
     * @return an instance of a remote read/write lock with a specified name
     */
    public static RemoteReadWriteLock remoteReadWriteLock(String sName)
        {
        return f_mapReadWrite.computeIfAbsent(sName, n -> new RemoteReadWriteLock(n, readWriteLocksMap()));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return Coherence {@link Session} for the Locks module.
     *
     * @return Coherence {@link Session} for the Locks module
     */
    protected static Session session()
        {
        return Coherence.findSession(SESSION_NAME)
                        .orElseThrow(() ->
                                new IllegalStateException(String.format("The session '%s' has not been initialized", SESSION_NAME)));
        }

    /**
     * Return Coherence {@link NamedMap} containing the exclusive locks state.
     *
     * @return Coherence {@link NamedMap} containing the exclusive locks state
     */
    public static NamedMap<String, ExclusiveLockHolder> exclusiveLocksMap()
        {
        return session().getMap("locks-exclusive");
        }

    /**
     * Return Coherence {@link NamedMap} containing the read/write locks state.
     *
     * @return Coherence {@link NamedMap} containing the read/write locks state
     */
    public static NamedMap<String, ReadWriteLockHolder> readWriteLocksMap()
        {
        return session().getMap("locks-read-write");
        }

    // ----- constants ------------------------------------------------------

    /**
     * The session name.
     */
    public static final String SESSION_NAME = ConcurrentServicesSessionConfiguration.SESSION_NAME;

    /**
     * A process-wide cache of named local locks.
     */
    private static final Map<String, ReentrantLock> f_mapExclusiveLocal = new ConcurrentHashMap<>();

    /**
     * A process-wide cache of named local read/write locks.
     */
    private static final Map<String, ReentrantReadWriteLock> f_mapReadWriteLocal = new ConcurrentHashMap<>();

    /**
     * A process-wide cache of remote locks, to avoid creating multiple lock
     * instances (and thus sync objects) for the same server-side lock.
     */
    private static final Map<String, RemoteLock> f_mapExclusive = new ConcurrentHashMap<>();

    /**
     * A process-wide cache of remote read/write locks, to avoid creating multiple lock
     * instances (and thus sync objects) for the same server-side lock.
     */
    private static final Map<String, RemoteReadWriteLock> f_mapReadWrite = new ConcurrentHashMap<>();
    }
