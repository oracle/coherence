
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.BackingMapManagerContext

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
import com.tangosol.net.CacheService;
import com.tangosol.net.security.LocalPermission;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.NullImplementation;

/**
 * The BackingMapManagerContext implementation.
 * 
 * Added decoration support methods in Coherence 3.2.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class BackingMapManagerContext
        extends    com.tangosol.coherence.component.Util
        implements com.tangosol.net.BackingMapManagerContext,
                   com.tangosol.util.MapListener
    {
    // ---- Fields declarations ----
    
    /**
     * Property ClassLoader
     *
     * The ClassLoader associated with this context.
     */
    private transient ClassLoader __m_ClassLoader;
    
    /**
     * Property Config
     *
     * The configuration of the associated BackingMapManager.
     */
    private transient com.tangosol.run.xml.XmlElement __m_Config;
    
    /**
     * Property ConfigKey
     *
     * The key used to store the Config XmlElement in the ServiceConfigMap. The
     * value must be set by concrete Context implementations.
     */
    private transient Object __m_ConfigKey;
    
    /**
     * Property Manager
     *
     * The associated manager.
     */
    private transient com.tangosol.net.BackingMapManager __m_Manager;
    protected static com.tangosol.coherence.Component __singleton;
    
    // Initializing constructor
    public BackingMapManagerContext(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        com.tangosol.coherence.Component singleton = __singleton;
        
        return singleton;
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
            clz = Class.forName("com.tangosol.coherence/component/util/BackingMapManagerContext".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public Object addInternalValueDecoration(Object oValue, int nDecorId, Object oDecor)
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper;
        
        if (oValue instanceof Binary)
            {
            Binary binDecor =  (Binary) (oDecor instanceof Binary ?
                oDecor : getValueToInternalConverter().convert(oDecor));
            return ExternalizableHelper.decorate((Binary) oValue, nDecorId, binDecor);
            }
        else
            {
            throw new IllegalArgumentException("Invalid internal format: " + oValue);
            }
        }
    
    // From interface: com.tangosol.util.MapListener
    public void entryDeleted(com.tangosol.util.MapEvent evt)
        {
        }
    
    // From interface: com.tangosol.util.MapListener
    public void entryInserted(com.tangosol.util.MapEvent evt)
        {
        if (evt.getKey().equals(getConfigKey()))
            {
            onConfigUpdate(evt);
            }
        }
    
    // From interface: com.tangosol.util.MapListener
    public void entryUpdated(com.tangosol.util.MapEvent evt)
        {
        if (evt.getKey().equals(getConfigKey()))
            {
            onConfigUpdate(evt);
            }
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public java.util.Map getBackingMap(String sCacheName)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public com.tangosol.net.BackingMapContext getBackingMapContext(String sCacheName)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    // Accessor for the property "CacheService"
    /**
     * Getter for property CacheService.<p>
    * The associated CacheService.
     */
    public com.tangosol.net.CacheService getCacheService()
        {
        // import com.tangosol.net.CacheService;
        
        return (CacheService) get_Parent();
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    // Accessor for the property "ClassLoader"
    /**
     * Getter for property ClassLoader.<p>
    * The ClassLoader associated with this context.
     */
    public ClassLoader getClassLoader()
        {
        ClassLoader loader = __m_ClassLoader;
        
        return loader == null ? getCacheService().getContextClassLoader() : loader;
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    // Accessor for the property "Config"
    /**
     * Getter for property Config.<p>
    * The configuration of the associated BackingMapManager.
     */
    public com.tangosol.run.xml.XmlElement getConfig()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid;
        // import com.tangosol.net.CacheService;
        // import com.tangosol.run.xml.XmlElement;
        
        CacheService service = getCacheService();
        XmlElement   xmlCfg  = null;
        if (service instanceof Grid)
            {
            xmlCfg = (XmlElement) ((Grid) service).getServiceConfigMap().get(getConfigKey());
            }
        
        // ServiceConfigMap will not propagate the change to a mutable
        // object unless the reference itself changes; moreover
        // we should clone before any changes are made!
        
        return xmlCfg == null ? null : (XmlElement) xmlCfg.clone();
        }
    
    // Accessor for the property "ConfigKey"
    /**
     * Getter for property ConfigKey.<p>
    * The key used to store the Config XmlElement in the ServiceConfigMap. The
    * value must be set by concrete Context implementations.
     */
    public Object getConfigKey()
        {
        return __m_ConfigKey;
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public Object getInternalValueDecoration(Object oValue, int nDecorId)
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper;
        
        if (oValue instanceof Binary)
            {
            return getValueFromInternalConverter().convert(
                ExternalizableHelper.getDecoration((Binary) oValue, nDecorId));
            }
        else
            {
            throw new IllegalArgumentException("Invalid internal format: " + oValue);
            }
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public com.tangosol.util.Converter getKeyFromInternalConverter()
        {
        // import com.tangosol.util.NullImplementation;
        
        return NullImplementation.getConverter();
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public int getKeyPartition(Object oKey)
        {
        return 0;
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public com.tangosol.util.Converter getKeyToInternalConverter()
        {
        // import com.tangosol.util.NullImplementation;
        
        return NullImplementation.getConverter();
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    // Accessor for the property "Manager"
    /**
     * Getter for property Manager.<p>
    * The associated manager.
     */
    public com.tangosol.net.BackingMapManager getManager()
        {
        return __m_Manager;
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public java.util.Set getPartitionKeys(String sCacheName, int nPartition)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public com.tangosol.util.Converter getValueFromInternalConverter()
        {
        // import com.tangosol.util.NullImplementation;
        
        return NullImplementation.getConverter();
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public com.tangosol.util.Converter getValueToInternalConverter()
        {
        // import com.tangosol.util.NullImplementation;
        
        return NullImplementation.getConverter();
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public boolean isInternalValueDecorated(Object oValue, int nDecorId)
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper;
        
        if (oValue instanceof Binary)
            {
            return ExternalizableHelper.isDecorated((Binary) oValue, nDecorId);
            }
        else
            {
            throw new IllegalArgumentException("Invalid internal format: " + oValue);
            }
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public boolean isKeyOwned(Object oKey)
        {
        return getCacheService().isRunning();
        }
    
    protected void onConfigUpdate(com.tangosol.util.MapEvent evt)
        {
        // import com.tangosol.run.xml.XmlConfigurable as com.tangosol.run.xml.XmlConfigurable;
        // import com.tangosol.run.xml.XmlElement;
        
        ((com.tangosol.run.xml.XmlConfigurable) getManager()).
            setConfig((XmlElement) evt.getNewValue());
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public Object removeInternalValueDecoration(Object oValue, int nDecorId)
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper;
        
        if (oValue instanceof Binary)
            {
            return ExternalizableHelper.undecorate((Binary) oValue, nDecorId);
            }
        else
            {
            throw new IllegalArgumentException("Invalid internal format: " + oValue);
            }
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    // Accessor for the property "ClassLoader"
    /**
     * Setter for property ClassLoader.<p>
    * The ClassLoader associated with this context.
     */
    public void setClassLoader(ClassLoader loader)
        {
        // import com.tangosol.net.security.LocalPermission;
        
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            {
            security.checkPermission(
                new LocalPermission("BackingMapManagerContext.setClassLoader"));
            }
        
        __m_ClassLoader = (loader);
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    // Accessor for the property "Config"
    /**
     * Setter for property Config.<p>
    * The configuration of the associated BackingMapManager.
     */
    public void setConfig(com.tangosol.run.xml.XmlElement xml)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import com.tangosol.run.xml.XmlElement;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid) getCacheService();
        
        service.getServiceConfigMap().put(getConfigKey(), xml);
        }
    
    // Accessor for the property "ConfigKey"
    /**
     * Setter for property ConfigKey.<p>
    * The key used to store the Config XmlElement in the ServiceConfigMap. The
    * value must be set by concrete Context implementations.
     */
    protected void setConfigKey(Object oKey)
        {
        __m_ConfigKey = oKey;
        }
    
    // Accessor for the property "Manager"
    /**
     * Setter for property Manager.<p>
    * The associated manager.
     */
    public void setManager(com.tangosol.net.BackingMapManager manager)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import com.tangosol.run.xml.XmlConfigurable as com.tangosol.run.xml.XmlConfigurable;
        
        __m_Manager = (manager);
        
        if (manager instanceof com.tangosol.run.xml.XmlConfigurable)
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid) getCacheService();
        
            service.getServiceConfigMap().addMapListener(this);
            }
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_Name() + ": CacheService=" + getCacheService() +
               "\nConfig=" + getConfig();
        }
    }
