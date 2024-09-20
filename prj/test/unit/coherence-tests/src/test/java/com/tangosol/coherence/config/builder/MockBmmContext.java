/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
import com.tangosol.io.Serializer;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.Member;
import com.tangosol.net.MemberListener;
import com.tangosol.net.NamedCache;
import com.tangosol.net.ServiceDependencies;
import com.tangosol.net.ServiceInfo;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Converter;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.ServiceListener;
import com.tangosol.util.SimpleResourceRegistry;

import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * The MockBmmContext is needed for unit tests require certain inner members
 * of the context to be populated.  For example, the ReadWriteBackingMapTest
 * creates a ReadWriteBackingMap that will expect a ServiceInfo if the
 * cache store is present.
 *
 * @author pfm  2011.12.28
 */
public class MockBmmContext
        implements BackingMapManagerContext
    {
    @Override
    public BackingMapManager getManager()
        {
        return null;
        }

    @Override
    public CacheService getCacheService()
        {
        return new MockCacheService();
        }

    @Override
    public ClassLoader getClassLoader()
        {
        return null;
        }

    @Override
    public void setClassLoader(ClassLoader loader)
        {
        }

    @Override
    public Converter getKeyToInternalConverter()
        {
        return null;
        }

    @Override
    public Converter getKeyFromInternalConverter()
        {
        return null;
        }

    @Override
    public Converter getValueToInternalConverter()
        {
        return null;
        }

    @Override
    public Converter getValueFromInternalConverter()
        {
        return null;
        }

    @Override
    public boolean isKeyOwned(Object oKey)
        {
        return false;
        }

    @Override
    public int getKeyPartition(Object oKey)
        {
        return 0;
        }

    @Override
    public Set getPartitionKeys(String sCacheName, int nPartition)
        {
        return null;
        }

    @Override
    public Map getBackingMap(String sCacheName)
        {
        return null;
        }

    @Override
    public BackingMapContext getBackingMapContext(String sCacheName)
        {
        return null;
        }

    @Override
    public Object addInternalValueDecoration(Object oValue, int nDecorId, Object oDecor)
        {
        return null;
        }

    @Override
    public Object removeInternalValueDecoration(Object oValue, int nDecorId)
        {
        return null;
        }

    @Override
    public boolean isInternalValueDecorated(Object oValue, int nDecorId)
        {
        return false;
        }

    @Override
    public Object getInternalValueDecoration(Object oValue, int nDecorId)
        {
        return null;
        }

    @Override
    public XmlElement getConfig()
        {
        return null;
        }

    @Override
    public void setConfig(XmlElement xml)
        {
        }

    // ----- inner class  MockCacheService ----------------------------------

    public static class MockCacheService
            implements CacheService
        {
        @Override
        public ServiceDependencies getDependencies()
            {
            return null;
            }

        @Override
        public Cluster getCluster()
            {
            Cluster cluster = m_cluster;

            if (cluster == null)
                {
                cluster = spy(CacheFactory.getCluster());
                when(cluster.getResourceRegistry()).thenReturn(m_registry);
                m_cluster = cluster;
                }
            return cluster;
            }

        @Override
        public ServiceInfo getInfo()
            {
            return new MockServiceInfo();
            }

        @Override
        public void addMemberListener(MemberListener listener)
            {
            }

        @Override
        public void removeMemberListener(MemberListener listener)
            {
            }

        @Override
        public Object getUserContext()
            {
            return null;
            }

        @Override
        public void setUserContext(Object oCtx)
            {
            }

        @Override
        public Serializer getSerializer()
            {
            return null;
            }

        @Override
        public void addServiceListener(ServiceListener listener)
            {
            }

        @Override
        public void removeServiceListener(ServiceListener listener)
            {
            }

        @Override
        public void configure(XmlElement xml)
            {
            }

        @Override
        public void start()
            {
            }

        @Override
        public boolean isRunning()
            {
            return false;
            }

        @Override
        public void shutdown()
            {
            }

        @Override
        public void stop()
            {
            }

        @Override
        public ClassLoader getContextClassLoader()
            {
            return null;
            }

        @Override
        public void setContextClassLoader(ClassLoader loader)
            {
            }

        @Override
        public BackingMapManager getBackingMapManager()
            {
            return null;
            }

        @Override
        public void setBackingMapManager(BackingMapManager manager)
            {
            }

        @Override
        public NamedCache ensureCache(String sName, ClassLoader loader)
            {
            return null;
            }

        @Override
        public Enumeration getCacheNames()
            {
            return null;
            }

        @Override
        public void releaseCache(NamedCache map)
            {
            }

        @Override
        public void destroyCache(NamedCache map)
            {
            }

        @Override
        public void setDependencies(ServiceDependencies deps)
            {
            }

        @Override
        public ResourceRegistry getResourceRegistry()
            {
            return null;
            }

        public boolean isSuspended()
            {
            return false;
            }

        @Override
        public boolean isVersionCompatible(int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
            {
            int nEncoded = ServiceMemberSet.encodeVersion(nMajor, nMinor, nMicro, nPatchSet, nPatch);
            return CacheFactory.VERSION_ENCODED >= nEncoded;
            }

        @Override
        public boolean isVersionCompatible(int nYear, int nMonth, int nPatch)
            {
            int nEncoded = ServiceMemberSet.encodeVersion(nYear, nMonth, nPatch);
            return CacheFactory.VERSION_ENCODED >= nEncoded;
            }

        @Override
        public boolean isVersionCompatible(int nVersion)
            {
            return CacheFactory.VERSION_ENCODED >= nVersion;
            }

        @Override
        public boolean isVersionCompatible(IntPredicate predicate)
            {
            return predicate.test(CacheFactory.VERSION_ENCODED);
            }

        @Override
        public int getMinimumServiceVersion()
            {
            return CacheFactory.VERSION_ENCODED;
            }

        private Cluster m_cluster;
        private final ResourceRegistry m_registry = new SimpleResourceRegistry();
        }

    // ----- inner class MockServiceInfo ------------------------------------

    public static class MockServiceInfo
            implements ServiceInfo
        {
        @Override
        public String getServiceName()
            {
            return "MockServiceName";
            }

        @Override
        public String getServiceType()
            {
            return CacheService.TYPE_LOCAL;
            }

        @Override
        public Set getServiceMembers()
            {
            return null;
            }

        @Override
        public String getServiceVersion(Member member)
            {
            return null;
            }

        @Override
        public Member getOldestMember()
            {
            return null;
            }

        @Override
        public Member getServiceMember(int nId)
            {
            return null;
            }
        }
    }
