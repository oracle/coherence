
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.security.Standard

package com.tangosol.coherence.component.net.security;

import com.tangosol.coherence.component.net.Cluster;
import com.tangosol.coherence.component.util.SafeCluster;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService;
import com.tangosol.internal.net.security.DefaultStandardDependencies;
import com.tangosol.internal.net.security.StandardDependencies;
import com.tangosol.net.ClusterPermission;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.security.PermissionInfo;
import com.tangosol.net.security.SecurityHelper;
import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;

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
 * 
 * The Security.Standard component is dependent on JAAS framework, which is
 * currently a part of J2SE 1.4
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Standard
        extends    com.tangosol.coherence.component.net.Security
    {
    // ---- Fields declarations ----
    
    /**
     * Property Dependencies
     *
     * The external dependencies needed by this component. The dependencies
     * object must be full populated and validated before this property is set.
     *  See setDependencies.  
     * 
     * The mechanism for creating and populating dependencies is hidden from
     * this component. Typically, the dependencies object is populated using
     * data from some external configuration, such as XML, but this may not
     * always be the case
     */
    private com.tangosol.internal.net.security.StandardDependencies __m_Dependencies;
    
    /**
     * Property ServiceContext
     *
     * (Private) A map of Service related Subject objects keyed by the service
     * name. Used by the client threads.
     */
    private transient java.util.Map __m_ServiceContext;
    
    /**
     * Property ThreadContext
     *
     * (Private) ThreadLocal holding the Subject. Used by the client threads.
     */
    private transient ThreadLocal __m_ThreadContext;
    
    /**
     * Property ValidSubjects
     *
     * Set of Subjects that have been validated. To avoid repetetive
     * validations of the same subject (which could be very expensive), we use
     * the LocalCache with 10 second expiration. We don't want to keep then
     * forever to allow policy changes take effect relatively quickly.
     */
    private java.util.Map __m_ValidSubjects;
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
        __mapChildren.put("CheckPermissionAction", com.tangosol.coherence.component.net.Security.CheckPermissionAction.get_CLASS());
        __mapChildren.put("ConfigAction", com.tangosol.coherence.component.net.Security.ConfigAction.get_CLASS());
        __mapChildren.put("CreateLoginCtxAction", Standard.CreateLoginCtxAction.get_CLASS());
        __mapChildren.put("RefAction", com.tangosol.coherence.component.net.Security.RefAction.get_CLASS());
        }
    
    // Default constructor
    public Standard()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Standard(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        
        
        // containment initialization: children
        
        // signal the end of the initialization
        set_Constructed(true);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        
        // state initialization: private properties
        try
            {
            __m_ServiceContext = new com.tangosol.util.SafeHashMap();
            __m_ThreadContext = new java.lang.ThreadLocal();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.net.security.Standard();
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
            clz = Class.forName("com.tangosol.coherence/component/net/security/Standard".replace('/', '.'));
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
    
    // Declared at the super level
    /**
     * Security API exposed to the all Service components. Called on a client
    * thread.
     */
    public void checkPermission(com.tangosol.net.Cluster cluster, com.tangosol.net.ClusterPermission permission, javax.security.auth.Subject subject)
        {
        // import Component.Net.Cluster;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.ClusterService;
        // import Component.Util.SafeCluster;
        // import com.tangosol.net.ClusterPermission;
        // import com.tangosol.net.security.AccessController as com.tangosol.net.security.AccessController;
        // import com.tangosol.net.security.PermissionInfo;
        // import com.tangosol.net.security.SecurityHelper;
        // import com.tangosol.util.Base;
        // import java.security.AccessControlException;
        // import java.security.GeneralSecurityException;
        // import javax.security.auth.Subject;
        
        String sService = permission.getServiceName();
        _assert(sService != null);
        
        if (subject == null)
            {
            subject = (Subject) getThreadContext().get(); // from Security.runAs()
            }
        if (subject == null)
            {
            subject = SecurityHelper.getCurrentSubject();     // from Subject.doAs()
            }
        
        boolean fValid = false;
        
        if (subject == null)
            {
            // no Subject available; try to use the login module
            subject = loginSecure(getDependencies().getCallbackHandler(), null);
            }
        else
            {
            try
                {
                validateSubject(sService, subject);
                fValid = true;
                }
            catch (SecurityException ex)
                {
                // the Subject didn't pass the AccessController validation, which is not
                // necessarily a security breach, but rather missing credentials;
                // try to login using that Subject
                subject = loginSecure(getDependencies().getCallbackHandler(), subject);
                }
            }
        
        if (subject == null)
            {
            throw new SecurityException(
                "Attempt to access a protected resource was made without credentials");
            }
        
        if (!fValid)
            {
            validateSubject(sService, subject);
            }
        
        // TODO: leave the audit trail?
        // _trace("checkPermission: " + permission, 3);
        
        // local check
        com.tangosol.net.security.AccessController controller = getDependencies().getAccessController();
        controller.checkPermission(permission, subject);
        
        if (cluster == null || !cluster.isRunning())
            {
            // strict check is not required or the cluster is not running yet
            return;
            }
        
        if (cluster instanceof SafeCluster)
            {
            // we were called from via the com.tangosol.net.Security API
            // directly passing the safe wrapper
            cluster = ((SafeCluster) cluster).getCluster();
            }
        ClusterService clusterservice = ((Cluster) cluster).getClusterService();
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid        service        = clusterservice.getService(sService);
        
        if (service != null && service.isRunning())
            {
            // we must have already proved the trustworthiness
            return;
            }
        
        clusterservice.getServiceContext().put(sService, encryptPermissionInfo(permission, subject));
        
        // the validation will be done by ClusterService.doServiceJoining()
        }
    
    protected com.tangosol.net.security.PermissionInfo encryptPermissionInfo(com.tangosol.net.ClusterPermission permission, javax.security.auth.Subject subject)
        {
        // import com.tangosol.net.security.PermissionInfo;
        
        try
            {
            return new PermissionInfo(permission, permission.getServiceName(),
                    getDependencies().getAccessController().encrypt(permission, subject), subject);
            }
        catch (Exception e) // GeneralSecurityException, IOException
            {
            throw new SecurityException("Invalid subject credentials: " + e);
            }
        }
    
    // Accessor for the property "Dependencies"
    /**
     * Getter for property Dependencies.<p>
    * The external dependencies needed by this component. The dependencies
    * object must be full populated and validated before this property is set. 
    * See setDependencies.  
    * 
    * The mechanism for creating and populating dependencies is hidden from
    * this component. Typically, the dependencies object is populated using
    * data from some external configuration, such as XML, but this may not
    * always be the case
     */
    public com.tangosol.internal.net.security.StandardDependencies getDependencies()
        {
        return __m_Dependencies;
        }
    
    // Accessor for the property "ServiceContext"
    /**
     * Getter for property ServiceContext.<p>
    * (Private) A map of Service related Subject objects keyed by the service
    * name. Used by the client threads.
     */
    private java.util.Map getServiceContext()
        {
        return __m_ServiceContext;
        }
    
    // Accessor for the property "ThreadContext"
    /**
     * Getter for property ThreadContext.<p>
    * (Private) ThreadLocal holding the Subject. Used by the client threads.
     */
    private ThreadLocal getThreadContext()
        {
        return __m_ThreadContext;
        }
    
    // Accessor for the property "ValidSubjects"
    /**
     * Getter for property ValidSubjects.<p>
    * Set of Subjects that have been validated. To avoid repetetive validations
    * of the same subject (which could be very expensive), we use the
    * LocalCache with 10 second expiration. We don't want to keep then forever
    * to allow policy changes take effect relatively quickly.
     */
    private java.util.Map getValidSubjects()
        {
        return __m_ValidSubjects;
        }
    
    // Declared at the super level
    /**
     * Security debugging helper. Not used for anything else!
     */
    public javax.security.auth.Subject impersonate(javax.security.auth.Subject subject, String sNameOld, String sNameNew)
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ClassHelper;
        // import java.security.Principal;
        // import javax.security.auth.Subject;
        // import java.util.Iterator;
        
        Subject subjectNew = new Subject();
        
        for (Iterator iter = subject.getPrincipals().iterator(); iter.hasNext();)
            {
            Principal p = (Principal) iter.next();
        
            String sName = p.getName();
            if (sName.indexOf(sNameOld) >= 0)
                {
                try
                    {
                    sName = Base.replace(sName, sNameOld, sNameNew);
                    p = (Principal) ClassHelper.newInstance(p.getClass(),
                        new Object[] {sName});
                    _trace("Successfully impersonated " + p + "@" + p.getClass());
                    }
                catch (Exception e)
                    {
                    _trace("Cannot impersonate " + p + "@" + p.getClass());
                    }
                }
            subjectNew.getPrincipals().add(p);
            }
        subjectNew.getPublicCredentials().addAll(subject.getPublicCredentials());
        subjectNew.getPrivateCredentials().addAll(subject.getPrivateCredentials());
        
        return subjectNew;
        }
    
    // Declared at the super level
    /**
     * Subclassing support.
     */
    protected javax.security.auth.Subject loginSecure(javax.security.auth.callback.CallbackHandler handler, javax.security.auth.Subject subject)
        {
        // import java.security.AccessController;
        // import javax.security.auth.login.LoginContext;
        
        Standard.CreateLoginCtxAction action = new Standard.CreateLoginCtxAction();
        action.setDependencies(getDependencies());
        action.setHandler(handler);
        action.setSubject(subject);
        
        LoginContext lc = (LoginContext) AccessController.doPrivileged(action);
        
        try
            {
            lc.login();
            return lc.getSubject();
            }
        catch (Exception e)
            {
            throw new SecurityException("Authentication failed: " + e.getMessage());
            }
        }
    
    /**
     * This event occurs when dependencies are injected into the component. 
    * First, call super.onDependencies to allow all super components to process
    * the Dependencies.  Each component is free to chose how it consumes
    * dependencies.  Typically, the  dependencies are copied into the
    * component's properties.  This technique isolates Dependency Injection
    * from the rest of the component code since components continue to access
    * properties just as they did before. 
    * 
    * However, for read-only dependency properties, the component can access
    * the dependencies directly as shown in the example below for Gateway
    * dependencies.  The advantage to this technique is that the property only
    * exists in the dependencies object, it is not duplicated in the component
    * properties.
    * 
    * StandardDependencies deps = (StandardDependencies) getDependencies();
    * deps.getFoo();
     */
    protected void onDependencies(com.tangosol.internal.net.security.StandardDependencies deps)
        {
        processDependencies(deps);
        }
    
    // Declared at the super level
    /**
     * The "component has been initialized" method-notification called out of
    * setConstructed() for the topmost component and that in turn notifies all
    * the children.
    * 
    * This notification gets called before the control returns back to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as) the control returns back to the
    * instantiator and possibly on a different thread.
     */
    public void onInit()
        {
        // import com.tangosol.net.cache.LocalCache;
        
        setValidSubjects(new LocalCache(Integer.MAX_VALUE, 300000));
        
        super.onInit();
        }
    
    // Declared at the super level
    /**
     * Callback API used to validate and respond to a security related request.
    * Called on a service thread.
    * 
    * @param memberThis the member validating the secure request
    * @param memberFrom the member requesting validation
    * @param oRequestInfo the information to validate
     */
    public Object processSecureRequest(com.tangosol.coherence.component.net.Member memberThis, com.tangosol.coherence.component.net.Member memberFrom, com.tangosol.net.security.PermissionInfo piRequest)
        {
        // import com.tangosol.net.ClusterPermission as com.tangosol.net.ClusterPermission;
        // import com.tangosol.net.security.AccessController as com.tangosol.net.security.AccessController;
        // import com.tangosol.net.security.PermissionInfo;
        // import com.tangosol.util.Base;
        // import javax.security.auth.Subject;
        
        com.tangosol.net.security.AccessController controller    = getDependencies().getAccessController();
        String     sService      = piRequest.getServiceName();
        Subject    subjRequestor = piRequest.getSubject();
        Subject    subjCurrent;
        com.tangosol.net.ClusterPermission permission;
        try
            {
            subjCurrent = (Subject) getServiceContext().get(sService);
            if (subjCurrent == null)
                {
                return new RuntimeException("No service context");
                }
        
            if (memberFrom.equals(memberThis))
                {
                // no need to decrypt since there was no serialization
                permission = piRequest.getPermission();
                }
            else
                {
                permission = (com.tangosol.net.ClusterPermission) controller.decrypt(
                    piRequest.getSignedPermission(), subjRequestor, subjCurrent);
                }
        
            _trace("Remote permission request: " + permission + " by " + memberFrom, 3);
        
            controller.checkPermission(permission, subjRequestor);
            }
        catch (Exception e)
            {
            // let the caller re-throw it
            return Base.ensureRuntimeException(e, "Remote permission check failed");
            }
        
        try
            {
            return encryptPermissionInfo(permission, subjCurrent);
            }
        catch (Exception e)
            {
            return Base.ensureRuntimeException(e, "Remote encryption failed");
            }
        }
    
    // Declared at the super level
    /**
     * Security API used by the Service components. Called on a service thread
    * upon the service termination.
    * 
    * @param sServiceName  the relevant Service name
     */
    public void releaseSecureContext(String sServiceName)
        {
        getServiceContext().remove(sServiceName);
        }
    
    // Declared at the super level
    /**
     * Helper method.
     */
    public static Object runAnonymously(Object oAction)
            throws java.security.PrivilegedActionException
        {
        return com.tangosol.coherence.component.net.Security.runAnonymously(oAction);
        }
    
    // Declared at the super level
    /**
     * Subclassing support.
     */
    protected Object runSecure(javax.security.auth.Subject subject, Object oAction)
            throws java.security.PrivilegedActionException
        {
        // import java.security.PrivilegedAction;
        // import java.security.PrivilegedExceptionAction;
        
        // _trace("runSecure " + oAction + "\n as " + subject.getPrincipals(), 3);
        if (subject == null)
            {
            return runAnonymously(oAction);
            }
        
        getThreadContext().set(subject);
        try
            {
            return oAction instanceof PrivilegedAction ?
                Subject.doAs(subject, (PrivilegedAction) oAction) :
                Subject.doAs(subject, (PrivilegedExceptionAction) oAction);
            }
        finally
            {
            getThreadContext().set(null);
            }
        }
    
    // Accessor for the property "Dependencies"
    /**
     * Inject the Dependencies object into the component.  First clone the
    * dependencies, then validate the cloned copy.  Note that the validate
    * method may modify the cloned dependencies, so it is important to use the
    * cloned dependencies for all subsequent operations.  Once the dependencies
    * have been validated, call onDependencies so that each Componenet in the
    * class hierarchy can process the dependencies as needed.  
     */
    public void setDependencies(com.tangosol.internal.net.security.StandardDependencies deps)
        {
        // import com.tangosol.internal.net.security.DefaultStandardDependencies;
        
        if (getDependencies() != null)
            {
            throw new IllegalStateException("Dependencies already set");
            }
        
        __m_Dependencies = (new DefaultStandardDependencies(deps).validate());
        
        // use the cloned dependencies
        onDependencies(getDependencies());
        }
    
    // Accessor for the property "ServiceContext"
    /**
     * Setter for property ServiceContext.<p>
    * (Private) A map of Service related Subject objects keyed by the service
    * name. Used by the client threads.
     */
    private void setServiceContext(java.util.Map mapCtx)
        {
        __m_ServiceContext = mapCtx;
        }
    
    // Accessor for the property "ThreadContext"
    /**
     * Setter for property ThreadContext.<p>
    * (Private) ThreadLocal holding the Subject. Used by the client threads.
     */
    private void setThreadContext(ThreadLocal ctx)
        {
        __m_ThreadContext = ctx;
        }
    
    // Accessor for the property "ValidSubjects"
    /**
     * Setter for property ValidSubjects.<p>
    * Set of Subjects that have been validated. To avoid repetetive validations
    * of the same subject (which could be very expensive), we use the
    * LocalCache with 10 second expiration. We don't want to keep then forever
    * to allow policy changes take effect relatively quickly.
     */
    private void setValidSubjects(java.util.Map cache)
        {
        __m_ValidSubjects = cache;
        }
    
    /**
     * Prevent a security hole when a caller would construct a Subject object
    * with a Principal object that have a high security clearance, but provide
    * a valid cerificate representing a low security clearance Principal. The
    * very first validated subject becomes assosiated with the specified
    * service.
     */
    protected void validateSubject(String sService, javax.security.auth.Subject subject)
        {
        // import com.tangosol.net.security.AccessController as com.tangosol.net.security.AccessController;
        // import java.util.Map;
        // import javax.security.auth.Subject;
        
        Map mapValid = getValidSubjects();
        
        if (!mapValid.containsKey(subject))
            {
            com.tangosol.net.security.AccessController controller = getDependencies().getAccessController();
            Object     oTest      = Double.valueOf(Math.random());
            try
                {
                Object o = controller.decrypt(
                    controller.encrypt(oTest, subject), subject, null);
                _assert(o.equals(oTest));
                mapValid.put(subject, null); // will expire shortly
                }
            catch (Exception e)
                {
                _trace("Failed to verify the subject: " + subject + " due to: " + e.getMessage(), 3);
                throw new SecurityException("Failed to verify the subject");
                }
            }
        
        Map     mapContext  = getServiceContext();
        Subject subjCurrent = (Subject) mapContext.get(sService);
        
        if (subjCurrent == null)
            {
            mapContext.put(sService, subject);
            }
        }
    
    // Declared at the super level
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
        // import com.tangosol.net.ClusterPermission;
        // import com.tangosol.util.Base;
        // import java.security.AccessControlException;
        // import java.security.GeneralSecurityException;
        // import javax.security.auth.Subject;
        
        Subject           subject    = (Subject) getServiceContext().get(service.getInfo().getServiceName());
        ClusterPermission permission = null;
        try
            {
            permission = (ClusterPermission) getDependencies().getAccessController().decrypt(
                info.getSignedPermission(), info.getSubject(), subject);
            }
        catch (GeneralSecurityException e)
            {
            throw new AccessControlException(
                "Security configuration mismatch or break-in attempt", permission);
            }
        catch (Exception e) // ClassNotFoundException, IOException
            {
            throw Base.ensureRuntimeException(e, "Security configuration mismatch");
            }
        }

    // ---- class: com.tangosol.coherence.component.net.security.Standard$CreateLoginCtxAction
    
    /**
     * Privileged action to login context.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class CreateLoginCtxAction
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        /**
         * Property Dependencies
         *
         */
        private com.tangosol.internal.net.security.StandardDependencies __m_Dependencies;
        
        /**
         * Property Handler
         *
         */
        private javax.security.auth.callback.CallbackHandler __m_Handler;
        
        /**
         * Property Subject
         *
         */
        private javax.security.auth.Subject __m_Subject;
        
        // Default constructor
        public CreateLoginCtxAction()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public CreateLoginCtxAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.security.Standard.CreateLoginCtxAction();
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
                clz = Class.forName("com.tangosol.coherence/component/net/security/Standard$CreateLoginCtxAction".replace('/', '.'));
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
        
        // Accessor for the property "Dependencies"
        /**
         * Getter for property Dependencies.<p>
         */
        public com.tangosol.internal.net.security.StandardDependencies getDependencies()
            {
            return __m_Dependencies;
            }
        
        // Accessor for the property "Handler"
        /**
         * Getter for property Handler.<p>
         */
        public javax.security.auth.callback.CallbackHandler getHandler()
            {
            return __m_Handler;
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
            // import com.tangosol.internal.net.security.StandardDependencies;
            // import com.tangosol.util.Base;
            // import javax.security.auth.Subject;
            // import javax.security.auth.callback.CallbackHandler;
            // import javax.security.auth.login.LoginContext;
            
            StandardDependencies dps     = getDependencies();
            CallbackHandler      handler = getHandler();
            Subject              subject = getSubject();
            
            LoginContext lc;
            
            try
                {
                lc = handler == null ?
                        subject == null ?
                            new LoginContext(dps.getLoginModuleName()) :
                            new LoginContext(dps.getLoginModuleName(), subject) :
                        subject == null ?
                            new LoginContext(dps.getLoginModuleName(), handler) :
                            new LoginContext(dps.getLoginModuleName(), subject, handler); 
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e, "Failed to create LoginContext");
                }
            
            return lc;
            }
        
        // Accessor for the property "Dependencies"
        /**
         * Setter for property Dependencies.<p>
         */
        public void setDependencies(com.tangosol.internal.net.security.StandardDependencies dependencies)
            {
            __m_Dependencies = dependencies;
            }
        
        // Accessor for the property "Handler"
        /**
         * Setter for property Handler.<p>
         */
        public void setHandler(javax.security.auth.callback.CallbackHandler handler)
            {
            __m_Handler = handler;
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
    }
