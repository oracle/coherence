/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.datagrid.persistence;

/**
 * PersistenceStatistics provides statistics in relation to the entries and
 * metadata persisted to allow recovery of Coherence caches. These statistics
 * are accumulated from either actively persisted data, snapshots or archived
 * snapshots. Fundamentally these statistics provide a means to validate the
 * integrity of the persisted data but also provide an insight into the data
 * and metadata stored.
 * <p>
 * The usage of this data structure is intended to pivot around cache names,
 * thus to output the byte size of all caches the following would typically be
 * executed:
 * <pre><code>
 *     PersistenceStatistics stats = ...;
 *     for (String sCacheName : stats)
 *         {
 *         long cb = stats.getCacheBytes(sCacheName);
 *         System.out.printf("%s has %d bytes\n", sCacheName, cb);
 *         }
 * </code></pre>
 *
 * @author  tam/hr  2014.11.18
 * @since   12.2.1
 *
 * @deprecated use {@link com.oracle.coherence.persistence.PersistenceStatistics} instead
 */
@Deprecated
public class PersistenceStatistics
        extends com.oracle.coherence.persistence.PersistenceStatistics
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new instance to store persistence statistics.
     */
    public PersistenceStatistics()
        {
        super();
        }
    }
