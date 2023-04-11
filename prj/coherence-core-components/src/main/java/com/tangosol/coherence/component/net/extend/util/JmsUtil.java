
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.util.JmsUtil

package com.tangosol.coherence.component.net.extend.util;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

/**
 * A collection of JMS-related utility methods.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class JmsUtil
        extends    com.tangosol.coherence.component.net.extend.Util
    {
    // ---- Fields declarations ----
    
    // Initializing constructor
    public JmsUtil(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/util/JmsUtil".replace('/', '.'));
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
     * Close the given Connection. If the Connection is closed successfully,
    * this method returns true; otherwise, this method returns false.
    * 
    * @param connection  the Connection to close; may be null
    * 
    * @return true if successful; false otherwise
     */
    public static boolean close(javax.jms.Connection connection)
        {
        // import javax.jms.JMSException;
        
        if (connection != null)
            {
            try
                {
                connection.close();
                return true;
                }
            catch (JMSException e) {}
            }
        return false;
        }
    
    /**
     * Close the given MessageConsumer. If the MessageConsumer is closed
    * successfully, this method returns true; otherwise, this method returns
    * false.
    * 
    * @param connection  the MessageConsumer to close; may be null
    * 
    * @return true if successful; false otherwise
     */
    public static boolean close(javax.jms.MessageConsumer consumer)
        {
        // import javax.jms.JMSException;
        
        if (consumer != null)
            {
            try
                {
                consumer.close();
                return true;
                }
            catch (JMSException e) {}
            }
        return false;
        }
    
    /**
     * Close the given MessageProducer. If the MessageProducer is closed
    * successfully, this method returns true; otherwise, this method returns
    * false.
    * 
    * @param connection  the MessageProducer to close; may be null
    * 
    * @return true if successful; false otherwise
     */
    public static boolean close(javax.jms.MessageProducer producer)
        {
        // import javax.jms.JMSException;
        
        if (producer != null)
            {
            try
                {
                producer.close();
                return true;
                }
            catch (JMSException e) {}
            }
        return false;
        }
    
    /**
     * Close the given QueueRequestor. If the QueueRequestor is closed
    * successfully, this method returns true; otherwise, this method returns
    * false.
    * 
    * @param requestor  the QueueRequestor to close; may be null
    * 
    * @return true if successful; false otherwise
     */
    public static boolean close(javax.jms.QueueRequestor requestor)
        {
        // import javax.jms.JMSException;
        
        if (requestor != null)
            {
            try
                {
                requestor.close();
                return true;
                }
            catch (JMSException e) {}
            }
        return false;
        }
    
    /**
     * Close the given Session. If the Session is closed successfully, this
    * method returns true; otherwise, this method returns false.
    * 
    * @param connection  the Session to close; may be null
    * 
    * @return true if successful; false otherwise
     */
    public static boolean close(javax.jms.Session session)
        {
        // import javax.jms.JMSException;
        
        if (session != null)
            {
            try
                {
                session.close();
                return true;
                }
            catch (JMSException e) {}
            }
        return false;
        }
    
    /**
     * Close the given TopicRequestor. If the TopicRequestor is closed
    * successfully, this method returns true; otherwise, this method returns
    * false.
    * 
    * @param requestor  the TopicRequestor to close; may be null
    * 
    * @return true if successful; false otherwise
     */
    public static boolean close(javax.jms.TopicRequestor requestor)
        {
        // import javax.jms.JMSException;
        
        if (requestor != null)
            {
            try
                {
                requestor.close();
                return true;
                }
            catch (JMSException e) {}
            }
        return false;
        }
    
    /**
     * Create a temporary Destination for the given Connection. 
    * 
    * If the Connection is an instanceof QueueConnection, a new TemporaryQueue
    * is returned. If the Connection is an instanceof TopicConnection, a new
    * TemporaryTopic is returned.
    * 
    * @param connection  the Connection used to create the temporary
    * Destination
    * 
    * @return a new temporary Destination
     */
    public static javax.jms.Destination createDestination(javax.jms.Connection connection)
            throws javax.jms.JMSException
        {
        // import javax.jms.Destination;
        // import javax.jms.QueueConnection;
        // import javax.jms.QueueSession;
        // import javax.jms.Session;
        // import javax.jms.TopicConnection;
        // import javax.jms.TopicSession;
        
        Destination destination = null;
        Session     session     = createSession(connection);
        
        try
            {
            if (connection instanceof QueueConnection)
                {
                destination = ((QueueSession) session).createTemporaryQueue();
                }
            else if (connection instanceof TopicConnection)
                {
                destination = ((TopicSession) session).createTemporaryTopic();
                }
            }
        finally
            {
            close(session);
            }
        
        return destination;
        }
    
    /**
     * Create a MessageConsumer for the given Session. 
    * 
    * If the Connection is an instanceof QueueConnection, a new QueueReceiver
    * is returned. If the Connection is an instanceof TopicConnection, a new
    * TopicSubscriber is returned.
    * 
    * The returned MessageConsumer will consume JMS Messages from the specified
    * Destination that pass the given message selector. The Destination must be
    * compatible with the given Session.
    * 
    * @param connection  the Connection that created the given Session
    * @param session       the Session used to create the MessageConsumer
    * @param destination  the Destination from which Messages will be consumed
    * @param sSelector    a Message selection string
    * 
    * @return a new temporary Destination
     */
    public static javax.jms.MessageConsumer createMessageConsumer(javax.jms.Connection connection, javax.jms.Session session, javax.jms.Destination destination, String sSelector)
            throws javax.jms.JMSException
        {
        // import javax.jms.MessageConsumer;
        // import javax.jms.Queue;
        // import javax.jms.QueueConnection;
        // import javax.jms.QueueSession;
        // import javax.jms.Topic;
        // import javax.jms.TopicConnection;
        // import javax.jms.TopicSession;
        
        MessageConsumer consumer = null;
        
        if (connection instanceof QueueConnection)
            {
            _assert(destination == null || destination instanceof Queue);
            consumer = ((QueueSession) session).createReceiver((Queue)destination,
                sSelector);
            }
        else if (connection instanceof TopicConnection)
            { 
            _assert(destination == null || destination instanceof Topic);
            consumer = ((TopicSession) session).createSubscriber((Topic)destination,
                sSelector, true);
            }
        
        return consumer;
        }
    
    /**
     * Create a MessageProducer for the given Session. 
    * 
    * If the Session is an instanceof QueueSession, a new anonymous QueueSender
    * is returned. If the Connection is an instanceof TopicSession, a new
    * anonymous TopicPublisher is returned.
    * 
    * @param session  the Session used to create the MessageProducer
    * 
    * @return a new MessageProducer
     */
    public static javax.jms.MessageProducer createMessageProducer(javax.jms.Session session)
            throws javax.jms.JMSException
        {
        // import javax.jms.MessageProducer;
        // import javax.jms.QueueSession;
        // import javax.jms.TopicSession;
        
        MessageProducer producer = null;
        
        if (session instanceof QueueSession)
            {
            producer = ((QueueSession) session).createSender(null);
            }
        else if (session instanceof TopicSession)
            {
            producer = ((TopicSession) session).createPublisher(null);  
            }
        
        return producer;
        }
    
    /**
     * Create a new Session from the given Connection. 
    * 
    * If the Connection is an instanceof QueueConnection, a new QueueSession is
    * returned. If the Connection is an instanceof TopicConnection, a new
    * TopicSession is returned.
    * 
    * @param connection  the Connection used to create the Session
    * 
    * @return a new Session for the given Connection
     */
    public static javax.jms.Session createSession(javax.jms.Connection connection)
            throws javax.jms.JMSException
        {
        // import javax.jms.QueueSession;
        // import javax.jms.QueueConnection;
        // import javax.jms.Session;
        // import javax.jms.TopicConnection;
        // import javax.jms.TopicSession;
        
        Session session = null;
        
        if (connection instanceof QueueConnection)
            {
            session = ((QueueConnection) connection)
                .createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            }
        else if (connection instanceof TopicConnection)
            {
            session = ((TopicConnection) connection)
                .createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            }
        
        return session;
        }
    
    /**
     * Delete the given temporary Destination. If the Destination is deleted
    * successfully, this method returns true; otherwise, this method returns
    * false.
    * 
    * @param connection  the temporary Destination to delete; may be null
    * 
    * @return true if successful; false otherwise
     */
    public static boolean delete(javax.jms.Destination destination)
        {
        // import javax.jms.JMSException;
        // import javax.jms.TemporaryQueue;
        // import javax.jms.TemporaryTopic;
        
        if (destination instanceof TemporaryQueue)
            {
            try
                {
                ((TemporaryQueue) destination).delete();
                return true;
                }
            catch (JMSException e) {}
            }
        else if (destination instanceof TemporaryTopic)
            {
            try
                {
                ((TemporaryTopic) destination).delete();
                return true;
                }
            catch (JMSException e) {}
            }
        return false;
        }
    
    /**
     * Read a packed integer value from the given BytesMessage.
    * 
    * @param message  the BytesMessage to read from
    * 
    * @return the integer value read from the BytesMessage
    * 
    * @throws JMSException on read error
     */
    public static int readPackedInt(javax.jms.BytesMessage message)
            throws javax.jms.JMSException
        {
        // this is an inlined version of BufferInput#readPackedInt()
        int     b     = message.readUnsignedByte();
        int     n     = b & 0x3F;           // only 6 bits of data in first byte
        int     cBits = 6;
        boolean fNeg  = (b & 0x40) != 0;    // seventh bit is a sign bit
        
        while ((b & 0x80) != 0)             // eighth bit is the continuation bit
            {
            b      = message.readUnsignedByte();
            n     |= ((b & 0x7F) << cBits);
            cBits += 7;
            }
        
        if (fNeg)
            {
            n = ~n;
            }
        
        return n;
        }
    
    /**
     * Send the given Message to the given Destination using the supplied
    * MessageProducer.
    * 
    * If the MessageProducer is a QueueSender, the QueueSender.send(Message,
    * int, int, long) method is used to send
    * the message. If the MessageProducer is a TopicPublisher, the
    * TopicPublisher.publish(Message, int, int, long) method is used to send
    * the message.
     */
    public static void send(javax.jms.MessageProducer producer, javax.jms.Destination destination, javax.jms.Message message, int nDeliveryMode, int nPriority, long cExpiry)
            throws javax.jms.JMSException
        {
        // import javax.jms.Queue;
        // import javax.jms.QueueSender;
        // import javax.jms.Topic;
        // import javax.jms.TopicPublisher;
        
        if (destination == null)
            {
            if (producer instanceof QueueSender)
                {
                ((QueueSender) producer).send(message, nDeliveryMode, nPriority,
                    cExpiry);
                }
            else if (producer instanceof TopicPublisher)
                {
                ((TopicPublisher) producer).publish(message, nDeliveryMode, nPriority,
                    cExpiry);
                }    
            }
        else
            {
            if (producer instanceof QueueSender)
                {
                _assert(destination instanceof Queue);
                ((QueueSender) producer).send((Queue) destination, message,
                        nDeliveryMode, nPriority, cExpiry);
                }
            else if (producer instanceof TopicPublisher)
                {
                _assert(destination instanceof Topic);
                ((TopicPublisher) producer).publish((Topic) destination, message,
                        nDeliveryMode, nPriority, cExpiry);
                }
            }
        }
    
    /**
     * Send the given Message using the supplied MessageProducer.
    * 
    * If the MessageProducer is a QueueSender, the QueueSender.send() method is
    * used to send the message. If the MessageProducer is a TopicPublisher, the
    * TopicPublisher.publish() method is used to send the message.
     */
    public static void send(javax.jms.MessageProducer producer, javax.jms.Message message, int nDeliveryMode, int nPriority, long cExpiry)
            throws javax.jms.JMSException
        {
        send(producer, null, message, nDeliveryMode, nPriority, cExpiry);
        }
    
    /**
     * Write a packed integer value to the given BytesMessage.
    * 
    * @param message  the BytesMessage to write to
    * @param n               the integer value to write
    * 
    * @return the number of bytes required to write the integer value
    * 
    * @throws JMSException on write error
     */
    public static void writePackedInt(javax.jms.BytesMessage message, int n)
            throws javax.jms.JMSException
        {
        // this is an inlined version of BufferOutput#writePackedInt()
        
        // first byte contains sign bit (bit 7 set if neg)
        int b = 0;
        if (n < 0)
            {
            b = 0x40;
            n = ~n;
            }
        
        // first byte contains only 6 data bits
        b |= (byte) (n & 0x3F);
        n >>>= 6;
        
        while (n != 0)
            {
            b |= 0x80;          // bit 8 is a continuation bit
            message.writeByte((byte) b);
        
            b = (n & 0x7F);
            n >>>= 7;
            }
        
        // remaining byte
        message.writeByte((byte) b);
        }
    }
