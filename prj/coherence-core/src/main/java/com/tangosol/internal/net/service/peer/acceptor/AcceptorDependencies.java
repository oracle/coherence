/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.internal.net.service.peer.PeerDependencies;

/**
* The AcceptorDependencies interface provides an Acceptor object with its external
* dependencies.
*
* @author pfm  2011.06.27
* @since Coherence 12.1.2
*/
public interface AcceptorDependencies
        extends PeerDependencies
    {
    /**
     * Return the maximum number of simultaneous connections allowed by the Acceptor.
     * A value of 0 implies no limit.
     *
     * @return the maximum number of simultaneous connection allowed
     */
    public int getConnectionLimit();
    }
