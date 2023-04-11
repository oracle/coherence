
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.util.JmsUtil

package com.tangosol.coherence.component.net.extend.util;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueSender;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TemporaryTopic;
import jakarta.jms.Topic;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicPublisher;
import jakarta.jms.TopicSession;

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
    public static boolean close(jakarta.jms.Connection connection)
        {
        // import jakarta.jms.JMSException;
        
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
    public static boolean close(jakarta.jms.MessageConsumer consumer)
        {
        // import jakarta.jms.JMSException;
        
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
    public static boolean close(jakarta.jms.MessageProducer producer)
        {
        // import jakarta.jms.JMSException;
        
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
    public static boolean close(jakarta.jms.QueueRequestor requestor)
        {
        // import jakarta.jms.JMSException;
        
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
    public static boolean close(jakarta.jms.Session session)
        {
        // import jakarta.jms.JMSException;
        
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
    public static boolean close(jakarta.jms.TopicRequestor requestor)
        {
        // import jakarta.jms.JMSException;
        
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
    public static jakarta.jms.Destination createDestination(jakarta.jms.Connection connection)
            throws jakarta.jms.JMSException
        {
        // import jakarta.jms.Destination;
        // import jakarta.jms.QueueConnection;
        // import jakarta.jms.QueueSession;
        // import jakarta.jms.Session;
        // import jakarta.jms.TopicConnection;
        // import jakarta.jms.TopicSession;
        
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
    public static jakarta.jms.MessageConsumer createMessageConsumer(jakarta.jms.Connection connection, jakarta.jms.Session session, jakarta.jms.Destination destination, String sSelector)
            throws jakarta.jms.JMSException
        {
        // import jakarta.jms.MessageConsumer;
        // import jakarta.jms.Queue;
        // import jakarta.jms.QueueConnection;
        // import jakarta.jms.QueueSession;
        // import jakarta.jms.Topic;
        // import jakarta.jms.TopicConnection;
        // import jakarta.jms.TopicSession;
        
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
    public static jakarta.jms.MessageProducer createMessageProducer(jakarta.jms.Session session)
            throws jakarta.jms.JMSException
        {
        // import jakarta.jms.MessageProducer;
        // import jakarta.jms.QueueSession;
        // import jakarta.jms.TopicSession;
        
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
    public static jakarta.jms.Session createSession(jakarta.jms.Connection connection)
            throws jakarta.jms.JMSException
        {
        // import jakarta.jms.QueueSession;
        // import jakarta.jms.QueueConnection;
        // import jakarta.jms.Session;
        // import jakarta.jms.TopicConnection;
        // import jakarta.jms.TopicSession;
        
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
    public static boolean delete(jakarta.jms.Destination destination)
        {
        // import jakarta.jms.JMSException;
        // import jakarta.jms.TemporaryQueue;
        // import jakarta.jms.TemporaryTopic;
        
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
    public static int readPackedInt(jakarta.jms.BytesMessage message)
            throws jakarta.jms.JMSException
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
    public static void send(jakarta.jms.MessageProducer producer, jakarta.jms.Destination destination, jakarta.jms.Message message, int nDeliveryMode, int nPriority, long cExpiry)
            throws jakarta.jms.JMSException
        {
        // import jakarta.jms.Queue;
        // import jakarta.jms.QueueSender;
        // import jakarta.jms.Topic;
        // import jakarta.jms.TopicPublisher;
        
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
    public static void send(jakarta.jms.MessageProducer producer, jakarta.jms.Message message, int nDeliveryMode, int nPriority, long cExpiry)
            throws jakarta.jms.JMSException
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
    public static void writePackedInt(jakarta.jms.BytesMessage message, int n)
            throws jakarta.jms.JMSException
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
