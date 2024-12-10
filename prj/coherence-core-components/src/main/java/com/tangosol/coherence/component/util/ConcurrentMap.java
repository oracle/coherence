
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.ConcurrentMap

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.component.util.collections.wrapperSet.EntrySet;
import com.tangosol.coherence.component.util.collections.wrapperSet.KeySet;

/**
 * Simple implementation of ConcurrentMap (and ObservableMap). This component
 * is a trivial integration of com.tangosol.util.WrapperConcurrentMap.
 * 
 * Note: the ConcurrentMap has to be instantiated using _initFeed(Map)
 * constructor.
 * 
 * Known subclasses LocalCache$CacheHandler.
 */
/*
* Integrates
*     com.tangosol.util.WrapperConcurrentMap
*     using Component.Dev.Compiler.Integrator.AbstractBean.JavaBean
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ConcurrentMap
        extends    com.tangosol.coherence.component.Util
        implements com.tangosol.util.ConcurrentMap,
                   com.tangosol.util.ObservableMap
    {
    // ---- Fields declarations ----
    
    // fields used by the integration model:
    private sink_ConcurrentMap __sink;
    private com.tangosol.util.WrapperConcurrentMap __feed;
    
    // Default constructor
    public ConcurrentMap()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ConcurrentMap(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
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
        return new com.tangosol.coherence.component.util.ConcurrentMap();
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
            clz = Class.forName("com.tangosol.coherence/component/util/ConcurrentMap".replace('/', '.'));
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
    
    //++ com.tangosol.util.WrapperConcurrentMap integration
    // Access optimization
    /**
     * Setter for property _Sink.<p>
    * Property used by Component Integration. This property is not meant to be
    * designed directly.
     */
    public void set_Sink(Object pSink)
        {
        __sink = (sink_ConcurrentMap) pSink;
        super.set_Sink(pSink);
        }
    /**
     * Setter for property _Feed.<p>
    * Property used by Component Integration. This property is not meant to be
    * designed directly.
     */
    public void set_Feed(Object pFeed)
        {
        __feed = (com.tangosol.util.WrapperConcurrentMap) pFeed;
        super.set_Feed(pFeed);
        }
    private void _initFeed$AutoGen(java.util.Map map)
        {
        jb_ConcurrentMap.__tloPeer.setObject(this);
        new jb_ConcurrentMap(map, this, false); // this sets the Sink which sets the Feed
        __init();
        }
    /**
     * Integratee constructor
     */
    public void _initFeed(java.util.Map map)
        {
        _initFeed$AutoGen(map);
        }
    private void _initFeed$AutoGen(java.util.Map map, boolean fEnforceLocking, long cWaitMillis)
        {
        jb_ConcurrentMap.__tloPeer.setObject(this);
        new jb_ConcurrentMap(map, fEnforceLocking, cWaitMillis, this, false); // this sets the Sink which sets the Feed
        __init();
        }
    /**
     * Integratee constructor
     */
    public void _initFeed(java.util.Map map, boolean fEnforceLocking, long cWaitMillis)
        {
        _initFeed$AutoGen(map, fEnforceLocking, cWaitMillis);
        }
    // properties integration
    // methods integration
    public void addMapListener(com.tangosol.util.MapListener listener)
        {
        __sink.addMapListener(listener);
        }
    public void addMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, boolean fLite)
        {
        __sink.addMapListener(listener, filter, fLite);
        }
    public void addMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fLite)
        {
        __sink.addMapListener(listener, oKey, fLite);
        }
    public void clear()
        {
        __sink.clear();
        }
    public boolean containsKey(Object oKey)
        {
        return __sink.containsKey(oKey);
        }
    public boolean containsValue(Object oValue)
        {
        return __sink.containsValue(oValue);
        }
    private java.util.Set entrySet$Router()
        {
        return __sink.entrySet();
        }
    public java.util.Set entrySet()
        {
        // import Component.Util.Collections.WrapperSet.EntrySet;
        
        return EntrySet.instantiate(entrySet$Router(), this);
        }
    public Object get(Object oKey)
        {
        return __sink.get(oKey);
        }
    /**
     * Getter for property ActualMap.<p>
    * The actual map.
    * <b>Note: direct modifications of the returned map may cause an
    * unpredictable behavior of the ConcurrentMap.</b>
     */
    public java.util.Map getActualMap()
        {
        return __sink.getMap();
        }
    /**
     * Getter for property Empty.<p>
     */
    public boolean isEmpty()
        {
        return __sink.isEmpty();
        }
    private java.util.Set keySet$Router()
        {
        return __sink.keySet();
        }
    public java.util.Set keySet()
        {
        // import Component.Util.Collections.WrapperSet.KeySet;
        
        return KeySet.instantiate(keySet$Router(), this);
        }
    public boolean lock(Object oKey)
        {
        return __sink.lock(oKey);
        }
    public boolean lock(Object oKey, long cWait)
        {
        return __sink.lock(oKey, cWait);
        }
    public Object put(Object oKey, Object oValue)
        {
        return __sink.put(oKey, oValue);
        }
    public void putAll(java.util.Map map)
        {
        __sink.putAll(map);
        }
    public Object remove(Object oKey)
        {
        return __sink.remove(oKey);
        }
    public void removeMapListener(com.tangosol.util.MapListener listener)
        {
        __sink.removeMapListener(listener);
        }
    public void removeMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter)
        {
        __sink.removeMapListener(listener, filter);
        }
    public void removeMapListener(com.tangosol.util.MapListener listener, Object oKey)
        {
        __sink.removeMapListener(listener, oKey);
        }
    public int size()
        {
        return __sink.size();
        }
    public boolean unlock(Object oKey)
        {
        return __sink.unlock(oKey);
        }
    public java.util.Collection values()
        {
        return __sink.values();
        }
    //-- com.tangosol.util.WrapperConcurrentMap integration
    
    // Declared at the super level
    public String toString()
        {
        return getActualMap().toString();
        }

    /* 
     * Class jb_ConcurrentMap
     *
     * automatically generated "Feed" which
     * a) extends an external bean:
     *      com.tangosol.util.WrapperConcurrentMap
     * b) delegates to the peer component:
     *      Component.Util.ConcurrentMap
     */
    private static class jb_ConcurrentMap
            extends    com.tangosol.util.WrapperConcurrentMap
            implements com.tangosol.run.component.ComponentPeer
        {
        // thread local storage for component peer during
        // the integratee and component peer initialization
        static com.tangosol.util.ThreadLocalObject __tloPeer;
        static
            {
            __tloPeer = new com.tangosol.util.ThreadLocalObject();
            }
        
        // component peer (integrator) accessible from sub-classes
        protected ConcurrentMap __peer;
        
        private static ConcurrentMap __createPeer(Class clzPeer)
            {
            try
                {
                // create uninitialized component peer
                ConcurrentMap peer = (ConcurrentMap)
                    com.tangosol.util.ClassHelper.newInstance
                        (clzPeer, new Object[] {null, null, Boolean.FALSE});
                
                // set-up the storage and return
                __tloPeer.setObject(peer);
                return peer;
                }
            catch (Exception e)
                {
                // catch everything and re-throw as a runtime exception
                throw new com.tangosol.run.component.IntegrationException(e.getMessage());
                }
            }
        
        // parameterized constructor
        public jb_ConcurrentMap(java.util.Map map)
            {
            this(map, ConcurrentMap.get_CLASS());
            }
        
        // parameterized constructor
        public jb_ConcurrentMap(java.util.Map map, boolean fEnforceLocking, long cWaitMillis)
            {
            this(map, fEnforceLocking, cWaitMillis, ConcurrentMap.get_CLASS());
            }
        
        // the following constructor is used only by derived beans
        // to create the corresponding component peer and hook it up
        protected jb_ConcurrentMap(java.util.Map map, Class clzPeer)
            {
            this(map, __createPeer(clzPeer), true);
            }
        
        // the following constructor is used only by derived beans
        // to create the corresponding component peer and hook it up
        protected jb_ConcurrentMap(java.util.Map map, boolean fEnforceLocking, long cWaitMillis, Class clzPeer)
            {
            this(map, fEnforceLocking, cWaitMillis, __createPeer(clzPeer), true);
            }
        
        // this (package private) constructor is used by both:
        // the component peer to hook up (fInit set to false)
        // and default Javabean constructor (fInit set to true)
        jb_ConcurrentMap(java.util.Map map, ConcurrentMap peer, boolean fInit)
            {
            super(map);
            
            if (__retrievePeer() != peer)
                {
                throw new com.tangosol.run.component.IntegrationException("Invalid peer component");
                }
            if (fInit)
                {
                peer.__init();
                }
            }
        
        // this (package private) constructor is used by both:
        // the component peer to hook up (fInit set to false)
        // and default Javabean constructor (fInit set to true)
        jb_ConcurrentMap(java.util.Map map, boolean fEnforceLocking, long cWaitMillis, ConcurrentMap peer, boolean fInit)
            {
            super(map, fEnforceLocking, cWaitMillis);
            
            if (__retrievePeer() != peer)
                {
                throw new com.tangosol.run.component.IntegrationException("Invalid peer component");
                }
            if (fInit)
                {
                peer.__init();
                }
            }
        
        private ConcurrentMap __retrievePeer()
            {
            if (__peer == null)
                {
                // first call -- the peer must be in the thread local storage
                __peer = (ConcurrentMap) __tloPeer.getObject();
                
                // clean-up the storage
                __tloPeer.setObject(null);
                
                // create the sink and notify the component peer
                __peer.set_Sink(new sink_ConcurrentMap(this));
                }
            return __peer;
            }
        
        // methods integration and/or remoted
        public void addMapListener(com.tangosol.util.MapListener listener)
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            peer.addMapListener(listener);
            }
        void super$addMapListener(com.tangosol.util.MapListener listener)
            {
            super.addMapListener(listener);
            }
        public void addMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, boolean fLite)
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            peer.addMapListener(listener, filter, fLite);
            }
        void super$addMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, boolean fLite)
            {
            super.addMapListener(listener, filter, fLite);
            }
        public void addMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fLite)
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            peer.addMapListener(listener, oKey, fLite);
            }
        void super$addMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fLite)
            {
            super.addMapListener(listener, oKey, fLite);
            }
        public void clear()
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            peer.clear();
            }
        void super$clear()
            {
            super.clear();
            }
        public boolean containsKey(Object oKey)
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            return peer.containsKey(oKey);
            }
        boolean super$containsKey(Object oKey)
            {
            return super.containsKey(oKey);
            }
        public boolean containsValue(Object oValue)
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            return peer.containsValue(oValue);
            }
        boolean super$containsValue(Object oValue)
            {
            return super.containsValue(oValue);
            }
        public java.util.Set entrySet()
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            return peer.entrySet();
            }
        java.util.Set super$entrySet()
            {
            return super.entrySet();
            }
        public Object get(Object oKey)
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            return peer.get(oKey);
            }
        Object super$get(Object oKey)
            {
            return super.get(oKey);
            }
        public java.util.Map getMap()
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            return peer.getActualMap();
            }
        java.util.Map super$getMap()
            {
            return super.getMap();
            }
        public boolean isEmpty()
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            return peer.isEmpty();
            }
        boolean super$isEmpty()
            {
            return super.isEmpty();
            }
        public java.util.Set keySet()
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            return peer.keySet();
            }
        java.util.Set super$keySet()
            {
            return super.keySet();
            }
        public boolean lock(Object oKey)
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            return peer.lock(oKey);
            }
        boolean super$lock(Object oKey)
            {
            return super.lock(oKey);
            }
        public boolean lock(Object oKey, long cWait)
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            return peer.lock(oKey, cWait);
            }
        boolean super$lock(Object oKey, long cWait)
            {
            return super.lock(oKey, cWait);
            }
        public Object put(Object oKey, Object oValue)
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            return peer.put(oKey, oValue);
            }
        Object super$put(Object oKey, Object oValue)
            {
            return super.put(oKey, oValue);
            }
        public void putAll(java.util.Map map)
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            peer.putAll(map);
            }
        void super$putAll(java.util.Map map)
            {
            super.putAll(map);
            }
        public Object remove(Object oKey)
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            return peer.remove(oKey);
            }
        Object super$remove(Object oKey)
            {
            return super.remove(oKey);
            }
        public void removeMapListener(com.tangosol.util.MapListener listener)
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            peer.removeMapListener(listener);
            }
        void super$removeMapListener(com.tangosol.util.MapListener listener)
            {
            super.removeMapListener(listener);
            }
        public void removeMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter)
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            peer.removeMapListener(listener, filter);
            }
        void super$removeMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter)
            {
            super.removeMapListener(listener, filter);
            }
        public void removeMapListener(com.tangosol.util.MapListener listener, Object oKey)
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            peer.removeMapListener(listener, oKey);
            }
        void super$removeMapListener(com.tangosol.util.MapListener listener, Object oKey)
            {
            super.removeMapListener(listener, oKey);
            }
        public int size()
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            return peer.size();
            }
        int super$size()
            {
            return super.size();
            }
        public boolean unlock(Object oKey)
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            return peer.unlock(oKey);
            }
        boolean super$unlock(Object oKey)
            {
            return super.unlock(oKey);
            }
        public java.util.Collection values()
            {
            ConcurrentMap peer = __peer;
            if (peer == null) peer = __retrievePeer();
            return peer.values();
            }
        java.util.Collection super$values()
            {
            return super.values();
            }
        
        // interface com.tangosol.run.component.ComponentPeer
        public Object get_ComponentPeer()
            {
            return __retrievePeer();
            }
        }

    /* 
     *  Class sink_ConcurrentMap
     *
     * automatically generated "Sink" which
     * represents a footprint of the class
     *      com.tangosol.util.WrapperConcurrentMap
     * when used as a component callback by 
     *      Component.Util.ConcurrentMap
     */
    private static class sink_ConcurrentMap
           extends com.tangosol.run.component.CallbackSink
        {
        private jb_ConcurrentMap __peer;
        
        // this default (protected) constructor is used by sinks that extend this one
        protected sink_ConcurrentMap()
            {
            }
        
        // this (protected) constructor is used by the feed
        protected sink_ConcurrentMap(jb_ConcurrentMap feed)
            {
            super();
            __peer = feed;
            }
        
        // Retrieves the feed object for this sink
        public Object get_Feed()
            {
            return __peer;
            }
        
        // methods integrated and/or remoted
        public void addMapListener(com.tangosol.util.MapListener listener)
            {
            __peer.super$addMapListener(listener);
            }
        public void addMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, boolean fLite)
            {
            __peer.super$addMapListener(listener, filter, fLite);
            }
        public void addMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fLite)
            {
            __peer.super$addMapListener(listener, oKey, fLite);
            }
        public void clear()
            {
            __peer.super$clear();
            }
        public boolean containsKey(Object oKey)
            {
            return __peer.super$containsKey(oKey);
            }
        public boolean containsValue(Object oValue)
            {
            return __peer.super$containsValue(oValue);
            }
        public java.util.Set entrySet()
            {
            return __peer.super$entrySet();
            }
        public Object get(Object oKey)
            {
            return __peer.super$get(oKey);
            }
        public java.util.Map getMap()
            {
            return __peer.super$getMap();
            }
        public boolean isEmpty()
            {
            return __peer.super$isEmpty();
            }
        public java.util.Set keySet()
            {
            return __peer.super$keySet();
            }
        public boolean lock(Object oKey)
            {
            return __peer.super$lock(oKey);
            }
        public boolean lock(Object oKey, long cWait)
            {
            return __peer.super$lock(oKey, cWait);
            }
        public Object put(Object oKey, Object oValue)
            {
            return __peer.super$put(oKey, oValue);
            }
        public void putAll(java.util.Map map)
            {
            __peer.super$putAll(map);
            }
        public Object remove(Object oKey)
            {
            return __peer.super$remove(oKey);
            }
        public void removeMapListener(com.tangosol.util.MapListener listener)
            {
            __peer.super$removeMapListener(listener);
            }
        public void removeMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter)
            {
            __peer.super$removeMapListener(listener, filter);
            }
        public void removeMapListener(com.tangosol.util.MapListener listener, Object oKey)
            {
            __peer.super$removeMapListener(listener, oKey);
            }
        public int size()
            {
            return __peer.super$size();
            }
        public boolean unlock(Object oKey)
            {
            return __peer.super$unlock(oKey);
            }
        public java.util.Collection values()
            {
            return __peer.super$values();
            }
        }
    }
