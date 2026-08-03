/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.junit;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.java.options.Headless;
import com.oracle.bedrock.runtime.java.options.HeapSize;
import com.oracle.bedrock.runtime.java.options.HotSpot;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.PlatformPredicate;
import com.oracle.bedrock.testsupport.junit.AbstractAssemblyResource;
import com.oracle.bedrock.util.SystemProperties;
import com.oracle.coherence.common.collections.ConcurrentHashMap;
import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.Session;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CoherenceClusterResource
        extends AbstractAssemblyResource<CoherenceClusterMember, CoherenceCluster, CoherenceClusterResource>
    {
    /**
     * Constructs a {@link CoherenceClusterResource}.
     */
    public CoherenceClusterResource()
        {
        super();

        // by default, we have no sessions
        f_mapCCF     = new HashMap<>();
        f_mapSession = new ConcurrentHashMap<>();

        // establish default java process options
        this.commonOptionsByType.add(Headless.enabled());

        // establish default bedrock options
        this.commonOptionsByType.add(Console.system());
        }


    @Override
    protected CoherenceClusterBuilder createBuilder()
        {
        return new CoherenceClusterBuilder();
        }


    /**
     * Obtains the {@link CoherenceCluster} created by the {@link CoherenceClusterResource}.
     *
     * @return the {@link CoherenceCluster}
     */
    public CoherenceCluster getCluster()
        {
        return assembly;
        }


    @Override
    public void before() throws Throwable
        {
        if (launchDefinitions.isEmpty())
            {
            throw new IllegalStateException("CoherenceClusterResource fails to define members to include when launching");
            }

        inheritInvocationProperties(null);

        // take a snapshot of the current system properties, so we can restore them when cleaning up the resource
        this.m_systemProperties = com.oracle.bedrock.util.SystemProperties.createSnapshot();

        // let's ensure that we don't have a local cluster member
        CacheFactory.setCacheFactoryBuilder(null);

        CacheFactory.shutdown();

        // let the super-class perform its initialization
        super.before();
        }


    @Override
    public void after()
        {
        // clean up the sessions
        synchronized (f_mapCCF)
            {
            for (ConfigurableCacheFactory session : f_mapCCF.values())
                {
                CacheFactory.getCacheFactoryBuilder().release(session);
                }
            }

        // let's ensure that we don't have a local cluster member
        CacheFactory.setCacheFactoryBuilder(null);

        CacheFactory.shutdown();

        // let the super-class perform it's clean-up
        super.after();

        // restore the system properties
        com.oracle.bedrock.util.SystemProperties.replaceWith(m_systemProperties);
        }


    @Override
    public Statement apply(
            Statement base,
            Description description)
        {
        inheritInvocationProperties(description.getClassName());

        return super.apply(base, description);
        }


    /**
     * Inherit invocation-level Coherence options unless the resource already defines them.
     *
     * @param sDefaultClusterName  the default cluster name to use when no invocation-level cluster is set
     */
    void inheritInvocationProperties(String sDefaultClusterName)
        {
        inheritSystemProperty(COHERENCE_MODE_PROPERTY);
        inheritSystemProperty(COHERENCE_WKA_PROPERTY);
        inheritSystemProperty(COHERENCE_LOCALHOST_PROPERTY);

        // automatically set the cluster name if it isn't configured
        String sClusterName = System.getProperty(ClusterName.PROPERTY);
        if (sClusterName != null || sDefaultClusterName != null)
            {
            commonOptionsByType.addIfAbsent(ClusterName.of(sClusterName == null ? sDefaultClusterName : sClusterName));
            }
        }


    /**
     * Inherit an invocation-level system property unless the resource already defines it.
     *
     * @param sName  the property name
     */
    private void inheritSystemProperty(String sName)
        {
        String sValue = System.getProperty(sName);
        if (sValue == null)
            {
            return;
            }

        com.oracle.bedrock.runtime.java.options.SystemProperties properties =
                commonOptionsByType.get(com.oracle.bedrock.runtime.java.options.SystemProperties.class);

        if (properties == null || properties.get(sName) == null)
            {
            commonOptionsByType.add(SystemProperty.of(sName, sValue));
            }
        }


    /**
     * Defines the necessary information for launching one or more {@link CoherenceClusterMember}s
     * as part of the {@link CoherenceCluster} when the {@link CoherenceClusterResource} is established.
     * <p>
     * The {@link Platform} on which the {@link CoherenceClusterMember}s are launched is based on the
     * {@link PlatformPredicate} specified as an {@link Option}. By default, this is {@link PlatformPredicate#any()}.
     * <p>
     * Multiple calls to this method are permitted, allowing a {@link CoherenceCluster} to be created containing
     * multiple different types of {@link CoherenceCluster}s.
     * <p>
     * This is equivalent to calling {@link #include(int, Class, Option...)} using a {@link CoherenceClusterMember}
     * class as the {@link Application} class.
     *
     * @param count   the number of instances of the {@link CoherenceCluster} that should be launched for
     *                the {@link CoherenceCluster}
     * @param options the {@link Option}s to use for launching the {@link CoherenceCluster}s
     * @return the {@link CoherenceClusterResource} to permit fluent-style method calls
     */
    public CoherenceClusterResource include(
            int count,
            Option... options)
        {
        return include(count, CoherenceClusterMember.class, options);
        }


    /**
     * Obtains a session, represented as a {@link ConfigurableCacheFactory}, against the {@link CoherenceCluster}.
     * <p>
     * Attempts to request a {@link ConfigurableCacheFactory} multiple times with the same {@link SessionBuilder}
     * will return the same {@link ConfigurableCacheFactory}.
     *
     * @param builder  the builder for the specific type of session
     *
     * @return a {@link ConfigurableCacheFactory} representing the Coherence Session
     */
    public synchronized ConfigurableCacheFactory createSession(SessionBuilder builder)
        {
        return f_mapCCF.compute(builder, (k, ccf) ->
            {
            if (ccf != null && !ccf.isDisposed())
                {
                return ccf;
                }

            // restore the system properties (as the session needs to start cleanly)
            SystemProperties.replaceWith(m_systemProperties);

            OptionsByType optionsByType = OptionsByType.of(commonOptionsByType);

            optionsByType.add(RoleName.of("client"));
            optionsByType.add(LocalStorage.disabled());

            return builder.build(LocalPlatform.get(), getCluster(), optionsByType);
            });
        }

    /**
     * Obtains a {@link Session}, against the {@link CoherenceCluster}.
     * <p/>
     * Attempts to request a session multiple times with the same {@link SessionBuilder} will return the same session.
     *
     * @param builder  the builder for the specific type of session
     *
     * @return a {@link Session}
     */
    public synchronized Session buildSession(SessionBuilder builder)
        {
        return f_mapSession.compute(builder, (k, session) ->
            {
            if (session != null && session.isActive())
                {
                return session;
                }
            ConfigurableCacheFactory ccf = createSession(builder);
            return new ClosingConfigurableCacheFactorySession(ccf, builder.getClass().getClassLoader());
            });
        }

    /**
     * Return the common options for this {@link CoherenceClusterResource}.
     *
     * @return the common options for this {@link CoherenceClusterResource}
     */
    public Option[] getCommonOptions()
    {
        return commonOptionsByType.asArray();
    }

    // ----- ClosingConfigurableCacheFactorySession -------------------------

    protected static class ClosingConfigurableCacheFactorySession
            extends ConfigurableCacheFactorySession
        {
        public ClosingConfigurableCacheFactorySession(ConfigurableCacheFactory ccf, ClassLoader loader)
            {
            super(ccf, loader);
            }

        @Override
        public synchronized void close() throws Exception
            {
            if (isClosed())
                {
                return;
                }
            super.close();
            ConfigurableCacheFactory ccf = getConfigurableCacheFactory();
            if (!ccf.isDisposed())
                {
                ccf.dispose();
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The system properties prior to the creation of the {@link CoherenceClusterResource <R>}.
     */
    private Properties m_systemProperties;

    /**
     * The Coherence runtime mode property.
     */
    private static final String COHERENCE_MODE_PROPERTY = "coherence.mode";

    /**
     * The Coherence WKA property.
     */
    private static final String COHERENCE_WKA_PROPERTY = "coherence.wka";

    /**
     * The Coherence localhost property.
     */
    private static final String COHERENCE_LOCALHOST_PROPERTY = "coherence.localhost";

    /**
     * The {@link ConfigurableCacheFactory} sessions that have been locally created against the
     * {@link CoherenceCluster} using this {@link CoherenceClusterResource}.
     */
    private final Map<SessionBuilder, ConfigurableCacheFactory> f_mapCCF;

    /**
     * The {@link ConfigurableCacheFactory} sessions that have been locally created against the
     * {@link CoherenceCluster} using this {@link CoherenceClusterResource}.
     */
    private final Map<SessionBuilder, Session> f_mapSession;
    }
