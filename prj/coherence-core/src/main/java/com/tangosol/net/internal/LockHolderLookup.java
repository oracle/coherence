/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.internal;


import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.AbstractPriorityTask;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.InvocableMap;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
* Aggregator that returns the id of the node that is holding a lock
* on a key.  This is used by Coherence*Web (AbstractHttpSessionCollection)
* for logging when a node is not able to obtain a lock for a session.
* <p>
* <b>Note:</b> In the current implementation, this can only be called
* using a keyset containing a single key.
*
* @author pp 2008-11-05
*/
public class LockHolderLookup
        extends AbstractPriorityTask
        implements InvocableMap.ParallelAwareAggregator
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a LockHolderLookup aggregator
    */
    public LockHolderLookup()
        {
        setRequestTimeoutMillis(REQUEST_TIMEOUT_MILLIS);
        }


    // ----- InvocableMap.ParallelAwareAggregator interface -----------------

    /**
    * {@inheritDoc}
    */
    public Object aggregate(Set setEntries)
        {
        Integer            IHolderId = null;
        InvocableMap.Entry entry;

        Iterator iter = setEntries.iterator();
        if (iter.hasNext())
            {
            entry = (InvocableMap.Entry) iter.next();
            }
        else
            {
            return null;
            }

        try
            {
            Object storage  = ClassHelper.invoke(entry, "getStorage", null);
            Map    mapLease = (Map) ClassHelper.invoke(storage, "getLeaseMap", null);
            Object binKey   = ClassHelper.invoke(entry, "getBinaryKey", null);
            Object lease    = mapLease.get(binKey);

            IHolderId = lease == null ? null :
                    (Integer) ClassHelper.invoke(lease, "getHolderId", null);
            }
        catch (Exception e)
            {
            Logger.config("Could not get lease information for " + entry.getKey() + ": " + e);
            }
        return IHolderId;
        }

    public Object aggregate(Stream stream)
        {
        return aggregate((Set) stream.collect(Collectors.toSet()));
        }

    /**
    * {@inheritDoc}
    */
    public InvocableMap.EntryAggregator getParallelAggregator()
        {
        return this;
        }

    /**
    * {@inheritDoc}
    */
    public Object aggregateResults(Collection collResults)
        {
        return collResults.isEmpty() ? null : collResults.iterator().next();
        }


    // ----- constants ------------------------------------------------------

    /**
    * Request timeout for this aggregator
    */
    //todo: sys prop
    private static final long REQUEST_TIMEOUT_MILLIS = 1000;
    }
