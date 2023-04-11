
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.model.localModel.ConnectionManagerModel

package com.tangosol.coherence.component.net.management.model.localModel;

import com.tangosol.coherence.component.util.Queue;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.GrpcAcceptor;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.HttpAcceptor;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.MemcachedAcceptor;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor;
import com.tangosol.io.MultiBufferWriteBuffer;
import com.tangosol.net.internal.SocketAddressHelper;
import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The status throughput and connection information for Proxy hosts.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ConnectionManagerModel
        extends    com.tangosol.coherence.component.net.management.model.LocalModel
    {
    // ---- Fields declarations ----
    
    /**
     * Property _Acceptor
     *
     */
    private com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor __m__Acceptor;
    
    /**
     * Property _CreateTime
     *
     */
    private transient java.util.Date __m__CreateTime;
    
    /**
     * Property HostIP
     *
     * The IP address and port of the Proxy host.
     */
    private transient String __m_HostIP;
    
    /**
     * Property MessagingDebug
     *
     * Debug flag.  When true and the node's logging level is 6 or higher, sent
     * and received messages will be logged for all the connections under this
     * service.
     */
    private transient boolean __m_MessagingDebug;
    
    // Default constructor
    public ConnectionManagerModel()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ConnectionManagerModel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.management.model.localModel.ConnectionManagerModel();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/model/localModel/ConnectionManagerModel".replace('/', '.'));
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
    
    // Accessor for the property "_Acceptor"
    /**
     * Getter for property _Acceptor.<p>
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor get_Acceptor()
        {
        return __m__Acceptor;
        }
    
    // Accessor for the property "_CreateTime"
    /**
     * Getter for property _CreateTime.<p>
     */
    public java.util.Date get_CreateTime()
        {
        return __m__CreateTime;
        }
    
    // Accessor for the property "AverageRequestTime"
    /**
     * Getter for property AverageRequestTime.<p>
    * The average processing time in milliseconds for HTTP requests.
     */
    public float getAverageRequestTime()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof HttpAcceptor)
            {
            return getHttpStatsFloat((HttpAcceptor) acceptor, "getAverageRequestTime");
            }
        else
            {
            return -1.0f;
            }
        }
    
    // Accessor for the property "ConnectionCount"
    /**
     * Getter for property ConnectionCount.<p>
    * The number of client connections.
     */
    public int getConnectionCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        // import java.util.Set;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof TcpAcceptor)
            {
            Set setConn  = ((TcpAcceptor) acceptor).getConnectionSet();
            return setConn == null ? 0 : setConn.size();
            }
        else
            {
            return -1;
            }
        }
    
    // Accessor for the property "HostIP"
    /**
     * Getter for property HostIP.<p>
    * The IP address and port of the Proxy host.
     */
    public String getHostIP()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        // import com.tangosol.net.internal.SocketAddressHelper;
        // import java.net.SocketAddress;
           
        String sAddr = __m_HostIP;
        if (sAddr == null)
            {
            Acceptor acceptor = get_Acceptor();
            if (acceptor instanceof TcpAcceptor)
                {
                SocketAddress address = ((TcpAcceptor) acceptor).getLocalAddress();
                if (address != null)
                    {
                    sAddr = SocketAddressHelper.toString(address);
                    setHostIP(sAddr);
                    }
                }
            else if (acceptor instanceof HttpAcceptor)
                {
                HttpAcceptor httpAcceptor = (HttpAcceptor) acceptor;
                return httpAcceptor.getLocalAddress() + ':' + httpAcceptor.getListenPort();
                } 
            }
        
        return sAddr;
        }
    
    // Accessor for the property "HttpServerType"
    /**
     * Getter for property HttpServerType.<p>
    * The type of HTTP server or n/a if not using HTTP protocol.
     */
    public String getHttpServerType()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        
        Object   oServer  = null;
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof HttpAcceptor)
            {
            oServer = ((HttpAcceptor) acceptor).getHttpServer();
            }
        
        return oServer == null ? "n/a" : oServer.getClass().getName();
        }
    
    /**
     * Return the value of a statistics from the HttpServer from a HttpAcceptor
    * as an Object.
    * This must be done via reflection as coherence-rest is not guaranteed to
    * be in the classpath.
     */
    protected Object getHttpStats(com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.HttpAcceptor httpAcceptor, String sMethod)
        {
        return getHttpStats(httpAcceptor, sMethod, new Object[0]);
        }
    
    /**
     * Return the value of a statistics from the HttpServer from a HttpAcceptor
    * as an Object.
    * This must be done via reflection as coherence-rest is not guaranteed to
    * be in the classpath.
     */
    protected Object getHttpStats(com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.HttpAcceptor httpAcceptor, String sMethod, Object[] oaArgs)
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ClassHelper;
        
        Object oServer = httpAcceptor.getHttpServer();
        _assert(oServer != null);
        
        try
            {
            return ClassHelper.invoke(oServer, sMethod, oaArgs);
            }
        catch (Exception e)
            {
            _trace("Unable to call method " + sMethod + " on " + oServer,1);
            throw Base.ensureRuntimeException(e);
            }
        }
    
    /**
     * Return the value of a statistics from the HttpServer from a HttpAcceptor
    * as a Float.
     */
    protected float getHttpStatsFloat(com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.HttpAcceptor httpAcceptor, String sMethod)
        {
        Float fValue = (Float) getHttpStats(httpAcceptor, sMethod);
        return fValue instanceof Float ? fValue.floatValue() : 0L;
        }
    
    /**
     * Return the value of a statistics from the HttpServer from a HttpAcceptor
    * as an Long.
     */
    protected long getHttpStatsLong(com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.HttpAcceptor httpAcceptor, String sMethod)
        {
        Long lValue = (Long) getHttpStats(httpAcceptor, sMethod);
        return lValue instanceof Long ? lValue.longValue() : 0L;
        }
    
    /**
     * Return the value of a status count from the HttpServer from a
    * HttpAcceptor as an Long.
     */
    protected long getHttpStatusCount(com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.HttpAcceptor httpAcceptor, int nPrefix)
        {
        Long lValue = (Long) getHttpStats(httpAcceptor, "getHttpStatusCount",
                                          new Object[] { Integer.valueOf(nPrefix) });
        return lValue instanceof Long ? lValue.longValue() : 0L;
        }
    
    // Accessor for the property "IncomingBufferPoolCapacity"
    /**
     * Getter for property IncomingBufferPoolCapacity.<p>
     */
    public long getIncomingBufferPoolCapacity()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor$BufferPool as com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.BufferPool;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof TcpAcceptor)
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.BufferPool pool = ((TcpAcceptor) acceptor).getBufferPoolIn();  
            return pool == null ? 0l : pool.getMaximumCapacity();
            }
        else
            {
            return -1L;
            }
        }
    
    // Accessor for the property "IncomingBufferPoolSize"
    /**
     * Getter for property IncomingBufferPoolSize.<p>
     */
    public long getIncomingBufferPoolSize()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor$BufferPool as com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.BufferPool;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof TcpAcceptor)
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.BufferPool pool = ((TcpAcceptor) acceptor).getBufferPoolIn();
            return pool == null ? 0l : pool.getSize() * pool.getBufferSize();
            }
        else
            {
            return -1L;
            }
        }
    
    // Accessor for the property "OutgoingBufferPoolCapacity"
    /**
     * Getter for property OutgoingBufferPoolCapacity.<p>
     */
    public long getOutgoingBufferPoolCapacity()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor$BufferPool as com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.BufferPool;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof TcpAcceptor)
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.BufferPool pool = ((TcpAcceptor) acceptor).getBufferPoolOut();
            return pool == null ? 0 : pool.getMaximumCapacity();
            }
        else
            {
            return -1L;
            }
        }
    
    // Accessor for the property "OutgoingBufferPoolSize"
    /**
     * Getter for property OutgoingBufferPoolSize.<p>
     */
    public long getOutgoingBufferPoolSize()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor$BufferPool as com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.BufferPool;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof TcpAcceptor)
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.BufferPool pool = ((TcpAcceptor) acceptor).getBufferPoolOut();  
            return pool == null ? 0l : pool.getSize() * pool.getBufferSize();
            }
        else
            {
            return -1L;
            }
        }
    
    // Accessor for the property "OutgoingByteBacklog"
    /**
     * Getter for property OutgoingByteBacklog.<p>
     */
    public long getOutgoingByteBacklog()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor$TcpConnection as com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection;
        // import Component.Util.Queue;
        // import com.tangosol.io.MultiBufferWriteBuffer;
        // import java.util.Iterator;
        // import java.util.Set;
        
        long cBacklog = 0l;
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof TcpAcceptor)
            {
            Set  setConn  = ((TcpAcceptor) acceptor).getConnectionSet();
            if (setConn != null)
                {
                for (Iterator iterConn = setConn.iterator(); iterConn.hasNext(); )
                    {
                    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection conn = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection) iterConn.next();
                    if (conn != null)
                        {
                        Queue q = conn.getOutgoingQueue();
                        for (Iterator iterBuff = q.iterator(); iterBuff.hasNext();)
                            {
                            MultiBufferWriteBuffer buff = (MultiBufferWriteBuffer) iterBuff.next();
                            cBacklog += buff.length();
                            }
                        }
                    }
                }
            }
        return cBacklog;
        }
    
    // Accessor for the property "OutgoingMessageBacklog"
    /**
     * Getter for property OutgoingMessageBacklog.<p>
     */
    public long getOutgoingMessageBacklog()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor$TcpConnection as com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection;
        // import java.util.Set;
        // import java.util.Iterator;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof TcpAcceptor)
            {
            Set setConn = ((TcpAcceptor) acceptor).getConnectionSet();
            if (setConn != null)
                {
                long cBacklog = 0l;
                for (Iterator iter = setConn.iterator(); iter.hasNext(); )
                    {
                    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection conn = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection) iter.next();
                    if (conn != null)
                        {
                        cBacklog += conn.getOutgoingQueue().size();
                        }
                    }
                return cBacklog;
                }
            }
        
        return 0l;
        }
    
    // Accessor for the property "Protocol"
    /**
     * Getter for property Protocol.<p>
    * Protocol associated with this ConnectionManagerMBean. Valid values are
    * tcp or http.
     */
    public String getProtocol()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.GrpcAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.MemcachedAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        
        Acceptor acceptor = get_Acceptor();
        
        return acceptor instanceof TcpAcceptor  ? "tcp" :
               (acceptor instanceof HttpAcceptor ? "http" :
                acceptor instanceof MemcachedAcceptor ? "memcached" :
                acceptor instanceof GrpcAcceptor ? "grpc" : "n/a");
        }
    
    // Accessor for the property "RequestsPerSecond"
    /**
     * Getter for property RequestsPerSecond.<p>
    * The number of HTTP requests per second since the statistics were reset.
     */
    public float getRequestsPerSecond()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof HttpAcceptor)
            {
            return getHttpStatsFloat((HttpAcceptor) acceptor, "getRequestsPerSecond");
            }
        else
            {
            return -1.0f;
            }
        }
    
    // Accessor for the property "ResponseCount1xx"
    /**
     * Getter for property ResponseCount1xx.<p>
    * The number of HTTP responses in the 100-199 range.
     */
    public long getResponseCount1xx()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof HttpAcceptor)
            {
            return getHttpStatusCount((HttpAcceptor) acceptor, 1);
            }
        else
            {
            return -1L;
            }
        }
    
    // Accessor for the property "ResponseCount2xx"
    /**
     * Getter for property ResponseCount2xx.<p>
    * The number of HTTP responses in the 200-299 range.
     */
    public long getResponseCount2xx()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof HttpAcceptor)
            {
            return getHttpStatusCount((HttpAcceptor) acceptor, 2);
            }
        else
            {
            return -1L;
            }
        }
    
    // Accessor for the property "ResponseCount3xx"
    /**
     * Getter for property ResponseCount3xx.<p>
    * The number of HTTP responses in the 300-399 range.
     */
    public long getResponseCount3xx()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof HttpAcceptor)
            {
            return getHttpStatusCount((HttpAcceptor) acceptor, 3);
            }
        else
            {
            return -1L;
            }
        }
    
    // Accessor for the property "ResponseCount4xx"
    /**
     * Getter for property ResponseCount4xx.<p>
    * The number of HTTP responses in the 400-499 range.
     */
    public long getResponseCount4xx()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof HttpAcceptor)
            {
            return getHttpStatusCount((HttpAcceptor) acceptor, 4);
            }
        else
            {
            return -1L;
            }
        }
    
    // Accessor for the property "ResponseCount5xx"
    /**
     * Getter for property ResponseCount5xx.<p>
    * The number of HTTP responses in the 500-599 range.
     */
    public long getResponseCount5xx()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof HttpAcceptor)
            {
            return getHttpStatusCount((HttpAcceptor) acceptor, 5);
            }
        else
            {
            return -1L;
            }
        }
    
    // Accessor for the property "TotalBytesReceived"
    /**
     * Getter for property TotalBytesReceived.<p>
     */
    public long getTotalBytesReceived()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof TcpAcceptor)
            {
            return ((TcpAcceptor) acceptor).getStatsBytesReceived();
            }
        else
            {
            return -1;
            }
        }
    
    // Accessor for the property "TotalBytesSent"
    /**
     * Getter for property TotalBytesSent.<p>
     */
    public long getTotalBytesSent()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof TcpAcceptor)
            {
            return ((TcpAcceptor) acceptor).getStatsBytesSent();
            }
        else
            {
            return -1;
            }
        }
    
    // Accessor for the property "TotalErrorCount"
    /**
     * Getter for property TotalErrorCount.<p>
    * The number of HTTP requests that caused errors.
     */
    public long getTotalErrorCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof HttpAcceptor)
            {
            return getHttpStatsLong((HttpAcceptor) acceptor, "getErrorCount");
            }
        else
            {
            return -1L;
            }
        }
    
    // Accessor for the property "TotalMessagesReceived"
    /**
     * Getter for property TotalMessagesReceived.<p>
     */
    public long getTotalMessagesReceived()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof TcpAcceptor)
            {
            return ((TcpAcceptor) acceptor).getStatsReceived();
            }
        else
            {
            return -1;
            }
        }
    
    // Accessor for the property "TotalMessagesSent"
    /**
     * Getter for property TotalMessagesSent.<p>
     */
    public long getTotalMessagesSent()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof TcpAcceptor)
            {
            return ((TcpAcceptor) acceptor).getStatsSent();
            }
        else
            {
            return -1L;
            }
        }
    
    // Accessor for the property "TotalRequestCount"
    /**
     * Getter for property TotalRequestCount.<p>
    * The number of requests serviced since the HTTP server was started or the
    * statistics were reset.
     */
    public long getTotalRequestCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof HttpAcceptor)
            {
            return getHttpStatsLong((HttpAcceptor) acceptor, "getRequestCount");
            }
        else
            {
            return -1L;
            }
        }
    
    // Accessor for the property "UnauthorizedConnectionAttempts"
    /**
     * Getter for property UnauthorizedConnectionAttempts.<p>
    * The number of connection attempts from unauthorized hosts.
     */
    public long getUnauthorizedConnectionAttempts()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        
        Acceptor acceptor = get_Acceptor();
        if (acceptor instanceof TcpAcceptor)
            {
            return ((TcpAcceptor) acceptor).getStatsUnauthorizedConnectionAttempts();
            }
        else
            {
            return -1;
            }
        }
    
    // Accessor for the property "MessagingDebug"
    /**
     * Getter for property MessagingDebug.<p>
    * Debug flag.  When true and the node's logging level is 6 or higher, sent
    * and received messages will be logged for all the connections under this
    * service.
     */
    public boolean isMessagingDebug()
        {
        return get_Acceptor().isDEBUG();
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void readExternal(java.io.DataInput in)
            throws java.io.IOException
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
        // import java.util.Map;
        
        super.readExternal(in);
        
        Map mapSnapshot = get_SnapshotMap();
        
        mapSnapshot.put("ConnectionCount", Integer.valueOf(com.tangosol.util.ExternalizableHelper.readInt(in)));
        mapSnapshot.put("HostIP", com.tangosol.util.ExternalizableHelper.readUTF(in));
        mapSnapshot.put("IncomingBufferPoolCapacity", Long.valueOf(com.tangosol.util.ExternalizableHelper.readLong(in)));
        mapSnapshot.put("IncomingBufferPoolSize", Long.valueOf(com.tangosol.util.ExternalizableHelper.readLong(in)));
        mapSnapshot.put("OutgoingBufferPoolCapacity", Long.valueOf(com.tangosol.util.ExternalizableHelper.readLong(in)));
        mapSnapshot.put("OutgoingBufferPoolSize", Long.valueOf(com.tangosol.util.ExternalizableHelper.readLong(in)));
        mapSnapshot.put("OutgoingByteBacklog", Long.valueOf(com.tangosol.util.ExternalizableHelper.readLong(in)));
        mapSnapshot.put("OutgoingMessageBacklog", Long.valueOf(com.tangosol.util.ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TotalBytesReceived", Long.valueOf(com.tangosol.util.ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TotalBytesSent", Long.valueOf(com.tangosol.util.ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TotalMessagesReceived", Long.valueOf(com.tangosol.util.ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TotalMessagesSent", Long.valueOf(com.tangosol.util.ExternalizableHelper.readLong(in)));
        mapSnapshot.put("UnauthorizedConnectionAttempts", Long.valueOf(com.tangosol.util.ExternalizableHelper.readLong(in)));
        
        if (com.tangosol.util.ExternalizableHelper.isVersionCompatible(in, 12, 2, 1, 1, 0))
            {
            mapSnapshot.put("AverageRequestTime", Float.valueOf(in.readFloat()));
            mapSnapshot.put("HttpServerType", com.tangosol.util.ExternalizableHelper.readUTF(in));
            mapSnapshot.put("Protocol", com.tangosol.util.ExternalizableHelper.readUTF(in));
            mapSnapshot.put("ResponseCount1xx", Long.valueOf(com.tangosol.util.ExternalizableHelper.readLong(in)));
            mapSnapshot.put("ResponseCount2xx", Long.valueOf(com.tangosol.util.ExternalizableHelper.readLong(in)));
            mapSnapshot.put("ResponseCount3xx", Long.valueOf(com.tangosol.util.ExternalizableHelper.readLong(in)));
            mapSnapshot.put("ResponseCount4xx", Long.valueOf(com.tangosol.util.ExternalizableHelper.readLong(in)));
            mapSnapshot.put("ResponseCount5xx", Long.valueOf(com.tangosol.util.ExternalizableHelper.readLong(in)));
            mapSnapshot.put("RequestsPerSecond", Float.valueOf(in.readFloat()));
            mapSnapshot.put("TotalErrorCount", Long.valueOf(com.tangosol.util.ExternalizableHelper.readLong(in)));
            mapSnapshot.put("TotalRequestCount", Long.valueOf(com.tangosol.util.ExternalizableHelper.readLong(in)));
            }
        else
            {
            mapSnapshot.put("AverageRequestTime", Float.valueOf(-1.0f));
            mapSnapshot.put("HttpServerType", "n/a");
            mapSnapshot.put("Protocol", "n/a");
            mapSnapshot.put("ResponseCount1xx", Long.valueOf(-1L));
            mapSnapshot.put("ResponseCount2xx", Long.valueOf(-1L));
            mapSnapshot.put("ResponseCount3xx", Long.valueOf(-1L));
            mapSnapshot.put("ResponseCount4xx", Long.valueOf(-1L));
            mapSnapshot.put("ResponseCount5xx", Long.valueOf(-1L));
            mapSnapshot.put("RequestsPerSecond", Float.valueOf(-1.0f));
            mapSnapshot.put("TotalErrorCount", Long.valueOf(-1L));
            mapSnapshot.put("TotalRequestCount", Long.valueOf(-1L));
            }
        }
    
    public void resetStatistics()
        {
        get_Acceptor().resetStats();
        }
    
    // Accessor for the property "_Acceptor"
    /**
     * Setter for property _Acceptor.<p>
     */
    public void set_Acceptor(com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor acceptor)
        {
        __m__Acceptor = acceptor;
        }
    
    // Accessor for the property "_CreateTime"
    /**
     * Setter for property _CreateTime.<p>
     */
    public void set_CreateTime(java.util.Date p_CreateTime)
        {
        __m__CreateTime = p_CreateTime;
        }
    
    // Accessor for the property "HostIP"
    /**
     * Setter for property HostIP.<p>
    * The IP address and port of the Proxy host.
     */
    protected void setHostIP(String sAddress)
        {
        __m_HostIP = sAddress;
        }
    
    // Accessor for the property "MessagingDebug"
    /**
     * Setter for property MessagingDebug.<p>
    * Debug flag.  When true and the node's logging level is 6 or higher, sent
    * and received messages will be logged for all the connections under this
    * service.
     */
    public void setMessagingDebug(boolean fMessagingDebug)
        {
        get_Acceptor().setDEBUG(fMessagingDebug);
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void writeExternal(java.io.DataOutput out)
            throws java.io.IOException
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
        // import java.util.Map;
        
        super.writeExternal(out);
        
        Map mapSnapshot = get_SnapshotMap();
        
        com.tangosol.util.ExternalizableHelper.writeInt (out, getConnectionCount());
        com.tangosol.util.ExternalizableHelper.writeUTF (out, getHostIP());
        com.tangosol.util.ExternalizableHelper.writeLong(out, getIncomingBufferPoolCapacity());
        com.tangosol.util.ExternalizableHelper.writeLong(out, getIncomingBufferPoolSize());
        com.tangosol.util.ExternalizableHelper.writeLong(out, getOutgoingBufferPoolCapacity());
        com.tangosol.util.ExternalizableHelper.writeLong(out, getOutgoingBufferPoolSize());
        com.tangosol.util.ExternalizableHelper.writeLong(out, getOutgoingByteBacklog());
        com.tangosol.util.ExternalizableHelper.writeLong(out, getOutgoingMessageBacklog());
        com.tangosol.util.ExternalizableHelper.writeLong(out, getTotalBytesReceived());
        com.tangosol.util.ExternalizableHelper.writeLong(out, getTotalBytesSent());
        com.tangosol.util.ExternalizableHelper.writeLong(out, getTotalMessagesReceived());
        com.tangosol.util.ExternalizableHelper.writeLong(out, getTotalMessagesSent());
        com.tangosol.util.ExternalizableHelper.writeLong(out, getUnauthorizedConnectionAttempts());
        
        if (com.tangosol.util.ExternalizableHelper.isVersionCompatible(out, 12, 2, 1, 1, 0))
            {
            out.writeFloat(getAverageRequestTime());
            com.tangosol.util.ExternalizableHelper.writeUTF (out, getHttpServerType());
            com.tangosol.util.ExternalizableHelper.writeUTF (out, getProtocol());
            com.tangosol.util.ExternalizableHelper.writeLong(out, getResponseCount1xx());
            com.tangosol.util.ExternalizableHelper.writeLong(out, getResponseCount2xx());
            com.tangosol.util.ExternalizableHelper.writeLong(out, getResponseCount3xx());
            com.tangosol.util.ExternalizableHelper.writeLong(out, getResponseCount4xx());
            com.tangosol.util.ExternalizableHelper.writeLong(out, getResponseCount5xx());
            out.writeFloat(getRequestsPerSecond());
            com.tangosol.util.ExternalizableHelper.writeLong(out, getTotalErrorCount());
            com.tangosol.util.ExternalizableHelper.writeLong(out, getTotalRequestCount());
            }
        }
    }
