/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Map;

import javax.json.bind.annotation.JsonbProperty;


/**
* Filter which limits the scope of another filter to those entries that have
* keys that belong to the specified partition set.
* <p>
* This filter is intended to be used in advanced scenarios, when a caller wants
* to retrieve the results of parallel processing restricted to a subset of
* partitions. This approach may somewhat complicate the client code, but can
* dramatically reduce the memory footprint used by the requestor.
* <p>
* Below are two examples of PartitionedFilter usage:
* <ul>
*  <li> Run a parallel query partition-by-partition:
*   <pre>
* void executeByPartitions(NamedCache cache, Filter filter)
*     {
*     DistributedCacheService service =
*         (DistributedCacheService) cache.getCacheService();
*     int cPartitions = service.getPartitionCount();
*
*     PartitionSet parts = new PartitionSet(cPartitions);
*     for (int iPartition = 0; iPartition &lt; cPartitions; iPartition++)
*         {
*         parts.add(iPartition);
*
*         Filter filterPart = new PartitionedFilter(filter, parts);
*
*         Set setEntriesPart = cache.entrySet(filterPart);
*
*         // process the entries ...
*
*         parts.remove(iPartition);
*         }
*     }</pre></li>
*
*  <li> Run a parallel query member-by-member:
*   <pre>
* void executeByMembers(NamedCache cache, Filter f)
*     {
*     DistributedCacheService service =
*         (DistributedCacheService) cache.getCacheService();
*     int cPartitions = service.getPartitionCount();
*
*     PartitionSet partsProcessed = new PartitionSet(cPartitions);
*     for (Iterator iter = service.getStorageEnabledMembers().iterator();
*             iter.hasNext();)
*         {
*         Member member = (Member) iter.next();
*
*         PartitionSet partsMember = service.getOwnedPartitions(member);
*
*         // due to a redistribution some partitions may have already been processed
*         partsMember.remove(partsProcessed);
*
*         Filter filterPart = new PartitionedFilter(filter, partsMember);
*         Set setEntriesPart = cache.entrySet(filterPart);
*
*         // process the entries ...
*
*         partsProcessed.add(partsMember);
*         }
*
*     // due to a possible redistribution, some partitions may have been skipped
*     if (!partsProcessed.isFull())
*         {
*         partsProcessed.invert();
*         Filter filter = new PartitionedFilter(filter, partsProcessed);
*
*         // process the remaining entries ...
*         }
*     }</pre></li>
* </ul>
*
* <b>Note:</b> This filter must be the outermost filter and cannot be used
* as a part of any composite filter (AndFilter, OrFilter, etc.)
* <p>
* To iterate through a query on a partition-by-partition or member-by-member
* basis, use the {@link com.tangosol.net.partition.PartitionedIterator
* PartitionedIterator} class.
*
* @author gg 2008.02.06
* @since Coherence 3.4
*/
public class PartitionedFilter<T>
        extends    ExternalizableHelper
        implements Filter<T>, EntryFilter<Object, T>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public PartitionedFilter()
        {
        }

    /**
    * Construct a PartitionedFilter.
    *
    * @param filter     the underlying (wrapped) filter
    * @param partitions the subset of partitions the filter should run against
    */
    public PartitionedFilter(Filter<T> filter, PartitionSet partitions)
        {
        if (filter == null || filter instanceof PartitionedFilter)
            {
            throw new IllegalArgumentException("Invalid filter: " + filter);
            }

        if (partitions == null)
            {
            throw new IllegalArgumentException("PartitionSet be specified");
            }

        m_filter     = filter;
        m_partitions = partitions;
        }


    // ----- EntryFilter interface ------------------------------------------

   /**
   * {@inheritDoc}
   */
   public boolean evaluate(T o)
        {
        throw new UnsupportedOperationException();
        }

    /**
    * {@inheritDoc}
    */
    public boolean evaluateEntry(Map.Entry entry)
        {
        PartitionSet parts = m_partitions;
        Object       oKey  = entry.getKey();
        int          nHash = oKey == null ? 0 : oKey.hashCode();

        return parts.contains(mod(nHash, parts.getPartitionCount())) &&
               InvocableMapHelper.evaluateEntry(m_filter, entry);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the wrapped Filter.
    *
    * @return the wrapped filter object
    */
    public Filter<T> getFilter()
        {
        return m_filter;
        }

    /**
    * Obtain the PartitionSet that specifies what partitions the wrapped filter
    * will be applied to.
    *
    * @return the partition set
    */
    public PartitionSet getPartitionSet()
        {
        return m_partitions;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the PartitionedFilter with another object to determine equality.
    * Two PartitionedFilter objects are considered equal iff the
    * wrapped filters and partition sets are equal.
    *
    * @return true iff this PartitionedFilter and the passed object are
    *         equivalent PartitionedFilter objects
    */
    public boolean equals(Object o)
        {
        if (o instanceof PartitionedFilter)
            {
            PartitionedFilter that = (PartitionedFilter) o;
            return equals(this.m_filter,   that.m_filter)
                && equals(this.m_partitions, that.m_partitions);
            }

        return false;
        }

    /**
    * Determine a hash value for the PartitionedFilter object according to
    * the general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this PartitionedFilter object
    */
    public int hashCode()
        {
        return hashCode(m_filter) + hashCode(m_partitions);
        }

    /**
    * Return a human-readable description for this Filter.
    *
    * @return a String description of the Filter
    */
    public String toString()
        {
        String sClass = getClass().getName();
        return sClass.substring(sClass.lastIndexOf('.') + 1) +
            '(' + m_filter + ", " + m_partitions + ')';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_filter     = readObject(in);
        m_partitions = readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        writeObject(out, m_filter);
        writeObject(out, m_partitions);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_filter     = in.readObject(0);
        m_partitions = in.readObject(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_filter);
        out.writeObject(1, m_partitions);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying filter.
    */
    @JsonbProperty("filter")
    private Filter<T> m_filter;

    /**
    * The associated PartitionSet.
    */
    @JsonbProperty("partitions")
    private PartitionSet m_partitions;
    }
