/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached.server;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.memcached.Request;
import com.tangosol.coherence.memcached.RequestHandler;
import com.tangosol.coherence.memcached.Response;
import com.tangosol.coherence.memcached.Response.ResponseCode;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.cache.KeyAssociation;

import java.io.IOException;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;

/**
 * Task executes Memcached requests using a proxy worker thread. KeyAssociation
 * maintains execution order of the requests on a per connection basis.
 * <p>
 * This class implements the <a href="http://code.google.com/p/memcached/wiki/BinaryProtocolRevamped">
 * binary protocol</a> parsing for binary memcached messages.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public class Task
        implements Runnable, KeyAssociation
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Constructor.
     *
     * @param request  Request
     * @param handler  RequestHandler
     */
    public Task(Request request, RequestHandler handler)
        {
        f_request = request;
        f_handler = handler;
        }

    // ----- KeyAssociation methods -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getAssociatedKey()
        {
        return f_request.getAssociatedKey();
        }

    // ----- Runnable methods -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void run()
        {
        final Request  request  = f_request;
        final Response response = request.getResponse();
        Subject subject = f_handler.getSubject(request);
        if (subject == null)
            {
            handleRequest(request, response);
            }
        else
            {
            Subject.doAs(subject, new PrivilegedAction<Void>()
                {
                public Void run()
                    {
                    handleRequest(request, response);
                    return null;
                    }
                });
            }
        }

    // ----- internal methods -----------------------------------------------

    /**
     * Execute the request.
     *
     * @param request   the Request to process
     * @param response  the Response for the request
     */
    protected void handleRequest(Request request, Response response)
        {
        boolean fFlush = false;
        try
            {
            switch (request.getOpCode())
                {
                case 0x00: // GET Request
                    {
                    f_handler.onGet(request, response);
                    break;
                    }
                case 0x01: // SET Request
                    {
                    f_handler.onSet(request, response);
                    break;
                    }
                case 0x02: // ADD Request
                    {
                    f_handler.onAdd(request, response);
                    break;
                    }
                case 0x03: // REPLACE Request
                    {
                    f_handler.onReplace(request, response);
                    break;
                    }
                case 0x04: // DELETE Request
                    {
                    f_handler.onDelete(request, response);
                    break;
                    }
                case 0x05: // INCREMENT Request
                    {
                    f_handler.onIncrement(request, response);
                    break;
                    }
                case 0x06: // DECREMENT Request
                    {
                    f_handler.onDecrement(request, response);
                    break;
                    }
                case 0x07: // Quit Request
                    {
                    fFlush = true;
                    response.setResponseCode(ResponseCode.OK.getCode());
                    break;
                    }
                case 0x08: // FLUSH Request
                    {
                    f_handler.onFlush(request, response);
                    break;
                    }
                case 0x09: // GETQ Request
                    {
                    f_handler.onGet(request, response);
                    break;
                    }
                case 0x0a: // NO-OP Request
                    {
                    fFlush = true;
                    response.setResponseCode(ResponseCode.OK.getCode());
                    break;
                    }
                case 0x0b: // VERSION Request
                    {
                    fFlush = true;
                    version(request, response);
                    break;
                    }
                case 0x0c: // GETK Request
                    {
                    response.setKey(request.getKey());
                    f_handler.onGet(request, response);
                    break;
                    }
                case 0x0d: // GETKQ Request
                    {
                    response.setKey(request.getKey());
                    f_handler.onGet(request, response);
                    break;
                    }
                case 0x0e: // APPEND Request
                    {
                    f_handler.onAppend(request, response);
                    break;
                    }
                case 0x0f: // PREPEND Request
                    {
                    f_handler.onPrepend(request, response);
                    break;
                    }
                case 0x10: // STAT Request
                    {
                    fFlush = true;
                    stat(request, response);
                    break;
                    }
                case 0x11: // SETQ Request
                    {
                    f_handler.onSet(request, response);
                    break;
                    }
                case 0x12: // ADDQ Request
                    {
                    f_handler.onAdd(request, response);
                    break;
                    }
                case 0x13: // REPLACEQ Request
                    {
                    f_handler.onReplace(request, response);
                    break;
                    }
                case 0x14: // DELETEQ Request
                    {
                    f_handler.onDelete(request, response);
                    break;
                    }
                case 0x15: // INCREMENTQ Request
                    {
                    f_handler.onIncrement(request, response);
                    break;
                    }
                case 0x16: // DECREMENTQ Request
                    {
                    f_handler.onDecrement(request, response);
                    break;
                    }
                case 0x17: // QuitQ Request
                    {
                    fFlush = true;
                    response.setResponseCode(ResponseCode.OK.getCode());
                    break;
                    }
                case 0x18: // FLUSHQ Request
                    {
                    f_handler.onFlush(request, response);
                    break;
                    }
                case 0x19: // APPENDQ Request
                    {
                    f_handler.onAppend(request, response);
                    break;
                    }
                case 0x1a: // PREPENDQ Request
                    {
                    f_handler.onPrepend(request, response);
                    break;
                    }
                case 0x1c: // TOUCH Request
                    {
                    f_handler.onTouch(request, response);
                    break;
                    }
                case 0x1d: // GAT Request
                    {
                    f_handler.onGAT(request, response);
                    break;
                    }
                case 0x1e: // GATQ Request
                    {
                    f_handler.onGAT(request, response);
                    break;
                    }
                case 0x20: // SASL LIST Mechanisms Request
                    {
                    fFlush = true;
                    f_handler.onSASLList(request, response);
                    break;
                    }
                case 0x21: // SASL Auth Request
                    {
                    fFlush = true;
                    f_handler.onSASLAuth(request, response);
                    break;
                    }
                case 0x22: // SASL Auth STEP Request
                    {
                    fFlush = true;
                    f_handler.onSASLAuthStep(request, response);
                    break;
                    }
                default:
                    {
                    Logger.err("Memcached adapter received unknown request: " + request.getOpCode());
                    response.setResponseCode(Response.ResponseCode.NOT_SUPPORTED.getCode());
                    }
                }
            }
        catch (Throwable thr)
            {
            fFlush = true;
            Logger.err("Exception in handling memcached request:", thr);
            response.setResponseCode(Response.ResponseCode.INTERNAL_ERROR.getCode());
            }
        finally
            {
            if (fFlush)
                {
                flush(response, /*fDisponseOnly*/ false);
                }
            }
        }

    /**
     * Handle stat request.
     *
     * @param request  the request
     *
     * @throws IOException
     */
    protected void stat(Request request, Response response)
            throws IOException
        {
        String sPid = CacheFactory.getCluster().getLocalMember().getProcessName();
        response.setKey("pid").setValue(sPid.getBytes("utf-8"));
        }

    /**
     * Handle version request.
     *
     * @param request   the request
     * @param response  the response
     *
     * @throws IOException
     */
    protected void version(Request request, Response response)
            throws IOException
        {
        response.setValue(CacheFactory.VERSION.getBytes("utf-8"));
        }

    /**
     * Flush the response. It may or may not be sent to the client immediately
     * depending upon if there are pending responses ahead of this response.
     *
     * @param response      the response to send to the client
     * @param fDisposeOnly  flag to indicate that the response should not be sent back
     *                      to client and only removed from the pending response queue.
     */
    public static void flush(Response response, boolean fDisposeOnly)
        {
        if (response instanceof BinaryConnection.BinaryResponse)
            {
            ((BinaryConnection.BinaryResponse) response).flush(fDisposeOnly);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The Memcached request.
     */
    protected final Request        f_request;

    /**
     * The RequestHandler.
     */
    protected final RequestHandler f_handler;
    }
