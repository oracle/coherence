/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.initiator;

import com.tangosol.internal.net.service.peer.PeerDependencies;

/**
* The InitiatorDependencies interface provides an Initiator object with its external
* dependencies.
*
* @author pfm  2011.06.27
* @since Coherence 12.1.2
*/
public interface InitiatorDependencies
        extends PeerDependencies
    {
    /**
     * Return the maximum amount of time (in milliseconds) that the Initiator will wait
     * for a new Connection to be established. If 0, the Initiator will wait indefinitely.
     * This property defaults to the ServiceDependencies.getRequestTimeoutMillis() value.
     *
     * @return the connect timeout
     */
    public long getConnectTimeoutMillis();

    /**
     * Return a default timeout value of the request send timeout value.
     *
     * @return the request send timeout
     */
    public long getRequestSendTimeoutMillis();
    }
