
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.base.Timeout;

import com.tangosol.coherence.component.util.safeNamedTopic.SafePublisherConnector;
import com.tangosol.coherence.component.util.safeNamedTopic.SafeSubscriberConnector;

import com.tangosol.internal.net.topic.NamedTopicPublisher;
import com.tangosol.internal.net.topic.NamedTopicSubscriber;
import com.tangosol.internal.net.topic.PublisherConnector;
import com.tangosol.internal.net.topic.SubscriberConnector;

import com.tangosol.io.ClassLoaderAware;

import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.ServiceDependencies;
import com.tangosol.net.TopicService;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.NamedTopicEvent;
import com.tangosol.net.topic.NamedTopicListener;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.Listeners;
import com.tangosol.util.SynchronousListener;
import com.tangosol.util.ValueExtractor;

import java.util.concurrent.TimeUnit;

import java.util.concurrent.locks.Lock;

import javax.security.auth.Subject;

/**
 * A safe wrapper for {@link NamedTopic}.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class SafeNamedTopic<V>
        extends com.tangosol.coherence.component.Util
        implements com.tangosol.net.topic.NamedTopic<V>,
                   ClassLoaderAware,
                   PublisherConnector.Factory<V>,
                   SubscriberConnector.Factory<V>
    {
    // ---- Fields declarations ----

    /**
     * Property ClassLoader
     */
    private transient ClassLoader __m_ClassLoader;

    /**
     * Property Destroyed
     * <p>
     * Specifies whether or not the underlying NamedCache has been explicitly
     * destroyed.
     */
    private boolean __m_Destroyed;

    /**
     * Property EnsureTopicAction
     * <p>
     * PrivilegedAction to call ensureRunningNamedService.
     */
    private java.security.PrivilegedAction __m_EnsureTopicAction;

    /**
     * Property InternalNamedTopic
     */
    private com.tangosol.net.topic.NamedTopic __m_InternalNamedTopic;

    /**
     * Property Lock
     * <p>
     * Lock used to protect this SafeNamedCache instance against multi-threaded
     * usage.
     */
    private java.util.concurrent.locks.ReentrantLock __m_Lock;

    /**
     * Property Released
     * <p>
     * Specifies whether or not the underlying NamedCache has been explicitly
     * released.
     */
    private boolean __m_Released;

    /**
     * Property RestartTopicAction
     * <p>
     * PrivilegedAction to call ensureCache.
     */
    private java.security.PrivilegedAction __m_RestartTopicAction;

    /**
     * Property SafeTopicService
     */
    private TopicService __m_SafeTopicService;

    /**
     * Property Started
     */
    private volatile boolean __m_Started;

    /**
     * Property Subject
     * <p>
     * The optional Subject associated with the cache.
     */
    private javax.security.auth.Subject __m_Subject;

    /**
     * Property TopicName
     */
    private String __m_TopicName;

    /**
     * The registered topic listeners.
     */
    private final Listeners f_listeners = new Listeners();

    private NamedTopicListener m_listener;

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
        __mapChildren.put("EnsureTopicAction", SafeNamedTopic.EnsureTopicAction.class);
        __mapChildren.put("RestartTopicAction", SafeNamedTopic.RestartTopicAction.class);
        }

    // Default constructor
    public SafeNamedTopic()
        {
        this(null, null, true);
        }

    // Initializing constructor
    public SafeNamedTopic(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        if (fInit)
            {
            __init();
            }
        }

    @Override
    public void __init()
        {
        // private initialization
        __initPrivate();

        // state initialization: public and protected properties
        try
            {
            setLock(new java.util.concurrent.locks.ReentrantLock());
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

    @Override
    protected void __initPrivate()
        {
        m_listener = new Listener();
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
            clz = Class.forName("com.tangosol.coherence/component/util/SafeNamedTopic".replace('/', '.'));
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
     * <p>
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
     * <p>
     * Note: the class generator will ignore any custom implementation for this
     * behavior.
     */
    @Override
    protected java.util.Map get_ChildClasses()
        {
        return __mapChildren;
        }

    @Override
    public void addListener(NamedTopicListener listener)
        {
        f_listeners.add(listener);
        }

    @Override
    public void removeListener(NamedTopicListener listener)
        {
        f_listeners.remove(listener);
        }

    @Override
    public int getChannelCount()
        {
        return getRunningNamedTopic().getChannelCount();
        }

    @Override
    public int getRemainingMessages(String sSubscriberGroup)
        {
        return getRunningNamedTopic().getRemainingMessages(sSubscriberGroup);
        }

    @Override
    public int getRemainingMessages(String Param_1, int[] Param_2)
        {
        return getRunningNamedTopic().getRemainingMessages(Param_1, Param_2);
        }

    @Override
    public Publisher<V> createPublisher(Publisher.Option[] options)
        {
        PublisherConnector<V> connector = new SafePublisherConnector<>(this, options);
        return new NamedTopicPublisher<>(this, connector, options);
        }

    @Override
    public <U> Subscriber<U> createSubscriber(Subscriber.Option<? super V, U>[] options)
        {
        SubscriberConnector<U> connector = new SafeSubscriberConnector<>(this, options);
        return new NamedTopicSubscriber<>(this, connector, options);
        }

    @Override
    public void ensureSubscriberGroup(String sGroupName, Filter filter, ValueExtractor extractor)
        {
        if (sGroupName == null)
            {
            throw new IllegalArgumentException("invalid group name");
            }
        getNamedTopic().ensureSubscriberGroup(sGroupName, filter, extractor);
        }

    @Override
    public void destroySubscriberGroup(String sGroupName)
        {
        if (sGroupName == null)
            {
            throw new IllegalArgumentException("invalid group name");
            }
        getNamedTopic().destroySubscriberGroup(sGroupName);
        }

    private com.tangosol.net.Service getService$Router()
        {
        return getRunningNamedTopic().getService();
        }

    @Override
    public com.tangosol.net.Service getService()
        {
        return getSafeTopicService();
        }

    @Override
    public java.util.Set getSubscriberGroups()
        {
        return getRunningNamedTopic().getSubscriberGroups();
        }

    @Override
    public void destroy()
        {
        TopicService safeservice = getSafeTopicService();

        safeservice.destroyTopic(this);

        ensureGlobalLock();
        try
            {
            setDestroyed(true);
            setReleased(true);
            setInternalNamedTopic(null);
            }
        finally
            {
            unlockGlobal();
            }
        }

    /**
     * Ensure the caller acquires all locks,  including SafeCluster, SafeService
     * and SafeNamedCache locks, or an excpetion is thrown.
     */
    public void ensureGlobalLock()
        {
        SafeService service = (SafeService) getSafeTopicService();

        service.ensureGlobalLock();
        try
            {
            ensureLocked();
            }
        catch (RuntimeException e)
            {
            service.unlockGlobal();
            throw e;
            }
        }

    /**
     * Ensure the caller acquires the SafeNamedCache lock, or an excpetion is
     * thrown.
     */
    public void ensureLocked()
        {
        // import com.oracle.coherence.common.base.Timeout;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.net.ServiceDependencies;
        // import com.tangosol.util.Base;
        // import java.util.concurrent.locks.Lock;
        // import java.util.concurrent.TimeUnit;

        ServiceDependencies deps = getSafeTopicService().getDependencies();

        long cRequestTimeout = deps == null ? 0L : deps.getRequestTimeoutMillis();
        long cTimeout = Timeout.isSet()
                ? Timeout.remainingTimeoutMillis()
                : cRequestTimeout;

        Lock lock = getLock();
        try
            {
            if (lock.tryLock(cTimeout <= 0 ? Long.MAX_VALUE : cTimeout, TimeUnit.MILLISECONDS))
                {
                return;
                }

            throw Base.ensureRuntimeException(
                    new RequestTimeoutException("Failed to acquire NamedTopic lock in " + cTimeout + "ms"));
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw Base.ensureRuntimeException(e, "Interrupted while attempting to acquire NamedTopic lock");
            }
        }

    public com.tangosol.net.topic.NamedTopic ensureRunningNamedTopic()
        {
        NamedTopic  topic       = getInternalNamedTopic();
        SafeService serviceSafe = (SafeService) getSafeTopicService();

        if (serviceSafe == null || !serviceSafe.isRunning() ||
                topic == null || !topic.isActive() || !isStarted())
            {
            if ((serviceSafe == null || !serviceSafe.isRunning()) && serviceSafe.isServiceThread())
                {
                throw new IllegalStateException(
                        "Service can not be restarted on a thread owned by the service");
                }

            ensureGlobalLock();
            try
                {
                topic       = getInternalNamedTopic();
                serviceSafe = (SafeService) getSafeTopicService();

                if (serviceSafe == null || !serviceSafe.isRunning() ||
                        topic == null || !topic.isActive() || !isStarted())
                    {
                    if (isReleased() || isDestroyed())
                        {
                        String reason = isDestroyed() ? "destroyed" : "released";
                        throw new IllegalStateException("SafeNamedTopic \"" + __m_TopicName
                                + "\" was explicitly " + reason);
                        }
                    else
                        {
                        // restart the actual named topic
                        if (topic != null)
                            {
                            setInternalNamedTopic(null);
                            _trace("Restarting NamedTopic: " + getTopicName(), 3);
                            }

                        setInternalNamedTopic(topic = restartNamedTopic());

                        setStarted(true);
                        }
                    }
                }
            finally
                {
                unlockGlobal();
                }
            }

        return topic;
        }

    // Accessor for the property "ClassLoader"

    /**
     * Getter for property ClassLoader.<p>
     */
    public ClassLoader getClassLoader()
        {
        return __m_ClassLoader;
        }

    // Accessor for the property "EnsureTopicAction"

    /**
     * Getter for property EnsureTopicAction.<p>
     * PrivilegedAction to call ensureRunningNamedService.
     */
    public java.security.PrivilegedAction getEnsureTopicAction()
        {
        return __m_EnsureTopicAction;
        }

    // Accessor for the property "InternalNamedTopic"

    /**
     * Getter for property InternalNamedTopic.<p>
     */
    public com.tangosol.net.topic.NamedTopic getInternalNamedTopic()
        {
        return __m_InternalNamedTopic;
        }

    // Accessor for the property "Lock"

    /**
     * Getter for property Lock.<p>
     * Lock used to protect this SafeNamedCache instance against multi-threaded
     * usage.
     */
    public java.util.concurrent.locks.ReentrantLock getLock()
        {
        return __m_Lock;
        }

    // From interface: com.tangosol.net.topic.NamedTopic
    public String getName()
        {
        return getTopicName();
        }

    // Accessor for the property "NamedTopic"

    /**
     * Getter for property NamedTopic.<p>
     */
    public com.tangosol.net.topic.NamedTopic getNamedTopic()
        {
        return getInternalNamedTopic();
        }

    // Accessor for the property "RestartTopicAction"

    /**
     * Getter for property RestartTopicAction.<p>
     * PrivilegedAction to call ensureCache.
     */
    public java.security.PrivilegedAction getRestartTopicAction()
        {
        return __m_RestartTopicAction;
        }

    // Accessor for the property "RunningNamedTopic"

    /**
     * Getter for property RunningNamedTopic.<p>
     */
    protected com.tangosol.net.topic.NamedTopic getRunningNamedTopic()
        {
        return ensureRunningNamedTopic();
        }

    // Accessor for the property "SafeTopicService"

    /**
     * Getter for property SafeTopicService.<p>
     */
    public TopicService getSafeTopicService()
        {
        return __m_SafeTopicService;
        }

    // Accessor for the property "Subject"

    /**
     * Getter for property Subject.<p>
     * The optional Subject associated with the cache.
     */
    public javax.security.auth.Subject getSubject()
        {
        return __m_Subject;
        }

    // Accessor for the property "TopicName"

    /**
     * Getter for property TopicName.<p>
     */
    public String getTopicName()
        {
        return __m_TopicName;
        }

    // From interface: com.tangosol.net.topic.NamedTopic
    public boolean isActive()
        {
        try
            {
            return getInternalNamedTopic().isActive();
            }
        catch (RuntimeException e)
            {
            return false;
            }
        }

    // From interface: com.tangosol.net.topic.NamedTopic
    // Accessor for the property "Destroyed"

    /**
     * Getter for property Destroyed.<p>
     * Specifies whether the underlying NamedCache has been explicitly
     * destroyed.
     */
    public boolean isDestroyed()
        {
        try
            {
            return __m_Destroyed || getInternalNamedTopic().isDestroyed();
            }
        catch (RuntimeException e)
            {
            // no way to compute, so return false
            return false;
            }
        }

    // From interface: com.tangosol.net.topic.NamedTopic
    // Accessor for the property "Released"

    /**
     * Getter for property Released.<p>
     * Specifies whether or not the underlying NamedCache has been explicitly
     * released.
     */
    public boolean isReleased()
        {
        try
            {
            return __m_Released || getInternalNamedTopic().isReleased();
            }
        catch (RuntimeException e)
            {
            // no way to compute, so return false
            return false;
            }
        }

    // Accessor for the property "Started"

    /**
     * Getter for property Started.<p>
     */
    public boolean isStarted()
        {
        return __m_Started;
        }

    // Declared at the super level

    /**
     * The "component has been initialized" method-notification called out of
     * setConstructed() for the topmost component and that in turn notifies all
     * the children.
     * <p>
     * This notification gets called before the control returns back to this
     * component instantiator (using <code>new Component.X()</code> or
     * <code>_newInstance(sName)</code>) and on the same thread. In addition,
     * visual components have a "posted" notification <code>onInitUI</code> that
     * is called after (or at the same time as) the control returns back to the
     * instantiator and possibly on a different thread.
     */
    public void onInit()
        {
        setEnsureTopicAction((SafeNamedTopic.EnsureTopicAction) _newChild("EnsureTopicAction"));
        setRestartTopicAction((SafeNamedTopic.RestartTopicAction) _newChild("RestartTopicAction"));
        }

    // From interface: com.tangosol.net.topic.NamedTopic
    public void release()
        {
        // import Component.Util.SafeService.SafeCacheService.SafeDistributedCacheService.SafePagedTopicService;

        TopicService safeservice = getSafeTopicService();

        safeservice.releaseTopic(this);

        ensureGlobalLock();
        try
            {
            setReleased(true);
            setClassLoader(null);
            setInternalNamedTopic(null);
            }
        finally
            {
            unlockGlobal();
            }
        }

    protected com.tangosol.net.topic.NamedTopic restartNamedTopic()
        {
        // import com.tangosol.net.TopicService;
        // import com.tangosol.net.topic.NamedTopic;
        // import javax.security.auth.Subject;

        Subject subject = getSubject();

        // In case the underlying topic is scoped by Subject, use the original Subject
        NamedTopic topic;

        if (subject == null)
            {
            TopicService service = (TopicService) ((SafeService) getSafeTopicService()).getRunningService();
            topic = service.ensureTopic(getTopicName(), getClassLoader());
            }
        else
            {
            topic = (NamedTopic) Subject.doAs(subject, getRestartTopicAction());
            }

        return topic;
        }

    // Accessor for the property "ClassLoader"

    /**
     * Setter for property ClassLoader.<p>
     */
    public void setClassLoader(ClassLoader loaderClass)
        {
        __m_ClassLoader = loaderClass;
        }

    // Accessor for the property "Destroyed"

    /**
     * Setter for property Destroyed.<p>
     * Specifies whether the underlying NamedCache has been explicitly
     * destroyed.
     */
    private void setDestroyed(boolean fDestroyed)
        {
        __m_Destroyed = fDestroyed;
        }

    // Accessor for the property "EnsureTopicAction"

    /**
     * Setter for property EnsureTopicAction.<p>
     * PrivilegedAction to call ensureRunningNamedService.
     */
    protected void setEnsureTopicAction(java.security.PrivilegedAction actionTopic)
        {
        __m_EnsureTopicAction = actionTopic;
        }

    // Accessor for the property "InternalNamedTopic"

    /**
     * Setter for property InternalNamedTopic.<p>
     */
    public void setInternalNamedTopic(com.tangosol.net.topic.NamedTopic topic)
        {
        if (topic == null)
            {
            setStarted(false);
            }
        NamedTopic<?> internal = __m_InternalNamedTopic;
        __m_InternalNamedTopic = topic;
        if (topic != null)
            {
            topic.addListener(m_listener);
            }
        }

    // Accessor for the property "Lock"

    /**
     * Setter for property Lock.<p>
     * Lock used to protect this SafeNamedCache instance against multi-threaded
     * usage.
     */
    public void setLock(java.util.concurrent.locks.ReentrantLock lock)
        {
        __m_Lock = lock;
        }

    // Accessor for the property "Released"

    /**
     * Setter for property Released.<p>
     * Specifies whether or not the underlying NamedCache has been explicitly
     * released.
     */
    public void setReleased(boolean fRelease)
        {
        ensureLocked();
        try
            {
            if (isReleased() && !fRelease)
                {
                throw new IllegalStateException("Topic cannot be un-released");
                }

            __m_Released = (fRelease);
            }
        finally
            {
            getLock().unlock();
            }
        }

    // Accessor for the property "RestartTopicAction"

    /**
     * Setter for property RestartTopicAction.<p>
     * PrivilegedAction to call ensureCache.
     */
    protected void setRestartTopicAction(java.security.PrivilegedAction actionTopic)
        {
        __m_RestartTopicAction = actionTopic;
        }

    // Accessor for the property "SafeTopicService"

    /**
     * Setter for property SafeTopicService.<p>
     */
    public void setSafeTopicService(TopicService sProperty)
        {
        __m_SafeTopicService = sProperty;
        }

    // Accessor for the property "Started"

    /**
     * Setter for property Started.<p>
     */
    public void setStarted(boolean fStarted)
        {
        __m_Started = fStarted;
        }

    // Accessor for the property "Subject"

    /**
     * Setter for property Subject.<p>
     * The optional Subject associated with the cache.
     */
    public void setSubject(javax.security.auth.Subject subject)
        {
        __m_Subject = subject;
        }

    // Accessor for the property "TopicName"

    /**
     * Setter for property TopicName.<p>
     */
    public void setTopicName(String sName)
        {
        __m_TopicName = sName;
        }

    // Declared at the super level
    public String toString()
        {
        return get_Name() + ": " + getInternalNamedTopic() +", loader=" + __m_ClassLoader;
        }

    public void unlock()
        {
        getLock().unlock();
        }

    /**
     * Unlock SafeNamedCache, SafeService and SafeCluster locks.
     */
    public void unlockGlobal()
        {
        unlock();
        ((SafeService) getSafeTopicService()).unlockGlobal();
        }

    @Override
    public ClassLoader getContextClassLoader()
        {
        return __m_ClassLoader;
        }

    @Override
    public void setContextClassLoader(ClassLoader loader)
        {
        throw new UnsupportedOperationException();
        }

    // ---- class: com.tangosol.coherence.component.util.SafeNamedTopic$EnsureTopicAction

    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class EnsureTopicAction
            extends com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----

        // Default constructor
        public EnsureTopicAction()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public EnsureTopicAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.SafeNamedTopic.EnsureTopicAction();
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
                clz = Class.forName("com.tangosol.coherence/component/util/SafeNamedTopic$EnsureTopicAction".replace('/', '.'));
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
         * <p>
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
            return ((SafeNamedTopic) get_Module()).ensureRunningNamedTopic();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.SafeNamedTopic$RestartTopicAction

    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class RestartTopicAction
            extends com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----

        // Default constructor
        public RestartTopicAction()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public RestartTopicAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.SafeNamedTopic.RestartTopicAction();
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
                clz = Class.forName("com.tangosol.coherence/component/util/SafeNamedTopic$RestartTopicAction".replace('/', '.'));
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
         * <p>
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
            // import com.tangosol.net.TopicService;

            SafeNamedTopic topicSafe = (SafeNamedTopic) get_Module();
            TopicService   service   = (TopicService) ((SafeService) topicSafe.getSafeTopicService()).getRunningService();

            return service.ensureTopic(topicSafe.getTopicName(), topicSafe.getClassLoader());
            }
        }

    // ----- inner class Listener -------------------------------------------

    /**
     * A topic listener to convert topic events.
     */
    protected class Listener
            implements NamedTopicListener, SynchronousListener
        {
        @Override
        public void onEvent(NamedTopicEvent evt)
            {
            SafeNamedTopic topicSafe = (SafeNamedTopic) get_Module();
            try
                {
                NamedTopicEvent event = evt.replaceSource(topicSafe);
                event.dispatch(f_listeners);
                if (evt.getType() == NamedTopicEvent.Type.Destroyed)
                    {
                    evt.getSource().removeListener(m_listener);
                    }
                }
            catch (Exception e)
                {
                Logger.err("Error dispatching event " + evt, e);
                }
            }
        }
    }
