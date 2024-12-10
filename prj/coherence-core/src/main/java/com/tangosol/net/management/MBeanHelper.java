/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.management;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.net.TcpSocketProvider;

import com.tangosol.internal.net.management.DefaultGatewayDependencies;
import com.tangosol.internal.net.management.GatewayDependencies;
import com.tangosol.internal.net.management.LegacyXmlGatewayHelper;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;
import com.tangosol.internal.net.topic.impl.paged.management.PagedTopicModel;
import com.tangosol.internal.net.topic.impl.paged.management.SubscriberGroupModel;
import com.tangosol.internal.net.topic.impl.paged.management.SubscriberModel;
import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.statistics.PagedTopicStatistics;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.InetAddressHelper;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;

import com.tangosol.net.PagedTopicService;
import com.tangosol.net.Service;
import com.tangosol.net.TopicService;

import com.tangosol.net.cache.ContinuousQueryCache;
import com.tangosol.net.management.annotation.Description;
import com.tangosol.net.management.annotation.Notification;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

import java.lang.annotation.Annotation;

import java.lang.reflect.Method;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.rmi.registry.LocateRegistry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.DynamicMBean;
import javax.management.MBeanNotificationInfo;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.QueryExp;

import javax.management.openmbean.SimpleType;

import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.rmi.server.RMISocketFactory;


/**
* Helper class providing various functionality related to aggregation of
* attributes and methods exposed by Coherence JMX framework MBeans.
*
* @since Coherence 3.3
* @author gg 2007/01/02
*/
@SuppressWarnings("rawtypes")
public abstract class MBeanHelper
        extends Base
    {
    /**
    * Return the default domain name as configured in the Coherence
    * operational configuration descriptor ("default-domain-name" element).
    *
    * @return the default domain name
    */
    public static String getDefaultDomain()
        {
        return ensureGatewayDependencies().getDefaultDomain();
        }

    /**
     *Checks whether "write" operations are allowed.
     *
     * @throws SecurityException if the model is "read-only"
     */
    public static void checkReadOnly(String sOperation)
       {
       if (ensureGatewayDependencies().isReadOnly())
           {
           throw new SecurityException("Operation is not allowed: " + sOperation);
           }
       }

    /**
    * Find an MBeanServer that Coherence MBeans are registered with.
    *
    * @return an existing or a new MBeanServer with the default
    *          domain name as configured in the Coherence operational
    *          configuration descriptor
    */
    public static MBeanServer findMBeanServer()
        {
        return findMBeanServer(null);
        }

    /**
    * Find an MBeanServer that has the specified default domain name. If the
    * domain name is not specified, any existing MBeanServer is chosen.
    *
    * @param sDefaultDomain  the default domain name
    *
    * @return an existing or a new MBeanServer with the specified default
    *          domain name
    */
    public static MBeanServer findMBeanServer(String sDefaultDomain)
        {
        return findMBeanServer(sDefaultDomain, ensureGatewayDependencies());
        }

    /**
    * Find an MBeanServer that has the specified default domain name. If the
    * domain name is not specified, any existing MBeanServer is chosen.
    *
    * @param sDefaultDomain  the default domain name
    * @param deps            the {@link GatewayDependencies} containing the
    *                        management configuration to use
    *
    * @return an existing or a new MBeanServer with the specified default
    *          domain name
    */
    public static MBeanServer findMBeanServer(String sDefaultDomain, GatewayDependencies deps)
        {
        try
            {
            if (sDefaultDomain == null || sDefaultDomain.isEmpty())
                {
                sDefaultDomain = deps.getDefaultDomain();
                }

            // allow custom
            MBeanServerFinder finder = deps.getMBeanServerFinder();
            if (finder != null)
                {
                MBeanServer server = finder.findMBeanServer(sDefaultDomain);
                if (server != null)
                    {
                    return server;
                    }
                }

            // check among existing MBeanServers
            for (MBeanServer server : MBeanServerFactory.findMBeanServer(null))
                {
                if (sDefaultDomain == null || sDefaultDomain.length() == 0 ||
                        server.getDefaultDomain().equals(sDefaultDomain))
                    {
                    return server;
                    }
                }

            // try the PlatformMBeanServer (JDK 1.5 specific)
            try
                {
                Class clzFactory = Class.forName("java.lang.management.ManagementFactory");
                MBeanServer server = (MBeanServer) ClassHelper.invokeStatic(clzFactory,
                    "getPlatformMBeanServer", ClassHelper.VOID);
                if (sDefaultDomain == null || sDefaultDomain.length() == 0 ||
                        server.getDefaultDomain().equals(sDefaultDomain))
                    {
                    return server;
                    }
                }
            catch (Exception eIgnore) {}

            return MBeanServerFactory.createMBeanServer(sDefaultDomain);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Failed to locate the server");
            }
        }

    /**
    * Find the JMXServiceURL for the MBeanConnector used by the
    * Coherence JMX framework.
    *
    * @param sDefaultDomain  the default domain name
    * @param deps            the {@link GatewayDependencies} containing the
    *                        management configuration to use
    *
    * @return JMXServiceUrl for the MBeanConnector or null if no Connector is
    *         running.
    */
    public static JMXServiceURL findJMXServiceUrl(String sDefaultDomain, GatewayDependencies deps)
        {
        if (sDefaultDomain == null || sDefaultDomain.isEmpty())
            {
            sDefaultDomain = deps.getDefaultDomain();
            }

        MBeanServerFinder finder = deps.getMBeanServerFinder();
        if (finder != null)
            {
            return finder.findJMXServiceUrl(sDefaultDomain);
            }
        return null;
        }

    /**
    * Find all MBeans matching to the specified query at a local MBeanServer and
    * register them with the specified Registry.
    * <p>
    * Note: the MBeanServer that the query runs against is not necessarily the
    * one used by the Registry.
    *
    * @param sMBeanServerDomain  the default domain of the MBeanServer where the
    *                            query should be executed.  If this value is empty
    *                            or null the Coherence default domain is used
    * @param sQuery              a JMX query string that will be used to find
    *                            the MBeans
    * @param sPrefix             a target location to prepend to converted MBean names
    * @param registry            a Registry to register the JMX query results with
    */
    public static void registerQueryMBeans(String sMBeanServerDomain,
            String sQuery, String sPrefix, Registry registry)
        {
        if (sPrefix.length() > 0)
            {
            sPrefix += ',';
            }

        try
            {
            ObjectName onameQuery = new ObjectName(sQuery);

            MBeanServer mbs = findMBeanServer(sMBeanServerDomain);

            Set<ObjectName> setNames = mbs.queryNames(onameQuery, null);
            for (ObjectName oname : setNames)
                {
                String sSourceDomain = oname.getDomain();
                String sSourceName   = oname.getKeyPropertyListString();
                String sTargetName   = sPrefix + "Domain=" + sSourceDomain + ','
                        + sSourceName.replaceFirst("type=", "subType=");

                registry.register(registry.ensureGlobalName(sTargetName),
                        new MBeanReference(oname, mbs));
                }
            }
        catch (MalformedObjectNameException e)
            {
            // the exception provides all necessary information about invalid config
            // and [we assume] makes it quite clear that the definition is ignored
            err("Ignoring failed registration request: " + e +
                "; " + getStackTrace(e));
            }
        }

    /**
    * Register the specified NamedCache with the cluster registry.
    *
    * @param cache     the NamedCache object to register
    * @param sContext  the cache context (tier)
    */
    public static void registerCacheMBean(NamedCache cache, String sContext)
        {
        registerCacheMBean(cache.getCacheService(), cache.getCacheName(), sContext, cache);
        }

    /**
    * Register the specified map with the cluster registry.
    *
    * @param service     the CacheService that the cache belongs to
    * @param sCacheName  the cache name
    * @param sContext    the cache context (tier)
    * @param map         the map object to register
    */
    public static void registerCacheMBean(CacheService service, String sCacheName,
                            String sContext, Map map)
        {
        try
            {
            Cluster  cluster  = service.getCluster();
            Registry registry = cluster.getManagement();
            if (registry != null)
                {
                String sName = Registry.CACHE_TYPE +
                    "," + Registry.KEY_SERVICE + service.getInfo().getServiceName() +
                    ",name="    + sCacheName;

                sName = registry.ensureGlobalName(sName);
                sName = sName + (sContext == null ? "" : "," + sContext);

                registry.register(sName, map);
                }
            }
        catch (Throwable e)
            {
            Logger.warn("Failed to register cache \"" + sCacheName + "\"; " + e);
            }
        }

    /**
     * Register the specified view cache with the cluster registry.
     *
     * @param cache  the ContinuousQueryCache to register
     */
    public static void registerViewMBean(ContinuousQueryCache cache)
        {
        registerViewMBean(cache.getCacheService(), cache);
        }

    /**
     * Register the specified view cache with the cluster registry.
     *
     * @param service     the CacheService that the cache belongs to
     * @param cache       the cache object to register
     */
    public static void registerViewMBean(CacheService service, ContinuousQueryCache cache)
        {
        try
            {
            Cluster  cluster  = service.getCluster();
            Registry registry = cluster.getManagement();
            if (registry != null)
                {
                String sName = Registry.VIEW_TYPE +
                               "," + Registry.KEY_SERVICE + service.getInfo().getServiceName() +
                               ",name="    + cache.getCacheName();

                sName = registry.ensureGlobalName(sName);

                ViewMBean viewMBean = new ViewMBeanImpl(cache);
                registry.register(sName, new AnnotatedStandardEmitterMBean(viewMBean, ViewMBean.class));
                }
            }
        catch (Throwable e)
            {
            Logger.warn("Failed to register view \"" + cache.getCacheName() + "\"; " + e);
            }
        }

    /**
     * Unregister all managed objects related to the given view cache and
     * context from the cluster registry.
     *
     * @param cache     the cache
     */
    public static void unregisterViewMBean(NamedCache cache)
        {
        CacheService service = cache.getCacheService();
        unregisterViewMBean(service.getCluster(), service.getInfo().getServiceName(), cache.getCacheName());
        }

    /**
     * Unregister all managed objects related to the given view cache and
     * context from the cluster registry.
     *
     * @param cluster       the Cluster object
     * @param sServiceName  the CacheService that the cache belongs to
     * @param sCacheName    the cache name
     */
    public static void unregisterViewMBean(Cluster cluster, String sServiceName, String sCacheName)
        {
        try
            {
            Registry registry = cluster.getManagement();
            if (registry != null)
                {
                String sName = Registry.VIEW_TYPE + "," + Registry.KEY_SERVICE + sServiceName +
                               ",name=" + sCacheName;

                sName = registry.ensureGlobalName(sName);
                registry.unregister(sName);
                }
            }
        catch (Throwable e)
            {
            }
        }

     /**
     * Unregister all managed objects that are related to the specified cache
     * from the registry.
     *
     * @param service     the CacheService that the cache belongs to
     * @param sCacheName  the cache name
     */
    public static void unregisterCacheMBean(CacheService service, String sCacheName)
        {
        unregisterCacheMBean(service, sCacheName, null);
        }

    /**
    * Unregister all managed objects that are related to the specified cache
    * from the registry.
    *
    * @param sServiceName  the CacheService name
    * @param sCacheName    the cache name
    * @param sContext      the cache context (tier)
    */
    public static void unregisterCacheMBean(String sServiceName, String sCacheName, String sContext)
        {
        unregisterCacheMBean(CacheFactory.getCluster(), sServiceName, sCacheName, sContext);
        }

     /**
     * Unregister all managed objects that are related to the specified cache
     * from the registry.
     *
     * @param service     the CacheService that the cache belongs to
     * @param sCacheName  the cache name
     * @param sContext    the cache context (tier)
     */
    public static void unregisterCacheMBean(CacheService service, String sCacheName, String sContext)
        {
        unregisterCacheMBean(service.getCluster(), service.getInfo().getServiceName(), sCacheName, sContext);
        }

    /**
    * Unregister all managed objects that are related to the specified cache
    * from the registry.
    *
    * @param cluster       the Cluster object
    * @param sServiceName  the CacheService that the cache belongs to
    * @param sCacheName    the cache name
    * @param sContext      the cache context (tier)
    */
    public static void unregisterCacheMBean(Cluster cluster, String sServiceName, String sCacheName, String sContext)
        {
        try
            {
            Registry registry = cluster.getManagement();
            if (registry != null)
                {
                String sName = Registry.CACHE_TYPE + "," + Registry.KEY_SERVICE + sServiceName +
                    ",name=" + sCacheName;

                sName = registry.ensureGlobalName(sName);
                sName = sName + (sContext == null ? "" : "," + sContext);
                registry.unregister(sName);
                }
            }
        catch (Throwable e) {}
        }

    /**
    * Unregister all managed objects related to the given cache name and
    * context from the cluster registry.
    *
    * @param sCacheName  the cache name
    * @param sContext    the cache context (tier)
    */
    public static void unregisterCacheMBean(String sCacheName, String sContext)
        {
        try
            {
            Cluster  cluster  = CacheFactory.getCluster();
            Registry registry = cluster.getManagement();
            if (registry != null)
                {
                Member member = cluster.getLocalMember();
                // use "*" in lieu of service name
                String sPattern = Registry.CACHE_TYPE +
                    ",name="    + sCacheName +
                    (member == null ? "" : ",nodeId=" + member.getId()) +
                    (sContext == null ? "" : (',' + sContext));
                registry.unregister(sPattern);
                }
            }
        catch (Throwable e) {}
        }

    /**
    * Unregister all managed objects related to the given cache name and
    * context from the cluster registry.
    *
    * @param cache     the cache
    * @param sContext  the cache context (tier)
    */
    public static void unregisterCacheMBean(NamedCache cache, String sContext)
        {
        unregisterCacheMBean(cache.getCacheService(), cache.getCacheName(), sContext);
        }

    /**
    * Register the specified PagedTopic with the cluster registry.
    *
    * @param service     the PagedTopic Service that the topic belongs to
    * @param sTopicName  the name of the topic to register
    */
    public static void registerPagedTopicMBean(PagedTopicService service, String sTopicName)
        {
        try
            {
            Cluster  cluster      = service.getCluster();
            Registry registry     = cluster.getManagement();
            String   sServiceName = service.getInfo().getServiceName();
            String   sMBeanName   = registry.ensureGlobalName(getTopicMBeanName(sServiceName, sTopicName));
            registry.register(sMBeanName, new PagedTopicModel(service, sTopicName));
            }
        catch (Throwable e)
            {
            Logger.warn("Failed to register topic \"" + sTopicName + "\"", e);
            }
        }

    /**
    * Unregister all managed objects related to the given topic name
    * from the cluster registry.
    *
     * @param service  the topic service
     * @param topic    the topic
    */
    public static void unregisterPagedTopicMBean(Service service, NamedTopic<?> topic)
        {
        unregisterPagedTopicMBean(service, topic.getName());
        }

    /**
    * Unregister all managed objects related to the given topic name
    * from the cluster registry.
    *
     * @param service     the topic service
     * @param sTopicName  the topic name
    */
    public static void unregisterPagedTopicMBean(Service service, String sTopicName)
        {
        try
            {
            Cluster  cluster      = service.getCluster();
            Registry registry     = cluster.getManagement();
            String   sServiceName = service.getInfo().getServiceName();
            registry.unregister(registry.ensureGlobalName(getTopicMBeanName(sServiceName, sTopicName)));
            unregisterSubscriberGroupMBean(null, sTopicName, service);
            }
        catch (Throwable ignored) {}
        }

    /**
     * Return the MBean name for the topic.
     * <p/>
     * The name returned will be the generic name without the member's
     * {@code nodeId} key.
     *
     * @param topic  the topic to obtain the MBean name for
     *
     * @return the MBean name for the topic
     */
    public static String getTopicMBeanName(NamedTopic<?> topic)
        {
        return getTopicMBeanName(topic.getTopicService().getInfo().getServiceName(), topic.getName());
        }

    /**
     * Return the MBean name pattern that will match all topic MBeans for the service.
     * <p/>
     * The name returned will be the generic name without the member's
     * {@code nodeId} key.
     *
     * @param service  the topic to service
     *
     * @return the MBean name for the topic
     */
    public static String getTopicMBeanPattern(TopicService service)
        {
        return getTopicMBeanName(service.getInfo().getServiceName(), "*");
        }

    /**
     * Return the MBean name pattern that will match all topic MBeans.
     * <p/>
     * The name returned will be the generic name without the member's
     * {@code nodeId} key.
     *
     * @return the MBean name for the topic
     */
    public static String getTopicMBeanPattern()
        {
        return getTopicMBeanName("*", "*");
        }

    private static String getTopicMBeanName(String sService, String sTopic)
        {
        return Registry.PAGED_TOPIC_TYPE +
                "," + Registry.KEY_SERVICE + sService +
                "," + Registry.KEY_NAME + sTopic;
        }

    /**
    * Register the specified PagedTopic subscriber group with the cluster registry.
    *
    * @param service      the topic Service that the topic belongs to
    * @param subscription the {@link PagedTopicSubscription subscription}
    */
    public static void registerSubscriberGroupMBean(PagedTopicService service, PagedTopicSubscription subscription)
        {
        SubscriberGroupId id = subscription.getSubscriberGroupId();

        if (id.isAnonymous())
            {
            // only register durable subscriber groups
            return;
            }

        String sTopicName = subscription.getTopicName();
        try
            {
            Cluster  cluster  = service.getCluster();
            Registry registry = cluster.getManagement();
            if (registry != null)
                {
                String sName = registry.ensureGlobalName(getSubscriberGroupMBeanName(id, sTopicName, service));
                if (!registry.isRegistered(sName))
                    {
                    s_lockTopicSubscriberGroups.lock();
                    try
                        {
                        if (!registry.isRegistered(sName))
                            {
                            PagedTopicStatistics statistics = service.getTopicStatistics(sTopicName);
                            Filter<?> filter = subscription.getFilter();
                            ValueExtractor<?, ?> extractor = subscription.getConverter();
                            registry.register(sName, new SubscriberGroupModel(statistics, id, filter, extractor, service));
                            }
                        }
                    finally
                        {
                        s_lockTopicSubscriberGroups.unlock();
                        }
                    }
                }
            }
        catch (Throwable e)
            {
            Logger.warn("Failed to register subscriber group \"" + id.getGroupName()
                        + "\" in topic \"" + sTopicName + "\"", e);
            }
        }

    /**
    * Unregister all managed objects related to the given topic subscriber group name
    * from the cluster registry.
    *
    * @param service      the topic Service that the topic belongs to
    * @param subscription the {@link PagedTopicSubscription subscription}
    */
    public static void unregisterSubscriberGroupMBean(PagedTopicService service, PagedTopicSubscription subscription)
        {
        String            sTopicName = subscription.getTopicName();
        SubscriberGroupId groupId    = subscription.getSubscriberGroupId();
        unregisterSubscriberGroupMBean(groupId, sTopicName, service);
        }

    /**
    * Unregister all managed objects related to the given topic subscriber group name
    * from the cluster registry.
    *
    * @param id            the subscriber group {@link SubscriberGroupId id} or {@code null}
     *                     to unregister all groups for the topic
    * @param sTopicName    the topic name
    * @param service       the topic service
    */
    private static void unregisterSubscriberGroupMBean(SubscriberGroupId id, String sTopicName, Service service)
        {
        try
            {
            Cluster  cluster  = service.getCluster();
            Registry registry = cluster.getManagement();
            String   sName    = registry.ensureGlobalName(getSubscriberGroupMBeanName(id, sTopicName, service));
            registry.unregister(sName);
            }
        catch (Throwable ignored) {}
        }

    /**
     * Return the MBean name for a subscriber group.
     * <p/>
     * The name returned will be the generic name without the member's
     * {@code nodeId} key.
     *
     * @param id          the {@link SubscriberGroupId}
     * @param sTopicName  the name of the topic
     * @param service     the service owning the topic
     *
     * @return the MBean name for a subscriber group
     */
    public static String getSubscriberGroupMBeanName(SubscriberGroupId id, String sTopicName, Service service)
        {
        String sGroupName = id == null ? "*" : id.getGroupName();
        return Registry.SUBSCRIBER_GROUP_TYPE +
                "," + Registry.KEY_SERVICE + service.getInfo().getServiceName() +
                "," + Registry.KEY_TOPIC + sTopicName + "," + Registry.KEY_NAME + sGroupName;
        }

    /**
    * Register the specified PagedTopic subscriber with the cluster registry.
    *
    * @param subscriber  the topic subscriber
    */
    public static void registerSubscriberMBean(PagedTopicSubscriber<?> subscriber)
        {
        NamedTopic<?> topic = subscriber.getNamedTopic();
        try
            {
            Service  service  = topic.getService();
            Cluster  cluster  = service.getCluster();
            Registry registry = cluster.getManagement();

            if (registry != null)
                {
                String sName = registry.ensureGlobalName(getSubscriberMBeanName(subscriber));
                if (!registry.isRegistered(sName))
                    {
                    registry.register(sName, new SubscriberModel(subscriber));
                    }
                }
            }
        catch (Throwable e)
            {
            Logger.warn("Failed to register subscriber \"" + subscriber.getId()
                        + "\" in topic \"" + topic.getName() + "\"", e);
            }
        }

    /**
    * Unregister all managed objects related to the given topic subscriber
    * from the cluster registry.
    *
    * @param subscriber  the topic subscriber
    */
    public static void unregisterSubscriberMBean(PagedTopicSubscriber<?> subscriber)
        {
        try
            {
            Service  service  = subscriber.getNamedTopic().getService();
            Cluster  cluster  = service.getCluster();
            Registry registry = cluster.getManagement();
            String   sName    = registry.ensureGlobalName(getSubscriberMBeanName(subscriber));
            registry.unregister(sName);
            }
        catch (Throwable ignored) {}
        }

    /**
     * Return the MBean name for a {@link PagedTopicSubscriber}.
     *
     * @param subscriber  the {@link PagedTopicSubscriber}
     *
     * @return the MBean name for a {@link PagedTopicSubscriber}
     */
    public static String getSubscriberMBeanName(PagedTopicSubscriber subscriber)
        {
        NamedTopic<?> topic      = subscriber.getNamedTopic();
        Service       service    = topic.getService();
        long          nNodeId    = service.getCluster().getLocalMember().getId();
        String        sTopicName = topic.getName();
        String        sGroup     = subscriber.getSubscriberGroupId().getGroupName();
        String        subType    = subscriber.isAnonymous()
                                        ? Registry.SUBSCRIBER_ANONYMOUS_TYPE
                                        : Registry.SUBSCRIBER_DURABLE_TYPE;
        long          nId        = subscriber.getId();
        String        sNode      = Registry.KEY_NODE_ID + nNodeId + ",";
        String        sId        = subscriber.isAnonymous()
                                        ? sNode + Registry.KEY_ID + nId
                                        : Registry.KEY_TOPIC_GROUP + sGroup + "," + sNode + Registry.KEY_ID + nId;

        return Registry.SUBSCRIBER_TYPE +
            "," + Registry.KEY_SERVICE + service.getInfo().getServiceName() +
            "," + Registry.KEY_TOPIC + sTopicName + "," + subType + "," + sId;
        }

    /**
     * Return the MBean name pattern for all subscribers in a topic.
     *
     * @param topic  the {@link NamedTopic}
     *
     * @return the MBean name pattern for all subscribers in a topic
     */
    public static String getSubscriberMBeanPattern(NamedTopic<?> topic, boolean fLocalOnly)
        {
        Service service    = topic.getService();
        String  sTopicName = topic.getName();
        int     nNodeId    = service.getCluster().getLocalMember().getId();
        String  sNode      = fLocalOnly ? "," + Registry.KEY_NODE_ID + nNodeId : "";

        return Registry.SUBSCRIBER_TYPE +
            "," + Registry.KEY_SERVICE + service.getInfo().getServiceName() +
            "," + Registry.KEY_TOPIC + sTopicName + sNode + ",*";
        }

    /**
     * Return the MBean name pattern for all subscribers in a
     * subscriber group for topic.
     *
     * @param topic  the {@link NamedTopic}
     *
     * @return the MBean name pattern for all subscribers in a
     *         subscriber group for a topic
     */
    public static String getSubscriberMBeanPattern(NamedTopic<?> topic, SubscriberGroupId groupId, boolean fLocalOnly)
        {
        Service service    = topic.getService();
        String  sTopicName = topic.getName();
        int     nNodeId    = service.getCluster().getLocalMember().getId();
        String  sNode      = fLocalOnly ? "," + Registry.KEY_NODE_ID + nNodeId : Registry.KEY_NODE_ID + "*";
        String  sGroup     = groupId.getGroupName();
        String  subType    = groupId.isAnonymous()
                                    ? Registry.SUBSCRIBER_ANONYMOUS_TYPE
                                    : Registry.SUBSCRIBER_DURABLE_TYPE;
        String  sId        = groupId.isAnonymous()
                                    ? sNode + "," + Registry.KEY_ID + sGroup
                                    : Registry.KEY_TOPIC_GROUP + sGroup + "," + sNode + "," + Registry.KEY_ID + "*";

        return Registry.SUBSCRIBER_TYPE +
            "," + Registry.KEY_SERVICE + service.getInfo().getServiceName() +
            "," + Registry.KEY_TOPIC + sTopicName + "," + subType + "," + sId;
        }

    /**
    * Ensure that there is an instance of a local MBean of the specified type
    * registered with the default MBeansServer.
    *
    * @param sName  an MBean name
    *
    * @return the MBeanReference for the registered MBean
    */
    public static MBeanReference ensureSingletonMBean(String sName)
        {
        try
            {
            MBeanServer server = findMBeanServer();
            ObjectName  name   = new ObjectName("Coherence:type=" + sName);
            if (!server.isRegistered(name))
                {
                Object oBean = Class.forName(MANAGEABLE + sName + "MBean").
                    newInstance();
                server.registerMBean(oBean, name);
                }
            return new MBeanReference(name, server);
            }
        catch (Throwable e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * Create a DynamicMBean driven by maps containing attribute descriptions and
    * values.
    *
    * @param mapDescr  Map&lt;String, String&gt; keyed by the attribute names with
    *                  values being attribute descriptions
    * @param mapValue  Map&lt;String, Object&gt; keyed by the attribute names with
    *                  values being attribute values
    *
    * @return a DynamicMBean
    */
     public static DynamicMBean createMapAdapterMbean(
            Map<String,String> mapDescr, Map<String, ?> mapValue)
         {
         try
             {
            DynamicMBean bean = (DynamicMBean)
                Class.forName(MANAGEABLE + "MapAdapter").newInstance();
            ClassHelper.invoke(bean, "_initialize", new Object[] {mapDescr, mapValue});
            return bean;
            }
        catch (Throwable e)
            {
            throw Base.ensureRuntimeException(e);
            }
         }

    /**
    * Start a {@link JMXConnectorServer}.  This method is used to expose the
    * specified MBeanServer to external agents (such as JConsole) using RMI.
    * <p>
    * This method also allows for the RMI functionality to emulate the
    * JConsole remote connection security.
    * <p>
    * For the list of relevant system properties please see:
    * <a href="http://download.oracle.com/javase/6/docs/technotes/guides/management/agent.html">
    * Java 1.6 Agent Documentation</a>.
    *
    * @param sAddr     host to bind to
    * @param nRegPort  port used for the JMX RMI registry
    * @param nConPort  port used for the JMX RMI connection
    * @param mbs       {@link MBeanServer} that contains Coherence MBeans
    * @param mapEnv    a set of attributes to control the new connector server's
    *                  behavior
    *
    * @return  a JMXConnectorServer that has been started
    *
    * @see JMXConnectorServerFactory
    */
    public static JMXConnectorServer startRmiConnector(String sAddr, int nRegPort,
            int nConPort, MBeanServer mbs, Map mapEnv)
        {
        String sPriorRmiHostname = System.getProperty("java.rmi.server.hostname");

        try
            {
            String sHost = sAddr == null ? null : InetAddressHelper.getLocalAddress(sAddr).getHostAddress(); // translate CIDR format

            // get the System Properties to copy to the JMX Connector
            // NOTE: atn and ssl default to false unless any atn/access/reg are setup in which case they default to true
            String sAuthFile   = System.getProperty(
                                 "com.sun.management.jmxremote.password.file");
            String sAccessFile = System.getProperty(
                                 "com.sun.management.jmxremote.access.file");
            String sAuth       = System.getProperty(
                                 "com.sun.management.jmxremote.authenticate",
                                    (sAuthFile != null || sAccessFile != null || nRegPort != 0 ||
                                    (mapEnv != null && "true".equals(mapEnv.get("com.oracle.coherence.tcmp.ssl"))))
                                            ? "true" : "false");
            String sSSL        = System.getProperty(
                                 "com.sun.management.jmxremote.ssl", sAuth);

            if (sAuth.equalsIgnoreCase("true"))
                {
                String sJavaHome = System.getProperty("java.home");

                if (sAuthFile == null)
                    {
                    Path pathPreJava11 = Paths.get(sJavaHome, "lib", "management", "jmxremote.password");
                    sAuthFile = pathPreJava11.toFile().exists()
                                ? pathPreJava11.toString()
                                : Paths.get(sJavaHome, "conf", "management", "jmxremote.password").toString(); // Java 11
                    }
                if (sAccessFile == null)
                    {
                    Path pathPreJava11 = Paths.get(sJavaHome, "lib", "management", "jmxremote.access");
                    sAccessFile = pathPreJava11.toFile().exists()
                                  ? pathPreJava11.toString()
                                  : Paths.get(sJavaHome, "conf", "management", "jmxremote.access").toString(); // Java 11
                    }
                }

            if (mapEnv == null)
                {
                mapEnv = new HashMap();
                }

            mapEnv.put("jmx.remote.x.daemon", "true"); // compensation for COH-14091

            if (sAuthFile != null
                    && mapEnv.get("jmx.remote.x.password.file") == null)
                {
                mapEnv.put("jmx.remote.x.password.file", sAuthFile);
                }

            if (sAccessFile != null
                  && mapEnv.get("jmx.remote.x.access.file") == null)
                {
                mapEnv.put("jmx.remote.x.access.file", sAccessFile);
                }

            if (sSSL.equalsIgnoreCase("true"))
                {
                try
                    {
                    if (mapEnv.get("jmx.remote.rmi.client.socket.factory") == null)
                        {
                        mapEnv.put("jmx.remote.rmi.client.socket.factory",
                            Class.forName("javax.rmi.ssl.SslRMIClientSocketFactory")
                                .newInstance());
                        }
                    if (mapEnv.get("jmx.remote.rmi.server.socket.factory") == null)
                        {
                        mapEnv.put("jmx.remote.rmi.server.socket.factory",
                            Class.forName("javax.rmi.ssl.SslRMIServerSocketFactory")
                                .newInstance());
                        }
                    }
                catch (ClassNotFoundException e)
                    {
                    String sMsg = "JMXConnectorServer not started. SSL security requires" +
                        " the Java Dynamic Management Kit or Java 1.5.";
                    throw ensureRuntimeException(e, sMsg);
                    }
                }
            else if (!InetAddressHelper.isAnyLocalAddress(sHost))
                {
                // RMI's socket factory API is only capable of creating servers which listen on wildcard
                // we use a custom factory to force ourselves onto a different IP if needed
                // Unfortunately we can't help on SSL
                RMISocketFactory factory = new RMISocketFactory()
                    {
                    public ServerSocket createServerSocket(int nPort)
                            throws IOException
                        {
                        // TODO: register a protocol prefix for RMI once our multiplexing layer supports it, for now
                        // JMX is the only public protocol supported on our multiplexed sockets.
                        ServerSocket socket = TcpSocketProvider.DEMULTIPLEXED.openServerSocket();
                        socket.bind(sHost == null ? new InetSocketAddress(nPort) : new InetSocketAddress(sHost, nPort));
                        return socket;
                        }

                    public Socket createSocket(String sRemoteHost, int nPort)
                            throws IOException
                        {
                        return RMISocketFactory.getDefaultSocketFactory().createSocket(sRemoteHost, nPort);
                        }
                    };
                mapEnv.put("jmx.remote.rmi.server.socket.factory", factory);

                try
                    {
                    // if not set then non-wildcard binding works but nothing can connect as the RMI stub may contain
                    // the wrong address. We restore the original value in the finally block at the end of this method
                    System.setProperty("java.rmi.server.hostname", sHost);
                    }
                catch (Throwable e)
                    {
                    }
                }

            for (int i = 0; ; ++i)
                {
                int nPortAttempt = nConPort;

                // Bind and release on the configured port.  This is a workaround for two JMX issues:

                // - JMX doesn't complain if asked to use a port which is not available and appears
                // to succeed but no client would be able to connect
                // - JMX doesn't properly handle ephemeral ports, specifically if you bind to
                // port 0 you will get an ephemeral binding but connector.getAddress() will
                // simply return the same URL which was passed in and you won't know what
                // you've bound to.  To work around this we pre-compute an ephemeral port to
                // try with

                try (ServerSocket srv = new ServerSocket(nPortAttempt, 0))
                    {
                    nPortAttempt = srv.getLocalPort(); // in case it was ephemeral
                    }

                JMXServiceURL url;
                if (nRegPort == 0)
                    {
                    // dynamic url
                    url = new JMXServiceURL("rmi", sHost, nPortAttempt);
                    }
                else
                    {
                    LocateRegistry.createRegistry(nRegPort);

                    // static url
                    url = new JMXServiceURL("service:jmx:rmi://" + sHost
                            + ":" + nPortAttempt + "/jndi/rmi://" + sHost + ":"
                            + nRegPort + "/server");
                    }

                try
                    {
                    JMXConnectorServer connector = JMXConnectorServerFactory.newJMXConnectorServer(url, mapEnv, mbs);
                    connector.start();
                    return connector;
                    }
                catch (IOException e)
                    {
                    if (nConPort != 0 || i > Short.MAX_VALUE)
                        {
                        throw e;
                        }
                    // else; the ephemeral port may have been concurrently taken; retry
                    }
                }
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, "Could not start JMXConnectorServer");
            }
        finally
            {
            try
                {
                if (sPriorRmiHostname == null)
                    {
                    System.getProperties().remove("java.rmi.server.hostname");
                    }
                else
                    {
                    System.setProperty("java.rmi.server.hostname", sPriorRmiHostname);
                    }
                }
            catch (Throwable e) {}
            }
        }

    /**
    * Start a <tt>com.sun.jdmk.comm.HtmlAdaptorServer</tt>, which is a part of
    * the Sun JMX reference implementation.  It is being created via
    * reflection to avoid a runtime dependency to this library.
    *
    * @param nPort  port to bind the HTTP server to
    * @param mbs    MBeanServer that this HTTP server will expose
    *
    * @return an HtmlAdaptorServer that has been started
    */
    public static Object startHttpConnector(int nPort, MBeanServer mbs)
        {
        try
            {
            // this is similar to HttpAdapter in TDE core-net
            String     sAdapter      = "HttpAdapter:port=" + nPort;
            ObjectName nameAdapter   = new ObjectName(sAdapter);
            Object     adaptorServer = ClassHelper.newInstance(
                    Class.forName("com.sun.jdmk.comm.HtmlAdaptorServer"),
                    new Object[] {Integer.valueOf(nPort)});

            mbs.registerMBean(adaptorServer, nameAdapter);
            mbs.invoke(nameAdapter, "start", null, null);

            return adaptorServer;
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e,
                "Please ensure that JMX RI (jmxtools.jar, jmxri.jar) is in the classpath");
            }
        }

    /**
    * Create an escape-sequence string that allows for special characters to
    * be included in a JMX ObjectName.
    *
    * @param s  the string to be quoted
    *
    * @return the quoted string
    *
    * @since Coherence 3.4
    */
    public static String quote(String s)
        {
        return quote(s, /*fKey*/ true);
        }

    /**
    * Create an escape-sequence string that allows for special characters to
    * be included in a JMX ObjectName.
    *
    * @param s     the string to be quoted
    * @param fKey  true if a key in the ObjectName is being quoted
    *
    * @return the quoted string
    *
    * @since 12.2.1.4
    */
    protected static String quote(String s, boolean fKey)
        {
        String          sDelims = "\n\\\"" + (fKey ? "*?" : "");
        StringTokenizer tokens  = new StringTokenizer(s, sDelims, true);
        StringBuilder   sb      = new StringBuilder("\"");

        while (tokens.hasMoreTokens())
            {
            String sToken = tokens.nextToken();
            if (sToken.length() == 1 && sDelims.contains(sToken))
                {
                sb.append('\\');
                if (sToken.equals("\n"))
                    {
                    sb.append('n');
                    }
                else
                    {
                    sb.append(sToken);
                    }
                }
            else
                {
                sb.append(sToken);
                }
            }
        sb.append('"');
        return sb.toString();
        }

   /**
    * Unquote a string iff given string is quoted otherwise return original string.
    *
    * @param s  the string
    *
    * @return the unquoted string
    *
    * @since 12.2.1.4
    */
    public static String safeUnquote(String s)
        {
        if (s.startsWith("\"") && s.endsWith("\""))
            {
            return s.replaceAll("^\"|\"$", "");
            }
        return s;
        }

    /**
    * Convert a string returned from the {@link #quote} method to the original
    * string.
    *
    * @param s  the string to be unquoted
    *
    * @return the unquoted string
    *
    * @throws IllegalArgumentException if the passed string could not have
    *         been returned by the {@link #quote} method; for instance
    *         if it does not begin and end with a quote (")
    *
    * @since Coherence 3.4
    */
    public static String unquote(String s)
        {
        String          sDelims = "\"\\";
        StringTokenizer tokens  = new StringTokenizer(s, sDelims, true);
        StringBuilder   sb      = new StringBuilder("");

        if (tokens.countTokens() == 1)
            {
            return s;
            }

        if (tokens.nextToken().equals("\""))
            {
            while (tokens.hasMoreTokens())
                {
                String sToken = tokens.nextToken();
                if (sDelims.contains(sToken))
                    {
                    if (tokens.hasMoreTokens())
                        {
                        String sNextToken = tokens.nextToken();
                        if (sNextToken.startsWith("n"))
                            {
                            sb.append('\n');
                            sb.append(sNextToken.substring(1));
                            }
                        else
                            {
                            sb.append(sNextToken);
                            }
                        }
                    }
                else
                    {
                    sb.append(sToken);
                    }
                }
            }
        else
            {
            throw new IllegalArgumentException("Argument not quoted");
            }

        return sb.toString();
        }

    /**
    * Determine if the string requires quotes.
    *
    * @param s  the string to be quoted
    *
    * @return true iff the {@link #quote} method needs to be called
    *
    * @since Coherence 3.4
    */
    public static boolean isQuoteRequired(String s)
        {
        return isQuoteRequired(s, /*fKey*/ true);
        }

    /**
    * Determine if the string requires quotes.
    *
    * @param s     the string to be quoted
    * @param fKey  true if a key in the ObjectName is being quoted
    *
    * @return true iff the {@link #quote} method needs to be called
    *
    * @since Coherence 3.4
    */
    public static boolean isQuoteRequired(String s, boolean fKey)
        {
        return !isQuoted(s)
               && (s.indexOf(',') != -1
                   || s.indexOf('=') != -1
                   || s.indexOf(':') != -1
                   || !quote(s, fKey).equals("\"" + s + "\""));
        }

    /**
     * Return {@code true} if the specified string is already quoted.
     *
     * @param s  the string to check
     *
     * @return {@code true} if the specified string is already quoted
     */
    private static boolean isQuoted(String s)
        {
        int nLen = s.length();
        return nLen >= 2
               && s.charAt(0) == '"'
               && s.charAt(nLen - 1) == '"';
        }

    /**
    * Return a quoted {@link ObjectName#getKeyPropertyListString KeyPropertyString}
    * or a quoted canonical name. Wildcard and AnyCharacter (* and ? respectively)
    * are not escaped when present on the value of a key value pair.
    *
    * @param sCanonical  a string to be quoted
    *
    * @return a quoted and escape-sequence string
    *
    * @throws MalformedObjectNameException if the name is invalid
    *
    * @since Coherence 3.4
    */
    public static String quoteCanonical(String sCanonical)
            throws MalformedObjectNameException
        {
        int           ofDomain    = sCanonical.indexOf(':');
        int           ofEquals    = sCanonical.indexOf('=');
        String        sName       = sCanonical;
        StringBuilder sbCanonical = new StringBuilder();

        // Note: a colon could be a part of a key-value pair
        // (e.g. "domain:k1=v:1,k2=v2" or "k1=v:1,k2=v2")
        if (ofDomain >= 0 && (ofEquals < 0 || ofDomain < ofEquals))
            {
            // strip the domain part of the name
            sName = sCanonical.substring(ofDomain + 1);
            sbCanonical.append(sCanonical.substring(0, ofDomain + 1));
            }

        String[] asPairs = Base.parseDelimitedString(sName , ',');
        int      cPairs  = asPairs.length;
        for (int i = 0; i < cPairs; i++)
            {
            StringBuilder sbValue = new StringBuilder();
            String        sPair   = asPairs[i];
            if (asPairs[i].equals("*"))
                {
                sbCanonical.append(asPairs[i]);
                }
            else
                {
                int ofValue = sPair.indexOf('=');
                if (ofValue > -1)
                    {
                    String[] asPair = Base.parseDelimitedString(asPairs[i], '=');
                    String   sKey   = asPair[0];
                    if (isQuoteRequired(sKey))
                        {
                        sKey = quote(sKey);
                        }

                    sbCanonical.append(sKey).append('=');
                    sbValue.append(sPair.substring(ofValue + 1));
                    }
                else
                    {
                    throw new MalformedObjectNameException("ObjectName \""
                            + sCanonical + "\" is invalid.");
                    }

                String sValue = sbValue.toString();
                if (isQuoteRequired(sValue, /*fKey*/ false))
                    {
                    sValue = quote(sValue, /*fKey*/ false);
                    }

                sbCanonical.append(sValue);
                if (i + 1 < cPairs)
                    {
                    sbCanonical.append(',');
                    }
                }
            }
        return sbCanonical.toString();
        }

    /**
    * Return true if the Canonical name is prefixed with the domain name.
    *
    * @param sCanonical  a canonical MBean name or key property list
    *
    * @return true iff the name contains the domain prefix
    *
    * @since Coherence 3.6
    */
    public static boolean hasDomain(String sCanonical)
        {
        if (sCanonical == null)
            {
            return false;
            }

        int ofDomain = sCanonical.indexOf(':');
        int ofEquals = sCanonical.indexOf('=');

        // Note: a colon could be a part of a key-value pair
        // (e.g. "domain:k1=v:1,k2=v2" or "k1=v:1,k2=v2")
        return ofDomain >= 0 && (ofEquals < 0 || ofDomain < ofEquals);
        }

    /**
     * Ensure the Canonical name is prefixed with the domain name.
     *
     * @param sCanonical  a canonical MBean name or key property list
     *
     * @return a Canonical name that is prefixed with the domain name.
     *
     * @since Coherence 12.2.1.4
     */
    public static String ensureDomain(String sCanonical)
        {
        if (sCanonical == null || hasDomain(sCanonical) && sCanonical.indexOf(':') > 0)
            {
            return sCanonical;
            }

        String sDomain = CacheFactory.getCluster().getManagement().getDomainName();
        if (sDomain == null || sDomain.length() == 0)
            {
            sDomain = getDefaultDomain();
            }

        if (sDomain == null)
            {
            sDomain = "Coherence";
            }

        if (sCanonical.indexOf(':') == 0)
            {
            return sDomain + sCanonical;
            }
        else
            {
            return sDomain + ":" + sCanonical;
            }
        }

    /**
    * Remove the domain prefix from the canonical name if one exists.
    *
    * @param sCanonical  a canonical MBean name or key property list
    *
    * @return the canonical name stripped of the domain prefix
    *
    * @since Coherence 3.6
    */
    public static String stripDomain(String sCanonical)
        {
        return hasDomain(sCanonical)
            ? sCanonical.substring(sCanonical.indexOf(':') + 1)
            : sCanonical;
        }

    /**
    * Compare two {@link Registry#ensureGlobalName(String) global MBean names}
    * forcing numeric comparison of the node ID while using string comparison
    * on all other key properties. For example, the following order is enforced:
    * <pre>
    *   Coherence:type=Node,nodeId=2 &lt; Coherence:type=Node,nodeId=10
    *   Coherence:type=Cache,nodeId=20 &lt; Coherence:type=Node,nodeId=1
    * </pre>
    * If the key sets are different the lexicographical comparison is used.
    *
    * @param oname1 the first ObjectName to be compared
    * @param oname2 the second ObjectName to be compared
    *
    * @return  a negative integer, zero, or a positive integer as the first name
    *          is less than, equal to, or greater than the second one
    *
    * @since Coherence 3.6
    */
    public static int compareKeyList(ObjectName oname1, ObjectName oname2)
        {
        Map mapKeyList1 = oname1.getKeyPropertyList();
        Map mapKeyList2 = oname2.getKeyPropertyList();
        Set setKeys1    = mapKeyList1.keySet();
        Set setKeys2    = mapKeyList2.keySet();

        if (setKeys1.equals(setKeys2))
            {
            for (Object o : mapKeyList1.keySet())
                {
                String sKey = (String) o;

                String sValue1 = (String) mapKeyList1.get(sKey);
                String sValue2 = (String) mapKeyList2.get(sKey);

                if ((sKey + '=').equals(Registry.KEY_NODE_ID))
                    {
                    int n1 = Integer.parseInt(sValue1);
                    int n2 = Integer.parseInt(sValue2);
                    if (n1 != n2)
                        {
                        return n1 - n2;
                        }
                    }
                else
                    {
                    int iCompare = sValue1.compareTo(sValue2);
                    if (iCompare != 0)
                        {
                        return iCompare;
                        }
                    }
                }
            return 0;
            }

        // use the lexicographical comparison
        return oname1.getCanonicalName().compareTo(oname2.getCanonicalName());
        }


    // ----- annotation-related helpers -------------------------------------

    /**
    * Retrieve the description for the MBean from the MBean interface annotation.
    *
    * @param clzMBeanIface the MBean interface
    * @param info          the {@link MBeanInfo} for the MBean
    *
    * @return the MBean description
    *
    * @since Coherence 12.1.2
    */
    public static String getDescription(Class<?> clzMBeanIface, MBeanInfo info)
        {
        String sDesc = null;
        if (clzMBeanIface != null)
            {
            Description desc = clzMBeanIface.getAnnotation(Description.class);
            if (desc != null)
                {
                sDesc = desc.value();
                }
            }

        return sDesc == null ? info.getDescription() : sDesc;
        }

    /**
    * Retrieve a description for a particular attribute by finding a
    * {@link Description} annotation on the getter method for the attribute.
    * If a description is not found on the getter method, the setter will
    * be checked.
    *
    * @param clzMBeanIface the MBean interface
    * @param info          the {@link MBeanAttributeInfo} for the attribute
    *
    * @return the description for an attribute
    *
    * @since Coherence 12.1.2
    */
    public static String getDescription(Class clzMBeanIface, MBeanAttributeInfo info)
        {
        String sDesc     = null;
        String sMethName = info.getName();

        if (info.isIs())
            {
            sMethName = "is" + sMethName;
            }
        else
            {
            sMethName = "get" + sMethName;
            }

        Method methGetter = findMethod(clzMBeanIface, sMethName);
        if (methGetter != null)
            {
            Description descGetter = methGetter.getAnnotation(Description.class);
            if (descGetter != null)
                {
                sDesc = descGetter.value();
                }
            else
                {
                // try the setter
                sMethName = "set" + info.getName();
                Method methSetter = findMethod(clzMBeanIface, sMethName);
                if (methSetter != null)
                    {
                    Description descSetter = methSetter.getAnnotation(Description.class);
                    if (descSetter != null)
                        {
                        sDesc = descSetter.value();
                        }
                    }
                }
            }

        return sDesc == null ? info.getDescription() : sDesc;
        }

    /**
    * Retrieve a description for the particular {@link MBeanOperationInfo} by
    * finding a {@link Description} annotation on the corresponding method.
    *
    *
    * @param clzMBeanIface the MBean interface
    * @param info          the {@link MBeanOperationInfo}
    *
    * @return the description for an operation
    *
    * @since Coherence 12.1.2
    */
    public static String getDescription(Class clzMBeanIface, MBeanOperationInfo info)
        {
        String sDesc = null;
        Method meth  = findMethod(clzMBeanIface, info);
        if (meth != null)
            {
            Description desc = meth.getAnnotation(Description.class);
            if (desc != null)
                {
                sDesc = desc.value();
                }
            }

        return sDesc == null ? info.getDescription() : sDesc;
        }

    /**
    * Retrieve the parameter name for the specified parameter by finding a
    * {@link Description} annotation on the corresponding method.
    *
    * @param clzMBeanIface  the MBean interface
    * @param infoOp         the {@link MBeanOperationInfo} for the op
    * @param infoParam      the {@link MBeanParameterInfo} for the parameter
    * @param iParam         zero-based sequence number of the parameter
    *
    * @return the name to use for the given parameter
    *
    * @since Coherence 12.1.2
    */
    public static String getParameterName(Class clzMBeanIface,
            MBeanOperationInfo infoOp, MBeanParameterInfo infoParam, int iParam)
        {
        String sName = infoParam.getName();
        Method meth  = findMethod(clzMBeanIface, infoOp);
        if (meth != null)
            {
            Description desc = getParameterAnnotation(meth, iParam, Description.class);
            if (desc != null)
                {
                sName = desc.value();
                }
            }

        return sName;
        }

    /**
    * Return an {@link MBeanNotificationInfo} if a {@link Notification}
    * annotation is present on the provided MBean interface.
    *
    * @param clzMBeanIface  the MBean interface
    *
    * @return an array of MBeanNotificationInfo of size 0 or 1 based on the
    *         presence of a Notification annotation.
    */
    public static MBeanNotificationInfo[] getNotificationInfo(Class<?> clzMBeanIface)
        {
        Notification anno = clzMBeanIface.getAnnotation(Notification.class);

        if (anno != null)
            {
            return new MBeanNotificationInfo[] {
                    new MBeanNotificationInfo(anno.types(), anno.className(), anno.description())};
            }
        return new MBeanNotificationInfo[0];
        }

    /**
    * Retrieve an {@link Annotation} for a parameter to a method.
    *
    * @param <A>      the Annotation sub type
    * @param meth     the {@link Method}
    * @param iParam   zero-based index of the parameter
    * @param clzAnno  the Annotation {@link Class} to be retrieved
    *
    * @return the annotation or null if no {@link Annotation} is found
    */
    protected static <A extends Annotation> A getParameterAnnotation(
            Method meth, int iParam, Class<A> clzAnno)
        {
        for (Annotation a : meth.getParameterAnnotations()[iParam])
            {
            if (clzAnno.isInstance(a))
                {
                return clzAnno.cast(a);
                }
            }
        return null;
        }

    /**
    * Find a {@link Method} for the specified {@link MBeanOperationInfo} in
    * the specified MBean class or interface.
    *
    * @param clzMBean  the MBean class (interface)
    * @param op        the {@link MBeanOperationInfo}
    *
    * @return the {@link Method} or null if no method is found
    */
    protected static Method findMethod(Class clzMBean, MBeanOperationInfo op)
        {
        MBeanParameterInfo[] aParam       = op.getSignature();
        int                  cParams      = aParam.length;
        String[]             asParamTypes = new String[cParams];
        for (int i = 0; i < cParams; i++)
            {
            asParamTypes[i] = aParam[i].getType();
            }
        return findMethod(clzMBean, op.getName(), asParamTypes);
        }

    /**
    * Find a {@link Method} with the specified name and parameter types in
    * the specified class.
    *
    * @param clz           the class (interface)
    * @param sName         the {@link Method} name
    * @param asParamTypes  the array of Strings representing parameter types
    *
    * @return the {@link Method} or null if no method is found
    */
    protected static Method findMethod(Class clz, String sName, String... asParamTypes)
        {
        ClassLoader loader    = clz.getClassLoader();
        int         cParams   = asParamTypes.length;
        Class[]     aclzParam = new Class[cParams];

        try
            {
            for (int i = 0; i < cParams; i++)
                {
                aclzParam[i] = classForName(asParamTypes[i], loader);
                }
            return ClassHelper.findMethod(clz, sName, aclzParam, false);
            }
        catch (Exception e)
            {
            return null;
            }
        }

    /**
     * Find a {@link Class} for the type name. This method also handles primitive
     * types.
     * <p>
     * Note: for security related reasons this method has package private access.
     *
     * @param sName   the name of the type
     * @param loader  the {@link ClassLoader} to use
     *
     * @return the {@link Class} for the type name
     *
     * @throws ClassNotFoundException if no such {@link Class} is found using the
     *         provided {@link ClassLoader}
     */
    static Class classForName(String sName, ClassLoader loader)
        throws ClassNotFoundException
        {
        Class clz = SCALAR_TYPES.get(sName);
        if (clz == null)
            {
            clz = Class.forName(sName, false, loader);
            }
        return clz;
        }

    /**
     * Helper class to expose a {@link Filter} object as a {@link QueryExp}.
     */
    static public class QueryExpFilter
            implements QueryExp
        {
        public QueryExpFilter(Filter<ObjectName> filter)
            {
            f_filter = filter;
            }

        @Override
        public boolean apply(ObjectName name)
            {
            return f_filter == null || f_filter.evaluate(name);
            }

        @Override
        public void setMBeanServer(MBeanServer s)
            {
            }


        // ----- data fields ----------------------------------------------

        /**
         * The underlying filter.
         */
        private final Filter<ObjectName> f_filter;
        }

    /**
     * Ensure {@link GatewayDependencies} and return them.
     *
     * @return {@link GatewayDependencies}
     */
    private static GatewayDependencies ensureGatewayDependencies()
        {
        GatewayDependencies deps = s_deps;
        if (deps == null)
            {
            deps = s_deps = LegacyXmlGatewayHelper.fromXml(CacheFactory.getManagementConfig(),
                new DefaultGatewayDependencies());
            }

        return deps;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Lazily loaded GatewayDependencies.
     */
    private static GatewayDependencies s_deps;

    /**
    * A map of scalar types (classes) keyed by the corresponding JMX signatures.
    *
    * @see <a href="http://download.oracle.com/otndocs/jcp/7127-jmx-1.2-mr-spec-oth-JSpec/">
    *     JMX 1.2 specification; Chapter 3, Basic Data Types</a>
    */
    public static final Map<String, Class> SCALAR_TYPES;

    /**
    * A map of scalar SimpleTypes (classes) keyed by the corresponding JMX signatures.
    */
    public static final Map<String, SimpleType> SCALAR_SIMPLETYPES;

    static
        {
        Map<String, Class> mapScalar = new HashMap<>();

        mapScalar.put("java.lang.Boolean", Boolean.TYPE);
        mapScalar.put("boolean", Boolean.TYPE);
        mapScalar.put("java.lang.Character", Character.TYPE);
        mapScalar.put("char", Character.TYPE);
        mapScalar.put("java.lang.Byte", Byte.TYPE);
        mapScalar.put("byte", Byte.TYPE);
        mapScalar.put("java.lang.Short", Short.TYPE);
        mapScalar.put("short", Short.TYPE);
        mapScalar.put("java.lang.Integer", Integer.TYPE);
        mapScalar.put("int", Integer.TYPE);
        mapScalar.put("java.lang.Long", Long.TYPE);
        mapScalar.put("long", Long.TYPE);
        mapScalar.put("java.lang.Float", Float.TYPE);
        mapScalar.put("float", Float.TYPE);
        mapScalar.put("java.lang.Double", Double.TYPE);
        mapScalar.put("double", Double.TYPE);

        SCALAR_TYPES = Collections.unmodifiableMap(mapScalar);

        Map<String, SimpleType> mapSimple = new HashMap<>();

        mapSimple.put("java.lang.Boolean", SimpleType.BOOLEAN);
        mapSimple.put("boolean", SimpleType.BOOLEAN);
        mapSimple.put("java.lang.Character", SimpleType.CHARACTER);
        mapSimple.put("character", SimpleType.CHARACTER);
        mapSimple.put("java.lang.Byte", SimpleType.BYTE);
        mapSimple.put("byte", SimpleType.BYTE);
        mapSimple.put("java.lang.Short", SimpleType.SHORT);
        mapSimple.put("short", SimpleType.SHORT);
        mapSimple.put("java.lang.Integer", SimpleType.INTEGER);
        mapSimple.put("int", SimpleType.INTEGER);
        mapSimple.put("java.lang.Long", SimpleType.LONG);
        mapSimple.put("long", SimpleType.LONG);
        mapSimple.put("java.lang.Float", SimpleType.FLOAT);
        mapSimple.put("float", SimpleType.FLOAT);
        mapSimple.put("java.lang.Double", SimpleType.DOUBLE);
        mapSimple.put("double", SimpleType.DOUBLE);
        mapSimple.put("java.lang.String", SimpleType.STRING);
        mapSimple.put("string", SimpleType.STRING);

        SCALAR_SIMPLETYPES = Collections.unmodifiableMap(mapSimple);
        }

    /**
     * Returns {@code true} if the specified MBean name is a non-member specific name.
     *
     * @param sName  the MBean name to test
     *
     * @return  {@code true} if the specified MBean name is a non-member specific name
     */
    public static boolean isNonMemberMBean(String sName)
        {
        return sName.contains(Registry.CLUSTER_TYPE) ||
               sName.contains(Registry.MANAGEMENT_TYPE) ||
               sName.contains(Registry.KEY_RESPONSIBILITY) ||
               sName.contains(Registry.SUBSCRIBER_TYPE + ",");
        }

    /**
    * Package name for TDE-based dynamic MBean objects.
    */
    private static final String MANAGEABLE = "com.tangosol.coherence.component.manageable.";

    /**
     * A lock to sync topic subscriber group MBean access.
     */
    private static final Lock s_lockTopicSubscriberGroups = new ReentrantLock();
    }
