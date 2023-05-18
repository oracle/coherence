/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.junit.CoherenceClusterExtension;
import com.oracle.bedrock.junit.SessionBuilder;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.coherence.common.base.Exceptions;
import com.tangosol.net.ConfigurableCacheFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper around {@link CoherenceClusterExtension}.
 */
public class ClosableCluster
        implements AutoCloseable
    {
    /**
     * Create a {@link ClosableCluster}.
     */
    public ClosableCluster()
        {
        m_extension = new CoherenceClusterExtension();
        }

    /**
     * Returns the wrapped {@link CoherenceClusterExtension}.
     *
     * @return the wrapped {@link CoherenceClusterExtension}
     */
    public CoherenceClusterExtension getExtension()
        {
        return m_extension;
        }

    /**
     * Create a {@link ConfigurableCacheFactory} session.
     *
     * @param builder  the builder to use
     *
     * @return a {@link ConfigurableCacheFactory} session
     */
    public ConfigurableCacheFactory createSession(SessionBuilder builder)
        {
        return m_extension.createSession(builder);
        }

    /**
     * Start the cluster.
     *
     * @return  this {@link ClosableCluster}
     */
    public ClosableCluster start()
        {
        try
            {
            m_extension.beforeAll(null);
            return this;
            }
        catch (Exception e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    @Override
    public void close()
        {
        try
            {
            m_mapMember.values().forEach(Application::close);
            m_extension.afterAll(null);
            }
        catch (Exception e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Return the {@link CoherenceCluster} started by this extension.
     *
     * @return the {@link CoherenceCluster} started by this extension
     */
    public CoherenceCluster getCluster()
        {
        return m_extension.getCluster();
        }

    /**
     * Run a {@link TopicPublisher} process.
     *
     * @param sName  the name for the process
     * @param opts   any additional options for the process
     *
     * @return the {@link TopicPublisher} process
     */
    public CoherenceClusterMember startPublisher(String sName, Option... opts)
        {
        OptionsByType options = OptionsByType.of(opts)
                .addIfAbsent(RoleName.of("Publisher"))
                .addIfAbsent(DisplayName.of(sName))
                .add(LocalStorage.disabled());

        return startMember(sName, TopicPublisher.class, options.asArray());
        }

    /**
     * Run a {@link TopicSubscriber} process.
     *
     * @param sName  the name for the process
     * @param opts   any additional options for the process
     *
     * @return the {@link TopicSubscriber} process
     */
    public CoherenceClusterMember startSubscriber(String sName, Option... opts)
        {
        OptionsByType options = OptionsByType.of(opts)
                .addIfAbsent(RoleName.of("Subscriber"))
                .addIfAbsent(DisplayName.of(sName))
                .add(LocalStorage.disabled());

        return startMember(sName, TopicSubscriber.class, options.asArray());
        }

    /**
     * Start an additional {@link CoherenceClusterMember}.
     *
     * @param sName      the name of the process
     * @param mainClass  the main class to use
     * @param opts       any additional options for the process
     *
     * @return the started additional {@link CoherenceClusterMember}
     */
    public CoherenceClusterMember startMember(String sName, Class<?> mainClass, Option... opts)
        {
        return m_mapMember.computeIfAbsent(sName, s ->
            {
            OptionsByType options = OptionsByType.of(m_extension.getCommonOptions())
                    .add(ClassName.of(mainClass))
                    .addAll(opts);
            return LocalPlatform.get().launch(CoherenceClusterMember.class, options.asArray());
            });
        }

    /**
     * Relaunch the cluster.
     *
     * @param version the version con to use to relaunch
     */
    public void relaunch(Version version)
        {
        if (version == Version.Current)
            {
            m_extension.getCluster().relaunch(Version.Current.getClassPath());
            }
        else
            {
            m_extension.getCluster().relaunch(Version.Previous.getClassPath());
            }
        }

    /**
     * Write a message to the log of every cluster member.
     *
     * @param msg  the message to write
     */
    public void log(String msg)
        {
        CoherenceCluster cluster = m_extension.getCluster();
        for (CoherenceClusterMember member : cluster)
            {
            member.invoke(() ->
                {
                System.err.println(msg);
                System.err.flush();
                return null;
                });
            }

        for (CoherenceClusterMember member : m_mapMember.values())
            {
            member.invoke(() ->
                {
                System.err.println(msg);
                System.err.flush();
                return null;
                });
            }
        }

    /**
     * Log a thread dump on every cluster member.
     */
    public void threadDump()
        {
        CoherenceCluster cluster = m_extension.getCluster();
        for (CoherenceClusterMember member : cluster)
            {
            try
                {
                member.threadDump();
                }
            catch (Exception e)
                {
                e.printStackTrace();
                }
            }

        for (CoherenceClusterMember member : m_mapMember.values())
            {
            try
                {
                member.threadDump();
                }
            catch (Exception e)
                {
                e.printStackTrace();
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The wrapped {@link CoherenceClusterExtension}.
     */
    private final CoherenceClusterExtension m_extension;

    /**
     * A map of additional cluster members.
     */
    private final Map<String, CoherenceClusterMember> m_mapMember = new HashMap<>();
    }
