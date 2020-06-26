/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.net.InetAddresses;
import com.oracle.coherence.common.util.Duration;
import com.oracle.coherence.common.util.MemorySize;

import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.NamedEventInterceptorBuilder;
import com.tangosol.coherence.config.builder.UnitCalculatorBuilder;
import com.tangosol.coherence.config.unit.Seconds;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.injection.SimpleInjector;

import com.tangosol.internal.net.topic.impl.paged.Configuration;
import com.tangosol.internal.net.topic.impl.paged.PagedTopic;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.Service;
import com.tangosol.net.ValueTypeAssertion;

import com.tangosol.net.cache.LocalCache;

import com.tangosol.net.security.StorageAccessAuthorizer;

import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.ResourceResolver;
import com.tangosol.util.ResourceResolverHelper;

import java.util.List;

/**
 * A {@link PagedTopicScheme} is responsible for building a topic.
 *
 * @author jk 2015.05.21
 * @since Coherence 14.1.1
 */
public class PagedTopicScheme
        extends DistributedScheme
        implements NamedTopicScheme
    {
    // ----- AbstractServiceScheme methods ----------------------------------

    /**
     * DefaultServiceName to use if none configured.
     *
     * @return default service name
     */
    @Override
    protected String getDefaultServiceName()
        {
        // override default service type name of "DistributedCache"
        return DEFAULT_SERVICE_NAME;
        }

    // ----- TopicScheme methods --------------------------------------------

    @Override
    public <T extends NamedCollection> boolean realizes(Class<T> type)
        {
        return NamedTopic.class.equals(type);
        }

    /**
     * Return the {@link BackingMapScheme} used for the storage of this scheme.

     * @param resolver potentially override default unit-calculator of BINARY
     *
     * @return the scheme
     */
    public CachingScheme getStorageScheme(ParameterResolver resolver)
        {
        if (m_schemeBackingMap == null)
            {
            // default storage scheme
            LocalScheme scheme = new LocalScheme();

            // NOTE: we don't set the scheme's high-units as topic data isn't subject to eviction
            // but we do set the calculator and factor so that the topic impl can evaluate the size
            // and throttle publishers accordingly
            scheme.setUnitCalculatorBuilder(getUnitCalculatorBuilder(resolver));
            long cbHigh = getHighUnits(resolver);
            if (cbHigh >= Integer.MAX_VALUE)
                {
                scheme.setUnitFactor((r) -> 1024);
                }
            m_schemeBackingMap = scheme;
            }
        return m_schemeBackingMap;
        }

    /**
     * Return the {@link BackingMapScheme} used for the storage of this scheme.
     *
     * @return the scheme
     */
    public CachingScheme getStorageScheme()
        {
        return getStorageScheme(NULL_PARAMETER_RESOLVER);
        }

    /**
     * Set the {@link BackingMapScheme} which builds the backing map for
     * the internal caches used to implement this scheme.
     *
     * @param scheme  the scheme builder
     */
    @Injectable("storage")
    public void setStorageScheme(CachingScheme scheme)
        {
        m_schemeBackingMap = scheme;
        }

    /**
     * Return the binary limit of the size of a page in a topic. Contains the target number
     * of bytes that can be placed in a page before the page is deemed to be full.
     * Legal values are positive integers.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the page size
     */
    public int getPageSize(ParameterResolver resolver)
        {
        return (int) m_exprPageSize.evaluate(resolver).getByteCount();
        }

    /**
     * Set the page size.
     *
     * @param expr  the page high units expression
     */
    @Injectable("page-size")
    public void setPageSize(Expression<MemorySize> expr)
        {
        m_exprPageSize = expr;
        }

    /**
     * Return the high-units
     *
     * @param resolver  the ParameterResolver
     *
     * @return the unit factor
     */
    public long getHighUnits(ParameterResolver resolver)
        {
        return m_exprHighUnits.evaluate(resolver).getByteCount();
        }

    /**
     * Set the high-units
     *
     * @param expr  the high-units expression
     */
    @Injectable("high-units")
    public void setHighUnits(Expression<MemorySize> expr)
        {
        m_exprHighUnits = expr;
        }

    /**
     * Return the {@link Expression} transient. to use to determine
     * whether the backing map is transient.
     *
     * @return the {@link Expression} transient. to use to determine
     *         whether the backing map is transient
     */
    public Expression<Boolean> getTransientExpression()
        {
        return m_exprTransient;
        }

    /**
     * Set the transient flag.
     *
     * @param expr  true to make the backing map transient.
     */
    @Injectable
    public void setTransient(Expression<Boolean> expr)
        {
        m_exprTransient = expr;
        }

    /**
     * Obtains the {@link Expression} defining the name of the {@link StorageAccessAuthorizer}.
     *
     * @return the name of the {@link StorageAccessAuthorizer} or <code>null</code> if
     *         one has not been configured.
     */
    public Expression<String> getStorageAccessAuthorizer()
        {
        return m_exprStorageAccessAuthorizer;
        }

    /**
     * Sets the {@link Expression} defining the name of the {@link StorageAccessAuthorizer}.
     *
     *  @param exprStorageAccessAuthorizer  the {@link Expression}
     */
    @Injectable("storage-authorizer")
    public void setStorageAccessAuthorizer(Expression<String> exprStorageAccessAuthorizer)
        {
        m_exprStorageAccessAuthorizer = exprStorageAccessAuthorizer;

        BackingMapScheme scheme = getBackingMapScheme();

        if (scheme != null)
            {
            scheme.setStorageAccessAuthorizer(m_exprStorageAccessAuthorizer);
            }
        }

    /**
     * Return the amount of time that elements offered to the queue remain
     * visible to consumers.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the amount of time that elements offered to the queue remain
     *         visible to consumers
     */
    public Seconds getExpiryDelay(ParameterResolver resolver)
        {
        return m_exprExpiryDelay.evaluate(resolver);
        }

    /**
     * Set the amount of time that elements offered to the queue
     * remain visible to consumers.
     *
     * @param expr  the element expiry delay expression
     */
    @Injectable
    public void setExpiryDelay(Expression<Seconds> expr)
        {
        m_exprExpiryDelay = expr;
        }

    /**
     * Determine whether to retain consumed values.
     *
     * @param resolver  the ParameterResolver
     *
     * @return {@code true} if the topic should retain consumed values
     */
    public boolean isRetainConsumed(ParameterResolver resolver)
        {
        Boolean fRetain = m_exprRetainConsumed.evaluate(resolver);

        return fRetain != null && fRetain;
        }

    /**
     * Set whether to retain consumed values.
     *
     * @param expr  the retain consumed values expression
     */
    @Injectable("retain-consumed")
    public void setRetainConsumed(Expression<Boolean> expr)
        {
        m_exprRetainConsumed = expr;
        }

    @Override
    @Injectable("interceptors")
    public void setEventInterceptorBuilders(List<NamedEventInterceptorBuilder> listBuilders)
        {
        super.setEventInterceptorBuilders(listBuilders);
        }

    // ----- ServiceScheme methods ------------------------------------------

    @Override
    public <V> NamedTopic realize(ValueTypeAssertion<V> typeConstraint,
                                  ParameterResolver resolver, Dependencies deps)
        {
        String           sQueueName  = deps.getCacheName();
        CacheService     service     = ensureConfiguredService(resolver, deps);
        PagedTopicCaches topicCaches = new PagedTopicCaches(sQueueName, service);

        return new PagedTopic<>(topicCaches);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Ensure service and its topic configuration.
     *
     * PagedTopicConfiguration is registered in corresponding service's resource registry.
     *
     * @param resolver       the ParameterResolver
     * @param deps           the {@link MapBuilder} dependencies
     *
     * @return corresponding CacheService for this scheme
     */
    public CacheService ensureConfiguredService(ParameterResolver resolver, Dependencies deps)
        {
        ClassLoader      loader        = deps.getClassLoader();
        String           sTopicName    = PagedTopicCaches.Names.getTopicName(deps.getCacheName());
        CacheService     service       = getOrEnsureService(deps);
        ResourceRegistry registry      = service.getResourceRegistry();
        Configuration    configuration = registry.getResource(Configuration.class, sTopicName);

        if (configuration == null)
            {
            configuration = createConfiguration(resolver, loader);

            registry.registerResource(Configuration.class, sTopicName, configuration);
            }

        return service;
        }

    /**
     * Get or ensure service corresponding to this scheme.
     *
     * Optimized to avoid ensureService synchronization on cluster and service
     * when possible. This behavoir is required on server side. Intermittent deadlock occurs
     * calling ensureService on server side from inside service implementation.
     *
     * @return {@link CacheService}
     */
    private CacheService getOrEnsureService(Dependencies deps)
        {
        ExtensibleConfigurableCacheFactory eccf =
                (ExtensibleConfigurableCacheFactory) deps.getConfigurableCacheFactory();

        Service service = CacheFactory.getCluster().getService(getScopedServiceName());

        if (service == null)
            {
            service = eccf.ensureService(this);
            }

        if (service instanceof CacheService)
            {
            return (CacheService) service;
            }
        else
            {
            throw new IllegalArgumentException("Error: the configured service " + service.getInfo().getServiceName()
                    + " is not a CacheService");
            }
        }


    /**
     * Create a {@link Configuration} based on the values contained in this scheme.
     *
     * @param resolver  the {@link ParameterResolver} to use to resolve configuration values
     * @param loader    the {@link ClassLoader} to use
     *
     * @return  a {@link Configuration} based on the values contained in this scheme
     */
    public Configuration createConfiguration(ParameterResolver resolver, ClassLoader loader)
        {
        // enable topic-mapping init-params to be injected into PagedTopicScheme.
        // still need to support DistributedTopics Service Parameters in future.
        SimpleInjector injector = new SimpleInjector();
        ResourceResolver resourceResolver = ResourceResolverHelper.resourceResolverFrom(PagedTopicScheme.class, this);

        injector.inject(this, ResourceResolverHelper.resourceResolverFrom(resourceResolver, resourceResolver));

        long    cbServer           = getHighUnits(resolver);
        int     cbPage             = getPageSize(resolver);
        long    expiryDelayMillis  = LocalCache.DEFAULT_EXPIRE;
        Seconds expiryDelaySeconds = getExpiryDelay(resolver);
        boolean fRetainConsumed    = isRetainConsumed(resolver);

        if (expiryDelaySeconds != null)
            {
            expiryDelayMillis = expiryDelaySeconds.as(Duration.Magnitude.MILLI);
            }

        Cluster cluster = CacheFactory.getCluster();
        int     nMTU    = InetAddresses.getLocalMTU(cluster.getLocalMember().getAddress());

        if (nMTU == 0)
            {
            nMTU = 1500;
            }

        int nMaxBatchSizeBytes;
        try
            {
            // Detect Overflow.InetAddresses.getLocalMTU(NetworkInterface) returns Integer.MAX_VALUE on windows/jdk11 for loopback.
            nMaxBatchSizeBytes = Math.multiplyExact(nMTU, cluster.getDependencies().getPublisherCloggedCount());
            }
        catch (ArithmeticException e)
            {
            nMaxBatchSizeBytes = Integer.MAX_VALUE;
            }


        Configuration configuration = new Configuration();

        configuration.setServerCapacity(cbServer);
        configuration.setPageCapacity(cbPage);
        configuration.setElementExpiryMillis(expiryDelayMillis);
        configuration.setMaxBatchSizeBytes(Math.min(cbPage, nMaxBatchSizeBytes));
        configuration.setRetainConsumed(fRetainConsumed);
        Logger.finer("PagedTopicScheme configuration: " + configuration);
        return configuration;
        }

    /**
     * Obtain the builder for the unit calculator.
     *
     * @param resolver  the {@link ParameterResolver} to use
     *
     * @return the builder for the unit calculator
     */
    private UnitCalculatorBuilder getUnitCalculatorBuilder(ParameterResolver resolver)
        {
        UnitCalculatorBuilder bldr = new UnitCalculatorBuilder();
        Parameter             parm = resolver.resolve("unit-calculator");
        Expression<String>    expr = parm == null ? new LiteralExpression<>("BINARY") : parm.evaluate(resolver).as(Expression.class);

        bldr.setUnitCalculatorType(expr);

        return bldr;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An empty {@link ParameterResolver}.
     */
    private static final ParameterResolver NULL_PARAMETER_RESOLVER = new NullParameterResolver();

    /**
     * Default service name for PagedTopicScheme, overrides PagedTopicScheme service type which is DistributedCache.
     */
    public static final String DEFAULT_SERVICE_NAME = "DistributedTopic";

    // ----- data members ---------------------------------------------------

    /**
     * The page capacity
     */
    private Expression<MemorySize> m_exprPageSize = new LiteralExpression<>(new MemorySize(Configuration.DEFAULT_PAGE_CAPACITY_BYTES));

    /**
     * The high-units
     */
    private Expression<MemorySize> m_exprHighUnits = new LiteralExpression<>(new MemorySize(0));

    /**
     * The partitioned flag.
     */
    private Expression<Boolean> m_exprTransient = new LiteralExpression<>(Boolean.FALSE);

    /**
     * The name of the StorageAccessAuthorizer to use.
     */
    private Expression<String> m_exprStorageAccessAuthorizer = null;

    /**
     * The backing map scheme for all internal caches used to implement topic.
     */
    private CachingScheme m_schemeBackingMap;

    /**
     * The retain consumed elements flag.
     */
    private Expression<Boolean> m_exprRetainConsumed = new LiteralExpression<>(Boolean.FALSE);

    /**
     * The duration that a value will live in the cache.
     * Zero indicates no timeout.
     */
    private Expression<Seconds> m_exprExpiryDelay = new LiteralExpression<>(new Seconds(0));
    }
