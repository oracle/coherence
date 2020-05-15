/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.datagrid.persistence;

/**
 * PersistenceTools provides an ability to submit operations under the context
 * of a {@link PersistenceManager}, hence a reference to PersistenceTools is
 * retrieved from {@link PersistenceManager#getPersistenceTools()}. The PersistenceManager
 * typically will refer to offline stores such as a snapshot, archived snapshot,
 * or an unlinked active store (disconnected from running cache server).
 * <p>
 * The intent of this interface is to describe some offline operations that can
 * be performed against {@link PersistentStore}s to validate their integrity
 * and accumulate offline information. One of the primary benefits is to verify
 * recovery will be successful. There are other operations that can be beneficial,
 * including retrieving statistics, correction and compaction.
 *
 * @since 12.2.1
 * @author hr/tam  2014.10.11
 *
 * @deprecated use {@link com.oracle.coherence.persistence.PersistenceTools} instead
 */
@Deprecated
public interface PersistenceTools
    extends com.oracle.coherence.persistence.PersistenceTools
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public OfflinePersistenceInfo getPersistenceInfo();

    /**
     * {@inheritDoc}
     */
    @Override
    public PersistenceStatistics getStatistics();
    }
