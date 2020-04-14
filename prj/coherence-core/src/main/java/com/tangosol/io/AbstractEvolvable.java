/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.Binary;

import javax.json.bind.annotation.JsonbProperty;


/**
* An abstract base class for implementing Evolvable objects.
*
* @author cp/jh/mf  2006.07.20
*/
public abstract class AbstractEvolvable
        implements Evolvable
    {
    // ----- Evolvable methods ----------------------------------------------
    
    /**
    * {@inheritDoc}
    */
    public abstract int getImplVersion();

    /**
    * {@inheritDoc}
    */
    public int getDataVersion()
        {
        return m_nDataVersion;
        }

    /**
    * {@inheritDoc}
    */
    public void setDataVersion(int nVersion)
        {
        m_nDataVersion = nVersion;
        }

    /**
    * {@inheritDoc}
    */
    public Binary getFutureData()
        {
        return m_binFuture;
        }

    /**
    * {@inheritDoc}
    */
    public void setFutureData(Binary binFuture)
        {
        m_binFuture = binFuture;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The version of the data stream that this object was deserialized from.
    */
    @JsonbProperty("dataVersion")
    private int m_nDataVersion;

    /**
    * The "unknown future data" from the data stream that this object was
    * deserialized from.
    */
    @JsonbProperty("binFuture")
    private Binary m_binFuture;
    }
