/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend;


import com.tangosol.internal.net.service.peer.acceptor.AcceptorDependencies;

import com.tangosol.net.ServiceDependencies;


/**
 * The NameServiceDependencies interface provides a NameService with its
 * external dependencies.
 *
 * @author phf  2012.02.01
 *
 * @since Coherence 12.1.2
 */
public interface NameServiceDependencies
        extends ServiceDependencies
    {
    /**
     * Return the AcceptorDependencies.
     *
     * @return AcceptorDependencies
     */
    public AcceptorDependencies getAcceptorDependencies();
    }
