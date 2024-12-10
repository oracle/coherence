
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.model.localModel.ConnectionModel

package com.tangosol.coherence.component.net.management.model.localModel;

import com.tangosol.coherence.component.util.Queue;
import com.tangosol.io.MultiBufferWriteBuffer;
import com.tangosol.net.Member;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

/**
 * The status and usage information for the remote client connection.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ConnectionModel
        extends    com.tangosol.coherence.component.net.management.model.LocalModel
    {
    // ---- Fields declarations ----
    
    /**
     * Property _TcpConnection
     *
     */
    private com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection __m__TcpConnection;
    
    /**
     * Property MessagingDebug
     *
     * Debug flag.  When true and the node's logging level is 6 or higher, sent
     * and received messages will be logged for this connection.   If
     * MessagingDebug is set to true in the ConnectionManager MBean, then
     * messages will be logged regardless of this setting.
     */
    private transient boolean __m_MessagingDebug;
    
    // Default constructor
    public ConnectionModel()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ConnectionModel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.management.model.localModel.ConnectionModel();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/model/localModel/ConnectionModel".replace('/', '.'));
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
    
    public void closeConnection()
        {
        checkReadOnly("closeConnection");
        get_TcpConnection().close(true, null);
        }
    
    // Accessor for the property "_TcpConnection"
    /**
     * Getter for property _TcpConnection.<p>
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection get_TcpConnection()
        {
        return __m__TcpConnection;
        }
    
    // Accessor for the property "ClientAddress"
    /**
     * Getter for property ClientAddress.<p>
    * The client member's address.
     */
    public String getClientAddress()
        {
        // import com.tangosol.net.Member;
        
        Member member = get_TcpConnection().getMember();
        return member == null ? canonicalString(null) : member.getAddress().getHostAddress();
        }
    
    // Accessor for the property "ClientProcessName"
    /**
     * Getter for property ClientProcessName.<p>
    * The client's process name.
     */
    public String getClientProcessName()
        {
        // import com.tangosol.net.Member;
        
        Member member = get_TcpConnection().getMember();
        return member == null ? canonicalString(null) : member.getProcessName();
        }
    
    // Accessor for the property "ClientRole"
    /**
     * Getter for property ClientRole.<p>
    * The client's member role.
     */
    public String getClientRole()
        {
        // import com.tangosol.net.Member;
        
        Member member = get_TcpConnection().getMember();
        return member == null ? canonicalString(null) : member.getRoleName();
        }
    
    // Accessor for the property "ConnectionTimeMillis"
    /**
     * Getter for property ConnectionTimeMillis.<p>
    * The number of milliseconds that the client has been connected.
     */
    public long getConnectionTimeMillis()
        {
        return System.currentTimeMillis() - get_TcpConnection().getConnectTimeMillis();
        }
    
    // Accessor for the property "Member"
    /**
     * Getter for property Member.<p>
    * Member information of the client connection.
     */
    public String getMember()
        {
        // import com.tangosol.net.Member;
        
        Member member = get_TcpConnection().getMember();
        return member == null ? canonicalString(null) : member.toString();
        }
    
    // Accessor for the property "OutgoingByteBacklog"
    /**
     * Getter for property OutgoingByteBacklog.<p>
     */
    public long getOutgoingByteBacklog()
        {
        // import Component.Util.Queue;
        // import com.tangosol.io.MultiBufferWriteBuffer;
        // import java.util.Iterator;
        
        Queue queue    = get_TcpConnection().getOutgoingQueue();
        long  cBacklog = 0;
        for (Iterator iter = queue.iterator(); iter.hasNext();)
            {
            MultiBufferWriteBuffer buffer = (MultiBufferWriteBuffer) iter.next();
            cBacklog += buffer.length();
            }
        return cBacklog;
        }
    
    // Accessor for the property "OutgoingMessageBacklog"
    /**
     * Getter for property OutgoingMessageBacklog.<p>
     */
    public int getOutgoingMessageBacklog()
        {
        // import Component.Util.Queue;
        
        Queue queue = get_TcpConnection().getOutgoingQueue();
        return queue == null ? 0 : queue.size();
        }
    
    // Accessor for the property "RemoteAddress"
    /**
     * Getter for property RemoteAddress.<p>
     */
    public String getRemoteAddress()
        {
        // import java.net.Socket;
        // import java.net.InetAddress;
        
        Socket socket = get_TcpConnection().getSocket();
        if (socket == null)
            {
            return null;
            }
        
        InetAddress address = socket.getInetAddress();
        return address == null ? null : address.getHostAddress();
        }
    
    // Accessor for the property "RemotePort"
    /**
     * Getter for property RemotePort.<p>
     */
    public int getRemotePort()
        {
        // import java.net.Socket;
        
        Socket socket = get_TcpConnection().getSocket();
        return socket == null ? 0 : socket.getPort();
        }
    
    // Accessor for the property "Timestamp"
    /**
     * Getter for property Timestamp.<p>
     */
    public java.util.Date getTimestamp()
        {
        // import java.util.Date;
        
        return new Date(get_TcpConnection().getConnectTimeMillis());
        }
    
    // Accessor for the property "TotalBytesReceived"
    /**
     * Getter for property TotalBytesReceived.<p>
     */
    public long getTotalBytesReceived()
        {
        return get_TcpConnection().getStatsBytesReceived();
        }
    
    // Accessor for the property "TotalBytesSent"
    /**
     * Getter for property TotalBytesSent.<p>
     */
    public long getTotalBytesSent()
        {
        return get_TcpConnection().getStatsBytesSent();
        }
    
    // Accessor for the property "TotalMessagesReceived"
    /**
     * Getter for property TotalMessagesReceived.<p>
     */
    public long getTotalMessagesReceived()
        {
        return get_TcpConnection().getStatsReceived();
        }
    
    // Accessor for the property "TotalMessagesSent"
    /**
     * Getter for property TotalMessagesSent.<p>
     */
    public long getTotalMessagesSent()
        {
        return get_TcpConnection().getStatsSent();
        }
    
    // Accessor for the property "UUID"
    /**
     * Getter for property UUID.<p>
     */
    public String getUUID()
        {
        return String.valueOf(get_TcpConnection().getId());
        }
    
    // Accessor for the property "MessagingDebug"
    /**
     * Getter for property MessagingDebug.<p>
    * Debug flag.  When true and the node's logging level is 6 or higher, sent
    * and received messages will be logged for this connection.   If
    * MessagingDebug is set to true in the ConnectionManager MBean, then
    * messages will be logged regardless of this setting.
     */
    public boolean isMessagingDebug()
        {
        return get_TcpConnection().isMessagingDebug();
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void readExternal(java.io.DataInput in)
            throws java.io.IOException
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ExternalizableHelper;
        // import java.util.Map;
        // import java.util.Date;
        
        super.readExternal(in);
        
        Map mapSnapshot = get_SnapshotMap();
        
        mapSnapshot.put("ConnectionTimeMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("Member", ExternalizableHelper.readUTF(in));
        mapSnapshot.put("OutgoingByteBacklog", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("OutgoingMessageBacklog", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("RemoteAddress", ExternalizableHelper.readUTF(in));
        mapSnapshot.put("RemotePort", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("Timestamp", new Date(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TotalBytesReceived", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TotalBytesSent", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TotalMessagesReceived", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TotalMessagesSent", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("UUID", ExternalizableHelper.readUTF(in));
        
        if (ExternalizableHelper.isVersionCompatible(in, 14, 1, 1, 0, 4))
            {
            mapSnapshot.put("ClientAddress", ExternalizableHelper.readUTF(in));
            mapSnapshot.put("ClientProcessName", ExternalizableHelper.readUTF(in));
            mapSnapshot.put("ClientRole", ExternalizableHelper.readUTF(in));
            }
        }
    
    public void resetStatistics()
        {
        get_TcpConnection().resetStats();
        }
    
    // Accessor for the property "_TcpConnection"
    /**
     * Setter for property _TcpConnection.<p>
     */
    public void set_TcpConnection(com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection connection)
        {
        __m__TcpConnection = connection;
        }
    
    // Accessor for the property "MessagingDebug"
    /**
     * Setter for property MessagingDebug.<p>
    * Debug flag.  When true and the node's logging level is 6 or higher, sent
    * and received messages will be logged for this connection.   If
    * MessagingDebug is set to true in the ConnectionManager MBean, then
    * messages will be logged regardless of this setting.
     */
    public void setMessagingDebug(boolean fMessageDebug)
        {
        get_TcpConnection().setMessagingDebug(fMessageDebug);
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void writeExternal(java.io.DataOutput out)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        
        super.writeExternal(out);
        
        ExternalizableHelper.writeLong(out, getConnectionTimeMillis());
        ExternalizableHelper.writeUTF (out, getMember());
        ExternalizableHelper.writeLong(out, getOutgoingByteBacklog());
        ExternalizableHelper.writeInt (out, getOutgoingMessageBacklog());
        ExternalizableHelper.writeUTF (out, getRemoteAddress());
        ExternalizableHelper.writeInt (out, getRemotePort());
        ExternalizableHelper.writeLong(out, getTimestamp().getTime());
        ExternalizableHelper.writeLong(out, getTotalBytesReceived());
        ExternalizableHelper.writeLong(out, getTotalBytesSent());
        ExternalizableHelper.writeLong(out, getTotalMessagesReceived());
        ExternalizableHelper.writeLong(out, getTotalMessagesSent());
        ExternalizableHelper.writeUTF (out, getUUID());
        
        // added in 14.1.1.0.4
        if (ExternalizableHelper.isVersionCompatible(out, 14, 1, 1, 0, 4))
            {
            ExternalizableHelper.writeUTF (out, getClientAddress());
            ExternalizableHelper.writeUTF (out, getClientProcessName());
            ExternalizableHelper.writeUTF (out, getClientRole());
            }
        }
    }
