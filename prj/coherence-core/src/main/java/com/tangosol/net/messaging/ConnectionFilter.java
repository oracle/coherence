/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.messaging;


/**
* A ConnectionFilter is used to evaluate whether or not a pending Connection
* should be accepted.
*
* @author rhl 2009.10.09
* @since Coherence 3.6
*/
public interface ConnectionFilter
    {
    /**
    * Determine whether or not the specified Connection should be accepted.
    * <p>
    * This method returns normally if the connection attempt is accepted by
    * the filter, otherwise an appropriate ConnectionException is thrown.
    *
    * @param connection  the pending Connection to evaluate
    *
    * @throws ConnectionException if the connection attempt is not accepted
    *                             by the filter
    */
    public void checkConnection(Connection connection)
            throws ConnectionException;
    }
