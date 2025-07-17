
/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.Message

package com.tangosol.coherence.component.net.extend;

import com.tangosol.internal.tracing.SpanContext;
import com.tangosol.internal.tracing.TracingHelper;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.messaging.Channel;

import com.tangosol.util.LiteMap;

import java.io.IOException;

import java.util.Map;

/**
 * Base definition of a Message component.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Message
        extends    com.tangosol.coherence.component.net.Extend
        implements com.tangosol.io.pof.EvolvablePortableObject,
                   com.tangosol.net.messaging.Message
    {
    // ---- Fields declarations ----
    
    /**
     * Property Channel
     *
     * The Channel through which this Message was sent or received.
     * 
     * @see com.tangosol.net.messaging.Message#getChannel
     */
    private transient com.tangosol.net.messaging.Channel __m_Channel;
    
    /**
     * Property DataVersion
     *
     * The version associated with the data stream from which this object was
     * deserialized. If the object was constructed (not deserialized), the data
     * version is the same as the implementation version.
     * 
     * @see com.tangosol.io.Evolvable#getDataVersion
     */
    private transient int __m_DataVersion;
    
    /**
     * Property FutureData
     *
     * The unknown remainder of the data stream from which this object was
     * deserialized. The remainder is unknown because it is data that was
     * originally written by a future version of this object's class.
     * 
     * @see com.tangosol.io.Evolvable#getFutureData
     */
    private transient com.tangosol.util.Binary __m_FutureData;

    /**
     * The tracing {@link SpanContext}.
     *
     * @since 15.1.1.0
     */
    private transient SpanContext __m_TracingSpanContext;
    
    /**
     * Property ImplVersion
     *
     * The serialization version supported by the implementing class.
     * 
     * @see com.tangosol.io.Evolvable#getImplVersion
     */
    private transient int __m_ImplVersion;
    
    // Initializing constructor
    public Message(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/Message".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.messaging.Message
    // Accessor for the property "Channel"
    /**
     * Getter for property Channel.<p>
    * The Channel through which this Message was sent or received.
    * 
    * @see com.tangosol.net.messaging.Message#getChannel
     */
    public com.tangosol.net.messaging.Channel getChannel()
        {
        return __m_Channel;
        }
    
    // From interface: com.tangosol.io.pof.EvolvablePortableObject
    // Accessor for the property "DataVersion"
    /**
     * Getter for property DataVersion.<p>
    * The version associated with the data stream from which this object was
    * deserialized. If the object was constructed (not deserialized), the data
    * version is the same as the implementation version.
    * 
    * @see com.tangosol.io.Evolvable#getDataVersion
     */
    public int getDataVersion()
        {
        return __m_DataVersion;
        }
    
    // Declared at the super level
    /**
     * Return a human-readable description of this component.
    * 
    * @return a String representation of this component
     */
    protected String getDescription()
        {
        // import com.tangosol.net.messaging.Channel;
        
        Channel channel  = getChannel();
        String  sChannel = channel == null ? "null" : String.valueOf(channel.getId());
        
        return "Type=" + getTypeId() + ", Channel=" + sChannel;
        }
    
    // From interface: com.tangosol.io.pof.EvolvablePortableObject
    // Accessor for the property "FutureData"
    /**
     * Getter for property FutureData.<p>
    * The unknown remainder of the data stream from which this object was
    * deserialized. The remainder is unknown because it is data that was
    * originally written by a future version of this object's class.
    * 
    * @see com.tangosol.io.Evolvable#getFutureData
     */
    public com.tangosol.util.Binary getFutureData()
        {
        return __m_FutureData;
        }
    
    // From interface: com.tangosol.io.pof.EvolvablePortableObject
    // Accessor for the property "ImplVersion"
    /**
     * Getter for property ImplVersion.<p>
    * The serialization version supported by the implementing class.
    * 
    * @see com.tangosol.io.Evolvable#getImplVersion
     */
    public int getImplVersion()
        {
        return __m_ImplVersion;
        }

    /**
     * Returns the tracing {@link SpanContext}, if any, associated
     * with this message.
     *
     * @return the tracing {@link SpanContext}, if any, associated
     *         with this message
     *
     * @since 15.1.1.0
     */
    public SpanContext getTracingSpanContext()
        {
        return __m_TracingSpanContext;
        }

    // From interface: com.tangosol.net.messaging.Message
    public int getTypeId()
        {
        return 0;
        }
    
    // From interface: com.tangosol.net.messaging.Message
    public boolean isExecuteInOrder()
        {
        return false;
        }
    
    // From interface: com.tangosol.io.pof.EvolvablePortableObject
    public void readExternal(com.tangosol.io.pof.PofReader in)
            throws java.io.IOException
        {
        }
    
    // From interface: com.tangosol.net.messaging.Message
    public void run()
        {
        }
    
    // From interface: com.tangosol.net.messaging.Message
    // Accessor for the property "Channel"
    /**
     * Setter for property Channel.<p>
    * The Channel through which this Message was sent or received.
    * 
    * @see com.tangosol.net.messaging.Message#getChannel
     */
    public void setChannel(com.tangosol.net.messaging.Channel channel)
        {
        _assert(channel != null);
        
        if (getChannel() != null)
            {
            throw new IllegalStateException("channel has already been set");
            }
        
        __m_Channel = (channel);
        }
    
    // From interface: com.tangosol.io.pof.EvolvablePortableObject
    // Accessor for the property "DataVersion"
    /**
     * Setter for property DataVersion.<p>
    * The version associated with the data stream from which this object was
    * deserialized. If the object was constructed (not deserialized), the data
    * version is the same as the implementation version.
    * 
    * @see com.tangosol.io.Evolvable#getDataVersion
     */
    public void setDataVersion(int nVersion)
        {
        __m_DataVersion = nVersion;
        }
    
    // From interface: com.tangosol.io.pof.EvolvablePortableObject
    // Accessor for the property "FutureData"
    /**
     * Setter for property FutureData.<p>
    * The unknown remainder of the data stream from which this object was
    * deserialized. The remainder is unknown because it is data that was
    * originally written by a future version of this object's class.
    * 
    * @see com.tangosol.io.Evolvable#getFutureData
     */
    public void setFutureData(com.tangosol.util.Binary binFuture)
        {
        __m_FutureData = binFuture;
        }
    
    // Accessor for the property "ImplVersion"
    /**
     * Setter for property ImplVersion.<p>
    * The serialization version supported by the implementing class.
    * 
    * @see com.tangosol.io.Evolvable#getImplVersion
     */
    public void setImplVersion(int nVersion)
        {
        __m_ImplVersion = nVersion;
        }

    /**
     * Sets the tracing {@link SpanContext} associated with this
     * message.
     *
     * @param tracingSpanContext the tracing {@link SpanContext}
     *
     * @since 15.1.1.0
     */
    public void setTracingSpanContext(SpanContext tracingSpanContext)
        {
        this.__m_TracingSpanContext = tracingSpanContext;
        }

    // From interface: com.tangosol.io.pof.EvolvablePortableObject
    public void writeExternal(com.tangosol.io.pof.PofWriter out)
            throws java.io.IOException
        {
        }

    /**
     * This method will write the tracing data, if any, to the given
     * {@link PofWriter}.
     *
     * @param pofWriter  the {@link PofWriter}
     * @param nProp      the index the tracing data is being written to
     *                   within the {@link PofWriter}
     *
     * @throws IOException if an error occurs writing the tracing data
     *
     * @since 15.1.1.0
     */
    protected void writeTracing(PofWriter pofWriter, int nProp)
            throws IOException
        {
        SpanContext ctxSpan    = getTracingSpanContext();
        Map         mapSpanCtx = null;

        if (!TracingHelper.isNoop(ctxSpan))
            {
            mapSpanCtx = TracingHelper.getTracer().inject(ctxSpan);
            }

        pofWriter.writeMap(nProp, mapSpanCtx);
        }

    /**
     * Reads the tracing data from the given {@link PofReader}.
     *
     * @param pofReader  the {@link PofReader}
     * @param nProp      the index to read within the {@link PofWriter}
     *                   to obtain the tracing data
     *
     * @throws IOException if an error occurs writing tracing data
     *
     * @since 15.1.1.0
     */
    protected void readTracing(PofReader pofReader, int nProp)
            throws IOException
        {
        Map mapSpanCtx = pofReader.readMap(nProp, new LiteMap());
        if (!mapSpanCtx.isEmpty())
            {
            setTracingSpanContext(TracingHelper.getTracer().extract(mapSpanCtx));
            }
        }
    }
