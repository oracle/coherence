/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.tangosol.util.Base;

import java.util.Map;


/**
* Abstract base of the BackingMapManager implementations.
*
* @author gg 2002.09.21
*
* @since Coherence 2.0
*/
public abstract class AbstractBackingMapManager
        extends Base
        implements BackingMapManager
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    protected AbstractBackingMapManager()
        {
        }


    // ----- BackingMapManager interface ------------------------------------

    /**
    * {@inheritDoc}
    */
    public void init(BackingMapManagerContext context)
        {
        if (m_context != null && m_context.getCacheService().isRunning())
            {
            throw new IllegalStateException(
                "BackingMapManager is already used by " + m_context.getCacheService());
            }
        m_context = context;
        }

    /**
    * {@inheritDoc}
    */
    public BackingMapManagerContext getContext()
        {
        return m_context;
        }

    /**
    * {@inheritDoc}
    */
    public void releaseBackingMap(String sName, Map map)
        {
        }


    // ----- data members ---------------------------------------------------

    /**
    * The BackingMapManagerContext object for this BackingMapManager
    */
    private BackingMapManagerContext m_context;
    }