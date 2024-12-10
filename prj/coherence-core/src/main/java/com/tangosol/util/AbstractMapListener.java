/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
* A base class that simplifies the implementation of a MapListener,
* particularly inner classes that only implement one or two of the
* three event methods.
*
* @author cp  2006.01.18
* @since Coherence 3.1
*/
public abstract class AbstractMapListener
        extends Base
        implements MapListener
    {
    /**
    * {@inheritDoc}
    */
    public void entryInserted(MapEvent evt)
        {
        }

    /**
    * {@inheritDoc}
    */
    public void entryUpdated(MapEvent evt)
        {
        }

    /**
    * {@inheritDoc}
    */
    public void entryDeleted(MapEvent evt)
        {
        }

    @Override
    public boolean equals(Object oThat)
        {
        return oThat != null &&
                super.equals(MapListenerSupport.unwrap((MapListener) oThat));
        }
    }
