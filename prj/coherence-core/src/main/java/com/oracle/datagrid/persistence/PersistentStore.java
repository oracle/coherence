/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.datagrid.persistence;

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
 *
 * @deprecated use {@link com.oracle.coherence.persistence.PersistentStore} instead
 */
@Deprecated
public interface PersistentStore<R>
    extends com.oracle.coherence.persistence.PersistentStore<R>
    {
    }
