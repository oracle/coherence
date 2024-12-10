
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.Codec

package com.tangosol.coherence.component.net.extend;

import com.tangosol.io.Evolvable;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.messaging.Message;
import com.tangosol.util.Binary;

/**
 * The default Codec implementation used by all ConnectionManagers.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Codec
        extends    com.tangosol.coherence.component.net.Extend
        implements com.tangosol.net.messaging.Codec
    {
    // ---- Fields declarations ----
    
    // Default constructor
    public Codec()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Codec(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.extend.Codec();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/Codec".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.messaging.Codec
    public com.tangosol.net.messaging.Message decode(com.tangosol.net.messaging.Channel channel, com.tangosol.io.ReadBuffer.BufferInput in)
            throws java.io.IOException
        {
        // import com.tangosol.io.Evolvable;
        // import com.tangosol.io.pof.PofBufferReader$UserTypeReader as com.tangosol.io.pof.PofBufferReader.UserTypeReader;
        // import com.tangosol.io.pof.PofContext;
        // import com.tangosol.io.pof.PortableObject;
        // import com.tangosol.net.messaging.Message;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        // import com.tangosol.util.Binary;
        
        _assert(channel instanceof PofContext);
        
        PofContext ctx        = (PofContext) channel;
        int        nTypeId    = in.readPackedInt();
        int        nVersionId = in.readPackedInt();
        com.tangosol.io.pof.PofBufferReader.UserTypeReader  reader     = new com.tangosol.io.pof.PofBufferReader.UserTypeReader(in, ctx, nTypeId, nVersionId);
        Message    message    = channel.getMessageFactory().createMessage(nTypeId);
        
        _assert(message instanceof PortableObject);
        
        // set the version identifier
        boolean   fEvolvable = message instanceof Evolvable;
        Evolvable evolvable  = null;
        if (fEvolvable)
            {
            evolvable = (Evolvable) message;
            evolvable.setDataVersion(nVersionId);
            }
        
        // read the Message properties
        ((PortableObject) message).readExternal(reader);
        
        // read the future properties
        Binary binFuture = reader.readRemainder();
        if (fEvolvable)
            {
            evolvable.setFutureData(binFuture);
            }
        
        return message;
        }
    
    // From interface: com.tangosol.net.messaging.Codec
    public void encode(com.tangosol.net.messaging.Channel channel, com.tangosol.net.messaging.Message message, com.tangosol.io.WriteBuffer.BufferOutput out)
            throws java.io.IOException
        {
        // import com.tangosol.io.Evolvable;
        // import com.tangosol.io.Serializer;
        // import com.tangosol.io.pof.ConfigurablePofContext;
        // import com.tangosol.io.pof.PofBufferWriter$UserTypeWriter as com.tangosol.io.pof.PofBufferWriter.UserTypeWriter;
        // import com.tangosol.io.pof.PofContext;
        // import com.tangosol.io.pof.PortableObject;
        
        _assert(channel instanceof PofContext);
        _assert(message instanceof PortableObject);
        
        PofContext ctx    = (PofContext) channel;
        com.tangosol.io.pof.PofBufferWriter.UserTypeWriter  writer = new com.tangosol.io.pof.PofBufferWriter.UserTypeWriter(out, ctx, message.getTypeId(), 0);
        
        // enable POF object reference support
        Serializer serializer = channel.getSerializer();
        if (serializer instanceof ConfigurablePofContext)
            {
            ConfigurablePofContext pofCtx = (ConfigurablePofContext) serializer;
            if (pofCtx.isReferenceEnabled())
                {
                writer.enableReference();
                }
            }
        
        // set the version identifier
        boolean   fEvolvable = message instanceof Evolvable;
        Evolvable evolvable  = null;
        if (fEvolvable)
            {
            evolvable = (Evolvable) message;
            writer.setVersionId(Math.max(evolvable.getDataVersion(),
                    evolvable.getImplVersion()));
            }
        
        // write the Message properties
        ((PortableObject) message).writeExternal(writer);
        
        // write the future properties
        writer.writeRemainder(fEvolvable ? evolvable.getFutureData() : null);
        }
    
    // Declared at the super level
    /**
     * Return a human-readable description of this component.
    * 
    * @return a String representation of this component
     */
    protected String getDescription()
        {
        return "Format=POF";
        }
    }
