/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.internal.net.service.peer.JmsDependencies;

/**
* The JmsAcceptorDependencies interface provides a JmsAcceptor object with its external
* dependencies.
*
* @author pfm  2011.06.27
* @since Coherence 12.1.2
*/
public interface JmsAcceptorDependencies
        extends AcceptorDependencies, JmsDependencies
    {
    }
