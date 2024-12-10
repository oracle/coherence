/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.partition;


import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.collections.AbstractStableIterator;

import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;

import com.tangosol.util.filter.PartitionedFilter;

import java.util.Iterator;


/**
 * Abstract base class for partitioned iterators.
 *
 * @author as  2015.01.27
 */
public abstract class AbstractPartitionedIterator<T>
        extends AbstractStableIterator<T>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Create AbstractPartitionedIterator instance.
     *
     * @param cache     the cache to query
     * @param filter    the query expressed as a Filter
     * @param setPids   the partitions to execute the query against
     */
    protected AbstractPartitionedIterator(Filter filter, NamedCache cache, PartitionSet setPids)
        {
        this(filter, cache, setPids, false, false);
        }

    /**
     * Create AbstractPartitionedIterator instance.
     *
     * @param cache      the cache to query
     * @param filter     the query expressed as a Filter
     * @param setPids    the partitions to execute the query against
     * @param fByMember  indicates whether it is by-member iterations
     * @param fRandom    indicates whether the order of iteration is randomized
     */
    protected AbstractPartitionedIterator(Filter filter, NamedCache cache, PartitionSet setPids, boolean fByMember, boolean fRandom)
        {
        m_filter    = new PartitionedFilter(filter, new PartitionSet(setPids.getPartitionCount()));
        m_cache     = cache;
        m_setPids   = new PartitionSet(setPids);
        m_fByMember = fByMember;
        m_fRandom   = fRandom;
        }

    // ----- abstract methods -----------------------------------------------

    /**
     * Obtain the next Iterable for a given filter. This method is called
     * when the Iterable returned by the previous call is exhausted.
     *
     * @param filter  filter to use
     *
     * @return  a next Iterable or null if there is no more data to iterate
     */
    protected abstract Iterable<T> nextIterable(PartitionedFilter filter);

    // ----- AbstractStableIterator methods ---------------------------------

    @Override
    protected void advance()
        {
        Iterator<T> iterKeys = m_iter;
        while (iterKeys == null || !iterKeys.hasNext())
            {
            PartitionedFilter filter = m_filter;
            if (!advancePartitionSet(filter.getPartitionSet()))
                {
                // all done
                m_iter = null;
                return;
                }

            Iterable<T> iterable = nextIterable(filter);
            m_iter = iterKeys = iterable.iterator();
            }

        setNext(iterKeys.next());
        }

    @Override
    protected void remove(Object oPrev)
        {
        throw new UnsupportedOperationException();
        }

    // ----- internal methods -----------------------------------------------

    /**
    * Select the next set of partitions to query. This method removes the
    * previously queried partition(s) from the passed partition set and adds
    * the next partition(s) to query to the partition set.
    *
    * @param setPids  the PartitionSet that is being used to query, which (if
    *                 this is not the first call) will have the previously
    *                 queried partitions marked in it
    *
    * @return true if there are more partitions to query
    */
    private boolean advancePartitionSet(PartitionSet setPids)
        {
        PartitionSet setRemain = m_setPids;
        int iPrevPid = m_iPrevPid;
        int iNextPid = m_fRandom ? setRemain.rnd() : setRemain.next(iPrevPid+1);
        if (iNextPid < 0)
            {
            return false;
            }

        if (m_fByMember)
            {
            // copy all remaining partitions into the next set of partitions
            // to query
            setPids.clear();
            setPids.add(setRemain);

            // find the member that owns that partition
            PartitionedService service = (PartitionedService) m_cache.getCacheService();
            Member member  = service.getPartitionOwner(iNextPid);
            while (member == null)
                {
                // we are in a partition redistribution scenario; back off to
                // allow the system to settle
                try
                    {
                    Blocking.sleep(5);
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    throw Base.ensureRuntimeException(e);
                    }

                // get the next available member that has data to iterate; there
                // is no reason to optimize at this point
                for (int iPid = setRemain.next(0); iPid >= 0;
                         iPid = setRemain.next(iPid+1))
                    {
                    member = service.getPartitionOwner(iPid);
                    if (member != null)
                        {
                        // make sure that the next time that this method is
                        // called, that it starts at the same point as it did
                        // this time around (since we had to skip out of order)
                        iNextPid = iPrevPid;
                        break;
                        }
                    }
                }

            // find all the other partitions owned by that member
            PartitionSet setColocated = service.getOwnedPartitions(member);

            // keep only the partitions owned by the same member
            setPids.retain(setColocated);

            // remove the selected PIDs from the remaining set
            setRemain.remove(setPids);
            }
        else
            {
            // clear out whatever PID was queried in the last iteration
            if (iPrevPid >= 0)
                {
                setPids.remove(iPrevPid);
                }

            // specify the PID to query in this iteration
            setPids.add(iNextPid);

            // remove the PID that is being queried from the remaining set
            setRemain.remove(iNextPid);
            }

        m_iPrevPid = iNextPid;
        return true;
        }

    // ----- constants ------------------------------------------------------

    /**
    * An option to iterate one partition at a time. This is the default.
    */
    public static final int OPT_BY_PARTITION    = 0;

    /**
    * An option to iterate one member at a time.
    */
    public static final int OPT_BY_MEMBER       = 2;

    /**
    * An option to iterate the members or partitions in a randomized order.
    * Note that this does not refer to the order that the individual keys or
    * entries themselves will be iterated.
    */
    public static final int OPT_RANDOMIZED      = 4;

    // ----- data members ---------------------------------------------------

    /**
    * The cache to query from.
    */
    protected NamedCache m_cache;

    /**
    * The filter to query with.
    */
    protected PartitionedFilter m_filter;

    /**
    * The remaining set of partitions to iterate.
    */
    protected PartitionSet m_setPids;

    /**
    * Differentiates between by-member and by-partition iteration. If true,
    * all partitions of a given member are iterated in a single chunk; if
    * false, each partition is iterated individually.
    */
    protected boolean m_fByMember;

    /**
    * An option to randomize the order of iteration to avoid harmonics in a
    * clustered environment.
    */
    protected boolean m_fRandom;

    /**
    * Index of the previous partition iterated, or -1 if no partitions have
    * been iterated. If multiple partitions are iterated at a time, then
    * this is the partition that was selected initially during the last
    * partition advance.
    */
    private int m_iPrevPid = -1;

    /**
    * Current iterator.
    */
    private Iterator m_iter;
    }
