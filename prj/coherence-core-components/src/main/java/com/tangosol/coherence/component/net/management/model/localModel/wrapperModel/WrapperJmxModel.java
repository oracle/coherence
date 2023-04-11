
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.model.localModel.wrapperModel.WrapperJmxModel

package com.tangosol.coherence.component.net.management.model.localModel.wrapperModel;

import com.tangosol.coherence.component.net.management.model.RemoteModel;
import com.tangosol.net.management.MBeanReference;
import com.tangosol.util.Base;
import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Model components implement the JMX-managed functionality of the
 * corresponding MBeans without being dependent on any JMX classes and could be
 * used both in-process and out-of-process (relative to an MBeanServer).
 * 
 * The LocalModel components operate in two distinct modes: live and snapshot.
 * In the live mode all model methods call corresponding methods on managed
 * objects. The snapshot mode uses the _SnapshotMap to keep the attribute
 * values.
 * 
 * Every time a remote invocation is used by the RemoteModel to do a
 * setAttribute or invoke call, the snapshot model is refreshed.
 * 
 * A WrapperModel can be used to manage any object that can be classified as
 * either a Dynamic or Standard MBean (as defined by the JMX specification).
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class WrapperJmxModel
        extends    com.tangosol.coherence.component.net.management.model.localModel.WrapperModel
    {
    // ---- Fields declarations ----
    
    /**
     * Property MBeanServer
     *
     * The MBeanServer the underlying MBean is registered with.
     */
    private transient javax.management.MBeanServer __m_MBeanServer;
    
    /**
     * Property ObjectName
     *
     * Th ObjectName of the "wrapped" MBean.
     */
    private javax.management.ObjectName __m_ObjectName;
    
    // Default constructor
    public WrapperJmxModel()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public WrapperJmxModel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            set_SnapshotMap(new java.util.HashMap());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
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
        return new com.tangosol.coherence.component.net.management.model.localModel.wrapperModel.WrapperJmxModel();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/model/localModel/wrapperModel/WrapperJmxModel".replace('/', '.'));
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
    
    // Declared at the super level
    /**
     * Add the Model as a notification listener to the JMX MBean.
     */
    protected void _addNotificationListener()
        {
        try
            {
            if (is_Notify())
                {
                getMBeanServer().addNotificationListener(getObjectName(), this, null, null);
                }
            }
        catch (Throwable e) // InstanceNotFoundException
            {
            _trace("Failed to add notification listener on MBean "
                 + get_ModelName() + "\n" + getStackTrace(e), 3);
            }
        }
    
    // Declared at the super level
    /**
     * Remove the Model as a notification listener from the JMX MBean
     */
    protected void _removeNotificationListener()
        {
        try
            {
            if (is_Notify())
                {
                getMBeanServer().removeNotificationListener(getObjectName(), this);
                }
            }
        catch (Throwable e) // ListenerNotFoundException, InstanceNotFoundException
            {
            _trace("Failed to remove notification listener on MBean "
                 + get_ModelName() + "\n" + getStackTrace(e), 3);
            }
        }
    
    // Declared at the super level
    /**
     * Returns the name of the associated MBean component.
     */
    public String get_MBeanComponent()
        {
        // theoretically we could improve the generic algothim to determine this name
        return is_Notify()
             ? "Component.Manageable.ModelAdapter.WrapperMBean.WrapperJmxMBean.WrapperJmxEmitterMBean"
             : "Component.Manageable.ModelAdapter.WrapperMBean.WrapperJmxMBean";
        }
    
    // Declared at the super level
    /**
     * Subclassing support.
     */
    protected Object getAttribute(javax.management.MBeanAttributeInfo attrInfo)
        {
        // import Component.Net.Management.Model.RemoteModel;
        // import com.tangosol.util.Base;
        
        try
            {
            return invoke(RemoteModel.OP_GET, attrInfo.getName(), null);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    
    // Accessor for the property "MBeanServer"
    /**
     * Getter for property MBeanServer.<p>
    * The MBeanServer the underlying MBean is registered with.
     */
    public javax.management.MBeanServer getMBeanServer()
        {
        return __m_MBeanServer;
        }
    
    // Accessor for the property "ObjectName"
    /**
     * Getter for property ObjectName.<p>
    * Th ObjectName of the "wrapped" MBean.
     */
    public javax.management.ObjectName getObjectName()
        {
        return __m_ObjectName;
        }
    
    /**
     * The declared type is an Object to avoid compile-time dependency on the
    * JMX classes by the Gateway.
     */
    public void initialize(Object oRef)
        {
        // import com.tangosol.net.management.MBeanReference;
        // import com.tangosol.util.Base;
        
        MBeanReference ref = (MBeanReference) oRef;
        
        setObjectName(ref.getObjectName());
        setMBeanServer(ref.getMBeanServer());
        try
            {
            setMBeanInfo(getMBeanServer().getMBeanInfo(getObjectName()));
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    
    // Declared at the super level
    /**
     * Invoke the method with the specified name and parameters on the MBean
    * represented by this model.
    * Invoke the method with the specified name on the wrapped MBean with the
    * specified parameters.
     */
    public Object invoke(int nOp, String sName, Object[] aoParam)
            throws java.lang.IllegalAccessException,
                   java.lang.NoSuchMethodException,
                   java.lang.reflect.InvocationTargetException,
                   javax.management.MBeanException
        {
        return invoke(nOp, sName, aoParam, null);
        }
    
    // Declared at the super level
    /**
     * Invoke the method with the specified name and parameters on the MBean
    * represented by this model.
    * Invoke the method with the specified name and parameters on the MBean
    * represented by this model.
    * Invoke the method with the specified name on the wrapped MBean with the
    * specified parameters.
     */
    public Object invoke(int nOp, String sName, Object[] aoParam, String[] asSignature)
            throws java.lang.IllegalAccessException,
                   java.lang.NoSuchMethodException,
                   java.lang.reflect.InvocationTargetException,
                   javax.management.MBeanException
        {
        // import Component.Net.Management.Model.RemoteModel;
        // import javax.management.Attribute;
        // import javax.management.MBeanServer;
        // import javax.management.ObjectName;
        
        try
            {
            ObjectName  oname = (ObjectName) getObjectName();
            MBeanServer mbs   = (MBeanServer) getMBeanServer();
        
            switch (nOp)
                {
                case RemoteModel.OP_GET:
                    return mbs.getAttribute(oname, sName);
         
                case RemoteModel.OP_SET:
                    mbs.setAttribute(oname, new Attribute(sName, aoParam[0]));
                    return null;
        
                case RemoteModel.OP_INVOKE:
                    return mbs.invoke(oname, sName, aoParam, asSignature);
        
                default:
                    throw new IllegalStateException();
                }
            }
        catch (Exception e)
            {
            checkIgnoreException(e,
                "invoke failure:" + getObjectName() + "; method=" + sName);
            return null;
            }
        }
    
    // Declared at the super level
    /**
     * Invoke the method with the specified name on the wrapped MBean with the
    * specified parameters.
     */
    public Object invoke(String sMethod, Object[] aoParam)
            throws java.lang.IllegalAccessException,
                   java.lang.NoSuchMethodException,
                   java.lang.reflect.InvocationTargetException
        {
        // import com.tangosol.util.Base;
        
        try
            {
            return getMBeanServer().invoke(getObjectName(), sMethod, aoParam, null);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    
    // Declared at the super level
    /**
     * Getter for property _Notify.<p>
    * flag that is true if the model supports notifications.
     */
    public boolean is_Notify()
        {
        return getMBeanInfo().getNotifications().length > 0;
        }
    
    // Accessor for the property "MBeanServer"
    /**
     * Setter for property MBeanServer.<p>
    * The MBeanServer the underlying MBean is registered with.
     */
    protected void setMBeanServer(javax.management.MBeanServer mbs)
        {
        __m_MBeanServer = mbs;
        }
    
    // Accessor for the property "ObjectName"
    /**
     * Setter for property ObjectName.<p>
    * Th ObjectName of the "wrapped" MBean.
     */
    protected void setObjectName(javax.management.ObjectName oname)
        {
        __m_ObjectName = oname;
        }
    }
