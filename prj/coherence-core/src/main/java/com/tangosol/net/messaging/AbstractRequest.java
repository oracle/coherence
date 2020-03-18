/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.messaging;


import com.tangosol.net.messaging.Protocol.MessageFactory;


/**
* Base Request implementation.
*
* @author jh  2007.11.16
*/
public abstract class AbstractRequest
        extends AbstractMessage
        implements Request
    {
    // ---- constructors ----------------------------------------------------

    /**
    * Default constructor.
    */
    public AbstractRequest()
        {
        super();
        }


    // ----- Message interface ----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void run()
        {
        Response response = ensureResponse();
        try
            {
            process(response);
            }
        catch (RuntimeException t)
            {
            response.setFailure(true);
            response.setResult(t);
            }
        }

    // ----- Request interface ----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public long getId()
        {
        return m_lId;
        }

    /**
    * @see #getId()
    */
    public void setId(long lId)
        {
        m_lId = lId;
        }

    /**
    * {@inheritDoc}
    */
    public Status getStatus()
        {
        return m_status;
        }

    /**
    * {@inheritDoc}
    */
    public void setStatus(Status status)
        {
        if (status == null)
            {
            throw new IllegalArgumentException("null status");
            }
        if (m_status == null)
            {
            m_status = status;
            }
        else
            {
            throw new IllegalStateException();
            }
        }

    /**
    * {@inheritDoc}
    */
    public Response ensureResponse()
        {
        Response response = m_response;
        if (response == null)
            {
            Channel channel = getChannel();
            if (channel == null)
                {
                throw new IllegalStateException("null channel");
                }

            MessageFactory factory = channel.getMessageFactory();
            if (factory == null)
                {
                throw new IllegalStateException("null factory");
                }

            response = m_response = instantiateResponse(factory);
            }

        return response;
        }


    // ----- abstract methods -----------------------------------------------

    /**
    * Create a new Response for this Request.
    *
    * @param factory  the MessageFactory that must be used to create the
    *                 returned Response; never null
    *
    * @return a new Response
    */
    protected abstract Response instantiateResponse(MessageFactory factory);

    /**
    * Process this Request and update the given Response wth the result.
    * <p>
    * Implementations of this method are free to throw an exception while
    * processing the Request. An exception will result in the Response
    * being marked as a failure that the Response result will be the
    * exception itself.
    *
    * @param response  the Response that will be sent back to the requestor
    *
    * @throws RuntimeException on execution error
    */
    protected abstract void process(Response response);


    // ----- data members ---------------------------------------------------

    /**
    * The Request identifier.
    */
    private long m_lId;

    /**
    * The Status.
    */
    protected transient Status m_status;

    /**
    * The Response.
    */
    protected transient Response m_response;
    }
