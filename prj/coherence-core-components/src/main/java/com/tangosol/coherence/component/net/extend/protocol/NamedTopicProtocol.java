
/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.protocol.NamedTopicProtocol

package com.tangosol.coherence.component.net.extend.protocol;

import com.tangosol.coherence.component.net.extend.messageFactory.NamedTopicFactory;

/**
 * The Extend protocol for a {@link com.tangosol.net.topic.NamedTopic}.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class NamedTopicProtocol
        extends com.tangosol.coherence.component.net.extend.Protocol
    {
    // ---- Fields declarations ----

    /**
     * Property Instance
     */
    private static NamedTopicProtocol __s_Instance;

    private static void _initStatic$Default()
        {
        }

    // Static initializer (from _initStatic)
    static
        {
        _initStatic$Default();

        setInstance(new NamedTopicProtocol());
        }

    // Default constructor
    public NamedTopicProtocol()
        {
        this(null, null, true);
        }

    // Initializing constructor
    public NamedTopicProtocol(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setVersionCurrent(1);
            setVersionSupported(1);
            }
        catch (Exception e)
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
        return new NamedTopicProtocol();
        }

    //++ getter for static property _CLASS

    /**
     * Getter for property _CLASS.<p>
     * Property with auto-generated accessor that returns the Class object for a
     * given component.
     */
    public static Class<?> get_CLASS()
        {
        return NamedTopicProtocol.class;
        }

    //++ getter for autogen property _Module

    /**
     * This is an auto-generated method that returns the global [design time]
     * parent component.
     * <p/>
     * Note: the class generator will ignore any custom implementation for this
     * behavior.
     */
    private com.tangosol.coherence.Component get_Module()
        {
        return this;
        }

    // Accessor for the property "Instance"

    /**
     * Getter for property Instance.<p>
     */
    public static NamedTopicProtocol getInstance()
        {
        return __s_Instance;
        }

    // Declared at the super level

    /**
     * Instantiate a new MessageFactory for the given version of this Protocol.
     *
     * @param nVersion the version of the Protocol that the returned
     *                 MessageFactory will use
     * @return a new MessageFactory for the given version of this Protocol
     */
    protected MessageFactory instantiateMessageFactory(int nVersion)
        {
        // import Component.Net.Extend.MessageFactory.NamedTopicFactory;

        return new NamedTopicFactory();
        }

    // Accessor for the property "Instance"

    /**
     * Setter for property Instance.<p>
     */
    public static void setInstance(NamedTopicProtocol protocolInstance)
        {
        __s_Instance = protocolInstance;
        }
    }
