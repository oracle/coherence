/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.internal;


import com.tangosol.net.ClusterException;


/**
 * ClusterJoinException is used internally to signal that the
 * handshake protocol could not continue joining the cluster.
 *
 * @author rhl  2011.02.08
 * @since  Coherence 3.7
 */
public class ClusterJoinException
        extends ClusterException
    {
    // ----- constructors ----------------------------------------------------

    /**
     * Default constructor.
     */
    public ClusterJoinException()
        {
        super();
        }
    }