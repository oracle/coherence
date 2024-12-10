/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.datagrid.persistence;

/**
 * A PersistenceManager is responsible for managing the collection, location,
 * and lifecycle of persistent key-value stores.
 *
 * @param <R>  the type of a raw, environment specific object representation
 *
 * @author rhl/gg/jh/mf/hr 2012.06.12
 *
 * @deprecated use {@link com.oracle.coherence.persistence.PersistenceManager} instead
 */
@Deprecated
public interface PersistenceManager<R>
    extends com.oracle.coherence.persistence.PersistenceManager<R>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public PersistenceTools getPersistenceTools();
    }
