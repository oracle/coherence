
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.safeService.safeCacheService.SafeDistributedCacheService

package com.tangosol.coherence.component.util.safeService.safeCacheService;

import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.PartitionedService;
import com.tangosol.util.Binary;
import com.tangosol.util.Converter;

/*
* Integrates
*     com.tangosol.net.DistributedCacheService
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class SafeDistributedCacheService
        extends    com.tangosol.coherence.component.util.safeService.SafeCacheService
        implements com.tangosol.net.DistributedCacheService
    {
    // ---- Fields declarations ----
    private static com.tangosol.util.ListMap __mapChildren;
    
    // Static initializer
    static
        {
        __initStatic();
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("DestroyCacheAction", com.tangosol.coherence.component.util.safeService.SafeCacheService.DestroyCacheAction.get_CLASS());
        __mapChildren.put("EnsureServiceAction", com.tangosol.coherence.component.util.SafeService.EnsureServiceAction.get_CLASS());
        __mapChildren.put("ReleaseCacheAction", com.tangosol.coherence.component.util.safeService.SafeCacheService.ReleaseCacheAction.get_CLASS());
        __mapChildren.put("StartAction", com.tangosol.coherence.component.util.SafeService.StartAction.get_CLASS());
        __mapChildren.put("Unlockable", com.tangosol.coherence.component.util.SafeService.Unlockable.get_CLASS());
        }
    
    // Default constructor
    public SafeDistributedCacheService()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public SafeDistributedCacheService(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        
        if (fInit)
            {
            __init();
            }
        }
    
    // Main initializer
    public void __init()
        {
        // private initialization
        __initPrivate();
        
        // state initialization: public and protected properties
        try
            {
            setLock(new java.util.concurrent.locks.ReentrantLock());
            setResourceRegistry(new com.tangosol.util.SimpleResourceRegistry());
            setSafeServiceState(0);
            setScopedCacheStore(new com.tangosol.net.internal.ScopedCacheReferenceStore());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        
        // signal the end of the initialization
        set_Constructed(true);
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
        return new com.tangosol.coherence.component.util.safeService.safeCacheService.SafeDistributedCacheService();
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
            clz = Class.forName("com.tangosol.coherence/component/util/safeService/safeCacheService/SafeDistributedCacheService".replace('/', '.'));
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
    
    //++ getter for autogen property _ChildClasses
    /**
     * This is an auto-generated method that returns the map of design time
    * [static] children.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    protected java.util.Map get_ChildClasses()
        {
        return __mapChildren;
        }
    
    //++ com.tangosol.net.DistributedCacheService integration
    // Access optimization
    // properties integration
    // methods integration
    /**
     * Getter for property BackupCount.<p>
     */
    public int getBackupCount()
        {
        return ((com.tangosol.net.DistributedCacheService) getRunningCacheService()).getBackupCount();
        }
    public com.tangosol.net.Member getBackupOwner(int nPartition, int iStore)
        {
        return ((com.tangosol.net.DistributedCacheService) getRunningCacheService()).getBackupOwner(nPartition, iStore);
        }
    public int getBackupStrength()
        {
        return ((com.tangosol.net.DistributedCacheService) getRunningCacheService()).getBackupStrength();
        }
    public String getBackupStrengthName()
        {
        return ((com.tangosol.net.DistributedCacheService) getRunningCacheService()).getBackupStrengthName();
        }
    /**
     * Getter for property KeyAssociator.<p>
     */
    public com.tangosol.net.partition.KeyAssociator getKeyAssociator()
        {
        return ((com.tangosol.net.DistributedCacheService) getRunningCacheService()).getKeyAssociator();
        }
    public com.tangosol.net.Member getKeyOwner(Object oKey)
        {
        return ((com.tangosol.net.DistributedCacheService) getRunningCacheService()).getKeyOwner(oKey);
        }

    @Override
    public <V> Converter<V, Binary> instantiateKeyToBinaryConverter(ClassLoader loader, boolean fPassThrough)
        {
        return ((DistributedCacheService) getRunningCacheService()).instantiateKeyToBinaryConverter(loader, fPassThrough);
        }

    /**
     * Getter for property KeyPartitioningStrategy.<p>
     */
    public com.tangosol.net.partition.KeyPartitioningStrategy getKeyPartitioningStrategy()
        {
        return ((com.tangosol.net.DistributedCacheService) getRunningCacheService()).getKeyPartitioningStrategy();
        }
    public com.tangosol.net.partition.PartitionSet getOwnedPartitions(com.tangosol.net.Member member)
        {
        return ((com.tangosol.net.DistributedCacheService) getRunningCacheService()).getOwnedPartitions(member);
        }
    /**
     * Getter for property OwnershipEnabledMembers.<p>
     */
    public java.util.Set getOwnershipEnabledMembers()
        {
        return ((com.tangosol.net.DistributedCacheService) getRunningCacheService()).getOwnershipEnabledMembers();
        }
    /**
     * Getter for property OwnershipSenior.<p>
     */
    public com.tangosol.net.Member getOwnershipSenior()
        {
        return ((com.tangosol.net.DistributedCacheService) getRunningCacheService()).getOwnershipSenior();
        }
    /**
     * Getter for property OwnershipVersion.<p>
     */
    public int getOwnershipVersion(int nPartition)
        {
        return ((com.tangosol.net.DistributedCacheService) getRunningCacheService()).getOwnershipVersion(nPartition);
        }
    /**
     * Getter for property PartitionAssignmentStrategy.<p>
     */
    public com.tangosol.net.partition.PartitionAssignmentStrategy getPartitionAssignmentStrategy()
        {
        return ((com.tangosol.net.DistributedCacheService) getRunningCacheService()).getPartitionAssignmentStrategy();
        }
    /**
     * Getter for property PartitionCount.<p>
     */
    public int getPartitionCount()
        {
        return ((com.tangosol.net.DistributedCacheService) getRunningCacheService()).getPartitionCount();
        }
    public com.tangosol.net.Member getPartitionOwner(int nPartition)
        {
        return ((com.tangosol.net.DistributedCacheService) getRunningCacheService()).getPartitionOwner(nPartition);
        }
    public String getPersistenceMode()
        {
        return ((com.tangosol.net.DistributedCacheService) getRunningCacheService()).getPersistenceMode();
        }
    /**
     * Getter for property StorageEnabledMembers.<p>
     */
    public java.util.Set getStorageEnabledMembers()
        {
        return ((com.tangosol.net.DistributedCacheService) getRunningCacheService()).getStorageEnabledMembers();
        }
    /**
     * Getter for property LocalStorageEnabled.<p>
     */
    public boolean isLocalStorageEnabled()
        {
        return ((com.tangosol.net.DistributedCacheService) getRunningCacheService()).isLocalStorageEnabled();
        }
    //-- com.tangosol.net.DistributedCacheService integration
    
    // From interface: com.tangosol.net.DistributedCacheService
    public void addPartitionListener(com.tangosol.net.partition.PartitionListener l)
        {
        // import com.tangosol.net.PartitionedService;
        
        PartitionedService service = (PartitionedService) getInternalService();
        
        if (service != null && service.isRunning())
            {
            service.addPartitionListener(l);
            }
        }
    
    // From interface: com.tangosol.net.DistributedCacheService
    public void removePartitionListener(com.tangosol.net.partition.PartitionListener l)
        {
        // import com.tangosol.net.PartitionedService;
        
        PartitionedService service = (PartitionedService) getInternalService();
        
        if (service != null && service.isRunning())
            {
            service.removePartitionListener(l);
            }
        }
    }
