/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.messaging;


/**
* Base Response implementation.
*
* @author jh  2007.mm.dd
*/
public abstract class AbstractResponse
        extends AbstractMessage
        implements Response
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public AbstractResponse()
        {
        super();
        }


    // ----- Response interface ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public long getRequestId()
        {
        return m_lId;
        }

    /**
    * {@inheritDoc}
    */
    public void setRequestId(long lId)
        {
        m_lId = lId;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isFailure()
        {
        return m_fFailure;
        }

    /**
    * {@inheritDoc}
    */
    public void setFailure(boolean fFailure)
        {
        m_fFailure = fFailure;
        }

    /**
    * {@inheritDoc}
    */
    public Object getResult()
        {
        return m_oResult;
        }

    /**
    * {@inheritDoc}
    */
    public void setResult(Object oResult)
        {
        m_oResult = oResult;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The Request identifier.
    */
    protected long m_lId;

    /**
    * Failure flag.
    */
    protected boolean m_fFailure;

    /**
    * The result.
    */
    protected Object m_oResult;
    }