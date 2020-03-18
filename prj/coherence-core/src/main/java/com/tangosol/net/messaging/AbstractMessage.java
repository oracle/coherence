/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.messaging;


import com.tangosol.util.Base;


/**
* Base Message implementation.
*
* @author jh  2007.11.16
*/
public abstract class AbstractMessage
        extends Base
        implements Message
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public AbstractMessage()
        {
        super();
        }


    // ----- Message interface ----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Channel getChannel()
        {
        return m_channel;
        }

    /**
    * {@inheritDoc}
    */
    public void setChannel(Channel channel)
        {
        assert (channel != null);
        if (m_channel == null)
            {
            m_channel = channel;
            }
        else
            {
            throw new IllegalStateException();
            }
        }

    /**
    * {@inheritDoc}
    */
    public boolean isExecuteInOrder()
        {
        return false;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The Channel associated with this Message.
    */
    protected transient Channel m_channel;
    }