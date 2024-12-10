/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.messaging;


/**
* Request is the root interface for all request messages sent by peer
* endpoints through a {@link Channel}.
* <p>
* A Request object is created by a {@link Protocol.MessageFactory} and has an
* identifier that uniquely identifies the Message instance.
*
* @author jh  2006.04.05
*
* @see Protocol.MessageFactory
*
* @since Coherence 3.2
*/
public interface Request
        extends Message
    {
    /**
    * Return the unique identifier for this Request.
    *
    * @return an identifier that uniquely identifies this Request object
    */
    public long getId();

    /**
    * Configure the unique identifier for this Request.
    *
    * @param lId  an identifier that uniquely identifies this Request object
    */
    public void setId(long lId);

    /**
    * Return the Status for this Request that can be used to wait for and
    * retrieve the Response.
    *
    * @return the Status or null if the Status hasn't been initialized
    */
    public Status getStatus();

    /**
    * Configure the Status for this Request.
    *
    * @param status  the new Status
    *
    * @throws IllegalStateException if the Status has already been configured
    */
    public void setStatus(Status status);

    /**
    * Return the Response for this Request.
    * <p>
    * If not already available, the Response must be created using the
    * {@link Protocol.MessageFactory} associated with the Channel that this
    * Request was sent through.
    *
    * @return the Response; must not be null
    */
    public Response ensureResponse();


    // ----- Status inner interface -----------------------------------------

    /**
    * A Status represents an asynchronous {@link Request} sent to a peer.
    * <p>
    * The status of the Request can be determined by calling the
    * {@link #isClosed()} method. If this method returns false, the Request
    * is still in progress. A return value of true indicates that the Request
    * has either completed successfully, completed unsuccessfully, or has
    * been canceled.
    * <p>
    * When the Request completes, the Response sent by the peer can be
    * retrieved using the {@link #getResponse()} method. If this method
    * returns null, the Request was explicitly {@link #cancel() canceled}.
    * <p>
    * Rather than constantly polling the Request for the outcome of the
    * Request, a thread can alternatively use the Status to
    * {@link #waitForResponse(long) wait} for the Request to complete.
    *
    * @author jh  2006.03.23
    *
    * @see Response
    *
    * @since Coherence 3.2
    */
    public interface Status
        {
        /**
        * Determine if the Request represented by this Status has been
        * completed successfully, completed unsuccessfully, or canceled.
        *
        * @return true if the Request has been completed successfully,
        *         completed unsuccessfully, or canceled; false if the Request
        *         is still pending
        */
        public boolean isClosed();

        /**
        * Cancel the Request represented by this Status.
        * <p>
        * The requestor can call this method when it is no longer interested
        * in a Response or outcome of the Request.
        */
        public void cancel();

        /**
        * Cancel the Request represented by this Status due to an error
        * condition.
        * <p>
        * After this method is called, the {@link #getResponse} method will
        * throw this exception (wrapping it if necessary).
        *
        * @param e  the reason that the Request is being cancelled
        */
        public void cancel(Throwable e);

        /**
        * Return the Request represented by this Status.
        *
        * @return the Request
        */
        public Request getRequest();

        /**
        * Return the Response sent by the peer.
        * <p>
        * This method will only return a non-null value if
        * {@link #isClosed()} is true.
        *
        * @return the Response
        *
        * @throws RuntimeException if the Request is cancelled
        */
        public Response getResponse();

        /**
        * Block the calling thread until the Request is completed
        * successfully, completed unsuccessfully, canceled, or a timeout
        * occurs.
        *
        * @return the Response
        *
        * @throws RuntimeException if the Request is cancelled, a timeout
        *         occurs, or the waiting thread is interrupted
        */
        public Response waitForResponse();

        /**
        * Block the calling thread until the Request is completed
        * successfully, completed unsuccessfully, canceled, or a timeout
        * occurs.
        *
        * @param cMillis  the number of milliseconds to wait for the result
        *                 of the Request; pass zero to block the calling
        *                 thread indefinitely
        *
        * @return the Response
        *
        * @throws RuntimeException if the Request is cancelled, a timeout
        *         occurs, or the waiting thread is interrupted
        */
        public Response waitForResponse(long cMillis);
        }
    }
