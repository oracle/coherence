/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.net.extend.messageFactory;

import com.tangosol.coherence.Component;
import com.tangosol.coherence.component.net.extend.MessageFactory;
import com.tangosol.coherence.component.net.extend.message.Response;
import com.tangosol.util.ListMap;

import java.util.Map;

/**
 * A base class for topics message factories.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class BaseTopicMessageFactory
        extends MessageFactory
    {
    /**
     * The message type identifier for a {@link TopicsResponse}.
     */
    public static final int TYPE_ID_RESPONSE = 0;

    private static ListMap<String, Class<?>> __mapChildren;

    static
        {
        __initStatic();
        }

    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new ListMap<>();
        __mapChildren.put("Response", TopicsResponse.class);
        }

    public BaseTopicMessageFactory()
        {
        this(null, null, true);
        }

    public BaseTopicMessageFactory(String sName, Component compParent, boolean fInit)
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
        // containment initialization: children
        // signal the end of the initialization
        set_Constructed(true);
        }

    @Override
    protected void __initPrivate()
        {
        super.__initPrivate();
        }

    @Override
    protected Map<String, Class<?>> get_ChildClasses()
        {
        return __mapChildren;
        }

    // ----- inner class: TopicsResponse ------------------------------------

    /**
     * A common {@link Response} for topics factories.
     */
    public static class TopicsResponse
            extends Response
        {
        /**
         * Default constructor.
         */
        public TopicsResponse()
            {
            this(null, null, true);
            }

        /**
         * Initializing constructor.
         */
        public TopicsResponse(String sName, Component compParent, boolean fInit)
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
            // signal the end of the initialization
            set_Constructed(true);
            }

        @Override
        protected void __initPrivate()
            {
            super.__initPrivate();
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_RESPONSE;
            }

        @Override
        public void run()
            {
            // no-op
            }
        }
    }
