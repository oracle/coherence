
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.Manageable

package com.tangosol.coherence.component;

import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.annotation.MetricsScope;
import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.comparator.InverseComparator;
import com.tangosol.util.comparator.SafeComparator;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.ImmutableDescriptor;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.RuntimeErrorException;
import javax.management.RuntimeMBeanException;
import javax.management.RuntimeOperationsException;

/**
 * Manageable component is a DynamicMBean implementation. 
 * 
 * For non-abstract components It automatically generates:
 *   1) MBeanInfo based on the component doc.
 *       Note: remember not to use the asterisk ('); use (`) instead.
 * 
 *   2) AttributeInfo for properties that:
 *      - are instance (non static);
 *      - not from super;
 *      - have names _not_ starting with underscore;
 *      - have at least one public accessor.
 * 
 *   3) MethodInfo for methods that:
 *      - are instance (non static);
 *      - have manual origin; not from super;
 *      - have names _not_ starting with underscore;
 *      - have public access.
 * 
 * As of Coherence 12.2.1, the doc for component, properties and methods could
 * have an optional @descriptor tag at the end. The format of that tag is:
 * 
 * @descriptor key1=value1,key2=value2,...
 * 
 * If this tag exists, the corresponding Descriptor will be added to the
 * MBeanInfo, AttributeInfo or MethodInfo.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Manageable
        extends    com.tangosol.coherence.Component
        implements javax.management.DynamicMBean,
                   javax.management.MBeanRegistration
    {
    // ---- Fields declarations ----
    
    /**
     * Property _Constructable
     *
     * Specifies whether the MBean is constructable. Used by auto-generated
     * code.
     */
    private transient boolean __m__Constructable;
    
    /**
     * Property _MBeanServer
     *
     * The MBeanServer this MBean is registered with.
     */
    private transient javax.management.MBeanServer __m__MBeanServer;
    
    /**
     * Property _NotificationDescription
     *
     * The description of the Notification that could be emitted by this MBean.
     */
    private String __m__NotificationDescription;
    
    /**
     * Property _NotificationName
     *
     * Then Notification name (class). According to the JMX contracts, this
     * should be a fully qualified Java class name of the corresponding
     * notification. Currently, only a single Notification class per MBean is
     * supported.
     */
    private String __m__NotificationName;
    
    /**
     * Property _NotificationType
     *
     * An array of Notification types for this MBean.
     */
    private transient String[] __m__NotificationType;
    
    /**
     * Property _ObjectName
     *
     * The ObjectName this bean has been registered with.
     */
    private transient javax.management.ObjectName __m__ObjectName;
    
    /**
     * Property MBeanInfo
     *
     * Lazy calculation.
     */
    private transient javax.management.MBeanInfo __m_MBeanInfo;
    
    /**
     * Property OP_GET
     *
     * Denotes getAttribute operation.
     */
    protected static final int OP_GET = 1;
    
    /**
     * Property OP_INVOKE
     *
     * Denotes invoke operation.
     */
    protected static final int OP_INVOKE = 2;
    
    /**
     * Property OP_SET
     *
     * Denotes setAttribute operation.
     */
    protected static final int OP_SET = 3;
    
    // Initializing constructor
    public Manageable(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/Manageable".replace('/', '.'));
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
    private java.util.Map get_PropertyInfo$Default()
        {
        java.util.Map mapInfo = new java.util.HashMap();
        
        // property MBeanInfo
            {
            mapInfo.put("MBeanInfo", new Object[]
                {
                "Lazy calculation.",
                "getMBeanInfo",
                null,
                "Ljavax/management/MBeanInfo;",
                null,
                });
            }
        
        return mapInfo;
        }
    /**
     * Auto-generated for concrete Components, for Properties that have:
    * 
    * - instance scope
    * - manual origin; not from super
    * - name _not_ starting with underscore
    * - at least one public accessor
     */
    protected java.util.Map get_PropertyInfo()
        {
        return new java.util.HashMap();
        }
    private java.util.Map get_MethodInfo$Default()
        {
        java.util.Map mapInfo = new java.util.HashMap();
        
        // behavior getAttribute(String sName)
            {
            mapInfo.put("getAttribute(Ljava.lang.String;)", new Object[]
                {
                "",
                "getAttribute",
                "Ljava/lang/Object;",
                new String[] {"sName", },
                new String[] {"Ljava.lang.String;", },
                null,
                });
            }
        
        // behavior getAttributes(String[] asName)
            {
            mapInfo.put("getAttributes([Ljava.lang.String;)", new Object[]
                {
                "",
                "getAttributes",
                "Ljavax/management/AttributeList;",
                new String[] {"asName", },
                new String[] {"[Ljava.lang.String;", },
                null,
                });
            }
        
        // behavior getNotificationInfo()
            {
            mapInfo.put("getNotificationInfo()", new Object[]
                {
                "",
                "getNotificationInfo",
                "[Ljavax/management/MBeanNotificationInfo;",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        // behavior invoke(String sName, Object[] aoParam, String[] asSignature)
            {
            mapInfo.put("invoke(Ljava.lang.String;[Ljava.lang.Object;[Ljava.lang.String;)", new Object[]
                {
                "",
                "invoke",
                "Ljava/lang/Object;",
                new String[] {"sName", "aoParam", "asSignature", },
                new String[] {"Ljava.lang.String;", "[Ljava.lang.Object;", "[Ljava.lang.String;", },
                null,
                });
            }
        
        // behavior postDeregister()
            {
            mapInfo.put("postDeregister()", new Object[]
                {
                "",
                "postDeregister",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        // behavior postRegister(Boolean registrationDone)
            {
            mapInfo.put("postRegister(Ljava.lang.Boolean;)", new Object[]
                {
                "",
                "postRegister",
                "V",
                new String[] {"registrationDone", },
                new String[] {"Ljava.lang.Boolean;", },
                null,
                });
            }
        
        // behavior preDeregister()
            {
            mapInfo.put("preDeregister()", new Object[]
                {
                "",
                "preDeregister",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        // behavior preRegister(javax.management.MBeanServer server, javax.management.ObjectName name)
            {
            mapInfo.put("preRegister(Ljavax.management.MBeanServer;Ljavax.management.ObjectName;)", new Object[]
                {
                "",
                "preRegister",
                "Ljavax/management/ObjectName;",
                new String[] {"server", "name", },
                new String[] {"Ljavax.management.MBeanServer;", "Ljavax.management.ObjectName;", },
                null,
                });
            }
        
        // behavior setAttribute(javax.management.Attribute attribute)
            {
            mapInfo.put("setAttribute(Ljavax.management.Attribute;)", new Object[]
                {
                "",
                "setAttribute",
                "V",
                new String[] {"attribute", },
                new String[] {"Ljavax.management.Attribute;", },
                null,
                });
            }
        
        // behavior setAttributes(javax.management.AttributeList attributes)
            {
            mapInfo.put("setAttributes(Ljavax.management.AttributeList;)", new Object[]
                {
                "",
                "setAttributes",
                "Ljavax/management/AttributeList;",
                new String[] {"attributes", },
                new String[] {"Ljavax.management.AttributeList;", },
                null,
                });
            }
        
        return mapInfo;
        }
    /**
     * Auto-generated for concrete Components, for Behaviors that have:
    * 
    * - instance scope
    * - manual origin; not from super
    * - name _not_ starting with underscore
    * - public access
     */
    protected java.util.Map get_MethodInfo()
        {
        // import com.tangosol.util.comparator.InverseComparator;
        // import com.tangosol.util.comparator.SafeComparator;
        
        // stupid HttpAdapter is showing the method in the reverse iterator order!
        // return new java.util.HashMap();
        
        return new java.util.TreeMap(
            new InverseComparator(new SafeComparator(null)));
        }
    
    /**
     * This method is a part of the NotificationEmitter interface, but has to be
    * declared here to avoid adding it as a JMX "operation".
     */
    protected void addNotificationListener(javax.management.NotificationListener listener, javax.management.NotificationFilter filter, Object handback)
        {
        }
    
    /**
     * Build a Descriptor object from a comma-delimited string of key-value
    * pairs.
     */
    protected javax.management.Descriptor buildDescriptor(String sDescriptor)
        {
        // import com.tangosol.net.management.annotation.MetricsScope;
        // import com.tangosol.util.Base;
        // import javax.management.ImmutableDescriptor;
        
        if (sDescriptor == null || sDescriptor.trim().length() == 0)
            {
            sDescriptor = MetricsScope.VENDOR;
            }    
        else
            {
            sDescriptor = sDescriptor + "," + MetricsScope.VENDOR;
            }
        
        return new ImmutableDescriptor(Base.parseDelimitedString(sDescriptor, ','));
        }
    
    protected String buildMethodSignature(String sName, String[] asSignature)
        {
        StringBuilder sb = new StringBuilder(sName);
        sb.append('(');
        
        int cParams = asSignature == null ? 0 : asSignature.length;
        for (int i = 0; i < cParams; i++)
            {
            sb.append(asSignature[i]);
            }
        sb.append(')');
        
        return sb.toString();
        }
    
    protected String findAttributeGetter(javax.management.MBeanInfo infoBean, Class clzBean, String sName)
            throws javax.management.AttributeNotFoundException
        {
        // import java.lang.reflect.Method;
        // import javax.management.AttributeNotFoundException;
        // import javax.management.MBeanAttributeInfo;
        
        Method method  = null;
        
        MBeanAttributeInfo[] aInfo = infoBean.getAttributes();
        for (int i = 0, c = aInfo.length; i < c; i++)
            {
            MBeanAttributeInfo info = aInfo[i];
            if (info.getName().equals(sName))
                {
                if (info.isReadable())
                    {
                    try
                        {
                        String sGetter = info.isIs() ? "is" + sName : "get" + sName;
                        method = clzBean.getMethod(sGetter, new Class[0]);
                        }
                    catch (NoSuchMethodException e) {}
                    }
                break;
                }
            }
        
        if (method == null)
            {
            throw new AttributeNotFoundException(
                "Attribute \"" + sName + "\" cannot be retrieved in " + clzBean);
            }
        
        return method.getName();
        }
    
    protected String findAttributeSetter(javax.management.MBeanInfo infoBean, Class clzBean, String sName)
            throws javax.management.AttributeNotFoundException
        {
        // import java.lang.reflect.Method;
        // import javax.management.AttributeNotFoundException;
        // import javax.management.MBeanAttributeInfo;
        
        Method method  = null;
        
        MBeanAttributeInfo[] aInfo = infoBean.getAttributes();
        for (int i = 0, c = aInfo.length; i < c; i++)
            {
            MBeanAttributeInfo info = aInfo[i];
            if (info.getName().equals(sName))
                {
                if (info.isWritable())
                    {
                    try
                        {
                        String sSetter  = "set" + sName;
                        method = clzBean.getMethod(sSetter,
                            new Class[]
                                {
                                loadClass(clzBean.getClassLoader(), info.getType())
                                });
                        }
                    catch (NoSuchMethodException e) {}
                    }
                break;
                }
            }
        
        if (method == null)
            {
            String sMsg =
                "Attribute \"" + sName + "\" cannot be set in " + clzBean;
            _trace(sMsg, 1);
            throw new AttributeNotFoundException(sMsg);
            }
        
        return method.getName();
        }
    
    /**
     * Validate the operation name. If signature array is not specified, obtain
    * the best match using the passed parameters.
    * 
    * @return the parameter signature array
     */
    protected String[] findOperation(javax.management.MBeanInfo infoBean, String sMethod, Object[] aoParam, String[] asSig)
        {
        // import javax.management.MBeanOperationInfo;
        // import javax.management.MBeanParameterInfo;
        
        int cParams = aoParam == null ? 0 : aoParam.length;
        
        if (asSig != null && asSig.length != cParams)
            {
            throw new IllegalArgumentException("Signature array length differs from"
                + " the parameter array for operation: " + sMethod);
            }
        
        if (asSig == null)
            {
            asSig = new String[cParams];
            }
        
        MBeanOperationInfo[] aInfo = infoBean.getOperations();
        
        Methods:
        for (int iM = 0, cM = aInfo.length; iM < cM; iM++)
            {
            MBeanOperationInfo info = aInfo[iM];
        
            if (info.getName().equals(sMethod))
                {
                MBeanParameterInfo[] aInfoParam = info.getSignature();
                int cP = aInfoParam == null ? 0 : aInfoParam.length;
        
                if (cP != cParams)
                    {
                    continue;
                    }
        
                for (int iP = 0; iP < cP; iP++)
                    {
                    MBeanParameterInfo infoParam  = aInfoParam[iP];
                    Object             oParam     = aoParam[iP];
                    String             sSigType   = asSig[iP];
                    String             sInfoType  = infoParam.getType();
        
                    Class clzInfo = loadClass(getClass().getClassLoader(), sInfoType);
                    if (sSigType != null)
                        {
                        // the signature was specifed;
                        Class clzSig  = loadClass(getClass().getClassLoader(), sSigType);
                        if (clzInfo.isAssignableFrom(clzSig))
                            {
                            continue;
                            }
                        else
                            {
                            continue Methods;
                            }
                        }
        
                    // the signature is missing - find a match
        
                    if (oParam == null)
                        {
                        // null matchess any
                        asSig[iP] = sInfoType;
                        continue;
                        }
        
                    Class clzParam = loadClass(getClass().getClassLoader(), oParam.getClass().getName());
                    if (clzInfo.isAssignableFrom(clzParam))
                        {
                        asSig[iP] = sInfoType;
                        }
                    else
                        {
                        // mismatch
                        continue Methods;
                        }
                    }
                
                // we have a match
                return asSig;
                }
            }
        
        throw new IllegalArgumentException("Operation \"" + buildMethodSignature(sMethod, asSig)
            + "\" cannot be invoked on " + infoBean.getClassName());
        }
    
    // Accessor for the property "_ComponentInfo"
    /**
     * Auto-generated for concrete Components.
     */
    protected Object[] get_ComponentInfo()
        {
        return new Object[] {"Manageable"};
        }
    
    // Accessor for the property "_MBeanServer"
    /**
     * Getter for property _MBeanServer.<p>
    * The MBeanServer this MBean is registered with.
     */
    public javax.management.MBeanServer get_MBeanServer()
        {
        return __m__MBeanServer;
        }
    
    // Accessor for the property "_NotificationDescription"
    /**
     * Getter for property _NotificationDescription.<p>
    * The description of the Notification that could be emitted by this MBean.
     */
    public String get_NotificationDescription()
        {
        return __m__NotificationDescription;
        }
    
    // Accessor for the property "_NotificationInfo"
    /**
     * Getter for property _NotificationInfo.<p>
    * [Calculated] NotificationInfo used by lazy MBeanInfo calculation. By
    * default it is a zero length array.
     */
    public javax.management.MBeanNotificationInfo[] get_NotificationInfo()
        {
        // import javax.management.MBeanNotificationInfo;
        
        String[] asType = get_NotificationType();
        
        return asType == null || asType.length == 0
            ? new MBeanNotificationInfo[0]
            : new MBeanNotificationInfo[]
                {
                new MBeanNotificationInfo(asType,
                    get_NotificationName(), get_NotificationDescription()),
                };
        }
    
    // Accessor for the property "_NotificationName"
    /**
     * Getter for property _NotificationName.<p>
    * Then Notification name (class). According to the JMX contracts, this
    * should be a fully qualified Java class name of the corresponding
    * notification. Currently, only a single Notification class per MBean is
    * supported.
     */
    public String get_NotificationName()
        {
        return __m__NotificationName;
        }
    
    // Accessor for the property "_NotificationType"
    /**
     * Getter for property _NotificationType.<p>
    * An array of Notification types for this MBean.
     */
    public String[] get_NotificationType()
        {
        return __m__NotificationType;
        }
    
    // Accessor for the property "_ObjectName"
    /**
     * Getter for property _ObjectName.<p>
    * The ObjectName this bean has been registered with.
     */
    public javax.management.ObjectName get_ObjectName()
        {
        return __m__ObjectName;
        }
    
    protected Object getAttribute(Object oTarget, String sName)
            throws javax.management.AttributeNotFoundException,
                   javax.management.MBeanException,
                   javax.management.ReflectionException
        {
        // import com.tangosol.util.ClassHelper;
        // import java.lang.reflect.Method;
        
        sName = findAttributeGetter(getMBeanInfo(), oTarget.getClass(), sName);
        return invoke(OP_GET, oTarget, sName, ClassHelper.VOID);
        }
    
    // From interface: javax.management.DynamicMBean
    public Object getAttribute(String sName)
            throws javax.management.AttributeNotFoundException,
                   javax.management.MBeanException,
                   javax.management.ReflectionException
        {
        return getAttribute(this, sName);
        }
    
    // From interface: javax.management.DynamicMBean
    public javax.management.AttributeList getAttributes(String[] asName)
        {
        // import javax.management.Attribute;
        // import javax.management.AttributeList;
        
        // it is quite hard to figure out the design intent for sure
        // (JMX Instrumentation and Agent Specification, v1.1 March 2002),
        // but judging by the lack of exceptions declared by this method
        // and the language of the spec on page 42 of the specification
        // we will ignore all the attributes that cannot be successfully
        // retrieved
        
        int cNames = asName == null ? 0 : asName.length;    
        AttributeList list = new AttributeList(cNames);
        for (int i = 0; i < cNames; i++)
            {
            String sName = asName[i];
            try
                {
                list.add(new Attribute(sName, getAttribute(sName)));
                }
            catch (Exception e)
                {
                }
            }
        return list;
        }
    
    // From interface: javax.management.DynamicMBean
    // Accessor for the property "MBeanInfo"
    /**
     * Getter for property MBeanInfo.<p>
    * Lazy calculation.
     */
    public javax.management.MBeanInfo getMBeanInfo()
        {
        // import com.tangosol.util.Base;
        // import java.util.Iterator;
        // import java.util.Map;
        // import javax.management.MBeanAttributeInfo;
        // import javax.management.MBeanConstructorInfo;
        // import javax.management.MBeanInfo;
        // import javax.management.MBeanNotificationInfo;
        // import javax.management.MBeanOperationInfo;
        // import javax.management.MBeanParameterInfo;
        
        MBeanInfo infoBean = __m_MBeanInfo;
        if (infoBean == null)
            {
            try
                {
                Object[] aoCompInfo  = get_ComponentInfo();
                Map      mapPropInfo = get_PropertyInfo();
                Map      mapMethInfo = get_MethodInfo();
        
                // MBean
                String sMBeanClass       = getClass().getName();
                String sMBeanDescription = (String) aoCompInfo[0];
                String sMBeanDescriptor  = (String) aoCompInfo[1];
        
                // attributes
                MBeanAttributeInfo[] aInfoAttribute =
                    new MBeanAttributeInfo[mapPropInfo.size()];
                    {
                    int iProp = 0;
                    for (Iterator iter = mapPropInfo.keySet().iterator(); iter.hasNext();)
                        {
                        String   sProp  = (String) iter.next();
                        Object[] aoInfo = (Object[]) mapPropInfo.get(sProp);
        
                        String sDescription = (String) aoInfo[0];
                        String sGetter      = (String) aoInfo[1];
                        String sSetter      = (String) aoInfo[2];
                        String sType        = (String) aoInfo[3];
                        String sDescriptor  = (String) aoInfo[4];                
        
                        aInfoAttribute[iProp++] = new MBeanAttributeInfo
                            (
                            sProp,
                            toJmxSignature(sType),
                            sDescription,
                            sGetter != null,
                            sSetter != null,
                            sGetter != null && sGetter.startsWith("is"),
                            buildDescriptor(sDescriptor)
                            );
                        }
                    }
        
                // constructors
                MBeanConstructorInfo[] aInfoConstructor;
                if (is_Constructable())
                    {
                    MBeanConstructorInfo info = new MBeanConstructorInfo(
                        "Default Constructor", getClass().getConstructors()[0]);
                    aInfoConstructor = new MBeanConstructorInfo[] {info};
                    }
                else
                    {
                    aInfoConstructor = new MBeanConstructorInfo[0];
                    }
        
                // operations
                MBeanOperationInfo[] aInfoOperation =
                    new MBeanOperationInfo[mapMethInfo.size()];
                    {
                    int iMethod = 0;
                    for (Iterator iter = mapMethInfo.keySet().iterator(); iter.hasNext();)
                        {
                        String   sSig   = (String) iter.next();
                        Object[] aoInfo = (Object[]) mapMethInfo.get(sSig);
        
                        String   sDescr      = (String)   aoInfo[0];
                        String   sMethod     = (String)   aoInfo[1];
                        String   sType       = (String)   aoInfo[2];
                        String[] asParamName = (String[]) aoInfo[3];
                        String[] asParamType = (String[]) aoInfo[4];
                        String   sDescriptor = (String)   aoInfo[5];
        
                        int cParams = asParamName == null ? 0 : asParamName.length;
                        MBeanParameterInfo[] aInfoParam = new MBeanParameterInfo[cParams];
                        for (int iParam = 0; iParam < cParams; iParam++)
                            {
                            aInfoParam[iParam] = new MBeanParameterInfo
                                (
                                asParamName[iParam],
                                toJmxSignature(asParamType[iParam]),
                                ""
                                );
                            }
        
                        aInfoOperation[iMethod++] = new MBeanOperationInfo
                            (
                            sMethod,
                            sDescr,
                            aInfoParam,
                            toJmxSignature(sType),
                            MBeanOperationInfo.UNKNOWN,
                            buildDescriptor(sDescriptor)
                            );
                        }
                    }
        
                infoBean = new MBeanInfo(sMBeanClass, sMBeanDescription,
                    aInfoAttribute, aInfoConstructor, aInfoOperation, getNotificationInfo(),
                    buildDescriptor(sMBeanDescriptor));
                setMBeanInfo(infoBean);
                }
            catch (Throwable e)
                {
                _trace("Internal error while generating MBeanInfo:", 1);
                _trace(e);
                }
            }
        return infoBean;
        }
    
    public javax.management.MBeanNotificationInfo[] getNotificationInfo()
        {
        // import javax.management.MBeanNotificationInfo;
        
        return new MBeanNotificationInfo[0];
        }
    
    protected Object invoke(int nOp, Object oTarget, String sMethod, Object[] aoParam)
            throws javax.management.MBeanException,
                   javax.management.ReflectionException
        {
        return null;
        }
    
    protected Object invoke(int nOp, Object oTarget, String sMethod, Object[] aoParam, String[] asSignature)
            throws javax.management.MBeanException,
                   javax.management.ReflectionException
        {
        return null;
        }
    
    protected Object invoke(Object oTarget, String sName, Object[] aoParam, String[] asSignature)
            throws javax.management.MBeanException,
                   javax.management.ReflectionException
        {
        // import java.lang.reflect.Method;
        // import javax.management.RuntimeOperationsException;
        
        Method method;
        try
            {
            asSignature = findOperation(getMBeanInfo(), sName, aoParam, asSignature);
            }
        catch (RuntimeException e)
            {
            _trace(e);
            throw new RuntimeOperationsException(e);
            }
        
        return invoke(OP_INVOKE, oTarget, sName, aoParam, asSignature);
        }
    
    // From interface: javax.management.DynamicMBean
    public Object invoke(String sName, Object[] aoParam, String[] asSignature)
            throws javax.management.MBeanException,
                   javax.management.ReflectionException
        {
        return invoke(this, sName, aoParam, asSignature);
        }
    
    // Accessor for the property "_Constructable"
    /**
     * Getter for property _Constructable.<p>
    * Specifies whether the MBean is constructable. Used by auto-generated code.
     */
    protected boolean is_Constructable()
        {
        return __m__Constructable;
        }
    
    /**
     * Load a Class object for the data type used in the MBeanInfo or passed to
    * the "invoke" method.
     */
    protected static Class loadClass(ClassLoader loader, String sJmxSig)
        {
        // import com.tangosol.net.management.MBeanHelper;
        
        _assert(sJmxSig.length() > 0, "Empty JMX signature");
        
        if (sJmxSig.length() == 1)
            {
            switch (sJmxSig.charAt(0))
                {
                case 'Z':
                    return Boolean.TYPE;
                case 'C':
                    return Character.TYPE;
                case 'B':
                    return Byte.TYPE;
                case 'S':
                    return Short.TYPE;
                case 'I':
                    return Integer.TYPE;
                case 'J':
                    return Long.TYPE;
                case 'F':
                    return Float.TYPE;
                case 'D':
                    return Double.TYPE;
                }
            }
        else
            {
            // we don't expose MBeanHelper.classForName() for security reasons
        
            Class clz = (Class) MBeanHelper.SCALAR_TYPES.get(sJmxSig);
            if (clz != null)
                {
                return clz;
                }
        
            try
                {
                return Class.forName(sJmxSig, true, loader);
                }
            catch (Exception e) {}
            }
        
        throw new IllegalArgumentException(
            "Class is not found or invalid JVM signature: " + sJmxSig);
        }
    
    // From interface: javax.management.MBeanRegistration
    public void postDeregister()
        {
        set_MBeanServer(null);
        set_ObjectName(null);
        }
    
    // From interface: javax.management.MBeanRegistration
    public void postRegister(Boolean registrationDone)
        {
        }
    
    // From interface: javax.management.MBeanRegistration
    public void preDeregister()
            throws java.lang.Exception
        {
        }
    
    // From interface: javax.management.MBeanRegistration
    public javax.management.ObjectName preRegister(javax.management.MBeanServer server, javax.management.ObjectName name)
            throws java.lang.Exception
        {
        set_MBeanServer(server);
        set_ObjectName(name);
        
        return name;
        }
    
    /**
     * This method is a part of the NotificationEmitter interface, but has to be
    * declared here to avoid adding it as a JMX "operation".
     */
    protected void removeNotificationListener(javax.management.NotificationListener listener)
            throws javax.management.ListenerNotFoundException
        {
        }
    
    /**
     * This method is a part of the NotificationEmitter interface, but has to be
    * declared here to avoid adding it as a JMX "operation".
     */
    protected void removeNotificationListener(javax.management.NotificationListener listener, javax.management.NotificationFilter filter, Object handback)
            throws javax.management.ListenerNotFoundException
        {
        }
    
    // Accessor for the property "_Constructable"
    /**
     * Setter for property _Constructable.<p>
    * Specifies whether the MBean is constructable. Used by auto-generated code.
     */
    protected void set_Constructable(boolean flag)
        {
        __m__Constructable = flag;
        }
    
    // Accessor for the property "_MBeanServer"
    /**
     * Setter for property _MBeanServer.<p>
    * The MBeanServer this MBean is registered with.
     */
    protected void set_MBeanServer(javax.management.MBeanServer server)
        {
        __m__MBeanServer = server;
        }
    
    // Accessor for the property "_NotificationDescription"
    /**
     * Setter for property _NotificationDescription.<p>
    * The description of the Notification that could be emitted by this MBean.
     */
    protected void set_NotificationDescription(String sDescr)
        {
        __m__NotificationDescription = sDescr;
        }
    
    // Accessor for the property "_NotificationName"
    /**
     * Setter for property _NotificationName.<p>
    * Then Notification name (class). According to the JMX contracts, this
    * should be a fully qualified Java class name of the corresponding
    * notification. Currently, only a single Notification class per MBean is
    * supported.
     */
    protected void set_NotificationName(String sName)
        {
        __m__NotificationName = sName;
        }
    
    // Accessor for the property "_NotificationType"
    /**
     * Setter for property _NotificationType.<p>
    * An array of Notification types for this MBean.
     */
    protected void set_NotificationType(String[] asType)
        {
        __m__NotificationType = asType;
        }
    
    // Accessor for the property "_ObjectName"
    /**
     * Setter for property _ObjectName.<p>
    * The ObjectName this bean has been registered with.
     */
    protected void set_ObjectName(javax.management.ObjectName name)
        {
        __m__ObjectName = name;
        }
    
    protected void setAttribute(Object oTarget, String sName, Object oValue)
            throws javax.management.AttributeNotFoundException,
                   javax.management.InvalidAttributeValueException,
                   javax.management.MBeanException,
                   javax.management.ReflectionException
        {
        // import java.lang.reflect.Method;
        
        sName = findAttributeSetter(getMBeanInfo(), oTarget.getClass(), sName);
        
        invoke(OP_SET, oTarget, sName, new Object[]{oValue});
        }
    
    // From interface: javax.management.DynamicMBean
    public void setAttribute(javax.management.Attribute attribute)
            throws javax.management.AttributeNotFoundException,
                   javax.management.InvalidAttributeValueException,
                   javax.management.MBeanException,
                   javax.management.ReflectionException
        {
        // import javax.management.InvalidAttributeValueException;
        
        if (attribute == null)
            {
            throw new InvalidAttributeValueException("Attribute cannot be null");
            }
        setAttribute(this, attribute.getName(), attribute.getValue());
        }
    
    // From interface: javax.management.DynamicMBean
    public javax.management.AttributeList setAttributes(javax.management.AttributeList attributes)
        {
        // import javax.management.Attribute;
        // import javax.management.AttributeList;
        // import javax.management.MBeanException;
        
        // it is quite hard to figure out the design intent for sure
        // (JMX Instrumentation and Agent Specification, v1.1 March 2002),
        // but judging by the lack of exceptions declared by this method
        // and the language of the spec on page 42 of the specification
        // we will ignore all the attributes that cannot be successfully set
        //
        // Update: 2022-09-22 - BUG 33334755
        // If the wrapped exception is a SecurityException, we must throw this exception otherwise
        // when using read-only management, these exceptions are ignored
        
        int           c    = attributes == null ? 0 : attributes.size();    
        AttributeList list = new AttributeList(c);
        for (int i = 0; i < c; i++)
            {
            Attribute attribute = (Attribute) attributes.get(i);
            String    sName     = attribute.getName();
            try
                {
                setAttribute(attribute);
                list.add(new Attribute(sName, getAttribute(sName)));
                }
            catch (MBeanException e)
                {
                Throwable cause = e.getTargetException();
                if (cause instanceof SecurityException)
                    {
                    throw (SecurityException) cause;
                    }
                }
            catch (Exception e)
                {
                }
            }
        return list;
        }
    
    // Accessor for the property "MBeanInfo"
    /**
     * Setter for property MBeanInfo.<p>
    * Lazy calculation.
     */
    protected void setMBeanInfo(javax.management.MBeanInfo infoBean)
        {
        __m_MBeanInfo = infoBean;
        }
    
    /**
     * Translate the JVM signature into the data type as used in the MBeanInfo
    * or passed to the "invoke" method.
     */
    protected static String toJmxSignature(String sJvmSig)
        {
        _assert(sJvmSig.length() > 0, "Empty JVM signature");
        
        // see Instrumentation and Agent Specification, v1.1
        // chapter "Basic Data Types" on page 60
        
        if (sJvmSig.length() == 1)
            {
            switch (sJvmSig.charAt(0))
                {
                case 'Z': // boolean
                    return "java.lang.Boolean";
                case 'C': // char
                    return "java.lang.Character";
                case 'B': // byte
                    return "java.lang.Byte";
                case 'S': // short
                    return "java.lang.Short";
                case 'I': // int
                    return "java.lang.Integer";
                case 'J': // long
                    return "java.lang.Long";
                case 'F': // float
                    return "java.lang.Float";
                case 'D': // double
                    return "java.lang.Double";
                case 'V':
                    return "java.lang.Void";
                }
            }
        else
            {
            switch (sJvmSig.charAt(0))
                {
                case '[':
                    // it's very inconsistent, but array signatures are the same as JVM's;
                    // the following implementation:
                    //      return toJmxSignature(sJvmSig.substring(1)) + "[]";
                    // worked fine with JMX 1.0 and 1.1, but breaks JMX 1.2
                    return sJvmSig.replace('/', '.');
                case 'L':
                    if (sJvmSig.endsWith(";"))
                        {
                        // already converted
                        return sJvmSig.substring(1, sJvmSig.length() - 1).replace('/', '.');
                        }
                    break;
                 }
            }
        
        throw new IllegalArgumentException("Invalid JVM signature: " + sJvmSig);
        }
    
    protected javax.management.MBeanException translateInvocationException(java.lang.reflect.InvocationTargetException ite)
            throws javax.management.RuntimeErrorException,
                   javax.management.RuntimeMBeanException
        {
        // import javax.management.MBeanException;
        // import javax.management.RuntimeErrorException;
        // import javax.management.RuntimeMBeanException;
        
        Throwable e = ite.getTargetException();
        
        if (e instanceof RuntimeException)
            {
            throw new RuntimeMBeanException((RuntimeException) e);
            }
        if (e instanceof Error)
            {
            throw new RuntimeErrorException((Error) e);
            }
        return new MBeanException((Exception) e);
        }
    }
