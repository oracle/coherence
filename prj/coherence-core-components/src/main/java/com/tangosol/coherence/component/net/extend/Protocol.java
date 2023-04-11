
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.Protocol

package com.tangosol.coherence.component.net.extend;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import java.util.HashMap;
import java.util.Map;

/**
 * Base definition of a Protocol component.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Protocol
        extends    com.tangosol.coherence.component.net.Extend
        implements com.tangosol.net.messaging.Protocol
    {
    // ---- Fields declarations ----
    
    /**
     * Property MessageFactoryMap
     *
     * A map of MessageFactory objects, keyed by Protocol version.
     */
    private transient java.util.Map __m_MessageFactoryMap;
    
    /**
     * Property Name
     *
     * The name of the Protocol.
     * 
     * @see com.tangosol.net.Protocol#getName
     */
    private String __m_Name;
    
    /**
     * Property VersionCurrent
     *
     * The newest protocol version supported by this Protocol.
     * 
     * @see com.tangosol.net.messaging.Protocol#getCurrentVersion
     */
    private int __m_VersionCurrent;
    
    /**
     * Property VersionSupported
     *
     * The oldest protocol version supported by this Protocol.
     * 
     * @see com.tangosol.net.messaging.Protocol#getSupportedVersion
     */
    private int __m_VersionSupported;
    
    // Initializing constructor
    public Protocol(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/Protocol".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.messaging.Protocol
    public int getCurrentVersion()
        {
        return getVersionCurrent();
        }
    
    // Declared at the super level
    /**
     * Return a human-readable description of this component.
    * 
    * @return a String representation of this component
     */
    protected String getDescription()
        {
        int cCurrentVersion   = getCurrentVersion();
        int cVersionSupported = getVersionSupported();
        
        StringBuilder sb = new StringBuilder("Versions=[");
        
        if (cCurrentVersion == cVersionSupported)
            {
            sb.append(cCurrentVersion);
            }
        else
            {
            sb.append(cVersionSupported)
              .append("..")
              .append(cCurrentVersion);
            }
        
        sb.append(']');
        
        return sb.toString();
        }
    
    // From interface: com.tangosol.net.messaging.Protocol
    public synchronized com.tangosol.net.messaging.Protocol.MessageFactory getMessageFactory(int nVersion)
        {
        // import Component.Net.Extend.MessageFactory as com.tangosol.coherence.component.net.extend.MessageFactory;
        // import com.tangosol.util.Base;
        // import java.util.HashMap;
        // import java.util.Map;
        
        if (nVersion < getSupportedVersion() || nVersion > getCurrentVersion())
            {
            throw new IllegalArgumentException("protocol " + getName()
                    + " does not support version " + nVersion);
            }
        
        Map map = getMessageFactoryMap();
        if (map == null)
            {
            setMessageFactoryMap(map = new HashMap());
            }
        
        Integer         IVersion = Base.makeInteger(nVersion);
        com.tangosol.coherence.component.net.extend.MessageFactory factory  = (com.tangosol.coherence.component.net.extend.MessageFactory) map.get(IVersion);
        if (factory == null)
            {
            factory = (com.tangosol.coherence.component.net.extend.MessageFactory) instantiateMessageFactory(nVersion);
            factory.setProtocol(this);
            factory.setVersion(nVersion);
        
            _assert(factory.getVersion() == nVersion);
            map.put(IVersion, factory);
            }
        
        return factory;
        }
    
    // Accessor for the property "MessageFactoryMap"
    /**
     * Getter for property MessageFactoryMap.<p>
    * A map of MessageFactory objects, keyed by Protocol version.
     */
    protected java.util.Map getMessageFactoryMap()
        {
        return __m_MessageFactoryMap;
        }
    
    // From interface: com.tangosol.net.messaging.Protocol
    // Accessor for the property "Name"
    /**
     * Getter for property Name.<p>
    * The name of the Protocol.
    * 
    * @see com.tangosol.net.Protocol#getName
     */
    public String getName()
        {
        // import com.tangosol.util.ClassHelper;
        
        String sName = __m_Name;
        if (sName == null)
            {
            setName(sName = ClassHelper.getSimpleName(getClass()));
            }
        
        return sName;
        }
    
    // From interface: com.tangosol.net.messaging.Protocol
    public int getSupportedVersion()
        {
        return getVersionSupported();
        }
    
    // Accessor for the property "VersionCurrent"
    /**
     * Getter for property VersionCurrent.<p>
    * The newest protocol version supported by this Protocol.
    * 
    * @see com.tangosol.net.messaging.Protocol#getCurrentVersion
     */
    private int getVersionCurrent()
        {
        return __m_VersionCurrent;
        }
    
    // Accessor for the property "VersionSupported"
    /**
     * Getter for property VersionSupported.<p>
    * The oldest protocol version supported by this Protocol.
    * 
    * @see com.tangosol.net.messaging.Protocol#getSupportedVersion
     */
    private int getVersionSupported()
        {
        return __m_VersionSupported;
        }
    
    /**
     * Instantiate a new MessageFactory for the given version of this Protocol.
    * 
    * @param nVersion  the version of the Protocol that the returned
    * MessageFactory will use
    * 
    * @return a new MessageFactory for the given version of this Protocol
     */
    protected MessageFactory instantiateMessageFactory(int nVersion)
        {
        return null;
        }
    
    // Accessor for the property "MessageFactoryMap"
    /**
     * Setter for property MessageFactoryMap.<p>
    * A map of MessageFactory objects, keyed by Protocol version.
     */
    protected void setMessageFactoryMap(java.util.Map map)
        {
        __m_MessageFactoryMap = map;
        }
    
    // Accessor for the property "Name"
    /**
     * Setter for property Name.<p>
    * The name of the Protocol.
    * 
    * @see com.tangosol.net.Protocol#getName
     */
    protected void setName(String sName)
        {
        __m_Name = sName;
        }
    
    // Accessor for the property "VersionCurrent"
    /**
     * Setter for property VersionCurrent.<p>
    * The newest protocol version supported by this Protocol.
    * 
    * @see com.tangosol.net.messaging.Protocol#getCurrentVersion
     */
    protected void setVersionCurrent(int nVersion)
        {
        __m_VersionCurrent = nVersion;
        }
    
    // Accessor for the property "VersionSupported"
    /**
     * Setter for property VersionSupported.<p>
    * The oldest protocol version supported by this Protocol.
    * 
    * @see com.tangosol.net.messaging.Protocol#getSupportedVersion
     */
    protected void setVersionSupported(int nVersion)
        {
        __m_VersionSupported = nVersion;
        }
    }
