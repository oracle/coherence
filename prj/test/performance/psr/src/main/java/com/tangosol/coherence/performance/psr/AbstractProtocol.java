/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.psr;


import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.net.messaging.Message;
import com.tangosol.net.messaging.Protocol;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;


/**
* A base Protocol implementation.
*
* @author jh  2007.02.12
*/
public abstract class AbstractProtocol
        implements Protocol, Protocol.MessageFactory
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public AbstractProtocol()
        {
        super();

        // create the request identifier generator
        m_counter = new AtomicLong();
        }


    // ----- Protocol interface ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public int getCurrentVersion()
        {
        return 0;
        }

    /**
    * {@inheritDoc}
    */
    public int getSupportedVersion()
        {
        return 0;
        }

    /**
    * {@inheritDoc}
    */
    public MessageFactory getMessageFactory(int nVersion)
        {
        if (nVersion != 0)
            {
            throw new IllegalArgumentException("Unsupported version: " + nVersion);
            }
        return this;
        }


    // ----- MessageFactory interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public Protocol getProtocol()
        {
        return this;
        }

    /**
    * {@inheritDoc}
    */
    public int getVersion()
        {
        return 0;
        }

    /**
    * {@inheritDoc}
    */
    public Message createMessage(int nType)
        {
        Message msg = instantiateMessage(nType);
        if (msg instanceof AbstractRequest)
            {
            ((AbstractRequest) msg).setId(m_counter.incrementAndGet());
            }

        return msg;
        }

    /**
    * Create a return a new Message instance of the specified type.
    *
    * @param nType  the type of Message to create
    *
    * @return a new instance of the Message
    */
    protected Message instantiateMessage(int nType)
        {
        throw new IllegalArgumentException("Illegal message type: " + nType);
        }


    // ----- Message classes ------------------------------------------------

    /**
    * Base Message implementation
    */
    public abstract static class AbstractMessage
            extends com.tangosol.net.messaging.AbstractMessage
            implements PortableObject, Serializable
        {
        }

    /**
    * Base Request implementation.
    */
    public abstract static class AbstractRequest<T>
            extends com.tangosol.net.messaging.AbstractRequest
            implements PortableObject, RemoteCallable<T>
        {
        // ----- RemoteCallable interface ---------------------------------

        @SuppressWarnings("unchecked")
        @Override
        public T call() throws Exception
            {
            RunnerProtocol.RunnerResponse response = new RunnerProtocol.RunnerResponse();

            process(response);

            return (T) response.getResult();
            }


        // ----- PortableObject interface ---------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            setId(in.readLong(0));
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeLong(0, getId());
            }
        }

    /**
    * Base Response implementation.
    */
    public abstract static class AbstractResponse
            extends com.tangosol.net.messaging.AbstractResponse
            implements PortableObject
        {
        // ----- PortableObject interface ---------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            setRequestId(in.readLong(0));
            setFailure  (in.readBoolean(1));
            setResult   (in.readObject(2));
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeLong(0, getRequestId());
            out.writeBoolean(1, isFailure());
            out.writeObject(2, getResult());
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The unique request identifier generator.
    */
    private final AtomicLong m_counter;
    }
