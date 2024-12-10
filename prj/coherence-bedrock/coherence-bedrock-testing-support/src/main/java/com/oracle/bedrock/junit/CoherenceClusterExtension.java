/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
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
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.java.options.Headless;
import com.oracle.bedrock.runtime.java.options.HeapSize;
import com.oracle.bedrock.runtime.java.options.HotSpot;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.PlatformPredicate;
import com.oracle.bedrock.testsupport.junit.AbstractBaseAssembly;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.HashMap;
import java.util.Properties;

public class CoherenceClusterExtension
        extends AbstractBaseAssembly<CoherenceClusterMember, CoherenceCluster, CoherenceClusterExtension>
        implements BeforeAllCallback, AfterAllCallback
{
    /**
     * The system properties prior to the creation of the {@link CoherenceClusterResource <R>}.
     */
    private Properties systemProperties;

    /**
     * The {@link ConfigurableCacheFactory} sessions that have been locally created against the
     * {@link CoherenceCluster} using this {@link CoherenceClusterResource}.
     */
    private final HashMap<SessionBuilder, ConfigurableCacheFactory> sessions;

    /**
     * Constructs a {@link CoherenceClusterExtension}.
     */
    public CoherenceClusterExtension()
    {
        super();

        // by default we have no sessions
        this.sessions = new HashMap<>();

        // establish default java process options
        commonOptionsByType.add(Headless.enabled());
        commonOptionsByType.add(HotSpot.Mode.SERVER);

        // establish default bedrock options
        this.commonOptionsByType.add(Console.system());
    }

    @Override
    protected CoherenceClusterBuilder createBuilder() {
        return new CoherenceClusterBuilder();
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        if (launchDefinitions.isEmpty())
        {
            throw new IllegalStateException("CoherenceClusterResource fails to define members to include when launching");
        }

        // take a snapshot of the current system properties so we can restore them when cleaning up the resource
        this.systemProperties = com.oracle.bedrock.util.SystemProperties.createSnapshot();

        // let's ensure that we don't have a local cluster member
        CacheFactory.setCacheFactoryBuilder(null);

        CacheFactory.shutdown();

        start();
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
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

        close();

        // restore the system properties
        com.oracle.bedrock.util.SystemProperties.replaceWith(systemProperties);
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
    public CoherenceClusterExtension include(int       count,
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

    /**
     * Return the common options for this {@link CoherenceClusterExtension}.
     *
     * @return the common options for this {@link CoherenceClusterExtension}
     */
    public Option[] getCommonOptions()
    {
        return commonOptionsByType.asArray();
    }
}
