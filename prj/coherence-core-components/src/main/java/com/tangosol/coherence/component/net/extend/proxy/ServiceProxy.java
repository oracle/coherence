
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.proxy.ServiceProxy

package com.tangosol.coherence.component.net.extend.proxy;

import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Member;
import java.util.Collections;
import java.util.function.IntPredicate;

/**
 * The ServiceProxy is the base component of cluster-side handlers (Proxy) for
 * Services. It enables non-clustered clients to invoke Service methods within
 * the cluster.
 * 
 * @see Component.Net.Extend.RemoteService
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class ServiceProxy
        extends    com.tangosol.coherence.component.net.extend.Proxy
        implements com.tangosol.net.Service,
                   com.tangosol.net.ServiceInfo,
                   com.tangosol.net.messaging.Channel.Receiver,
                   com.tangosol.run.xml.XmlHelper.ParameterResolver
    {
    // ---- Fields declarations ----
    
    /**
     * Property ContextClassLoader
     *
     * @see com.tangosol.net.Service#getContextClassLoader
     */
    private ClassLoader __m_ContextClassLoader;
    
    /**
     * Property Serializer
     *
     * @see com.tangosol.net.Service#getSerializer
     */
    private com.tangosol.io.Serializer __m_Serializer;
    
    /**
     * Property ServiceVersion
     *
     * @see com.tangosol.net.ServiceInfo#getServiceVersion
     */
    private String __m_ServiceVersion;
    
    /**
     * Property UserContext
     *
     * The user context object associated with this "pseudo" InvocationService.
     */
    private transient Object __m_UserContext;
    
    // Initializing constructor
    public ServiceProxy(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        Class clz;
        try
            {
            clz = Class.forName("com.tangosol.coherence/component/net/extend/proxy/ServiceProxy".replace('/', '.'));
            }
        catch (ClassNotFoundException e)
            {
            throw new NoClassDefFoundError(e.getMessage());
            }
        return clz;
        }
    
    //++ getter for autogen property _Module
    /**
     * This is an auto-generated method that returns the global [design time]
    * parent component.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    private com.tangosol.coherence.Component get_Module()
        {
        return this;
        }
    
    // From interface: com.tangosol.net.Service
    public void addMemberListener(com.tangosol.net.MemberListener listener)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.Service
    public void addServiceListener(com.tangosol.util.ServiceListener listener)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.Service
    public void configure(com.tangosol.run.xml.XmlElement xml)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.Service
    public com.tangosol.net.Cluster getCluster()
        {
        // import com.tangosol.net.CacheFactory;
        
        return CacheFactory.ensureCluster();
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "ContextClassLoader"
    /**
     * Getter for property ContextClassLoader.<p>
    * @see com.tangosol.net.Service#getContextClassLoader
     */
    public ClassLoader getContextClassLoader()
        {
        return __m_ContextClassLoader;
        }
    
    // From interface: com.tangosol.net.Service
    public com.tangosol.net.ServiceInfo getInfo()
        {
        return this;
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public String getName()
        {
        return null;
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    public com.tangosol.net.Member getOldestMember()
        {
        return getCluster().getLocalMember();
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public com.tangosol.net.messaging.Protocol getProtocol()
        {
        return null;
        }
    
    // From interface: com.tangosol.net.Service
    public com.tangosol.util.ResourceRegistry getResourceRegistry()
        {
        return null;
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "Serializer"
    /**
     * Getter for property Serializer.<p>
    * @see com.tangosol.net.Service#getSerializer
     */
    public com.tangosol.io.Serializer getSerializer()
        {
        return __m_Serializer;
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    public com.tangosol.net.Member getServiceMember(int nId)
        {
        // import com.tangosol.net.Member;
        
        Member member = getCluster().getLocalMember();
        return nId == member.getId() ? member : null;
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    public java.util.Set getServiceMembers()
        {
        // import java.util.Collections;
        
        return Collections.singleton(getCluster().getLocalMember());
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    public String getServiceName()
        {
        return getName();
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    public String getServiceType()
        {
        return null;
        }
    
    // Accessor for the property "ServiceVersion"
    /**
     * Getter for property ServiceVersion.<p>
    * @see com.tangosol.net.ServiceInfo#getServiceVersion
     */
    public String getServiceVersion()
        {
        return __m_ServiceVersion;
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    // Accessor for the property "ServiceVersion"
    /**
     * Getter for property ServiceVersion.<p>
    * @see com.tangosol.net.ServiceInfo#getServiceVersion
     */
    public String getServiceVersion(com.tangosol.net.Member member)
        {
        return getCluster().getLocalMember().equals(member) ? getServiceVersion() : null;
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "UserContext"
    /**
     * Getter for property UserContext.<p>
    * The user context object associated with this "pseudo" InvocationService.
     */
    public Object getUserContext()
        {
        return __m_UserContext;
        }
    
    // From interface: com.tangosol.net.Service
    public boolean isRunning()
        {
        return true;
        }
    
    // From interface: com.tangosol.net.Service
    public boolean isSuspended()
        {
        return false;
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public void onMessage(com.tangosol.net.messaging.Message message)
        {
        message.run();
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public void registerChannel(com.tangosol.net.messaging.Channel channel)
        {
        }
    
    // From interface: com.tangosol.net.Service
    public void removeMemberListener(com.tangosol.net.MemberListener listener)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.Service
    public void removeServiceListener(com.tangosol.util.ServiceListener listener)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.run.xml.XmlHelper$ParameterResolver
    public Object resolveParameter(String sType, String sValue)
        {
        // import com.tangosol.run.xml.XmlHelper$ParameterResolver as com.tangosol.run.xml.XmlHelper.ParameterResolver;
        
        return com.tangosol.run.xml.XmlHelper.ParameterResolver.UNRESOLVED;
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "ContextClassLoader"
    /**
     * Setter for property ContextClassLoader.<p>
    * @see com.tangosol.net.Service#getContextClassLoader
     */
    public void setContextClassLoader(ClassLoader loader)
        {
        __m_ContextClassLoader = loader;
        }
    
    // Accessor for the property "Serializer"
    /**
     * Setter for property Serializer.<p>
    * @see com.tangosol.net.Service#getSerializer
     */
    public void setSerializer(com.tangosol.io.Serializer serializer)
        {
        __m_Serializer = serializer;
        }
    
    // Accessor for the property "ServiceVersion"
    /**
     * Setter for property ServiceVersion.<p>
    * @see com.tangosol.net.ServiceInfo#getServiceVersion
     */
    protected void setServiceVersion(String sVersion)
        {
        __m_ServiceVersion = sVersion;
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "UserContext"
    /**
     * Setter for property UserContext.<p>
    * The user context object associated with this "pseudo" InvocationService.
     */
    public void setUserContext(Object oCtx)
        {
        __m_UserContext = oCtx;
        }
    
    // From interface: com.tangosol.net.Service
    public void shutdown()
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.Service
    public void start()
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.Service
    public void stop()
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public void unregisterChannel(com.tangosol.net.messaging.Channel channel)
        {
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
    }
