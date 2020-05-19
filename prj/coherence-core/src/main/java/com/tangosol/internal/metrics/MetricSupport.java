/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.metrics;

import com.tangosol.coherence.discovery.Discovery;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Member;

import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;

import com.tangosol.net.management.annotation.MetricsScope;
import com.tangosol.net.management.annotation.MetricsTag;
import com.tangosol.net.management.annotation.MetricsValue;

import com.tangosol.net.metrics.MBeanMetric;
import com.tangosol.net.metrics.MetricsRegistryAdapter;

import com.tangosol.util.Base;

import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import java.util.function.Function;
import java.util.function.Supplier;

import static com.tangosol.util.Base.LOG_WARN;

/**
 * A helper class to provide support for registering and un-registering
 * Coherence metrics based on MBeans.
 *
 * @author jk  2019.06.19
 * @since 12.2.1.4
 */
public class MetricSupport
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link MetricSupport} that obtains a {@link MBeanServerProxy}
     * from the supplied {@link Registry}.
     * <p>
     * The {@link ServiceLoader} is used to discover and load instances of
     * {@link MetricsRegistryAdapter} that will be notified when Coherence
     * metrics are registered or removed.
     */
    // Called from TDE Gateway
    @SuppressWarnings("unused")
    public MetricSupport()
        {
        this(() -> CacheFactory.getCluster().getManagement());
        }

    /**
     * Create a {@link MetricSupport} instance that uses the specified
     * {@link Registry} supplier and adapters.
     *
     * @param supplier  the {@link Registry} supplier
     * @param adapters  the list of {@link MetricsRegistryAdapter}s
     */
    MetricSupport(Supplier<Registry> supplier, List<MetricsRegistryAdapter> adapters)
        {
        this(supplier, () -> adapters);
        }

    /**
     * Create a {@link MetricSupport} instance that uses the specified
     * {@link Registry} supplier and adapters.
     *
     * @param supplier          the {@link Registry} supplier
     * @param adaptersSupplier  the supplier providing a {@link List} of {@link MetricsRegistryAdapter}s
     */
    MetricSupport(Supplier<Registry> supplier, Supplier<List<MetricsRegistryAdapter>> adaptersSupplier)
        {
        f_suppRegistry         = supplier;
        f_suppMBeanServerProxy = () -> f_suppRegistry.get().getMBeanServerProxy().local();
        f_listRegistry         = adaptersSupplier.get();
        f_fHasRegistries       = !f_listRegistry.isEmpty();
        f_mapMetric            = new HashMap<>();
        }

    /**
     * Create a {@link MetricSupport} instance that uses the specified
     * {@link Registry} supplier.
     *
     * @param supplier  the {@link Registry} supplier
     */
    MetricSupport(Supplier<Registry> supplier)
        {
        this(supplier, () ->
            {
            List<MetricsRegistryAdapter> list = new ArrayList<>();

            ClassLoader[] classLoaders = new ClassLoader[]
                {
                Base.getContextClassLoader(),
                MetricsRegistryAdapter.class.getClassLoader()   // fallback if context classloader fails
                };

            for (int i = 0, len = classLoaders.length; i < len; i++)
                {
                ClassLoader loader = classLoaders[i];
                try
                    {
                    ServiceLoader<MetricsRegistryAdapter> serviceLoader =
                            ServiceLoader.load(MetricsRegistryAdapter.class, loader);

                    for (MetricsRegistryAdapter metricsRegistry : serviceLoader)
                        {
                        list.add(metricsRegistry);
                        }
                    break;
                    }
                catch (Throwable t)
                    {
                    list.clear();

                    if (CacheFactory.isLogEnabled(Base.LOG_WARN))
                        {
                        String msg = "Error loading MetricRegistryAdapter using the %s classloader: %s";
                        if (i == 0)
                            {
                            CacheFactory.log(String.format(msg, "context", t), Base.LOG_WARN);
                            CacheFactory.log("Attempting to load adapters using the fallback classloader.",
                                             Base.LOG_WARN);
                            }
                        else
                            {
                            CacheFactory.log(String.format(msg, "fallback", t), Base.LOG_WARN);
                            CacheFactory.log("Metrics failed to initialize.", Base.LOG_WARN);
                            }
                        CacheFactory.log(t);
                        }
                    }
                }
                return list;
            });
        }

    // ----- MetricsSupport methods -----------------------------------------

    /**
     * Determine whether there are any {@link MetricsRegistryAdapter} instances.
     *
     * @return  {@code true} if there are {@link MetricsRegistryAdapter} loaded
     */
    public boolean hasRegistries()
        {
        return f_fHasRegistries;
        }

    /**
     * Register Coherence metrics for the specified local MBean name.
     * <p>
     * If the MBean name has already been registered then it will be ignored.
     *
     * @param sMBeanName  the name of the local MBean to create and register
     *                    metrics for.
     */
    public void register(String sMBeanName)
        {
        if (f_fHasRegistries)
            {
            // We skip the Cluster MBean because when it is registered the cluster is not started
            // so we will be unable to obtain values such as cluster name, member, role etc. We
            // will register the cluster when we see the Node MBean.
            if (sMBeanName.startsWith(Registry.CLUSTER_TYPE)
                    || sMBeanName.startsWith(Registry.MANAGEMENT_TYPE)
                    || sMBeanName.startsWith(Discovery.DISCOVERY_TYPE))
                {
                return;
                }

            MBeanServerProxy proxy = f_suppMBeanServerProxy.get();
            if (sMBeanName.startsWith(Registry.NODE_TYPE))
                {
                registerInternal(f_suppRegistry.get().ensureGlobalName(Registry.CLUSTER_TYPE),
                                     proxy);
                }
            registerInternal(sMBeanName, proxy);
            }
        }

    /**
     * Remove all Coherence metrics for the specified local MBean name.
     * <p>
     * If the MBean name is not already registered it will be ignored.
     *
     * @param sMBeanName  the name of the local MBean
     */
    public synchronized void remove(String sMBeanName)
        {
        if (f_fHasRegistries && f_setRegistered.remove(sMBeanName))
            {
            Set<MBeanMetric> setMetric = f_mapMetric.remove(createObjectName(sMBeanName));

            if (setMetric != null)
                {
                for (MBeanMetric metric : setMetric)
                    {
                    for (MetricsRegistryAdapter adapter : f_listRegistry)
                        {
                        try
                            {
                            adapter.remove(metric.getIdentifier());
                            }
                        catch (Exception e)
                            {
                            CacheFactory.log("Caught exception removing metrics for "
                                             + sMBeanName + " from " + adapter);
                            }
                        }
                    }
                }
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Register metrics from the specified MBean.
     * <p>
     * If the MBean has already been registered it will be ignored
     *
     * @param sMBeanName  the name of the MBean
     * @param proxy       the {@link MBeanServerProxy} to use to obtain MBean information
     */
    private synchronized void registerInternal(String sMBeanName, MBeanServerProxy proxy)
        {
        if (f_setRegistered.contains(sMBeanName))
            {
            return;
            }

        f_setRegistered.add(sMBeanName);

        MBeanInfo        mBeanInfo = proxy.getMBeanInfo(sMBeanName);
        Set<MBeanMetric> setMetric = getMetrics(sMBeanName, mBeanInfo, proxy);
        if (setMetric.size() > 0)
            {
            f_mapMetric.put(createObjectName(sMBeanName), setMetric);
            for (MetricsRegistryAdapter adapter : f_listRegistry)
                {
                for (MBeanMetric metric : setMetric)
                    {
                    try
                        {
                        adapter.register(metric);
                        }
                    catch (Throwable t)
                        {
                        CacheFactory.log("Caught exception registering metric "
                                + metric.getIdentifier() + " with " + adapter, LOG_WARN);
                        }
                    }
                }
            }
        }

    /**
     * Create an ObjectName from the MBean name.
     *
     * @param sMBeanName  the MBean name
     *
     * @return an {@link ObjectName}
     */
    private ObjectName createObjectName(String sMBeanName)
        {
        try
            {
            sMBeanName = MBeanHelper.ensureDomain(sMBeanName);
            return new ObjectName(MBeanHelper.quoteCanonical(sMBeanName));
            }
        catch (MalformedObjectNameException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Returns the metrics published by the specified MBean.
     *
     * @param sMBeanName the MBean name
     * @param mBeanInfo  the MBeanInfo for the MBean
     * @param proxy      the {@link MBeanServerProxy} to use to obtain MBean values
     *
     * @return  the {@link Set} of metrics for the MBean, or an empty {@link Set}
     *          if the MBean has no metrics
     */
    Set<MBeanMetric> getMetrics(String sMBeanName, MBeanInfo mBeanInfo, MBeanServerProxy proxy)
        {
        MBeanAttributeInfo[]           aAttributeInfo   = mBeanInfo.getAttributes();
        ObjectName                     objectName       = createObjectName(sMBeanName);
        Map<String,MBeanAttributeInfo> mapMetricValues  = new HashMap<>();
        Map<String,String>             mapTagAttributes = new HashMap<>();

        if (isPlatformMBean(objectName))
            {
            getMetricsForPlatformMBean(aAttributeInfo, objectName, mapMetricValues, mapTagAttributes);
            }
        else
            {
            getMetricsForCoherenceMBean(aAttributeInfo, mapMetricValues, mapTagAttributes);
            }

        Set<MBeanMetric> setMetric = new HashSet<>();

        if (!mapMetricValues.isEmpty())
            {
            Map<String, String> mapTag = createMetricTags(sMBeanName, proxy, objectName, mapTagAttributes);

            for (Map.Entry<String, MBeanAttributeInfo> entry : mapMetricValues.entrySet())
                {
                List<MBeanMetric> list = getMetricsForAttribute(sMBeanName,
                                                                objectName,
                                                                mBeanInfo,
                                                                entry.getValue(),
                                                                mapTag);
                setMetric.addAll(list);
                }
            }

        return setMetric;
        }

    /**
     * Obtain the metrics for a Coherence MBean.
     *
     * @param aAttributeInfo    the array of the MBean's MBeanAttributeInfo instances
     * @param mapMetricValues   the {@link Map} of metric value attributes to populate
     * @param mapTagAttributes  the {@link Map} of metric tag attributes to populate
     */
    private void getMetricsForCoherenceMBean(MBeanAttributeInfo[]            aAttributeInfo,
                                             Map<String, MBeanAttributeInfo> mapMetricValues,
                                             Map<String, String>             mapTagAttributes)
        {
        for (MBeanAttributeInfo attributeInfo : aAttributeInfo)
            {
            if (ATTRIBUTE_FILTER.evaluate(attributeInfo))
                {
                String sTagName = getMetric("metrics.tag", attributeInfo);

                if (sTagName != null)
                    {
                    if (sTagName.equals(MetricsTag.DEFAULT))
                        {
                        sTagName = attributeInfo.getName();
                        }

                    sTagName = sTagName.replaceAll(REGEX_TAG, UNDER_SCORE);

                    if (sTagName.equals("senior_member_id"))
                        {
                        // skip - this was a hack tag in the original metrics
                        continue;
                        }

                    mapTagAttributes.put(sTagName, attributeInfo.getName());
                    }
                else
                    {
                    mapMetricValues.put(attributeInfo.getName(), attributeInfo);
                    }
                }
            }
        }

    /**
     * Obtain the metrics for a Coherence Platform MBean.
     *
     * @param aAttributeInfo    the array of the MBean's MBeanAttributeInfo instances
     * @param objectName        the MBean's {@link ObjectName}
     * @param mapMetricValues   the {@link Map} of metric value attributes to populate
     * @param mapTagAttributes  the {@link Map} of metric tag attributes to populate
     */
    private void getMetricsForPlatformMBean(MBeanAttributeInfo[]            aAttributeInfo,
                                            ObjectName                      objectName,
                                            Map<String, MBeanAttributeInfo> mapMetricValues,
                                            Map<String, String>             mapTagAttributes)
        {
        if (f_setPlatfromMBeans.stream().anyMatch(pattern -> pattern.apply(objectName)))
            {
            for (MBeanAttributeInfo attributeInfo : aAttributeInfo)
                {
                String sName = attributeInfo.getName();
                String sType = attributeInfo.getType();

                if (!sType.startsWith("[") && !LIST_SKIP_ATTRIBUTES.contains(sName))
                    {
                    if (LIST_TAG_TYPES.contains(sType))
                        {
                        mapTagAttributes.put(sName, sName);
                        }
                    else
                        {
                        mapMetricValues.put(sName, attributeInfo);
                        }
                    }
                }
            }
        }

    /**
     * Obtain the metrics for an attribute.
     * <p>
     * Some attributes (for example {@link CompositeData} types) may produce
     * multiple metrics.
     *
     * @param sMBeanName     the MBean name
     * @param objectName     the MBean's {@link ObjectName}
     * @param mBeanInfo      the MBean's {@link MBeanInfo}
     * @param attributeInfo  the {@link MBeanAttributeInfo} of the attribute
     * @param mapTag         the map of tags to apply to the metrics
     *
     * @return  the {@link List} of metrics from the attribute or an empty list of the attribute
     *          does not produce any metrics
     */
    private List<MBeanMetric> getMetricsForAttribute(String              sMBeanName,
                                                     ObjectName          objectName,
                                                     MBeanInfo           mBeanInfo,
                                                     MBeanAttributeInfo  attributeInfo,
                                                     Map<String, String> mapTag)
        {
        List<MBeanMetric> listMetric     = new ArrayList<>();
        String            sAttribType    = attributeInfo.getType();
        MBeanMetric.Scope scope          = getRegistryType(mBeanInfo, objectName);
        String            sAttributeName = attributeInfo.getName();

        if (attributeInfo instanceof OpenMBeanAttributeInfo)
            {
            OpenMBeanAttributeInfo openInfo = (OpenMBeanAttributeInfo) attributeInfo;
            OpenType<?>            openType = openInfo.getOpenType();

            if (openType instanceof CompositeType)
                {
                Function<MBeanServerProxy, CompositeData> function =
                        mbsp -> (CompositeData) mbsp.getAttribute(sMBeanName, sAttributeName);

                listMetric.addAll(getMetricsForCompositeType(sMBeanName, objectName, attributeInfo, mapTag,
                                                             scope, function, (CompositeType) openType));
                }
            }
        else if (TabularDataSupport.class.getName().equals(sAttribType))
            {
            // not currently supported
            }
        else
            {
            String      sMetricName  = createMetricName(objectName, attributeInfo);
            String      sDescription = attributeInfo.getDescription();
            MBeanMetric metric       = createSimpleMetric(sMBeanName, sMetricName, sAttributeName, scope, mapTag, sDescription);

            // If the metric value is not set then do not create a metric from it.
            // This stops us creating metrics for meaningless attributes, for example
            // creating metrics for service persistence attributes on a Proxy service.
            if (shouldInclude(metric))
                {
                listMetric.add(metric);
                }
            }

        return listMetric;
        }

    /**
     * Determine whether to include a metric based on whether it has a value.
     * Some Coherence MBeans have values that do not apply and are hence set
     * to a "not set" value, such as -1.
     *
     * @param metric  the metric to test
     *
     * @return  {@code true} if the metric should be created
     */
    private boolean shouldInclude(MBeanMetric metric)
        {
        Object oValue = metric.getValue();
        if (oValue != null)
            {
            return true;
            }

        // for some weird reason cache metrics may be -1 initially  ???
        return metric.getName().startsWith("Coherence.Cache.")
               && !metric.getName().startsWith("Coherence.Cache.Store")
               && !metric.getName().startsWith("Coherence.Cache.Queue");
        }

    /**
     * Obtain the metrics for an attribute.
     * <p>
     * Some attributes (for example {@link CompositeData} types) may produce
     * multiple metrics.
     *
     * @param sMBeanName     the MBean name
     * @param objectName     the MBean's {@link ObjectName}
     * @param attributeInfo  the {@link MBeanAttributeInfo} of the attribute
     * @param mapTag         the map of tags to apply to the metrics
     * @param scope          the scope of the metric
     * @param supplierParent the function that supplied the parent {@link CompositeData}
     *                       to use when obtaining the metric values
     * @param openType       the {@link CompositeType} of the metric
     *
     * @return  the {@link List} of metrics from the attribute or an empty list of the attribute
     *          does not produce any metrics
     */
    private List<MBeanMetric> getMetricsForCompositeType(String                                   sMBeanName,
                                                         ObjectName                               objectName,
                                                         MBeanAttributeInfo                       attributeInfo,
                                                         Map<String, String>                      mapTag,
                                                         MBeanMetric.Scope                        scope,
                                                         Function<MBeanServerProxy,CompositeData> supplierParent,
                                                         CompositeType                            openType)
        {
        List<MBeanMetric> list = new ArrayList<>();

        for (String sKey : openType.keySet())
            {
            OpenType<?> type = openType.getType(sKey);
            if (type instanceof SimpleType)
                {
                if (METRIC_ATTRIBUTE_TYPES.contains(type.getTypeName()))
                    {
                    String sMetricName = createMetricName(objectName, attributeInfo) + '.' + sKey;
                    String sHelp       = attributeInfo.getDescription();

                    list.add(createCompositeMetric(sMBeanName,
                                                   sMetricName,
                                                   supplierParent,
                                                   sKey,
                                                   scope,
                                                   mapTag,
                                                   sHelp));
                    }
                }
            else if (type instanceof TabularData)
                {
                // ToDo: this is where we'd need to do work to support Heap after GC
                }
            else if (type instanceof CompositeData)
                {
                Function<MBeanServerProxy, CompositeData> supplier = mbs ->
                    {
                    CompositeData data = supplierParent.apply(mbs);
                    return data == null ? null : (CompositeData) data.get(sKey);
                    };

                list.addAll(getMetricsForCompositeType(sMBeanName, objectName, attributeInfo, mapTag,
                                                       scope, supplier, (CompositeType) type));
                }
            }
        return list;
        }

    /**
     * Create a simple MBean metric.
     *
     * @param sMBeanName  the MBean name
     * @param sMetricName the metric name
     * @param sAttribute  the MBean attribute name
     * @param scope       the metric scope
     * @param mapTag      the metric's tags
     * @param sHelp       the metric's help text
     *
     * @return  a simple MBean metric
     */
    private MBeanMetric createSimpleMetric(String              sMBeanName,
                                           String              sMetricName,
                                           String              sAttribute,
                                           MBeanMetric.Scope   scope,
                                           Map<String, String> mapTag,
                                           String              sHelp)
        {
        MBeanMetric.Identifier identifier = new MBeanMetric.Identifier(scope, sMetricName, mapTag);
        return new SimpleMetric(identifier, sMBeanName, sAttribute, sHelp, f_suppMBeanServerProxy);
        }

    /**
     * Create a MBean metric wrapping a value from a {@link CompositeData} attribute.
     *
     * @param sMBeanName     the MBean name
     * @param sMetricName    the metric name
     * @param supplierParent the function that supplied the parent {@link CompositeData}
     *                       to use when obtaining the metric values
     * @param scope          the metric scope
     * @param mapTag         the metric's tags
     * @param sHelp          the metric's help text
     *
     * @return  a {@link CompositeData} MBean metric
     */
    private MBeanMetric createCompositeMetric(String                                       sMBeanName,
                                                 String                                    sMetricName,
                                                 Function<MBeanServerProxy, CompositeData> supplierParent,
                                                 String                                    sKey,
                                                 MBeanMetric.Scope                         scope,
                                                 Map<String, String>                       mapTag,
                                                 String                                    sHelp)
        {
        MBeanMetric.Identifier identifier = new MBeanMetric.Identifier(scope, sMetricName, mapTag);
        return new CompositeMetric(identifier, sMBeanName, supplierParent, sKey, sHelp, f_suppMBeanServerProxy);
        }

    private MBeanMetric.Scope getRegistryType(MBeanInfo mBeanInfo, ObjectName objectName)
        {
        Object sType = mBeanInfo.getDescriptor().getFieldValue(MetricsScope.KEY);

        if (sType == null)
            {
            return isPlatformMBean(objectName)
                    ? MBeanMetric.Scope.VENDOR
                    : MBeanMetric.Scope.APPLICATION;
            }

        try
            {
            return MBeanMetric.Scope.valueOf(String.valueOf(sType));
            }
        catch (IllegalArgumentException e)
            {
            return MBeanMetric.Scope.APPLICATION;
            }
        }

    /**
     * Create the name to use for a metric.
     *
     * @param objectName      the MBean {@link ObjectName}
     * @param attributeInfo   the {@link MBeanAttributeInfo}
     *
     * @return  the name to use for the metric
     */
    private String createMetricName(ObjectName objectName, MBeanAttributeInfo attributeInfo)
        {
        String sMetricName = getMetric("metrics.value", attributeInfo);
        String sPrefix     = s_mapMetricPrefix.entrySet()
                                    .stream()
                                    .filter(e -> e.getKey().apply(objectName))
                                    .map(Map.Entry::getValue)
                                    .findFirst()
                                    .orElse(null);

        if (sPrefix == null)
            {
            sPrefix = "Coherence." + objectName.getKeyProperty("type");
            }

        if (sMetricName == null || sMetricName.isEmpty() || sMetricName.equals(MetricsValue.DEFAULT))
            {
            sMetricName = attributeInfo.getName();
            }

        if (!sPrefix.endsWith("."))
            {
            sPrefix = sPrefix + ".";
            }

        return sPrefix + sMetricName;
        }

    /**
     * Create the {@link Map} of metric tags from the {@link ObjectName} properties.
     *
     * @param sMBeanName        the MBean name
     * @param proxy             the {@link MBeanServerProxy} to use to obtain MBean attribute values
     * @param objectName        the {@link ObjectName} to use to create the tags
     * @param mapTagAttributes  the {@link Map} of MBean attributes to use as tags
     *
     * @return  the {@link Map} of metric tags
     */
    private Map<String, String> createMetricTags(String              sMBeanName,
                                                 MBeanServerProxy    proxy,
                                                 ObjectName          objectName,
                                                 Map<String, String> mapTagAttributes)
        {
        Map<String, String> mapTag  = new HashMap<>();
        Cluster             cluster = CacheFactory.getCluster();
        Member              member  = cluster.getLocalMember();

        // these metric tags uniquely
        // identify metrics per cluster member
        if (cluster.isRunning())
            {
            // Note: a call to getClusterName will ensure the cluster which
            // is not desired when registering services that do not require
            // a running cluster (extend clients & LocalCache service)
            mapTag.put(GLOBAL_TAG_CLUSTER, cluster.getClusterName());
            }

        if (!sMBeanName.startsWith(Registry.CLUSTER_TYPE)
            && objectName.getKeyProperty("responsibility") == null)
            {
            mapTag.put(GLOBAL_TAG_SITE,    member.getSiteName());
            mapTag.put(GLOBAL_TAG_MACHINE, member.getMachineName());
            mapTag.put(GLOBAL_TAG_MEMBER,  member.getMemberName());
            mapTag.put(GLOBAL_TAG_ROLE,    member.getRoleName());
            }

        objectName.getKeyPropertyList()
                .entrySet()
                .stream()
                .filter(e -> !OBJECT_NAME_TAG_EXCLUDES.contains(e.getKey()))
                .forEach(e -> addTag(mapTag, e.getKey(), e.getValue()));

        for (Map.Entry<String, String> entry : mapTagAttributes.entrySet())
            {
            Object oTag = proxy.getAttribute(sMBeanName, entry.getValue());
            addTag(mapTag, entry.getKey(), oTag);
            }

        return mapTag;
        }

    /**
     * Add a tag to the map of tags.
     * <p>
     * This method performs any conversion of the tag name before adding it.
     *
     * @param mapTag  the {@link Map} of tags to add to
     * @param sKey    the tag key, which may be mutated before adding to the map
     * @param oValue  the tag value
     */
    private void addTag(Map<String, String> mapTag, String sKey, Object oValue)
        {
        // Only create tags that have values that are not Coherence "not set" values,
        // such as null, -1 "n/a" etc.
        if (isValueSet(oValue))
            {
            sKey = ensureStartsWithLowercase(sKey);
            String sValue = String.valueOf(oValue).replaceAll(QUOTE, ESC_QUOTE);

            if ("service".equals(sKey))
                {
                // COH-19269: transform ObjectName key property "service" to "coherence_service" to avoid clash
                // with prometheus ServiceMonitor using metric label service.  Prometheus ServiceMonitor was
                // renaming service to exported_service.
                sKey = "coherence_service";
                }

            // If the tag map already contains the key add the "tag_" prefix
            // but only if the tag with the same name AND value doesn't already exist
            if (mapTag.containsKey(sKey) && !sValue.equals(mapTag.get(sKey)))
                {
                sKey = "tag_" + sKey;
                }

            mapTag.put(sKey, sValue);
            }
        }

    /**
     * Ensures that the first letter of a string is lower case.
     *
     * @param s  string to check
     *
     * @return specified string, with a first character converted to
     *         lower case, if necessary
     */
    private static String ensureStartsWithLowercase(String s)
        {
        return Character.isLowerCase(s.charAt(0))
               ? s
               : Character.toLowerCase(s.charAt(0)) + s.substring(1);
        }

    /**
     * Return true if <code>oValue</code> set to a non-default value.
     * Optimization to avoid returning metric tag or metric value for an unset value.
     * A null value is always considered not set. For {@link String}, <i>n/a</i> and
     * <i>Not configured</i> are considered not set. For {@link Long}, the value <i>-1</i>
     * is considered not set.
     *
     * @param oValue  MBean attribute value
     *
     * @return true if value set to a non-default value for its type
     */
     private static boolean isValueSet(Object oValue)
        {
        if (oValue == null)
            {
            return false;
            }

        if (oValue instanceof Number)
            {
            return ((Number) oValue).longValue() != -1L;
            }
        else if (oValue instanceof String)
            {
            String sValue = (String) oValue;
            return sValue.length() > 0
                    && !sValue.equals("Not configured")
                    && !sValue.equalsIgnoreCase("n/a");
            }
        return true;
        }

    /**
     * Return the metric name.
     *
     * @param sMetricFieldName  either "metric.tag" or "metric.value"
     * @param attrInfo          MBean attribute information
     *
     * @return metrics.tag name from MBeanAttributeInfo @descriptor annotation.
     */
    private static String getMetric(String sMetricFieldName, MBeanAttributeInfo attrInfo)
        {
        Descriptor descriptor         = attrInfo.getDescriptor();
        Object     oMetricsFieldValue = descriptor == null ? null : descriptor.getFieldValue(sMetricFieldName);

        return oMetricsFieldValue instanceof String ? (String) oMetricsFieldValue : null;
        }

    /**
     * Initialise the static set of patterns to use to match platform MBeans
     * that should be used for metrics.
     *
     * @return the set of {@link ObjectName} patterns to use to match platform MBeans
     */
    private static Set<ObjectName> getPlatformPatterns()
        {
        try
            {
            Set<ObjectName> set = new HashSet<>();

            set.add(new ObjectName("*:type=Platform,Domain=java.lang,subType=Memory,*"));
            set.add(new ObjectName("*:type=Platform,Domain=java.lang,subType=OperatingSystem,*"));
            set.add(new ObjectName("*:type=Platform,Domain=java.lang,subType=GarbageCollector,*"));

            return set;
            }
        catch (MalformedObjectNameException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Create a map of {@link ObjectName} patterns to metric name prefixes.
     * <p>
     * This is used to figure out the prefix to use for a metric from a
     * given MBean, for example all metrics from MBeans matching the object
     * name "*:type=Node,*" are prefixed with "coherence_node_".
     *
     * @return a map of {@link ObjectName} patterns to metric name prefixes
     */
    private static Map<ObjectName, String> getMetricNamePrefixMap()
        {
        Map<ObjectName, String> map = new HashMap<>();

        map.put(createPattern(FEDERATION_ORIGIN_TYPE), "Coherence.Federation.Origin.");
        map.put(createPattern(FEDERATION_DESTINATION_TYPE), "Coherence.Federation.Destination.");
        map.put(createPattern(PLATFORM_MEMORY_TYPE), "Coherence.Memory.");
        map.put(createPattern(PLATFORM_OS_TYPE), "Coherence.OS.");
        map.put(createPattern(PLATFORM_GC), "Coherence.GC.");

        return map;
        }

    private static ObjectName createPattern(String sType)
        {
        try
            {
            return new ObjectName(String.format("*:%s,*", sType));
            }
        catch (MalformedObjectNameException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Determine whether the {@link ObjectName} is a Coherence Platform MBean
     *
     * @param objectName  the {@link ObjectName} to test
     *
     * @return {@code true} if the {@link ObjectName} is a Platform MBean
     */
    private boolean isPlatformMBean(ObjectName objectName)
        {
        return ("Platform".equals(objectName.getKeyProperty("type"))
                && "java.lang".equals(objectName.getKeyProperty("Domain")));
        }


    // ----- inner class: BaseMetric ----------------------------------------

    /**
     * A base class for a {@link MBeanMetric} that wraps
     * a Coherence MBean attribute.
     */
    abstract class BaseMetric extends BaseMBeanMetric
        {
        BaseMetric(Identifier identifier, String sMBeanName, String sDescription)
            {
            super(identifier, sMBeanName, sDescription);
            }

        // ----- BaseMetric methods -----------------------------------------

        /**
         * Obtain the MBean attribute's current value.
         *
         * @return  the current value of the MBean attribute
         */
        abstract Object getAttributeValue();

        // ----- CoherenceMetrics methods -----------------------------------

        @Override
        public Object getValue()
            {
            try
                {
                Object oValue = getAttributeValue();

                // special case handling:
                // transform of Coherence MBean attribute of type Date to Prometheus metric timestamp.
                // The timestamp is an int64 (milliseconds since epoch, i.e. 1970-01-01 00:00:00 UTC,
                // excluding leap seconds).
                if (oValue instanceof Date)
                    {
                    oValue = ((Date) oValue).getTime();
                    }

                return MetricSupport.isValueSet(oValue) ? oValue : null;
                }
            catch (IllegalArgumentException e)
                {
                // thrown if MBean no longer exists
                try
                    {
                    remove(getMBeanName());
                    }
                catch (Exception ex)
                    {
                    // ignored
                    }
                return null;
                }
            catch (Exception e)
                {
                CacheFactory.err("Caught exception getting metric value for " + getIdentifier());
                CacheFactory.err(e);
                return null;
                }
            }
        }

    // ----- inner class: SimpleMetric --------------------------------------

    /**
     * An implementation of a {@link MBeanMetric} that wraps
     * a simple MBean attribute.
     */
    class SimpleMetric extends BaseMetric
        {
        /**
         * Create a metric that wraps an attribute of a MBean.
         *
         * @param identifier    the metric {@link MBeanMetric.Identifier}
         * @param sMbean        the MBean name for this metric
         * @param sAttribute    the name of the MBean attribute represented by this metric
         * @param sDescription  the description of this metric
         * @param supplier      the supplier of the {@link MBeanServerProxy} to use to obtain
         *                      this metric's value
         */
        SimpleMetric(Identifier identifier, String sMbean, String sAttribute,
                     String sDescription, Supplier<MBeanServerProxy> supplier)
            {
            super(identifier, sMbean, sDescription);
            f_sAttribute           = sAttribute;
            f_suppMBeanServerProxy = supplier;
            }

        // ----- BaseMetric methods -----------------------------------------

        @Override
        Object getAttributeValue()
            {
            return f_suppMBeanServerProxy.get().getAttribute(getMBeanName(), f_sAttribute);
            }

        // ----- data members -----------------------------------------------

        /**
         * The supplier to use to obtain an {@link MBeanServerProxy}.
         */
        private final Supplier<MBeanServerProxy> f_suppMBeanServerProxy;

        /**
         * The MBean attribute name.
         */
        private final String f_sAttribute;
        }

    // ----- inner class: CompositeMetric -----------------------------------

    /**
     * An implementation of a {@link MBeanMetric} that wraps
     * a MBean composite data attribute.
     */
    class CompositeMetric extends BaseMetric
        {
        /**
         * Create a metric that wraps an composite attribute of a MBean.
         *
         * @param identifier      the metric {@link MBeanMetric.Identifier}
         * @param sMbean          the MBean name for this metric
         * @param supplierParent  the function to use to obtain the parent {@link CompositeData}
         * @param sDescription    the description of this metric
         * @param supplier        the supplier of the {@link MBeanServerProxy} to use to obtain
         *                        this metric's value
         */
        CompositeMetric(Identifier identifier, String sMbean, Function<MBeanServerProxy, CompositeData> supplierParent,
                        String sKey, String sDescription, Supplier<MBeanServerProxy> supplier)
            {
            super(identifier, sMbean, sDescription);
            f_supplierParent       = supplierParent;
            f_sKey                 = sKey;
            f_suppMBeanServerProxy = supplier;
            }

        // ----- BaseMetric methods -----------------------------------------

        @Override
        Object getAttributeValue()
            {
            CompositeData data = f_supplierParent.apply(f_suppMBeanServerProxy.get());

            return data == null ? null : data.get(f_sKey);
            }

        // ----- data members -----------------------------------------------

        /**
         * The supplier to use to obtain an {@link MBeanServerProxy}.
         */
        private final Supplier<MBeanServerProxy> f_suppMBeanServerProxy;

        /**
         * The supplier of the parent attribute.
         */
        private final Function<MBeanServerProxy, CompositeData> f_supplierParent;

        /**
         * The key of the value in the {@link CompositeData} that this metric represents.
         */
        private String f_sKey;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Filter out MBean attributes that do not map to a metric's tag or value.
     */
    private static final MetricsMBeanAttributeFilter ATTRIBUTE_FILTER = new MetricsMBeanAttributeFilter();

    /**
     * MBean object name query for java.lang domain of type Platform for subType Memory.
     */
    private static final String PLATFORM_MEMORY_TYPE = "type=Platform,Domain=java.lang,subType=Memory";

    /**
     * MBean object name query for java.lang domain of type Platform for subType OperatingSystem.
     */
    private static final String PLATFORM_OS_TYPE = "type=Platform,Domain=java.lang,subType=OperatingSystem";

    /**
     * MBean object name query for java.lang domain of type Platform for subType GarbageCollection.
     */
    private static final String PLATFORM_GC = "type=Platform,Domain=java.lang,subType=GarbageCollector";

    /**
     * MBean object name query for Federation OriginMBean.
     */
    private static final String FEDERATION_ORIGIN_TYPE = Registry.FEDERATION_TYPE + ",subType=Origin";

    /**
     * MBean object name query for Federation DestinationMBean.
     */
    private static final String FEDERATION_DESTINATION_TYPE = Registry.FEDERATION_TYPE + ",subType=Destination";

    /**
     * ObjectName key properties to exclude as metrics labels.
     * Most keys were redundant since the metrics value name contains this info.
     * UUID was excluded due to large size.
     */
    public static final Set<String> OBJECT_NAME_TAG_EXCLUDES =
        new HashSet<>(Arrays.asList("type","subType","responsibility", "UUID", "Domain"));

    /**
     * A list of attribute types that may be used as tags.
     */
    private static final List<String> LIST_TAG_TYPES = Arrays.asList("String",
                                                                     "boolean",
                                                                     "java.lang.String",
                                                                     "java.lang.Boolean");

    /**
     * A set of attribute types that may be used as values.
     */
    private static final Set<String> METRIC_ATTRIBUTE_TYPES = new HashSet<>(Arrays.asList(
            "long", "int", "double", "float",
            Long.class.getName(),
            Integer.class.getName(),
            Double.class.getName(),
            Float.class.getName(),
            BigDecimal.class.getName(),
            BigInteger.class.getName()
    ));

    /**
     * A set of attribute names never used as metric values.
     */
    private static final List<String> LIST_SKIP_ATTRIBUTES = Arrays.asList(
        //"LastGcInfo",    // skip overly verbose tabular data for LastGcInfo BeforeGc and AfterGc.
        "ObjectName",    // skip MBean attribute ObjectName
        "Verbose",
        "Valid"
    );

    private static final String REGEX_TAG = "[^a-zA-Z0-9]";
    private static final String UNDER_SCORE = "_";
    private static final String ESC_QUOTE = "\\\\\"";
    private static final String QUOTE = "\"";

    /**
     * The {@link ObjectName} patterns used to recognise Coherence Platform MBeans.
     */
    private static final Set<ObjectName> f_setPlatfromMBeans = getPlatformPatterns();

    /**
     * A map of {@link ObjectName} patterns to metric name prefixes.
     */
    private static final Map<ObjectName, String> s_mapMetricPrefix = getMetricNamePrefixMap();

    /**
     * The tag key used for the cluster name on all metrics.
     */
    public static final String GLOBAL_TAG_CLUSTER = "cluster";

    /**
     * The tag key used for the site name on all metrics.
     */
    public static final String GLOBAL_TAG_SITE = "site";

    /**
     * The tag key used for the machine name on all metrics.
     */
    public static final String GLOBAL_TAG_MACHINE = "machine";

    /**
     * The tag key used for the member name on all metrics.
     */
    public static final String GLOBAL_TAG_MEMBER = "member";

    /**
     * The tag key used for the role name on all metrics.
     */
    public static final String GLOBAL_TAG_ROLE = "role";

    // ----- data members ---------------------------------------------------

    /**
     * The {@link List} of {@link MetricsRegistryAdapter} to register metrics with.
     */
    private final List<MetricsRegistryAdapter> f_listRegistry;

    /**
     * A flag indicating whether there are any {@link MetricsRegistryAdapter} instances.
     */
    private final boolean f_fHasRegistries;

    /**
     * A {@link Map} of {@link ObjectName} to the metrics registered for that name.
     */
    private final Map<ObjectName, Set<MBeanMetric>> f_mapMetric;

    /**
     * The supplier to use to obtain an {@link MBeanServerProxy}.
     */
    private final Supplier<MBeanServerProxy> f_suppMBeanServerProxy;

    /**
     * The supplier to use to obtain a {@link Registry}.
     */
    private final Supplier<Registry> f_suppRegistry;

    /**
     * The currently registered MBeans.
     */
    private final Set<String> f_setRegistered = new HashSet<>();
    }
