/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached;

import com.oracle.coherence.common.base.Continuation;

import java.io.IOException;

import javax.security.auth.Subject;

/**
 * Memcached RequestHandler Interface.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public interface RequestHandler
    {
    /**
     * Return the current subject associated with the handler.
     *
     * @param request  the request to determine the subject for
     *
     * @return the Subject associated with the specified request
     *
     * @throws SecurityException if the request handler is configured with an
     *         authorization method and the request does not carry a subject
     */
    Subject getSubject(Request request);

    /**
     * Handle a Get Request.
     *
     * @param request   the Request
     * @param response  the Response
     *
     * @throws IOException
     */
    void onGet(Request request, Response response)
            throws IOException;

    /**
     * Handle the Response of the Get EntryProcessor.
     *
     * @param request   the Request
     * @param response  the Response
     * @param oReturn   Object returned from the EP
     *
     * @throws IOException
     */
    void onGetComplete(Request request, Response response, Object oReturn)
            throws IOException;

    /**
     * Handle a Set Request.
     *
     * @param request   the Request
     * @param response  the Response
     *
     * @throws IOException
     */
    void onSet(Request request, Response response)
            throws IOException;

    /**
     * Handle the Response of the Set EntryProcessor.
     *
     * @param request   the Request
     * @param response  the Response
     * @param oReturn   Object returned from the EP
     *
     * @throws IOException
     */
    void onSetComplete(Request request, Response response, Object oReturn)
            throws IOException;

    /**
     * Handle an Add request.
     *
     * @param request   the Request
     * @param response  the Response
     *
     * @throws IOException
     */
    void onAdd(Request request, Response response)
            throws IOException;

    /**
     * Handle the Response of the Add EntryProcessor.
     *
     * @param request   the Request
     * @param response  the Response
     * @param oReturn   Object returned from the EP
     *
     * @throws IOException
     */
    void onAddComplete(Request request, Response response, Object oReturn)
            throws IOException;


    /**
     * Handle a Replace request.
     *
     * @param request   the Request
     * @param response  the Response
     *
     * @throws IOException
     */
    void onReplace(Request request, Response response)
            throws IOException;

    /**
     * Handle the Response of the Replace EntryProcessor.
     *
     * @param request   the Request
     * @param response  the Response
     * @param oReturn   Object returned from the EP
     *
     * @throws IOException
     */
    void onReplaceComplete(Request request, Response response, Object oReturn)
            throws IOException;

    /**
     * Handle a Delete request.
     *
     * @param request   the Request
     * @param response  the Response
     *
     * @throws IOException
     */
    void onDelete(Request request, Response response)
            throws IOException;

    /**
     * Handle the Response of the Delete EntryProcessor.
     *
     * @param request   the Request
     * @param response  the Response
     * @param oReturn   Object returned from the EP
     *
     * @throws IOException
     */
    void onDeleteComplete(Request request, Response response, Object oReturn)
            throws IOException;

    /**
     * Handle an Increment request.
     *
     * @param request   the Request
     * @param response  the Response
     *
     * @throws IOException
     */
    void onIncrement(Request request, Response response)
            throws IOException;

    /**
     * Handle the Response of the Increment EntryProcessor.
     *
     * @param request   the Request
     * @param response  the Response
     * @param oReturn   Object returned from the EP
     *
     * @throws IOException
     */
    void onIncrementComplete(Request request, Response response, Object oReturn)
            throws IOException;

    /**
     * Handle a Decrement request.
     *
     * @param request   the Request
     * @param response  the Response
     *
     * @throws IOException
     */
    void onDecrement(Request request, Response response)
            throws IOException;

    /**
     * Handle the Response of the Decrement EntryProcessor.
     *
     * @param request   the Request
     * @param response  the Response
     * @param oReturn   Object returned from the EP
     *
     * @throws IOException
     */
    void onDecrementComplete(Request request, Response response, Object oReturn)
            throws IOException;

    /**
     * Handle an Append request.
     *
     * @param request   the Request
     * @param response  the Response
     *
     * @throws IOException
     */
    void onAppend(Request request, Response response)
            throws IOException;

    /**
     * Handle the Response of the Append EntryProcessor.
     *
     * @param request   the Request
     * @param response  the Response
     * @param oReturn   Object returned from the EP
     *
     * @throws IOException
     */
    void onAppendComplete(Request request, Response response, Object oReturn)
            throws IOException;

    /**
     * Handle a Prepend request.
     *
     * @param request   the Request
     * @param response  the Response
     *
     * @throws IOException
     */
    void onPrepend(Request request, Response response)
            throws IOException;

    /**
     * Handle the Response of the Prepend EntryProcessor.
     *
     * @param request   the Request
     * @param response  the Response
     * @param oReturn   Object returned from the EP
     *
     * @throws IOException
     */
    void onPrependComplete(Request request, Response response, Object oReturn)
            throws IOException;

    /**
     * Handle a Flush request.
     *
     * @param request   the Request
     * @param response  the Response
     *
     * @throws IOException
     */
    void onFlush(Request request, Response response)
            throws IOException;

    /**
     * Handle the Response of the Flush EntryProcessor.
     *
     * @param request   the Request
     * @param response  the Response
     * @param oReturn   Object returned from the EP
     *
     * @throws IOException
     */
    void onFlushComplete(Request request, Response response, Object oReturn)
            throws IOException;

    /**
     * Handle a Touch request.
     *
     * @param request   the Request
     * @param response  the Response
     *
     * @throws IOException
     */
    void onTouch(Request request, Response response)
            throws IOException;

    /**
     * Handle the Response of the Touch EntryProcessor.
     *
     * @param request   the Request
     * @param response  the Response
     * @param oReturn   Object returned from the EP
     *
     * @throws IOException
     */
    void onTouchComplete(Request request, Response response, Object oReturn)
            throws IOException;

    /**
     * Handle Get and Touch requests.
     *
     * @param request   the Request
     * @param response  the Response
     *
     * @throws IOException
     */
    void onGAT(Request request, Response response)
            throws IOException;

    /**
     * Handle the Response of the GAT EntryProcessor.
     *
     * @param request   the Request
     * @param response  the Response
     * @param oReturn   Object returned from the EP
     *
     * @throws IOException
     */
    void onGATComplete(Request request, Response response, Object oReturn)
            throws IOException;

    /**
     * Handle a supported SASL mechanisms request.
     *
     * @param request   the Request
     * @param response  the Response
     *
     * @throws IOException
     */
    void onSASLList(Request request, Response response)
            throws IOException;

    /**
     * Handle SASL Authentication.
     *
     * @param request   the Request
     * @param response  the Response
     *
     * @throws IOException
     */
    void onSASLAuth(Request request, Response response)
            throws IOException;

    /**
     * Handle a SASL authentication continuation request.
     *
     * @param request   the Request
     * @param response  the Response
     *
     * @throws IOException
     */
    void onSASLAuthStep(Request request, Response response)
            throws IOException;

    /**
     * Signal the Handler that there are no more requests to execute in the
     * current batch. For async execution, this allows the Handler to flush all
     * the batched requests.
     */
    void flush();

    /**
     * Check if the associated cache service is backlogged.
     *
     * @param backlogEndedContinuation  Continuation to call when backlog ends.
     *
     * @return true iff associated cache service is backlogged.
     */
    boolean checkBacklog(Continuation<Void> backlogEndedContinuation);
    }