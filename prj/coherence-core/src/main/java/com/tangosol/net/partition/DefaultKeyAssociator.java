/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import com.oracle.coherence.common.base.NaturalAssociator;

import com.tangosol.net.PartitionedService;


/**
* A DefaultKeyAssociator provides key associations on behalf of keys that
* implement the {@link com.tangosol.net.cache.KeyAssociation} interface.
*
* @author gg 2005.05.19
*
* @since Coherence 3.0
*/
public class DefaultKeyAssociator
        extends    NaturalAssociator
        implements KeyAssociator
    {
    /**
    * Default constructor.
    */
    public DefaultKeyAssociator()
        {
        }

    /**
     * Initialize the KeyAssociator and bind it to a PartitionedService.
     *
     * @param service the PartitionedService that this associator is being
     *                bound to
     */
    public void init(PartitionedService service)
        {
        m_service = service;
        }

    // ----- data fields ----------------------------------------------------

    /**
    * The PartitionedService that this DefaultKeyAssociator is bound to.
    */
    protected PartitionedService m_service;
    }
