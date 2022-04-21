/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.junit;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.AssemblyBuilder;
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
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.PlatformPredicate;
import com.oracle.bedrock.testsupport.junit.AbstractAssemblyResource;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.HashMap;
import java.util.Properties;

public class CoherenceClusterResource
    extends AbstractAssemblyResource<CoherenceClusterMember, CoherenceCluster, CoherenceClusterResource>
{
    /**
     * The system properties prior to the creation of the {@link CoherenceClusterResource <R>}.
     */
    private Properties systemProperties;

    /**
     * The {@link ConfigurableCacheFactory} sessions that have been locally created against the
     * {@link CoherenceCluster} using this {@link CoherenceClusterResource}.
     */
    private HashMap<SessionBuilder, ConfigurableCacheFactory> sessions;


    /**
     * Constructs a {@link CoherenceClusterResource}.
     */
    public CoherenceClusterResource()
    {
        super();

        // by default we have no sessions
        this.sessions = new HashMap<>();

        // establish default java process options
        this.commonOptionsByType.add(Headless.enabled());
        this.commonOptionsByType.add(HotSpot.Mode.SERVER);
        this.commonOptionsByType.add(HeapSize.of(256, HeapSize.Units.MB, 1024, HeapSize.Units.MB));

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
    protected void before() throws Throwable
    {
        if (launchDefinitions.isEmpty())
        {
            throw new IllegalStateException("CoherenceClusterResource fails to define members to include when launching");
        }

        // take a snapshot of the current system properties so we can restore them when cleaning up the resource
        this.systemProperties = com.oracle.bedrock.util.SystemProperties.createSnapshot();

        // let's ensure that we don't have a local cluster member
        CacheFactory.setCacheFactoryBuilder(null);

        CacheFactory.shutdown();

        // let the super-class perform it's initialization
        super.before();
    }

         
    @Override
    protected void after()
    {
        // clean up the sessions
        synchronized (sessions)
        {
            for (ConfigurableCacheFactory session : sessions.values())
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
        com.oracle.bedrock.util.SystemProperties.replaceWith(systemProperties);
    }


    @Override
    public Statement apply(Statement   base,
                           Description description)
    {
        // automatically set the cluster name to the test class name
        // if the cluster name isn't configured
        commonOptionsByType.addIfAbsent(ClusterName.of(description.getClassName()));

        return super.apply(base, description);
    }


    /**
     * Defines the necessary information for launching one or more {@link CoherenceClusterMember}s
     * as part of the {@link CoherenceCluster} when the {@link CoherenceClusterResource} is established.
     * <p>
     * The {@link Platform} on which the {@link CoherenceClusterMember}s are launched is based on the
     * {@link PlatformPredicate} specified as an {@link Option}.  By default this is {@link PlatformPredicate#any()}.
     * <p>
     * Multiple calls to this method are permitted, allowing a {@link CoherenceCluster} to be created containing
     * multiple different types of {@link CoherenceCluster}s.
     * <p>
     * This is equivalent to calling {@link #include(int, Class, Option...)} using a {@link CoherenceClusterMember}
     * class as the {@link Application} class.
     *
     * @param count             the number of instances of the {@link CoherenceCluster} that should be launched for
     *                          the {@link CoherenceCluster}
     * @param options           the {@link Option}s to use for launching the {@link CoherenceCluster}s
     *
     * @return the {@link CoherenceClusterResource} to permit fluent-style method calls
     */
    public CoherenceClusterResource include(int       count,
                                            Option... options)
    {
        return include(count, CoherenceClusterMember.class, options);
    }


    /**
     * Obtains a session, represented as a {@link ConfigurableCacheFactory}, against the {@link CoherenceCluster}.
     * <p>
     * Only a single session may be created by a {@link CoherenceClusterResource} against the {@link CoherenceCluster}.
     * <p>
     * Attempts to request a session multiple times with the same {@link SessionBuilder} will return the same session.
     *
     * @param builder the builder for the specific type of session
     *
     * @return a {@link ConfigurableCacheFactory} representing the Coherence Session.
     *
     * @throws IllegalStateException when an attempt to request sessions for
     *                               different {@link SessionBuilder}s is made
     */
    public synchronized ConfigurableCacheFactory createSession(SessionBuilder builder)
    {
        // restore the system properties (as the session needs to start cleanly)
        com.oracle.bedrock.util.SystemProperties.replaceWith(systemProperties);

        ConfigurableCacheFactory session = sessions.get(builder);

        if (session == null)
        {
            OptionsByType optionsByType = OptionsByType.of(commonOptionsByType);

            optionsByType.add(RoleName.of("client"));
            optionsByType.add(LocalStorage.disabled());

            session = builder.build(LocalPlatform.get(), getCluster(), optionsByType);

            sessions.put(builder, session);
        }

        return session;
    }
}
