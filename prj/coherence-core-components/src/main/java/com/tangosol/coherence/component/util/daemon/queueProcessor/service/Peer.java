/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer

package com.tangosol.coherence.component.util.daemon.queueProcessor.service;

import com.tangosol.coherence.component.net.extend.Channel;
import com.tangosol.coherence.component.net.extend.Codec;
import com.tangosol.coherence.component.net.extend.Connection;
import com.tangosol.coherence.component.util.daemon.queueProcessor.Service;
import com.oracle.coherence.common.base.Blocking;
import com.tangosol.coherence.config.Config;
import com.tangosol.internal.net.security.AccessAdapter;
import com.tangosol.internal.net.service.peer.DefaultPeerDependencies;
import com.tangosol.internal.net.service.peer.PeerDependencies;
import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.Serializer;
import com.tangosol.io.WrapperBufferInput;
import com.tangosol.io.WrapperBufferOutput;
import com.tangosol.io.WrapperDataInputStream;
import com.tangosol.io.WrapperDataOutputStream;
import com.tangosol.io.WriteBuffer;
import com.tangosol.license.LicenseException;
import com.tangosol.net.Guardian;
import com.tangosol.net.Member;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.messaging.ConnectionEvent;
import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.net.messaging.Message;
import com.tangosol.net.security.IdentityAsserter;
import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.Listeners;
import com.tangosol.util.UUID;
import com.tangosol.util.WrapperException;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.security.auth.Subject;

/**
 * Base definition of a ConnectionManager component.
 * 
 * A ConnectionManager has a service thread, an optional execute thread pool,
 * and a ConnectionEvent dispatcher thread.
 * 
 * Concrete implementations must implement the abstract send() method using the
 * underlying transport. Additionally, the underlying transport must call the
 * receive() or post() method when a Message is received over the underlying
 * transport.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Peer
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service
        implements com.tangosol.net.messaging.Channel.Receiver,
                   com.tangosol.net.messaging.ConnectionManager
    {
    // ---- Fields declarations ----
    
    /**
     * Property _Channel
     *
     * The Channel used for all internal communication.
     */
    private transient com.tangosol.coherence.component.net.extend.Channel __m__Channel;
    
    /**
     * Property _Connection
     *
     * The Connection used for all internal communication.
     */
    private transient com.tangosol.coherence.component.net.extend.Connection __m__Connection;
    
    /**
     * Property Codec
     *
     * The Codec used to encode and decode all Messages sent by this
     * ConnectionManager.
     * 
     * @see com.tangosol.net.messaging.ConnectionManager#getCodec
     */
    private com.tangosol.net.messaging.Codec __m_Codec;
    
    /**
     * Property ConnectionListeners
     *
     * The list of registered ConnectionListeners.
     * 
     * @see #addConnectionListener and #removeConnectionListener
     */
    private com.tangosol.util.Listeners __m_ConnectionListeners;
    
    /**
     * Property DEBUG
     *
     * Debug flag. The value of this flag is set using the following system
     * property:
     * 
     * tangosol.coherence.messaging.debug
     * 
     * or it can be dynamically set through the MessagingDebug attribute of the
     * ConnectionManagerMBean.
     */
    private static transient boolean __s_DEBUG;
    
    /**
     * Property LicenseError
     *
     * An optional license check exception message.
     * 
     * @see _initStatic()
     */
    private static transient String __s_LicenseError;
    
    /**
     * Property MaxIncomingMessageSize
     *
     * The size limit of an incoming message.
     */
    private int __m_MaxIncomingMessageSize;
    
    /**
     * Property MaxOutgoingMessageSize
     *
     * The size limit of an outgoing message.
     */
    private int __m_MaxOutgoingMessageSize;
    
    /**
     * Property ParentService
     *
     * The optional parent com.tangosol.net.Service.
     * 
     * @see #assertIdentityToken
     * @see #generateIdentityToken
     */
    private transient com.tangosol.net.Service __m_ParentService;
    
    /**
     * Property PingInterval
     *
     * The number of milliseconds between successive Connection "pings" or 0 if
     * heartbeats are disabled.
     */
    private long __m_PingInterval;
    
    /**
     * Property PingLastCheckMillis
     *
     * The last time the Connection(s) managed by this ConnectionManager were
     * checked for a "ping" timeout.
     */
    private transient long __m_PingLastCheckMillis;
    
    /**
     * Property PingLastMillis
     *
     * The last time the Connection(s) managed by this ConnectionManager were
     * "pinged".
     */
    private transient long __m_PingLastMillis;
    
    /**
     * Property PingTimeout
     *
     * The default request timeout for a PingRequest. A timeout of 0 is
     * interpreted as an infinite timeout. This property defaults to the value
     * of the RequestTimeout property.
     */
    private long __m_PingTimeout;
    
    /**
     * Property ProcessId
     *
     * The unique identifier (UUID) of the process using this ConnectionManager.
     */
    private static transient com.tangosol.util.UUID __s_ProcessId;
    
    /**
     * Property Protocol
     *
     */
    private com.tangosol.net.messaging.Protocol __m_Protocol;
    
    /**
     * Property ProtocolMap
     *
     * The Map of registered Protocol objects.
     * 
     * @see com.tangosol.net.messaging.ConnectionManager#getProtocol
     */
    private java.util.Map __m_ProtocolMap;
    
    /**
     * Property ProtocolVersionMap
     *
     * A Map of version ranges for registered Protocols. The keys are the names
     * of the Protocols and the values are two element Integer arrays, the
     * first element being the current version and the second being the
     * supported version of the corresponding Protocol.
     */
    private transient java.util.Map __m_ProtocolVersionMap;
    
    /**
     * Property ReceiverMap
     *
     * The Map of registered Receiver objects.
     * 
     * @see com.tangosol.net.messaging.ConnectionManager#getRecievers
     */
    private transient java.util.Map __m_ReceiverMap;
    
    /**
     * Property RequestTimeout
     *
     * The default request timeout for all Channel objects created by
     * Connection objects managed by this ConnectionManager. A timeout of 0 is
     * interpreted as an infinite timeout.
     * 
     * @see Connection#getRequestTimeout
     */
    private long __m_RequestTimeout;
    
    /**
     * Property StatsBytesReceived
     *
     * Statistics: total number of bytes received.
     */
    private transient long __m_StatsBytesReceived;
    
    /**
     * Property StatsBytesSent
     *
     * Statistics: total number of bytes sent.
     */
    private transient long __m_StatsBytesSent;
    
    /**
     * Property StatsSent
     *
     * Statistics: total number of messages sent.
     */
    private transient long __m_StatsSent;
    
    /**
     * Property StatsTimeoutCount
     *
     * The total number of timed-out requests since the last time the
     * statistics were reset.
     * 
     * @volatile
     */
    private volatile transient long __m_StatsTimeoutCount;
    
    /**
     * Property WrapperStreamFactoryList
     *
     * A List of WrapperStreamFactory objects that affect how Messages are
     * written and read.
     */
    private transient java.util.List __m_WrapperStreamFactoryList;
    private static com.tangosol.util.ListMap __mapChildren;
    
    private static void _initStatic$Default()
        {
        __initStatic();
        }
    
    // Static initializer (from _initStatic)
    static
        {
        // import com.tangosol.coherence.config.Config;
        // import com.tangosol.util.UUID;
        
        _initStatic$Default();
        
        // configure debug flag
        setDEBUG(Config.getBoolean("coherence.messaging.debug"));
        
        // generate a UUID for this process
        setProcessId(new UUID());
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("DispatchEvent", Peer.DispatchEvent.get_CLASS());
        __mapChildren.put("MessageFactory", Peer.MessageFactory.get_CLASS());
        __mapChildren.put("Queue", com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue.get_CLASS());
        }
    
    // Initializing constructor
    public Peer(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        
        // state initialization: private properties
        try
            {
            __m_ConnectionListeners = new com.tangosol.util.Listeners();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer".replace('/', '.'));
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
     * Accept a new Channel. This method is called by Connection.acceptChannel()
    * and is always run on client threads.
    * 
    * @see com.tangosol.net.messaging.Connection#acceptChannel
     */
    public com.tangosol.net.messaging.Channel acceptChannel(com.tangosol.coherence.component.net.extend.Connection connection, java.net.URI uri, ClassLoader loader, com.tangosol.net.messaging.Channel.Receiver receiver, javax.security.auth.Subject subject)
        {
        // import com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        // import com.tangosol.net.messaging.Request$Status as com.tangosol.net.messaging.Request.Status;
        
        _assert(connection != null);
        
        Channel channel0 = get_Channel();
        com.tangosol.net.messaging.Protocol.MessageFactory factory0 = channel0.getMessageFactory();
        
        Peer.MessageFactory.AcceptChannel request = (Peer.MessageFactory.AcceptChannel)
                factory0.createMessage(Peer.MessageFactory.AcceptChannel.TYPE_ID);
        
        request.setChannelUri(uri);
        request.setClassLoader(loader);
        request.setConnection(connection);
        request.setIdentityToken(serializeIdentityToken(generateIdentityToken(subject)));
        request.setReceiver(receiver);
        request.setSubject(subject);
        
        com.tangosol.net.messaging.Request.Status status = (com.tangosol.net.messaging.Request.Status) channel0.request(request);
        
        Peer.MessageFactory.AcceptChannelResponse response = (Peer.MessageFactory.AcceptChannelResponse)
                status.waitForResponse(getRequestTimeout());
        
        return (Channel) response.getResult();
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionManager
    public void addConnectionListener(com.tangosol.net.messaging.ConnectionListener l)
        {
        ensureEventDispatcher();
        getConnectionListeners().add(l);
        }
    
    /**
     * Allocate a WriteBuffer for encoding a Message. After the Message has been
    * encoded and the resulting ReadBuffer sent via the Connection, the
    * WriteBuffer is released via the releaseWriteBuffer() method.
    * 
    * This method is called on both client and service threads.
    * 
    * @return a WriteBuffer that can be used to encode a Message
     */
    protected com.tangosol.io.WriteBuffer allocateWriteBuffer()
        {
        // import com.tangosol.io.ByteArrayWriteBuffer;
        
        return new ByteArrayWriteBuffer(1024);
        }
    
    /**
     * Validates a token in order to establish a user's identity.
    * 
    * @param oToken  an identity assertion, a statement that asserts an
    * identity
    * 
    * @return a Subject representing the identity
     */
    public javax.security.auth.Subject assertIdentityToken(Object oToken)
        {
        return getOperationalContext().getIdentityAsserter().
                assertIdentity(oToken, getParentService());
        }
    
    /**
     * Check the given Connection for a ping timeout. A Connection that has not
    * received a PingResponse for an oustanding PingRequest within the
    * configured PingTimeout will be closed.
    * 
    * @param connection  the Connection to check
     */
    protected void checkPingTimeout(com.tangosol.coherence.component.net.extend.Connection connection)
        {
        // import com.tangosol.net.messaging.ConnectionException;
        // import com.tangosol.util.Base;
        
        long cMillis = getPingTimeout();
        if (cMillis > 0L)
            {
            long ldtPing = connection.getPingLastMillis();
            if (ldtPing > 0L)
                {
                if (Base.getSafeTimeMillis() >= ldtPing + cMillis)
                    {
                    connection.close(false, new ConnectionException(
                            "did not receive a response to a ping within "
                            + cMillis + " millis", connection));
                    }
                }
            }
        }
    
    /**
     * Check the Connection(s) managed by this ConnectionManager for a ping
    * timeout. A Connection that has not received a PingResponse for an
    * oustanding PingRequest within the configured PingTimeout will be closed.
     */
    protected void checkPingTimeouts()
        {
        }
    
    // Declared at the super level
    /**
     * Create a new Default dependencies object by copying the supplies
    * dependencies.  Each class or component that uses dependencies implements
    * a Default dependencies class which provides the clone functionality.  
    * The dependency injection design pattern requires every component in the
    * component hierarchy to implement clone, producing their variant of the
    * dependencies interface.
    * 
    * @return the cloned dependencies
     */
    protected com.tangosol.internal.net.service.DefaultServiceDependencies cloneDependencies(com.tangosol.net.ServiceDependencies deps)
        {
        // import com.tangosol.internal.net.service.peer.DefaultPeerDependencies;
        // import com.tangosol.internal.net.service.peer.PeerDependencies;
        
        return new DefaultPeerDependencies((PeerDependencies) deps);
        }
    
    /**
     * Close the given Channel. This method is called by Channel.close() and is
    * always run on client threads.
    * 
    * @param channel  the Channel to close
    * @param fNotify  if true, notify the peer that the Channel is being closed
    * @param e  the optional reason why the Channel is being closed
     */
    public void closeChannel(com.tangosol.coherence.component.net.extend.Channel channel, boolean fNotify, Throwable e)
        {
        closeChannel(channel, fNotify, e, /*fWait*/ true);
        }
    
    /**
     * Close the given Channel. This method is always run on client threads.
    * 
    * @param channel  the Channel to close
    * @param fNotify  if true, notify the peer that the Channel is being closed
    * @param e  the optional reason why the Channel is being closed
    * @param fWait  if true, wait for the Channel to close before returning
     */
    public void closeChannel(com.tangosol.coherence.component.net.extend.Channel channel, boolean fNotify, Throwable e, boolean fWait)
        {
        // import com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        
        _assert(channel != null);
        
        Channel channel0 = get_Channel();
        com.tangosol.net.messaging.Protocol.MessageFactory factory0 = channel0.getMessageFactory();
        
        Peer.MessageFactory.CloseChannel request = (Peer.MessageFactory.CloseChannel)
                factory0.createMessage(Peer.MessageFactory.CloseChannel.TYPE_ID);
        
        request.setCause(e);
        request.setChannelClose(channel);
        request.setNotify(fNotify);
        
        if (fWait)
            {
            channel0.request(request);
            }
        else
            {
            channel0.send(request);
            }
        }
    
    /**
     * Close the given Connection. This method is called by Connection.close()
    * and is always run on client threads.
    * 
    * @param channel  the Connection to close
    * @param fNotify  if true, notify the peer that the Connection is being
    * closed
    * @param e  the optional reason why the Connection is being closed
    * @param fWait  if true, wait for the Connection to close before returning
     */
    public void closeConnection(com.tangosol.coherence.component.net.extend.Connection connection, boolean fNotify, Throwable e, boolean fWait)
        {
        // import com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        
        _assert(connection != null);
        
        Channel channel0 = get_Channel();
        com.tangosol.net.messaging.Protocol.MessageFactory factory0 = channel0.getMessageFactory();
        
        Peer.MessageFactory.CloseConnection request = (Peer.MessageFactory.CloseConnection)
                factory0.createMessage(Peer.MessageFactory.CloseConnection.TYPE_ID);
        
        request.setCause(e);
        request.setConnectionClose(connection);
        request.setNotify(fNotify);
        
        if (fWait)
            {
            channel0.request(request);
            }
        else
            {
            channel0.send(request);
            }
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionManager
    // Declared at the super level
    public synchronized void configure(com.tangosol.run.xml.XmlElement xml)
        {
        // import com.tangosol.internal.net.service.peer.DefaultPeerDependencies;
        // import com.tangosol.internal.net.service.peer.LegacyXmlPeerHelper as com.tangosol.internal.net.service.peer.LegacyXmlPeerHelper;
        
        setDependencies(com.tangosol.internal.net.service.peer.LegacyXmlPeerHelper.fromXml(xml,
            new DefaultPeerDependencies(), getOperationalContext(), getContextClassLoader()));
        
        setServiceConfig(xml);
        }
    
    /**
     * Create a new Channel. This method is called by Connection.createChannel()
    * and is always run on client threads.
    * 
    * @see com.tangosol.net.messaging.Connection#createChannel
     */
    public java.net.URI createChannel(com.tangosol.coherence.component.net.extend.Connection connection, com.tangosol.net.messaging.Protocol protocol, ClassLoader loader, com.tangosol.net.messaging.Channel.Receiver receiver)
        {
        // import com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        // import java.net.URI;
        
        _assert(connection != null);
        
        Channel channel0 = get_Channel();
        com.tangosol.net.messaging.Protocol.MessageFactory factory0 = channel0.getMessageFactory();
        
        Peer.MessageFactory.CreateChannel request = (Peer.MessageFactory.CreateChannel)
                factory0.createMessage(Peer.MessageFactory.CreateChannel.TYPE_ID);
        
        request.setClassLoader(loader);
        request.setConnection(connection);
        request.setProtocol(protocol);
        request.setReceiver(receiver);
        
        return (URI) channel0.request(request);
        }
    
    /**
     * Decode the given BufferInput with the configured Codec and return a new
    * decoded Message. This method is called on either the service thread (see
    * EncodedMessage) or a client (I/O) thread.
    * 
    * @param in  the BufferInput that contains an encoded Message
    * @param connection  the Connection that received the Message
    * @param fFilter  if true, the BufferInput will be filtered using the list
    * of configured WrapperStreamFactory objects
    * 
    * @return the decoded Message or null if the Message was sent via an
    * unknown Channel
    * 
    * @throws IOException on I/O error decoding the Message
     */
    protected com.tangosol.net.messaging.Message decodeMessage(com.tangosol.io.ReadBuffer.BufferInput in, com.tangosol.coherence.component.net.extend.Connection connection, boolean fFilter)
            throws java.io.IOException
        {
        // import Component.Net.Extend.Channel;
        // import com.tangosol.net.messaging.Codec as com.tangosol.net.messaging.Codec;
        // import com.tangosol.net.messaging.Message;
        
        _assert(in != null);
        _assert(connection != null);
        
        com.tangosol.net.messaging.Codec codec = getCodec();
        _assert(codec != null);
        
        // filter the input, if necessary
        if (fFilter)
            {
            in = filterBufferInput(in);
            }
        
        // resolve the Channel
        Channel channel = (Channel) connection.getChannel(in.readPackedInt());
        if (channel == null || !channel.isOpen())
            {
            return null;
            }
        
        // attempt to decode the Message
        Message message = codec.decode(channel, in);
        message.setChannel(channel);
        return message;
        }
    
    /**
     * Deserialize the identity token object.
    * 
    * @param abToken  the identity token
    * 
    * @return the token
    * 
    * @throws SecurityException on a deserialization error
     */
    public Object deserializeIdentityToken(byte[] abToken)
        {
        // import com.tangosol.io.ByteArrayReadBuffer;
        
        if (abToken != null)
            {
            try
                {
                ByteArrayReadBuffer buf = new ByteArrayReadBuffer(abToken);
                return ensureSerializer().deserialize(buf.getBufferInput());
                }
            catch (Exception e)
                {
                _trace(e, "An exception occurred while deserializing an identity token");
                throw new SecurityException("invalid identity token");
                }
            }
        
        return null;
        }
    
    /**
     * Dispatch a ConnectionEvent to the EventDispatcher.
    * 
    * @param connection  the Connection associated with the ConnectionEvent
    * @param nEvent the ID of the ConnectionEvent
    * @param e  the optional Throwable associated with the ConnectionEvent
     */
    protected void dispatchConnectionEvent(com.tangosol.net.messaging.Connection connection, int nEvent, Throwable e)
        {
        // import com.tangosol.net.messaging.ConnectionEvent;
        // import com.tangosol.util.Listeners;
        
        Listeners listeners = getConnectionListeners();
        if (!listeners.isEmpty())
            {
            dispatchEvent(new ConnectionEvent(connection, nEvent, e), listeners);
            }
        }
    
    /**
     * Wait for any associated backlog to drain.
    * 
    * @param cMillisTimeout  the maximum wait time, or 0 for indefinite
    * 
    * @return the remaining timeout
    * 
    * @throw RequestTimeoutException on timeout
     */
    public long drainOverflow(long cMillisTimeout)
            throws java.lang.InterruptedException
        {
        // monitor the event dispatcher queue and slow down if it gets too long
        Peer.EventDispatcher dispatcher = (Peer.EventDispatcher) getEventDispatcher();
        return dispatcher == null
            ? cMillisTimeout
            : dispatcher.drainOverflow(cMillisTimeout);
        }
    
    /**
     * Encode the given Message with the configured Codec into the specified
    * BufferOutput.
    * 
    * @param message  the Message to encode
    * @param bo  the BufferOutput that will be used to write out the encoded
    * Message
    * @param fFilter  if true, the BufferOutput will be filtered using the list
    * of configured WrapperStreamFactory objects
    * 
    * @throws IOException on encoding error
     */
    protected void encodeMessage(com.tangosol.net.messaging.Message message, com.tangosol.io.WriteBuffer.BufferOutput out, boolean fFilter)
            throws java.io.IOException
        {
        // import com.tangosol.net.messaging.Channel as com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.Codec as com.tangosol.net.messaging.Codec;
        
        _assert(message != null);
        _assert(out != null);
        
        com.tangosol.net.messaging.Channel channel = message.getChannel();
        _assert(channel != null);
        
        com.tangosol.net.messaging.Codec codec = getCodec();
        _assert(codec != null);
        
        // filter the output, if necessary
        if (fFilter)
            {
            out = filterBufferOutput(out);
            }
        
        // write the com.tangosol.net.messaging.Channel ID
        out.writePackedInt(channel.getId());
        
        // encode the Message
        codec.encode(channel, message, out);
        
        // close the BufferOutput
        out.close();
        }
    
    /**
     * Enforce the message size limit of an incoming message.
    * 
    * @param cbSize  the message size
    * 
    * @throws IOException if the message size exceeds the limit
     */
    public void enforceMaxIncomingMessageSize(int cbSize)
            throws java.io.IOException
        {
        // import java.io.IOException;
        
        int cbMax = getMaxIncomingMessageSize();
        if (cbMax > 0 && cbSize > cbMax)
            {
            throw new IOException("message length: " + cbSize + " exceeds the maximum incoming message size");
            }
        }
    
    /**
     * Enforce the message size limit of an outgoing message.
    * 
    * @param cbSize  the message size
    * 
    * @throws IOException if the message size exceeds the limit
     */
    public void enforceMaxOutgoingMessageSize(int cbSize)
            throws java.io.IOException
        {
        // import java.io.IOException;
        
        int cbMax = getMaxOutgoingMessageSize();
        
        if (cbMax > 0 && cbSize > cbMax)
            {
            throw new IOException("message length: " + cbSize + " exceeds the maximum outgoing message size");
            }
        }
    
    /**
     * Filter the given BufferInput using the configured List of
    * WrapperStreamFactory objects. If the List of WrapperStreamFactory objects
    * is null or empty, the given BufferInput is returned.
    * 
    * @param bi  the BufferInput to filter
    * 
    * @return a filtered BufferInput
     */
    protected com.tangosol.io.ReadBuffer.BufferInput filterBufferInput(com.tangosol.io.ReadBuffer.BufferInput in)
            throws java.io.IOException
        {
        // import com.tangosol.io.WrapperBufferInput;
        // import com.tangosol.io.WrapperDataInputStream;
        // import com.tangosol.io.WrapperStreamFactory as com.tangosol.io.WrapperStreamFactory;
        // import com.tangosol.util.Base;
        // import java.io.DataInput;
        // import java.io.DataInputStream;
        // import java.io.InputStream;
        // import java.io.IOException;
        // import java.util.List;
        
        List list = getWrapperStreamFactoryList();
        if (list == null || list.isEmpty())
            {
            return in;
            }
        
        // wrap the BufferInput
        InputStream stream = new WrapperDataInputStream(in);
        for (int i = 0, c = list.size(); i < c; ++i)
            {
            try
                {
                stream = ((com.tangosol.io.WrapperStreamFactory) list.get(i)).getInputStream(stream);
                }
            catch (RuntimeException e)
                {
                Throwable t = Base.getOriginalException(e);
                if (t instanceof IOException)
                    {
                    throw (IOException) t;
                    }
                throw e;
                }
            }
        
        DataInput din;
        if (stream instanceof DataInput)
            {
            din = (DataInput) stream;
            }
        else
            {
            din = new DataInputStream(stream);
            }
        
        return new WrapperBufferInput(din, getContextClassLoader());
        }
    
    /**
     * Filter the given BufferOutput using the configured List of
    * WrapperStreamFactory objects. If the List of WrapperStreamFactory objects
    * is null or empty, the given BufferOutput is returned.
    * 
    * @param bi  the BufferOutput to filter
    * 
    * @return a filtered BufferOutput
     */
    protected com.tangosol.io.WriteBuffer.BufferOutput filterBufferOutput(com.tangosol.io.WriteBuffer.BufferOutput out)
            throws java.io.IOException
        {
        // import com.tangosol.io.WrapperBufferOutput;
        // import com.tangosol.io.WrapperDataOutputStream;
        // import com.tangosol.io.WrapperStreamFactory as com.tangosol.io.WrapperStreamFactory;
        // import com.tangosol.util.Base;
        // import java.io.DataOutput;
        // import java.io.DataOutputStream;
        // import java.io.IOException;
        // import java.io.OutputStream;
        // import java.util.List;
        
        List list = getWrapperStreamFactoryList();
        if (list == null || list.isEmpty())
            {
            return out;
            }
        
        // wrap the BufferOutput
        OutputStream stream = new WrapperDataOutputStream(out);
        for (int i = 0, c = list.size(); i < c; ++i)
            {
            try
                {
                stream = ((com.tangosol.io.WrapperStreamFactory) list.get(i)).getOutputStream(stream);
                }
            catch (RuntimeException e)
                {
                Throwable t = Base.getOriginalException(e);
                if (t instanceof IOException)
                    {
                    throw (IOException) t;
                    }
                throw e;        
                }
            }
        
        DataOutput dout;
        if (stream instanceof DataOutput)
            {
            dout = (DataOutput) stream;
            }
        else
            {
            dout = new DataOutputStream(stream);
            }
        
        return new WrapperBufferOutput(dout);
        }
    
    // Declared at the super level
    /**
     * @return a human-readable description of the Service statistics
     */
    public String formatStats()
        {
        // import com.tangosol.util.Base;
        
        long cTotal  = Math.max(System.currentTimeMillis() - getStatsReset(), 0L);
        long cbRcvd  = getStatsBytesReceived();
        long cbSent  = getStatsBytesSent();
        long cbpsIn  = cTotal == 0L ? 0L : (cbRcvd / cTotal)*1000L;
        long cbpsOut = cTotal == 0L ? 0L : (cbSent / cTotal)*1000L;
        
        StringBuffer sb = new StringBuffer(super.formatStats());
        sb.append(", BytesReceived=")
          .append(Base.toMemorySizeString(cbRcvd, false))
          .append(", BytesSent=")
          .append(Base.toMemorySizeString(cbSent, false))
          .append(", ThroughputInbound=")
          .append(Base.toBandwidthString(cbpsIn, false))
          .append(", ThroughputOutbound=")
          .append(Base.toBandwidthString(cbpsOut, false));
        
        return sb.toString();
        }
    
    /**
     * Transform a Subject to a token that asserts identity.
    * 
    * @param subject  the subject to transform
    * 
    * @return a token that asserts identity
     */
    public Object generateIdentityToken(javax.security.auth.Subject subject)
        {
        return getOperationalContext().getIdentityTransformer().
                transformIdentity(subject, getParentService());
        }
    
    // Accessor for the property "_Channel"
    /**
     * Getter for property _Channel.<p>
    * The Channel used for all internal communication.
     */
    public com.tangosol.coherence.component.net.extend.Channel get_Channel()
        {
        return __m__Channel;
        }
    
    // Accessor for the property "_Connection"
    /**
     * Getter for property _Connection.<p>
    * The Connection used for all internal communication.
     */
    public com.tangosol.coherence.component.net.extend.Connection get_Connection()
        {
        return __m__Connection;
        }
    
    /**
     * Returns an AccessAdapter by determining if the IdentityAsserter is also
    * an AccessAdapter
     */
    public com.tangosol.internal.net.security.AccessAdapter getAccessAdapter()
        {
        // import com.tangosol.internal.net.security.AccessAdapter;
        // import com.tangosol.net.security.IdentityAsserter;
        
        IdentityAsserter asserter = getOperationalContext().getIdentityAsserter();
        
        return asserter instanceof AccessAdapter ? ((AccessAdapter) asserter) : null;
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionManager
    // Accessor for the property "Codec"
    /**
     * Getter for property Codec.<p>
    * The Codec used to encode and decode all Messages sent by this
    * ConnectionManager.
    * 
    * @see com.tangosol.net.messaging.ConnectionManager#getCodec
     */
    public com.tangosol.net.messaging.Codec getCodec()
        {
        return __m_Codec;
        }
    
    // Accessor for the property "ConnectionListeners"
    /**
     * Getter for property ConnectionListeners.<p>
    * The list of registered ConnectionListeners.
    * 
    * @see #addConnectionListener and #removeConnectionListener
     */
    public com.tangosol.util.Listeners getConnectionListeners()
        {
        return __m_ConnectionListeners;
        }
    
    // Declared at the super level
    /**
     * Getter for property DefaultGuardRecovery.<p>
    * Default recovery percentage for guardables manged by this Daemon.
     */
    public float getDefaultGuardRecovery()
        {
        // import com.tangosol.net.Guardian;
        // import com.tangosol.net.Service as com.tangosol.net.Service;
        
        // COH-4604: If the peer is being used by a parent service (e.g. ProxyService),
        //           delegate the guardian duties to that service
        com.tangosol.net.Service service = getParentService();
        if (service instanceof Guardian)
            {
            return ((Guardian) service).getDefaultGuardRecovery();
            }
        return super.getDefaultGuardRecovery();
        }
    
    // Declared at the super level
    /**
     * Getter for property DefaultGuardTimeout.<p>
    * Default timeout interval for guardables manged by this Daemon.
     */
    public long getDefaultGuardTimeout()
        {
        // import com.tangosol.net.Guardian;
        // import com.tangosol.net.Service as com.tangosol.net.Service;
        
        // COH-4604: If the peer is being used by a parent service (e.g. ProxyService),
        //           delegate the guardian duties to that service
        com.tangosol.net.Service service = getParentService();
        if (service instanceof Guardian)
            {
            return ((Guardian) service).getDefaultGuardTimeout();
            }
        return super.getDefaultGuardTimeout();
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human-readable description of additional Service properties. Used by
    * toString().
     */
    public String getDescription()
        {
        // import Component.Util.DaemonPool as com.tangosol.coherence.component.util.DaemonPool;
        // import com.tangosol.net.messaging.Codec as com.tangosol.net.messaging.Codec;
        // import java.util.Iterator;
        // import java.util.List;
        
        StringBuffer sb = new StringBuffer();
        
        com.tangosol.coherence.component.util.DaemonPool pool = getDaemonPool();
        if (pool.isStarted())
            {
            sb.append("ThreadCount=")
              .append(pool.getDaemonCount())
              .append(", HungThreshold=")
              .append(pool.getHungThreshold())
              .append(", TaskTimeout=")
              .append(pool.getTaskTimeout());
            }
        else
            {
            sb.append("ThreadCount=0");
            }
        
        List list = getWrapperStreamFactoryList();
        if (list != null && !list.isEmpty())
            {
            sb.append(", Filters=[");
        
            for (Iterator iter = list.iterator(); iter.hasNext(); )
                {
                sb.append(iter.next().getClass().getName());
                if (iter.hasNext())
                    {
                    sb.append(',');
                    }
                }
        
            sb.append(']');
            }
        
        com.tangosol.net.messaging.Codec codec = getCodec();
        if (codec != null)
            {
            sb.append(", Codec=")
              .append(codec);
            }
        
        sb.append(", Serializer=")
          .append(ensureSerializer().getClass().getName())
          .append(", PingInterval=")
          .append(getPingInterval())
          .append(", PingTimeout=")
          .append(getPingTimeout())
          .append(", RequestTimeout=")
          .append(getRequestTimeout())
          .append(", MaxIncomingMessageSize=")
          .append(getMaxIncomingMessageSize())
          .append(", MaxOutgoingMessageSize=")
          .append(getMaxOutgoingMessageSize());
        
        return sb.toString();
        }
    
    // Accessor for the property "LicenseError"
    /**
     * Getter for property LicenseError.<p>
    * An optional license check exception message.
    * 
    * @see _initStatic()
     */
    public static String getLicenseError()
        {
        return __s_LicenseError;
        }
    
    // Accessor for the property "MaxIncomingMessageSize"
    /**
     * Getter for property MaxIncomingMessageSize.<p>
    * The size limit of an incoming message.
     */
    public int getMaxIncomingMessageSize()
        {
        return __m_MaxIncomingMessageSize;
        }
    
    // Accessor for the property "MaxOutgoingMessageSize"
    /**
     * Getter for property MaxOutgoingMessageSize.<p>
    * The size limit of an outgoing message.
     */
    public int getMaxOutgoingMessageSize()
        {
        return __m_MaxOutgoingMessageSize;
        }
    
    // Accessor for the property "MessageFactory"
    /**
     * Getter for property MessageFactory.<p>
    * The MessageFactory used to create Messages processed by this Service.
     */
    protected com.tangosol.net.messaging.Protocol.MessageFactory getMessageFactory()
        {
        // import com.tangosol.net.messaging.Protocol as com.tangosol.net.messaging.Protocol;
        
        com.tangosol.net.messaging.Protocol protocol = getProtocol();
        return protocol.getMessageFactory(protocol.getCurrentVersion());
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public String getName()
        {
        return getServiceName();
        }
    
    // Accessor for the property "ParentService"
    /**
     * Getter for property ParentService.<p>
    * The optional parent com.tangosol.net.Service.
    * 
    * @see #assertIdentityToken
    * @see #generateIdentityToken
     */
    public com.tangosol.net.Service getParentService()
        {
        return __m_ParentService;
        }
    
    // Accessor for the property "PingInterval"
    /**
     * Getter for property PingInterval.<p>
    * The number of milliseconds between successive Connection "pings" or 0 if
    * heartbeats are disabled.
     */
    public long getPingInterval()
        {
        return __m_PingInterval;
        }
    
    // Accessor for the property "PingLastCheckMillis"
    /**
     * Getter for property PingLastCheckMillis.<p>
    * The last time the Connection(s) managed by this ConnectionManager were
    * checked for a "ping" timeout.
     */
    public long getPingLastCheckMillis()
        {
        return __m_PingLastCheckMillis;
        }
    
    // Accessor for the property "PingLastMillis"
    /**
     * Getter for property PingLastMillis.<p>
    * The last time the Connection(s) managed by this ConnectionManager were
    * "pinged".
     */
    public long getPingLastMillis()
        {
        return __m_PingLastMillis;
        }
    
    // Accessor for the property "PingNextCheckMillis"
    /**
     * Getter for property PingNextCheckMillis.<p>
    * The next time the Connection(s) managed by this ConnectionManager should
    * be checked for a "ping" timeout.
     */
    public long getPingNextCheckMillis()
        {
        long ldtLast = getPingLastMillis();
        long cMillis = getPingTimeout();
        
        return cMillis == 0L || ldtLast == 0L || getPingLastCheckMillis() > 0L
                ? Long.MAX_VALUE : ldtLast + cMillis;
        }
    
    // Accessor for the property "PingNextMillis"
    /**
     * Getter for property PingNextMillis.<p>
    * The next time the Connection(s) managed by this ConnectionManager should
    * be "pinged".
     */
    public long getPingNextMillis()
        {
        // import com.tangosol.util.Base;
        
        long ldtLast = getPingLastMillis();
        long cMillis = getPingInterval();
        
        return cMillis == 0L ? Long.MAX_VALUE :
               ldtLast == 0L ? Base.getSafeTimeMillis() : ldtLast + cMillis;
        }
    
    // Accessor for the property "PingTimeout"
    /**
     * Getter for property PingTimeout.<p>
    * The default request timeout for a PingRequest. A timeout of 0 is
    * interpreted as an infinite timeout. This property defaults to the value
    * of the RequestTimeout property.
     */
    public long getPingTimeout()
        {
        return __m_PingTimeout;
        }
    
    // Accessor for the property "ProcessId"
    /**
     * Getter for property ProcessId.<p>
    * The unique identifier (UUID) of the process using this ConnectionManager.
     */
    public static com.tangosol.util.UUID getProcessId()
        {
        return __s_ProcessId;
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    // Accessor for the property "Protocol"
    /**
     * Getter for property Protocol.<p>
     */
    public com.tangosol.net.messaging.Protocol getProtocol()
        {
        // import com.tangosol.net.messaging.Protocol as com.tangosol.net.messaging.Protocol;
        
        com.tangosol.net.messaging.Protocol protocol = __m_Protocol;
        if (protocol == null)
            {
            setProtocol(protocol = (com.tangosol.net.messaging.Protocol) _findChild("Protocol"));
            }
        return protocol;
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionManager
    // Accessor for the property "Protocol"
    /**
     * Getter for property Protocol.<p>
     */
    public com.tangosol.net.messaging.Protocol getProtocol(String sName)
        {
        // import com.tangosol.net.messaging.Protocol as com.tangosol.net.messaging.Protocol;
        
        return (com.tangosol.net.messaging.Protocol) getProtocolMap().get(sName);
        }
    
    // Accessor for the property "ProtocolMap"
    /**
     * Getter for property ProtocolMap.<p>
    * The Map of registered Protocol objects.
    * 
    * @see com.tangosol.net.messaging.ConnectionManager#getProtocol
     */
    public java.util.Map getProtocolMap()
        {
        return __m_ProtocolMap;
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionManager
    public java.util.Map getProtocols()
        {
        // import java.util.HashMap;
        // import java.util.Map;
        
        Map map = getProtocolMap();
        synchronized (map)
            {
            return new HashMap(map);
            }
        }
    
    // Accessor for the property "ProtocolVersionMap"
    /**
     * Getter for property ProtocolVersionMap.<p>
    * A Map of version ranges for registered Protocols. The keys are the names
    * of the Protocols and the values are two element Integer arrays, the first
    * element being the current version and the second being the supported
    * version of the corresponding Protocol.
     */
    public java.util.Map getProtocolVersionMap()
        {
        return __m_ProtocolVersionMap;
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionManager
    public com.tangosol.net.messaging.Channel.Receiver getReceiver(String sName)
        {
        // import com.tangosol.net.messaging.Channel$Receiver as com.tangosol.net.messaging.Channel.Receiver;
        
        return (com.tangosol.net.messaging.Channel.Receiver) getReceiverMap().get(sName);
        }
    
    // Accessor for the property "ReceiverMap"
    /**
     * Getter for property ReceiverMap.<p>
    * The Map of registered Receiver objects.
    * 
    * @see com.tangosol.net.messaging.ConnectionManager#getRecievers
     */
    public java.util.Map getReceiverMap()
        {
        return __m_ReceiverMap;
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionManager
    public java.util.Map getReceivers()
        {
        // import java.util.HashMap;
        // import java.util.Map;
        
        Map map = getReceiverMap();
        synchronized (map)
            {
            return new HashMap(map);
            }
        }
    
    // Accessor for the property "RequestTimeout"
    /**
     * Getter for property RequestTimeout.<p>
    * The default request timeout for all Channel objects created by Connection
    * objects managed by this ConnectionManager. A timeout of 0 is interpreted
    * as an infinite timeout.
    * 
    * @see Connection#getRequestTimeout
     */
    public long getRequestTimeout()
        {
        return __m_RequestTimeout;
        }
    
    // Accessor for the property "StatsBytesReceived"
    /**
     * Getter for property StatsBytesReceived.<p>
    * Statistics: total number of bytes received.
     */
    public long getStatsBytesReceived()
        {
        return __m_StatsBytesReceived;
        }
    
    // Accessor for the property "StatsBytesSent"
    /**
     * Getter for property StatsBytesSent.<p>
    * Statistics: total number of bytes sent.
     */
    public long getStatsBytesSent()
        {
        return __m_StatsBytesSent;
        }
    
    // Accessor for the property "StatsSent"
    /**
     * Getter for property StatsSent.<p>
    * Statistics: total number of messages sent.
     */
    public long getStatsSent()
        {
        return __m_StatsSent;
        }
    
    // Accessor for the property "StatsTimeoutCount"
    /**
     * Getter for property StatsTimeoutCount.<p>
    * The total number of timed-out requests since the last time the statistics
    * were reset.
    * 
    * @volatile
     */
    public long getStatsTimeoutCount()
        {
        return __m_StatsTimeoutCount;
        }
    
    // Declared at the super level
    /**
     * Getter for property WaitMillis.<p>
    * The number of milliseconds that the daemon will wait for notification.
    * Zero means to wait indefinitely. Negative value means to skip waiting
    * altogether.
    * 
    * @see #onWait
     */
    public long getWaitMillis()
        {
        // import com.tangosol.util.Base;
        
        long cMillis = super.getWaitMillis();
        if (getPingInterval() > 0L)
            {
            long ldtNow  = Base.getLastSafeTimeMillis();
            long ldtNext = Math.min(getPingNextMillis(), getPingNextCheckMillis());
            long cNext   = ldtNext > ldtNow ? ldtNext - ldtNow : -1L;
        
            cMillis = cMillis == 0L ? cNext : Math.min(cNext, cMillis);
            }
        if (isGuardian())
            {
            // a Guardian must wake up on a regular intervals to manage its Guardables
            long ldtNow       = Base.getLastSafeTimeMillis();
            long cGuardMillis = Math.max(1L, getGuardSupport().getNextCheckTime() - ldtNow);
        
            cMillis = cMillis == 0L ? cGuardMillis : Math.min(cMillis, cGuardMillis);
            }
        else if (isGuarded())
            {
            // a guarded Daemon must wake up periodically (at least once a second)
            cMillis = cMillis == 0L ? 1000L : Math.min(cMillis, 1000L);
            }
        return cMillis;
        }
    
    // Accessor for the property "WrapperStreamFactoryList"
    /**
     * Getter for property WrapperStreamFactoryList.<p>
    * A List of WrapperStreamFactory objects that affect how Messages are
    * written and read.
     */
    public java.util.List getWrapperStreamFactoryList()
        {
        return __m_WrapperStreamFactoryList;
        }
    
    // Declared at the super level
    public com.tangosol.net.Guardian.GuardContext guard(com.tangosol.net.Guardable guardable)
        {
        // import com.tangosol.net.Guardian;
        // import com.tangosol.net.Service as com.tangosol.net.Service;
        
        // COH-4604: If the peer is being used by a parent service (e.g. ProxyService),
        //           delegate the guardian duties to that service
        com.tangosol.net.Service service = getParentService();
        if (service instanceof Guardian)
            {
            return ((Guardian) service).guard(guardable);
            }
        return super.guard(guardable);
        }
    
    // Declared at the super level
    public com.tangosol.net.Guardian.GuardContext guard(com.tangosol.net.Guardable guardable, long cMillis, float flPctRecover)
        {
        // import com.tangosol.net.Guardian;
        // import com.tangosol.net.Service as com.tangosol.net.Service;
        
        // COH-4604: If the peer is being used by a parent service (e.g. ProxyService),
        //           delegate the guardian duties to that service
        com.tangosol.net.Service service = getParentService();
        if (service instanceof Guardian)
            {
            return ((Guardian) service).guard(guardable, cMillis, flPctRecover);
            }
        return super.guard(guardable, cMillis, flPctRecover);
        }
    
    /**
     * Factory method: create a new Connection.
    * 
    * Implementations must configure the Connection with a reference to this
    * ConnectionManager (see Connection#setConnectionManager).
    * 
    * @return a new Connection object that has yet to be opened
     */
    protected com.tangosol.coherence.component.net.extend.Connection instantiateConnection()
        {
        return null;
        }
    
    // Accessor for the property "DEBUG"
    /**
     * Getter for property DEBUG.<p>
    * Debug flag. The value of this flag is set using the following system
    * property:
    * 
    * tangosol.coherence.messaging.debug
    * 
    * or it can be dynamically set through the MessagingDebug attribute of the
    * ConnectionManagerMBean.
     */
    public static boolean isDEBUG()
        {
        return __s_DEBUG;
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    /**
     * Called after a Channel has been closed.  When called as the
    * ConnectionManager for the Channel, this method will be called on the
    * service thread.
    * 
    * @param channel the Channel that has been closed
     */
    public void onChannelClosed(com.tangosol.net.messaging.Channel channel)
        {
        if (channel.getId() != 0 && _isTraceEnabled(6))
            {
            _trace("Closed: " + channel, 6);
            }
        }
    
    /**
     * Called after a Channel has been opened. This method is called on the
    * service thread.
    * 
    * @param channel the Channel that has been opened
     */
    public void onChannelOpened(com.tangosol.coherence.component.net.extend.Channel channel)
        {
        if (channel.getId() != 0 && _isTraceEnabled(6))
            {
            _trace("Opened: " + channel, 6);
            }
        }
    
    /**
     * Called after a Connection has closed. This method is called on the
    * service thread.
    * 
    * @param connection  the Connection that was closed
     */
    public void onConnectionClosed(com.tangosol.coherence.component.net.extend.Connection connection)
        {
        // import com.tangosol.net.messaging.ConnectionEvent;
        
        if (get_Connection() == connection)
            {
            return;
            }
        
        dispatchConnectionEvent(connection, ConnectionEvent.CONNECTION_CLOSED, null);
        
        if (_isTraceEnabled(6))
            {
            _trace("Closed: " + connection, 6);
            }
        }
    
    /**
     * Called after a Connection is closed due to an error or exception. This
    * method is called on the service thread.
    * 
    * @param connection  the Connection that was closed
    * @param e  the reason the Connection was closed
     */
    public void onConnectionError(com.tangosol.coherence.component.net.extend.Connection connection, Throwable e)
        {
        // import com.tangosol.net.messaging.ConnectionEvent;
        
        if (get_Connection() == connection)
            {
            return;
            }
        
        dispatchConnectionEvent(connection, ConnectionEvent.CONNECTION_ERROR, e);

        _trace("Closed: " + connection + " due to: " + e, 6);
        }
    
    /**
     * Called after a Connection has been successfully established. This method
    * is called on the service thread.
    * 
    * @param connection  the Connection that was opened
     */
    public void onConnectionOpened(com.tangosol.coherence.component.net.extend.Connection connection)
        {
        // import com.tangosol.net.messaging.ConnectionEvent;
        
        if (get_Connection() == connection)
            {
            return;
            }
        
        dispatchConnectionEvent(connection, ConnectionEvent.CONNECTION_OPENED, null);
        
        if (_isTraceEnabled(6))
            {
            _trace("Opened: " + connection, 6);
            }
        }
    
    // Declared at the super level
    /**
     * This event occurs when dependencies are injected into the component. 
    * First, call super.onDependencies to allow all super components to process
    * the Dependencies.  Each component is free to chose how it consumes
    * dependencies from within onDependencies.  Often (though not ideal), the 
    * dependencies are copied into the component's properties.  This technique
    * isolates Dependency Injection from the rest of the component code since
    * components continue to access properties just as they did before. 
    * 
    * PartitionedCacheDependencies deps = (PartitionedCacheDependencies)
    * getDependencies();
    * deps.getFoo();
     */
    protected void onDependencies(com.tangosol.net.ServiceDependencies deps)
        {
        // import com.tangosol.internal.net.service.peer.PeerDependencies;
        
        super.onDependencies(deps);
        
        PeerDependencies peerDeps = (PeerDependencies) deps;
        
        setWrapperStreamFactoryList(peerDeps.getFilterList());
        setCodec(peerDeps.getMessageCodec());
        setPingInterval(peerDeps.getPingIntervalMillis());
        setPingTimeout(peerDeps.getPingTimeoutMillis());
        setRequestTimeout(peerDeps.getRequestTimeoutMillis());
        setMaxIncomingMessageSize(peerDeps.getMaxIncomingMessageSize());
        setMaxOutgoingMessageSize(peerDeps.getMaxOutgoingMessageSize());
        }
    
    // Declared at the super level
    /**
     * Event notification called once the daemon's thread starts and before the
    * daemon thread goes into the "wait - perform" loop. Unlike the
    * <code>onInit()</code> event, this method executes on the daemon's thread.
    * 
    * Note1: this method is called while the caller's thread is still waiting
    * for a notification to  "unblock" itself.
    * Note2: any exception thrown by this method will terminate the thread
    * immediately
     */
    protected void onEnter()
        {
        // import Component.Net.Extend.Channel;
        // import com.tangosol.util.Base;
        
        // open the internal Connection and Channel
        get_Connection().openInternal();
        
        setStartTimestamp(Base.getSafeTimeMillis());
        
        resetStats();
        
        setServiceState(SERVICE_STARTING);
        
        Channel channel = get_Channel();
        channel.send(channel.createMessage(Peer.MessageFactory.NotifyStartup.TYPE_ID));
        }
    
    // Declared at the super level
    /**
     * Event notification called right before the daemon thread terminates. This
    * method is guaranteed to be called only once and on the daemon's thread.
     */
    protected void onExit()
        {
        super.onExit();
        
        get_Connection().closeInternal(false, null, -1L);
        }
    
    // Declared at the super level
    /**
     * The "component has been initialized" method-notification (kind of
    * WM_NCCREATE event) called out of setConstructed() for the topmost
    * component and that in turn notifies all the children. <p>
    * 
    * This notification gets called before the control returns back to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as)  the control returns back to the
    * instatiator and possibly on a different thread.
     */
    public void onInit()
        {
        // import Component.Net.Extend.Channel;
        // import Component.Net.Extend.Connection;
        // import com.tangosol.license.LicenseException;
        // import com.tangosol.net.messaging.Protocol as com.tangosol.net.messaging.Protocol;
        // import java.util.Collections;
        
        if (getLicenseError() != null)
            {
            // see #_initStatic()
            throw new LicenseException("cannot instantiate " + get_Name() + ": "
                    + getLicenseError());
            }
        
        // add the MessagingProtocol
        com.tangosol.net.messaging.Protocol protocol = getProtocol();
        registerProtocol(protocol);
        
        // initialize the internal Connection and Channel
        Connection connection = new Connection();
        connection.setConnectionManager(this);
        connection.setId(getProcessId());
        connection.setMessageFactoryMap(Collections.singletonMap(protocol.getName(),
                protocol.getMessageFactory(protocol.getCurrentVersion())));
        
        Channel channel = (Channel) connection.getChannel(0);
        channel.setReceiver(this);
        
        set_Channel(channel);
        set_Connection(connection);
        
        super.onInit();
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    /**
     * Called on the service thread.
     */
    public void onMessage(com.tangosol.net.messaging.Message message)
        {
        message.run();
        }
    
    /**
     * Called when an exception occurs during Message decoding. This method is
    * called on the service thread.
    * 
    * @param e  the Throwable thrown during decoding
    * @param in  the BufferInput that contains the encoded Message
    * @param connection  the Connection that received the encoded Message
    * @param fFilter  true iff the BufferInput was filtered using the list of
    * configured WrapperStreamFactory objects
    * 
    * @see #onNotify
     */
    protected void onMessageDecodeException(Throwable e, com.tangosol.io.ReadBuffer.BufferInput in, com.tangosol.coherence.component.net.extend.Connection connection, boolean fFilter)
        {
        // import Component.Net.Extend.Channel;
        // import Component.Net.Extend.Connection;
        // import java.io.IOException;
        
        // resolve the Channel
        Channel channel;
        try
            {
            // filter the input, if necessary
            if (fFilter)
                {
                in = filterBufferInput(in);
                }
            channel = (Channel) connection.getChannel(in.readPackedInt());
            }
        catch (IOException ee)
            {
            channel = null;
            }
        
        StringBuffer sb = new StringBuffer("An exception occurred while decoding a Message for Service \"")
               .append(getServiceName())
               .append("\" received from: ")
               .append(connection).append('.');
        
        if (channel != null)
           {
           sb.append(" This service is configured with a serializer: ")
             .append(channel.getSerializer())
             .append(", which may be incompatible with the serializer configured by the connecting client.");
           }
        
        _trace(e, sb.toString());
        
        // close the Channel or Connection
        if (channel == null || !channel.isOpen() || channel.getId() == 0)
            {
            connection.close(true, e, false);
            }
        else
            {
            channel.close(true, e);
            }
        }
    
    /**
     * Called when an exception occurs during Message encoding. This method is
    * called on both client and service threads.
    * 
    * @param e  the Throwable thrown during encoding
    * @param message  the Message being encoded
    * 
    * @see #send
     */
    protected void onMessageEncodeException(Throwable e, com.tangosol.net.messaging.Message message)
        {
        // import Component.Net.Extend.Channel;
        // import Component.Net.Extend.Connection;
        // import com.tangosol.util.ClassHelper;
        
        _trace(e, "An exception occurred while encoding a "
                + ClassHelper.getSimpleName(message.getClass())
                + " for Service=" + getServiceName());
        
        // close the Channel or Connection
        Channel channel = (Channel) message.getChannel();
        if (!channel.isOpen() || channel.getId() == 0)
            {
            Connection connection = (Connection) channel.getConnection();
        
            // see #send and Channel#receive
            connection.setCloseOnExit(true);
            connection.setCloseNotify(true);
            connection.setCloseThrowable(e);
            }
        else
            {
            // see #send and Channel#receive
            channel.setCloseOnExit(true);
            channel.setCloseNotify(true);
            channel.setCloseThrowable(e);
            }
        }
    
    // Declared at the super level
    /**
     * Event notification to perform a regular daemon activity. To get it
    * called, another thread has to set Notification to true:
    * <code>daemon.setNotification(true);</code>
    * 
    * @see #onWait
     */
    protected void onNotify()
        {
        // import Component.Net.Extend.Channel;
        // import Component.Net.Extend.Connection;
        // import Component.Util.DaemonPool as com.tangosol.coherence.component.util.DaemonPool;
        // import com.tangosol.io.ReadBuffer;
        // import com.tangosol.net.messaging.Message;
        // import com.tangosol.util.Base;
        // import java.io.IOException;
        
        long       ldtStart  = Base.getSafeTimeMillis();
        long       cMessage  = getStatsReceived();
        long       cbReceive = getStatsBytesReceived();
        com.tangosol.coherence.component.util.DaemonPool pool      = getDaemonPool();
        
        while (!isExiting())
            {
            // issue/check heartbeats on every 512th message.
            // ("modulo" is much more expensive than "and")
            if ((cMessage & 0x1FFL) == 0x1FFL)
                {
                heartbeat();
                }
        
            Message message = (Message) getQueue().removeNoWait();
            if (message == null)
                {
                break;
                }
            else
                {
                cMessage++;
                }
        
            // decode the Message if necessary
            if (message instanceof Peer.MessageFactory.EncodedMessage)
                {
                Peer.MessageFactory.EncodedMessage messageImpl =
                        (Peer.MessageFactory.EncodedMessage) message;
        
                ReadBuffer rb = messageImpl.getReadBuffer();
                if (rb == null || rb.length() == 0)
                    {
                    continue;
                    }
                int cb = rb.length();
        
                // update stats
                Connection connection = messageImpl.getConnection();
                connection.setStatsBytesReceived(connection.getStatsBytesReceived() + cb);
                cbReceive += cb;
        
                // decode the Message
                try
                    {
                    message = decodeMessage(rb.getBufferInput(), connection, true);
                    }
                catch (Throwable e)
                    {
                    onMessageDecodeException(e, rb.getBufferInput(), connection, true);
                    continue;
                    }
                finally
                    {
                    releaseReadBuffer(rb);
                    }
        
                if (message == null)
                    {
                    continue;
                    }
        
                if ((isDEBUG() || connection.isMessagingDebug()) && _isTraceEnabled(6))
                    {
                    _trace("Received: " + message, 6);
                    }
                }
        
            // make sure the target Channel is still open
            Channel channel = (Channel) message.getChannel();
            if (channel == null || !channel.isOpen())
                {
                continue;
                }
        
            // make sure the target Connection is still open
            Connection connection = (Connection) channel.getConnection();
            if (connection == null || !connection.isOpen())
                {
                continue;
                }
        
            // update stats
            connection.setStatsReceived(connection.getStatsReceived() + 1);
        
            // execute the Message
            if (this == channel.getReceiver() ||
                !pool.isStarted()             ||
                message.isExecuteInOrder())
                {
                // (1) the Message is an internal Message; or
                // (2) the Message is a "Channel0" Message; or
                // (3) the daemon pool has not been started; or
                // (4) the Message calls for in-order execution
                channel.receive(message);
                }
            else
                {
                pool.add(message);
                }
            }
        
        // heartbeat
        long ldtNow = Base.getSafeTimeMillis();
        if (ldtNow >= getPingNextCheckMillis())
            {
            checkPingTimeouts();
            setPingLastCheckMillis(ldtNow);
            }
        if (ldtNow >= getPingNextMillis())
            {
            ping();
            setPingLastCheckMillis(0L);
            setPingLastMillis(ldtNow);
            }
        
        setStatsReceived(cMessage);
        setStatsBytesReceived(cbReceive);
        setStatsCpu(getStatsCpu() + (ldtNow - ldtStart));
        
        super.onNotify();
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to true.
    * If the Service has not completed preparing at this point, then the
    * Service must override this implementation and only set AcceptingClients
    * to true when the Service has actually "finished starting".
     */
    public void onServiceStarted()
        {
        // import Component.Util.DaemonPool as com.tangosol.coherence.component.util.DaemonPool;
        
        // start the daemon pool if necessary
        com.tangosol.coherence.component.util.DaemonPool pool = getDaemonPool();
        if (pool.getDaemonCount() > 0)
            {
            pool.setThreadGroup(new ThreadGroup(getServiceName()));
            pool.start();
            }
        
        // open for business
        setAcceptingClients(true);
        
        _trace("Started: " + this, 6);
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method does nothing.
     */
    protected void onServiceStarting()
        {
        // import Component.Net.Extend.Codec;
        // import com.tangosol.net.messaging.Protocol as com.tangosol.net.messaging.Protocol;
        // import java.util.Collections;
        // import java.util.HashMap;
        // import java.util.Iterator;
        // import java.util.Map;
        
        super.onServiceStarting();
        
        // make sure a Codec is set up
        if (getCodec() == null)
            {
            setCodec(new Codec());
            }
        
        // set up the com.tangosol.net.messaging.Protocol version map
        Map map = new HashMap();
        for (Iterator iter = getProtocolMap().values().iterator(); iter.hasNext(); )
            {
            com.tangosol.net.messaging.Protocol protocol        = (com.tangosol.net.messaging.Protocol) iter.next();
            String   sName           = protocol.getName();
            int      nVersionCurrent = protocol.getCurrentVersion();
            int      nVersionSupport = protocol.getSupportedVersion();
        
            if (sName == null)
                {
                throw new IllegalArgumentException("protocol has no name: "
                        + protocol);
                }
        
            map.put(sName, new Integer[] { Integer.valueOf(nVersionCurrent),
                    Integer.valueOf(nVersionSupport) });
            }
        setProtocolVersionMap(Collections.unmodifiableMap(map));
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to false.
     */
    protected void onServiceStopped()
        {
        super.onServiceStopped();
        
        _trace("Stopped: " + this, 6);
        }
    
    /**
     * Open a new Channel. This method is called by Connection.openChannel() and
    * is always run on client threads.
    * 
    * @see com.tangosol.net.messaging.Connection#openChannel
     */
    public com.tangosol.net.messaging.Channel openChannel(com.tangosol.coherence.component.net.extend.Connection connection, com.tangosol.net.messaging.Protocol protocol, String sName, ClassLoader loader, com.tangosol.net.messaging.Channel.Receiver receiver, javax.security.auth.Subject subject)
        {
        // import com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        // import com.tangosol.net.messaging.Request$Status as com.tangosol.net.messaging.Request.Status;
        
        _assert(connection != null);
        
        Channel channel0 = get_Channel();
        com.tangosol.net.messaging.Protocol.MessageFactory factory0 = channel0.getMessageFactory();
        
        Peer.MessageFactory.OpenChannel request = (Peer.MessageFactory.OpenChannel)
                factory0.createMessage(Peer.MessageFactory.OpenChannel.TYPE_ID);
        
        request.setClassLoader(loader);
        request.setConnection(connection);
        request.setIdentityToken(serializeIdentityToken(generateIdentityToken(subject)));
        request.setProtocol(protocol);
        request.setReceiver(receiver);
        request.setReceiverName(sName);
        request.setSubject(subject);
        
        com.tangosol.net.messaging.Request.Status status = (com.tangosol.net.messaging.Request.Status) channel0.request(request);
        
        Peer.MessageFactory.OpenChannelResponse response = (Peer.MessageFactory.OpenChannelResponse)
                status.waitForResponse(getRequestTimeout());
        
        return (Channel) response.getResult();
        }
    
    /**
     * Open the given Connection. This method is called by Connection.open() and
    * is always run on client threads.
    * 
    * @param connection  the Connection to open
     */
    public void openConnection(com.tangosol.coherence.component.net.extend.Connection connection)
        {
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        // import com.tangosol.net.messaging.Request$Status as com.tangosol.net.messaging.Request.Status;
        
        _assert(connection != null);
        
        Channel channel0 = get_Channel();
        com.tangosol.net.messaging.Protocol.MessageFactory factory0 = channel0.getMessageFactory();
        
        Peer.MessageFactory.OpenConnection request = (Peer.MessageFactory.OpenConnection)
                factory0.createMessage(Peer.MessageFactory.OpenConnection.TYPE_ID);
        
        request.setConnectionOpen(connection);
        
        com.tangosol.net.messaging.Request.Status status = (com.tangosol.net.messaging.Request.Status) channel0.request(request);
        if (status != null)
            {
            try
                {
                status.waitForResponse(getRequestTimeout());
                }
            catch (RequestTimeoutException e)
                {
                connection.close(false, e);
                throw e;
                }
            }
        }
    
    /**
     * Parse the String value of the child XmlElement with the given name as a
    * memory size in bytes. If the specified child XmlElement does not exist or
    * is empty, the specified default value is returned.
    * 
    * @param xml  the parent XmlElement
    * @param sName  the name of the child XmlElement
    * @param cbDefault  the default value
    * 
    * @return the memory size (in bytes) represented by the specified child
    * XmlElement
     */
    protected static long parseMemorySize(com.tangosol.run.xml.XmlElement xml, String sName, long cbDefault)
        {
        // import com.tangosol.util.Base;
        
        if (xml == null)
            {
            return cbDefault;
            }
        
        String sBytes = xml.getSafeElement(sName).getString();
        if (sBytes.length() == 0)
            {
            return cbDefault;
            }
        
        try
            {
            return Base.parseMemorySize(sBytes);
            }
        catch (RuntimeException e)
            {
            throw Base.ensureRuntimeException(e, "illegal \"" + sName + "\" value: " + sBytes);
            }
        }
    
    /**
     * Ping the Connection(s) managed by this ConnectionManager.
     */
    protected void ping()
        {
        }
    
    /**
     * Handle the given Message by either adding it to the service thread queue
    * (internal Messages) or sending asynchronously (external Messages). This
    * method is called on both client and service threads.
    * 
    * @param message  the Message to post
     */
    public void post(com.tangosol.net.messaging.Message message)
        {
        // import com.tangosol.net.messaging.Channel as com.tangosol.net.messaging.Channel;
        
        _assert(message != null);
        
        com.tangosol.net.messaging.Channel channel = message.getChannel();
        _assert(channel != null);
        
        if (this == channel.getReceiver() && message.getTypeId() < 0)
            {
            // internal message
            getQueue().add(message);
            }
        else
            {
            // external message
            send(message);
            }
        }
    
    /**
     * Called by the underlying transport when an encoded Message is received.
    * Called on client threads.
    * 
    * @param rb  the ReadBuffer that contains the encoded Message
    * @param connection  the Connection that received the encoded Message
     */
    public void receive(com.tangosol.io.ReadBuffer rb, com.tangosol.coherence.component.net.extend.Connection connection)
        {
        // import com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        
        _assert(rb != null);
        _assert(connection != null);
        
        Channel channel0 = get_Channel();
        if (channel0 == null)
            {
            return;
            }
        
        com.tangosol.net.messaging.Protocol.MessageFactory factory0 = channel0.getMessageFactory();
        if (factory0 == null)
            {
            return;
            }
        
        Peer.MessageFactory.EncodedMessage message = (Peer.MessageFactory.EncodedMessage)
                factory0.createMessage(Peer.MessageFactory.EncodedMessage.TYPE_ID);
        
        message.setChannel(channel0);
        message.setConnection(connection);
        message.setReadBuffer(rb);
        
        post(message);
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public void registerChannel(com.tangosol.net.messaging.Channel channel)
        {
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionManager
    public void registerProtocol(com.tangosol.net.messaging.Protocol protocol)
        {
        if (getServiceState() > SERVICE_INITIAL)
            {
            throw new IllegalStateException();
            }
        
        if (protocol == null)
            {
            throw new IllegalArgumentException("protocol cannot be null");
            }
        
        String sName = protocol.getName();
        if (sName == null)
            {
            throw new IllegalArgumentException("missing protocol name: " + protocol);
            }
        
        getProtocolMap().put(sName, protocol);
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionManager
    public void registerReceiver(com.tangosol.net.messaging.Channel.Receiver receiver)
        {
        if (getServiceState() > SERVICE_INITIAL)
            {
            throw new IllegalStateException();
            }
        
        if (receiver == null)
            {
            throw new IllegalArgumentException("receiver cannot be null");
            }
        
        String sName = receiver.getName();
        if (sName == null)
            {
            throw new IllegalArgumentException("missing receiver name: " + receiver);
            }
        
        getReceiverMap().put(sName, receiver);
        }
    
    /**
     * Release a ReadBuffer that contains an encode a Message. This method is
    * called on the service thread.
    * 
    * @param rb  the ReadBuffer to release
     */
    protected void releaseReadBuffer(com.tangosol.io.ReadBuffer rb)
        {
        }
    
    /**
     * Release a WriteBuffer that was used to encode a Message. This method is
    * called on both client and service threads.
    * 
    * @param wb  the WriteBuffer to release
    * @param e  if not null, the WriteBuffer is being released due to a
    * decoding or send error
    * 
    * @see #allocateWriteBufer
     */
    protected void releaseWriteBuffer(com.tangosol.io.WriteBuffer wb, Throwable e)
        {
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionManager
    public void removeConnectionListener(com.tangosol.net.messaging.ConnectionListener l)
        {
        getConnectionListeners().remove(l);
        }
    
    // Declared at the super level
    /**
     * Reset the Service statistics.
     */
    public void resetStats()
        {
        setStatsBytesReceived(0L);
        setStatsBytesSent(0L);
        setStatsSent(0L);
        setStatsTimeoutCount(0L);
        
        super.resetStats();
        }
    
    /**
     * Perform an asynchronous send of the given Message using the underlying
    * transport. This method is called on both client and service threads.
    * 
    * @see Channel#post and Channel#receive
    * 
    * @param message the Message to send
     */
    protected void send(com.tangosol.net.messaging.Message message)
        {
        // import Component.Net.Extend.Channel;
        // import Component.Net.Extend.Connection;
        // import com.tangosol.io.WriteBuffer;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.util.Base;
        
        Channel channel = (Channel) message.getChannel();
        _assert(channel != null);
        _assert(channel.isActiveThread());
        
        // cache the connection earlier than necessary as it's needed
        // to query the message debug configuration
        Connection connection = (Connection) channel.getConnection();
        boolean    fMsgDebug  = (isDEBUG() || connection.isMessagingDebug()) && _isTraceEnabled(6);
        String     sDebugMsg  = null;
        
        // if messaging debug enabled, cache the result of toString() on message *before* serialization
        // to ensure all state present when creating the message
        if (fMsgDebug)
            {
            sDebugMsg = message.toString();
            }
        
        // allocate a WriteBuffer
        WriteBuffer wb = allocateWriteBuffer();
        
        // encode the Message
        try
            {
            encodeMessage(message, wb.getBufferOutput(), true);
            enforceMaxOutgoingMessageSize(wb.length());
            }
        catch (Throwable e)
            {
            releaseWriteBuffer(wb, e);
            onMessageEncodeException(e, message);
            throw Base.ensureRuntimeException(e);
            }
        
        // send the Message
        try
            {
            connection.send(wb);
            releaseWriteBuffer(wb, null);
            }
        // a RequestTimeoutException may be thrown by send() if
        // a lock to gain access to the output stream cannot
        // be obtained before the request timeout; in this case
        // the connection does not need to be closed
        catch (RequestTimeoutException e)
            {
            releaseWriteBuffer(wb, e);
            throw e;
            }
        catch (Throwable e)
            {
            releaseWriteBuffer(wb, e);
        
            // see Channel#post
            connection.setCloseOnExit(true);
            connection.setCloseNotify(false);
            connection.setCloseThrowable(e);
        
            throw Base.ensureRuntimeException(e);
            }
        
        // update stats
        setStatsSent(getStatsSent() + 1);
        setStatsBytesSent(getStatsBytesSent() + wb.length());
        
        if (fMsgDebug)
            {
            _trace("Sent: " + sDebugMsg, 6);
            }
        }
    
    /**
     * Serialize an identity token.
    * 
    * @param oToken  the identity token object to serialize
    * 
    * @return the serialized token
    * 
    * @throws SecurityException on a serialization error
     */
    public byte[] serializeIdentityToken(Object oToken)
        {
        // import com.tangosol.io.ByteArrayWriteBuffer;
        
        if (oToken != null)
            {
            try
                {
                ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(1024);
                ensureSerializer().serialize(buf.getBufferOutput(), oToken);
                return buf.toByteArray();
                }
            catch (Exception e)
                {
                _trace(e, "An exception occurred while serializing an identity token");
                throw new SecurityException("unable to produce identity token");
                }
            }
        
        return null;
        }
    
    // Accessor for the property "_Channel"
    /**
     * Setter for property _Channel.<p>
    * The Channel used for all internal communication.
     */
    protected void set_Channel(com.tangosol.coherence.component.net.extend.Channel channel)
        {
        __m__Channel = channel;
        }
    
    // Accessor for the property "_Connection"
    /**
     * Setter for property _Connection.<p>
    * The Connection used for all internal communication.
     */
    protected void set_Connection(com.tangosol.coherence.component.net.extend.Connection channel)
        {
        __m__Connection = channel;
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionManager
    // Accessor for the property "Codec"
    /**
     * Setter for property Codec.<p>
    * The Codec used to encode and decode all Messages sent by this
    * ConnectionManager.
    * 
    * @see com.tangosol.net.messaging.ConnectionManager#getCodec
     */
    public void setCodec(com.tangosol.net.messaging.Codec codec)
        {
        __m_Codec = codec;
        }
    
    // Accessor for the property "ConnectionListeners"
    /**
     * Setter for property ConnectionListeners.<p>
    * The list of registered ConnectionListeners.
    * 
    * @see #addConnectionListener and #removeConnectionListener
     */
    private void setConnectionListeners(com.tangosol.util.Listeners listeners)
        {
        __m_ConnectionListeners = listeners;
        }
    
    // Accessor for the property "DEBUG"
    /**
     * Setter for property DEBUG.<p>
    * Debug flag. The value of this flag is set using the following system
    * property:
    * 
    * tangosol.coherence.messaging.debug
    * 
    * or it can be dynamically set through the MessagingDebug attribute of the
    * ConnectionManagerMBean.
     */
    public static void setDEBUG(boolean fDEBUG)
        {
        __s_DEBUG = fDEBUG;
        }
    
    // Accessor for the property "LicenseError"
    /**
     * Setter for property LicenseError.<p>
    * An optional license check exception message.
    * 
    * @see _initStatic()
     */
    protected static void setLicenseError(String sMsg)
        {
        __s_LicenseError = sMsg;
        }
    
    // Accessor for the property "MaxIncomingMessageSize"
    /**
     * Setter for property MaxIncomingMessageSize.<p>
    * The size limit of an incoming message.
     */
    protected void setMaxIncomingMessageSize(int cbSize)
        {
        __m_MaxIncomingMessageSize = cbSize;
        }
    
    // Accessor for the property "MaxOutgoingMessageSize"
    /**
     * Setter for property MaxOutgoingMessageSize.<p>
    * The size limit of an outgoing message.
     */
    protected void setMaxOutgoingMessageSize(int cbSize)
        {
        __m_MaxOutgoingMessageSize = cbSize;
        }
    
    // Accessor for the property "ParentService"
    /**
     * Setter for property ParentService.<p>
    * The optional parent com.tangosol.net.Service.
    * 
    * @see #assertIdentityToken
    * @see #generateIdentityToken
     */
    public void setParentService(com.tangosol.net.Service service)
        {
        __m_ParentService = service;
        }
    
    // Accessor for the property "PingInterval"
    /**
     * Setter for property PingInterval.<p>
    * The number of milliseconds between successive Connection "pings" or 0 if
    * heartbeats are disabled.
     */
    protected void setPingInterval(long cMillis)
        {
        __m_PingInterval = cMillis;
        }
    
    // Accessor for the property "PingLastCheckMillis"
    /**
     * Setter for property PingLastCheckMillis.<p>
    * The last time the Connection(s) managed by this ConnectionManager were
    * checked for a "ping" timeout.
     */
    protected void setPingLastCheckMillis(long ldt)
        {
        __m_PingLastCheckMillis = ldt;
        }
    
    // Accessor for the property "PingLastMillis"
    /**
     * Setter for property PingLastMillis.<p>
    * The last time the Connection(s) managed by this ConnectionManager were
    * "pinged".
     */
    protected void setPingLastMillis(long ldt)
        {
        __m_PingLastMillis = ldt;
        }
    
    // Accessor for the property "PingTimeout"
    /**
     * Setter for property PingTimeout.<p>
    * The default request timeout for a PingRequest. A timeout of 0 is
    * interpreted as an infinite timeout. This property defaults to the value
    * of the RequestTimeout property.
     */
    protected void setPingTimeout(long cMillis)
        {
        __m_PingTimeout = cMillis;
        }
    
    // Accessor for the property "ProcessId"
    /**
     * Setter for property ProcessId.<p>
    * The unique identifier (UUID) of the process using this ConnectionManager.
     */
    protected static void setProcessId(com.tangosol.util.UUID uuid)
        {
        _assert(uuid != null);
        
        __s_ProcessId = (uuid);
        }
    
    // Accessor for the property "Protocol"
    /**
     * Setter for property Protocol.<p>
     */
    protected void setProtocol(com.tangosol.net.messaging.Protocol protocol)
        {
        __m_Protocol = protocol;
        }
    
    // Accessor for the property "ProtocolMap"
    /**
     * Setter for property ProtocolMap.<p>
    * The Map of registered Protocol objects.
    * 
    * @see com.tangosol.net.messaging.ConnectionManager#getProtocol
     */
    protected void setProtocolMap(java.util.Map map)
        {
        __m_ProtocolMap = map;
        }
    
    // Accessor for the property "ProtocolVersionMap"
    /**
     * Setter for property ProtocolVersionMap.<p>
    * A Map of version ranges for registered Protocols. The keys are the names
    * of the Protocols and the values are two element Integer arrays, the first
    * element being the current version and the second being the supported
    * version of the corresponding Protocol.
     */
    protected void setProtocolVersionMap(java.util.Map map)
        {
        __m_ProtocolVersionMap = map;
        }
    
    // Accessor for the property "ReceiverMap"
    /**
     * Setter for property ReceiverMap.<p>
    * The Map of registered Receiver objects.
    * 
    * @see com.tangosol.net.messaging.ConnectionManager#getRecievers
     */
    protected void setReceiverMap(java.util.Map map)
        {
        __m_ReceiverMap = map;
        }
    
    // Accessor for the property "RequestTimeout"
    /**
     * Setter for property RequestTimeout.<p>
    * The default request timeout for all Channel objects created by Connection
    * objects managed by this ConnectionManager. A timeout of 0 is interpreted
    * as an infinite timeout.
    * 
    * @see Connection#getRequestTimeout
     */
    protected void setRequestTimeout(long cMillis)
        {
        __m_RequestTimeout = cMillis;
        }
    
    // Accessor for the property "StatsBytesReceived"
    /**
     * Setter for property StatsBytesReceived.<p>
    * Statistics: total number of bytes received.
     */
    protected void setStatsBytesReceived(long cb)
        {
        __m_StatsBytesReceived = cb;
        }
    
    // Accessor for the property "StatsBytesSent"
    /**
     * Setter for property StatsBytesSent.<p>
    * Statistics: total number of bytes sent.
     */
    protected void setStatsBytesSent(long cb)
        {
        __m_StatsBytesSent = cb;
        }
    
    // Accessor for the property "StatsSent"
    /**
     * Setter for property StatsSent.<p>
    * Statistics: total number of messages sent.
     */
    protected void setStatsSent(long pStatsSent)
        {
        __m_StatsSent = pStatsSent;
        }
    
    // Accessor for the property "StatsTimeoutCount"
    /**
     * Setter for property StatsTimeoutCount.<p>
    * The total number of timed-out requests since the last time the statistics
    * were reset.
    * 
    * @volatile
     */
    protected void setStatsTimeoutCount(long cRequests)
        {
        __m_StatsTimeoutCount = cRequests;
        }
    
    // Accessor for the property "WrapperStreamFactoryList"
    /**
     * Setter for property WrapperStreamFactoryList.<p>
    * A List of WrapperStreamFactory objects that affect how Messages are
    * written and read.
     */
    public void setWrapperStreamFactoryList(java.util.List list)
        {
        __m_WrapperStreamFactoryList = list;
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionManager
    // Declared at the super level
    /**
     * Stop the Service. The default implementation of this method simply calls
    * stop().
     */
    public synchronized void shutdown()
        {
        // import Component.Net.Extend.Channel;
        // import com.oracle.coherence.common.base.Blocking;
        // import com.tangosol.util.WrapperException;
        
        if (isStarted())
            {
            if (getServiceState() < SERVICE_STOPPING)
                {
                // send the request to shut down
                Channel channel = get_Channel();
                channel.send(channel.createMessage(Peer.MessageFactory.NotifyShutdown.TYPE_ID));
                }
            }
        
        Thread thread = getThread();
        if (thread != Thread.currentThread())
            {
            // wait for the service to stop or the thread to die
            while (isStarted() && getServiceState() < SERVICE_STOPPED)
                {
                try
                    {
                    Blocking.wait(this);
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    throw new WrapperException(e);
                    }
                }
        
            if (getServiceState() != SERVICE_STOPPED)
                {
                stop();
                }
            }
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public void unregisterChannel(com.tangosol.net.messaging.Channel channel)
        {
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$DaemonPool
    
    /**
     * DaemonPool is a class thread pool implementation for processing queued
     * operations on one or more daemon threads.
     * 
     * The designable properties are:
     *     AutoStart
     *     DaemonCount
     * 
     * The simple API for the DaemonPool is:
     *     public void start()
     *     public boolean isStarted()
     *     public void add(Runnable task)
     *     public void stop()
     * 
     * The advanced API for the DaemonPool is:
     *     DaemonCount property
     *     Daemons property
     *     Queues property
     *     ThreadGroup property
     * 
     * The DaemonPool is composed of two key components:
     * 
     * 1) An array of WorkSlot components that may or may not share Queues with
     * other WorkSlots. 
     * 
     * 2) An array of Daemon components feeding off the Queues. This collection
     * is accessed by the DaemonCount and Daemons properties, and is managed by
     * the DaemonCount mutator.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class DaemonPool
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool
        {
        // ---- Fields declarations ----
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
            __mapChildren.put("Daemon", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.Daemon.get_CLASS());
            __mapChildren.put("ResizeTask", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.ResizeTask.get_CLASS());
            __mapChildren.put("ScheduleTask", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.ScheduleTask.get_CLASS());
            __mapChildren.put("StartTask", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.StartTask.get_CLASS());
            __mapChildren.put("StopTask", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.StopTask.get_CLASS());
            __mapChildren.put("WorkSlot", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.WorkSlot.get_CLASS());
            __mapChildren.put("WrapperTask", Peer.DaemonPool.WrapperTask.get_CLASS());
            }
        
        // Default constructor
        public DaemonPool()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public DaemonPool(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setAbandonThreshold(8);
                setDaemonCountMax(2147483647);
                setDaemonCountMin(1);
                setScheduledTasks(new java.util.HashSet());
                setStatsTaskAddCount(new java.util.concurrent.atomic.AtomicLong());
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.DaemonPool();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$DaemonPool".replace('/', '.'));
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
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        /**
         * Factory method: create a new WrapperTask component.
         */
        protected com.tangosol.coherence.component.util.DaemonPool.WrapperTask instantiateWrapperTask()
            {
            // override the super, as it does not use virtual construction
            
            // instantiate directly to avoid reflection
            Peer.DaemonPool.WrapperTask task = new Peer.DaemonPool.WrapperTask();
            _linkChild(task);
            return task;
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$DaemonPool$WrapperTask
        
        /**
         * A task that is used to wrap the actual tasks.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class WrapperTask
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.WrapperTask
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public WrapperTask()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public WrapperTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.DaemonPool.WrapperTask();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$DaemonPool$WrapperTask".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            /**
             * Run the specified task.
            * 
            * @param task  the task to run
             */
            protected void run(Runnable task)
                {
                // import Component.Net.Extend.Channel;
                // import com.tangosol.net.messaging.Message;
                
                if (task instanceof Message)
                    {
                    Message message = (Message) task;
                    Channel channel = (Channel) message.getChannel();
                
                    channel.receive(message);
                    }
                else
                    {
                    task.run();
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$DispatchEvent
    
    /**
     * Runnable event.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class DispatchEvent
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DispatchEvent
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public DispatchEvent()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public DispatchEvent(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.DispatchEvent();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$DispatchEvent".replace('/', '.'));
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
        
        // Declared at the super level
        /**
         * import com.tangosol.net.MemberEvent;
        * 
        * ((MemberEvent) getEvent()).dispatch(getListeners());
         */
        public void run()
            {
            // import com.tangosol.net.messaging.ConnectionEvent;
            // import java.util.EventObject;
            
            EventObject evt = getEvent();
            if (evt instanceof ConnectionEvent)
                {
                ((ConnectionEvent) evt).dispatch(getListeners());
                }
            else
                {
                super.run();
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory
    
    /**
     * MessageFactory implementation for version 2 of the MessagingProtocol.
     * This MessageFactory contains Message classes necessary to manage the
     * lifecycle of Connections and Channels.
     *  
     * The type identifiers of the Message classes instantiated by this
     * MessageFactory are organized as follows:
     * 
     * Internal (<0):
     * 
     * (-1)  AcceptChannel
     * (-2)  CloseChannel
     * (-3)  CloseConnection
     * (-4)  CreateChannel
     * (-5)  NotifyShutdown
     * (-6)  NotifyStartup
     * (-7)  OpenChannel
     * (-8)  OpenConnection
     * (-9)  Response
     * (-10) EncodedMessage
     * 
     * Connection Lifecycle (0 - 10):
     * 
     * (0)  OpenConnectionResponse (*)
     * (1)  OpenConnectionRequest
     * (3)  PingRequest
     * (4)  PingResponse
     * (10) NotifyConnectionClosed
     * 
     * * The OpenConnectionResponse has type identifier 0 for historical
     * reasons. Prior to version 2 of the Messaging Protocol, all Request
     * messages used a common Response type with type identifier 0. Since the
     * first Response that a client expects to receive is an
     * OpenConnectionResponse, this allows version 2 and newer servers to
     * rejects connection attempts from version 1 clients.
     * 
     * Channel Lifecycle (11-20):
     * 
     * (11) OpenChannelRequest
     * (12) OpenChannelResponse
     * (13) AcceptChannelRequest
     * (14) AcceptChannelResponse
     * (20) NotifyChannelClosed
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static abstract class MessageFactory
            extends    com.tangosol.coherence.component.net.extend.MessageFactory
        {
        // ---- Fields declarations ----
        
        /**
         * Property MESSAGE_OFFSET
         *
         * Offset to allow for the internal (negative) Message types.
         */
        public static final int MESSAGE_OFFSET = 32;
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
            __mapChildren.put("AcceptChannel", Peer.MessageFactory.AcceptChannel.get_CLASS());
            __mapChildren.put("AcceptChannelRequest", Peer.MessageFactory.AcceptChannelRequest.get_CLASS());
            __mapChildren.put("AcceptChannelResponse", Peer.MessageFactory.AcceptChannelResponse.get_CLASS());
            __mapChildren.put("CloseChannel", Peer.MessageFactory.CloseChannel.get_CLASS());
            __mapChildren.put("CloseConnection", Peer.MessageFactory.CloseConnection.get_CLASS());
            __mapChildren.put("CreateChannel", Peer.MessageFactory.CreateChannel.get_CLASS());
            __mapChildren.put("EncodedMessage", Peer.MessageFactory.EncodedMessage.get_CLASS());
            __mapChildren.put("NotifyChannelClosed", Peer.MessageFactory.NotifyChannelClosed.get_CLASS());
            __mapChildren.put("NotifyConnectionClosed", Peer.MessageFactory.NotifyConnectionClosed.get_CLASS());
            __mapChildren.put("NotifyShutdown", Peer.MessageFactory.NotifyShutdown.get_CLASS());
            __mapChildren.put("NotifyStartup", Peer.MessageFactory.NotifyStartup.get_CLASS());
            __mapChildren.put("OpenChannel", Peer.MessageFactory.OpenChannel.get_CLASS());
            __mapChildren.put("OpenChannelRequest", Peer.MessageFactory.OpenChannelRequest.get_CLASS());
            __mapChildren.put("OpenChannelResponse", Peer.MessageFactory.OpenChannelResponse.get_CLASS());
            __mapChildren.put("OpenConnection", Peer.MessageFactory.OpenConnection.get_CLASS());
            __mapChildren.put("OpenConnectionRequest", Peer.MessageFactory.OpenConnectionRequest.get_CLASS());
            __mapChildren.put("OpenConnectionResponse", Peer.MessageFactory.OpenConnectionResponse.get_CLASS());
            __mapChildren.put("PingRequest", Peer.MessageFactory.PingRequest.get_CLASS());
            __mapChildren.put("PingResponse", Peer.MessageFactory.PingResponse.get_CLASS());
            __mapChildren.put("Response", Peer.MessageFactory.Response.get_CLASS());
            }
        
        // Initializing constructor
        public MessageFactory(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory".replace('/', '.'));
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
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        /**
         * Getter for property MessageClass.<p>
        * An array of static child component classes that are subclasses of the
        * Message component.
         */
        protected Class getMessageClass(int i)
            {
            // offset the index to account for internal Messages
            return super.getMessageClass(i + MESSAGE_OFFSET);
            }
        
        // Declared at the super level
        /**
         * Setter for property MessageClass.<p>
        * An array of static child component classes that are subclasses of the
        * Message component.
         */
        protected void setMessageClass(int i, Class clz)
            {
            // offset the index to account for internal Messages
            super.setMessageClass(i + MESSAGE_OFFSET, clz);
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$AcceptChannel
        
        /**
         * Internal Request used to accept a Channel.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class AcceptChannel
                extends    com.tangosol.coherence.component.net.extend.message.Request
            {
            // ---- Fields declarations ----
            
            /**
             * Property ChannelUri
             *
             * The URI that identifies the Channel to accept from the
             * perspective of the peer that created the Channel.
             */
            private java.net.URI __m_ChannelUri;
            
            /**
             * Property ClassLoader
             *
             * The ClassLoader used by the new Channel.
             */
            private transient ClassLoader __m_ClassLoader;
            
            /**
             * Property Connection
             *
             * The Connection used to accept the Channel.
             */
            private transient com.tangosol.coherence.component.net.extend.Connection __m_Connection;
            
            /**
             * Property IdentityToken
             *
             * A token representing a user's identity.
             */
            private byte[] __m_IdentityToken;
            
            /**
             * Property Receiver
             *
             * The optional Receiver that the Channel will register with.
             */
            private transient com.tangosol.net.messaging.Channel.Receiver __m_Receiver;
            
            /**
             * Property Subject
             *
             * The identity under which Messages received by the new Channel
             * will be executed.
             */
            private transient javax.security.auth.Subject __m_Subject;
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = -1;
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
                __mapChildren.put("Status", Peer.MessageFactory.AcceptChannel.Status.get_CLASS());
                }
            
            // Default constructor
            public AcceptChannel()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public AcceptChannel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannel();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$AcceptChannel".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }
            
            // Accessor for the property "ChannelUri"
            /**
             * Getter for property ChannelUri.<p>
            * The URI that identifies the Channel to accept from the
            * perspective of the peer that created the Channel.
             */
            public java.net.URI getChannelUri()
                {
                return __m_ChannelUri;
                }
            
            // Accessor for the property "ClassLoader"
            /**
             * Getter for property ClassLoader.<p>
            * The ClassLoader used by the new Channel.
             */
            public ClassLoader getClassLoader()
                {
                return __m_ClassLoader;
                }
            
            // Accessor for the property "Connection"
            /**
             * Getter for property Connection.<p>
            * The Connection used to accept the Channel.
             */
            public com.tangosol.coherence.component.net.extend.Connection getConnection()
                {
                return __m_Connection;
                }
            
            // Accessor for the property "IdentityToken"
            /**
             * Getter for property IdentityToken.<p>
            * A token representing a user's identity.
             */
            public byte[] getIdentityToken()
                {
                return __m_IdentityToken;
                }
            
            // Accessor for the property "Receiver"
            /**
             * Getter for property Receiver.<p>
            * The optional Receiver that the Channel will register with.
             */
            public com.tangosol.net.messaging.Channel.Receiver getReceiver()
                {
                return __m_Receiver;
                }
            
            // Accessor for the property "Subject"
            /**
             * Getter for property Subject.<p>
            * The identity under which Messages received by the new Channel
            * will be executed.
             */
            public javax.security.auth.Subject getSubject()
                {
                return __m_Subject;
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            /**
             * Factory method: create a new Response instance.
            * 
            * @param  the MessageFactory used to create the new Response object
            * 
            * @return a new Response object
             */
            protected com.tangosol.coherence.component.net.extend.message.Response instantiateResponse(com.tangosol.net.messaging.Protocol.MessageFactory factory)
                {
                return (Peer.MessageFactory.Response) factory.createMessage(Peer.MessageFactory.Response.TYPE_ID);
                }
            
            // Declared at the super level
            /**
             * Called when the Request is run.
            * 
            * @param response  the Response that should be populated with the
            * result of running the Request
             */
            protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
                {
                // import Component.Net.Extend.Connection;
                // import com.tangosol.net.messaging.Channel as com.tangosol.net.messaging.Channel;
                // import javax.security.auth.Subject;
                
                Connection connection = getConnection();
                _assert(connection != null);
                
                com.tangosol.net.messaging.Channel channel0 = getChannel();
                Subject subject  = getSubject();
                Peer module   = (Peer) channel0.getReceiver();
                
                response.setResult
                    (
                    connection.acceptChannelInternal
                        (
                        getChannelUri(),
                        module.ensureSerializer(getClassLoader()),
                        getReceiver(),
                        subject,
                        getIdentityToken()
                        )
                    );
                }
            
            // Accessor for the property "ChannelUri"
            /**
             * Setter for property ChannelUri.<p>
            * The URI that identifies the Channel to accept from the
            * perspective of the peer that created the Channel.
             */
            public void setChannelUri(java.net.URI uri)
                {
                __m_ChannelUri = uri;
                }
            
            // Accessor for the property "ClassLoader"
            /**
             * Setter for property ClassLoader.<p>
            * The ClassLoader used by the new Channel.
             */
            public void setClassLoader(ClassLoader loader)
                {
                __m_ClassLoader = loader;
                }
            
            // Accessor for the property "Connection"
            /**
             * Setter for property Connection.<p>
            * The Connection used to accept the Channel.
             */
            public void setConnection(com.tangosol.coherence.component.net.extend.Connection connection)
                {
                __m_Connection = connection;
                }
            
            // Accessor for the property "IdentityToken"
            /**
             * Setter for property IdentityToken.<p>
            * A token representing a user's identity.
             */
            public void setIdentityToken(byte[] abToken)
                {
                __m_IdentityToken = abToken;
                }
            
            // Accessor for the property "Receiver"
            /**
             * Setter for property Receiver.<p>
            * The optional Receiver that the Channel will register with.
             */
            public void setReceiver(com.tangosol.net.messaging.Channel.Receiver receiver)
                {
                __m_Receiver = receiver;
                }
            
            // Accessor for the property "Subject"
            /**
             * Setter for property Subject.<p>
            * The identity under which Messages received by the new Channel
            * will be executed.
             */
            public void setSubject(javax.security.auth.Subject subject)
                {
                __m_Subject = subject;
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$AcceptChannel$Status
            
            /**
             * Implementation of the Request$Status interface.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Status
                    extends    com.tangosol.coherence.component.net.extend.message.Request.Status
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Status()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannel.Status();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$AcceptChannel$Status".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$AcceptChannelRequest
        
        /**
         * This Request is used to accept a Channel that was spawned by a peer.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class AcceptChannelRequest
                extends    com.tangosol.coherence.component.net.extend.message.Request
            {
            // ---- Fields declarations ----
            
            /**
             * Property ChannelId
             *
             * The unique identifier of the Channel to accept. This Channel
             * identifier must have been previously returned in a Response sent
             * by the peer.
             */
            private int __m_ChannelId;
            
            /**
             * Property IdentityToken
             *
             * An optional token representing a Subject to associate with the
             * Channel. Operations performed on receipt of Messages sent via
             * the newly established Channel will be performed on behalf of
             * this Subject.
             */
            private byte[] __m_IdentityToken;
            
            /**
             * Property MessageFactory
             *
             * The MessageFactory used by the new Channel.
             */
            private transient com.tangosol.net.messaging.Protocol.MessageFactory __m_MessageFactory;
            
            /**
             * Property ProtocolName
             *
             * The name of the Protocol used by the new Channel. This Protocol
             * name must have been previously returned in a Response sent by
             * the peer.
             */
            private transient String __m_ProtocolName;
            
            /**
             * Property Receiver
             *
             * The optional Receiver that the Channel will register with.
             */
            private transient com.tangosol.net.messaging.Channel.Receiver __m_Receiver;
            
            /**
             * Property Serializer
             *
             * The Serializer used by the new Channel.
             */
            private transient com.tangosol.io.Serializer __m_Serializer;
            
            /**
             * Property Subject
             *
             * The identity under which Messages received by the new Channel
             * will be executed.
             */
            private transient javax.security.auth.Subject __m_Subject;
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = 13;
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
                __mapChildren.put("Status", Peer.MessageFactory.AcceptChannelRequest.Status.get_CLASS());
                }
            
            // Default constructor
            public AcceptChannelRequest()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public AcceptChannelRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannelRequest();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$AcceptChannelRequest".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }
            
            // Accessor for the property "ChannelId"
            /**
             * Getter for property ChannelId.<p>
            * The unique identifier of the Channel to accept. This Channel
            * identifier must have been previously returned in a Response sent
            * by the peer.
             */
            public int getChannelId()
                {
                return __m_ChannelId;
                }
            
            // Declared at the super level
            /**
             * Return a human-readable description of this component.
            * 
            * @return a String representation of this component
             */
            protected String getDescription()
                {
                return super.getDescription() + ", ChannelId=" + getChannelId();
                }
            
            // Accessor for the property "IdentityToken"
            /**
             * Getter for property IdentityToken.<p>
            * An optional token representing a Subject to associate with the
            * Channel. Operations performed on receipt of Messages sent via the
            * newly established Channel will be performed on behalf of this
            * Subject.
             */
            public byte[] getIdentityToken()
                {
                return __m_IdentityToken;
                }
            
            // Accessor for the property "MessageFactory"
            /**
             * Getter for property MessageFactory.<p>
            * The MessageFactory used by the new Channel.
             */
            public com.tangosol.net.messaging.Protocol.MessageFactory getMessageFactory()
                {
                return __m_MessageFactory;
                }
            
            // Accessor for the property "ProtocolName"
            /**
             * Getter for property ProtocolName.<p>
            * The name of the Protocol used by the new Channel. This Protocol
            * name must have been previously returned in a Response sent by the
            * peer.
             */
            public String getProtocolName()
                {
                return __m_ProtocolName;
                }
            
            // Accessor for the property "Receiver"
            /**
             * Getter for property Receiver.<p>
            * The optional Receiver that the Channel will register with.
             */
            public com.tangosol.net.messaging.Channel.Receiver getReceiver()
                {
                return __m_Receiver;
                }
            
            // Accessor for the property "Serializer"
            /**
             * Getter for property Serializer.<p>
            * The Serializer used by the new Channel.
             */
            public com.tangosol.io.Serializer getSerializer()
                {
                return __m_Serializer;
                }
            
            // Accessor for the property "Subject"
            /**
             * Getter for property Subject.<p>
            * The identity under which Messages received by the new Channel
            * will be executed.
             */
            public javax.security.auth.Subject getSubject()
                {
                return __m_Subject;
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            /**
             * Factory method: create a new Response instance.
            * 
            * @param  the MessageFactory used to create the new Response object
            * 
            * @return a new Response object
             */
            protected com.tangosol.coherence.component.net.extend.message.Response instantiateResponse(com.tangosol.net.messaging.Protocol.MessageFactory factory)
                {
                return (Peer.MessageFactory.AcceptChannelResponse) factory.createMessage(Peer.MessageFactory.AcceptChannelResponse.TYPE_ID);
                }
            
            // Declared at the super level
            /**
             * Called when the Request is run.
            * 
            * @param response  the Response that should be populated with the
            * result of running the Request
             */
            protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
                {
                // import Component.Net.Extend.Connection;
                // import com.tangosol.net.messaging.Channel as com.tangosol.net.messaging.Channel;
                // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
                
                com.tangosol.net.messaging.Channel channel0 = getChannel();
                _assert(channel0.getId() == 0);
                
                Connection connection = (Connection) channel0.getConnection();
                _assert(connection != null);
                
                Peer module = (Peer) channel0.getReceiver();
                int     nId    = getChannelId();
                
                connection.acceptChannelRequest
                    (
                    nId,
                    module.assertIdentityToken(module.deserializeIdentityToken(getIdentityToken())),
                    module.getAccessAdapter()
                    );
                
                // Cannot use getImplVersion() here as channel0 is established prior to protocol
                // version negotiation.  Instead check the negotiated version directly on the connection's
                // MessageFactoryMap.
                com.tangosol.net.messaging.Protocol.MessageFactory clientFactory = (com.tangosol.net.messaging.Protocol.MessageFactory) connection.getMessageFactoryMap()
                    .get(channel0.getMessageFactory().getProtocol().getName());
                if (clientFactory != null && clientFactory.getVersion() >= 3)
                    {
                    response.setResult(Integer.valueOf(nId));
                    }
                }
            
            // Declared at the super level
            public void readExternal(com.tangosol.io.pof.PofReader in)
                    throws java.io.IOException
                {
                super.readExternal(in);
                
                setChannelId(in.readInt(1));
                setIdentityToken(in.readByteArray(2));
                }
            
            // Accessor for the property "ChannelId"
            /**
             * Setter for property ChannelId.<p>
            * The unique identifier of the Channel to accept. This Channel
            * identifier must have been previously returned in a Response sent
            * by the peer.
             */
            public void setChannelId(int nId)
                {
                __m_ChannelId = nId;
                }
            
            // Accessor for the property "IdentityToken"
            /**
             * Setter for property IdentityToken.<p>
            * An optional token representing a Subject to associate with the
            * Channel. Operations performed on receipt of Messages sent via the
            * newly established Channel will be performed on behalf of this
            * Subject.
             */
            public void setIdentityToken(byte[] ab)
                {
                __m_IdentityToken = ab;
                }
            
            // Accessor for the property "MessageFactory"
            /**
             * Setter for property MessageFactory.<p>
            * The MessageFactory used by the new Channel.
             */
            public void setMessageFactory(com.tangosol.net.messaging.Protocol.MessageFactory factory)
                {
                __m_MessageFactory = factory;
                }
            
            // Accessor for the property "ProtocolName"
            /**
             * Setter for property ProtocolName.<p>
            * The name of the Protocol used by the new Channel. This Protocol
            * name must have been previously returned in a Response sent by the
            * peer.
             */
            public void setProtocolName(String sName)
                {
                __m_ProtocolName = sName;
                }
            
            // Accessor for the property "Receiver"
            /**
             * Setter for property Receiver.<p>
            * The optional Receiver that the Channel will register with.
             */
            public void setReceiver(com.tangosol.net.messaging.Channel.Receiver receiver)
                {
                __m_Receiver = receiver;
                }
            
            // Accessor for the property "Serializer"
            /**
             * Setter for property Serializer.<p>
            * The Serializer used by the new Channel.
             */
            public void setSerializer(com.tangosol.io.Serializer serializer)
                {
                __m_Serializer = serializer;
                }
            
            // Accessor for the property "Subject"
            /**
             * Setter for property Subject.<p>
            * The identity under which Messages received by the new Channel
            * will be executed.
             */
            public void setSubject(javax.security.auth.Subject subject)
                {
                __m_Subject = subject;
                }
            
            // Declared at the super level
            public void writeExternal(com.tangosol.io.pof.PofWriter out)
                    throws java.io.IOException
                {
                super.writeExternal(out);
                
                out.writeInt(1, getChannelId());
                out.writeByteArray(2, getIdentityToken());
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$AcceptChannelRequest$Status
            
            /**
             * Implementation of the Request$Status interface.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Status
                    extends    com.tangosol.coherence.component.net.extend.message.Request.Status
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Status()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannelRequest.Status();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$AcceptChannelRequest$Status".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$AcceptChannelResponse
        
        /**
         * Response to an AcceptChannelRequest.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class AcceptChannelResponse
                extends    com.tangosol.coherence.component.net.extend.message.Response
            {
            // ---- Fields declarations ----
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = 14;
            
            // Default constructor
            public AcceptChannelResponse()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public AcceptChannelResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannelResponse();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$AcceptChannelResponse".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            public void run()
                {
                // import Component.Net.Extend.Channel;
                // import Component.Net.Extend.Connection;
                // import com.tangosol.io.Serializer;
                // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
                // import com.tangosol.net.messaging.Channel$Receiver as com.tangosol.net.messaging.Channel.Receiver;
                // import javax.security.auth.Subject;
                
                Channel channel0 = (Channel) getChannel();
                _assert(channel0.getId() == 0);
                
                if (isFailure())
                    {
                    return;
                    }
                
                // extract the new Channel configuration from the AcceptChannelRequest
                Peer.MessageFactory.AcceptChannelRequest request = (Peer.MessageFactory.AcceptChannelRequest) channel0.getRequest(getRequestId());
                if (request == null)
                    {
                    Integer nId = (Integer) getResult();
                    if (nId != null)
                        {
                        // request timed-out and needs to be closed on the remote end; send a NotifyChannelClosed to the peer via "Channel0"
                        Peer.MessageFactory.NotifyChannelClosed message = (Peer.MessageFactory.NotifyChannelClosed) channel0.getMessageFactory().createMessage(Peer.MessageFactory.NotifyChannelClosed.TYPE_ID);
                
                        message.setChannelId(nId.intValue());
                        channel0.send(message);
                        }
                    }
                else
                    {
                    Connection connection = (Connection) channel0.getConnection();
                    int        nId        = request.getChannelId();
                    com.tangosol.net.messaging.Protocol.MessageFactory    factory    = request.getMessageFactory();
                    com.tangosol.net.messaging.Channel.Receiver   receiver   = request.getReceiver();
                    Serializer serializer = request.getSerializer();
                    Subject    subject    = request.getSubject();
                
                    setResult
                        (
                        connection.acceptChannelResponse
                            (
                            nId, factory, serializer, receiver, subject
                            )
                        );
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$CloseChannel
        
        /**
         * Internal Request used to close a Channel.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class CloseChannel
                extends    com.tangosol.coherence.component.net.extend.message.Request
            {
            // ---- Fields declarations ----
            
            /**
             * Property Cause
             *
             * The optional reason why the Channel is being closed.
             */
            private transient Throwable __m_Cause;
            
            /**
             * Property ChannelClose
             *
             * The Channel to close.
             */
            private transient com.tangosol.coherence.component.net.extend.Channel __m_ChannelClose;
            
            /**
             * Property Notify
             *
             * If true, notify the peer that the Channel is being closed.
             */
            private transient boolean __m_Notify;
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = -2;
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
                __mapChildren.put("Status", Peer.MessageFactory.CloseChannel.Status.get_CLASS());
                }
            
            // Default constructor
            public CloseChannel()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public CloseChannel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.CloseChannel();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$CloseChannel".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }
            
            // Accessor for the property "Cause"
            /**
             * Getter for property Cause.<p>
            * The optional reason why the Channel is being closed.
             */
            public Throwable getCause()
                {
                return __m_Cause;
                }
            
            // Accessor for the property "ChannelClose"
            /**
             * Getter for property ChannelClose.<p>
            * The Channel to close.
             */
            public com.tangosol.coherence.component.net.extend.Channel getChannelClose()
                {
                return __m_ChannelClose;
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            /**
             * Factory method: create a new Response instance.
            * 
            * @param  the MessageFactory used to create the new Response object
            * 
            * @return a new Response object
             */
            protected com.tangosol.coherence.component.net.extend.message.Response instantiateResponse(com.tangosol.net.messaging.Protocol.MessageFactory factory)
                {
                return (Peer.MessageFactory.Response) factory.createMessage(Peer.MessageFactory.Response.TYPE_ID);
                }
            
            // Accessor for the property "Notify"
            /**
             * Getter for property Notify.<p>
            * If true, notify the peer that the Channel is being closed.
             */
            public boolean isNotify()
                {
                return __m_Notify;
                }
            
            // Declared at the super level
            /**
             * Called when the Request is run.
            * 
            * @param response  the Response that should be populated with the
            * result of running the Request
             */
            protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
                {
                getChannelClose().closeInternal(isNotify(), getCause(), 0L);
                }
            
            // Accessor for the property "Cause"
            /**
             * Setter for property Cause.<p>
            * The optional reason why the Channel is being closed.
             */
            public void setCause(Throwable e)
                {
                __m_Cause = e;
                }
            
            // Accessor for the property "ChannelClose"
            /**
             * Setter for property ChannelClose.<p>
            * The Channel to close.
             */
            public void setChannelClose(com.tangosol.coherence.component.net.extend.Channel channel)
                {
                __m_ChannelClose = channel;
                }
            
            // Accessor for the property "Notify"
            /**
             * Setter for property Notify.<p>
            * If true, notify the peer that the Channel is being closed.
             */
            public void setNotify(boolean fNotify)
                {
                __m_Notify = fNotify;
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$CloseChannel$Status
            
            /**
             * Implementation of the Request$Status interface.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Status
                    extends    com.tangosol.coherence.component.net.extend.message.Request.Status
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Status()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.CloseChannel.Status();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$CloseChannel$Status".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$CloseConnection
        
        /**
         * Internal Message used to close a Connection.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class CloseConnection
                extends    com.tangosol.coherence.component.net.extend.message.Request
            {
            // ---- Fields declarations ----
            
            /**
             * Property Cause
             *
             * The optional reason why the Connection is being closed.
             */
            private transient Throwable __m_Cause;
            
            /**
             * Property ConnectionClose
             *
             * The Connection to close.
             */
            private transient com.tangosol.coherence.component.net.extend.Connection __m_ConnectionClose;
            
            /**
             * Property Notify
             *
             * If true, notify the peer that the Connection is being closed.
             */
            private transient boolean __m_Notify;
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = -3;
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
                __mapChildren.put("Status", Peer.MessageFactory.CloseConnection.Status.get_CLASS());
                }
            
            // Default constructor
            public CloseConnection()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public CloseConnection(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.CloseConnection();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$CloseConnection".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }
            
            // Accessor for the property "Cause"
            /**
             * Getter for property Cause.<p>
            * The optional reason why the Connection is being closed.
             */
            public Throwable getCause()
                {
                return __m_Cause;
                }
            
            // Accessor for the property "ConnectionClose"
            /**
             * Getter for property ConnectionClose.<p>
            * The Connection to close.
             */
            public com.tangosol.coherence.component.net.extend.Connection getConnectionClose()
                {
                return __m_ConnectionClose;
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            /**
             * Factory method: create a new Response instance.
            * 
            * @param  the MessageFactory used to create the new Response object
            * 
            * @return a new Response object
             */
            protected com.tangosol.coherence.component.net.extend.message.Response instantiateResponse(com.tangosol.net.messaging.Protocol.MessageFactory factory)
                {
                return (Peer.MessageFactory.Response) factory.createMessage(Peer.MessageFactory.Response.TYPE_ID);
                }
            
            // Accessor for the property "Notify"
            /**
             * Getter for property Notify.<p>
            * If true, notify the peer that the Connection is being closed.
             */
            public boolean isNotify()
                {
                return __m_Notify;
                }
            
            // Declared at the super level
            /**
             * Called when the Request is run.
            * 
            * @param response  the Response that should be populated with the
            * result of running the Request
             */
            protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
                {
                getConnectionClose().closeInternal(isNotify(), getCause(), 0L);
                }
            
            // Accessor for the property "Cause"
            /**
             * Setter for property Cause.<p>
            * The optional reason why the Connection is being closed.
             */
            public void setCause(Throwable e)
                {
                __m_Cause = e;
                }
            
            // Accessor for the property "ConnectionClose"
            /**
             * Setter for property ConnectionClose.<p>
            * The Connection to close.
             */
            public void setConnectionClose(com.tangosol.coherence.component.net.extend.Connection connection)
                {
                __m_ConnectionClose = connection;
                }
            
            // Accessor for the property "Notify"
            /**
             * Setter for property Notify.<p>
            * If true, notify the peer that the Connection is being closed.
             */
            public void setNotify(boolean fNotify)
                {
                __m_Notify = fNotify;
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$CloseConnection$Status
            
            /**
             * Implementation of the Request$Status interface.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Status
                    extends    com.tangosol.coherence.component.net.extend.message.Request.Status
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Status()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.CloseConnection.Status();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$CloseConnection$Status".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$CreateChannel
        
        /**
         * Internal Request used to create a Channel.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class CreateChannel
                extends    com.tangosol.coherence.component.net.extend.message.Request
            {
            // ---- Fields declarations ----
            
            /**
             * Property ClassLoader
             *
             * The ClassLoader used by the new Channel.
             */
            private transient ClassLoader __m_ClassLoader;
            
            /**
             * Property Connection
             *
             * The Connection used to create the Channel.
             */
            private transient com.tangosol.coherence.component.net.extend.Connection __m_Connection;
            
            /**
             * Property Protocol
             *
             * The Protocol used by the new Channel.
             */
            private transient com.tangosol.net.messaging.Protocol __m_Protocol;
            
            /**
             * Property Receiver
             *
             * The optional Receiver that the Channel will register with.
             */
            private transient com.tangosol.net.messaging.Channel.Receiver __m_Receiver;
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = -4;
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
                __mapChildren.put("Status", Peer.MessageFactory.CreateChannel.Status.get_CLASS());
                }
            
            // Default constructor
            public CreateChannel()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public CreateChannel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.CreateChannel();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$CreateChannel".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }
            
            // Accessor for the property "ClassLoader"
            /**
             * Getter for property ClassLoader.<p>
            * The ClassLoader used by the new Channel.
             */
            public ClassLoader getClassLoader()
                {
                return __m_ClassLoader;
                }
            
            // Accessor for the property "Connection"
            /**
             * Getter for property Connection.<p>
            * The Connection used to create the Channel.
             */
            public com.tangosol.coherence.component.net.extend.Connection getConnection()
                {
                return __m_Connection;
                }
            
            // Accessor for the property "Protocol"
            /**
             * Getter for property Protocol.<p>
            * The Protocol used by the new Channel.
             */
            public com.tangosol.net.messaging.Protocol getProtocol()
                {
                return __m_Protocol;
                }
            
            // Accessor for the property "Receiver"
            /**
             * Getter for property Receiver.<p>
            * The optional Receiver that the Channel will register with.
             */
            public com.tangosol.net.messaging.Channel.Receiver getReceiver()
                {
                return __m_Receiver;
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            /**
             * Factory method: create a new Response instance.
            * 
            * @param  the MessageFactory used to create the new Response object
            * 
            * @return a new Response object
             */
            protected com.tangosol.coherence.component.net.extend.message.Response instantiateResponse(com.tangosol.net.messaging.Protocol.MessageFactory factory)
                {
                return (Peer.MessageFactory.Response) factory.createMessage(Peer.MessageFactory.Response.TYPE_ID);
                }
            
            // Declared at the super level
            /**
             * Called when the Request is run.
            * 
            * @param response  the Response that should be populated with the
            * result of running the Request
             */
            protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
                {
                // import Component.Net.Extend.Connection;
                // import com.tangosol.net.messaging.Channel as com.tangosol.net.messaging.Channel;
                
                Connection connection = getConnection();
                _assert(connection != null);
                
                com.tangosol.net.messaging.Channel channel0 = getChannel();
                Peer module   = (Peer) channel0.getReceiver();
                
                response.setResult
                    (
                    connection.createChannelInternal
                        (
                        getProtocol(),
                        module.ensureSerializer(getClassLoader()),
                        getReceiver()
                        )
                    );
                }
            
            // Accessor for the property "ClassLoader"
            /**
             * Setter for property ClassLoader.<p>
            * The ClassLoader used by the new Channel.
             */
            public void setClassLoader(ClassLoader loader)
                {
                __m_ClassLoader = loader;
                }
            
            // Accessor for the property "Connection"
            /**
             * Setter for property Connection.<p>
            * The Connection used to create the Channel.
             */
            public void setConnection(com.tangosol.coherence.component.net.extend.Connection connection)
                {
                __m_Connection = connection;
                }
            
            // Accessor for the property "Protocol"
            /**
             * Setter for property Protocol.<p>
            * The Protocol used by the new Channel.
             */
            public void setProtocol(com.tangosol.net.messaging.Protocol protocol)
                {
                __m_Protocol = protocol;
                }
            
            // Accessor for the property "Receiver"
            /**
             * Setter for property Receiver.<p>
            * The optional Receiver that the Channel will register with.
             */
            public void setReceiver(com.tangosol.net.messaging.Channel.Receiver receiver)
                {
                __m_Receiver = receiver;
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$CreateChannel$Status
            
            /**
             * Implementation of the Request$Status interface.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Status
                    extends    com.tangosol.coherence.component.net.extend.message.Request.Status
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Status()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.CreateChannel.Status();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$CreateChannel$Status".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$EncodedMessage
        
        /**
         * A Message with a ReadBuffer that contains an encoded Message. The
         * service thread will decode the Message using the configured Codec
         * before dispatching it for execution.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class EncodedMessage
                extends    com.tangosol.coherence.component.net.extend.Message
            {
            // ---- Fields declarations ----
            
            /**
             * Property Connection
             *
             * The Connection that received the encoded Message.
             */
            private com.tangosol.coherence.component.net.extend.Connection __m_Connection;
            
            /**
             * Property ReadBuffer
             *
             * The encoded Message.
             */
            private com.tangosol.io.ReadBuffer __m_ReadBuffer;
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = -10;
            
            // Default constructor
            public EncodedMessage()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public EncodedMessage(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.EncodedMessage();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$EncodedMessage".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Accessor for the property "Connection"
            /**
             * Getter for property Connection.<p>
            * The Connection that received the encoded Message.
             */
            public com.tangosol.coherence.component.net.extend.Connection getConnection()
                {
                return __m_Connection;
                }
            
            // Accessor for the property "ReadBuffer"
            /**
             * Getter for property ReadBuffer.<p>
            * The encoded Message.
             */
            public com.tangosol.io.ReadBuffer getReadBuffer()
                {
                return __m_ReadBuffer;
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            public void run()
                {
                // no-op; see #onNotify
                }
            
            // Accessor for the property "Connection"
            /**
             * Setter for property Connection.<p>
            * The Connection that received the encoded Message.
             */
            public void setConnection(com.tangosol.coherence.component.net.extend.Connection connection)
                {
                __m_Connection = connection;
                }
            
            // Accessor for the property "ReadBuffer"
            /**
             * Setter for property ReadBuffer.<p>
            * The encoded Message.
             */
            public void setReadBuffer(com.tangosol.io.ReadBuffer rb)
                {
                __m_ReadBuffer = rb;
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$NotifyChannelClosed
        
        /**
         * This Message is sent to the peer when a Channel has been closed.
         * This allows the peer to collect any resources held by the Channel.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class NotifyChannelClosed
                extends    com.tangosol.coherence.component.net.extend.Message
            {
            // ---- Fields declarations ----
            
            /**
             * Property Cause
             *
             * The optional reason why the Channel was closed.
             */
            private Throwable __m_Cause;
            
            /**
             * Property ChannelId
             *
             * The identifier of the Channel that was closed.
             */
            private transient int __m_ChannelId;
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = 20;
            
            // Default constructor
            public NotifyChannelClosed()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public NotifyChannelClosed(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyChannelClosed();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$NotifyChannelClosed".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Accessor for the property "Cause"
            /**
             * Getter for property Cause.<p>
            * The optional reason why the Channel was closed.
             */
            public Throwable getCause()
                {
                return __m_Cause;
                }
            
            // Accessor for the property "ChannelId"
            /**
             * Getter for property ChannelId.<p>
            * The identifier of the Channel that was closed.
             */
            public int getChannelId()
                {
                return __m_ChannelId;
                }
            
            // Declared at the super level
            /**
             * Return a human-readable description of this component.
            * 
            * @return a String representation of this component
             */
            protected String getDescription()
                {
                return super.getDescription() + ", ChannelId=" + getChannelId();
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            public void readExternal(com.tangosol.io.pof.PofReader in)
                    throws java.io.IOException
                {
                super.readExternal(in);
                
                setChannelId(in.readInt(0));
                setCause((Throwable) in.readObject(1));
                }
            
            // Declared at the super level
            public void run()
                {
                // import Component.Net.Extend.Channel;
                // import com.tangosol.net.messaging.Connection as com.tangosol.net.messaging.Connection;
                
                Channel channel0 = (Channel) getChannel();
                _assert(channel0.getId() == 0);
                
                com.tangosol.net.messaging.Connection connection = channel0.getConnection();
                _assert(connection != null);
                
                Channel channel = (Channel) connection.getChannel(getChannelId());
                if (channel != null)
                    {
                    channel.close(false, getCause());
                    }
                }
            
            // Accessor for the property "Cause"
            /**
             * Setter for property Cause.<p>
            * The optional reason why the Channel was closed.
             */
            public void setCause(Throwable e)
                {
                __m_Cause = e;
                }
            
            // Accessor for the property "ChannelId"
            /**
             * Setter for property ChannelId.<p>
            * The identifier of the Channel that was closed.
             */
            public void setChannelId(int nId)
                {
                __m_ChannelId = nId;
                }
            
            // Declared at the super level
            public void writeExternal(com.tangosol.io.pof.PofWriter out)
                    throws java.io.IOException
                {
                super.writeExternal(out);
                
                out.writeInt(0, getChannelId());
                out.writeObject(1, getCause());
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$NotifyConnectionClosed
        
        /**
         * This Message is sent to the peer when a Connection has been closed.
         * This allows the peer to collect any resources held by the Connection.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class NotifyConnectionClosed
                extends    com.tangosol.coherence.component.net.extend.Message
            {
            // ---- Fields declarations ----
            
            /**
             * Property Cause
             *
             * The optional reason why the Connection was closed.
             */
            private Throwable __m_Cause;
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = 10;
            
            // Default constructor
            public NotifyConnectionClosed()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public NotifyConnectionClosed(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyConnectionClosed();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$NotifyConnectionClosed".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Accessor for the property "Cause"
            /**
             * Getter for property Cause.<p>
            * The optional reason why the Connection was closed.
             */
            public Throwable getCause()
                {
                return __m_Cause;
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            public void readExternal(com.tangosol.io.pof.PofReader in)
                    throws java.io.IOException
                {
                super.readExternal(in);
                
                setCause((Throwable) in.readObject(0));
                }
            
            // Declared at the super level
            public void run()
                {
                // import Component.Net.Extend.Connection;
                // import com.tangosol.net.messaging.Channel as com.tangosol.net.messaging.Channel;
                
                com.tangosol.net.messaging.Channel channel0 = getChannel();
                _assert(channel0.getId() == 0);
                
                Connection connection = (Connection) channel0.getConnection();
                _assert(connection != null);
                
                connection.close(false, getCause());
                }
            
            // Accessor for the property "Cause"
            /**
             * Setter for property Cause.<p>
            * The optional reason why the Connection was closed.
             */
            public void setCause(Throwable e)
                {
                __m_Cause = e;
                }
            
            // Declared at the super level
            public void writeExternal(com.tangosol.io.pof.PofWriter out)
                    throws java.io.IOException
                {
                super.writeExternal(out);
                
                out.writeObject(0, getCause());
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$NotifyShutdown
        
        /**
         * This internal Message is sent to a ConnectionManager it is supposed
         * to shut down. The ConnectionManager must clean up and unregister
         * itself. Note that the only task of the shut-down is to begin the
         * process of shutting down the service; technically the
         * ConnectionManager does not have to be stopped by the time the
         * shutdown Message completes its processing, although the default
         * implementation does stop it immediately.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class NotifyShutdown
                extends    com.tangosol.coherence.component.net.extend.Message
            {
            // ---- Fields declarations ----
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = -5;
            
            // Default constructor
            public NotifyShutdown()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public NotifyShutdown(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyShutdown();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$NotifyShutdown".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            public void run()
                {
                // import Component.Util.Daemon.QueueProcessor.Service;
                
                Peer module = (Peer) getChannel().getReceiver();
                
                try
                    {
                    module.setServiceState(Service.SERVICE_STOPPING);
                    }
                finally
                    {
                    module.stop();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$NotifyStartup
        
        /**
         * This internal Message is sent to a ConnectionManager when it first
         * has been started.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class NotifyStartup
                extends    com.tangosol.coherence.component.net.extend.Message
            {
            // ---- Fields declarations ----
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = -6;
            
            // Default constructor
            public NotifyStartup()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public NotifyStartup(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyStartup();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$NotifyStartup".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            public void run()
                {
                // import Component.Util.Daemon.QueueProcessor.Service;
                
                Peer module = (Peer) getChannel().getReceiver();
                
                module.setServiceState(Service.SERVICE_STARTED);
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$OpenChannel
        
        /**
         * Internal Request used to open a Channel.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class OpenChannel
                extends    com.tangosol.coherence.component.net.extend.message.Request
            {
            // ---- Fields declarations ----
            
            /**
             * Property ClassLoader
             *
             * The Serializer used by the new Channel.
             */
            private transient ClassLoader __m_ClassLoader;
            
            /**
             * Property Connection
             *
             * The Connection used to open the Channel.
             */
            private transient com.tangosol.coherence.component.net.extend.Connection __m_Connection;
            
            /**
             * Property IdentityToken
             *
             * A token representing a user's identity.
             */
            private byte[] __m_IdentityToken;
            
            /**
             * Property Protocol
             *
             * The Protocol used by the new Channel.
             */
            private transient com.tangosol.net.messaging.Protocol __m_Protocol;
            
            /**
             * Property Receiver
             *
             * The optional Receiver that the Channel will register with.
             */
            private transient com.tangosol.net.messaging.Channel.Receiver __m_Receiver;
            
            /**
             * Property ReceiverName
             *
             * The name of the remote Receiver.
             */
            private String __m_ReceiverName;
            
            /**
             * Property Subject
             *
             * The identity under which Messages received by the new Channel
             * will be executed.
             */
            private transient javax.security.auth.Subject __m_Subject;
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = -7;
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
                __mapChildren.put("Status", Peer.MessageFactory.OpenChannel.Status.get_CLASS());
                }
            
            // Default constructor
            public OpenChannel()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public OpenChannel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannel();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$OpenChannel".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }
            
            // Accessor for the property "ClassLoader"
            /**
             * Getter for property ClassLoader.<p>
            * The Serializer used by the new Channel.
             */
            public ClassLoader getClassLoader()
                {
                return __m_ClassLoader;
                }
            
            // Accessor for the property "Connection"
            /**
             * Getter for property Connection.<p>
            * The Connection used to open the Channel.
             */
            public com.tangosol.coherence.component.net.extend.Connection getConnection()
                {
                return __m_Connection;
                }
            
            // Accessor for the property "IdentityToken"
            /**
             * Getter for property IdentityToken.<p>
            * A token representing a user's identity.
             */
            public byte[] getIdentityToken()
                {
                return __m_IdentityToken;
                }
            
            // Accessor for the property "Protocol"
            /**
             * Getter for property Protocol.<p>
            * The Protocol used by the new Channel.
             */
            public com.tangosol.net.messaging.Protocol getProtocol()
                {
                return __m_Protocol;
                }
            
            // Accessor for the property "Receiver"
            /**
             * Getter for property Receiver.<p>
            * The optional Receiver that the Channel will register with.
             */
            public com.tangosol.net.messaging.Channel.Receiver getReceiver()
                {
                return __m_Receiver;
                }
            
            // Accessor for the property "ReceiverName"
            /**
             * Getter for property ReceiverName.<p>
            * The name of the remote Receiver.
             */
            public String getReceiverName()
                {
                return __m_ReceiverName;
                }
            
            // Accessor for the property "Subject"
            /**
             * Getter for property Subject.<p>
            * The identity under which Messages received by the new Channel
            * will be executed.
             */
            public javax.security.auth.Subject getSubject()
                {
                return __m_Subject;
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            /**
             * Factory method: create a new Response instance.
            * 
            * @param  the MessageFactory used to create the new Response object
            * 
            * @return a new Response object
             */
            protected com.tangosol.coherence.component.net.extend.message.Response instantiateResponse(com.tangosol.net.messaging.Protocol.MessageFactory factory)
                {
                return (Peer.MessageFactory.Response) factory.createMessage(Peer.MessageFactory.Response.TYPE_ID);
                }
            
            // Declared at the super level
            /**
             * Called when the Request is run.
            * 
            * @param response  the Response that should be populated with the
            * result of running the Request
             */
            protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
                {
                // import Component.Net.Extend.Connection;
                // import com.tangosol.net.messaging.Channel as com.tangosol.net.messaging.Channel;
                
                Connection connection = getConnection();
                _assert(connection != null);
                
                com.tangosol.net.messaging.Channel channel0 = getChannel();
                Peer module   = (Peer) channel0.getReceiver();
                
                response.setResult
                    (
                    connection.openChannelInternal
                        (
                        getProtocol(),
                        getReceiverName(),
                        module.ensureSerializer(getClassLoader()),
                        getReceiver(),
                        getSubject(),
                        getIdentityToken()
                        )
                    );
                }
            
            // Accessor for the property "ClassLoader"
            /**
             * Setter for property ClassLoader.<p>
            * The Serializer used by the new Channel.
             */
            public void setClassLoader(ClassLoader loader)
                {
                __m_ClassLoader = loader;
                }
            
            // Accessor for the property "Connection"
            /**
             * Setter for property Connection.<p>
            * The Connection used to open the Channel.
             */
            public void setConnection(com.tangosol.coherence.component.net.extend.Connection connection)
                {
                __m_Connection = connection;
                }
            
            // Accessor for the property "IdentityToken"
            /**
             * Setter for property IdentityToken.<p>
            * A token representing a user's identity.
             */
            public void setIdentityToken(byte[] abToken)
                {
                __m_IdentityToken = abToken;
                }
            
            // Accessor for the property "Protocol"
            /**
             * Setter for property Protocol.<p>
            * The Protocol used by the new Channel.
             */
            public void setProtocol(com.tangosol.net.messaging.Protocol protocol)
                {
                __m_Protocol = protocol;
                }
            
            // Accessor for the property "Receiver"
            /**
             * Setter for property Receiver.<p>
            * The optional Receiver that the Channel will register with.
             */
            public void setReceiver(com.tangosol.net.messaging.Channel.Receiver receiver)
                {
                __m_Receiver = receiver;
                }
            
            // Accessor for the property "ReceiverName"
            /**
             * Setter for property ReceiverName.<p>
            * The name of the remote Receiver.
             */
            public void setReceiverName(String sName)
                {
                __m_ReceiverName = sName;
                }
            
            // Accessor for the property "Subject"
            /**
             * Setter for property Subject.<p>
            * The identity under which Messages received by the new Channel
            * will be executed.
             */
            public void setSubject(javax.security.auth.Subject subject)
                {
                __m_Subject = subject;
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$OpenChannel$Status
            
            /**
             * Implementation of the Request$Status interface.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Status
                    extends    com.tangosol.coherence.component.net.extend.message.Request.Status
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Status()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannel.Status();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$OpenChannel$Status".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$OpenChannelRequest
        
        /**
         * This Request is used to open a new Channel.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class OpenChannelRequest
                extends    com.tangosol.coherence.component.net.extend.message.Request
            {
            // ---- Fields declarations ----
            
            /**
             * Property IdentityToken
             *
             * An optional token representing a Subject to associate with the
             * Channel. Operations performed on receipt of Messages sent via
             * the newly established Channel will be performed on behalf of
             * this Subject.
             */
            private byte[] __m_IdentityToken;
            
            /**
             * Property MessageFactory
             *
             * The MessageFactory used by the new Channel.
             */
            private transient com.tangosol.net.messaging.Protocol.MessageFactory __m_MessageFactory;
            
            /**
             * Property ProtocolName
             *
             * The name of the Protocol that must be used by the peer Receiver.
             */
            private String __m_ProtocolName;
            
            /**
             * Property Receiver
             *
             * The optional Receiver that the Channel will register with.
             */
            private transient com.tangosol.net.messaging.Channel.Receiver __m_Receiver;
            
            /**
             * Property ReceiverName
             *
             * The name of the peer Receiver to which the Channel should be
             * bound.
             */
            private String __m_ReceiverName;
            
            /**
             * Property Serializer
             *
             * The Serializer used by the new Channel.
             */
            private transient com.tangosol.io.Serializer __m_Serializer;
            
            /**
             * Property Subject
             *
             * The identity under which Messages received by the new Channel
             * will be executed.
             */
            private transient javax.security.auth.Subject __m_Subject;
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = 11;
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
                __mapChildren.put("Status", Peer.MessageFactory.OpenChannelRequest.Status.get_CLASS());
                }
            
            // Default constructor
            public OpenChannelRequest()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public OpenChannelRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannelRequest();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$OpenChannelRequest".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }
            
            // Declared at the super level
            /**
             * Return a human-readable description of this component.
            * 
            * @return a String representation of this component
             */
            protected String getDescription()
                {
                return super.getDescription()
                        + ", Protocol=" + getProtocolName()
                        + ", Receiver=" + getReceiverName();
                }
            
            // Accessor for the property "IdentityToken"
            /**
             * Getter for property IdentityToken.<p>
            * An optional token representing a Subject to associate with the
            * Channel. Operations performed on receipt of Messages sent via the
            * newly established Channel will be performed on behalf of this
            * Subject.
             */
            public byte[] getIdentityToken()
                {
                return __m_IdentityToken;
                }
            
            // Accessor for the property "MessageFactory"
            /**
             * Getter for property MessageFactory.<p>
            * The MessageFactory used by the new Channel.
             */
            public com.tangosol.net.messaging.Protocol.MessageFactory getMessageFactory()
                {
                return __m_MessageFactory;
                }
            
            // Accessor for the property "ProtocolName"
            /**
             * Getter for property ProtocolName.<p>
            * The name of the Protocol that must be used by the peer Receiver.
             */
            public String getProtocolName()
                {
                return __m_ProtocolName;
                }
            
            // Accessor for the property "Receiver"
            /**
             * Getter for property Receiver.<p>
            * The optional Receiver that the Channel will register with.
             */
            public com.tangosol.net.messaging.Channel.Receiver getReceiver()
                {
                return __m_Receiver;
                }
            
            // Accessor for the property "ReceiverName"
            /**
             * Getter for property ReceiverName.<p>
            * The name of the peer Receiver to which the Channel should be
            * bound.
             */
            public String getReceiverName()
                {
                return __m_ReceiverName;
                }
            
            // Accessor for the property "Serializer"
            /**
             * Getter for property Serializer.<p>
            * The Serializer used by the new Channel.
             */
            public com.tangosol.io.Serializer getSerializer()
                {
                return __m_Serializer;
                }
            
            // Accessor for the property "Subject"
            /**
             * Getter for property Subject.<p>
            * The identity under which Messages received by the new Channel
            * will be executed.
             */
            public javax.security.auth.Subject getSubject()
                {
                return __m_Subject;
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            /**
             * Factory method: create a new Response instance.
            * 
            * @param  the MessageFactory used to create the new Response object
            * 
            * @return a new Response object
             */
            protected com.tangosol.coherence.component.net.extend.message.Response instantiateResponse(com.tangosol.net.messaging.Protocol.MessageFactory factory)
                {
                return (Peer.MessageFactory.OpenChannelResponse) factory.createMessage(Peer.MessageFactory.OpenChannelResponse.TYPE_ID);
                }
            
            // Declared at the super level
            /**
             * Called when the Request is run.
            * 
            * @param response  the Response that should be populated with the
            * result of running the Request
             */
            protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
                {
                // import Component.Net.Extend.Connection;
                // import com.tangosol.net.messaging.Channel as com.tangosol.net.messaging.Channel;
                // import com.tangosol.net.messaging.Channel$Receiver as com.tangosol.net.messaging.Channel.Receiver;
                
                com.tangosol.net.messaging.Channel channel0 = getChannel();
                _assert(channel0.getId() == 0);
                
                Connection connection = (Connection) channel0.getConnection();
                _assert(connection != null);
                
                Peer  module   = (Peer) channel0.getReceiver();
                com.tangosol.net.messaging.Channel.Receiver receiver = module.getReceiver(getReceiverName());
                
                if (receiver == null)
                    {
                    throw new IllegalArgumentException("unknown receiver: "
                            + getReceiverName());
                    }
                
                response.setResult
                    (
                    Integer.valueOf
                        (
                        connection.openChannelRequest
                            (
                            getProtocolName(),
                            channel0.getSerializer(),
                            receiver,
                            module.assertIdentityToken(module.deserializeIdentityToken(
                                    getIdentityToken())),
                            module.getAccessAdapter()
                            )
                        )
                    );
                }
            
            // Declared at the super level
            public void readExternal(com.tangosol.io.pof.PofReader in)
                    throws java.io.IOException
                {
                super.readExternal(in);
                
                setProtocolName(in.readString(1));
                setReceiverName(in.readString(2));
                setIdentityToken(in.readByteArray(3));
                }
            
            // Accessor for the property "IdentityToken"
            /**
             * Setter for property IdentityToken.<p>
            * An optional token representing a Subject to associate with the
            * Channel. Operations performed on receipt of Messages sent via the
            * newly established Channel will be performed on behalf of this
            * Subject.
             */
            public void setIdentityToken(byte[] abToken)
                {
                __m_IdentityToken = abToken;
                }
            
            // Accessor for the property "MessageFactory"
            /**
             * Setter for property MessageFactory.<p>
            * The MessageFactory used by the new Channel.
             */
            public void setMessageFactory(com.tangosol.net.messaging.Protocol.MessageFactory factory)
                {
                __m_MessageFactory = factory;
                }
            
            // Accessor for the property "ProtocolName"
            /**
             * Setter for property ProtocolName.<p>
            * The name of the Protocol that must be used by the peer Receiver.
             */
            public void setProtocolName(String sName)
                {
                __m_ProtocolName = sName;
                }
            
            // Accessor for the property "Receiver"
            /**
             * Setter for property Receiver.<p>
            * The optional Receiver that the Channel will register with.
             */
            public void setReceiver(com.tangosol.net.messaging.Channel.Receiver receiver)
                {
                __m_Receiver = receiver;
                }
            
            // Accessor for the property "ReceiverName"
            /**
             * Setter for property ReceiverName.<p>
            * The name of the peer Receiver to which the Channel should be
            * bound.
             */
            public void setReceiverName(String sName)
                {
                __m_ReceiverName = sName;
                }
            
            // Accessor for the property "Serializer"
            /**
             * Setter for property Serializer.<p>
            * The Serializer used by the new Channel.
             */
            public void setSerializer(com.tangosol.io.Serializer serializer)
                {
                __m_Serializer = serializer;
                }
            
            // Accessor for the property "Subject"
            /**
             * Setter for property Subject.<p>
            * The identity under which Messages received by the new Channel
            * will be executed.
             */
            public void setSubject(javax.security.auth.Subject subject)
                {
                __m_Subject = subject;
                }
            
            // Declared at the super level
            public void writeExternal(com.tangosol.io.pof.PofWriter out)
                    throws java.io.IOException
                {
                super.writeExternal(out);
                
                out.writeString(1, getProtocolName());
                out.writeString(2, getReceiverName());
                out.writeByteArray(3, getIdentityToken());
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$OpenChannelRequest$Status
            
            /**
             * Implementation of the Request$Status interface.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Status
                    extends    com.tangosol.coherence.component.net.extend.message.Request.Status
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Status()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannelRequest.Status();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$OpenChannelRequest$Status".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$OpenChannelResponse
        
        /**
         * Response to an OpenChannelRequest.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class OpenChannelResponse
                extends    com.tangosol.coherence.component.net.extend.message.Response
            {
            // ---- Fields declarations ----
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = 12;
            
            // Default constructor
            public OpenChannelResponse()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public OpenChannelResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannelResponse();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$OpenChannelResponse".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            public void run()
                {
                // import Component.Net.Extend.Channel;
                // import Component.Net.Extend.Connection;
                // import com.tangosol.io.Serializer;
                // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
                // import com.tangosol.net.messaging.Channel$Receiver as com.tangosol.net.messaging.Channel.Receiver;
                // import javax.security.auth.Subject;
                
                Channel channel0 = (Channel) getChannel();
                _assert(channel0.getId() == 0);
                
                if (isFailure())
                    {
                    return;
                    }
                
                // extract the new Channel configuration from the OpenChannelRequest
                Connection          connection = (Connection) channel0.getConnection();
                int                 nId        = ((Integer) getResult()).intValue();
                Peer.MessageFactory.OpenChannelRequest request    = (Peer.MessageFactory.OpenChannelRequest) channel0.getRequest(getRequestId());
                if (request == null)
                    {
                    // request timed-out and needs to be closed on the remote end; send a NotifyChannelClosed to the peer via "Channel0"
                    Peer.MessageFactory.NotifyChannelClosed message = (Peer.MessageFactory.NotifyChannelClosed) channel0.getMessageFactory().createMessage(Peer.MessageFactory.NotifyChannelClosed.TYPE_ID);
                
                    message.setChannelId(nId);
                    channel0.send(message);
                    }
                else
                    {
                    com.tangosol.net.messaging.Protocol.MessageFactory    factory    = request.getMessageFactory();
                    com.tangosol.net.messaging.Channel.Receiver   receiver   = request.getReceiver();
                    Serializer serializer = request.getSerializer();
                    Subject    subject    = request.getSubject();
                
                    setResult
                        (
                        connection.openChannelResponse
                            (
                            nId, factory, serializer, receiver, subject
                            )
                        );
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$OpenConnection
        
        /**
         * Internal Request used to open a Connection.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static abstract class OpenConnection
                extends    com.tangosol.coherence.component.net.extend.message.Request
            {
            // ---- Fields declarations ----
            
            /**
             * Property ConnectionOpen
             *
             * The Connection to open.
             */
            private transient com.tangosol.coherence.component.net.extend.Connection __m_ConnectionOpen;
            
            /**
             * Property IdentityToken
             *
             * A token representing a user's identity.
             */
            private byte[] __m_IdentityToken;
            
            /**
             * Property Subject
             *
             * The identity under which Messages received by "Channel0" will be
             * executed.
             */
            private transient javax.security.auth.Subject __m_Subject;
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = -8;
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
                __mapChildren.put("Status", Peer.MessageFactory.OpenConnection.Status.get_CLASS());
                }
            
            // Initializing constructor
            public OpenConnection(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$OpenConnection".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }
            
            // Accessor for the property "ConnectionOpen"
            /**
             * Getter for property ConnectionOpen.<p>
            * The Connection to open.
             */
            public com.tangosol.coherence.component.net.extend.Connection getConnectionOpen()
                {
                return __m_ConnectionOpen;
                }
            
            // Accessor for the property "IdentityToken"
            /**
             * Getter for property IdentityToken.<p>
            * A token representing a user's identity.
             */
            public byte[] getIdentityToken()
                {
                return __m_IdentityToken;
                }
            
            // Accessor for the property "Subject"
            /**
             * Getter for property Subject.<p>
            * The identity under which Messages received by "Channel0" will be
            * executed.
             */
            public javax.security.auth.Subject getSubject()
                {
                return __m_Subject;
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            /**
             * Factory method: create a new Response instance.
            * 
            * @param  the MessageFactory used to create the new Response object
            * 
            * @return a new Response object
             */
            protected com.tangosol.coherence.component.net.extend.message.Response instantiateResponse(com.tangosol.net.messaging.Protocol.MessageFactory factory)
                {
                return (Peer.MessageFactory.Response) factory.createMessage(Peer.MessageFactory.Response.TYPE_ID);
                }
            
            // Accessor for the property "ConnectionOpen"
            /**
             * Setter for property ConnectionOpen.<p>
            * The Connection to open.
             */
            public void setConnectionOpen(com.tangosol.coherence.component.net.extend.Connection connection)
                {
                __m_ConnectionOpen = connection;
                }
            
            // Accessor for the property "IdentityToken"
            /**
             * Setter for property IdentityToken.<p>
            * A token representing a user's identity.
             */
            public void setIdentityToken(byte[] abToken)
                {
                __m_IdentityToken = abToken;
                }
            
            // Accessor for the property "Subject"
            /**
             * Setter for property Subject.<p>
            * The identity under which Messages received by "Channel0" will be
            * executed.
             */
            public void setSubject(javax.security.auth.Subject subject)
                {
                __m_Subject = subject;
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$OpenConnection$Status
            
            /**
             * Implementation of the Request$Status interface.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Status
                    extends    com.tangosol.coherence.component.net.extend.message.Request.Status
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Status()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenConnection.Status();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$OpenConnection$Status".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$OpenConnectionRequest
        
        /**
         * This Request is used to open a new Channel.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static abstract class OpenConnectionRequest
                extends    com.tangosol.coherence.component.net.extend.message.Request
            {
            // ---- Fields declarations ----
            
            /**
             * Property ClientId
             *
             * The unique identifier (UUID) of the client that sent this
             * Request.
             */
            private com.tangosol.util.UUID __m_ClientId;
            
            /**
             * Property ClusterName
             *
             * The name of the cluster the peer wishes to connect to.
             * 
             * @since 12.2.1
             */
            private String __m_ClusterName;
            
            /**
             * Property Edition
             *
             * The product edition used by the client.
             * 
             * @see Component.Application.Console.Coherence#getEdition
             */
            private int __m_Edition;
            
            /**
             * Property IdentityToken
             *
             * An optional token representing a Subject to associate with
             * "Channel0". Operations performed on receipt of Messages sent via
             * "Channel0" will be performed on behalf of this Subject.
             */
            private byte[] __m_IdentityToken;
            
            /**
             * Property Member
             *
             * The optional Member object to associate with the new Connection.
             */
            private com.tangosol.net.Member __m_Member;
            
            /**
             * Property ProtocolVersionMap
             *
             * A map of required Protocols. The keys are the names of the
             * required Protocols and the values are two element Integer
             * arrays, the first element being the current version and the
             * second being the supported version of the corresponding Protocol.
             */
            private java.util.Map __m_ProtocolVersionMap;
            
            /**
             * Property ServiceName
             *
             * The name of the service the peer wishes to connect to.
             * 
             * @since 12.2.1
             */
            private String __m_ServiceName;
            
            /**
             * Property Subject
             *
             * The identity under which Messages received by "Channel0" will be
             * executed.
             */
            private transient javax.security.auth.Subject __m_Subject;
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = 1;
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
                __mapChildren.put("Status", Peer.MessageFactory.OpenConnectionRequest.Status.get_CLASS());
                }
            
            // Initializing constructor
            public OpenConnectionRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$OpenConnectionRequest".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }
            
            // Accessor for the property "ClientId"
            /**
             * Getter for property ClientId.<p>
            * The unique identifier (UUID) of the client that sent this Request.
             */
            public com.tangosol.util.UUID getClientId()
                {
                return __m_ClientId;
                }
            
            // Accessor for the property "ClusterName"
            /**
             * Getter for property ClusterName.<p>
            * The name of the cluster the peer wishes to connect to.
            * 
            * @since 12.2.1
             */
            public String getClusterName()
                {
                return __m_ClusterName;
                }
            
            // Declared at the super level
            /**
             * Return a human-readable description of this component.
            * 
            * @return a String representation of this component
             */
            protected String getDescription()
                {
                return super.getDescription()
                        + ", ClientId="           + getClientId()
                        + ", Edition="            + getEdition()
                        + ", ProtocolVersionMap=" + getVersionMapDescription(getProtocolVersionMap())
                        + ", Member="             + getMember();
                }
            
            // Accessor for the property "Edition"
            /**
             * Getter for property Edition.<p>
            * The product edition used by the client.
            * 
            * @see Component.Application.Console.Coherence#getEdition
             */
            public int getEdition()
                {
                return __m_Edition;
                }
            
            // Accessor for the property "IdentityToken"
            /**
             * Getter for property IdentityToken.<p>
            * An optional token representing a Subject to associate with
            * "Channel0". Operations performed on receipt of Messages sent via
            * "Channel0" will be performed on behalf of this Subject.
             */
            public byte[] getIdentityToken()
                {
                return __m_IdentityToken;
                }
            
            // Accessor for the property "Member"
            /**
             * Getter for property Member.<p>
            * The optional Member object to associate with the new Connection.
             */
            public com.tangosol.net.Member getMember()
                {
                return __m_Member;
                }
            
            // Accessor for the property "ProtocolVersionMap"
            /**
             * Getter for property ProtocolVersionMap.<p>
            * A map of required Protocols. The keys are the names of the
            * required Protocols and the values are two element Integer arrays,
            * the first element being the current version and the second being
            * the supported version of the corresponding Protocol.
             */
            public java.util.Map getProtocolVersionMap()
                {
                return __m_ProtocolVersionMap;
                }
            
            // Accessor for the property "ServiceName"
            /**
             * Getter for property ServiceName.<p>
            * The name of the service the peer wishes to connect to.
            * 
            * @since 12.2.1
             */
            public String getServiceName()
                {
                return __m_ServiceName;
                }
            
            // Accessor for the property "Subject"
            /**
             * Getter for property Subject.<p>
            * The identity under which Messages received by "Channel0" will be
            * executed.
             */
            public javax.security.auth.Subject getSubject()
                {
                return __m_Subject;
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            /**
             * Return a human-readable description of the protocol version map.
            * 
            * @return a String representation of the protocol version map
             */
            protected String getVersionMapDescription(java.util.Map map)
                {
                // import java.util.Iterator;
                // import java.util.Map$Entry as java.util.Map.Entry;
                
                StringBuilder sb = new StringBuilder("{");
                for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
                    {
                    java.util.Map.Entry  entry    = (java.util.Map.Entry) iter.next();
                    String sName    = (String) entry.getKey();
                    Object oVersion = entry.getValue();
                
                    sb.append(sName + "=");
                    if (oVersion instanceof Integer)
                        {
                        // strict version requirement
                        sb.append(oVersion);
                        }
                    else
                        {
                        // version range supported
                        Object[] aVersions       = (Object[]) oVersion;
                        int      nRequestCurrent = ((Integer) aVersions[0]).intValue();
                        int      nRequestSupport = ((Integer) aVersions[1]).intValue();
                
                        sb.append("[" + nRequestSupport + ", " + nRequestCurrent + "]");
                        }
                    if (iter.hasNext())
                        {
                        sb.append(", ");
                        }
                    }
                sb.append('}');
                return sb.toString();
                }
            
            // Declared at the super level
            /**
             * Factory method: create a new Response instance.
            * 
            * @param  the MessageFactory used to create the new Response object
            * 
            * @return a new Response object
             */
            protected com.tangosol.coherence.component.net.extend.message.Response instantiateResponse(com.tangosol.net.messaging.Protocol.MessageFactory factory)
                {
                return (Peer.MessageFactory.OpenConnectionResponse) factory.createMessage(Peer.MessageFactory.OpenConnectionResponse.TYPE_ID);
                }
            
            // Declared at the super level
            public void readExternal(com.tangosol.io.pof.PofReader in)
                    throws java.io.IOException
                {
                // import com.tangosol.net.Member;
                // import com.tangosol.util.UUID;
                // import java.util.HashMap;
                
                super.readExternal(in);
                
                setClientId((UUID) in.readObject(1));
                setEdition(in.readInt(2));
                setProtocolVersionMap(in.readMap(3, new HashMap()));
                setIdentityToken(in.readByteArray(4));
                setMember((Member) in.readObject(5));
                setClusterName(in.readString(6));
                setServiceName(in.readString(7));
                }
            
            // Accessor for the property "ClientId"
            /**
             * Setter for property ClientId.<p>
            * The unique identifier (UUID) of the client that sent this Request.
             */
            public void setClientId(com.tangosol.util.UUID uuid)
                {
                __m_ClientId = uuid;
                }
            
            // Accessor for the property "ClusterName"
            /**
             * Setter for property ClusterName.<p>
            * The name of the cluster the peer wishes to connect to.
            * 
            * @since 12.2.1
             */
            public void setClusterName(String sName)
                {
                __m_ClusterName = sName;
                }
            
            // Accessor for the property "Edition"
            /**
             * Setter for property Edition.<p>
            * The product edition used by the client.
            * 
            * @see Component.Application.Console.Coherence#getEdition
             */
            public void setEdition(int nEdition)
                {
                __m_Edition = nEdition;
                }
            
            // Accessor for the property "IdentityToken"
            /**
             * Setter for property IdentityToken.<p>
            * An optional token representing a Subject to associate with
            * "Channel0". Operations performed on receipt of Messages sent via
            * "Channel0" will be performed on behalf of this Subject.
             */
            public void setIdentityToken(byte[] abToken)
                {
                __m_IdentityToken = abToken;
                }
            
            // Accessor for the property "Member"
            /**
             * Setter for property Member.<p>
            * The optional Member object to associate with the new Connection.
             */
            public void setMember(com.tangosol.net.Member member)
                {
                __m_Member = member;
                }
            
            // Accessor for the property "ProtocolVersionMap"
            /**
             * Setter for property ProtocolVersionMap.<p>
            * A map of required Protocols. The keys are the names of the
            * required Protocols and the values are two element Integer arrays,
            * the first element being the current version and the second being
            * the supported version of the corresponding Protocol.
             */
            public void setProtocolVersionMap(java.util.Map map)
                {
                __m_ProtocolVersionMap = map;
                }
            
            // Accessor for the property "ServiceName"
            /**
             * Setter for property ServiceName.<p>
            * The name of the service the peer wishes to connect to.
            * 
            * @since 12.2.1
             */
            public void setServiceName(String sName)
                {
                __m_ServiceName = sName;
                }
            
            // Accessor for the property "Subject"
            /**
             * Setter for property Subject.<p>
            * The identity under which Messages received by "Channel0" will be
            * executed.
             */
            public void setSubject(javax.security.auth.Subject subject)
                {
                __m_Subject = subject;
                }
            
            // Declared at the super level
            public void writeExternal(com.tangosol.io.pof.PofWriter out)
                    throws java.io.IOException
                {
                super.writeExternal(out);
                
                out.writeObject(1, getClientId());
                out.writeInt(2, getEdition());
                out.writeMap(3, getProtocolVersionMap(), String.class, Integer[].class);
                out.writeByteArray(4, getIdentityToken());
                out.writeObject(5, getMember());
                out.writeString(6, getClusterName());
                out.writeString(7, getServiceName());
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$OpenConnectionRequest$Status
            
            /**
             * Implementation of the Request$Status interface.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Status
                    extends    com.tangosol.coherence.component.net.extend.message.Request.Status
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Status()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenConnectionRequest.Status();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$OpenConnectionRequest$Status".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$OpenConnectionResponse
        
        /**
         * Response to an OpenChannelRequest.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static abstract class OpenConnectionResponse
                extends    com.tangosol.coherence.component.net.extend.message.Response
            {
            // ---- Fields declarations ----
            
            /**
             * Property ProtocolVersionMap
             *
             * A map of negotiated Protocol versions. The keys are the names of
             * the required Protocols and the values are the negotiated version
             * numbers of the corresponding Protocol.
             */
            private java.util.Map __m_ProtocolVersionMap;
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = 0;
            
            // Initializing constructor
            public OpenConnectionResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$OpenConnectionResponse".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            /**
             * Return a human-readable description of this component.
            * 
            * @return a String representation of this component
             */
            protected String getDescription()
                {
                return super.getDescription()
                        + ", ProtocolVersionMap=" + getProtocolVersionMap();
                }
            
            // Accessor for the property "ProtocolVersionMap"
            /**
             * Getter for property ProtocolVersionMap.<p>
            * A map of negotiated Protocol versions. The keys are the names of
            * the required Protocols and the values are the negotiated version
            * numbers of the corresponding Protocol.
             */
            public java.util.Map getProtocolVersionMap()
                {
                return __m_ProtocolVersionMap;
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            public void readExternal(com.tangosol.io.pof.PofReader in)
                    throws java.io.IOException
                {
                super.readExternal(in);
                
                setProtocolVersionMap(in.readMap(6, null));
                }
            
            // Accessor for the property "ProtocolVersionMap"
            /**
             * Setter for property ProtocolVersionMap.<p>
            * A map of negotiated Protocol versions. The keys are the names of
            * the required Protocols and the values are the negotiated version
            * numbers of the corresponding Protocol.
             */
            public void setProtocolVersionMap(java.util.Map map)
                {
                __m_ProtocolVersionMap = map;
                }
            
            // Declared at the super level
            public void writeExternal(com.tangosol.io.pof.PofWriter out)
                    throws java.io.IOException
                {
                super.writeExternal(out);
                
                out.writeMap(6, getProtocolVersionMap());
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$PingRequest
        
        /**
         * This Request is used to test the integrity of a Connection.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class PingRequest
                extends    com.tangosol.coherence.component.net.extend.message.Request
            {
            // ---- Fields declarations ----
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = 3;
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
                __mapChildren.put("Status", Peer.MessageFactory.PingRequest.Status.get_CLASS());
                }
            
            // Default constructor
            public PingRequest()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public PingRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.PingRequest();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$PingRequest".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            /**
             * Factory method: create a new Response instance.
            * 
            * @param  the MessageFactory used to create the new Response object
            * 
            * @return a new Response object
             */
            protected com.tangosol.coherence.component.net.extend.message.Response instantiateResponse(com.tangosol.net.messaging.Protocol.MessageFactory factory)
                {
                // import Component.Net.Extend.Message.Response as com.tangosol.coherence.component.net.extend.message.Response;
                
                return (com.tangosol.coherence.component.net.extend.message.Response) factory.createMessage(Peer.MessageFactory.PingResponse.TYPE_ID);
                }
            
            // Declared at the super level
            /**
             * Called when the Request is run.
            * 
            * @param response  the Response that should be populated with the
            * result of running the Request
             */
            protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
                {
                _assert(getChannel().getId() == 0);
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$PingRequest$Status
            
            /**
             * Implementation of the Request$Status interface.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Status
                    extends    com.tangosol.coherence.component.net.extend.message.Request.Status
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Status()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.PingRequest.Status();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$PingRequest$Status".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$PingResponse
        
        /**
         * Response to a PingRequest
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class PingResponse
                extends    com.tangosol.coherence.component.net.extend.message.Response
            {
            // ---- Fields declarations ----
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = 4;
            
            // Default constructor
            public PingResponse()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public PingResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.PingResponse();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$PingResponse".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            public void run()
                {
                // import Component.Net.Extend.Channel;
                // import Component.Net.Extend.Connection;
                
                Channel channel = (Channel) getChannel();
                _assert(channel.getId() == 0);
                
                Connection connection = (Connection) channel.getConnection();
                connection.setPingLastMillis(0L);
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$MessageFactory$Response
        
        /**
         * Generic Response used for all internal Requests.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Response
                extends    com.tangosol.coherence.component.net.extend.message.Response
            {
            // ---- Fields declarations ----
            
            /**
             * Property TYPE_ID
             *
             * The type identifier for this Message component class.
             */
            public static final int TYPE_ID = -9;
            
            // Default constructor
            public Response()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Response(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.Response();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$MessageFactory$Response".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            public int getTypeId()
                {
                return TYPE_ID;
                }
            
            // Declared at the super level
            public void run()
                {
                // no-op
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer$Protocol
    
    /**
     * The Protocol used by the ConnectionManager to manage the lifecycle of
     * Connection and Channel objects. 
     * 
     * The name of this Protocol is "MessagingProtocol".
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Protocol
            extends    com.tangosol.coherence.component.net.extend.Protocol
        {
        // ---- Fields declarations ----
        
        /**
         * Property PROTOCOL_NAME
         *
         * This name of this Protocol.
         */
        public static final String PROTOCOL_NAME = "MessagingProtocol";
        
        // Default constructor
        public Protocol()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Protocol(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setVersionCurrent(3);
                setVersionSupported(2);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.Protocol();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Peer$Protocol".replace('/', '.'));
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
        
        // Declared at the super level
        /**
         * Getter for property Name.<p>
        * The name of the Protocol.
        * 
        * @see com.tangosol.net.Protocol#getName
         */
        public String getName()
            {
            return PROTOCOL_NAME;
            }
        
        // Declared at the super level
        /**
         * Instantiate a new MessageFactory for the given version of this
        * Protocol.
        * 
        * @param nVersion  the version of the Protocol that the returned
        * MessageFactory will use
        * 
        * @return a new MessageFactory for the given version of this Protocol
         */
        protected com.tangosol.coherence.component.net.extend.MessageFactory instantiateMessageFactory(int nVersion)
            {
            return (Peer.MessageFactory) ((Peer) get_Module())._newChild("MessageFactory");
            }
        }
    }
