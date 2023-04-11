
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.leaseMessage.ResourceMessage

package com.tangosol.coherence.component.net.message.leaseMessage;

import com.tangosol.coherence.component.net.Lease;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

/**
 * ResourceMessage is the base component for Lease related messages used by
 * ReplicatedCache service that have to deal with a resource update.
 * 
 * Attributes:
 *     Lease
 *     Resource
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ResourceMessage
        extends    com.tangosol.coherence.component.net.message.LeaseMessage
    {
    // ---- Fields declarations ----
    
    /**
     * Property Resource
     *
     * Resource value.
     */
    private Object __m_Resource;
    
    /**
     * Property ResourceBinary
     *
     * Binary form of the resource value; may be null.
     * 
     * This property allows the value to be "pre-serialized" to allow a client
     * thread to perform the serialization to make sure that the resource is
     * indeed serializable, and if not, to report the exception to the caller.
     * 
     * Furthermore, this property allows the resource to be explicitly handled
     * as a binary without having to deserialize/reserialze.
     */
    private com.tangosol.util.Binary __m_ResourceBinary;
    
    /**
     * Property ResourceExpiry
     *
     * Resource expiry value.
     */
    private long __m_ResourceExpiry;
    
    // Default constructor
    public ResourceMessage()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ResourceMessage(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.message.leaseMessage.ResourceMessage();
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/leaseMessage/ResourceMessage".replace('/', '.'));
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
     * Getter for property Lease.<p>
    * Reference to a Lease object that this message carries an information
    * about. This object is always just a copy of an actual Lease.
     */
    public com.tangosol.coherence.component.net.Lease getLease()
        {
        // import Component.Net.Lease;
        // import com.tangosol.util.Binary;
        
        Lease  lease     = super.getLease();
        Object oResource = getResource();
        
        if (oResource instanceof Binary)
            {
            Binary binValue = (Binary) oResource;
            lease.setResourceSize(binValue.length());
            }
        return lease;
        }
    
    // Accessor for the property "Resource"
    /**
     * Getter for property Resource.<p>
    * Resource value.
     */
    public Object getResource()
        {
        return __m_Resource;
        }
    
    // Accessor for the property "ResourceBinary"
    /**
     * Getter for property ResourceBinary.<p>
    * Binary form of the resource value; may be null.
    * 
    * This property allows the value to be "pre-serialized" to allow a client
    * thread to perform the serialization to make sure that the resource is
    * indeed serializable, and if not, to report the exception to the caller.
    * 
    * Furthermore, this property allows the resource to be explicitly handled
    * as a binary without having to deserialize/reserialze.
     */
    public com.tangosol.util.Binary getResourceBinary()
        {
        return __m_ResourceBinary;
        }
    
    // Accessor for the property "ResourceExpiry"
    /**
     * Getter for property ResourceExpiry.<p>
    * Resource expiry value.
     */
    public long getResourceExpiry()
        {
        return __m_ResourceExpiry;
        }
    
    // Declared at the super level
    public void read(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper;
        
        super.read(input);
        
        setResourceExpiry(ExternalizableHelper.readLong(input));
        
        // load the binary value of the resource
        Binary binResource = new Binary(Base.read(input));
        setResourceBinary(binResource);
        
        // we have to defer the deserialization until the ClassLoader is known
        // (see CacheHandler#getCachedResource());
        // since Coherence 2.2: keep it as Binary
        setResource(binResource);
        }
    
    // Accessor for the property "Resource"
    /**
     * Setter for property Resource.<p>
    * Resource value.
     */
    public void setResource(Object oResource)
        {
        __m_Resource = oResource;
        }
    
    // Accessor for the property "ResourceBinary"
    /**
     * Setter for property ResourceBinary.<p>
    * Binary form of the resource value; may be null.
    * 
    * This property allows the value to be "pre-serialized" to allow a client
    * thread to perform the serialization to make sure that the resource is
    * indeed serializable, and if not, to report the exception to the caller.
    * 
    * Furthermore, this property allows the resource to be explicitly handled
    * as a binary without having to deserialize/reserialze.
     */
    public void setResourceBinary(com.tangosol.util.Binary binResource)
        {
        __m_ResourceBinary = binResource;
        }
    
    // Accessor for the property "ResourceExpiry"
    /**
     * Setter for property ResourceExpiry.<p>
    * Resource expiry value.
     */
    public void setResourceExpiry(long cMillis)
        {
        __m_ResourceExpiry = cMillis;
        }
    
    // Declared at the super level
    public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper;
        
        super.write(output);
        
        ExternalizableHelper.writeLong(output, getResourceExpiry());
        
        Binary bin = getResourceBinary();
        if (bin == null)
            {
            bin = ExternalizableHelper.toBinary(getResource(), getService().ensureSerializer());
            }
        bin.writeTo(output);
        
        // cleanup no longer needed data as soon as we can
        setResource(null);
        setResourceBinary(null);
        }
    }
