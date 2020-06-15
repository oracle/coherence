/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

import com.tangosol.net.InetAddressHelper;

import com.tangosol.util.Filter;
import com.tangosol.util.SafeLinkedList;

import java.net.InetAddress;

import java.util.Iterator;
import java.util.List;

/**
 * InetAddressRangeFilter evaluates if an IP address is within a specific range.
 *
 * @author pfm  2011.05.10
 * @since Coherence 3.7.1
 */
public class InetAddressRangeFilter implements Filter
    {
    /**
     * Add an InetAddress range to the filter.
     *
     * @param addrFrom  the lower bound of the range (inclusive)
     * @param addrTo    the upper bound of the range (inclusive)
     */
    public void addRange(InetAddress addrFrom, InetAddress addrTo)
        {
        long lFrom = InetAddressHelper.toLong(addrFrom);
        long lTo   = InetAddressHelper.toLong(addrTo);
        getAuthorizedHostList().add(new long[] {lFrom, lTo});
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluate(Object obj)
        {
        InetAddress addrTarget = (InetAddress)obj;
        long lAddr = InetAddressHelper.toLong(addrTarget);

        List<long[]> listHost = getAuthorizedHostList();
        if (listHost != null)
            {
            for (Iterator<long[]> iter = listHost.iterator(); iter.hasNext();)
                {
                long[] alAddr = iter.next();

                if (alAddr[0] <= lAddr && lAddr <= alAddr[1])
                    {
                    return true;
                    }
                }
            }
        return false;
        }

    // ----- internal methods -----------------------------------------------

    /**
     * Return the list of authorized hosts ranges where each range is an array of 2 longs.
     * The [0] is the "from" address and [1] is the "to" address.
     *
     * @return the authorized host list.
     */
    @SuppressWarnings("unchecked")
    protected List<long[]> getAuthorizedHostList()
        {
        List<long[]>  listAuthorizedHost = m_listAuthorizedHost;
        if (listAuthorizedHost == null)
            {
            m_listAuthorizedHost = listAuthorizedHost =  new SafeLinkedList();
            }
        return listAuthorizedHost;
        }

    // ----- data members and constants -------------------------------------

    /**
     * The list of authorized host ranges.
     */
    List<long[]> m_listAuthorizedHost;
    }
