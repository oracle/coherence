/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.tangosol.application.ContainerContext;
import com.tangosol.application.Context;

import com.tangosol.coherence.config.builder.NamedEventInterceptorBuilder;
import com.tangosol.coherence.config.scheme.CachingScheme;
import com.tangosol.coherence.config.scheme.NamedTopicScheme;
import com.tangosol.coherence.config.scheme.ServiceScheme;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.util.Base;
import com.tangosol.util.ResourceRegistry;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * A {@link CacheConfig} is the top-level container for Coherence Cache
 * Configuration, that of which is used at runtime to establish caches and
 * services.
 *
 * @author pfm 2011.12.2
 * @since Coherence 12.1.2
 */
public class CacheConfig
    {
    // ----- constructors  --------------------------------------------------

    /**
     * Construct a {@link CacheConfig}.
     */
    public CacheConfig(ParameterResolver defaultParameterResolver)
        {
        // by default cache configurations aren't scoped
        m_sScopeName = "";

        // by default the registries are empty
        m_registrySchemeMapping        = new SchemeMappingRegistry();
        m_registrySchemeRegistry       = new ServiceSchemeRegistry();
        m_listEventInterceptorBuilders = new LinkedList<>();

        // remember the default parameter resolver for the configuration
        m_parameterResolver = defaultParameterResolver;
        }

    // ----- CacheConfig methods  -------------------------------------------

    /**
     * Obtain the scope name of the {@link CacheConfig}.
     */
    public String getScopeName()
        {
        return m_sScopeName;
        }

    /**
     * Set the scope name of this {@link CacheConfig} (which will be trimmed)
     *
     * @param sScopeName  the scope name
     */
    @Injectable("scope-name")
    @Deprecated
    public void setScopeName(String sScopeName)
        {
        // the top level "scope-name" is deprecated as of 12.1.3;
        // we maintain this only for backward compatibility
        m_sScopeName = sScopeName == null ? null : sScopeName.trim();
        }

    /**
     * Obtain the {@link List} of {@link NamedEventInterceptorBuilder}s for
     * this {@link CacheConfig}.
     *
     * @return a List of NamedEventInterceptorBuilders or {@code null} if unspecified
     */
    public List<NamedEventInterceptorBuilder> getEventInterceptorBuilders()
        {
        return m_listEventInterceptorBuilders;
        }

    /**
     * Set the {@link List} of {@link NamedEventInterceptorBuilder}s for this
     * {@link CacheConfig}.
     *
     * @param listBuilders  the List of NamedEventInterceptorBuilders for this
     *                      CacheConfig
     */
    @Injectable("interceptors")
    public void setEventInterceptorBuilders(List<NamedEventInterceptorBuilder> listBuilders)
        {
        m_listEventInterceptorBuilders = listBuilders;
        }

    /**
     * Obtain the cache/topic {@link ResourceMappingRegistry} for the {@link CacheConfig}.
     *
     * @return the cache/topic {@link ResourceMappingRegistry}
     *
     * @since Coherence 14.1.1
     */
    public ResourceMappingRegistry getMappingRegistry()
        {
        return m_registrySchemeMapping;
        }

    /**
     * Obtain the {@link CacheMappingRegistry} for the {@link CacheConfig}.
     *
     * @return the {@link CacheMappingRegistry}
     *
     * @deprecated  As of Coherence 14.1.1, replaced by {@link #getMappingRegistry()}.
     */
    public CacheMappingRegistry getCacheMappingRegistry()
        {
        return new CacheMappingRegistry(getMappingRegistry());
        }

    /**
     * Set the {@link CacheMappingRegistry}.
     *
     * @param registry  the {@link CacheMappingRegistry}
     *
     * @deprecated  As of Coherence 14.1.1, replaced by {@link #addCacheMappingRegistry(SchemeMappingRegistry)}.
     */
    public void setCacheMappingRegistry(CacheMappingRegistry registry)
        {
        if (registry == null)
            {
            return;
            }

        SchemeMappingRegistry schemeMappingRegistry = m_registrySchemeMapping;

        if (schemeMappingRegistry == null)
            {
            m_registrySchemeMapping = new SchemeMappingRegistry();
            }
        for (CacheMapping mapping : registry)
            {
            if (schemeMappingRegistry.findMapping(mapping.getNamePattern(), mapping.getClass()) == null)
                {
                schemeMappingRegistry.register(mapping);
                }
            }
        }

    /**
     * Add cache scheme mappings to the {@link SchemeMappingRegistry}.
     *
     * @param registry  the {@link SchemeMappingRegistry}
     *
     * @since Coherence 14.1.1
     */
    @Injectable("caching-scheme-mapping")
    public void addCacheMappingRegistry(SchemeMappingRegistry registry)
        {
        addRegistrySchemeMapping(registry);
        }

    /**
     * Add topic scheme mappings to scheme {@link SchemeMappingRegistry}
     * if no mapping already exists for the same patter.
     *
     * @param registry  the {@link SchemeMappingRegistry}
     *
     * @since Coherence 14.1.1
     */
    @Injectable("topic-scheme-mapping")
    public void addRegistrySchemeMapping(SchemeMappingRegistry registry)
        {
        if (registry == null)
            {
            return;
            }

        SchemeMappingRegistry schemeMappingRegistry = m_registrySchemeMapping;

        if (schemeMappingRegistry == null)
            {
            m_registrySchemeMapping = registry;
            }
        else
            {
            for (ResourceMapping mapping : registry)
                {
                if (schemeMappingRegistry.findMapping(mapping.getNamePattern(), mapping.getClass()) == null)
                    {
                    schemeMappingRegistry.register(mapping);
                    }
                }
            }
        }

    /**
     * Obtain the {@link ServiceSchemeRegistry} for the {@link CacheConfig}.
     *
     * @return the {@link ServiceSchemeRegistry}
     */
    public ServiceSchemeRegistry getServiceSchemeRegistry()
        {
        return m_registrySchemeRegistry;
        }

    /**
     * Set the {@link ServiceSchemeRegistry} for the {@link CacheConfig}.
     *
     * @param registry  the {@link ServiceSchemeRegistry}
     */
    @Injectable("caching-schemes")
    public void setServiceSchemeRegistry(ServiceSchemeRegistry registry)
        {
        m_registrySchemeRegistry = registry;
        }

    /**
     * Find the {@link CachingScheme} for the specified cache name.
     *
     * @param sCacheName  the cache name
     *
     * @return the {@link CachingScheme} or <code>null</code> if not found
     */
    public CachingScheme findSchemeByCacheName(String sCacheName)
        {
        // get name of the scheme used by the cache
        ResourceMappingRegistry registry = getMappingRegistry();
        CacheMapping            mapping  = registry.findMapping(sCacheName, CacheMapping.class);

        if (mapping == null)
            {
            return null;
            }
        else
            {
            String        sSchemeName   = mapping.getSchemeName();
            ServiceScheme serviceScheme = findSchemeBySchemeName(sSchemeName);

            return serviceScheme instanceof CachingScheme ? (CachingScheme) serviceScheme : null;
            }
        }

    /**
     * Find the {@link CachingScheme} for the specified topic name.
     *
     * @param sTopicName  the topic name
     *
     * @return the {@link NamedTopicScheme} or <code>null</code> if not found
     *
     * @since Coherence 14.1.1
     */
    public NamedTopicScheme findSchemeByTopicName(String sTopicName)
        {
        // get name of the scheme used by the topic
        ResourceMappingRegistry registry = getMappingRegistry();
        TopicMapping            mapping  = registry.findMapping(sTopicName, TopicMapping.class);

        if (mapping == null)
            {
            return null;
            }
        else
            {
            String        sSchemeName   = mapping.getSchemeName();
            ServiceScheme serviceScheme = findSchemeBySchemeName(sSchemeName);

            return serviceScheme instanceof NamedTopicScheme
                    ? (NamedTopicScheme) serviceScheme : null;
            }
        }

    /**
     * Find the {@link ServiceScheme} given the service name.
     *
     * @param sServiceName  the service name to match
     *
     * @return the {@link ServiceScheme} or null
     */
    public ServiceScheme findSchemeByServiceName(String sServiceName)
        {
        return getServiceSchemeRegistry().findSchemeByServiceName(sServiceName);
        }

    /**
     * Find the {@link ServiceScheme} given the scheme name.
     *
     * @param sSchemeName  the scheme name to match
     *
     * @return the {@link ServiceScheme} or null
     */
    public ServiceScheme findSchemeBySchemeName(String sSchemeName)
        {
        return getServiceSchemeRegistry().findSchemeBySchemeName(sSchemeName);
        }

    /**
     * Obtain the {@link ParameterResolver} to use for the {@link CacheConfig}
     * when no other is available or in context.
     *
     * @return  the default {@link ParameterResolver}
     */
    public ParameterResolver getDefaultParameterResolver()
        {
        return m_parameterResolver;
        }

    // ----- internal -------------------------------------------------------

    /**
     * Validate the cache configuration.
     *
     * @param registry the ResourceRegistry associated with this configuration.
     *
     * @return this object
     */
    public CacheConfig validate(ResourceRegistry registry)
        {
        ResourceMappingRegistry regMapping = getMappingRegistry();
        ServiceSchemeRegistry   regSchemes = getServiceSchemeRegistry();

        Base.checkNotNull(regMapping, "ResourceMappingRegistry");
        Base.checkNotNull(regSchemes, "ServiceSchemeRegistry");

        Context          ctxApp  = registry.getResource(Context.class);
        ContainerContext ctxCont = ctxApp == null ? null : ctxApp.getContainerContext();

        // Ensure mappings map to valid schemes
        for (ResourceMapping mapping : regMapping)
            {
            String sSchemeName = mapping.getSchemeName();
            ServiceScheme scheme = regSchemes.findSchemeBySchemeName(sSchemeName);
            if (scheme == null)
                {
                throw new ConfigurationException("Scheme definition missing for scheme " + sSchemeName, "Provide the scheme definition.");
                }
            mapping.validateScheme(scheme);
            }

        if (ctxCont != null)
            {
            // ensure that the configuration is non-ambiguous within the associated
            // ContainerContext, which means that none of the service schemes are
            // used as both scoped and shared

            // collect all mapped service schemes
            Set<ResourceMapping> setScopeMapping = new HashSet<>();
            for (ResourceMapping mapping : regMapping)
                {
                setScopeMapping.add(mapping);
                }

            // extract scheme names for shared mappings
            Set<String> setSharedScheme = new HashSet<>();
            Set<String> setSharedCaches = ctxCont.getSharedCaches();

            if (setSharedCaches != null && !setSharedCaches.isEmpty())
                {
                for (String sCache : setSharedCaches)
                    {
                    CacheMapping mapping = regMapping.findCacheMapping(sCache);
                    if (mapping != null)
                        {
                        setScopeMapping.remove(mapping);
                        setSharedScheme.add(mapping.getSchemeName());
                        }
                    }
                }

            // ensure there is no intersection - this would cause the same ServiceScheme
            // to be used to configure both scoped and shared services, which is
            // not allowable (see AbstractServiceScheme.realizeService)
            for (ResourceMapping mapping : setScopeMapping)
                {
                String sScheme = mapping.getSchemeName();
                if (setSharedScheme.contains(sScheme) && !mapping.isInternal())
                    {
                    // the scheme was referred by both shared and scoped mapping
                    throw new IllegalStateException("Ambiguous configuration: scheme \""
                        + sScheme + "\" is referred to by both shared and non-shared cache mappings.");
                    }
                }
            }

        return this;
        }

    // ----- constants ------------------------------------------------------

    /**
     *  Top-level element name.
     */
    public final static String TOP_LEVEL_ELEMENT_NAME = "cache-config";

    // ----- data members ---------------------------------------------------

    /**
     * The scope name.
     */
    private String m_sScopeName;

    /**
     * The topic/cache mapping to scheme {@link SchemeMappingRegistry} of the {@link CacheConfig}.
     */
    private SchemeMappingRegistry m_registrySchemeMapping;

    /**
     * The {@link ServiceSchemeRegistry} for the {@link CacheConfig}.
     */
    private ServiceSchemeRegistry m_registrySchemeRegistry;

    /**
     * The default {@link ParameterResolver} to be used for resolving
     * parameters in expressions.  This is typically used when attempting
     * to resolve expressions outside of a cache context.
     */
    private ParameterResolver m_parameterResolver;

    /**
     * The {@link List} of {@link NamedEventInterceptorBuilder}s associated
     * with this {@link CacheConfig}.
     */
    private List<NamedEventInterceptorBuilder> m_listEventInterceptorBuilders;
    }
