/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.datagrid.persistence;

/**
 * A PersistenceEnvironment is responsible for managing a singleton
 * PersistenceManager and provides facilities for creating, opening, and
 * deleting persistent copies or "snapshots" of a PersistenceManager.
 *
 * @param <R>  the type of a raw, environment specific object representation
 *
 * @author jh/rl/hr  2013.05.09
 *
 * @deprecated use {@link com.oracle.coherence.persistence.PersistenceEnvironment} instead
 */
@Deprecated
public interface PersistenceEnvironment<R>
    extends com.oracle.coherence.persistence.PersistenceEnvironment<R>
    {
    }
