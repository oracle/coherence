
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.TcpRing

package com.tangosol.coherence.component.net;

import com.tangosol.coherence.component.net.Member;
import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.net.Sockets;
import com.tangosol.net.SocketOptions;
import com.tangosol.util.Base;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This daemon maintains TcpRing connections with other Cluster members to
 * quickly detect member departure.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class TcpRing
        extends    com.tangosol.coherence.component.Net
    {
    // ---- Fields declarations ----
    
    /**
     * Property Buddies
     *
     * Map of buddies being monitored.  This is a map keyed by Member, where
     * the value is the corresponding MemberMonitor.
     */
    private java.util.Map __m_Buddies;
    
    /**
     * Property Buffer
     *
     * A scratch ByteBuffer used for all IO.
     */
    private transient java.nio.ByteBuffer __m_Buffer;
    
    /**
     * Property InboundConnectionCount
     *
     * The number of inbound connections, i.e. the number of connections
     * monitoring this member.
     */
    private int __m_InboundConnectionCount;
    
    /**
     * Property RedundancyLevel
     *
     * The number of suplemental connections to maintain for each buddy. 
     * Maintaining multiple connections decreases the chance of a false
     * positive death detection due to a spuratic TCP failure in that a member
     * is only considered dead if all connections are simultaneously down.
     */
    private int __m_RedundancyLevel;
    
    /**
     * Property Selector
     *
     * The Selector used to select from the various SelectableChannel objects
     * created by the TCP ring listener.
     */
    private transient java.nio.channels.Selector __m_Selector;
    
    /**
     * Property ServerSocketChannel
     *
     * The TCP ring listener's server socket channel.
     */
    private transient java.nio.channels.ServerSocketChannel __m_ServerSocketChannel;
    
    /**
     * Property SocketOptions
     *
     * Specifies the SocketOptions to be applied to each socket.
     */
    private transient com.tangosol.net.SocketOptions __m_SocketOptions;
    
    /**
     * Property SocketProvider
     *
     * The SocketProvider used to create the Sockets used by TcpRing.
     */
    private com.oracle.coherence.common.net.SocketProvider __m_SocketProvider;
    
    /**
     * Property StatsFailures
     *
     * Statistics: total number of failures.
     */
    private transient long __m_StatsFailures;
    
    /**
     * Property StatsPings
     *
     * Statistics: total number of pings.
     */
    private transient long __m_StatsPings;
    
    // Initializing constructor
    public TcpRing(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/TcpRing".replace('/', '.'));
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
     * Close all connections maintained by the TcpRing.
     */
    public void close()
        {
        // import java.io.IOException;
        // import java.nio.channels.ClosedSelectorException;
        // import java.nio.channels.SelectionKey;
        // import java.nio.channels.Selector;
        // import java.nio.channels.ServerSocketChannel;
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        
        Selector selector = getSelector();
        selector.wakeup();
        
        synchronized (selector)
            {
            if (selector.isOpen())
                {
                try
                    {
                    for (Iterator iter = selector.keys().iterator(); iter.hasNext(); )
                        {
                        SelectionKey key = (SelectionKey) iter.next();
                        try
                            {
                            if (key.isValid() && key.channel().isOpen())
                                {
                                key.channel().close();
                                }
                            }
                        catch (IOException e) {}
                        }
                    }
                catch (ClosedSelectorException cse) {}
        
                try
                    {
                    selector.close();
                    }
                catch (IOException e) {}
                }
            }
        
        ServerSocketChannel server = getServerSocketChannel();
        if (server != null)
            {
            try
                {
                server.close();
                }
            catch (IOException e) {}
            }
        }
    
    /**
     * Disconnect the channel associated with the specified key.
     */
    protected TcpRing.MemberMonitor close(java.nio.channels.SelectionKey key)
        {
        // import java.io.IOException;
        // import java.util.Set;
        
        if (key != null)
            {
            TcpRing.MemberMonitor monitor = (TcpRing.MemberMonitor) key.attachment();
            if (monitor != null)
                {
                Set setKeys = monitor.getKeys();
                if (!setKeys.remove(key))
                    {
                    monitor.setPendingConnections(monitor.getPendingConnections() - 1);
                    }
                }
        
            try
                {
                if (key.channel().isOpen())
                    {
                    key.channel().close();
                    }
                }
            catch (IOException e)
                {
                onDisconnectException(e, key);
                }
        
            return monitor;
            }
        
        return null;
        }
    
    /**
     * Connect to the specified cluster member.
    * 
     */
    protected void connect(Member member)
        {
        // import com.tangosol.net.SocketOptions;
        // import com.oracle.coherence.common.net.Sockets;
        // import java.io.IOException;
        // import java.net.InetSocketAddress;
        // import java.nio.channels.SelectionKey;
        // import java.nio.channels.SocketChannel;
        // import java.util.Map;
        
        
        Map            mapBuddies = getBuddies();
        TcpRing.MemberMonitor monitor    = (TcpRing.MemberMonitor) mapBuddies.get(member);
        
        if (monitor == null)
            {
            monitor = new TcpRing.MemberMonitor();    
            monitor.setMember(member);
            mapBuddies.put(member, monitor);
            }
        
        int cNew = (1 + getRedundancyLevel()) - (monitor.getKeys().size() + monitor.getPendingConnections());
        
        for (int i = 0; i < cNew; ++i)
            {
            SelectionKey key = null;
            try
                {
                SocketChannel channel = getSocketProvider().openSocketChannel();
        
                Sockets.configureBlocking(channel, false);
                getSocketOptions().apply(channel.socket());
        
                // register CONNECT interest, and attach member
                // Note: we don't register OP_READ at the same time as there appears to be
                // an issue (at least on OS X), where OP_READ truns OP_CONNECT and we won't
                // get the CONNECT event for a buddy which dies while we're connecting
                key = channel.register(ensureSelector(channel), SelectionKey.OP_CONNECT, monitor);
        
                monitor.setPendingConnections(monitor.getPendingConnections() + 1);
                if (channel.connect(new InetSocketAddress(member.getAddress(), member.getTcpRingPort())))
                    {
                    // uncommon but connect can succeed immediately
                    onConnect(key);
                    }
                else
                    {
                    // common case
                    _trace("TcpRing connecting to " + member, 6);
                    }
                }
            catch (IOException e)
                {
                onDisconnect(key, e);
                }
            }
        }
    
    /**
     * Disconnect all channels.
     */
    protected void disconnectAll()
        {
        // import java.nio.channels.SelectionKey;
        // import java.util.Iterator;
        
        // attempt an orderly disconnect on all valid registered channels
        for (Iterator iter = getSelector().keys().iterator(); iter.hasNext(); )
            {
            SelectionKey key = (SelectionKey) iter.next();
            if (key.isValid())
                {
                close(key);
                }
            }
        }
    
    /**
     * Return the Selector, creating it if necessary.
     */
    protected java.nio.channels.Selector ensureSelector(java.nio.channels.SelectableChannel channel)
            throws java.io.IOException
        {
        // import java.nio.channels.Selector;
        
        Selector selector = getSelector();
        
        if (selector == null)
            {
            selector = channel.provider().openSelector();
            setSelector(selector);
            }
        
        return selector;
        }
    
    /**
     * Ensure that the appropriate TcpRing connection is maintained.
     */
    public void ensureTopology(java.util.Set setConnect)
        {
        // import Component.Net.Member;
        // import java.nio.channels.SelectionKey;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        // import java.util.Set;
        
        // ensure we have appropriate connections
        Map mapBuddies = getBuddies();
        
        // drop existing connections as needed
        for (Iterator iter = mapBuddies.entrySet().iterator(); iter.hasNext(); )
            {
            java.util.Map.Entry  entry  = (java.util.Map.Entry) iter.next();
            Member member = (Member) entry.getKey();
        
            if (!setConnect.remove(member))
                {
                // not in the connect set; disconnect from this buddy
                iter.remove();
        
                // drop connections
                TcpRing.MemberMonitor monitor = (TcpRing.MemberMonitor) entry.getValue();
                Set            setKeys = monitor.getKeys();
                if (!setKeys.isEmpty())
                    {
                    _trace("TcpRing disconnected from " + member + " to maintain ring", 3);
                    for (Iterator iterKey = setKeys.iterator(); iterKey.hasNext(); )
                        {
                        SelectionKey key = (SelectionKey) iterKey.next();
                        if (key.isValid())
                            {
                            close(key);
                            }
                        }
                    }
                }
            }
        
        // add new buddies if needed
        for (Iterator iter = setConnect.iterator(); iter.hasNext(); )
            {
            connect((Member) iter.next());
            }
        }
    
    public String formatStats()
        {
        return "Pings=" + getStatsPings() + ", Failures=" + getStatsFailures();
        }
    
    // Accessor for the property "Buddies"
    /**
     * Getter for property Buddies.<p>
    * Map of buddies being monitored.  This is a map keyed by Member, where the
    * value is the corresponding MemberMonitor.
     */
    public java.util.Map getBuddies()
        {
        return __m_Buddies;
        }
    
    // Accessor for the property "Buffer"
    /**
     * Getter for property Buffer.<p>
    * A scratch ByteBuffer used for all IO.
     */
    public java.nio.ByteBuffer getBuffer()
        {
        return __m_Buffer;
        }
    
    // Accessor for the property "InboundConnectionCount"
    /**
     * Getter for property InboundConnectionCount.<p>
    * The number of inbound connections, i.e. the number of connections
    * monitoring this member.
     */
    public int getInboundConnectionCount()
        {
        return __m_InboundConnectionCount;
        }
    
    // Accessor for the property "RedundancyLevel"
    /**
     * Getter for property RedundancyLevel.<p>
    * The number of suplemental connections to maintain for each buddy. 
    * Maintaining multiple connections decreases the chance of a false positive
    * death detection due to a spuratic TCP failure in that a member is only
    * considered dead if all connections are simultaneously down.
     */
    public int getRedundancyLevel()
        {
        return __m_RedundancyLevel;
        }
    
    // Accessor for the property "Selector"
    /**
     * Getter for property Selector.<p>
    * The Selector used to select from the various SelectableChannel objects
    * created by the TCP ring listener.
     */
    public java.nio.channels.Selector getSelector()
        {
        return __m_Selector;
        }
    
    // Accessor for the property "ServerSocketChannel"
    /**
     * Getter for property ServerSocketChannel.<p>
    * The TCP ring listener's server socket channel.
     */
    public java.nio.channels.ServerSocketChannel getServerSocketChannel()
        {
        return __m_ServerSocketChannel;
        }
    
    // Accessor for the property "SocketOptions"
    /**
     * Getter for property SocketOptions.<p>
    * Specifies the SocketOptions to be applied to each socket.
     */
    public com.tangosol.net.SocketOptions getSocketOptions()
        {
        return __m_SocketOptions;
        }
    
    // Accessor for the property "SocketProvider"
    /**
     * Getter for property SocketProvider.<p>
    * The SocketProvider used to create the Sockets used by TcpRing.
     */
    public com.oracle.coherence.common.net.SocketProvider getSocketProvider()
        {
        return __m_SocketProvider;
        }
    
    // Accessor for the property "StatsFailures"
    /**
     * Getter for property StatsFailures.<p>
    * Statistics: total number of failures.
     */
    public long getStatsFailures()
        {
        return __m_StatsFailures;
        }
    
    // Accessor for the property "StatsPings"
    /**
     * Getter for property StatsPings.<p>
    * Statistics: total number of pings.
     */
    public long getStatsPings()
        {
        return __m_StatsPings;
        }
    
    /**
     * Ensure that an outgoing connection is still up.
     */
    protected void heartbeat(java.nio.channels.SelectionKey key)
        {
        // import Component.Net.Member;
        // import java.io.IOException;
        // import java.nio.ByteBuffer;
        // import java.nio.channels.SocketChannel;
        
        if (key != null)
            {
            SocketChannel channel = (SocketChannel) key.channel();
            if (!channel.isConnectionPending())
                {
                // issue a ping to keep the channel from being closed by certain TCP stack
                // implementations such as Linux IPChains.  Note that SO_KEEPALIVE despite
                // the name is not necessarily sufficent to do this, SO_KEEPALIVE is there
                // to detect the death of an idle connection, not to ensure an idle
                // connection is kept open.
                ByteBuffer buffer = getBuffer();
                buffer.clear();
                try
                    {
                    channel.write(buffer);
                    setStatsPings(getStatsPings() + 1L);           
                    }
                catch (IOException e)
                    {
                    onDisconnect(key, e);
                    }
                }
            }
        }
    
    public void heartbeatBuddies()
        {
        // import java.nio.channels.SelectionKey;
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        // import java.util.Set;
        
        try
            {
            for (Iterator iter = getBuddies().values().iterator(); iter.hasNext(); )
                {
                TcpRing.MemberMonitor monitor = (TcpRing.MemberMonitor) iter.next();
        
                // reestablish any missing connection(s) to this member
                connect(monitor.getMember());
        
                for (Iterator iterKey = ((Set) monitor.getKeys()).iterator(); iterKey.hasNext();  )
                    {
                    heartbeat((SelectionKey) iterKey.next());
                    }
                }
            }
        catch (ConcurrentModificationException e)
            {
            // quick fix for COH-34357; TODO: full fix needs to remove re-entrant calls
            }
        
        if (getInboundConnectionCount() == 0)
            {
            onIsolation();
            }
        }
    
    /**
     * Event notification called when there is a new SocketChannel to accept.
     */
    protected void onAccept(java.nio.channels.SelectionKey key)
        {
        // import java.io.IOException;
        // import java.nio.channels.SelectionKey;
        // import java.nio.channels.ServerSocketChannel;
        // import java.nio.channels.SocketChannel;
        
        SocketChannel channel = null;
        try
            {
            // accept a SocketChannel
            channel = ((ServerSocketChannel) key.channel()).accept();
            if (channel == null)
                {
                // COH-4483: #accept() can spuriously return null
                return;
                }
            }
        catch (IOException e)
            {
            onAcceptException(e);
            return;
            }
        
        SelectionKey keyClient = null;
        try
            {
            // configure the SocketChannel
            channel.configureBlocking(false);
            
            try
                {
                channel.socket().setSoLinger(false, 0);
                }
            catch (IOException e) {}
        
            // register interest in OP_READ for the new SocketChannel
            keyClient = channel.register(ensureSelector(channel), SelectionKey.OP_READ);
            setInboundConnectionCount(getInboundConnectionCount() + 1);
            }
        catch (IOException e)
            {
            if (!channel.socket().isClosed())
                {
                _trace("error on TcpRing accept: " + channel.socket() + "\n" + getStackTrace(e), 1);
                }
            
            if (keyClient == null)
                {
                try
                    {
                    channel.socket().close();
                    }
                catch (IOException e2) {}        
                }
            else
                {
                close(keyClient);
                }
            }
        }
    
    /**
     * Called to signal an exception raised during the "accept" operation.
     */
    protected void onAcceptException(Exception e)
        {
        onException(e);
        }
    
    /**
     * Event notification called when a SocketChannel is ready to complete its
    * connection sequence
     */
    protected void onConnect(java.nio.channels.SelectionKey key)
        {
        // import Component.Net.Member;
        // import java.io.IOException;
        // import java.nio.channels.SocketChannel;
        
        SocketChannel  channel = (SocketChannel) key.channel();
        TcpRing.MemberMonitor monitor = (TcpRing.MemberMonitor) key.attachment();
        
        // attempt to finish the connection being established.
        try
            {
            if (channel.finishConnect())
                {
                // the connect request has finished;
                // register interest in read operations
                key.interestOps(key.OP_READ);
                monitor.setPendingConnections(monitor.getPendingConnections() - 1);
        
                monitor.getKeys().add(key);
        
                Member member = monitor.getMember();
                if (getBuddies().get(member) == monitor)
                    {
                    _trace("TcpRing connected to " + member, 6);
                    }
                else // no longer a buddy
                    {
                    close(key);
                    }
                }
            }
        catch (IOException e)
            {
            onDisconnect(key, e);
            }
        }
    
    /**
     * Remove the current buddy from the cluster
     */
    protected void onDeadBuddy(Member member, java.io.IOException e)
        {
        setStatsFailures(getStatsFailures() + 1);
        
        // remove the closed connection
        getBuddies().remove(member);
        }
    
    /**
     * Called when a channel becomes disconnected.
     */
    protected void onDisconnect(java.nio.channels.SelectionKey key, java.io.IOException e)
        {
        // import Component.Net.Member;
        // import java.net.NoRouteToHostException;
        // import java.util.Set;
        
        TcpRing.MemberMonitor monitor = close(key);
        if (monitor == null)
            {
            // inbound connection
            int cInbound = getInboundConnectionCount() - 1;
            setInboundConnectionCount(cInbound);
            if (cInbound == 0)
                {
                onIsolation();
                }
            }
        else
            {
            Member member = monitor.getMember();
            if (getBuddies().containsKey(member))
                {
                String sReason = e.getMessage();
        
                sReason = e.getClass().getSimpleName() + (sReason == null ? "" : ": " + sReason);
        
                if (e instanceof NoRouteToHostException       ||
                    "No route to host".equals(e.getMessage()) ||
                    "Connection timed out".equals(e.getMessage()))
                    {
                    // It is the job of TcpRing to only provide positive death detection, and as such it
                    // should not declare death because of a timeout or unreachable server. The SocketChannel
                    // API does not provide a way to differentiate these from an actual connect refusal, but
                    // based on the HotSpot sources it will be one of the above. Note that on Linux a TCP
                    // connection will timeout by default after 20s, which is way too low given our default
                    // IpMon timeout of 15s. So here we retry on timeout for as long as our other timeout
                    // based mechanisms (packet-timeout and IpMon) decide to allow this unreachable member
                    // to live. The only downside to this approach is that if there is a firewall permanently
                    // blocking access we've effectivly disabled TcpRing. We do of course verify TcpRing
                    // to the senior on join, but that doesn't mean we couldn't be blocked later when
                    // trying to monitor some other member.
        
                    _trace("TcpRing connection to " + member + " timed out (" + sReason + "); retrying.", 2);
        
                    // re-try will occur on next call to heartbeatBuddies
                    }
                else if (monitor.getKeys().isEmpty())
                    {
                    // we've lost all our connections to the member; declare it dead
        
                    _trace("TcpRing disconnected from " + member + " due to a peer departure (" +
                        sReason +"); removing the member.", 3);
            
                    onDeadBuddy(member, e);
                    }
                else
                    {
                    // retry until all channels are simultanteously in a unconnected state
                
                    // unlike the above case this is not uncommon, during any death where we are
                    // maintaining multiple connections they can only fail serially, so we don't want
                    // to be overly aggressive about logging this
                
                    _trace("TcpRing connection to " + member + " failed (" + sReason + "); retrying.", 6);
        
                    // re-try will occur on next call to heartbeatBuddies
                    }
                }
            // else; no longer a buddy
            }
        }
    
    /**
     * Called to signal an exception raised during the "disconnect" operation.
     */
    protected void onDisconnectException(Exception e, java.nio.channels.SelectionKey key)
        {
        _trace("TcpRing disconnect from " + key.attachment() + " failed: " + e.getMessage(), 3);
        
        // take no action; it could be our fault; if the problem is with the other node
        // that member's new buddy should fail to connect and kill it
        }
    
    /**
     * Called to signal an exception raised during processing
     */
    protected void onException(Exception e)
        {
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
        // import com.tangosol.net.SocketOptions;
        // import com.tangosol.util.Base;
        // import java.net.SocketException;
        // import java.nio.ByteBuffer;
        
        // obtain a buffer for I/O
        setBuffer(ByteBuffer.allocate(1));
        
        super.onInit();
        
        // specify socket option defaults
        try
            {
            SocketOptions options = getSocketOptions();
            options.setOption(SocketOptions.TCP_NODELAY, Boolean.TRUE);
            options.setOption(SocketOptions.SO_LINGER, Integer.valueOf(0));
            }
        catch (SocketException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    
    /**
     * Invoked when the TcpRing finds that it is has no inbound buddies, i.e. is
    * unmonitored.
     */
    protected void onIsolation()
        {
        }
    
    /**
     * Event notification called when a SocketChannel has been selected for a
    * read operation.
     */
    protected void onRead(java.nio.channels.SelectionKey key)
        {
        // import Component.Net.Member;
        // import java.io.IOException;
        // import java.nio.ByteBuffer;
        // import java.nio.channels.SocketChannel;
        
        // attempt a read in order to find out if the socket has been closed
        // (this also discards any unsolicited data)
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer    buffer  = getBuffer();
        
        buffer.clear();
        
        try
            {
            // IOException or -1 (end-of-stream) on channel read
            // signifies that the remote end of the channel has been
            // closed, and the peer member has departed
            if (channel.read(buffer) == -1)
                {
                onDisconnect(key, new IOException("end of stream"));
                }
            }
        catch (IOException e)
            {
            onDisconnect(key, e);
            }
        }
    
    /**
     * Called to signal that the selector as indicated IO events are ready for
    * processing.
     */
    protected void onSelect()
        {
        // import java.nio.channels.SelectionKey;
        // import java.util.Iterator;
        
        // process any selected keys as they become available
        for (Iterator iter = getSelector().selectedKeys().iterator();
             iter.hasNext(); )
            {
            SelectionKey key = (SelectionKey) iter.next();
            iter.remove();
        
            if (!key.isValid())
                {
                continue;
                }
        
            int nMaskOps = key.readyOps();
        
            // handle new inbound connections
            if ((nMaskOps & SelectionKey.OP_ACCEPT) != 0)
                {
                onAccept(key);
                }
        
            // handle new outbound connections
            if ((nMaskOps & SelectionKey.OP_CONNECT) != 0)
                {
                onConnect(key);
                }
        
            // handle reads
            if ((nMaskOps & SelectionKey.OP_READ) != 0)
                {
                onRead(key);
                }
            }
        }
    
    public void resetStats()
        {
        setStatsFailures(0L);
        setStatsPings(0L);
        }
    
    /**
     * Wait for IO events on the TcpRing's selector.
    * 
    * @param cMillis how long to wait, 0 for infinite, -1 for immediate
     */
    public void select(long cMillis)
        {
        // import com.oracle.coherence.common.base.Blocking;
        // import java.io.IOException;
        // import java.nio.channels.ClosedSelectorException;
        // import java.nio.channels.Selector;
        
        Selector selector = getSelector();
        synchronized (selector)
            {
            try
                {
                if (!selector.isOpen())
                    {
                    // select after close; this is a usage error and ignoring it will lead to endless logging
                    throw new IllegalStateException("TcpRing has been closed");
                    }
                else if (cMillis < 0L)
                    {
                    selector.selectNow();
                    }
                else
                    {
                    Blocking.select(selector, cMillis);
                    }
        
                onSelect();
                }
            catch (ClosedSelectorException e)
                {
                // concurrent close; not an error
                _trace(e, "socket is closed");
                }
            catch (IOException e)
                {
                // NOTE: As there have been prior "benign" intermittent issues
                //       in the Java Selector provider implementations, log but
                //       otherwise ignore the exception.
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4504001
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6322825
                _trace(e, "Caught an I/O exception while processing a TcpRing Socket; "            
                        + "the exception has been logged and will be ignored");
                }
            }
        }
    
    // Accessor for the property "Buddies"
    /**
     * Setter for property Buddies.<p>
    * Map of buddies being monitored.  This is a map keyed by Member, where the
    * value is the corresponding MemberMonitor.
     */
    protected void setBuddies(java.util.Map mapBuddies)
        {
        __m_Buddies = mapBuddies;
        }
    
    // Accessor for the property "Buffer"
    /**
     * Setter for property Buffer.<p>
    * A scratch ByteBuffer used for all IO.
     */
    protected void setBuffer(java.nio.ByteBuffer buffer)
        {
        __m_Buffer = buffer;
        }
    
    // Accessor for the property "InboundConnectionCount"
    /**
     * Setter for property InboundConnectionCount.<p>
    * The number of inbound connections, i.e. the number of connections
    * monitoring this member.
     */
    public void setInboundConnectionCount(int nCount)
        {
        __m_InboundConnectionCount = nCount;
        }
    
    // Accessor for the property "RedundancyLevel"
    /**
     * Setter for property RedundancyLevel.<p>
    * The number of suplemental connections to maintain for each buddy. 
    * Maintaining multiple connections decreases the chance of a false positive
    * death detection due to a spuratic TCP failure in that a member is only
    * considered dead if all connections are simultaneously down.
     */
    public void setRedundancyLevel(int nLevel)
        {
        __m_RedundancyLevel = nLevel;
        }
    
    // Accessor for the property "Selector"
    /**
     * Setter for property Selector.<p>
    * The Selector used to select from the various SelectableChannel objects
    * created by the TCP ring listener.
     */
    public void setSelector(java.nio.channels.Selector selector)
        {
        __m_Selector = selector;
        }
    
    // Accessor for the property "ServerSocketChannel"
    /**
     * Setter for property ServerSocketChannel.<p>
    * The TCP ring listener's server socket channel.
     */
    public void setServerSocketChannel(java.nio.channels.ServerSocketChannel channel)
        {
        // import com.tangosol.util.Base;
        // import java.io.IOException;
        // import java.nio.channels.SelectionKey;
        // import java.nio.channels.ServerSocketChannel;
        
        ServerSocketChannel channelOld = getServerSocketChannel();
        if (channelOld != null)
            {
            try
                {
                channel.close();
                }
            catch (IOException e) {}
            }
        
        // obtain a Selector and register the ServerSocketChannel for
        // accept readiness
        try
            {
            channel.register(ensureSelector(channel), SelectionKey.OP_ACCEPT);
            getSocketOptions().apply(channel.socket());
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        
        __m_ServerSocketChannel = (channel);
        }
    
    // Accessor for the property "SocketOptions"
    /**
     * Setter for property SocketOptions.<p>
    * Specifies the SocketOptions to be applied to each socket.
     */
    protected void setSocketOptions(com.tangosol.net.SocketOptions options)
        {
        _assert(options != null);
        _assert(getSocketOptions() == null);
        
        __m_SocketOptions = (options);
        }
    
    // Accessor for the property "SocketProvider"
    /**
     * Setter for property SocketProvider.<p>
    * The SocketProvider used to create the Sockets used by TcpRing.
     */
    public void setSocketProvider(com.oracle.coherence.common.net.SocketProvider providerSocket)
        {
        __m_SocketProvider = providerSocket;
        }
    
    // Accessor for the property "StatsFailures"
    /**
     * Setter for property StatsFailures.<p>
    * Statistics: total number of failures.
     */
    protected void setStatsFailures(long cFailures)
        {
        __m_StatsFailures = cFailures;
        }
    
    // Accessor for the property "StatsPings"
    /**
     * Setter for property StatsPings.<p>
    * Statistics: total number of pings.
     */
    protected void setStatsPings(long cPings)
        {
        __m_StatsPings = cPings;
        }
    
    // Declared at the super level
    public String toString()
        {
        // import Component.Net.Member;
        // import java.nio.channels.SelectionKey;
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        // import java.util.Set;
        
        StringBuilder sb         = new StringBuilder("TcpRing{Connections=[");
        Set           setBuddies = getBuddies().keySet();
        try
            {        
            for (Iterator iter = setBuddies.iterator(); iter.hasNext(); )
                {
                sb.append(((Member) iter.next()).getId());
                if (iter.hasNext())
                    {
                    sb.append(", ");
                    }
                }
            }
        catch (ConcurrentModificationException e)
            {
            // eat it        
            }
        sb.append("]}");
        return sb.toString();
        }
    
    /**
     * Verify that a connection can be established (within the specified
    * timeout) to the TcpRing listener configured on the specified member.
    * This method may be called on other threads.
    * 
    * @param member                 the member to test connectivity
    * @param cTimeoutMillis      the timeout
     */
    public boolean verifyReachable(Member member, long cTimeoutMillis)
        {
        // import com.oracle.coherence.common.base.Blocking;
        // import java.io.IOException;
        // import java.net.InetSocketAddress;
        // import java.net.Socket;
        
        try
            {
            // try to a blocking connect to the member's TcpRing listener
            Socket socket = getSocketProvider().openSocket();
            Blocking.connect(socket, new InetSocketAddress(member.getAddress(), member.getTcpRingPort()), (int) cTimeoutMillis);
            socket.getOutputStream().write(0);
            socket.close();
            }
        catch (IOException e) // SocketTimeoutException also
            {
            return false;
            }
        return true;
        }
    
    /**
     * Wakeup the TcpRing's selector.
     */
    public void wakeup()
        {
        getSelector().wakeup();
        }

    // ---- class: com.tangosol.coherence.component.net.TcpRing$MemberMonitor
    
    /**
     * The MemberMonitor contains state associated with monitoring a given
     * member.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MemberMonitor
            extends    com.tangosol.coherence.component.Net
        {
        // ---- Fields declarations ----
        
        /**
         * Property Keys
         *
         * The set of SelectionKeys associated with this member.
         */
        private com.tangosol.util.LiteSet __m_Keys;
        
        /**
         * Property Member
         *
         */
        private Member __m_Member;
        
        /**
         * Property PendingConnections
         *
         * The number of connections which have been initiated but have not yet
         * connected.
         */
        private int __m_PendingConnections;
        
        // Default constructor
        public MemberMonitor()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public MemberMonitor(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setKeys(new com.tangosol.util.LiteSet());
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
            return new com.tangosol.coherence.component.net.TcpRing.MemberMonitor();
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
                clz = Class.forName("com.tangosol.coherence/component/net/TcpRing$MemberMonitor".replace('/', '.'));
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
        
        // Accessor for the property "Keys"
        /**
         * Getter for property Keys.<p>
        * The set of SelectionKeys associated with this member.
         */
        public com.tangosol.util.LiteSet getKeys()
            {
            return __m_Keys;
            }
        
        // Accessor for the property "Member"
        /**
         * Getter for property Member.<p>
         */
        public Member getMember()
            {
            return __m_Member;
            }
        
        // Accessor for the property "PendingConnections"
        /**
         * Getter for property PendingConnections.<p>
        * The number of connections which have been initiated but have not yet
        * connected.
         */
        public int getPendingConnections()
            {
            return __m_PendingConnections;
            }
        
        // Accessor for the property "Keys"
        /**
         * Setter for property Keys.<p>
        * The set of SelectionKeys associated with this member.
         */
        protected void setKeys(com.tangosol.util.LiteSet setChannel)
            {
            __m_Keys = setChannel;
            }
        
        // Accessor for the property "Member"
        /**
         * Setter for property Member.<p>
         */
        public void setMember(Member member)
            {
            __m_Member = member;
            }
        
        // Accessor for the property "PendingConnections"
        /**
         * Setter for property PendingConnections.<p>
        * The number of connections which have been initiated but have not yet
        * connected.
         */
        public void setPendingConnections(int nConnections)
            {
            __m_PendingConnections = nConnections;
            }
        }
    }
