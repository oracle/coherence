/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

/**
 * DefaultConnectorDependencies is a default implementation for ConnectorDependencies.
 *
 * @author der  2011.07.10
 * @since Coherence 12.1.2
 */
public class DefaultConnectorDependencies
        implements ConnectorDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultConnectorDependencies object.
     */
    public DefaultConnectorDependencies()
        {
        this(null);
        }

    /**
     * Construct a {@link DefaultConnectorDependencies} object, copying the values
     * from the specified DefaultConnectorDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultConnectorDependencies(ConnectorDependencies deps)
        {
        if (deps != null)
            {
            m_cRefreshRequestTimeoutMillis = deps.getRefreshRequestTimeoutMillis();
            m_nRefreshPolicy               = deps.getRefreshPolicy();
            m_cRefreshTimeoutMillis        = deps.getRefreshTimeoutMillis();
            }
        }

    // ----- accessors ------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRefreshPolicy()
        {
        return m_nRefreshPolicy;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultConnectorDependencies setRefreshPolicy(String sPolicy)
        {
        m_nRefreshPolicy = convertRefreshPolicy(sPolicy);
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getRefreshRequestTimeoutMillis()
        {
        return m_cRefreshRequestTimeoutMillis;
        }

    /**
     * Sets the timeout interval for making remote method calls.
     *
     * @param cMillis  timeout interval for making remote method calls
     *
     * @return this object
     */
    protected DefaultConnectorDependencies setRefreshRequestTimeoutMillis(long cMillis)
        {
        m_cRefreshRequestTimeoutMillis = cMillis;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getRefreshTimeoutMillis()
        {
        return m_cRefreshTimeoutMillis;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultConnectorDependencies setRefreshTimeoutMillis(long cMillis)
        {
        m_cRefreshTimeoutMillis = cMillis;
        return this;
        }

    // ----- DefaultConnectorDependencies methods ---------------------------

     /**
     * Convert the refresh policy string to an appropriate integer value.
     *
     * @param sRefreshPolicy  refreshPolicy in string format
     *
     * @return converted refreshPolicy
     */
    public int convertRefreshPolicy(String sRefreshPolicy)
        {
        int nRefreshPolicy;

        if (sRefreshPolicy.equalsIgnoreCase("refresh-behind"))
            {
            nRefreshPolicy = REFRESH_BEHIND;
            }
        else if (sRefreshPolicy.equalsIgnoreCase("refresh-ahead"))
            {
            nRefreshPolicy = REFRESH_AHEAD;
            }
        else if (sRefreshPolicy.equalsIgnoreCase("refresh-onquery"))
            {
            nRefreshPolicy = REFRESH_ONQUERY;
            }
        else if (sRefreshPolicy.equalsIgnoreCase("refresh-expired"))
            {
            nRefreshPolicy = REFRESH_EXPIRED;
            }
        else
            {
            throw new IllegalArgumentException("Error invalid refresh-policy specified: " + sRefreshPolicy);
            }

        return nRefreshPolicy;
        }

    /**
     * Convert the specified refresh policy value to a human-readable string.
     *
     * @param nPolicy  refresh policy in integer format
     *
     * @return converted refresh policy in string format
     */
    protected String formatRefreshPolicy(int nPolicy)
        {
        switch (nPolicy)
            {
            case REFRESH_EXPIRED:
                return "refresh-expired";

            case REFRESH_AHEAD:
                return "refresh-ahead";

            case REFRESH_BEHIND:
                return "refresh-behind";

            case REFRESH_ONQUERY:
                return "refresh-onquery";

            default:
                return "n/a";
            }
        }

    /**
     * Validate the supplied dependencies.
     *
     * @throws IllegalArgumentException if the dependencies are not valid
     *
     * @return this object
     */
    public DefaultConnectorDependencies validate()
        {
        Base.checkRange(getRefreshRequestTimeoutMillis(), 1, Integer.MAX_VALUE, "RefreshRequestTimeout");
        Base.checkRange(getRefreshTimeoutMillis(), 1, Integer.MAX_VALUE, "RefreshTimeout");
        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass())
            + "{RefreshRequestTimeout=" + getRefreshRequestTimeoutMillis()
            + ", RefreshPolicy="        + formatRefreshPolicy(getRefreshPolicy())
            + ", RefreshTimeout="       + getRefreshTimeoutMillis()
            + "}";
        }

    // ----- data members and constants -------------------------------------

    /**
     * The refresh policy the connector will use.
     *
     */
    private int m_nRefreshPolicy = REFRESH_EXPIRED;

    /**
     * The timeout interval for making remote method calls.
     */
    private long m_cRefreshRequestTimeoutMillis = 250;

    /**
     * The number of milliseconds that the managing server can use a model
     * snapshot before a refresh is required.
     */
    private long m_cRefreshTimeoutMillis = 1000;
    }
