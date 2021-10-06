/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.tangosol.net.security.StorageAccessAuthorizer;

import com.tangosol.run.xml.XmlConfigurable;
import com.tangosol.run.xml.XmlElement;

import java.util.Map;


/**
* A callback interface used by CacheService implementations. By providing
* a custom implementation of this interface, it is possible to use an
* alternative underlying store for the cached data.
* <p>
* A custom implementation may return a {@link com.tangosol.net.cache.LocalCache}
* in order to size-restrict the cache or force automatic expiry of cached data.
* <p>
* Currently, the "Distributed" and "Optimistic" CacheService implementations
* accept custom implementations of the BackingMapManager.
* <p>
* If the BackingMapManager object also implements {@link XmlConfigurable}
* interface then the {@link XmlConfigurable#setConfig(XmlElement)} method on it
* is called every time the configuration xml is changed by other cluster members.
* <p>
* <b>Very important note: all methods of this interface are called on a thread
* associated with the corresponding CacheService and any significant delay will
* negatively affect the performance of this service around the cluster.</b>
*
* @author gg 2002.09.21
*
* @since Coherence 2.0
*/
public interface BackingMapManager
    {
    /**
    * Called by a CacheService to indicate to this manager that the manager
    * is being associated with the CacheService. This method is called once
    * immediately upon the startup of the CacheService, before any NamedCache
    * objects are created by the CacheService.
    * <p>
    * <b>Important note:</b> BackingMapManager cannot be associated with more
    * then one instance of a CacheService. However, in a situation when a
    * CacheService automatically restarts, it is possible that this manager
    * instance is re-used by a newly created (restarted) CacheService
    * calling this method once again providing a new context.
    *
    * @param context  the BackingMapManagerContext object for this BackingMapManager
    */
    public void init(BackingMapManagerContext context);

    /**
    * Obtain the "container" ConfigurableCacheFactory that created
    * this manager and which this manager is bound to.
    *
    * @return the ConfigurableCacheFactory that created this manager
    */
    public ConfigurableCacheFactory getCacheFactory();

    /**
    * Determine the current BackingMapManagerContext for this
    * BackingMapManager.
    *
    * @return the current context
    */
    public BackingMapManagerContext getContext();

    /**
    * Instantiate a [thread safe] Map that should be used by a CacheService
    * to store cached values for a NamedCache with the specified name.
    * <p>
    * If the contents of the Map can be modified by anything other than
    * the CacheService itself (e.g. if the Map automatically expires its
    * entries periodically or size-limits its contents), then the returned
    * object <b>must</b> implement the ObservableMap interface.
    *
    * @param sName  the name of the NamedCache for which this backing map
    *               is being created
    *
    * @return an object implementing the Map interface that will provide
    *         backing storage for the specified cache name
    */
    public Map instantiateBackingMap(String sName);

    /**
    * Determine if the contents of the Map that is used by a CacheService to
    * store cached values for a NamedCache with the specified name should be
    * persisted.
    *
    * @param sName  the name of the NamedCache
    *
    * @return true if the CacheService should persist the backing storage of
    *         the specified NamedCache
    */
    public boolean isBackingMapPersistent(String sName);

    /**
    * Determine if the contents of the Map that is used by a CacheService to
    * store cached values for a NamedCache with the specified name should be
    * persisted.
    *
    * @param sName      the name of the NamedCache
    * @param fSnapshot  true if asked for the purpose of creating a snapshot
    *
    * @return true if the CacheService should persist the backing storage of
    *         the specified NamedCache
    */
    public default boolean isBackingMapPersistent(String sName, boolean fSnapshot)
        {
        return isBackingMapPersistent(sName);
        }

    /**
     * Determine if the Map that is used by a CacheService to store cached values
     * for a NamedCache with specified name enables the sliding expiry - the
     * expiry delay being extended by the read operations.
     *
     * @param sName  the name of the NamedCache
     *
     * @return true if the backing map of the specified NamedCache
     *         enables the sliding expiry
     */
    public boolean isBackingMapSlidingExpiry(String sName);

    /**
     * Determine the {@link StorageAccessAuthorizer} that is used by a
     * CacheService to secure access to the NamedCache with the specified name.
     *
     * @param sName  the name of the NamedCache
     *
     * @return  the {@link StorageAccessAuthorizer} or null if the authorizer is not configured
     *
     * @throws RuntimeException if the {@link StorageAccessAuthorizer} was configured, but not able to instantiate
     */
    public StorageAccessAuthorizer getStorageAccessAuthorizer(String sName);

    /**
     * Determine if the Map that is used by a CacheService to store cached values
     * for a NamedCache with the specified name enables reading from the closest
     * member.
     * <p>
     * This can result in a stale read such that the primary or other backups
     * have seen future values but the member that performed the read has not.
     * There is a latency benefit that can be realized and is the primary reason
     * to enable this functionality (balancing read requests across primary and
     * backup nodes may be another reason to counter hot partitions).
     * Note: 'closeness' is based on the member performing the read being on the
     * same machine, rack or site as any of the nodes that hold data for the
     * partition(s) being targeted.
     *
     * @param sName  the name of the NamedCache
     *
     * @return true if the cache associated with the given name allows reading
     *         from backup members that are deemed closer
     */
    public default boolean isReadFromClosest(String sName)
        {
        return false;
        }

    /**
    * Release the specified Map that was created using the
    * {@link #instantiateBackingMap(String)} method. This method is invoked
    * by the CacheService when the CacheService no longer requires the
    * specified Map object.
    *
    * @param sName  the name of the NamedCache for which the specified Map
    *               object has acted as the backing map
    * @param map    the Map object that is being released
    */
    public void releaseBackingMap(String sName, Map map);
    }