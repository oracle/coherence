/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached.processor;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.memcached.Request;
import com.tangosol.coherence.memcached.RequestHandler;
import com.tangosol.coherence.memcached.Response;
import com.tangosol.coherence.memcached.Response.ResponseCode;

import com.tangosol.coherence.memcached.server.Task;

import com.tangosol.util.InvocableMap.EntryProcessor;

import com.tangosol.util.processor.AsynchronousProcessor;

import java.util.Iterator;
import java.util.Map;

/**
 * MemcachedAsyncProcessor is an async marker/wrapper class for executing various
 * memcached EP asynchronously.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public class MemcachedAsyncProcessor
        extends AsynchronousProcessor
    {

   /**
    * Constructor.
    * @param handler    RequestHandler to call when the EP returns.
    * @param request    Memcached request
    * @param response   Memcached resposne
    * @param processor  EP to execute async.
    */
    public MemcachedAsyncProcessor(RequestHandler handler, Request request, Response response, EntryProcessor processor)
        {
        super(processor, ((Integer) request.getAssociatedKey()).intValue());
        m_handler  = handler;
        m_request  = request;
        m_response = response;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onComplete()
        {
        super.onComplete();
        RequestHandler handler  = m_handler;
        Request        request  = m_request;
        Response       response = m_response;
        boolean        fQuiet   = false;
        try
            {
            Object oReturn = getReturnValue();
            switch (m_request.getOpCode())
                {
                case 0x00: // GET Request
                    {
                    handler.onGetComplete(request, response, oReturn);
                    break;
                    }
                case 0x01: // SET Request
                    {
                    handler.onSetComplete(request, response, oReturn);
                    break;
                    }
                case 0x02: // ADD Request
                    {
                    handler.onAddComplete(request, response, oReturn);
                    break;
                    }
                case 0x03: // REPLACE Request
                    {
                    handler.onReplaceComplete(request, response, oReturn);
                    break;
                    }
                case 0x04: // DELETE Request
                    {
                    handler.onDeleteComplete(request, response, oReturn);
                    break;
                    }
                case 0x05: // INCREMENT Request
                    {
                    handler.onIncrementComplete(request, response, oReturn);
                    break;
                    }
                case 0x06: // DECREMENT Request
                    {
                    handler.onDecrementComplete(request, response, oReturn);
                    break;
                    }
                case 0x08: // FLUSH Request
                    {
                    handler.onFlushComplete(request, response, oReturn);
                    break;
                    }
                case 0x09: // GETQ Request
                    {
                    handler.onGetComplete(request, response, oReturn);
                    if (response.getResponseCode() == ResponseCode.KEYNF.getCode())
                        {
                        fQuiet = true;
                        }
                    break;
                    }
                case 0x0a: // NO-OP Request
                    {
                    response.setResponseCode(ResponseCode.OK.getCode());
                    break;
                    }
                case 0x0c: // GETK Request
                    {
                    response.setKey(request.getKey());
                    handler.onGetComplete(request, response, oReturn);
                    break;
                    }
                case 0x0d: // GETKQ Request
                    {
                    response.setKey(request.getKey());
                    handler.onGetComplete(request, response, oReturn);
                    if (response.getResponseCode() == ResponseCode.KEYNF.getCode())
                        {
                        fQuiet = true;
                        }
                    break;
                    }
                case 0x0e: // APPEND Request
                    {
                    handler.onAppendComplete(request, response, oReturn);
                    break;
                    }
                case 0x0f: // PREPEND Request
                    {
                    handler.onPrependComplete(request, response, oReturn);
                    break;
                    }
                case 0x11: // SETQ Request
                    {
                    handler.onSetComplete(request, response, oReturn);
                    fQuiet = response.getResponseCode() == ResponseCode.OK.getCode();
                    break;
                    }
                case 0x12: // ADDQ Request
                    {
                    handler.onAddComplete(request, response, oReturn);
                    fQuiet = response.getResponseCode() == ResponseCode.OK.getCode();
                    break;
                    }
                case 0x13: // REPLACEQ Request
                    {
                    handler.onReplaceComplete(request, response, oReturn);
                    fQuiet = response.getResponseCode() == ResponseCode.OK.getCode();
                    break;
                    }
                case 0x14: // DELETEQ Request
                    {
                    handler.onDeleteComplete(request, response, oReturn);
                    fQuiet = response.getResponseCode() == ResponseCode.OK.getCode();
                    break;
                    }
                case 0x15: // INCREMENTQ Request
                    {
                    handler.onIncrementComplete(request, response, oReturn);
                    fQuiet = response.getResponseCode() == ResponseCode.OK.getCode();
                    break;
                    }
                case 0x16: // DECREMENTQ Request
                    {
                    handler.onDecrementComplete(request, response, oReturn);
                    fQuiet = response.getResponseCode() == ResponseCode.OK.getCode();
                    break;
                    }
                case 0x18: // FLUSHQ Request
                    {
                    handler.onFlushComplete(request, response, oReturn);
                    fQuiet = response.getResponseCode() == ResponseCode.OK.getCode();
                    break;
                    }
                case 0x19: // APPENDQ Request
                    {
                    handler.onAppendComplete(request, response, oReturn);
                    fQuiet = response.getResponseCode() == ResponseCode.OK.getCode();
                    break;
                    }
                case 0x1a: // PREPENDQ Request
                    {
                    handler.onPrependComplete(request, response, oReturn);
                    fQuiet = response.getResponseCode() == ResponseCode.OK.getCode();
                    break;
                    }
                case 0x1c: // TOUCH Request
                    {
                    handler.onTouchComplete(request, response, oReturn);
                    break;
                    }
                case 0x1d: // GAT Request
                    {
                    handler.onGATComplete(request, response, oReturn);
                    break;
                    }
                case 0x1e: // GATQ Request
                    {
                    handler.onGATComplete(request, response, oReturn);
                    fQuiet = response.getResponseCode() == ResponseCode.OK.getCode();
                    break;
                    }
                default:
                    {
                    Logger.err("Memcached adapter received unknown request in EP response: " +
                                request.getOpCode());
                    response.setResponseCode(Response.ResponseCode.INTERNAL_ERROR.getCode());
                    }
                }
            }
        catch (Throwable thr)
            {
            Logger.err("Exception in handling memcached async response:", thr);
            response.setResponseCode(Response.ResponseCode.INTERNAL_ERROR.getCode());
            fQuiet = false;
            }
        finally
            {
            Task.flush(response, fQuiet);
            }
        }

    /**
     * Get the object returned by the EP.
     *
     * @return  Object returned from the EP.
     *
     * @throws  Exception
     */
    protected Object getReturnValue() throws Exception
        {
        Map map = (Map) get();
        if (map != null)
            {
            Iterator itr = map.values().iterator();
            return (itr.hasNext()) ? itr.next() : null;
            }
        return null;
        }

    // ----- data members ---------------------------------------------------

    /**
     * RequestHandler to call when the async EP returns.
     */
    protected RequestHandler m_handler;

    /**
     * Memcached request.
     */
    protected Request m_request;

    /**
     * Memcached response.
     */
    protected Response m_response;
    }
