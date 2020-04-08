/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import com.tangosol.net.PartitionedService;

import com.tangosol.util.ClassHelper;


/**
* The BroadKeyPartitioningStrategy is a strategy that could be used in cases
* when the standard key association-based strategy produces very uneven
* partition distribution - some partitions having significantly larger amount of
* data than others. Another use case is a partitioned cache with relatively
* small number of logical groups of values, when using the standard association
* strategy would result in only a small number of partitions containing any data
* and large number of partitions being completely empty.
* <br>
* While using the BroadKeyPartitioningStrategy allows to spread data much more
* evenly across partitions and still have scalable parallel queries (based on
* the {@link com.tangosol.util.filter.KeyAssociatedFilter KeyAssociatedFilter}),
* it comes with a trade-off - associated entries can no longer be assume to
* reside in the same JVM, though with a relatively high probability they will.
* <p>
* If the semantics of an application allows dynamically determining the desired
* span for different logically associated groups of entries, it is recommended to
* extend this strategy and override the {@link #getSpan getSpan()} method.
*
* @author gg 2010.05.19
* @since Coherence 3.6
*/
public class BroadKeyPartitioningStrategy
        extends DefaultKeyPartitioningStrategy
    {
    /**
    * Construct the BroadKeyPartitioningStrategy based on the specified span.
    * Span value of zero means that this strategy will behave identically to the
    * DefaultKeyPartitioningStrategy. Span value of N will place associated
    * entries into no more than (N+1) distinct partitions.
    *
    * @param nSpan  the default span value
    */
    public BroadKeyPartitioningStrategy(int nSpan)
        {
        m_nSpan = Math.max(0, nSpan);
        }


    // ----- KeyPartitioningStrategy interface ------------------------------

    /**
    * {@inheritDoc}
    */
    public void init(PartitionedService service)
        {
        super.init(service);

        int nMaxSpan = (int) Math.sqrt(service.getPartitionCount());
        if (m_nSpan > nMaxSpan)
            {
            throw new IllegalStateException(ClassHelper.getSimpleName(getClass())
                + ": the span should not exceed " + nMaxSpan);
            }
        }

    /**
    * {@inheritDoc}
    */
    public int getKeyPartition(Object oKey)
        {
        PartitionedService service = m_service;

        Object oBaseKey = service.getKeyAssociator().getAssociatedKey(oKey);
        if (oBaseKey == null)
            {
            oBaseKey = oKey;
            }

        int cParts = service.getPartitionCount();
        int nSpan  = getSpan(oBaseKey);
        int nBase  = toBinary(oBaseKey, service.getSerializer()).
            calculateNaturalPartition(cParts);

        int of = mod(oKey.hashCode(), nSpan + 1);
        return (nBase + of) % cParts;
        }

    /**
    * {@inheritDoc}
    */
    public PartitionSet getAssociatedPartitions(Object oKey)
        {
        PartitionedService service = m_service;

        Object oBaseKey = service.getKeyAssociator().getAssociatedKey(oKey);
        if (oBaseKey == null)
            {
            oBaseKey = oKey;
            }

        int cParts = service.getPartitionCount();
        int nSpan  = getSpan(oBaseKey);
        int nBase  = toBinary(oBaseKey, service.getSerializer()).
            calculateNaturalPartition(cParts);

        PartitionSet parts = new PartitionSet(cParts);
        for (int of = 0; of <= nSpan; of++)
            {
            parts.add((nBase + of) % cParts);
            }
        return parts;
        }


    // ----- subclassing support --------------------------------------------

    /**
    * Calculate the partition span for a logical group of keys represented by
    * the specified "base" key. The passed in key no longer needs to be
    * checked for the key association; in fact if there was any association, it
    * is the one that was returned by the service's {@link KeyAssociator}.
    * <p>
    * The default implementation just returns the constant span value, but
    * subclasses could implement this method based on additional application
    * level semantics or construction-time configuration to dynamically assign
    * greater span values to larger logical groups.
    *
    * @param oBaseKey  the "base" key in Object format
    *
    * @return the partition span for the given key
    */
    protected int getSpan(Object oBaseKey)
        {
        return m_nSpan;
        }


    // ----- data fields ----------------------------------------------------

    /**
    * The span value.
    */
    private int m_nSpan;
    }
