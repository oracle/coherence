/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import java.util.Enumeration;


/**
* A CacheService is a clustered service providing a collection of named Maps
* that hold resources shared among members of a cluster. These resources are
* expected to be managed in memory, and are typically composed of data that
* are also stored persistently in a database, or data that have been
* assembled or calculated at some significant cost, thus these resources are
* referred to as <i>cached</i>.
*
* @author gg  2002.02.08
*
* @since Coherence 1.1
*/
public interface CacheService
        extends Service
    {
    /**
    * Return a backing map manager used by this CacheService.
    *
    * @return a backing map manager
    *
    * @see #setBackingMapManager(BackingMapManager)
    *
    * @since Coherence 2.0
    */
    public BackingMapManager getBackingMapManager();

    /**
    * Set a backing map manager to be used by this CacheService to create
    * underlying stores for the cached data. Some cache services may choose to
    * ignore this setting.
    *
    * @param manager  a backing map manager
    *
    * @exception IllegalStateException thrown if the service is already running
    *
    * @since Coherence 2.0
    */
    public void setBackingMapManager(BackingMapManager manager);

    /**
    * Obtain a NamedCache interface that provides a view of resources shared
    * among members of a cluster. The view is identified by name within this
    * CacheService. Typically, repeated calls to this method with the same
    * view name and class loader instance will result in the same view
    * reference being returned.
    *
    * @param sName   the name, within this CacheService, that uniquely
    *                identifies a view; null is legal, and may imply a
    *                default name
    * @param loader  ClassLoader that should be used to deserialize objects
    *                inserted in the map by other members of the cluster;
    *                null is legal, and implies the default ClassLoader,
    *                which will typically be the context ClassLoader for
    *                this service
    *
    * @return a NamedCache interface which can be used to access the resources
    *         of the specified view
    *
    * @exception IllegalStateException thrown if the service is not running
    */
    public NamedCache ensureCache(String sName, ClassLoader loader);

    /**
    * Returns an Enumeration of String objects, one for each cache name that
    * has been previously registered with this CacheService.
    *
    * @return Enumeration of cache names
    *
    * @exception IllegalStateException thrown if the CacheService
    *            is not running or has stopped
    */
    public Enumeration getCacheNames();

    /**
    * Release local resources associated with the specified instance of the
    * cache. This invalidates a reference obtained by using the
    * {@link #ensureCache(String, ClassLoader)} method.
    * <p>
    * Releasing a Map reference to a cache makes the Map reference no longer
    * usable, but does not affect the cache itself. In other words, all other
    * references to the cache will still be valid, and the cache data is not
    * affected by releasing the reference.
    * <p>
    * The reference that is released using this method can no longer be used;
    * any attempt to use the reference will result in an exception.
    * <p>
    * The purpose for releasing a cache reference is to allow the cache
    * implementation to release the ClassLoader used to deserialize items
    * in the cache. The cache implementation ensures that all references to
    * that ClassLoader are released. This implies that objects in the cache
    * that were loaded by that ClassLoader will be re-serialized to release
    * their hold on that ClassLoader. The result is that the ClassLoader can
    * be garbage-collected by Java in situations where the cache is operating
    * in an application server and applications are dynamically loaded and
    * unloaded.
    *
    * @param map  the cache object to be released
    *
    * @see NamedCache#release()
    */
    public void releaseCache(NamedCache map);

    /**
    * Release and destroy the specified cache.
    * <p>
    * <b>Warning:</b> This method is used to completely destroy the specified
    * cache across the cluster. All references in the entire cluster to this
    * cache will be invalidated, the cached data will be cleared, and all
    * resources will be released.
    *
    * @param map  the cache object to be released
    *
    * @see NamedCache#destroy()
    */
    public void destroyCache(NamedCache map);


    // ----- inner interface: CacheAction ---------------------------------

    /**
    * CacheAction represents a type of action taken by a CacheService.
    */
    public interface CacheAction
            extends Action
        {
        /**
        * Singleton action for cache writes.
        */
        public static final Action WRITE = new CacheAction(){};

        /**
        * Singleton action for cache reads.
        */
        public static final Action READ  = new CacheAction(){};
        }


    // ----- constants ------------------------------------------------------

    /**
    * ReplicatedCache service type constant.
    * <p>
    * ReplicatedCache service provides the means for handling a collection
    * of resources replicated across a cluster with concurrent access control.
    *
    * @see Cluster#ensureService(String, String)
    */
    public static final String TYPE_REPLICATED  = "ReplicatedCache";

    /**
    * OptimisticCache service type constant.
    * <p>
    * OptimisticCache service is an implementation similar to the ReplicatedCache,
    * but without any concurrency control and the highest possible throughput.
    *
    * @see Cluster#ensureService(String, String)
    */
    public static final String TYPE_OPTIMISTIC  = "OptimisticCache";

    /**
    * DistributedCache service type constant.
    * <p>
    * DistributedCache service provides the means for handling a collection
    * of resources distributed across a cluster with concurrent access control.
    *
    * @see Cluster#ensureService(String, String)
    */
    public static final String TYPE_DISTRIBUTED = "DistributedCache";

    /**
    * PagedTopic service type constant.
    * <p>
    * DistributedTopic service provides the means for handling a collection
    * of paged topics across a cluster with concurrent access control.
    *
    * @see Cluster#ensureService(String, String)
    */
    public static final String TYPE_PAGED_TOPIC = "PagedTopic";

    /**
    * LocalCache service type constant.
    * <p>
    * LocalCache service provides the means for handling a collection
    * of resources limited to a single JVM with concurrent access control.
    *
    * @see Cluster#ensureService(String, String)
    */
    public static final String TYPE_LOCAL       = "LocalCache";

    /**
    * RemoteCache service type constant.
    * <p>
    * RemoteCache service provides the means for handling a collection
    * of resources managed by a remote JVM with concurrent access control.
    *
    * @see Cluster#ensureService(String, String)
    */
    public static final String TYPE_REMOTE      = "RemoteCache";

    /**
    * RemoteGrpcCache service type constant.
    * <p>
    * RemoteCache service provides the means for handling a collection
    * of resources managed by a remote JVM with concurrent access control
    * connecting over gRPC.
    *
    * @see Cluster#ensureService(String, String)
    */
    public static final String TYPE_REMOTE_GRPC = "RemoteGrpcCache";

    /**
    * FederatedCache service type constant.
    * <p>
    * FederatedCache is a super set of DistributedCache that
    * provides the means of replicating data to members of
    * a federation (typically remote clusters).
    *
    * @see Cluster#ensureService(String, String)
    */
    public static final String TYPE_FEDERATED   = "FederatedCache";
    }
