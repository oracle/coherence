/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.oracle.coherence.common.util.MemorySize;

import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Filter;

import java.util.ArrayList;
import java.util.List;

/**
 * QueryResult is a structure that holds the state of the query execution
 * (see PartitionedCache.Storage#query)
 *
 * @author gg 2015.04.13
 */
public class QueryResult
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@link QueryResult} instance.
     *
     * @param partitionSet     the partition set this result is for
     * @param aoResult         the array of results (keys or entries) that match the query
     */
    public QueryResult(PartitionSet partitionSet, Object[] aoResult)
        {
        this(partitionSet, aoResult, aoResult == null ? 0 : aoResult.length, null);
        }

    /**
     * Construct {@link QueryResult} instance.
     *
     * @param partitionSet     the partition set this result is for
     * @param aoResult         the array of results (keys or entries) that match the query
     * @param cResults         the number of results (keys or entries) that match the query
     */
    public QueryResult(PartitionSet partitionSet, Object[] aoResult, int cResults)
        {
        this(partitionSet, aoResult, cResults, null);
        }

    /**
     * Construct {@link QueryResult} instance.
     *
     * @param partitionSet     the partition set this result is for
     * @param aoResult         the array of results (keys or entries) that match the query
     * @param cResults         the number of results (keys or entries) that match the query
     * @param filterRemaining  the filter that still needs to be evaluated
     */
    public QueryResult(PartitionSet partitionSet, Object[] aoResult, int cResults, Filter<?> filterRemaining)
        {
        m_partitionSet    = partitionSet;
        m_aoResult        = aoResult;
        m_cResults        = cResults;
        m_filterRemaining = filterRemaining;
        m_fOptimized      = filterRemaining == null;
        }

    /**
     * Construct {@link QueryResult} instance from partial results.
     *
     * @param aPartResults  partial, typically partition-level query results that should be merged
     */
    public QueryResult(QueryResult[] aPartResults)
        {
        PartitionSet parts      = null;
        int          cResults   = 0;
        boolean      fOptimized = true;
        for (QueryResult result : aPartResults)
            {
            cResults += result.getCount();
            if (parts == null)
                {
                parts = new PartitionSet(result.getPartitionSet());
                }
            else
                {
                parts.add(result.getPartitionSet());
                }
            fOptimized = fOptimized && result.m_fOptimized;
            }

        m_partitionSet    = parts;
        m_aPartResult     = aPartResults;
        m_cResults        = cResults;
        m_fOptimized      = fOptimized;
        m_filterRemaining = null;
        }

    // ---- accessors -------------------------------------------------------

    /**
     * Return the partition set this result is for.
     *
     * @return the partition set this result is for
     */
    public PartitionSet getPartitionSet()
        {
        return m_partitionSet;
        }

    /**
     * Return the array of results (keys or entries) that match the query.
     *
     * @return the array of results (keys or entries) that match the query
     */
    public Object[] getResults()
        {
        if (m_aoResult == null && m_aPartResult != null)
            {
            // this query result was created from partial results,
            // so we need to merge them into a unified result array
            m_aoResult = mergePartialResults(m_aPartResult, m_cResults);
            }

        return m_aoResult;
        }

    /**
     * Set the array of results (keys or entries) that match the query.
     *
     * @param aoResult  the array of results (keys or entries) that match the query
     */
    public void setResults(Object[] aoResult)
        {
        setResults(aoResult, aoResult.length);
        }

    /**
     * Set the array of results (keys or entries) that match the query.
     *
     * @param aoResult  the array of results (keys or entries) that match the query
     * @param cResults  the number of elements in the array that represent valid results
     */
    public void setResults(Object[] aoResult, int cResults)
        {
        m_aoResult = aoResult;
        m_cResults = cResults;
        m_cbSize   = -1; // reset
        }

    /**
     * The number of results (keys or entries) that match the query.
     *
     * @return the number of results (keys or entries) that match the query
     */
    public int getCount()
        {
        return m_cResults;
        }

    /**
     * Return the filter that still needs to be evaluated.
     *
     * @return the filter that still needs to be evaluated.
     */
    public Filter<?> getFilterRemaining()
        {
        return m_filterRemaining;
        }

    /**
     * Return {@code true} if this query result was fully resolved using indexes.
     *
     * @return {@code true} if this query result was fully resolved using indexes
     */
    public boolean isOptimized()
        {
        return m_fOptimized;
        }

    /**
     * Return the count of results that were individually evaluated.
     *
     * @return the count of results that were individually evaluated
     */
    public int getScannedCount()
        {
        int cScanned = 0;
        if (m_aPartResult != null)
            {
            for (QueryResult r : m_aPartResult)
                {
                Object[] aoResult = r.m_aoResult;
                if (aoResult != null)
                    {
                    cScanned += aoResult.length;
                    }
                }
            }
        else if (m_aoResult != null)
            {
            cScanned = m_aoResult.length;
            }

        return cScanned;
        }

    /**
     * Return the approximate size of the results in bytes.
     *
     * @return the approximate size of the results in bytes
     */
    public long getSize()
        {
        long cbSize = m_cbSize;

        if (cbSize == -1L)  // not set
            {
            int cResults = m_cResults;

            for (int i = 0; i < cResults; i++)
                {
                Object o = m_aoResult[i];
                if (o instanceof BinaryEntry)
                    {
                    BinaryEntry<?, ?> binEntry = (BinaryEntry<?, ?>) o;
                    cbSize += (binEntry.getBinaryKey().length() + binEntry.getBinaryValue().length());
                    }
                else if (o instanceof Binary)
                    {
                    Binary binKey = (Binary) o;
                    cbSize += binKey.length();
                    }
                else
                    {
                    // ignore EntryStatus instances; not relevant for size calculation
                    }
                }

            m_cbSize = cbSize;
            }

        return cbSize;
        }

    /**
     * Split this query result into one or more results required to represent
     * the same result set, with each result not exceeding the specified maximum size.
     *
     * @param maxSize  the maximum size of each individual result
     *
     * @return an array of one or more partial results with a specified maximum size, or
     *         the result object itself if splitting cannot be done (no results or
     *         they were not collated from partial results)
     */
    public QueryResult[] split(MemorySize maxSize)
        {
        if (m_aPartResult == null || m_aPartResult.length == 0)
            {
            return new QueryResult[] { this };
            }

        List<QueryResult> lstResults = new ArrayList<>();

        PartitionSet parts = null;

        int  cResults  = 0;
        long cbSize    = 0;
        long cbMaxSize = maxSize.getByteCount();

        List<QueryResult> lstBatch = new ArrayList<>();

        for (QueryResult result : m_aPartResult)
            {
            if (result.getSize() > cbMaxSize)
                {
                // just add it directly, no need to batch it
                lstResults.add(result);
                }
            else
                {
                // add to the batch and increment acumulated count and size
                lstBatch.add(result);
                cResults += result.getCount();
                cbSize   += result.getSize();
                if (parts == null)
                    {
                    parts = new PartitionSet(result.getPartitionSet());
                    }
                else
                    {
                    parts.add(result.getPartitionSet());
                    }

                if (cbSize >= cbMaxSize)
                    {
                    // merge the results in the batch and add to the list
                    Object[] aoResult = mergePartialResults(lstBatch.toArray(QueryResult[]::new), cResults);
                    lstResults.add(new QueryResult(parts, aoResult));

                    // reset state for the next batch
                    parts    = null;
                    cResults = 0;
                    cbSize   = 0L;
                    lstBatch.clear();
                    }
                }
            }

        if (!lstBatch.isEmpty())
            {
            // merge the results in the final batch and add to the list
            Object[] aoResult = mergePartialResults(lstBatch.toArray(QueryResult[]::new), cResults);
            lstResults.add(new QueryResult(parts, aoResult));
            }

        return lstResults.toArray(QueryResult[]::new);
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Merges partial results into a single result array.
     *
     * @return merged result array
     */
    private static Object[] mergePartialResults(QueryResult[] aPartResult, int cResults)
        {
        Object[] aoResult = new Object[cResults];
        if (cResults > 0)
            {
            int nPos = 0;
            for (QueryResult resultPart : aPartResult)
                {
                int cPartResults = resultPart.getCount();
                if (cPartResults > 0)
                    {
                    System.arraycopy(resultPart.getResults(), 0, aoResult, nPos, cPartResults);
                    nPos += cPartResults;
                    }
                }
            }
        return aoResult;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The partition set this result is for.
     */
    private final PartitionSet m_partitionSet;

    /**
     * The array of partial results (typically one per partition) that
     * this QueryResult was constructed from.
     */
    private QueryResult[] m_aPartResult;

    /**
     * The array of results (keys or entries) that match the query.
     */
    private Object[] m_aoResult;

    /**
     * The number of results (keys or entries) that match the query.
     */
    private int m_cResults;

    /**
     * The approximate size of the results (keys or entries) in bytes.
     */
    private long m_cbSize = -1L;

    /**
     * The filter that still needs to be evaluated.
     */
    private final Filter<?> m_filterRemaining;

    /**
     * True if this query result was fully resolved using indexes.
     */
    private final boolean m_fOptimized;
    }
