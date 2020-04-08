/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.net.InetAddressRangeFilter;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * InetAddressRangeFilterBuilder defers evaluating configuration parameters
 * until Filter is instantiated.
 *
 * @author jf  2015.02.10
 * @since Coherence 12.2.1
 */
public class InetAddressRangeFilterBuilder
        implements ParameterizedBuilder<Filter>
    {
    // ----- Constructors ---------------------------------------------------

    /**
     * Construct a {@link InetAddressRangeFilterBuilder}
     */
    public InetAddressRangeFilterBuilder()
        {
        m_filter            = new InetAddressRangeFilter();
        m_fFilterAdded      = false;
        m_exceptionDeferred = null;
        }

    // ----- ParameterizedBuilder methods -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Filter realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        if (m_exceptionDeferred != null)
            {
            throw m_exceptionDeferred;
            }

        return m_fFilterAdded ? m_filter : null;
        }

    // ----- InetAddressRangeFilterBuilder methods --------------------------

    /**
     * Add an authorized host range to the filter.
     *
     * @param sAddrFrom from address
     * @param sAddrTo   to address
     */
    public void addAuthorizedHostsToFilter(String sAddrFrom, String sAddrTo)
        {
        if (sAddrFrom == null || sAddrFrom.length() == 0)
            {
            if (sAddrTo != null && sAddrTo.length() != 0)
                {
                m_exceptionDeferred =
                    new IllegalStateException("Error adding host filter.  Both <from-ip> and <to-ip> elements must be specified");
                }

            return;
            }

        InetAddress addrFrom;
        InetAddress addrTo;

        try
            {
            addrFrom = InetAddress.getByName(sAddrFrom);
            addrTo   = sAddrTo == null ? addrFrom : InetAddress.getByName(sAddrTo);
            }
        catch (UnknownHostException e)
            {
            Base.trace("Unresolvable authorized host will be ignored: " + e);

            return;
            }

        m_filter.addRange(addrFrom, addrTo);
        m_fFilterAdded = true;

        return;
        }

    // ----- data members ---------------------------------------------------

    /**
     * {@link Filter} being built by this builder.
     */
    private InetAddressRangeFilter m_filter;

    /**
     * Have any address ranges been successfully added to filter.
     */
    private boolean                m_fFilterAdded;

    /**
     * Deferred configuration exception thrown only when realized.
     */
    private IllegalStateException  m_exceptionDeferred;
    }
