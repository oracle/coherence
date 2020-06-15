/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.messaging;


/**
* Response is the root interface for all response messages sent by peer
* endpoints through a {@link Channel}.
* <p>
* Response objects are created by a {@link Protocol.MessageFactory} and are
* associated with a corresponding Request. The status of the Response may be
* determined by calling the {@link #isFailure()} method:
* <ul>
*   <li>if false, the Request was successfully processed by the receiver and
*       the Response object contains a valid result</li>
*   <li>if true, an error or exception occurred while processing the
*       Request</li>
* </ul>
* <p>
*
* @author jh  2006.04.05
*
* @see Protocol.MessageFactory
*
* @since Coherence 3.2
*/
public interface Response
        extends Message
    {
    /**
    * Return the unique identifier of the Request for which this Response is
    * being sent.
    *
    * @return the unique identifier of the Request associated with this
    *         Response
    */
    public long getRequestId();

    /**
    * Associate a Request with this Response.
    *
    * @param lId  the unique identifier of the Request associated with this
    *             Response
    */
    public void setRequestId(long lId);

    /**
    * Determine if an error or exception occurred while processing the
    * Request.
    * <p>
    * If this method returns false, the result of processing the Request can
    * be determined using the {@link #getResult()} method. If this method
    * returns true, {@link #getResult()} may return the cause of the failure
    * (in the form of an Error or Exception object).
    *
    * @return false if the Request was processed successfully; true if an
    *         error or exception occurred while processing the Request
    */
    public boolean isFailure();

    /**
    * Set the error state of this Response.
    *
    * @param fFailure  if true, an error or exception occurred while
    *                  processing the Request associated with this Response
    */
    public void setFailure(boolean fFailure);

    /**
    * Return the result of processing the Request.
    *
    * @return the result of processing the Request associated with this
    *         Response
    */
    public Object getResult();

    /**
    * Set the result of processing the Request.
    *
    * @param oResult  the result of processing the Request
    */
    public void setResult(Object oResult);
    }
