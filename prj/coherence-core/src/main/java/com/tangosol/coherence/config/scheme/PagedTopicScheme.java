/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.common.net.InetAddresses;

import com.oracle.coherence.common.util.Duration;
import com.oracle.coherence.common.util.MemorySize;

import com.oracle.coherence.common.util.Options;
import com.tangosol.coherence.config.builder.ElementCalculatorBuilder;
import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.NamedEventInterceptorBuilder;
import com.tangosol.coherence.config.builder.UnitCalculatorBuilder;

import com.tangosol.coherence.config.unit.Seconds;

import com.tangosol.coherence.config.unit.Units;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.config.injection.SimpleInjector;

import com.tangosol.internal.net.service.grid.DefaultPagedTopicServiceDependencies;
import com.tangosol.internal.net.service.grid.PartitionedCacheDependencies;
import com.tangosol.internal.net.topic.impl.paged.DefaultPagedTopicDependencies;
import com.tangosol.internal.net.topic.impl.paged.PagedTopic;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMapManager;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicDependencies;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;

import com.tangosol.net.BackingMapManager;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.Service;
import com.tangosol.net.TopicService;
import com.tangosol.net.ValueTypeAssertion;

import com.tangosol.net.cache.LocalCache;

import com.tangosol.net.events.annotation.Interceptor;

import com.tangosol.net.security.StorageAccessAuthorizer;

import com.tangosol.net.topic.BinaryElementCalculator;
import com.tangosol.net.topic.FixedElementCalculator;
import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.ResourceResolver;
import com.tangosol.util.ResourceResolverHelper;

import java.util.ArrayList;
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
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link PagedTopicScheme}.
     */
    public PagedTopicScheme()
        {
        super(new DefaultPagedTopicServiceDependencies());
        }

    // ----- ServiceScheme interface  ---------------------------------------

    /**
     * Return the service type.
     */
    @Override
    public String getServiceType()
        {
        return CacheService.TYPE_PAGED_TOPIC;
        }

    // ----- BackingMapManagerBuilder interface -----------------------------

    @Override
    public BackingMapManager realizeBackingMapManager(ConfigurableCacheFactory ccf)
        {
        if (ccf instanceof ExtensibleConfigurableCacheFactory)
            {
            return new PagedTopicBackingMapManager((ExtensibleConfigurableCacheFactory) ccf);
            }
        else
            {
            throw new IllegalArgumentException("The BackingMapManager cannot be must be instantiated"
                                               + "with a given a ExtensibleConfigurableCacheFactory");
            }
        }

    // ----- TopicScheme methods --------------------------------------------

    @Override
    public <T extends NamedCollection> boolean realizes(Class<T> type)
        {
        return NamedTopic.class.equals(type);
        }

    @Override
    public PagedTopicScheme getNamedCollectionBuilder(Class<? extends NamedCollection> clz, Options<NamedCollection.Option> options)
        {
        if (clz.isAssignableFrom(NamedTopic.class))
            {
            return this;
            }
        return null;
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
            // but, we do set the calculator and factor so that the topic impl can evaluate the size
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
     * Set the number of channels in the topic.
     *
     * @param expr  the number of channels in the topic
     */
    @Injectable("channel-count")
    public void setChannelCount(Expression<Integer> expr)
        {
        m_exprChannelCount = expr;
        }

    /**
     * Returns the number of channels in the topic, or the {@link PagedTopic#DEFAULT_CHANNEL_COUNT}
     * value to indicate the topic uses the default channel count.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the number of channels in the topic, or the {@link PagedTopic#DEFAULT_CHANNEL_COUNT}
     *         value to indicate the topic uses the default channel count
     */
    public int getChannelCount(ParameterResolver resolver)
        {
        return m_exprChannelCount.evaluate(resolver);
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
    public Units getPageSize(ParameterResolver resolver)
        {
        return m_exprPageSize.evaluate(resolver);
        }

    /**
     * Set the page size.
     *
     * @param expr  the page high units expression
     */
    @Injectable("page-size")
    public void setPageSize(Expression<Units> expr)
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
     * @param expr  the expression to produce the retain-consumed values flag
     */
    @Injectable("retain-consumed")
    public void setRetainConsumed(Expression<Boolean> expr)
        {
        m_exprRetainConsumed = expr;
        }

    /**
     * Returns {@code true} if the topic allows commits for a position in a channel by
     * subscribers that do not own the channel.
     *
     * @param resolver  the ParameterResolver
     *
     * @return {@code true} if the topic allows commits for a position in a channel by
     *         subscribers that do not own the channel
     */
    public boolean isAllowUnownedCommits(ParameterResolver resolver)
        {
        Boolean fRetain = m_exprAllowUnownedCommits.evaluate(resolver);
        return fRetain != null && fRetain;
        }

    /**
     * Set the flag that indicates whether the topic allows commits for a position in a
     * channel by subscribers that do not own the channel
     *
     * @param expr  {@code true} if the topic allows commits for a position in a channel by
     *              subscribers that do not own the channel or {@code false} to only accept
     *              commits from channel owners
     */
    @Injectable("allow-unowned-commits")
    public void setAllowUnownedCommits(Expression<Boolean> expr)
        {
        m_exprAllowUnownedCommits = expr;
        }

    /**
     * Returns the subscriber timeout value.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the subscriber timeout value
     */
    public Seconds getSubscriberTimeout(ParameterResolver resolver)
        {
        return m_exprSubscriberTimeout.evaluate(resolver);
        }

    /**
     * Set the subscriber timeout value.
     *
     * @param expr  the expression representing the timeout value for subscribers
     */
    @Injectable("subscriber-timeout")
    public void setSubscriberTimeout(Expression<Seconds> expr)
        {
        m_exprSubscriberTimeout = expr == null
                ? new LiteralExpression<>(PagedTopic.DEFAULT_SUBSCRIBER_TIMEOUT_SECONDS)
                : expr;
        }

    /**
     * Return the ElementCalculatorBuilder used to build a ElementCalculator.
     *
     * @return the element calculator
     */
    public ElementCalculatorBuilder getElementCalculatorBuilder()
        {
        return m_bldrElementCalculator;
        }

    /**
     * Set the ElementCalculatorBuilder.
     *
     * @param builder  the ElementCalculatorBuilder
     */
    @Injectable("element-calculator")
    public void setElementCalculatorBuilder(ElementCalculatorBuilder builder)
        {
        m_bldrElementCalculator = builder;
        }

    @Override
    @Injectable("interceptors")
    public void setEventInterceptorBuilders(List<NamedEventInterceptorBuilder> listBuilders)
        {
        super.setEventInterceptorBuilders(listBuilders);
        }

    @Override
    public List<NamedEventInterceptorBuilder> getEventInterceptorBuilders()
        {
        List<NamedEventInterceptorBuilder> list = super.getEventInterceptorBuilders();
        if (list == null)
            {
            list = new ArrayList<>();
            }

        // add the subscriber expiry interceptor
        NamedEventInterceptorBuilder builderTimeout = new NamedEventInterceptorBuilder();
        builderTimeout.setOrder(Interceptor.Order.HIGH);
        builderTimeout.setName("$SubscriberExpiry$" + getServiceName());
        builderTimeout.setRegistrationBehavior(RegistrationBehavior.REPLACE);
        builderTimeout.setCustomBuilder((resolver, loader, listParameters) -> new PagedTopicSubscriber.TimeoutInterceptor());

        list.add(builderTimeout);

        return list;
        }

    /**
     * Returns the maximum amount of time publishers and subscribers will
     * attempt to reconnect after being disconnected.
     *
     * @param resolver  the parameter resolver
     *
     * @return the maximum amount of time publishers and subscribers will
     *         attempt to reconnect after being disconnected
     */
    public Seconds getReconnectTimeoutMillis(ParameterResolver resolver)
        {
        return m_exprReconnectTimeout.evaluate(resolver);
        }

    /**
     * Set the maximum amount of time publishers and subscribers will
     * attempt to reconnect after being disconnected.
     *
     * @param expr  the maximum amount of time publishers and subscribers will
     *              attempt to reconnect after being disconnected
     */
    @Injectable("reconnect-timeout")
    public void setReconnectTimeoutMillis(Expression<Seconds> expr)
        {
        m_exprReconnectTimeout = expr == null
                ? new LiteralExpression<>(PagedTopic.DEFAULT_RECONNECT_TIMEOUT_SECONDS)
                : expr;
        }

    /**
     * Return the amount of time publishers and subscribers will wait between
     * attempts to reconnect after being disconnected.
     *
     * @param resolver  the parameter resolver
     *
     * @return the maximum amount of time publishers and subscribers will
     *         wait between attempts to reconnect after being disconnected
     */
    public Seconds getReconnectRetryMillis(ParameterResolver resolver)
        {
        return m_exprReconnectRetry.evaluate(resolver);
        }

    /**
     * Set the amount of time publishers and subscribers will wait between
     * attempts to reconnect after being disconnected.
     *
     * @param expr  the maximum amount of time publishers and subscribers will
     *              wait between attempts to reconnect after being disconnected
     */
    @Injectable("reconnect-retry")
    public void setReconnectRetryMillis(Expression<Seconds> expr)
        {
        m_exprReconnectRetry = expr == null
                ? new LiteralExpression<>(PagedTopic.DEFAULT_RECONNECT_RETRY_SECONDS)
                : expr;
        }

    /**
     * Return the amount of time publishers and subscribers will wait before
     * attempts to reconnect after being disconnected.
     *
     * @param resolver  the parameter resolver
     *
     * @return the maximum amount of time publishers and subscribers will
     *         wait before attempts to reconnect after being disconnected
     */
    public Seconds getReconnectWaitMillis(ParameterResolver resolver)
        {
        return m_exprReconnectWait.evaluate(resolver);
        }

    /**
     * Set the amount of time publishers and subscribers will wait before
     * attempts to reconnect after being disconnected.
     *
     * @param expr  the maximum amount of time publishers and subscribers will
     *              wait before attempts to reconnect after being disconnected
     */
    @Injectable("reconnect-wait")
    public void setReconnectWaitMillis(Expression<Seconds> expr)
        {
        m_exprReconnectWait = expr == null
                ? new LiteralExpression<>(PagedTopic.DEFAULT_RECONNECT_WAIT_SECONDS)
                : expr;
        }

    // ----- ServiceScheme methods ------------------------------------------

    @Override
    @SuppressWarnings("rawtypes")
    public <V> NamedTopic realize(ValueTypeAssertion<V> typeConstraint,
                                  ParameterResolver resolver, Dependencies deps)
        {
        String           sQueueName  = deps.getCacheName();
        TopicService     service     = ensureConfiguredService(resolver, deps);
        return service.ensureTopic(sQueueName, deps.getClassLoader());
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Ensure service and its topic configuration.
     * <p>
     * PagedTopicConfiguration is registered in corresponding service's resource registry.
     *
     * @param resolver       the ParameterResolver
     * @param deps           the {@link MapBuilder} dependencies
     *
     * @return corresponding TopicService for this scheme
     */
    public TopicService ensureConfiguredService(ParameterResolver resolver, Dependencies deps)
        {
        return getOrEnsureService(deps);
        }

    /**
     * Get or ensure service corresponding to this scheme.
     * <p>
     * Optimized to avoid ensureService synchronization on cluster and service
     * when possible. This behavior is required on server side. Intermittent deadlock occurs
     * calling ensureService on server side from inside service implementation.
     *
     * @return {@link TopicService}
     */
    private TopicService getOrEnsureService(Dependencies deps)
        {
        ExtensibleConfigurableCacheFactory eccf =
                (ExtensibleConfigurableCacheFactory) deps.getConfigurableCacheFactory();

        Service service = CacheFactory.getCluster().getService(getScopedServiceName());

        if (service == null)
            {
            service = eccf.ensureService(this);
            }

        if (service instanceof TopicService)
            {
            return (TopicService) service;
            }
        else
            {
            throw new IllegalArgumentException("Error: the configured service " + service.getInfo().getServiceName()
                    + " is not a TopicService");
            }
        }

    /**
     * Create a {@link PagedTopicDependencies} based on the values contained in this scheme.
     *
     * @param resolver  the {@link ParameterResolver} to use to resolve configuration values
     * @param loader    the {@link ClassLoader} to use
     *
     * @return  a {@link PagedTopicDependencies} based on the values contained in this scheme
     */
    public PagedTopicDependencies createConfiguration(ParameterResolver resolver, ClassLoader loader)
        {
        // enable topic-mapping init-params to be injected into PagedTopicScheme.
        // still need to support DistributedTopics Service Parameters in the future.
        SimpleInjector   injector         = new SimpleInjector();
        ResourceResolver resourceResolver = ResourceResolverHelper.resourceResolverFrom(PagedTopicScheme.class, this);

        injector.inject(this, ResourceResolverHelper.resourceResolverFrom(resourceResolver, resourceResolver));

        long    cbServer           = getHighUnits(resolver);
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
            // Detect Overflow.InetAddresses.getLocalMTU(NetworkInterface) returns Integer.MAX_VALUE on Windows/jdk11 for loopback.
            nMaxBatchSizeBytes = Math.multiplyExact(nMTU, cluster.getDependencies().getPublisherCloggedCount());
            }
        catch (ArithmeticException e)
            {
            nMaxBatchSizeBytes = Integer.MAX_VALUE;
            }

        Units   pageSize    = getPageSize(resolver);
        long    cbPage      = pageSize.getUnitCount();
        boolean fBinarySize = pageSize.isMemorySize();

        if (cbPage <= 0)
            {
            // if page size not set use the calculators page size
            cbPage      = PagedTopic.DEFAULT_PAGE_CAPACITY_BYTES;
            fBinarySize = true;
            }
        else if (cbPage > Integer.MAX_VALUE)
            {
            cbPage = Integer.MAX_VALUE;
            }

        NamedTopic.ElementCalculator calculatorDefault = fBinarySize
                ? BinaryElementCalculator.INSTANCE
                : FixedElementCalculator.INSTANCE;

        ElementCalculatorBuilder     calculatorBuilder = getElementCalculatorBuilder();
        NamedTopic.ElementCalculator calculator        = calculatorBuilder == null
                ? calculatorDefault
                : calculatorBuilder.realize(resolver, loader, null);

        if (pageSize.isMemorySize() && calculator instanceof FixedElementCalculator)
            {
            throw new ConfigurationException("Cannot use the FIXED element calculator with a memory (or default) page-size",
                    "When using a FIXED element calculator a page-size without a memory-unit suffix must be specified");
            }

        PartitionedCacheDependencies  depsService  = getServiceDependencies();
        int                           cPart        = depsService.getPreferredPartitionCount();
        DefaultPagedTopicDependencies dependencies = new DefaultPagedTopicDependencies(cPart);

        dependencies.setServerCapacity(cbServer);
        dependencies.setPageCapacity((int) cbPage);
        dependencies.setElementExpiryMillis(expiryDelayMillis);
        dependencies.setMaxBatchSizeBytes(Math.min((int) cbPage, nMaxBatchSizeBytes));
        dependencies.setRetainConsumed(fRetainConsumed);
        dependencies.setElementCalculator(calculator);
        dependencies.setChannelCount(getChannelCount(resolver));
        dependencies.setAllowUnownedCommits(isAllowUnownedCommits(resolver));
        dependencies.setSubscriberTimeoutMillis(getSubscriberTimeout(resolver).as(Duration.Magnitude.MILLI));
        dependencies.setReconnectTimeoutMillis(getReconnectTimeoutMillis(resolver).as(Duration.Magnitude.MILLI));
        dependencies.setReconnectRetryMillis(getReconnectRetryMillis(resolver).as(Duration.Magnitude.MILLI));
        dependencies.setReconnectWaitMillis(getReconnectWaitMillis(resolver).as(Duration.Magnitude.MILLI));
        return dependencies;
        }

    /**
     * Obtain the builder for the unit calculator.
     *
     * @param resolver  the {@link ParameterResolver} to use
     *
     * @return the builder for the unit calculator
     */
    @SuppressWarnings("unchecked")
    private UnitCalculatorBuilder getUnitCalculatorBuilder(ParameterResolver resolver)
        {
        UnitCalculatorBuilder bldr  = new UnitCalculatorBuilder();
        Parameter             param = resolver.resolve("unit-calculator");
        Expression<String>    expr  = param == null
                ? new LiteralExpression<>("BINARY")
                : param.evaluate(resolver).as(Expression.class);

        bldr.setUnitCalculatorType(expr);

        return bldr;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An empty {@link ParameterResolver}.
     */
    private static final ParameterResolver NULL_PARAMETER_RESOLVER = new NullParameterResolver();

    // ----- data members ---------------------------------------------------

    /**
     * The number of channels in the topic.
     */
    private Expression<Integer> m_exprChannelCount = new LiteralExpression<>(PagedTopic.DEFAULT_CHANNEL_COUNT);

    /**
     * The page capacity
     */
    private Expression<Units> m_exprPageSize = new LiteralExpression<>(new Units(new MemorySize(PagedTopic.DEFAULT_PAGE_CAPACITY_BYTES)));

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
     * The retain-consumed elements flag.
     */
    private Expression<Boolean> m_exprRetainConsumed = new LiteralExpression<>(Boolean.FALSE);

    /**
     * The duration that a value will live in the cache.
     * Zero indicates no timeout.
     */
    private Expression<Seconds> m_exprExpiryDelay = new LiteralExpression<>(new Seconds(0));

    /**
     * The subscriber timeout value.
     */
    private Expression<Seconds> m_exprSubscriberTimeout = new LiteralExpression<>(PagedTopic.DEFAULT_SUBSCRIBER_TIMEOUT_SECONDS);

    /**
     * The allow-unowned commits flag.
     */
    private Expression<Boolean> m_exprAllowUnownedCommits = new LiteralExpression<>(Boolean.FALSE);

    /**
     * The {@link ElementCalculatorBuilder}.
     */
    private ElementCalculatorBuilder m_bldrElementCalculator;

    /**
     * The reconnection timeout value.
     */
    private Expression<Seconds> m_exprReconnectTimeout = new LiteralExpression<>(PagedTopic.DEFAULT_RECONNECT_TIMEOUT_SECONDS);

    /**
     * The reconnection retry value.
     */
    private Expression<Seconds> m_exprReconnectRetry = new LiteralExpression<>(PagedTopic.DEFAULT_RECONNECT_RETRY_SECONDS);

    /**
     * The reconnection wait value.
     */
    private Expression<Seconds> m_exprReconnectWait = new LiteralExpression<>(PagedTopic.DEFAULT_RECONNECT_WAIT_SECONDS);
    }
