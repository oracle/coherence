/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net.exabus;


/**
 * A Depot serves as a factory for creating EndPoints and Buses.
 *
 * @author mf  2010.10.06
 */
public interface Depot
    {
    /**
     * Resolve the EndPoint for the specified canonical name.
     *
     * @param sName  the EndPoint's canonical name
     *
     * @return the EndPoint
     *
     * @throws IllegalArgumentException if the format is unresolvable
     */
    public EndPoint resolveEndPoint(String sName);

    /**
     * Create a new MessageBus bound to the specified local EndPoint.
     *
     * @param pointLocal  the local EndPoint or null for an ephemeral EndPoint
     *
     * @return the MessageBus
     */
    public MessageBus createMessageBus(EndPoint pointLocal);

    /**
     * Create a new MemoryBus bound to the specified local EndPoint.
     *
     * @param pointLocal  the local EndPoint or null for an ephemeral EndPoint
     *
     * @return the MemoryBus
     */
    public MemoryBus createMemoryBus(EndPoint pointLocal);
    }
