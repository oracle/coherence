/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net.exabus.spi;

import com.oracle.coherence.common.net.exabus.Bus;
import com.oracle.coherence.common.net.exabus.Depot;
import com.oracle.coherence.common.net.exabus.EndPoint;


/**
 * A Driver represents a distinct bus implementation.
 *
 * @author mf  2010.10.02
 */
public interface Driver
    {
    /**
     * Set the depot associated with this driver.
     *
     * @param depot  the depot associated with the driver
     */
    public void setDepot(Depot depot);

    /**
     * Return the depot associated with this driver.
     *
     * @return the depot associated with this driver
     */
    public Depot getDepot();

    /**
     * Resolve the EndPoint for the specified canonical name.
     *
     * @param sName  the EndPoint's canonical name
     *
     * @return the EndPoint or null if the format is not supported
     *
     * @throws IllegalArgumentException if the format is supported, but the
     *         address is not resolvable
     */
    public EndPoint resolveEndPoint(String sName);

    /**
     * Indicate if the specified EndPoint is supported by this driver.
     *
     * @param point  the EndPoint
     *
     * @return true iff the EndPoint is supported
     */
    public boolean isSupported(EndPoint point);

    /**
     * Create a new Bus bound to the specified local EndPoint.
     *
     * @param pointLocal  the local EndPoint or null for an ephemeral EndPoint
     *
     * @return the Bus
     *
     * @throws IllegalArgumentException if the EndPoint is not compatible
     */
    public Bus createBus(EndPoint pointLocal);
    }
