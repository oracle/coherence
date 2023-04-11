
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.Security

package com.tangosol.coherence.component.net;

import com.tangosol.coherence.component.application.console.Coherence;
import com.tangosol.coherence.component.net.security.Standard;
import com.tangosol.internal.net.security.DefaultSecurityDependencies;
import com.tangosol.internal.net.security.DefaultStandardDependencies;
import com.tangosol.internal.net.security.LegacyXmlStandardHelper;
import com.tangosol.net.ClusterPermission;
import com.tangosol.net.security.Authorizer;
import com.tangosol.net.security.DefaultIdentityAsserter;
import com.tangosol.net.security.DefaultIdentityTransformer;
import com.tangosol.net.security.DoAsAction;
import com.tangosol.net.security.IdentityAsserter;
import com.tangosol.net.security.IdentityTransformer;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.security.auth.Subject;

/**
 * The base component for the Coherence Security framework implementation.
 * 
 * The basic pattern of usage is:
 * 
 *     Security security = Security.getInstance();
 *     if (security != null)
 *         {
 *         security.checkPermission(cluster, 
 *             new ClusterPermission(sTarget, sAction));
 *         }
 * 
 * alternatively there is a helper method:
 * 
 *     Security.checkPermission(cluster, sService, sCache, sAction);
 * 
 * that incapsulates the above logic where basically:
 *     sTarget = sService +'/' + sCache;
 * 
 * The oddities in the design of this Component tree are historical; prior to
 * Coherence 3.6, we had the following requirment:
 * 
 * "The Security component itself MUST NOT be J2SE 1.4 dependent."
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Security
        extends    com.tangosol.coherence.component.Net
        implements java.security.PrivilegedAction
    {
    // ---- Fields declarations ----
    
    /**
     * Property Authorizer
     *
     * Authorizer represents an environment-specific facility for authorizing
     * callers to perform actions described by the corresponding permission
     * objects.
     */
    private static transient com.tangosol.net.security.Authorizer __s_Authorizer;
    
    /**
     * Property Configured
     *
     * Is security already configured?
     * 
     * @volatile - else if getInstance were called concurrently when not yet
     * configured, one thread could see configured as true but not see the
     * corresponding Security instance.  This can then result in the
     * PermissionInfo not getting inserted into the ServiceContext. Bug 27376204
     */
    private static volatile transient boolean __s_Configured;
    
    /**
     * Property IdentityAsserter
     *
     * IdentityAsserter validates a token in order to establish a user's
     * identity.
     */
    private static transient com.tangosol.net.security.IdentityAsserter __s_IdentityAsserter;
    
    /**
     * Property IdentityTransformer
     *
     * IdentityTransformer transforms a Subject to a token that asserts
     * identity.
     */
    private static transient com.tangosol.net.security.IdentityTransformer __s_IdentityTransformer;
    
    /**
     * Property Instance
     *
     * The Security instance.
     */
    private static transient Security __s_Instance;
    
    /**
     * Property SubjectScoped
     *
     * Indicates if the security configuration specifies subject scoping.
     */
    private static transient boolean __s_SubjectScoped;
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
        __mapChildren.put("CheckPermissionAction", Security.CheckPermissionAction.get_CLASS());
        __mapChildren.put("ConfigAction", Security.ConfigAction.get_CLASS());
        __mapChildren.put("RefAction", Security.RefAction.get_CLASS());
        }
    
    // Initializing constructor
    public Security(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/Security".replace('/', '.'));
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
    
    /**
     * Security API exposed to the all Service components. Called on a client
    * thread.
     */
    public void checkPermission(com.tangosol.net.Cluster cluster, com.tangosol.net.ClusterPermission permission, javax.security.auth.Subject subject)
        {
        }
    
    /**
     * Helper method around "Security API".
     */
    public static void checkPermission(com.tangosol.net.Cluster cluster, String sServiceName, String sCacheName, String sAction)
        {
        // import com.tangosol.net.ClusterPermission;
        // import com.tangosol.net.security.Authorizer;
        // import com.tangosol.net.security.DoAsAction;
        // import java.security.AccessController;
        // import javax.security.auth.Subject;
        
        Authorizer authorizer = getAuthorizer();
        Security   security   = Security.getInstance();
        
        if (authorizer == null && security == null)
            {
            return;
            }
        
        _assert(sServiceName != null, "Service must be specified");
        
        String            sTarget    = "service=" + sServiceName +
                                       (sCacheName == null ? "" : ",cache=" + sCacheName);
        ClusterPermission permission = new ClusterPermission(cluster == null || !cluster.isRunning() ? null :
                                       cluster.getClusterName(), sTarget, sAction);
        Subject           subject    = null;
        
        if (authorizer != null)
            {
            subject = authorizer.authorize(subject, permission);
            }
        
        if (security != null)
            {
            Security.CheckPermissionAction action = new Security.CheckPermissionAction();
            action.setCluster(cluster);
            action.setPermission(permission);
            action.setSubject(subject);
            action.setSecurity(security);
        
            AccessController.doPrivileged(new DoAsAction(action));
            }
        }
    
    /**
     * Create a new Default dependencies object by cloning the input
    * dependencies.  Each class or component that uses dependencies implements
    * a Default dependencies class which provides the clone functionality.  
    * The dependency injection design pattern requires every component in the
    * component hierarchy to implement clone.
    * 
    * @return DefaultSecurityDependencies  the cloned dependencies
     */
    public static com.tangosol.internal.net.security.DefaultSecurityDependencies cloneDeps(com.tangosol.internal.net.security.SecurityDependencies deps)
        {
        // import com.tangosol.internal.net.security.DefaultSecurityDependencies;
        
        return new DefaultSecurityDependencies(deps);
        }
    
    /**
     * Declared as public only to be accessed by the action.
     */
    public static synchronized void configureSecurity()
        {
        // import Component.Application.Console.Coherence;
        // import Component.Net.Security.Standard;
        // import com.tangosol.internal.net.security.DefaultStandardDependencies;
        // import com.tangosol.internal.net.security.LegacyXmlStandardHelper;
        // import com.tangosol.run.xml.XmlElement;
        
        if (isConfigured())
            {
            return;
            }
        
        DefaultStandardDependencies deps = null;
        Security security                = null;
        
        try
            {
            // create security dependencies including default values
            deps = new DefaultStandardDependencies();
        
            // internal call equivalent to "CacheFactory.getSecurityConfig();"
            XmlElement xmlConfig = Coherence.getServiceConfig("$Security");
            if (xmlConfig != null)
                {
                // load the security dependencies given the xml config 
                deps = LegacyXmlStandardHelper.fromXml(xmlConfig, deps);
        
                if (deps.isEnabled())
                    {
                    // "model" element is not documented for now
                    security = (Standard) _newInstance("Component.Net.Security." + deps.getModel());   
                    }
                }
            }
        finally
            {
            // if Security is not instantiated, we still neeed to process
            // the dependencies to pickup the IdentityAsserter and IdentityTransformer
            // objects for the Security component (see onDeps()).
            if (security == null)
                {
                processDependencies(deps.validate());
                }
            else
                {
                // load the standard dependencies (currently only support Standard)
                if (deps.getModel().equals("Standard"))
                    {
                    ((Standard) security).setDependencies(deps);
                    }
                setInstance(security);
                }
        
            setConfigured(true);
            }
        }
    
    /**
     * Helper method.
     */
    public static java.security.PrivilegedAction createPrivilegedAction(java.lang.reflect.Method method)
        {
        Security.RefAction action = new Security.RefAction();
        
        action.setMethod(method);
        
        return action;
        }
    
    /**
     * Helper method.
     */
    public static java.security.PrivilegedAction createPrivilegedAction(java.lang.reflect.Method method, Object oTarget, Object[] aoArg)
        {
        Security.RefAction action = new Security.RefAction();
        
        action.setMethod(method);
        action.setTarget(oTarget);
        action.setArguments(aoArg);
        
        return action;
        }
    
    // Accessor for the property "Authorizer"
    /**
     * Getter for property Authorizer.<p>
    * Authorizer represents an environment-specific facility for authorizing
    * callers to perform actions described by the corresponding permission
    * objects.
     */
    public static com.tangosol.net.security.Authorizer getAuthorizer()
        {
        return __s_Authorizer;
        }
    
    // Accessor for the property "IdentityAsserter"
    /**
     * Getter for property IdentityAsserter.<p>
    * IdentityAsserter validates a token in order to establish a user's
    * identity.
     */
    public static com.tangosol.net.security.IdentityAsserter getIdentityAsserter()
        {
        return __s_IdentityAsserter;
        }
    
    // Accessor for the property "IdentityTransformer"
    /**
     * Getter for property IdentityTransformer.<p>
    * IdentityTransformer transforms a Subject to a token that asserts identity.
     */
    public static com.tangosol.net.security.IdentityTransformer getIdentityTransformer()
        {
        return __s_IdentityTransformer;
        }
    
    // Accessor for the property "Instance"
    /**
     * Getter for property Instance.<p>
    * The Security instance.
     */
    public static Security getInstance()
        {
        // import java.security.AccessController;
        
        if (!isConfigured())
            {
            AccessController.doPrivileged(new Security.ConfigAction());
            }
        return __s_Instance;
        }
    
    /**
     * Security debugging helper. Not used for anything else!
     */
    public javax.security.auth.Subject impersonate(javax.security.auth.Subject subject, String sNameOld, String sNameNew)
        {
        return null;
        }
    
    // Accessor for the property "Configured"
    /**
     * Getter for property Configured.<p>
    * Is security already configured?
    * 
    * @volatile - else if getInstance were called concurrently when not yet
    * configured, one thread could see configured as true but not see the
    * corresponding Security instance.  This can then result in the
    * PermissionInfo not getting inserted into the ServiceContext. Bug 27376204
     */
    protected static boolean isConfigured()
        {
        return __s_Configured;
        }
    
    // Accessor for the property "SecurityEnabled"
    /**
     * Getter for property SecurityEnabled.<p>
    * Indicates if security is enabled by the configuration.
     */
    public static boolean isSecurityEnabled()
        {
        return Security.getInstance() != null || getAuthorizer() != null;
        }
    
    // Accessor for the property "SubjectScoped"
    /**
     * Getter for property SubjectScoped.<p>
    * Indicates if the security configuration specifies subject scoping.
     */
    public static boolean isSubjectScoped()
        {
        return __s_SubjectScoped;
        }
    
    /**
     * @param oHandler  a CallbackHandler object
    * 
    * @see com.tangosol.net.security.Security
     */
    public static javax.security.auth.Subject login(javax.security.auth.callback.CallbackHandler handler)
        {
        Security security = getInstance();
        return security == null ? null : security.loginSecure(handler, null);
        }
    
    /**
     * Subclassing support.
     */
    protected javax.security.auth.Subject loginSecure(javax.security.auth.callback.CallbackHandler handler, javax.security.auth.Subject subject)
        {
        return null;
        }
    
    /**
     * Process the Dependencies for the component.
     */
    protected static void processDependencies(com.tangosol.internal.net.security.SecurityDependencies deps)
        {
        // import com.tangosol.net.security.DefaultIdentityAsserter;
        // import com.tangosol.net.security.DefaultIdentityTransformer;
        // import com.tangosol.net.security.IdentityAsserter;
        // import com.tangosol.net.security.IdentityTransformer;
        
        // if asserter and transformer are not configured then use defaults
        IdentityAsserter asserter = deps.getIdentityAsserter();
        setIdentityAsserter(asserter == null ? DefaultIdentityAsserter.INSTANCE : asserter);
        
        IdentityTransformer transformer = deps.getIdentityTransformer();
        setIdentityTransformer(transformer == null ? DefaultIdentityTransformer.INSTANCE : transformer);
        
        setAuthorizer(deps.getAuthorizer());
        setSubjectScoped(deps.isSubjectScoped());
        }
    
    /**
     * Callback API used to validate and respond to a security related request.
    * Called on a corresponding service thread of the service senior node.
    * 
    * @param memberThis the member validating the secure request
    * @param memberFrom the member requesting validation
    * @param oRequestInfo the information to validate
     */
    public Object processSecureRequest(Member memberThis, Member memberFrom, com.tangosol.net.security.PermissionInfo piRequest)
        {
        return null;
        }
    
    /**
     * Security API used by the Service components. Called on a service thread
    * upon the service termination.
    * 
    * @param sServiceName  the relevant Service name
     */
    public void releaseSecureContext(String sServiceName)
        {
        }
    
    // From interface: java.security.PrivilegedAction
    public Object run()
        {
        return null;
        }
    
    /**
     * Helper method.
     */
    protected static Object runAnonymously(Object oAction)
            throws java.security.PrivilegedActionException
        {
        // import java.security.PrivilegedAction;
        // import java.security.PrivilegedActionException;
        // import java.security.PrivilegedExceptionAction;
        
        if (oAction instanceof PrivilegedAction)
            {
            return ((PrivilegedAction) oAction).run();
            }
        else
            {
            try
                {
                return ((PrivilegedExceptionAction) oAction).run();
                }
            catch (Exception e)
                {
                throw new PrivilegedActionException(e);
                }
            }
        }
    
    /**
     * @param oSubject  a Subject object (optional)
    * @param oAction  a PrivilegedAction or PrivilegedExceptionAction object
    * 
    * @see com.tangosol.net.security.Security
     */
    public static Object runAs(javax.security.auth.Subject subject, Object oAction)
            throws java.security.PrivilegedActionException
        {
        Security security = getInstance();
        return security == null ?
            runAnonymously(oAction) : security.runSecure(subject, oAction);
        }
    
    /**
     * Subclassing support.
     */
    protected Object runSecure(javax.security.auth.Subject subject, Object oAction)
            throws java.security.PrivilegedActionException
        {
        return null;
        }
    
    // Accessor for the property "Authorizer"
    /**
     * Setter for property Authorizer.<p>
    * Authorizer represents an environment-specific facility for authorizing
    * callers to perform actions described by the corresponding permission
    * objects.
     */
    protected static void setAuthorizer(com.tangosol.net.security.Authorizer authorizer)
        {
        __s_Authorizer = authorizer;
        }
    
    // Accessor for the property "Configured"
    /**
     * Setter for property Configured.<p>
    * Is security already configured?
    * 
    * @volatile - else if getInstance were called concurrently when not yet
    * configured, one thread could see configured as true but not see the
    * corresponding Security instance.  This can then result in the
    * PermissionInfo not getting inserted into the ServiceContext. Bug 27376204
     */
    protected static void setConfigured(boolean fConfig)
        {
        __s_Configured = fConfig;
        }
    
    // Accessor for the property "IdentityAsserter"
    /**
     * Setter for property IdentityAsserter.<p>
    * IdentityAsserter validates a token in order to establish a user's
    * identity.
     */
    protected static void setIdentityAsserter(com.tangosol.net.security.IdentityAsserter asserter)
        {
        __s_IdentityAsserter = asserter;
        }
    
    // Accessor for the property "IdentityTransformer"
    /**
     * Setter for property IdentityTransformer.<p>
    * IdentityTransformer transforms a Subject to a token that asserts identity.
     */
    protected static void setIdentityTransformer(com.tangosol.net.security.IdentityTransformer transformer)
        {
        __s_IdentityTransformer = transformer;
        }
    
    // Accessor for the property "Instance"
    /**
     * Setter for property Instance.<p>
    * The Security instance.
     */
    protected static void setInstance(Security security)
        {
        __s_Instance = security;
        }
    
    // Accessor for the property "SubjectScoped"
    /**
     * Setter for property SubjectScoped.<p>
    * Indicates if the security configuration specifies subject scoping.
     */
    protected static void setSubjectScoped(boolean fSubjectScoped)
        {
        __s_SubjectScoped = fSubjectScoped;
        }
    
    /**
     * Callback API used to verify that the joining service member has passed
    * the authentication step. Called on a corresponding service thread on the
    * joining node.
    * 
    * @param service  the Service
    * @param info  the security request info
     */
    public void verifySecureResponse(com.tangosol.net.Service service, com.tangosol.net.security.PermissionInfo info)
        {
        }

    // ---- class: com.tangosol.coherence.component.net.Security$CheckPermissionAction
    
    /**
     * Privileged action to check permission.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class CheckPermissionAction
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        /**
         * Property Cluster
         *
         */
        private com.tangosol.net.Cluster __m_Cluster;
        
        /**
         * Property Permission
         *
         */
        private com.tangosol.net.ClusterPermission __m_Permission;
        
        /**
         * Property Security
         *
         */
        private Security __m_Security;
        
        /**
         * Property Subject
         *
         */
        private javax.security.auth.Subject __m_Subject;
        
        // Default constructor
        public CheckPermissionAction()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public CheckPermissionAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.Security.CheckPermissionAction();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/net/Security$CheckPermissionAction".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Accessor for the property "Cluster"
        /**
         * Getter for property Cluster.<p>
         */
        public com.tangosol.net.Cluster getCluster()
            {
            return __m_Cluster;
            }
        
        // Accessor for the property "Permission"
        /**
         * Getter for property Permission.<p>
         */
        public com.tangosol.net.ClusterPermission getPermission()
            {
            return __m_Permission;
            }
        
        // Accessor for the property "Security"
        /**
         * Getter for property Security.<p>
         */
        public Security getSecurity()
            {
            return __m_Security;
            }
        
        // Accessor for the property "Subject"
        /**
         * Getter for property Subject.<p>
         */
        public javax.security.auth.Subject getSubject()
            {
            return __m_Subject;
            }
        
        // From interface: java.security.PrivilegedAction
        public Object run()
            {
            getSecurity().checkPermission(getCluster(), getPermission(), getSubject());
            return null;
            }
        
        // Accessor for the property "Cluster"
        /**
         * Setter for property Cluster.<p>
         */
        public void setCluster(com.tangosol.net.Cluster cluster)
            {
            __m_Cluster = cluster;
            }
        
        // Accessor for the property "Permission"
        /**
         * Setter for property Permission.<p>
         */
        public void setPermission(com.tangosol.net.ClusterPermission permission)
            {
            __m_Permission = permission;
            }
        
        // Accessor for the property "Security"
        /**
         * Setter for property Security.<p>
         */
        public void setSecurity(Security security)
            {
            __m_Security = security;
            }
        
        // Accessor for the property "Subject"
        /**
         * Setter for property Subject.<p>
         */
        public void setSubject(javax.security.auth.Subject subject)
            {
            __m_Subject = subject;
            }
        }

    // ---- class: com.tangosol.coherence.component.net.Security$ConfigAction
    
    /**
     * Privileged action to configure security.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ConfigAction
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public ConfigAction()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ConfigAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.Security.ConfigAction();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/net/Security$ConfigAction".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // From interface: java.security.PrivilegedAction
        public Object run()
            {
            try
                {
                ((Security) get_Module()).configureSecurity();
                }
            catch (RuntimeException e)
                {
                _trace("Failed to configure the Security module", 1);
                _trace(e);
                }
            
            return null;
            }
        }

    // ---- class: com.tangosol.coherence.component.net.Security$RefAction
    
    /**
     * Reflection based PrivilegedAction
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class RefAction
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        /**
         * Property Arguments
         *
         */
        private transient Object[] __m_Arguments;
        
        /**
         * Property Method
         *
         */
        private transient java.lang.reflect.Method __m_Method;
        
        /**
         * Property Target
         *
         */
        private transient Object __m_Target;
        
        // Default constructor
        public RefAction()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public RefAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.Security.RefAction();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/net/Security$RefAction".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Accessor for the property "Arguments"
        /**
         * Getter for property Arguments.<p>
         */
        public Object[] getArguments()
            {
            return __m_Arguments;
            }
        
        // Accessor for the property "Method"
        /**
         * Getter for property Method.<p>
         */
        public java.lang.reflect.Method getMethod()
            {
            return __m_Method;
            }
        
        // Accessor for the property "Target"
        /**
         * Getter for property Target.<p>
         */
        public Object getTarget()
            {
            return __m_Target;
            }
        
        // From interface: java.security.PrivilegedAction
        public Object run()
            {
            // import com.tangosol.util.Base;
            
            try
                {
                return getMethod().invoke(getTarget(), getArguments());
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e, toString());
                }
            }
        
        // Accessor for the property "Arguments"
        /**
         * Setter for property Arguments.<p>
         */
        public void setArguments(Object[] method)
            {
            __m_Arguments = method;
            }
        
        // Accessor for the property "Method"
        /**
         * Setter for property Method.<p>
         */
        public void setMethod(java.lang.reflect.Method method)
            {
            __m_Method = method;
            }
        
        // Accessor for the property "Target"
        /**
         * Setter for property Target.<p>
         */
        public void setTarget(Object method)
            {
            __m_Target = method;
            }
        
        // Declared at the super level
        public String toString()
            {
            // import Component.Application.Console.Coherence;
            
            return "RefAction{Method=" + getMethod().getName() + ", Target=" + getTarget() +
                   ", Args=" + Coherence.toString(getArguments()) + '}';
            }
        }
    }
