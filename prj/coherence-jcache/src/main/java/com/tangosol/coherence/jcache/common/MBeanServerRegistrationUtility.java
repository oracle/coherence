/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import com.tangosol.coherence.jcache.AbstractCoherenceBasedCache;
import com.tangosol.coherence.jcache.CoherenceBasedCacheManager;

import com.tangosol.net.management.MBeanHelper;

import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheException;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * A convenience class for registering CacheStatisticsMBeans with an MBeanServer.
 *
 * @author jf  2013.10.24
 * @since Coherence 12.1.3
 */
public final class MBeanServerRegistrationUtility
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Singleton class. Prevent construction of a {@link MBeanServerRegistrationUtility}
     *
     */
    private MBeanServerRegistrationUtility()
        {
        // prevent construction
        }

    // ----- MBeanServerRegistrationUtility methods -------------------------

    /**
     * Utility method for registering CacheStatistics with the platform MBeanServer
     *
     * @param cache the cache to register
     */
    static public void registerCacheObject(AbstractCoherenceBasedCache cache, ObjectNameType objectNameType)
        {
        // these can change during runtime, so always look it up
        MBeanServer mBeanServer          = MBeanHelper.findMBeanServer();
        ObjectName  registeredObjectName = calculateObjectName(cache, objectNameType);

        try
            {
            switch (objectNameType)
                {
                case Configuration:
                    if (!isRegistered(cache, objectNameType))
                        {
                        mBeanServer.registerMBean(cache.getMBean(), registeredObjectName);
                        }

                    break;

                case Statistics:
                    if (!isRegistered(cache, objectNameType))
                        {
                        mBeanServer.registerMBean(cache.getStatistics(), registeredObjectName);
                        }

                    break;

                default:
                    throw new UnsupportedOperationException("registerCacheObject ObjectNameType=" + objectNameType);
                }
            }
        catch (Exception e)
            {
            throw new CacheException("Error registering cache MXBeans for CacheManager " + registeredObjectName
                                     + " . Error was " + e.getMessage(), e);
            }
        }

    /**
     * Checks whether an ObjectName is already registered.
     *
     * @throws javax.cache.CacheException - all exceptions are wrapped in CacheException
     */
    static public boolean isRegistered(Cache cache, ObjectNameType objectNameType)
        {
        Set<ObjectName> registeredObjectNames;
        MBeanServer     mBeanServer = MBeanHelper.findMBeanServer();

        ObjectName      objectName  = calculateObjectName(cache, objectNameType);

        registeredObjectNames = mBeanServer.queryNames(objectName, null);

        return !registeredObjectNames.isEmpty();
        }

    /**
     * Removes registered JCache MBean for a Cache
     *
     * @param cache           remove registered JCache MBean for this cache
     * @param objectNameType  JCache MBean type {@link ObjectNameType}
     *
     * @throws javax.cache.CacheException - all exceptions are wrapped in CacheException
     */
    static public void unregisterCacheObject(Cache cache, ObjectNameType objectNameType)
        {
        Set<ObjectName> registeredObjectNames;
        MBeanServer     mBeanServer = MBeanHelper.findMBeanServer();

        ObjectName      objectName  = calculateObjectName(cache, objectNameType);

        registeredObjectNames = mBeanServer.queryNames(objectName, null);

        // should just be one
        for (ObjectName registeredObjectName : registeredObjectNames)
            {
            try
                {
                mBeanServer.unregisterMBean(registeredObjectName);
                }
            catch (Exception e)
                {
                throw new CacheException("Error unregistering object instance " + registeredObjectName
                                         + " . Error was " + e.getMessage(), e);
                }
            }
        }

    /**
     * Removes registered JCache MBean for a Cache
     *
     * @param mgr             Coherence JCache CacheManager of JCache cache MBean to be unregistered
     * @param id              JCache cache identifier of JCache cache MBean to be unregistered
     * @param objectNameType  JCache MBean type {@link ObjectNameType} to be unregistered
     *
     * @throws javax.cache.CacheException - all exceptions are wrapped in CacheException
     */
    static public void unregisterCacheObject(CoherenceBasedCacheManager mgr, JCacheIdentifier id, ObjectNameType objectNameType)
        {
        Set<ObjectName> registeredObjectNames;
        MBeanServer     mBeanServer = MBeanHelper.findMBeanServer();

        ObjectName      objectName  = calculateObjectName(mgr, id, objectNameType);

        registeredObjectNames = mBeanServer.queryNames(objectName, null);

        // should just be one
        for (ObjectName registeredObjectName : registeredObjectNames)
            {
            try
                {
                mBeanServer.unregisterMBean(registeredObjectName);
                }
            catch (Exception e)
                {
                throw new CacheException("Error unregistering object instance " + registeredObjectName
                        + " . Error was " + e.getMessage(), e);
                }
            }
        }

    // ----- helpers ---------------------------------------------------------

    /**
     * Creates an object name using the scheme
     * "javax.cache:type=Cache&lt;Statistics|Configuration&gt;,CacheManager=&lt;cacheManagerName&gt;,Cache=&lt;cacheName&gt;"
     * <p>
     * MultiTenancy support:
     * "javax.cache:type=Cache&lt;Statistics|Configuration&gt;,CacheManager=&lt;cacheManagerName&gt;,Cache=&lt;cacheName&gt;domainPartition=&lt;domainPartition&gt;"
     *
     *
     * @param cache the JCache that this MBean ObjectName will represent
     * @param objectNameType whether a Statistics or Configuration MBean type
     *
     * @return computed MBean object name for <code>cache</code> with <code>objectNameType</code>
     */
    public static ObjectName calculateObjectName(Cache cache, ObjectNameType objectNameType)
        {
        StringBuilder sb = new StringBuilder();

        try
            {
            sb.append("javax.cache:type=Cache").append(objectNameType).append(",CacheManager=").append(mbeanSafe(
                cache.getCacheManager().getURI().toString())).append(",Cache=").append(mbeanSafe(cache.getName()));

            String domainPartition =
                cache.getCacheManager().unwrap(CoherenceBasedCacheManager.class).getDomainPartition();

            if (domainPartition != null)
                {
                sb.append(",domainPartition=").append(mbeanSafe(domainPartition));
                }

            return new ObjectName(sb.toString());
            }
        catch (MalformedObjectNameException e)
            {
            throw new CacheException(e);
            }
        }

    /**
     * Creates an object name using the scheme
     * "javax.cache:type=Cache&lt;Statistics|Configuration&gt;,CacheManager=&lt;cacheManagerName&gt;,Cache=&lt;cacheName&gt;"
     * <p>
     * MultiTenancy support:
     * "javax.cache:type=Cache&lt;Statistics|Configuration&gt;,CacheManager=&lt;cacheManagerName&gt;,Cache=&lt;cacheName&gt;domainPartition=&lt;domainPartition&gt;"
     *
     *
     * @param mgr   CoherenceBasedCacheManager
     * @param id    the JCache Identier that this MBean ObjectName will represent
     * @param objectNameType whether a Statistics or Configuration MBean type
     *
     * @return computed MBean object name for <code>cache</code> with <code>objectNameType</code>
     */
    public static ObjectName calculateObjectName(CoherenceBasedCacheManager mgr, JCacheIdentifier id, ObjectNameType objectNameType)
        {
        StringBuilder sb = new StringBuilder();

        try
            {
            sb.append("javax.cache:type=Cache").append(objectNameType).append(",CacheManager=").append(mbeanSafe(
                    id.getCacheManagerURI())).append(",Cache=").append(mbeanSafe(id.getName()));

            String domainPartition =
                    mgr.unwrap(CoherenceBasedCacheManager.class).getDomainPartition();

            if (domainPartition != null)
                {
                sb.append(",domainPartition=").append(mbeanSafe(domainPartition));
                }

            return new ObjectName(sb.toString());
            }
        catch (MalformedObjectNameException e)
            {
            throw new CacheException(e);
            }
        }


    /**
     * Filter out invalid ObjectName characters from string.
     *
     * @param string input string
     * @return A valid JMX ObjectName attribute value.
     */
    private static String mbeanSafe(String string)
        {
        return string == null ? "" : string.replaceAll(":|=|\n", ".");
        }

    /**
     * Get name of MBeanServer <code>mbs</code>
     *
     * @param mbs the MBeanServer to get
     *
     * @return name of MBeanServer
     */
    private static String getMBeanServerName(MBeanServer mbs)
        {
        try
            {
            return (String) mbs.getAttribute(MBeanServerDelegate.DELEGATE_NAME, "MBeanServerId");
            }
        catch (JMException e)
            {
            return null;
            }
        }

    // ----- inner class ----------------------------------------------------

    /**
     * The type of registered Object
     */
    public enum ObjectNameType
        {
        /**
         * Cache Statistics
         */
        Statistics,

        /**
         * Cache Configuration
         */
        Configuration
        }
    }
