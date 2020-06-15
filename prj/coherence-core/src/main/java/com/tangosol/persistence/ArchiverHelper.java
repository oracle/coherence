/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.Base;

import java.io.File;
import java.io.FileFilter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Various helper methods for the archiver functionality.
 *
 * @since  12.2.1
 * @author tam  2014.03.18
 * @deprecated this API is intended for internal purposes only and will be removed
 *             in a future version of Coherence
 */
@Deprecated
// Internal Notes: THIS IMPLEMENTATION HAS BEEN ABANDONED AS OF 12.2.1.2
public class ArchiverHelper
    {
    /**
     * Given a {@link Map} of member Id's and the stores each member can see
     * for a given snapshot, allocate the stores evenly as so to allow the
     * archival process to run in parallel across the greatest number of
     * members. Each member may see the same stores, as they may be on
     * shared disk or on the same machine.
     *
     * @param mapStores    a {@link Map} of members and {@link Object} arrays
     *                     that need sorting and allocating
     * @param cPartitions  the number of partitions
     *
     */
    public static void allocateSnapshotPartitions(Map<Integer, Object[]> mapStores, int cPartitions)
        {
        try
            {
            if (mapStores == null || mapStores.size() == 0)
                {
                throw new IllegalArgumentException("You must supply a collected map of partitions");
                }

            int      nMemberCount = mapStores.size();
            String[] asStoresAll  = getDistinctStores(mapStores);

            if (asStoresAll.length != cPartitions)
                {
                String sMessage = "Number of unique stores is " + asStoresAll.length
                                + " and does not match partition count of " + cPartitions
                                + ". This means that some stores are missing or the snapshot is corrupted.";
                CacheFactory.log(sMessage, CacheFactory.LOG_ERR);

                CacheFactory.log("Dumping distinct store list because of error", CacheFactory.LOG_WARN);
                for (String sStore : asStoresAll)
                    {
                    CacheFactory.log(sStore,CacheFactory.LOG_WARN);
                    }

                throw new IllegalArgumentException(sMessage);
                }

            // check for the special case where all members see the same partitions,
            // e.g. shared disk
            boolean fSharedPartitions = true;

            for (Map.Entry<Integer, Object[]> entry : mapStores.entrySet())
                {
                Object[] asSorted = entry.getValue();

                if (asSorted != null)
                    {
                    Arrays.sort(asSorted);
                    }

                if (!Arrays.equals(asSorted, asStoresAll))
                    {
                    fSharedPartitions = false;
                    break;
                    }
                }

            if (fSharedPartitions)
                {
                // work out a fair share and allocate to all
                int nFairShare        = cPartitions / nMemberCount;
                int nPartitionStart   = 0;
                int nEntriesProcessed = 0;

                for (Iterator<Map.Entry<Integer, Object[]>> iter = mapStores.entrySet().iterator(); iter.hasNext();)
                    {
                    Map.Entry<Integer, Object[]> entry = iter.next();
                    nEntriesProcessed++;

                    int nLastPartition = nEntriesProcessed < nMemberCount ? (nPartitionStart + nFairShare - 1) : cPartitions - 1;
                    String[]  asStores = new String[nLastPartition - nPartitionStart + 1];

                    for (int i = nPartitionStart, j = 0; i <= nLastPartition; i++)
                        {
                        asStores[j++] = asStoresAll[i];
                        }

                    mapStores.put(entry.getKey(), asStores);
                    nPartitionStart += nFairShare;
                    }
                }
            else
                {
                // partitions are distributed amongst all members with some potential
                // duplicates. Remove the duplicates trying to keep it as balanced as possible

                // go through each store and see if the store is known to more than 1 member
                for (int iPart = 0; iPart < cPartitions; iPart++)
                    {
                    Set<Integer> setMembers = getMembersOwningPartition(mapStores, asStoresAll[iPart]);

                    // keep reducing the members that know about this partition to 1 while trying to keep balanced
                    while (setMembers.size() > 1)
                        {
                        // find the member that has the most partitions allocated and remove the partition from it
                        int nMemberMaxPartitions = -1;
                        int nMaxPartitions       = -1;

                        for (Integer nMember : setMembers)
                            {
                            int nMax = mapStores.get(nMember).length;

                            if (nMax > nMaxPartitions)
                                {
                                nMaxPartitions       = nMax;
                                nMemberMaxPartitions = nMember;
                                }
                            }

                        // the member nMemberMaxPartitions will have the partition "iPart" removed
                        Object[] aoStores = mapStores.get(nMemberMaxPartitions);

                        // remove the store from this array
                        mapStores.put(nMemberMaxPartitions, removeStoreFromArray(asStoresAll[iPart], aoStores));

                        // re-query the set again
                        setMembers = getMembersOwningPartition(mapStores, asStoresAll[iPart]);
                        }
                    }
                }
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Error in ArchiverHelper.allocateSnapshotPartitions");
            }
        }

    /**
     * Return the {@link Set} of members who are aware of a particular
     * store.
     *
     * @param mapStores  the {@link Map} of discovered partitions for members
     * @param sStore     the store we are interested in
     *
     * @return  the {@link Set} of members who are aware of a particular store
     */
    public static Set<Integer> getMembersOwningPartition(Map<Integer, Object[]> mapStores, String sStore)
        {
        Set<Integer> setMembers = new HashSet<>();

        for (Map.Entry<Integer, Object[]> entry : mapStores.entrySet())
            {
            Object[] aoValue = entry.getValue();
            if (aoValue != null)
                {
                for (int i = 0; i < aoValue.length; i++)
                    {
                    if (sStore.equals(aoValue[i]))
                        {
                        setMembers.add(entry.getKey());
                        break;
                        }
                    }
                }
            }

        return setMembers;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return a a sorted array of distinct stores
     *
     * @param mapStores the {@link Map} of discovered partitions for members
     *
     * @return a {@link String} array of distinct stores
     */
    private static String[] getDistinctStores(Map<Integer, Object[]> mapStores)
        {
        Set<String> setStores = new HashSet<>();

        for (Map.Entry<Integer, Object[]> entry : mapStores.entrySet())
            {
            Object[] aoValue = entry.getValue();

            if (aoValue != null)
                {
                for (int i = 0; i < aoValue.length; i++)
                    {
                    String sStore = (String) aoValue[i];
                    if (!setStores.contains(sStore))
                        {
                        setStores.add(sStore);
                        }
                    }
                }
            }

        String[] asResults = setStores.toArray(new String[setStores.size()]);

        Arrays.sort(asResults);

        return asResults;
        }

    /**
     * Remove the given store from the array and return a new array.
     *
     * @param sStoreToRemove the store to remove
     * @param aoArray        the array to remove from
     *
     * @return the new array with the store removed
     */
    private static Object[] removeStoreFromArray(String sStoreToRemove, Object[] aoArray)
        {
        Object[] aoNewArray = new Object[aoArray.length - 1];

        for (int i = 0, j = 0; j < aoArray.length; j++)
            {
            String sStore = (String) aoArray[j];
            if (!sStore.equals(sStoreToRemove))
                {
                aoNewArray[i++] = sStore;
                }
            }
        return aoNewArray;
        }

    // ----- inner class: DirectoryFileFilter -------------------------------

    /**
     * FileFilter implementation that only includes directories.
     */
    public static class DirectoryFileFilter
            implements FileFilter
        {
        /**
         * Accept the given file only if it is a directory.
         *
         * @param file the file
         *
         * @return true if the given file is a directory
         */
        @Override
        public boolean accept(File file)
            {
            return file.isDirectory();
            }

        /**
         * Singleton instance.
         */
        public static final FileFilter INSTANCE = new DirectoryFileFilter();
        }
    }
