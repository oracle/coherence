/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.config;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.CacheMapping;
import com.tangosol.coherence.config.Config;

import com.tangosol.coherence.rest.io.KeyConverterAdapter;
import com.tangosol.coherence.rest.io.Marshaller;
import com.tangosol.coherence.rest.io.MarshallerRegistry;

import com.tangosol.coherence.rest.query.QueryEngineRegistry;

import com.tangosol.coherence.rest.util.StaticContent;
import com.tangosol.coherence.rest.util.aggregator.AggregatorRegistry;

import com.tangosol.coherence.rest.util.processor.ProcessorRegistry;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;

import com.tangosol.net.Service;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.inject.Inject;

import javax.ws.rs.core.MediaType;

/**
 * The RestConfig class encapsulates information related to Coherence REST
 * configuration.
 *
 * @author vp 2011.07.08
 */
public class RestConfig
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct an instance of RestConfig based on the specified XML element.
     *
     * @param xmlConfig  the XML element that contains the configuration info
     */
    public RestConfig(XmlElement xmlConfig)
        {
        configure(xmlConfig);
        }

    // ----- factory method -------------------------------------------------

    /**
     * Return a singleton instance of <tt>RestConfig</tt> based on the
     * REST configuration descriptor.
     * <p>
     * NOTE: The default REST configuration descriptor
     * <tt>{@value RestConfig#DESCRIPTOR_NAME}</tt> can be overridden with the
     * <tt>{@value RestConfig#DESCRIPTOR_PROPERTY}</tt> system property.
     *
     * @return REST configuration
     */
    public static synchronized RestConfig create()
        {
        try
            {
            String sDescriptor = Config.getProperty(DESCRIPTOR_PROPERTY, DESCRIPTOR_NAME);
            XmlDocument xml = XmlHelper.loadFileOrResource(sDescriptor,
                    "REST configuration");
            return new RestConfig(xml);
            }
        catch (Exception e) // FileNotFoundException
            {
            Logger.warn("Failed to load REST configuration file " + DESCRIPTOR_NAME, e);
            throw Base.ensureRuntimeException(e);
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Configures this RestConfig instance using the specified XML element.
     * <p>
     * This method assumes that the given configuration has been validated
     * against the coherence-rest-config.xsd schema.
     *
     * @param xmlConfig  the XML element that contains the configuration info
     */
    protected void configure(XmlElement xmlConfig)
        {
        if (xmlConfig == null ||
            !xmlConfig.getRoot().getName().equals("rest") ||
            xmlConfig.getAttribute("xsi:schemaLocation") == null)
            {
            throw new IllegalArgumentException("invalid REST configuration:\n"
                    + xmlConfig);
            }

        MarshallerRegistry marshallerRegistry = m_marshallerRegistry = new MarshallerRegistry();

        // <defaults>
        XmlElement xmlDefaults    = xmlConfig.getSafeElement("defaults");
        XmlElement xmlMarshallers = xmlDefaults.getSafeElement("marshallers");
        marshallerRegistry.setDefaultMarshallers(createMarshallerMap(xmlMarshallers));

        // <resources>
        XmlElement xmlResources = xmlConfig.getSafeElement("resources");

        // <resource>
        Map<String, ResourceConfig> mapResources = createResourceMap(xmlResources);
        m_mapResources.putAll(mapResources);

        // register key converters as marshallers
        for (ResourceConfig resourceConfig : mapResources.values())
            {
            marshallerRegistry.registerMarshaller(
                    resourceConfig.getKeyClass(),
                    MediaType.WILDCARD_TYPE,
                    new KeyConverterAdapter(resourceConfig.getKeyConverter()));
            }

        // register marshallers
        for (ResourceConfig resourceConfig : mapResources.values())
            {
            Class clzValue = resourceConfig.getValueClass();
            for (Map.Entry<String, Class> entry : resourceConfig.getMarshallerMap().entrySet())
                {
                marshallerRegistry.registerMarshaller(clzValue,
                        entry.getKey(), entry.getValue());
                }
            }

        // <aggregators>
        XmlElement xmlAggregators = xmlConfig.getSafeElement("aggregators");
        m_aggregatorRegistry = new AggregatorRegistry(
                createAggregatorMap(xmlAggregators));

        // <processors>
        XmlElement xmlProcessors = xmlConfig.getSafeElement("processors");
        m_processorRegistry = new ProcessorRegistry(
                createProcessorMap(xmlProcessors));

        // <query-engines>
        XmlElement xmlQueryEngines = xmlConfig.getSafeElement("query-engines");
        m_queryEngineRegistry = new QueryEngineRegistry(
                createQueryEngines(xmlQueryEngines));
        }

    /**
     * Create a map of ResourceConfig keyed by cache name or alias (if
     * defined) from the given XML configuration.
     *
     * @param xml  the XML configuration
     *
     * @return a map of ResourceConfig keyed by cache name or alias
     */
    protected Map<String, ResourceConfig> createResourceMap(XmlElement xml)
        {
        Map<String, ResourceConfig> mapResources = new HashMap<String, ResourceConfig>();
        for (Iterator iter = xml.getElements("resource"); iter.hasNext(); )
            {
            XmlElement xmlResource = (XmlElement) iter.next();

            String sCacheName    = xmlResource.getSafeElement("cache-name").getString();
            String sAlias        = xmlResource.getSafeElement("alias").getString(null);
            String sKeyClass     = xmlResource.getSafeElement("key-class").getString(null);
            String sValueClass   = xmlResource.getSafeElement("value-class").getString(null);
            String sKeyConverter = xmlResource.getSafeElement("key-converter").getString(null);
            String sResourceName = xmlResource.getSafeAttribute("name").getString();
            int    cMaxResults   = xmlResource.getSafeAttribute("max-results").getInt(-1);

            Map<String, Class> mapMarshaller = createMarshallerMap(xmlResource);
            QueryConfig        queryConfig   = createQueryConfig(xmlResource);
            try
                {
                // inherit key and value types from the cache mapping, if available
                ConfigurableCacheFactory ccf = CacheFactory.getConfigurableCacheFactory();
                if (ccf instanceof ExtensibleConfigurableCacheFactory)
                    {
                    ExtensibleConfigurableCacheFactory eccf = (ExtensibleConfigurableCacheFactory) ccf;
                    CacheMapping mapping = eccf.getCacheConfig()
                            .getMappingRegistry()
                            .findCacheMapping(sCacheName);
                    if (mapping != null)
                        {
                        if (sKeyClass == null)
                            {
                            sKeyClass = mapping.getKeyClassName();
                            }
                        if (sValueClass == null)
                            {
                            sValueClass = mapping.getValueClassName();
                            }
                        }
                    }

                Class clzKey   = sKeyClass   == null ? String.class        : getContextClassLoader().loadClass(sKeyClass);
                Class clzValue = sValueClass == null ? StaticContent.class : getContextClassLoader().loadClass(sValueClass);

                // initialize key converter
                Class clzKeyConverter = null;
                if (sKeyConverter != null)
                    {
                    clzKeyConverter = getContextClassLoader().loadClass(sKeyConverter);
                    }

                // create ResourceConfig instance
                ResourceConfig cfgResource = new ResourceConfig();
                cfgResource.setCacheName(sCacheName);
                cfgResource.setKeyClass(clzKey);
                cfgResource.setValueClass(clzValue);
                cfgResource.setKeyConverterClass(clzKeyConverter);
                cfgResource.setMarshallerMap(mapMarshaller);
                cfgResource.setQueryConfig(queryConfig);
                cfgResource.setMaxResults(cMaxResults);

                // we are introducing name attribute to replace alias configuration element
                if (sResourceName == null || sResourceName.isEmpty())
                    {
                    // if resource name attribute was not configured, fall back to alias or cache name
                    sResourceName = sAlias == null ? sCacheName : sAlias;
                    }
                mapResources.put(sResourceName, cfgResource);
                }
            catch (ClassNotFoundException e)
                {
                throw Base.ensureRuntimeException(e,
                        "class not found while initializing REST configuration");
                }
            }
        return mapResources;
        }

    /**
     * Create a map of REST marshaller classes keyed by media type from the
     * given XML configuration.
     *
     * @param xml  the XML configuration
     *
     * @return a map of REST marshaller classes keyed by media type
     */
    protected Map<String, Class> createMarshallerMap(XmlElement xml)
        {
        Map<String, Class> mapMarshallers = new HashMap<String, Class>();
        for (Iterator iter = xml.getElements("marshaller"); iter.hasNext(); )
            {
            XmlElement xmlMarshaller = (XmlElement) iter.next();

            String sMediaType = xmlMarshaller.getSafeElement("media-type").getString();
            String sClass     = xmlMarshaller.getSafeElement("class-name").getString();

            Class clz;
            try
                {
                clz = getContextClassLoader().loadClass(sClass);
                }
            catch (ClassNotFoundException e)
                {
                throw Base.ensureRuntimeException(e, "class \"" + sClass
                        + "\" not found while initializing REST configuration");
                }

            if (Marshaller.class.isAssignableFrom(clz))
                {
                mapMarshallers.put(sMediaType, clz);
                }
            else
                {
                throw new IllegalArgumentException("class \"" + sClass
                        + "\" does not implement the Marshaller interface");
                }
            }
        return mapMarshallers;
        }

    /**
     * Create a map of {@link QueryConfig} objects from the given XML
     * configuration.
     *
     * @param xml  the XML configuration
     *
     * @return a map of <tt>QueryConfig</tt>s, keyed by query name
     */
    protected QueryConfig createQueryConfig(XmlElement xml)
        {
        QueryConfig config = new QueryConfig();
        for (Iterator iter = xml.getElements("query"); iter.hasNext(); )
            {
            XmlElement xmlQuery = (XmlElement) iter.next();

            int    cMaxResults = xmlQuery.getSafeAttribute("max-results").getInt(-1);
            String sEngine     = xmlQuery.getSafeAttribute("engine").getString();
            String sName       = xmlQuery.getSafeElement("name").getString();
            String sExpression = xmlQuery.getSafeElement("expression").getString();

            config.addNamedQuery(new NamedQuery(sName, sExpression, sEngine, cMaxResults));
            }

        XmlElement xmlDirectQuery = xml.getElement("direct-query");
        if (xmlDirectQuery != null)
            {
            boolean fEnabled = xmlDirectQuery.getSafeAttribute("enabled").getBoolean(false);
            if (fEnabled)
                {
                String  sEngine     = xmlDirectQuery.getSafeAttribute("engine").getString();
                int     cMaxResults = xmlDirectQuery.getSafeAttribute("max-results").getInt(-1);
                config.setDirectQuery(new DirectQuery(sEngine, cMaxResults));
                }
            }

        return config;
        }

    /**
     * Create a collection of AggregatorConfig objects from the given XML
     * configuration.
     *
     * @param xml  the XML configuration
     *
     * @return a collection of AggregatorConfig objects
     */
    protected Collection<AggregatorConfig> createAggregatorMap(XmlElement xml)
        {
        Collection<AggregatorConfig> colAggregators = new LinkedList<AggregatorConfig>();
        for (Iterator iter = xml.getElements("aggregator"); iter.hasNext(); )
            {
            XmlElement xmlAggregator = (XmlElement) iter.next();

            String sName  = xmlAggregator.getSafeElement("name").getString();
            String sClass = xmlAggregator.getSafeElement("class-name").getString();

            Class clz;
            try
                {
                clz = getContextClassLoader().loadClass(sClass);
                }
            catch (ClassNotFoundException e)
                {
                throw Base.ensureRuntimeException(e, "class \"" + sClass
                        + "\" not found while initializing REST configuration");
                }

            colAggregators.add(new AggregatorConfig(sName, clz));
            }
        return colAggregators;
        }

    /**
     * Create a collection of ProcessorConfig objects from the given XML
     * configuration.
     *
     * @param xml  the XML configuration
     *
     * @return a collection of ProcessorConfig objects
     */
    protected Collection<ProcessorConfig> createProcessorMap(XmlElement xml)
        {
        Collection<ProcessorConfig> colProcessors = new LinkedList<ProcessorConfig>();
        for (Iterator iter = xml.getElements("processor"); iter.hasNext(); )
            {
            XmlElement xmlProcessor = (XmlElement) iter.next();

            String sName  = xmlProcessor.getSafeElement("name").getString();
            String sClass = xmlProcessor.getSafeElement("class-name").getString();

            Class clz;
            try
                {
                clz = getContextClassLoader().loadClass(sClass);
                }
            catch (ClassNotFoundException e)
                {
                throw Base.ensureRuntimeException(e, "class \"" + sClass
                        + "\" not found while initializing REST configuration");
                }

            colProcessors.add(new ProcessorConfig(sName, clz));
            }
        return colProcessors;
        }

    /**
     * Create a collection of {@link QueryEngineConfig} objects from the given
     * XML configuration.
     *
     * @param xml  the XML configuration
     *
     * @return a collection of <tt>QueryEngineConfig</tt> objects
     */
    protected Collection<QueryEngineConfig> createQueryEngines(XmlElement xml)
        {
        Collection<QueryEngineConfig> colQueryEngines = new ArrayList<QueryEngineConfig>();

        for (Iterator iter = xml.getElements("engine"); iter.hasNext(); )
            {
            XmlElement xmlEngine = (XmlElement) iter.next();

            String sName  = xmlEngine.getSafeElement("name").getString();
            String sClass = xmlEngine.getSafeElement("class-name").getString();

            Class clz;
            try
                {
                clz = getContextClassLoader().loadClass(sClass);
                }
            catch (ClassNotFoundException e)
                {
                throw Base.ensureRuntimeException(e, "class \"" + sClass
                        + "\" not found while initializing REST configuration");
                }

            colQueryEngines.add(new QueryEngineConfig(sName, clz));
            }

        return colQueryEngines;
        }

    /**
     * Return the context class loader to use.
     *
     * @return the context class loader to use
     */
    protected ClassLoader getContextClassLoader()
        {
        return m_service == null
               ? Base.getContextClassLoader(this)
               : m_service.getContextClassLoader();
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return a map of configured resources, keyed by cache name or alias.
     *
     * @return map of configured resources
     */
    public Map<String, ResourceConfig> getResources()
        {
        return m_mapResources;
        }

    /**
     * Return the configured aggregator registry.
     *
     * @return aggregator registry
     */
    public AggregatorRegistry getAggregatorRegistry()
        {
        return m_aggregatorRegistry;
        }

    /**
     * Return the configured processor registry.
     *
     * @return processor registry
     */
    public ProcessorRegistry getProcessorRegistry()
        {
        return m_processorRegistry;
        }

    /**
     * Return the configured marshaller registry.
     *
     * @return marshaller registry
     */
    public MarshallerRegistry getMarshallerRegistry()
        {
        return m_marshallerRegistry;
        }

    /**
     * Return the configured query engine registry.
     *
     * @return query engine registry
     */
    public QueryEngineRegistry getQueryEngineRegistry()
        {
        return m_queryEngineRegistry;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The default name of the Coherence REST XML descriptor.
     */
    public static final String DESCRIPTOR_NAME = "coherence-rest-config.xml";

     /**
     * The name of the system property that can be used to override the
     * default Coherence REST configuration file {@value RestConfig#DESCRIPTOR_NAME}.
     */
    public static final String DESCRIPTOR_PROPERTY = "coherence.rest.config";

    // ----- data members ---------------------------------------------------

    /**
     * Map of resources, keyed by cache name or alias.
     */
    private Map<String, ResourceConfig> m_mapResources
            = new HashMap<>();

    /**
     * The Coherence service this REST application is deployed to.
     */
    @Inject
    private Service m_service;
    /**
     * Aggregator registry.
     */
    @Inject
    private AggregatorRegistry m_aggregatorRegistry;

    /**
     * Processor registry.
     */
    @Inject
    private ProcessorRegistry m_processorRegistry;

    /**
     * Marshaller registry.
     */
    @Inject
    private MarshallerRegistry m_marshallerRegistry;

    /**
     * Query engine registry.
     */
    @Inject
    private QueryEngineRegistry m_queryEngineRegistry;
    }
