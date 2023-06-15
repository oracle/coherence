/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.ApplicationProcess;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.coherence.callables.*;
import com.oracle.bedrock.runtime.java.AbstractJavaApplication;
import com.oracle.bedrock.runtime.java.JavaApplication;
import com.oracle.bedrock.runtime.java.JavaApplicationProcess;
import com.oracle.bedrock.runtime.java.features.JmxFeature;
import com.oracle.bedrock.util.Trilean;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.util.UID;
import com.tangosol.util.UUID;

import javax.management.MBeanInfo;
import javax.management.ObjectName;
import java.util.Set;

public abstract class AbstractCoherenceClusterMember
        extends AbstractJavaApplication<JavaApplicationProcess>
        implements CoherenceClusterMember
    {
    /**
     * The MBean name of the Coherence Cluster MBean.
     */
    public static final String MBEAN_NAME_CLUSTER = "Coherence:type=Cluster";


    /**
     * Constructs an {@link AbstractCoherenceClusterMember}.
     *
     * @param platform      the {@link Platform} on which the {@link Application} was launched
     * @param process       the underlying {@link ApplicationProcess} representing the {@link Application}
     * @param optionsByType the {@link OptionsByType} used to launch the {@link Application}
     */
    public AbstractCoherenceClusterMember(
            Platform platform,
            JavaApplicationProcess process,
            OptionsByType optionsByType)
        {
        super(platform, process, optionsByType);
        }


    /**
     * Obtains the Coherence Cluster {@link MBeanInfo} for the {@link AbstractCoherenceClusterMember}.
     * <p>
     * If the JMX infrastructure in the {@link JavaApplication} is not yet
     * available, it will block at wait for the default application timeout
     * until it becomes available.
     *
     * @return a {@link MBeanInfo}
     * @throws com.oracle.bedrock.deferred.PermanentlyUnavailableException when the resource is not available
     * @throws UnsupportedOperationException                               when JMX is not enabled for the
     *                                                                     {@link JavaApplication}
     */
    public MBeanInfo getClusterMBeanInfo()
        {
        JmxFeature jmxFeature = get(JmxFeature.class);

        if (jmxFeature == null)
            {
            throw new UnsupportedOperationException("The JmxFeature (Java Management Extensions) haven't been enabled for this application");
            }
        else
            {
            try
                {
                return jmxFeature.getMBeanInfo(new ObjectName(MBEAN_NAME_CLUSTER));
                }
            catch (RuntimeException e)
                {
                throw e;
                }
            catch (Exception e)
                {
                throw new UnsupportedOperationException("Could not retrieve the Coherence Cluster MBean", e);
                }
            }
        }


    @Override
    public int getClusterSize()
        {
        return invoke(new GetClusterSize());
        }


    @Override
    public int getLocalMemberClusterPort()
        {
        return invoke(new GetLocalMemberClusterPort());
        }


    @Override
    public int getLocalMemberId()
        {
        return invoke(new GetLocalMemberId());
        }


    @Override
    public UID getLocalMemberUID()
        {
        return invoke(new GetLocalMemberUID());
        }


    @Override
    public UUID getLocalMemberUUID()
        {
        return invoke(new GetLocalMemberUUID());
        }


    @Override
    public Set<UID> getClusterMemberUIDs()
        {
        return invoke(new GetClusterMemberUIDs());
        }


    /**
     * Obtains the Coherence Service {@link MBeanInfo} for the {@link AbstractCoherenceClusterMember}.
     * <p>
     * If the JMX infrastructure in the {@link JavaApplication} is not yet
     * available, it will block at wait for the default application timeout
     * until it becomes available.
     *
     * @param serviceName the name of the service
     * @param nodeId      the nodeId on which the service is defined
     * @return a {@link MBeanInfo}
     * @throws com.oracle.bedrock.deferred.PermanentlyUnavailableException when the resource is not available
     * @throws UnsupportedOperationException                               when JMX is not enabled for the
     *                                                                     {@link JavaApplication}
     */
    public MBeanInfo getServiceMBeanInfo(
            String serviceName,
            int nodeId)
        {
        JmxFeature jmxFeature = get(JmxFeature.class);

        if (jmxFeature == null)
            {
            throw new UnsupportedOperationException("The JmxFeature (Java Management Extensions) haven't been enabled for this application");
            }
        else
            {
            try
                {
                return jmxFeature.getMBeanInfo(new ObjectName(String.format("Coherence:type=Service,name=%s,nodeId=%d",
                                                                            serviceName,
                                                                            nodeId)));
                }
            catch (RuntimeException e)
                {
                throw e;
                }
            catch (Exception e)
                {
                throw new UnsupportedOperationException(String.format("Could not retrieve the Coherence Service MBean [%s]",
                                                                      serviceName),
                                                        e);
                }
            }
        }


    @Override
    public String getMachineName()
        {
        return invoke(new GetLocalMemberMachineName());
        }


    @Override
    public String getMemberName()
        {
        return invoke(new GetLocalMemberName());
        }


    @Override
    public String getRoleName()
        {
        return invoke(new GetLocalMemberRoleName());
        }


    @Override
    public String getRackName()
        {
        return invoke(new GetLocalMemberRackName());
        }


    @Override
    public String getSiteName()
        {
        return invoke(new GetLocalMemberSiteName());
        }


    @Override
    public String getClusterName()
        {
        return invoke(new GetClusterName());
        }


    @Override
    @SuppressWarnings("unchecked")
    public <K, V> NamedCache<K, V> getCache(String cacheName)
        {
        return (NamedCache<K, V>) getCache(cacheName, Object.class, Object.class);
        }


    @Override
    public <K, V> NamedCache<K, V> getCache(
            String cacheName,
            Class<K> keyClass,
            Class<V> valueClass)
        {
        return new CoherenceNamedCache<>(this, cacheName, keyClass, valueClass);
        }


    @Override
    @SuppressWarnings({"unchecked"})
    public <K, V> NamedCache<K, V> getCache(String sessionName, String cacheName)
        {
        return (NamedCache<K, V>) getCache(Coherence.DEFAULT_NAME, sessionName, cacheName, Object.class, Object.class);
        }


    @Override
    public <K, V> NamedCache<K, V> getCache(
            String sessionName,
            String cacheName,
            Class<K> keyClass,
            Class<V> valueClass)
        {
        return getCache(Coherence.DEFAULT_NAME, sessionName, cacheName, keyClass, valueClass);
        }


    @Override
    @SuppressWarnings({"unchecked"})
    public <K, V> NamedCache<K, V> getCache(String coherenceName, String sessionName, String cacheName)
        {
        return (NamedCache<K, V>) getCache(coherenceName, sessionName, cacheName, Object.class, Object.class);
        }


    @Override
    public <K, V> NamedCache<K, V> getCache(
            String coherenceName,
            String sessionName,
            String cacheName,
            Class<K> keyClass,
            Class<V> valueClass)
        {
        return new CoherenceNamedCache<>(this, cacheName, keyClass, valueClass,
                                         new GetSessionCache(coherenceName, sessionName, cacheName));
        }


    @Override
    public Session getSession()
        {
        return getSession(Coherence.DEFAULT_NAME, Coherence.DEFAULT_NAME);
        }


    @Override
    public Session getSession(String sessionName)
        {
        return getSession(Coherence.DEFAULT_NAME, sessionName);
        }


    @Override
    public Session getSession(String coherenceName, String sessionName)
        {
        if (invoke(new SessionExists(coherenceName, sessionName)))
            {
            return new CoherenceSession(this, coherenceName, sessionName);
            }
        throw new IllegalArgumentException("No session exists in the remote member named " + sessionName
                                                   + " in Coherence instance named " + coherenceName);
        }


    @Override
    public boolean isServiceRunning(String serviceName)
        {
        return invoke(new IsServiceRunning(serviceName));
        }

    @Override
    public boolean isCoherenceRunning()
        {
        return invoke(IsCoherenceRunning.instance());
        }

    @Override
    public boolean isCoherenceRunning(String sName)
        {
        return invoke(IsCoherenceRunning.named(sName));
        }

    @Override
    public boolean isSafe()
        {
        return invoke(IsSafe.INSTANCE);
        }


    @Override
    public boolean isReady()
        {
        return invoke(IsReady.INSTANCE);
        }


    @Override
    public Trilean isStorageEnabled(String serviceName)
        {
        return invoke(new IsServiceStorageEnabled(serviceName));
        }


    @Override
    public Set<String> getServiceNames()
        {
        return invoke(new GetServiceNames());
        }

    @Override
    public ServiceStatus getServiceStatus(String serviceName)
        {
        return invoke(new GetServiceStatus(serviceName));
        }


    @Override
    public int getExtendConnectionCount(String sProxyName)
        {
        return invoke(new GetExtendConnectionCount(sProxyName));
        }


    @Override
    public boolean hasExtendConnection(String sProxyName, UUID uuid)
        {
        return invoke(new HasExtendConnection(sProxyName, uuid));
        }

    @Override
    public void threadDump()
        {
        submit(LogThreadDump.INSTANCE);
        }
    }
