/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.oracle.coherence.common.base.Classes;
import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.CacheMapping;
import com.tangosol.coherence.config.CacheMappingRegistry;
import com.tangosol.coherence.config.ResourceMapping;
import com.tangosol.coherence.config.ResourceMappingRegistry;
import com.tangosol.coherence.config.ServiceSchemeRegistry;
import com.tangosol.coherence.config.TopicMapping;

import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.NamedResourceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;

import com.tangosol.coherence.config.scheme.CachingScheme;
import com.tangosol.coherence.config.scheme.ClassScheme;
import com.tangosol.coherence.config.scheme.ExternalScheme;
import com.tangosol.coherence.config.scheme.FlashJournalScheme;
import com.tangosol.coherence.config.scheme.InvocationScheme;
import com.tangosol.coherence.config.scheme.LocalScheme;
import com.tangosol.coherence.config.scheme.ObservableCachingScheme;
import com.tangosol.coherence.config.scheme.OverflowScheme;
import com.tangosol.coherence.config.scheme.RamJournalScheme;
import com.tangosol.coherence.config.scheme.ReadWriteBackingMapScheme;
import com.tangosol.coherence.config.scheme.RemoteInvocationScheme;
import com.tangosol.coherence.config.scheme.ServiceScheme;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.Value;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.NamedCollection;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.ValueTypeAssertion;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.UUID;

import java.util.Map;

/**
 * An {@link InitParamProcessor} is responsible for processing &lt;init-param&gt; {@link XmlElement}s to produce
 * {@link Parameter}s.
 *
 * @author bo  2011.06.24
 * @since Coherence 12.1.2
 */
@XmlSimpleName("init-param")
public class InitParamProcessor
        implements ElementProcessor<Parameter>
    {
    // ----- ElementProcessor interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Parameter process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        // when param-name is undefined, use a newly generated UUID
        String        sName = context.getOptionalProperty("param-name", String.class, new UUID().toString(), element);
        Expression<?> exprValue = context.getMandatoryProperty("param-value", Expression.class, element);
        String        sType     = context.getOptionalProperty("param-type", String.class, null, element);

        // handle the special case when the parameter type is a reference to another cache.
        Class<?> clzType = null;

        if (sType != null && sType.equals("{cache-ref}"))
            {
            Expression<?>           exprCacheName = exprValue;
            ClassLoader             loader        = context.getContextClassLoader();
            ResourceMappingRegistry registry      = context.getCookie(CacheConfig.class).getMappingRegistry();

            clzType   = NamedCache.class;
            exprValue = new CacheRefExpression(exprCacheName, loader, registry);
            }
        else if (sType != null && sType.equals("{destination-ref}"))
            {
            Expression<?>           exprCacheName = exprValue;
            ClassLoader             loader        = context.getContextClassLoader();
            ResourceMappingRegistry registry      = context.getCookie(CacheConfig.class).getMappingRegistry();

            clzType   = NamedTopic.class;
            exprValue = new CollectionRefExpression(exprCacheName, loader, registry);
            }
        else if (sType != null && sType.equals("{scheme-ref}"))
            {
            Expression<?>         exprSchemeName = exprValue;
            ServiceSchemeRegistry registry       = context.getCookie(CacheConfig.class).getServiceSchemeRegistry();

            // we use Object.class here as we don't know the type of value the scheme-ref will produce.
            clzType   = Object.class;
            exprValue = new SchemeRefExpression(exprSchemeName, registry);
            }
        else if (sType != null && sType.equals("{resource}"))
            {
            Expression<?>                exprResourceName = exprValue;
            ParameterizedBuilderRegistry registry         = context.getCookie(ParameterizedBuilderRegistry.class);

            if (registry == null)
                {
                // grab the operational context from which we can look up the serializer
                OperationalContext ctxOperational = context.getCookie(OperationalContext.class);
                if (ctxOperational == null)
                    {
                    throw new ConfigurationException("Attempted to resolve the OperationalContext in [" + element
                        + "] but it was not defined", "The registered ElementHandler for the <" + element.getName()
                        + "> element is not operating in an OperationalContext");
                    }
                registry = ctxOperational.getBuilderRegistry();
                }

            exprValue = new ResourceRefExpression(exprResourceName, registry);
            }
        else
            {
            // attempt to load the specified class
            if (sType != null)
                {
                try
                    {
                    clzType = context.getContextClassLoader().loadClass(sType);
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new ConfigurationException(String.format(
                        "Failed to resolve the specified param-type in [%s]",
                        element), "Please ensure that the specified class is publicly accessible via the class path",
                                  e);
                    }
                }
            }

        return new Parameter(sName, clzType, exprValue);
        }

    // ----- inner class: DataStructureRefExpression ------------------------

    /**
     * An {@link Expression} implementation that represents the use of
     * a {cache-ref} macro in a Cache Configuration File.
     */
    public static abstract class DataStructureRefExpression<E>
            implements Expression<E>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a DataStructureRefExpression.
         *
         * @param exprCacheName  the Expression representing the Cache Name
         * @param classLoader    the ClassLoader to use when loading the Cache
         * @param registry       the ResourceMappingRegistry to locate CacheMapping/TopicMapping
         *                       definitions
         */
        public DataStructureRefExpression(Expression<?> exprCacheName, ClassLoader classLoader,
                ResourceMappingRegistry registry)
            {
            m_exprCacheName            = exprCacheName;
            m_classLoader              = classLoader;
            m_registryResourceMappings = registry;
            }

        // ----- DataStructureRefExpression methods -------------------------

        @SuppressWarnings("StatementWithEmptyBody")
        public String evaluateName(ParameterResolver                resolver,
                                   Class<? extends ResourceMapping> clsType,
                                   String                           sElementName)
            {
            String sDesiredName = new Value(m_exprCacheName.evaluate(resolver)).as(String.class);

            if (sDesiredName != null && sDesiredName.contains("*"))
                {
                // the desired name contains a wildcard * and as
                // such we need to replace that with the string that
                // matched the wildcard * in the parameterized name

                // the parameter resolver will have the name
                Parameter paramCacheName = resolver.resolve(sElementName);
                String sCacheName = paramCacheName == null ? null : paramCacheName.evaluate(resolver).as(String.class);

                if (sCacheName == null)
                    {
                    // as we can't resolve the parameterized cache-name
                    // we'll just use the desired name as is.
                    }
                else
                    {
                    ResourceMapping mapping = m_registryResourceMappings.findMapping(sCacheName, clsType);

                    if (mapping.usesWildcard())
                        {
                        String sWildcardValue = mapping.getWildcardMatch(sCacheName);

                        sDesiredName = sDesiredName.replaceAll("\\*", sWildcardValue);
                        }
                    else
                        {
                        // as the parameterized cache-name doesn't use a wildcard
                        // we'll just use the desired name as is.
                        }
                    }
                }

            return sDesiredName;
            }

        public ClassLoader getClassLoader()
            {
            return m_classLoader;
            }

        // ----- Object interface ---------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            return String.format(getClass().getSimpleName() + "{exprCacheName=%s}", m_exprCacheName);
            }

        // ----- data members -----------------------------------------------

        /**
         * The Expression that specifies the Cache Name to resolve.
         */
        private final Expression<?> m_exprCacheName;

        /**
         * The ClassLoader to use to resolve/load the underlying Cache.
         */
        private final ClassLoader m_classLoader;

        /**
         * The ResourceMappingRegistry to use to lookup CacheMapping/TopicMapping definitions.
         */
        private final ResourceMappingRegistry m_registryResourceMappings;
        }

    // ----- inner class: CacheRefExpression --------------------------------

    /**
     * An {@link Expression} implementation that represents the use of
     * a {cache-ref} macro in a Configuration File.
     */
    public static class CacheRefExpression
            extends DataStructureRefExpression<NamedCache<?, ?>>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a CacheRefExpression.
         *
         * @param exprCacheName  the Expression representing the Cache Name
         * @param classLoader    the ClassLoader to use when loading the Cache
         * @param registry       the ResourceMappingRegistry to locate ResourceMapping
         *                       definitions
         */
        public CacheRefExpression(Expression<?> exprCacheName, ClassLoader classLoader, ResourceMappingRegistry registry)
            {
            super(exprCacheName, classLoader, registry);
            }

        /**
         * Constructs a CacheRefExpression.
         *
         * @param exprCacheName  the Expression representing the Cache Name
         * @param classLoader    the ClassLoader to use when loading the Cache
         * @param registry       the CacheMappingRegistry to locate CacheMapping
         *                       definitions
         *
         * @deprecated As of Coherence 14.1.1, use {@link #CacheRefExpression(Expression, ClassLoader, ResourceMappingRegistry)}.
         */
        public CacheRefExpression(Expression<?> exprCacheName, ClassLoader classLoader, CacheMappingRegistry registry)
            {
            super(exprCacheName, classLoader, registry.getMappingRegistry());
            }

        // ----- Expression interface ---------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public NamedCache<?, ?> evaluate(ParameterResolver resolver)
            {
            String                   sName     = evaluateName(resolver, CacheMapping.class, "cache-name");
            ClassLoader              loader    = getClassLoader();
            Parameter                parameter = resolver.resolve("manager-context");
            BackingMapManagerContext ctx       = (BackingMapManagerContext) parameter.evaluate(resolver).get();
            ConfigurableCacheFactory ccf       = ctx == null
                ? CacheFactory.getConfigurableCacheFactory(loader)
                : ctx.getManager().getCacheFactory();

            return ccf.ensureCache(sName, loader);
            }
        }

    // ----- inner-class: CollectionRefExpression ---------------------------

    /**
     * An {@link Expression} implementation that represents the use of
     * a {collection-ref} macro in a Configuration File.
     */
    public static class CollectionRefExpression
            extends DataStructureRefExpression<NamedCollection>
        {
        // ----- constructors ----------------------------------------------------

        /**
         * Constructs a CollectionRefExpression.
         *
         * @param exprCacheName  the Expression representing the collection name
         * @param classLoader    the ClassLoader to use when loading the collection
         * @param registry       the ResourceMappingRegistry to locate QueueMapping
         *                       definitions
         */
        public CollectionRefExpression(Expression<?> exprCacheName, ClassLoader classLoader, ResourceMappingRegistry registry)
            {
            super(exprCacheName, classLoader, registry);
            }

        // ----- Expression interface ---------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public NamedCollection evaluate(ParameterResolver resolver)
            {
            String                             sName       = evaluateName(resolver, TopicMapping.class, "topic-name");
            ClassLoader                        classLoader = getClassLoader();
            Parameter                          parameter   = resolver.resolve("manager-context");
            BackingMapManagerContext           ctx         = (BackingMapManagerContext) parameter.evaluate(resolver).get();
            ExtensibleConfigurableCacheFactory eccf        = ctx == null
                ? (ExtensibleConfigurableCacheFactory) CacheFactory.getConfigurableCacheFactory(classLoader)
                : (ExtensibleConfigurableCacheFactory) ctx.getManager().getCacheFactory();

            return eccf.ensureTopic(sName, classLoader, ValueTypeAssertion.withRawTypes());
            }
        }

    // ----- inner class: SchemeRefExpression -------------------------------

    /**
     * An {@link Expression} implementation that represents the use of
     * a {scheme-ref} macro in a Cache Configuration File.
     */
    public static class SchemeRefExpression
            implements Expression<Object>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a SchemeRefExpression.
         *
         * @param exprSchemeName  the name of the ServiceScheme to resolve
         * @param registry        the ServiceSchemeRegistry to lookup ServiceSchemes
         */
        public SchemeRefExpression(Expression<?> exprSchemeName, ServiceSchemeRegistry registry)
            {
            m_exprSchemeName = exprSchemeName;
            m_registry       = registry;
            }

        // ----- Expression interface ---------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public Object evaluate(ParameterResolver resolver)
            {
            String        sSchemeName = new Value(m_exprSchemeName.evaluate(resolver)).as(String.class);
            ServiceScheme scheme      = m_registry.findSchemeBySchemeName(sSchemeName);

            // the parameter resolver will have the classloader
            Parameter paramClassLoader = resolver.resolve("class-loader");
            ClassLoader classLoader = paramClassLoader == null
                                      ? Classes.getContextClassLoader()
                                      : paramClassLoader.evaluate(resolver).as(ClassLoader.class);

            if (scheme instanceof InvocationScheme)
                {
                return ((InvocationScheme) scheme).realizeService(resolver, classLoader, CacheFactory.getCluster());
                }
            if (scheme instanceof RemoteInvocationScheme)
                {
                return ((RemoteInvocationScheme) scheme).realizeService(resolver, classLoader, CacheFactory.getCluster());
                }
            else if (scheme instanceof ClassScheme)
                {
                return ((ClassScheme) scheme).realize(resolver, classLoader, null);
                }
            else if (scheme instanceof CachingScheme)
                {
                CachingScheme cachingScheme = (CachingScheme) scheme;

                // construct MapBuilder.Dependencies so that we can realize the caching scheme/backing map
                ConfigurableCacheFactory ccf = CacheFactory.getConfigurableCacheFactory();

                // the parameter resolver will have the cache-name
                Parameter paramCacheName = resolver.resolve("cache-name");
                String sCacheName = paramCacheName == null ? null : paramCacheName.evaluate(resolver).as(String.class);

                // the parameter resolver will have the manager-context
                Parameter paramBMMC = resolver.resolve("manager-context");
                BackingMapManagerContext bmmc = paramBMMC == null
                                                ? null
                                                : paramBMMC.evaluate(resolver).as(BackingMapManagerContext.class);

                MapBuilder.Dependencies dependencies = new MapBuilder.Dependencies(ccf, bmmc, classLoader, sCacheName,
                                                           cachingScheme.getServiceType());

                // the resulting map/cache
                Map<?, ?> map;

                if (scheme instanceof LocalScheme || scheme instanceof OverflowScheme
                    || scheme instanceof ExternalScheme || scheme instanceof ReadWriteBackingMapScheme
                    || scheme instanceof FlashJournalScheme || scheme instanceof RamJournalScheme)
                    {
                    // for "local" schemes we realize a backing map
                    map = cachingScheme.realizeMap(resolver, dependencies);
                    }
                else
                    {
                    // for anything else, we realize the cache
                    // NOTE: this cache won't be registered or visible
                    // via ensureCache/getCache
                    map = cachingScheme.realizeCache(resolver, dependencies);
                    }

                // add the listeners when the scheme is observable
                if (cachingScheme instanceof ObservableCachingScheme)
                    {
                    ObservableCachingScheme observableScheme = (ObservableCachingScheme) cachingScheme;

                    observableScheme.establishMapListeners(map, resolver, dependencies);
                    }

                return map;
                }
            else
                {
                throw new ConfigurationException(String.format("Failed to resolve the {scheme-ref} name [%s]",
                    sSchemeName), "Please ensure that the specified {scheme-ref} refers to a defined <scheme-name>");
                }
            }

        // ----- Object interface -------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            return String.format("SchemeRefExpression{exprSchemeName=%s}", m_exprSchemeName);
            }

        // ----- data members -----------------------------------------------

        /**
         * The Expression that specifies the Scheme Name to resolve.
         */
        private final Expression<?> m_exprSchemeName;

        /**
         * The ServiceSchemeRegistry to use to lookup ServiceSchemes.
         */
        private final ServiceSchemeRegistry m_registry;
        }

    // ----- inner class: ResourceRefExpression -----------------------------

    /**
     * An {@link Expression} implementation that represents the use of
     * a {resource} macro in a configuration File.
     */
    public static class ResourceRefExpression
            implements Expression<Object>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a SchemeRefExpression.
         *
         * @param exprResourceName  the name of the ServiceScheme to resolve
         * @param registry        the ServiceSchemeRegistry to lookup ServiceSchemes
         */
        public ResourceRefExpression(Expression<?> exprResourceName, ParameterizedBuilderRegistry registry)
            {
            m_exprResourceName = exprResourceName;
            m_registry         = registry;
            }

        // ----- Expression interface ---------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public Object evaluate(ParameterResolver resolver)
            {
            String                  sName        = new Value(m_exprResourceName.evaluate(resolver)).as(String.class);
            NamedResourceBuilder<?> namedBuilder = (NamedResourceBuilder<?>) m_registry.getBuilder(NamedResourceBuilder.class, sName);

            if (namedBuilder == null)
                {
                throw new ConfigurationException("No cluster resource has been configured with the name " + sName,
                        "Please define a valid named {resource} parameter");
                }

            // the parameter resolver will have the classloader
            Parameter paramClassLoader = resolver.resolve("class-loader");
            ClassLoader classLoader = paramClassLoader == null
                                      ? Classes.getContextClassLoader()
                                      : paramClassLoader.evaluate(resolver).as(ClassLoader.class);

            return namedBuilder.realize(resolver, classLoader, null);
            }

        // ----- Object interface -------------------------------------------

        @Override
        public String toString()
            {
            return String.format("ResourceRefExpression{exprResourceName=%s}", m_exprResourceName);
            }

        // ----- data members -----------------------------------------------

        /**
         * The Expression that specifies the resource name to resolve.
         */
        private final Expression<?> m_exprResourceName;

        /**
         * The {@link ParameterizedBuilderRegistry} to use to lookup ServiceSchemes.
         */
        private final ParameterizedBuilderRegistry m_registry;
        }
    }
