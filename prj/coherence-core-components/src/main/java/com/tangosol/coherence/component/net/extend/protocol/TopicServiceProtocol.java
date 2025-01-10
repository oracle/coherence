
/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.protocol.TopicServiceProtocol

package com.tangosol.coherence.component.net.extend.protocol;

import com.tangosol.coherence.component.net.extend.messageFactory.TopicServiceFactory;

/**
 * The Extend protocol for a {@link com.tangosol.net.TopicService}.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class TopicServiceProtocol
        extends com.tangosol.coherence.component.net.extend.Protocol
    {
    /**
     * Singleton instance
     */
    private static final TopicServiceProtocol __s_Instance = new TopicServiceProtocol();

    /**
     * Property VERSION
     */
    public static final int VERSION = 1;

    public TopicServiceProtocol()
        {
        this(null, null, true);
        }

    public TopicServiceProtocol(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        if (fInit)
            {
            __init();
            }
        }

    @Override
    public void __init()
        {
        // private initialization
        __initPrivate();

        // state initialization: public and protected properties
        try
            {
            setVersionCurrent(VERSION);
            setVersionSupported(VERSION);
            }
        catch (Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }

        // signal the end of the initialization
        set_Constructed(true);
        }

    @Override
    protected void __initPrivate()
        {
        super.__initPrivate();
        }

    /**
     * Getter for property Instance.<p>
     */
    public static TopicServiceProtocol getInstance()
        {
        return __s_Instance;
        }

    /**
     * Instantiate a new MessageFactory for the given version of this Protocol.
     *
     * @param nVersion the version of the Protocol that the returned
     *                 MessageFactory will use
     * @return a new MessageFactory for the given version of this Protocol
     */
    @Override
    protected MessageFactory instantiateMessageFactory(int nVersion)
        {
        return new TopicServiceFactory();
        }

    /**
     * The "component has been initialized" method-notification called out of
     * setConstructed() for the topmost component and that in turn notifies all
     * the children.
     * <p>
     * This notification gets called before the control returns back to this
     * component instantiator (using <code>new Component.X()</code> or
     * <code>_newInstance(sName)</code>) and on the same thread. In addition,
     * visual components have a "posted" notification <code>onInitUI</code> that
     * is called after (or at the same time as) the control returns back to the
     * instantiator and possibly on a different thread.
     */
    @Override
    public void onInit()
        {
        setVersionCurrent(VERSION);
        }
    }
