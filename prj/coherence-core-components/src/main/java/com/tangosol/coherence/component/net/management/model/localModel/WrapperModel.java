
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.model.localModel.WrapperModel

package com.tangosol.coherence.component.net.management.model.localModel;

import com.tangosol.coherence.component.net.management.model.RemoteModel;
import com.tangosol.net.management.annotation.Description;
import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.management.Attribute;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MXBean;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationEmitter;

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
public class WrapperModel
        extends    com.tangosol.coherence.component.net.management.model.LocalModel
        implements javax.management.NotificationListener
    {
    // ---- Fields declarations ----
    
    /**
     * Property _NotificationInfo
     *
     */
    private javax.management.MBeanNotificationInfo[] __m__NotificationInfo;
    
    /**
     * Property _Notify
     *
     * flag that is true if the model supports notifications.
     */
    private transient boolean __m__Notify;
    
    /**
     * Property Dynamic
     *
     * Determines if an MBean represented by this Model is a Dynamic MBean.
     */
    private transient boolean __m_Dynamic;
    
    /**
     * Property MBean
     *
     * The MBean wrapped by this WrapperModel.
     */
    private transient Object __m_MBean;
    
    /**
     * Property MBeanInfo
     *
     * The MBeanInfo that describes the management interface of the wrapped
     * MBean managed by this WrapperModel.
     */
    private transient javax.management.MBeanInfo __m_MBeanInfo;
    
    /**
     * Property MBeanInfoCache
     *
     * A cache of MBeanInfo objects keyed by MBean classes.
     */
    private static transient java.util.Map __s_MBeanInfoCache;
    
    // Default constructor
    public WrapperModel()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public WrapperModel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.management.model.localModel.WrapperModel();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/model/localModel/WrapperModel".replace('/', '.'));
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
    
    /**
     * Add this Model as a notification listener to the corresponding (Standard
    * or Dynamic) MBean.
     */
    protected void _addNotificationListener()
        {
        // import javax.management.NotificationBroadcaster;
        
        try
            {
            Object oBean = getMBean();
            if (oBean instanceof NotificationBroadcaster)
                {
                ((NotificationBroadcaster) oBean).addNotificationListener(this, null, null);
                }
            }
        catch (Throwable e)
            {
            _trace("Failed to add notification listener on MBean "
                + get_ModelName() + "\n" + getStackTrace(e), 3);
            }
        }
    
    // Declared at the super level
    /**
     * Add the Local Notification Listener on a Local Model to a Local Model
    *  
     */
    public com.tangosol.coherence.component.net.management.listenerHolder.LocalHolder _addNotificationListener(javax.management.NotificationListener listener, javax.management.NotificationFilter filter, Object handback)
        {
        if (!is_SubscribedTo())
            {
            _addNotificationListener();
            }
        
        return super._addNotificationListener(listener, filter, handback);
        }
    
    // Declared at the super level
    /**
     * Subscribe to the local model from a remote model using the Notification
    * Listener Reference
     */
    public void _addRemoteNotificationListener(com.tangosol.coherence.component.net.management.listenerHolder.RemoteHolder holder, com.tangosol.coherence.component.net.management.Connector connector)
        {
        if (!is_SubscribedTo())
            {
            _addNotificationListener();
            }
        
        super._addRemoteNotificationListener(holder, connector);
        }
    
    /**
     * Remove this Model as a notification listener from the corresponding
    * (Standard and Dynamic) MBean.
     */
    protected void _removeNotificationListener()
        {
        // import javax.management.NotificationBroadcaster;
        // import javax.management.NotificationEmitter;
        
        Object oBean = getMBean();
        try
            {
            if (oBean instanceof NotificationEmitter)
                {
                ((NotificationEmitter) oBean).removeNotificationListener(this, null, null);
                }
            else
                {
                if (oBean instanceof NotificationBroadcaster)
                    {
                    ((NotificationBroadcaster) oBean).removeNotificationListener(this);
                    }
                }
            }
        catch (Throwable e) // ListenerNotFoundException
            {
            _trace("Failed to remove notification listener on MBean "
                 + get_ModelName() + "\n" + getStackTrace(e), 3);
            }
        }
    
    // Declared at the super level
    /**
     * Remove all the notifications for the specified listener from the model.
    * 
    * @return a Set<LocalHolder> of holders associated with the specified
    * listener
     */
    public java.util.Set _removeNotificationListener(javax.management.NotificationListener listener)
        {
        try
            {
            return super._removeNotificationListener(listener);
            }
        finally
            {
            if (!is_SubscribedTo())
                {
                _removeNotificationListener();
                }
            }
        }
    
    // Declared at the super level
    /**
     * Remove the Notification Listener from the model.
    * 
    * @return the LocalHolder representing the listener/filter/handback tuple
    * or null if the registration did not exist
     */
    public com.tangosol.coherence.component.net.management.listenerHolder.LocalHolder _removeNotificationListener(javax.management.NotificationListener listener, javax.management.NotificationFilter filter, Object handback)
        {
        try
            {
            return super._removeNotificationListener(listener, filter, handback);
            }
        finally
            {
            if (!is_SubscribedTo())
                {
                _removeNotificationListener();
                }
            }
        }
    
    // Declared at the super level
    /**
     * Remove all notifications from the model.
     */
    public void _removeNotificationListeners()
        {
        if (is_SubscribedTo())
            {
            _removeNotificationListener();
            }
        
        super._removeNotificationListeners();
        }
    
    // Declared at the super level
    /**
     * Unsubscribe remote notifications represented by the specified holder id
    * for the given member.
     */
    public void _removeRemoteNotificationListener(int nMember, long lHolderId)
        {
        super._removeRemoteNotificationListener(nMember, lHolderId);
        
        if (!is_SubscribedTo())
            {
            _removeNotificationListener();
            }
        }
    
    /**
     * Check whether or not the specified exception could be ignored. The caller
    * would be responsible for returning an appropriate default value.
     */
    protected void checkIgnoreException(Throwable e, String sMsg)
        {
        // import com.tangosol.util.Base;
        
        if (e instanceof UnsupportedOperationException
         || e.getCause() instanceof UnsupportedOperationException)
            {
            // Some of the base JVM Management (MemoryPool) Objects throw UOE
            // or RuntimeMBeanException when the option is "disabled"; ignore...
            return;
            }
        throw Base.ensureRuntimeException(e, sMsg);
        }
    
    /**
     * Search for the interface with the given name on the specified class. 
    * This method does not load the class with the given name.
    * Check if the interface follows the standard MBean or MXBean standards:
    * 1) A standard MBean is defined by a Java interface called SomethingMBean
    * that is located in the same package as a Java class called Something that
    * implements that interface; 
    * 2) An MXBean is defined by a Java interface called SomethingMXBean and a
    * Java class that implements that interface, but could be located in
    * another package; 
    * 3) The annotation @MXBean can be also used to annotate the Java interface
    * in (2), instead of requiring the interface's name to be followed by the
    * MXBean suffix.
    * Refer: http://docs.oracle.com/javase/tutorial/jmx/mbeans
    * 
    * @return the interface class if found; null otherwise
     */
    protected static Class findCompliantInterface(Class clz, String sName)
        {
        // import javax.management.MXBean;
        
        _assert(clz   != null);
        _assert(sName != null);
        
        if (clz.isInterface())
            {
            // Standard MBean or MXBean or @MXBean Annotation
            if (clz.getName().equals(sName+"MBean") ||
                clz.getName().endsWith("MXBean") ||
                clz.isAnnotationPresent(MXBean.class))
                {
                return clz;
                }
            }
        
        Class[] aclz = clz.getInterfaces();
        for (int i = 0, c = aclz == null ? 0 : aclz.length; i < c; i++)
            {
            Class clzIntf = findCompliantInterface(aclz[i], sName);
            if (clzIntf != null)
                {
                return clzIntf;
                }
            }
        
        return null;
        }
    
    // Declared at the super level
    /**
     * Getter for property _MBeanComponent.<p>
    * The name of the corresponding MBean component. If not overridden at the
    * specific Model subcomponent, the naming convention is:
    * 
    * sMBeanName = "Component.Manageable.ModelAdapter." + 
    * (get_Name() - "Model") + "MBean";
    * The name of the corresponding MBean component. If not overriden at the
    * specific Model subcomponent, the naming convention is:
    * 
    * sMBeanName = "Component.Manageable.ModelAdapter." + 
    * get_Name().replace("Model", "MBean");
     */
    public String get_MBeanComponent()
        {
        // Theoretically we could improve the generic algorithm to determine this name.
        return is_Notify()
             ? "Component.Manageable.ModelAdapter.WrapperMBean.WrapperEmitterMBean"
             : "Component.Manageable.ModelAdapter.WrapperMBean";
        }
    
    // Accessor for the property "_NotificationInfo"
    /**
     * Getter for property _NotificationInfo.<p>
     */
    public javax.management.MBeanNotificationInfo[] get_NotificationInfo()
        {
        return __m__NotificationInfo;
        }
    
    /**
     * Subclassing support.
     */
    protected Object getAttribute(javax.management.MBeanAttributeInfo attrInfo)
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ClassHelper;
        // import javax.management.DynamicMBean;
        
        Object oBean = getMBean();
        String sAttr = attrInfo.getName();
        
        try
            {
            if (oBean instanceof DynamicMBean)
                {
                return ((DynamicMBean) oBean).getAttribute(sAttr);
                }
            else
                {
                String sMethodName = attrInfo.isIs() ? "is"  + sAttr
                                                     : "get" + sAttr;
                return invoke(sMethodName, ClassHelper.VOID);
                }
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    
    // Accessor for the property "MBean"
    /**
     * Getter for property MBean.<p>
    * The MBean wrapped by this WrapperModel.
     */
    public Object getMBean()
        {
        return __m_MBean;
        }
    
    // Accessor for the property "MBeanInfo"
    /**
     * Getter for property MBeanInfo.<p>
    * The MBeanInfo that describes the management interface of the wrapped
    * MBean managed by this WrapperModel.
     */
    public javax.management.MBeanInfo getMBeanInfo()
        {
        return __m_MBeanInfo;
        }
    
    // Accessor for the property "MBeanInfo"
    /**
     * Return an MBeanInfo for the given MBean object.
    * 
    * @throws NotCompliantMBeanException if the given object is not a compliant
    * Dynamic or Standard MBean
     */
    protected synchronized javax.management.MBeanInfo getMBeanInfo(Object oBean)
            throws javax.management.NotCompliantMBeanException
        {
        // import java.util.Map;
        // import java.util.WeakHashMap;
        // import javax.management.DynamicMBean;
        // import javax.management.MBeanInfo;
        // import javax.management.NotificationEmitter;
        
        _assert(oBean != null);
        
        Class clzBean  = oBean.getClass();
        Map   mapCache = getMBeanInfoCache();
        
        if (oBean instanceof NotificationEmitter)
            {
            set_NotificationInfo(((NotificationEmitter) oBean)
                                        .getNotificationInfo());
            set_Notify(true); 
            }
        
        if (mapCache == null)
            {
            setMBeanInfoCache(mapCache = new WeakHashMap());
            }
        
        MBeanInfo info = (MBeanInfo) mapCache.get(clzBean);
        if (info == null)
            {
            if (oBean instanceof DynamicMBean)
                {
                return ((DynamicMBean) oBean).getMBeanInfo();
                }
            else
                {
                info = introspectMBean(clzBean);
                }
        
            mapCache.put(clzBean, info);
            }
        
        return info;
        }
    
    // Accessor for the property "MBeanInfoCache"
    /**
     * Getter for property MBeanInfoCache.<p>
    * A cache of MBeanInfo objects keyed by MBean classes.
     */
    protected static java.util.Map getMBeanInfoCache()
        {
        return __s_MBeanInfoCache;
        }
    
    /**
     * Return the MBean interface class that defines the management interface
    * for the given Standard MBean or MXBean class.
    * 
    * @return the MBean interface class
    * @throw NotCompliantMBeanException if the given class is not a compliant
    * Standard MBean or MXBean class
     */
    protected static Class getMBeanInterface(Class clzBean)
            throws javax.management.NotCompliantMBeanException
        {
        // import java.lang.reflect.Modifier;
        // import javax.management.NotCompliantMBeanException;
        
        _assert(clzBean != null);
        
        if (clzBean.isInterface() || clzBean.isPrimitive())
            {
            throw new NotCompliantMBeanException("Illegal MBean type: "
                + clzBean);
            }
        
        for (Class clz = clzBean; clz != null; clz = clz.getSuperclass())
            {
            Class clzIntf = findCompliantInterface(clz, clz.getName());
        
            if (clzIntf != null && Modifier.isPublic(clzIntf.getModifiers()))
                {
                return clzIntf;
                }
            }
        
        throw new NotCompliantMBeanException("Illegal MBean: " + clzBean +
            " neither follows the Standard MBean conventions nor the MXBean conventions");
        }
    
    // From interface: javax.management.NotificationListener
    public void handleNotification(javax.management.Notification notification, Object handback)
        {
        _handleNotification(notification);
        }
    
    /**
     * Return the MBeanInfo that describes the management interface exposed by
    * the given Standard MBean interface.
    * 
    * @throw NotCompliantMBeanException if the given class is not a compliant
    * Standard MBean class
     */
    protected javax.management.MBeanInfo introspectMBean(Class clzBean)
            throws javax.management.NotCompliantMBeanException
        {
        // import com.tangosol.net.management.annotation.Description;
        // import com.tangosol.util.Base;
        // import java.lang.reflect.Method;
        // import java.util.ArrayList;
        // import java.util.HashMap;
        // import java.util.List;
        // import java.util.Map;
        // import javax.management.MBeanInfo;
        // import javax.management.MBeanAttributeInfo;
        // import javax.management.MBeanOperationInfo;
        // import javax.management.NotCompliantMBeanException;
        
        Class       clzBeanIntf    = getMBeanInterface(clzBean);
        
        Method[]    aMethod        = clzBeanIntf.getMethods();
        int         cMethod        = aMethod == null ? 0 : aMethod.length;
        
        Map         mapAttrInfo    = new HashMap(cMethod);
        List        listOpInfo     = new ArrayList(cMethod);
        Description descrMBean     = (Description) clzBeanIntf.getAnnotation(Description.class);
            
        String      sAttrDesc      = "MBean attribute exposed for management.";
        String      sOpDesc        = "MBean operation exposed for management.";
        String      sDesc          = "MBean(Class=" + clzBean.getName() +
                                         ", Interface=" + clzBeanIntf.getName() + ")";
        // If there is a description annotation for the MBean interface
        // we replace the standard description.                              
        if (descrMBean != null)
            {
            sDesc = descrMBean.value();
            }
        
        // find all attributes and operations on the standard mbean interface
        for (int i = 0; i < cMethod; i++)
            {
            Method  method = aMethod[i];
            String  sName  = method.getName();
            Class[] aclz   = method.getParameterTypes();
            Class   clz    = method.getReturnType();
            int     cParam = aclz == null ? 0 : aclz.length;
        
            String  sAttrName = null;
            Class   clzAttr   = null;
            boolean fReadable = false;
            boolean fWritable = false;
            boolean fIs       = false;
        
            Description descrMethod = (Description) method.getAnnotation(Description.class);
             
            // filter outlier operations:
            // X(), XY(), XYZ(), is(), and methods with more than 1 parameter
            if ((sName.length() >= 4 || sName.startsWith("is")) &&
                !sName.equals("is") &&
                cParam <= 1)
                {
                // process getters
                if (cParam == 0 && clz != Void.TYPE)
                    {
                    if (sName.startsWith("get"))
                        {
                        sAttrName = sName.substring(3);
                        }
                    else if (sName.startsWith("is") &&
                             (clz == Boolean.TYPE || clz == Boolean.class))
                        {
                        sAttrName = sName.substring(2);
                        fIs       = true;
                        }
                
                    clzAttr   = clz;
                    fReadable = true;
                    }
        
                // process setters
                else if (cParam == 1 && clz == Void.TYPE)
                    {
                    if (sName.startsWith("set"))
                        {
                        sAttrName = sName.substring(3);
                        clzAttr   = aclz[0];
                        fWritable = true;
                        }
                    }
                }
        
            // handle operations
            if (sAttrName == null)
                {
                if (descrMethod != null)
                    {
                    sOpDesc = descrMethod.value();
                    }
                listOpInfo.add(new MBeanOperationInfo(sOpDesc, method));
                }
            
            // handle attributes
            else
                {
                MBeanAttributeInfo attrInfo =
                    (MBeanAttributeInfo) mapAttrInfo.get(sAttrName);
        
                if (attrInfo != null)
                    {                    
                    if (clzAttr.getName().equals(attrInfo.getType()))
                        {
                        if (fReadable && attrInfo.isReadable() &&
                            fIs != attrInfo.isIs())
                            {
                            throw Base.ensureRuntimeException(
                                new NotCompliantMBeanException("Getter is"
                                    + sAttrName
                                    + " cannot co-exist with getter get"
                                    + sAttrName));
                            }
                        else
                            {
                            fReadable = fReadable || attrInfo.isReadable();
                            fWritable = fWritable || attrInfo.isWritable();
                            fIs       = fIs       || attrInfo.isIs();
                            }
                        }
                    else
                        {
                        if (fWritable == attrInfo.isWritable())
                            {
                            throw Base.ensureRuntimeException(
                                new NotCompliantMBeanException(
                                    "Type mismatch between parameters of set"
                                    + sAttrName
                                    + " methods."));
                            }
                        else
                            {
                            throw Base.ensureRuntimeException(
                                new NotCompliantMBeanException(
                                    "Type mismatch between parameters of get or is"
                                    + sAttrName
                                    + " and set"
                                    + sAttrName
                                    + " methods."));
                            }
                        }
                    }
                    if (descrMethod != null)
                        {
                        sAttrDesc = descrMethod.value();
                        }
                    attrInfo = new MBeanAttributeInfo(sAttrName,
                                                  clzAttr.getName(),
                                                  sAttrDesc,
                                                  fReadable,
                                                  fWritable,
                                                  fIs);
        
                mapAttrInfo.put(sAttrName, attrInfo);
                }
            }
        
        // assemble the final MBeanInfo
        MBeanAttributeInfo[] aAttrInfo = (MBeanAttributeInfo[])
            mapAttrInfo.values().toArray(new MBeanAttributeInfo[mapAttrInfo.size()]);
        
        MBeanOperationInfo[] aOpInfo = (MBeanOperationInfo[])
            listOpInfo.toArray(new MBeanOperationInfo[listOpInfo.size()]);
        
        return new MBeanInfo(clzBean.getName(),
                             sDesc,
                             aAttrInfo,
                             null,
                             aOpInfo,
                             get_NotificationInfo());
        }
    
    // Declared at the super level
    /**
     * Invoke the method with the specified name and parameters on the MBean
    * represented by this model.
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
     */
    public Object invoke(int nOp, String sName, Object[] aoParam, String[] asSignature)
            throws java.lang.IllegalAccessException,
                   java.lang.NoSuchMethodException,
                   java.lang.reflect.InvocationTargetException,
                   javax.management.MBeanException
        {
        // import Component.Net.Management.Model.RemoteModel;
        // import javax.management.DynamicMBean;
        // import javax.management.Attribute;
        
        if (isDynamic())
            {
            try
                {
                DynamicMBean oBean = (DynamicMBean) getMBean();
                switch (nOp)
                    {
                    case RemoteModel.OP_GET:
                        return oBean.getAttribute(sName);
         
                    case RemoteModel.OP_SET:
                        oBean.setAttribute(new Attribute(sName, aoParam[0]));
                        return null;
        
                    case RemoteModel.OP_INVOKE:
                        return oBean.invoke(sName, aoParam, asSignature);
        
                    default:
                        throw new IllegalStateException();
                    }
                }
            catch (Exception e)
                {
                checkIgnoreException(e, null);
                return null;
                }
            }
        else
            {
            return super.invoke(nOp, sName, aoParam, asSignature);
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
        // import com.tangosol.util.ClassHelper;
        
        Object oBean = getMBean();
        _assert(oBean != null, "Managed object was not set");
        
        return ClassHelper.invoke(oBean, sMethod, aoParam);
        }
    
    // Accessor for the property "_Notify"
    /**
     * Getter for property _Notify.<p>
    * flag that is true if the model supports notifications.
     */
    public boolean is_Notify()
        {
        return __m__Notify;
        }
    
    // Accessor for the property "Dynamic"
    /**
     * True if the underlying model implements javax.management.DynamicMBean.
     */
    public boolean isDynamic()
        {
        return __m_Dynamic;
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void readExternal(java.io.DataInput in)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        // import java.util.Map;
        
        super.readExternal(in);
        
        Map mapSnapshot = get_SnapshotMap();
        
        setDynamic(in.readBoolean());
        set_Notify(in.readBoolean());
        
        int cAttr  = ExternalizableHelper.readInt(in);
        for (int i = 0; i < cAttr; i++)
            {
            String sAttrName = null;
            Object oAttrValue;
            try
                {
                sAttrName  = ExternalizableHelper.readSafeUTF(in);
                oAttrValue = ExternalizableHelper.readObject(in);
                }
            catch (Exception e)
                {
                _trace("The MBean attribute \"" + sAttrName + "\" could not be retrieved; "
                    + "all remaining attributes will be ignored:\n"
                    + getStackTrace(e), 2);
                break;
                }
        
            mapSnapshot.put(sAttrName, oAttrValue);
            }
        }
    
    /**
     * Must be supplemented at each specific Model implementation.
     */
    protected void readExternalImpl(java.io.DataInput in)
            throws java.io.IOException
        {
        }
    
    // Accessor for the property "_NotificationInfo"
    /**
     * Setter for property _NotificationInfo.<p>
     */
    public void set_NotificationInfo(javax.management.MBeanNotificationInfo[] p_NotificationInfo)
        {
        __m__NotificationInfo = p_NotificationInfo;
        }
    
    // Accessor for the property "_Notify"
    /**
     * Setter for property _Notify.<p>
    * flag that is true if the model supports notifications.
     */
    public void set_Notify(boolean notifyFlag)
        {
        __m__Notify = notifyFlag;
        }
    
    // Accessor for the property "Dynamic"
    /**
     * Setter for property Dynamic.<p>
    * Determines if an MBean represented by this Model is a Dynamic MBean.
     */
    protected void setDynamic(boolean fDynamic)
        {
        __m_Dynamic = fDynamic;
        }
    
    // Accessor for the property "MBean"
    /**
     * Set the underlying Bean and the associated MBeanInfo
     */
    public void setMBean(Object oBean)
        {
        // import com.tangosol.util.Base;
        // import javax.management.NotCompliantMBeanException;
        // import javax.management.DynamicMBean;
        
        _assert(oBean != null, "Managed object cannot be null");
        
        setDynamic(oBean instanceof DynamicMBean);
        
        try
            {
            setMBeanInfo(getMBeanInfo(oBean));
            }
        catch (NotCompliantMBeanException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        
        __m_MBean = (oBean);
        }
    
    // Accessor for the property "MBeanInfo"
    /**
     * Setter for property MBeanInfo.<p>
    * The MBeanInfo that describes the management interface of the wrapped
    * MBean managed by this WrapperModel.
     */
    public void setMBeanInfo(javax.management.MBeanInfo infoBean)
        {
        __m_MBeanInfo = infoBean;
        }
    
    // Accessor for the property "MBeanInfoCache"
    /**
     * Setter for property MBeanInfoCache.<p>
    * A cache of MBeanInfo objects keyed by MBean classes.
     */
    protected static void setMBeanInfoCache(java.util.Map mapCache)
        {
        __s_MBeanInfoCache = mapCache;
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void writeExternal(java.io.DataOutput out)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        // import javax.management.MBeanAttributeInfo;
        // import javax.management.MBeanInfo;
        
        super.writeExternal(out);
        
        MBeanInfo info = getMBeanInfo();
        
        _assert(info != null);
        
        out.writeBoolean(isDynamic());
        out.writeBoolean(is_Notify());
        
        MBeanAttributeInfo[] aAttrInfo = info.getAttributes();
        
        // prepare the attributes
        
        int      cAttrs  = aAttrInfo == null ? 0 : aAttrInfo.length;
        String[] asName  = new String[cAttrs];
        Object[] aoValue = new Object[cAttrs];
        int      ix      = 0;
        
        for (int i = 0; i < cAttrs; i++)
            {
            MBeanAttributeInfo attrinfo = aAttrInfo[i];
            if (attrinfo.isReadable())
                {
                try
                    {
                    asName [ix] = attrinfo.getName();
                    aoValue[ix] = getAttribute(attrinfo);
                    ix++;
                    }
                catch (Exception e)
                    {
                    _trace("The value of the attribute \"" + attrinfo.getName()
                        + "\" for MBean \"" + get_ModelName()
                        + "\" could not be retrieved and is ignored; " + getStackTrace(e), 2);
                    continue;
                    }
                }
            }
        
        ExternalizableHelper.writeInt(out, cAttrs = ix);
        
        for (int i = 0; i < cAttrs; i++)
            {
            try
                {
                ExternalizableHelper.writeSafeUTF(out, asName[i]);
                ExternalizableHelper.writeObject (out, aoValue[i]);
                }
            catch (Exception e)
                {
                _trace("The MBean attribute \"" + asName[i] + "\" could not be serialized; "
                    + "all remaining attributes will be ignored:\n"
                    + getStackTrace(e), 2);
                break;
                }
            }
        }
    }
