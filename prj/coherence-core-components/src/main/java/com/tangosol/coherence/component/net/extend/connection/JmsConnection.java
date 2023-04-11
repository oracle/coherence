
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.connection.JmsConnection

package com.tangosol.coherence.component.net.extend.connection;

import com.tangosol.coherence.component.net.extend.util.JmsUtil;
import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.net.messaging.ConnectionException;
import java.io.IOException;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;

/**
 * Connection implementation that wraps a JMS QueueSession.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class JmsConnection
        extends    com.tangosol.coherence.component.net.extend.Connection
        implements javax.jms.MessageListener
    {
    // ---- Fields declarations ----
    
    /**
     * Property MessageDeliveryMode
     *
     * The delivery mode of JMS Messages sent by the JmsConnection. Must be one
     * of DeliveryMode.NON_PERSISTENT or DeliveryMode.PERSISTENT.
     * 
     * @see javax.jms.DeliveryMode
     */
    private int __m_MessageDeliveryMode;
    
    /**
     * Property MessageExpiration
     *
     * The expiration value of JMS Messages sent by the JmsConnection.
     * 
     * @see javax.jms.Message#getJMSExpiration
     */
    private long __m_MessageExpiration;
    
    /**
     * Property MessagePriority
     *
     * The priority of JMS Messages sent by the JmsConnection.
     * 
     * @see javax.jms.Message#getJMSPriority
     */
    private int __m_MessagePriority;
    
    /**
     * Property QueueIn
     *
     * The JMS Queue used by this JmsConnection to receive Messages.
     * 
     * @see #getQueueReceiver
     */
    private javax.jms.Queue __m_QueueIn;
    
    /**
     * Property QueueOut
     *
     * The JMS Queue used by this JmsConnection to send Messages.
     * 
     * @see #getQueueSender
     */
    private javax.jms.Queue __m_QueueOut;
    
    /**
     * Property QueueReceiver
     *
     * The QueueReceiver used to receive Messages from the peer via QueueIn.
     * 
     * @see #getQueueIn
     */
    private javax.jms.QueueReceiver __m_QueueReceiver;
    
    /**
     * Property QueueSender
     *
     * The QueueSender used to send Messages to the peer via QueueOut.
     * 
     * @see #getQueueOut
     */
    private javax.jms.QueueSender __m_QueueSender;
    
    /**
     * Property QueueSession
     *
     * The QueueSession wrapped by this JmsConnection.
     */
    private javax.jms.QueueSession __m_QueueSession;
    
    // Default constructor
    public JmsConnection()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public JmsConnection(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setChannelArray(new com.tangosol.util.SparseArray());
            setChannelPendingArray(new com.tangosol.util.SparseArray());
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
        return new com.tangosol.coherence.component.net.extend.connection.JmsConnection();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/connection/JmsConnection".replace('/', '.'));
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
    
    // Declared at the super level
    /**
     * The close() implementation method. This method is called on the service
    * thread.
    * 
    * @param fNotify  if true, notify the peer that the Connection is being
    * closed
    * @param e  the optional reason why the Connection is being closed
    * @param cMillis  the number of milliseconds to wait for the Connection to
    * close; pass 0L to perform a non-blocking close or -1L to wait forever
    * 
    * @return true iff the invocation of this method closed the Connection
     */
    public boolean closeInternal(boolean fNotify, Throwable e, long cMillis)
        {
        // import Component.Net.Extend.Util.JmsUtil;
        
        if (super.closeInternal(fNotify, e, cMillis))
            {
            JmsUtil.close(getQueueReceiver());
            JmsUtil.close(getQueueSender());
            JmsUtil.close(getQueueSession());
            JmsUtil.delete(getQueueIn());
        
            return true;
            }
        
        return false;
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
                + ", In="  + getQueueIn()
                + ", Out=" + getQueueOut();
        }
    
    // Accessor for the property "MessageDeliveryMode"
    /**
     * Getter for property MessageDeliveryMode.<p>
    * The delivery mode of JMS Messages sent by the JmsConnection. Must be one
    * of DeliveryMode.NON_PERSISTENT or DeliveryMode.PERSISTENT.
    * 
    * @see javax.jms.DeliveryMode
     */
    public int getMessageDeliveryMode()
        {
        return __m_MessageDeliveryMode;
        }
    
    // Accessor for the property "MessageExpiration"
    /**
     * Getter for property MessageExpiration.<p>
    * The expiration value of JMS Messages sent by the JmsConnection.
    * 
    * @see javax.jms.Message#getJMSExpiration
     */
    public long getMessageExpiration()
        {
        return __m_MessageExpiration;
        }
    
    // Accessor for the property "MessagePriority"
    /**
     * Getter for property MessagePriority.<p>
    * The priority of JMS Messages sent by the JmsConnection.
    * 
    * @see javax.jms.Message#getJMSPriority
     */
    public int getMessagePriority()
        {
        return __m_MessagePriority;
        }
    
    // Accessor for the property "QueueIn"
    /**
     * Getter for property QueueIn.<p>
    * The JMS Queue used by this JmsConnection to receive Messages.
    * 
    * @see #getQueueReceiver
     */
    public javax.jms.Queue getQueueIn()
        {
        return __m_QueueIn;
        }
    
    // Accessor for the property "QueueOut"
    /**
     * Getter for property QueueOut.<p>
    * The JMS Queue used by this JmsConnection to send Messages.
    * 
    * @see #getQueueSender
     */
    public javax.jms.Queue getQueueOut()
        {
        return __m_QueueOut;
        }
    
    // Accessor for the property "QueueReceiver"
    /**
     * Getter for property QueueReceiver.<p>
    * The QueueReceiver used to receive Messages from the peer via QueueIn.
    * 
    * @see #getQueueIn
     */
    public javax.jms.QueueReceiver getQueueReceiver()
        {
        return __m_QueueReceiver;
        }
    
    // Accessor for the property "QueueSender"
    /**
     * Getter for property QueueSender.<p>
    * The QueueSender used to send Messages to the peer via QueueOut.
    * 
    * @see #getQueueOut
     */
    public javax.jms.QueueSender getQueueSender()
        {
        return __m_QueueSender;
        }
    
    // Accessor for the property "QueueSession"
    /**
     * Getter for property QueueSession.<p>
    * The QueueSession wrapped by this JmsConnection.
     */
    public javax.jms.QueueSession getQueueSession()
        {
        return __m_QueueSession;
        }
    
    // From interface: javax.jms.MessageListener
    public void onMessage(javax.jms.Message message)
        {
        // import Component.Net.Extend.Util.JmsUtil;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
        // import com.tangosol.io.ByteArrayReadBuffer;
        // import java.io.IOException;
        // import javax.jms.BytesMessage;
        // import javax.jms.JMSException;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer manager = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer) getConnectionManager();
        _assert(manager != null);
        
        if (message instanceof BytesMessage)
            {
            BytesMessage bMessage = (BytesMessage) message;
            try
                {
                int cb = JmsUtil.readPackedInt(bMessage);
                manager.enforceMaxIncomingMessageSize(cb);
                if (cb < 0)
                    {
                    throw new JMSException("Received a message with a negative length");
                    }
                else if (cb == 0)
                    {
                    throw new JMSException("Received a message with a length of zero");
                    }
                else
                    {
                    byte[] ab     = new byte[cb];
                    int    cbRead = bMessage.readBytes(ab);
                    if (cbRead == cb)
                        {
                        // update stats
                        setStatsBytesReceived(getStatsBytesReceived() + cbRead);
                        setStatsReceived(getStatsReceived() + 1);
        
                        // dispatch Message
                        manager.receive(new ByteArrayReadBuffer(ab), this);
                        }
                    else
                        {
                        throw new JMSException("Error reading a message; expected "
                                + cb + " bytes, read " + cbRead + " bytes instead");
                        }
                    }
                }
            catch (JMSException e)
                {
                close(true, e);
                }
            catch (IOException e)
                {
                close(true, e);
                }
            }
        else
            {
            close(true, new JMSException("Received an unexpected message: " + message));
            }
        }
    
    // Declared at the super level
    /**
     * The open() implementation method. This method is called on the service
    * thread.
     */
    public void openInternal()
        {
        // import javax.jms.JMSException;
        // import javax.jms.Queue;
        // import javax.jms.QueueReceiver;
        // import javax.jms.QueueSender;
        // import javax.jms.QueueSession;
        
        super.openInternal();
        
        QueueSession session = getQueueSession();
        _assert(session != null);
        
        Queue queueIn = getQueueIn();
        _assert(queueIn != null);
        
        Queue queueOut = getQueueOut();
        _assert(queueOut != null);
        
        try
            {
            // create a QueueReceiver and QueueSender
            QueueReceiver receiver = session.createReceiver(queueIn);
            QueueSender   sender   = session.createSender(queueOut);
        
            setQueueReceiver(receiver);
            setQueueSender(sender);
            
            // add ourself as a MessageListener
            receiver.setMessageListener(this);
        
            // disable outgoing Message IDs and timestamps
            sender.setDisableMessageID(true);
            sender.setDisableMessageTimestamp(true);
            }
        catch (JMSException e)
            {
            closeInternal(false, e, -1L);
            throw ensureRuntimeException(e, "error opening connection");   
            }
        }
    
    // Declared at the super level
    /**
     * Send the given WriteBuffer over this Connection.
    * 
    * @param wb  the WriteBuffer to send
    * 
    * @throws ConnectionException on fatal Connection error
     */
    public void send(com.tangosol.io.WriteBuffer wb)
            throws com.tangosol.net.messaging.ConnectionException
        {
        // import Component.Net.Extend.Util.JmsUtil;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
        // import com.tangosol.io.ByteArrayWriteBuffer;
        // import com.tangosol.net.messaging.ConnectionException;
        // import java.io.IOException;
        // import javax.jms.BytesMessage;
        // import javax.jms.JMSException;
        // import javax.jms.QueueSession;
        
        int     cb      = wb.length();
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer manager = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer) getConnectionManager();
        try
            {
            manager.enforceMaxOutgoingMessageSize(cb);
            }
        catch (IOException e)
            {
            throw new ConnectionException("error creating a JMS Message", e, this);
            }
        
        super.send(wb);
        
        byte[]       ab      = ((ByteArrayWriteBuffer) wb).getRawByteArray();
        QueueSession session = getQueueSession();
        
        synchronized (session)
            {
            // create a JMS Message
            BytesMessage message;
            try
                {
                message = session.createBytesMessage();
                message.setJMSDeliveryMode(getMessageDeliveryMode());
                message.setJMSExpiration(getMessageExpiration());
                message.setJMSPriority(getMessagePriority());
                message.setJMSReplyTo(getQueueReceiver().getQueue());
        
                JmsUtil.writePackedInt(message, cb);
                message.writeBytes(ab, 0, cb);
                }
            catch (JMSException e)
                {
                throw new ConnectionException("error creating a JMS Message", e, this);
                }
        
            // send the JMS Message to the peer
            try
                {
                getQueueSender().send(message);
                }
            catch (JMSException e)
                {
                throw new ConnectionException("error sending a JMS Message", e, this);
                }
            }
        }
    
    // Accessor for the property "MessageDeliveryMode"
    /**
     * Setter for property MessageDeliveryMode.<p>
    * The delivery mode of JMS Messages sent by the JmsConnection. Must be one
    * of DeliveryMode.NON_PERSISTENT or DeliveryMode.PERSISTENT.
    * 
    * @see javax.jms.DeliveryMode
     */
    public void setMessageDeliveryMode(int nMode)
        {
        __m_MessageDeliveryMode = nMode;
        }
    
    // Accessor for the property "MessageExpiration"
    /**
     * Setter for property MessageExpiration.<p>
    * The expiration value of JMS Messages sent by the JmsConnection.
    * 
    * @see javax.jms.Message#getJMSExpiration
     */
    public void setMessageExpiration(long cMillis)
        {
        __m_MessageExpiration = cMillis;
        }
    
    // Accessor for the property "MessagePriority"
    /**
     * Setter for property MessagePriority.<p>
    * The priority of JMS Messages sent by the JmsConnection.
    * 
    * @see javax.jms.Message#getJMSPriority
     */
    public void setMessagePriority(int nPriority)
        {
        __m_MessagePriority = nPriority;
        }
    
    // Accessor for the property "QueueIn"
    /**
     * Setter for property QueueIn.<p>
    * The JMS Queue used by this JmsConnection to receive Messages.
    * 
    * @see #getQueueReceiver
     */
    public void setQueueIn(javax.jms.Queue queue)
        {
        _assert(!isOpen());
        
        __m_QueueIn = (queue);
        }
    
    // Accessor for the property "QueueOut"
    /**
     * Setter for property QueueOut.<p>
    * The JMS Queue used by this JmsConnection to send Messages.
    * 
    * @see #getQueueSender
     */
    public void setQueueOut(javax.jms.Queue queue)
        {
        _assert(!isOpen());
        
        __m_QueueOut = (queue);
        }
    
    // Accessor for the property "QueueReceiver"
    /**
     * Setter for property QueueReceiver.<p>
    * The QueueReceiver used to receive Messages from the peer via QueueIn.
    * 
    * @see #getQueueIn
     */
    public void setQueueReceiver(javax.jms.QueueReceiver receiver)
        {
        __m_QueueReceiver = receiver;
        }
    
    // Accessor for the property "QueueSender"
    /**
     * Setter for property QueueSender.<p>
    * The QueueSender used to send Messages to the peer via QueueOut.
    * 
    * @see #getQueueOut
     */
    public void setQueueSender(javax.jms.QueueSender sender)
        {
        __m_QueueSender = sender;
        }
    
    // Accessor for the property "QueueSession"
    /**
     * Setter for property QueueSession.<p>
    * The QueueSession wrapped by this JmsConnection.
     */
    public void setQueueSession(javax.jms.QueueSession session)
        {
        _assert(!isOpen());
        
        __m_QueueSession = (session);
        }
    }
