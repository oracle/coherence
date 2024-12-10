/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service;

import com.tangosol.coherence.config.scheme.PagedTopicScheme;

import com.tangosol.io.ConfigurableSerializerFactory;
import com.tangosol.io.SerializerFactory;

import com.tangosol.net.CacheService;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.InvocationService;
import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import java.util.Map;

/**
 * LegacyXmlServiceHelper parses the XML to populate the DefaultServiceDependencies.
 *
 * @author pfm 2011.05.08
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlServiceHelper
    {
    /**
     * Populate the DefaultServiceDependencies object from the given XML configuration.
     * The XML may be one of several different elements, such as <distributed-cache>.
     *
     * @param xml   the XML parent element that contains the child service elements
     * @param deps  the DefaultServiceDependencies to be populated
     * @param ctx   the OperationalContext
     *
     * @return this object
     */
    @SuppressWarnings("rawtypes")
    public static DefaultServiceDependencies fromXml(XmlElement xml,
            DefaultServiceDependencies deps, OperationalContext ctx)
        {
        if (xml == null)
            {
            throw new IllegalArgumentException("XML argument cannot be null");
            }

        deps.setRequestTimeoutMillis(
           XmlHelper.parseTime(xml, "request-timeout", deps.getRequestTimeoutMillis()));

        XmlElement xmlThreads = xml.getElement("thread-count");
        if (xmlThreads == null || xmlThreads.isEmpty())
            {
            deps.setWorkerThreadCountMax(xml.getSafeElement("thread-count-max").getInt(deps.getWorkerThreadCountMax()));
            deps.setWorkerThreadCountMin(xml.getSafeElement("thread-count-min").getInt(deps.getWorkerThreadCountMin()));
            }
        else
            {
            deps.setWorkerThreadCount(xmlThreads.getInt(deps.getWorkerThreadCount()));
            }

        deps.setTaskHungThresholdMillis(
           XmlHelper.parseTime(xml, "task-hung-threshold", deps.getTaskHungThresholdMillis()));

        deps.setTaskTimeoutMillis(
           XmlHelper.parseTime(xml, "task-timeout", deps.getTaskTimeoutMillis()));

        deps.setThreadPriority(
                xml.getSafeElement("service-priority").getInt(deps.getThreadPriority()));

        deps.setEventDispatcherThreadPriority(xml.getSafeElement("event-dispatcher-priority")
                .getInt(deps.getEventDispatcherThreadPriority()));

        deps.setWorkerThreadPriority(
                xml.getSafeElement("worker-priority").getInt(deps.getWorkerThreadPriority()));

        XmlElement xmlSerializer = xml.getElement("serializer");
        if (xmlSerializer != null && !XmlHelper.isEmpty(xmlSerializer))
            {
            SerializerFactory factory;
            if (xmlSerializer.isEmpty())
                {
                ConfigurableSerializerFactory factoryImpl = new ConfigurableSerializerFactory();
                factoryImpl.setConfig(xmlSerializer);
                factory = factoryImpl;
                }
            else
                {
                String sName = xmlSerializer.getString();
                Map mapSerializer = ctx.getSerializerMap();

                // The <serializer> element is _not_ empty; it contains
                // a string value (e.g. <serializer>pof</serializer>).
                // The serializer map should contain a corresponding serializer.
                factory = (SerializerFactory) mapSerializer.get(sName);
                if (factory == null)
                    {
                    throw new IllegalArgumentException(
                            "Serializer name \"" + sName + "\" is undefined:\n" + xmlSerializer);
                    }
                }
            deps.setSerializerFactory(factory);
            }

        return deps;
        }

    /**
    * Based on the provided XmlElement, which represents the caching scheme,
    * determine the service name that will be used for the service.
    *
    * @param xmlScheme  the caching scheme to derive a service name from
    *
    * @return the service name that will be used based on the provided
    *         scheme
    */
    public static String getServiceName(XmlElement xmlScheme)
        {
        String sSchemeType  = xmlScheme.getName();
        String sServiceName = xmlScheme.getSafeElement("service-name").getString();

        if (sServiceName.isEmpty())
            {
            switch (DefaultConfigurableCacheFactory
                .translateStandardSchemeType(sSchemeType))
                {
                case DefaultConfigurableCacheFactory.SCHEME_REPLICATED:
                    sServiceName = CacheService.TYPE_REPLICATED;
                    break;
                case DefaultConfigurableCacheFactory.SCHEME_OPTIMISTIC:
                    sServiceName = CacheService.TYPE_OPTIMISTIC;
                    break;
                case DefaultConfigurableCacheFactory.SCHEME_DISTRIBUTED:
                    sServiceName = CacheService.TYPE_DISTRIBUTED;
                    break;
                case DefaultConfigurableCacheFactory.SCHEME_LOCAL:
                case DefaultConfigurableCacheFactory.SCHEME_OVERFLOW:
                case DefaultConfigurableCacheFactory.SCHEME_DISK:
                case DefaultConfigurableCacheFactory.SCHEME_EXTERNAL:
                case DefaultConfigurableCacheFactory.SCHEME_EXTERNAL_PAGED:
                case DefaultConfigurableCacheFactory.SCHEME_FLASHJOURNAL:
                case DefaultConfigurableCacheFactory.SCHEME_RAMJOURNAL:
                case DefaultConfigurableCacheFactory.SCHEME_CLASS:
                    sServiceName = CacheService.TYPE_LOCAL;
                    break;
                case DefaultConfigurableCacheFactory.SCHEME_NEAR:
                case DefaultConfigurableCacheFactory.SCHEME_VERSIONED_NEAR:
                case DefaultConfigurableCacheFactory.SCHEME_INVOCATION:
                    sServiceName = InvocationService.TYPE_DEFAULT;
                    break;
                case DefaultConfigurableCacheFactory.SCHEME_PROXY:
                    sServiceName = "Proxy";
                    break;
                case DefaultConfigurableCacheFactory.SCHEME_REMOTE_CACHE:
                    sServiceName = CacheService.TYPE_REMOTE;
                    break;
                case DefaultConfigurableCacheFactory.SCHEME_REMOTE_INVOCATION:
                    sServiceName = InvocationService.TYPE_REMOTE;
                    break;
                case DefaultConfigurableCacheFactory.SCHEME_PAGED_TOPIC:
                    sServiceName = CacheService.TYPE_PAGED_TOPIC;
                    break;
                }
            }
        return sServiceName;
        }
    }
