/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import com.oracle.coherence.common.base.Disposable;
import com.oracle.coherence.common.collections.AbstractStableIterator;

import com.tangosol.io.nio.BinaryMap;

import com.tangosol.net.BackingMapManager;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.cache.CacheStatistics;

import com.tangosol.util.AbstractKeyBasedMap;
import com.tangosol.util.ChainedEnumerator;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.LiteMap;
import com.tangosol.util.LongArray;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SparseArray;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
* In a partitioned configuration, the PartitionSplittingBackingMap is a
* "map multi-plexer" that enables multiple backing maps to be used in place
* of a single backing map. The data and operations related to each partition
* are routed by the PartitionSplittingBackingMap to a partition-specific
* backing map. The two primary benefits are:
* <ol><li>Less data are stored in each backing map, potentially enabling much
* more total data to be managed, and</li>
* <li>each partition is managed by a separate data structure, potentially
* increasing concurrency by spreading concurrent operations over multiple
* data structures.</li></ol>
* <p>
* Also, as with all usage of BinaryMap, if the data are stored off-heap, the
* proper use of the MaxDirectMemorySize JVM switch will be crucial.
*
* @since Coherence 3.5
* @author cp  2008-11-20
*/
public class PartitionSplittingBackingMap
        extends AbstractKeyBasedMap
        implements Disposable, PartitionAwareBackingMap
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a PartitionSplittingBackingMap that will delegate each
    * partition's data and operations to a separate backing map.
    *
    * @param bmm    a BackingMapManager that knows how to create and release
    *               the backing maps that this PartitionSplittingBackingMap is
    *               responsible for
    * @param sName  the cache name for which this backing map exists
    */
    public PartitionSplittingBackingMap(BackingMapManager bmm, String sName)
        {
        m_bmm      = bmm;
        m_ctx      = bmm.getContext();
        m_sName    = sName;
        m_maparray = new MapArray(
            ((PartitionedService) m_ctx.getCacheService()).getPartitionCount());
        }


    // ----- Map interface --------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void clear()
        {
        Map[] amap = getMapArray().getBackingMaps();
        for (int i = 0, c = amap.length; i < c; ++i)
            {
            amap[i].clear();
            }
        }

    /**
    * {@inheritDoc}
    */
    public boolean containsKey(Object oKey)
        {
        Map map = getBackingMap(oKey);
        return map != null && map.containsKey(oKey);
        }

    /**
    * {@inheritDoc}
    */
    public Object get(Object oKey)
        {
        Map map = getBackingMap(oKey);
        return map == null ? null : map.get(oKey);
        }

    /**
    * {@inheritDoc}
    */
    public boolean isEmpty()
        {
        return isEmpty(getMapArray().getBackingMaps());
        }

    /**
    * {@inheritDoc}
    */
    public Object put(Object oKey, Object oValue)
        {
        Map map = getBackingMap(oKey);
        if (map == null)
            {
            reportMissingPartition(oKey, -1);
            return null;
            }
        else
            {
            return putInternal(map, oKey, oValue);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void putAll(Map map)
        {
        if (map.isEmpty())
            {
            return;
            }

        BackingMapManagerContext ctx = getContext();

        // optimization for single item (very common due to "put blind"
        // optimization) or single partition (which is a pass-through to
        // the backing map for that partition)
        {
        Iterator iter  = map.keySet().iterator();
        int      nPid  = ctx.getKeyPartition(iter.next());
        boolean  fSame = true;
        while (iter.hasNext())
            {
            int nPidNext = ctx.getKeyPartition(iter.next());
            if (nPidNext != nPid)
                {
                fSame = false;
                break;
                }
            }

        if (fSame)
            {
            Map mapPart = getPartitionMap(nPid);
            if (mapPart == null)
                {
                reportMissingPartition(null, nPid);
                }
            else
                {
                putAllInternal(mapPart, map);
                }
            return;
            }
        }

        // sort all of the puts by partition
        LongArray array = new SparseArray();
        for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
            {
            Map.Entry entry  = (Map.Entry) iter.next();
            Object    oKey   = entry.getKey();
            int       nPid   = ctx.getKeyPartition(oKey);
            Map       mapPid = (Map) array.get(nPid);
            if (mapPid == null)
                {
                mapPid = new LiteMap();
                array.set(nPid, mapPid);
                }

            mapPid.put(oKey, entry.getValue());
            }

        // perform the puts against the appropriate backing maps
        for (LongArray.Iterator iter = array.iterator(); iter.hasNext(); )
            {
            Map mapPid = (Map) iter.next();
            int nPid   = (int) iter.getIndex();

            Map mapPart = getPartitionMap(nPid);
            if (mapPart == null)
                {
                reportMissingPartition(null, nPid);
                }
            else
                {
                putAllInternal(mapPart, mapPid);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public Object remove(Object oKey)
        {
        Map mapPart = getBackingMap(oKey);
        if (mapPart == null)
            {
            reportMissingPartition(oKey, -1);
            return null;
            }
        else
            {
            return mapPart.remove(oKey);
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected boolean removeBlind(Object oKey)
        {
        Map mapPart = getBackingMap(oKey);
        return mapPart != null && mapPart.keySet().remove(oKey);
        }

    /**
    * {@inheritDoc}
    */
    public int size()
        {
        return size(getMapArray().getBackingMaps());
        }

    /**
    * {@inheritDoc}
    */
    protected Iterator iterateKeys()
        {
        return iterateKeys(getMapArray().getBackingMaps());
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected Set instantiateKeySet()
        {
        return new KeySet();
        }


    // ----- statistics -----------------------------------------------------

    /**
    * Returns the CacheStatistics for this cache.
    *
    * @return a CacheStatistics object
    */
    public CacheStatistics getCacheStatistics()
        {
        AggregatingCacheStatistics stats = m_stats;
        if (stats == null)
            {
            m_stats = stats = new AggregatingCacheStatistics();
            }
        return stats;
        }


    // ----- PartitionAwareBackingMap methods -------------------------------

    /**
    * {@inheritDoc}
    */
    public BackingMapManager getBackingMapManager()
        {
        return m_bmm;
        }

    /**
    * {@inheritDoc}
    */
    public String getName()
        {
        return m_sName;
        }

    /**
    * {@inheritDoc}
    */
    public void createPartition(int nPid)
        {
        if (getPartitionMap(nPid) != null)
            {
            String sMsg = "Partition " + nPid + " already exists at " + toString(false);
            if (isStrict())
                {
                throw new IllegalStateException(sMsg);
                }
            else
                {
                err(format(sMsg, 0));
                return;
                }
            }

        // use the factory to create a new backing map
        Map map = getBackingMapManager().
            instantiateBackingMap(makeName(getName(), nPid));

        // register the partition
        m_maparray = m_maparray.addMap(nPid, map);
        }

    /**
    * {@inheritDoc}
    */
    public void destroyPartition(int nPid)
        {
        Map map = getPartitionMap(nPid);
        if (map == null)
            {
            reportMissingPartition(null, nPid);
            return;
            }

        m_maparray = m_maparray.removeMap(nPid);
        getBackingMapManager().releaseBackingMap(makeName(getName(), nPid), map);
        }

    /**
    * {@inheritDoc}
    */
    public Map getPartitionMap(int nPid)
        {
        return m_maparray.getBackingMap(nPid);
        }

    /**
    * {@inheritDoc}
    */
    public Map getPartitionMap(PartitionSet partitions)
        {
        return new MaskedPartitionMap(partitions);
        }


    // ----- Disposable interface -------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public void dispose()
        {
        for (Map mapPart : m_maparray.getBackingMaps())
            {
            if (mapPart instanceof Disposable)
                {
                ((Disposable) mapPart).dispose();
                }
            }
        m_maparray = new MapArray(
                    ((PartitionedService) m_ctx.getCacheService()).getPartitionCount());
        }


    // ----- internal -------------------------------------------------------

    /**
    * Determine if any access to data in missing partitions is being treated
    * as an error.
    *
    * @return true if access to missing partitions is being treated as error
    */
    public boolean isStrict()
        {
        return m_fStrict;
        }

    /**
    * Specify whether any access to data in missing partitions should be treated
    * as an error.  If set to true, any read operation against a missing
    * partition will log an error and any write operation will generate an
    * exception. Otherwise, read operations against missing partitions will just
    * return natural default values and write operations will log warnings.
    *
    * @param fStrict  if true, any access to missing partitions should being
    *                 treated as error
    */
    public void setStrict(boolean fStrict)
        {
        m_fStrict = fStrict;
        }


    // ----- sub-classing support -------------------------------------------

    /**
    * Put the key and value into a map representing a given partition.
    *
    * @param mapPart  a partition map
    * @param oKey     oKey with which the specified value is to be associated
    * @param oValue   oValue to be associated with the specified oKey
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for the key
    */
    protected Object putInternal(Map mapPart, Object oKey, Object oValue)
        {
        return mapPart.put(oKey, oValue);
        }

    /**
    * Put all entries in mapUpdate into the provided map.
    *
    * @param mapPart    a partition map
    * @param mapUpdate  the Map containing the key/value pairings to put into
    *                   mapPart
    */
    protected void putAllInternal(Map mapPart, Map mapUpdate)
        {
        mapPart.putAll(mapUpdate);
        }

    /**
    * Obtain the BackingMapManagerContext that provides the partition
    * information for keys that are stored in the backing map.
    *
    * @return the BackingMapManagerContext for the underlying service
    */
    protected BackingMapManagerContext getContext()
        {
        return m_ctx;
        }

    /**
    * Return the number of key-value mappings in a subset of the maps that
    * belong to specified partitions.
    *
    * @param amap  the array of maps to process
    *
    * @return the number of entries in a subset of the maps
    */
    protected int size(Map[] amap)
        {
        int cItems = 0;
        for (int i = 0, c = amap.length; i < c; ++i)
            {
            cItems += amap[i].size();
            }
        return cItems;
        }

    /**
    * Return true iff a subset of the maps that belong to specified partitions
    * contains no entries.
    *
    * @param amap  the array of maps to process
    *
    * @return true iff the subset of the maps contains no entries
    */
    protected boolean isEmpty(Map[] amap)
        {
        for (int i = 0, c = amap.length; i < c; ++i)
            {
            if (!amap[i].isEmpty())
                {
                return false;
                }
            }
        return true;
        }

    /**
    * Create an iterator over the keys in maps that belong to partitions
    * contained in the specified PartitionSet.
    *
    * @param amap  the array of maps to process
    *
    * @return the key iterator
    */
    protected Iterator iterateKeys(Map[] amap)
        {
        int c = amap.length;
        if (c == 0)
            {
            return NullImplementation.getIterator();
            }

        if (amap[0] instanceof BinaryMap)
            {
            // BinaryMap must be iterated differently because its iterator
            // is not thread-safe
            return new PartitionedIterator(amap);
            }

        Iterator[] aiter = new Iterator[c];
        for (int i = 0; i < c; ++i)
            {
            aiter[i] = amap[i].keySet().iterator();
            }

        return new ChainedEnumerator(aiter);
        }

    /**
    * Get the mapping between partition IDs and backing maps.
    *
    * @return the current MapArray data
    */
    protected MapArray getMapArray()
        {
        return m_maparray;
        }

    /**
    * Obtain a backing map for the specified key.
    *
    * @param oKey  the key
    *
    * @return the backing map for the specified key or null if the partition is
    *         missing
    */
    protected Map getBackingMap(Object oKey)
        {
        return getPartitionMap(getContext().getKeyPartition(oKey));
        }

    /**
    * Format a synthetic name for a partition-specific backing map.
    *
    * @param sName  the name of the cache
    * @param nPid   the partition number
    *
    * @return a name that a backing map for a specific partition will be
    *         known by, for example as it should appear in JMX
    */
    protected String makeName(String sName, int nPid)
        {
        return sName + "-" + nPid;
        }

    /**
    * Return a human-readable description for this PartitionBackingMap.
    *
    * @return a String description of the PartitionBackingMap
    */
    public String toString()
        {
        return toString(false);
        }

    /**
    * Return a human-readable description for this PartitionBackingMap.
    *
    * @param fVerbose  if true, generate a verbose descrition
    *
    * @return a String description of the PartitionBackingMap
    */
    public String toString(boolean fVerbose)
        {
        /*
        Verbose format:
            PSBM{
            2: {k21=v21, k22=v22},
            7: {k71=v71, k72=v72},
            ...
            }
        Brief format:
            PSBM{Partitions=[1,3,6,]}
        */

        StringBuffer sb = new StringBuffer(ClassHelper.getSimpleName(getClass()));
        sb.append("{Name=")
          .append(getName())
          .append(',');

        if (!fVerbose)
            {
            sb.append("Partitions=[");
            }

        MapArray array = getMapArray();
        int[]    aiPid = array.getPartitions();
        for (int i = 0, c = aiPid.length; i < c; i++)
            {
            int iPid = aiPid[i];
            if (fVerbose)
                {
                sb.append('\n')
                  .append(iPid).append(": ")
                  .append(array.getBackingMap(iPid));
                }
            else
                {
                sb.append(iPid);
                }
            sb.append(',');
            }

        if (!fVerbose)
            {
            sb.append(']');
            }

        sb.append('}');
        return sb.toString();
        }

    /**
    * Report a missing partition according to the "strict" flag rules.
    *
    * @param oKey  the key for which an operation failed (optional)
    * @param nPid  the missing partition; meaningful only if oKey is null
    */
    protected void reportMissingPartition(Object oKey, int nPid)
        {
        if (nPid < 0)
            {
            nPid = getContext().getKeyPartition(oKey);
            }

        String sMsg = "Partition " + nPid + " does not exist at " +
            toString(false) + (oKey == null ? "" : "; key=" + oKey);
        if (isStrict())
            {
            throw new IllegalStateException(sMsg);
            }
        else
            {
            err(format(sMsg, 1));
            }
        }

    /**
    * Decorate the specified message with the class, method and cache name info.
    *
    * @param sMessage     the raw message
    * @param iStackDepth  index of the actual method on a call stack
    *
    * @return message decorated with the class, method and cache name info
    */
    private String format(String sMessage, int iStackDepth)
        {
        try
            {
            return getStackFrames()[iStackDepth + 1]
                + ": " + getName() + ": " + sMessage;
            }
        catch (Throwable e)
            {
            return sMessage;
            }
        }


    // ----- inner class: MapArray ------------------------------------------

    /**
    * The MapArray is a data structure that provides a mapping between
    * partition IDs and backing maps. It is immutable.
    */
    public static class MapArray
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct an empty MapArray for a given partition count.
        *
        * @param cPartitions  the maximum number of partitions
        */
        public MapArray(int cPartitions)
            {
            this(new PartitionSet(cPartitions), new int[0], new Map[0], new CacheStatistics[0]);
            }

        /**
        * Construct a MapArray.
        *
        * @param partitions   the PartitionSet containing all partitions
        * @param aiPid        an ordered array of partition IDs
        * @param amapBacking  a corresponding array of backing maps
        * @param astat        a corresponding array of CacheStatistics
        *                     objects, any/all of which may be null
        */
        private MapArray(PartitionSet partitions, int[] aiPid, Map[] amapBacking, CacheStatistics[] astat)
            {
            m_partitions = partitions;
            m_ai         = aiPid;
            m_amap       = amapBacking;
            m_astat      = astat;
            }

        // ----- accessors ----------------------------------------------

        /**
        * Obtain an array of all of the current partition IDs for which
        * there are backing maps.
        *
        * @return an array of all the partition IDs; the caller must not
        *         modify the returned array
        */
        public int[] getPartitions()
            {
            return m_ai;
            }

        /**
        * Obtain a PartitionSet containing partition IDs for which there are
        * backing maps.
        *
        * @return a PartitionSet containing all existing partition IDs; the
        *         caller must not modify the returned PartitionSet
        */
        public PartitionSet getPartitionSet()
            {
            return m_partitions;
            }

        /**
        * Obtain the backing map that corresponds to a specified partition.
        *
        * @param nPid  the partition id to obtain the backing map for
        *
        * @return the specified backing map or null if the partition does
        *         not have a backing map
        */
        public Map getBackingMap(int nPid)
            {
            int i = Arrays.binarySearch(m_ai, nPid);
            return i < 0 ? null : m_amap[i];
            }

        /**
        * Obtain an array of all of the current backing maps, one for each
        * partition.
        *
        * @return an array of all the backing maps; the caller must not
        *         modify the returned array
        */
        public Map[] getBackingMaps()
            {
            return m_amap;
            }

        /**
        * Obtain an array of the current backing maps for the partitions
        * specified in the passed PartitionSet.
        *
        * @param partitions a PartitionSet to mask the backing maps by
        *
        * @return an array of the backing maps; the caller must not
        *         modify the returned array
        */
        public Map[] getBackingMaps(PartitionSet partitions)
            {
            if (m_partitions.equals(partitions))
                {
                return m_amap;
                }

            int   cPids = partitions.cardinality();
            Map[] amap  = new Map[cPids];
            if (cPids <= 16)  // ~ cPids/log2(cPids)
                {
                for (int nPid = partitions.next(0), iAppend = 0; nPid >= 0;
                         nPid = partitions.next(nPid + 1))
                    {
                    amap[iAppend++] = getBackingMap(nPid);
                    }
                }
            else
                {
                int[] aiPid   = m_ai;
                Map[] amapAll = m_amap;
                for (int i = 0, c = aiPid.length, iAppend = 0; i < c; ++i)
                    {
                    if (partitions.contains(aiPid[i]))
                        {
                        amap[iAppend++] = amapAll[i];
                        }
                    }
                }
            return amap;
            }

        /**
        * Obtain an array of all of the CacheStatistics objects for the
        * current backing maps, one for each partition. Note that the
        * CacheStatistics object can be null for any/all backing maps.
        *
        * @return an array of all the CacheStatics objects for the backing
        *         maps; the caller must not modify the returned array; the
        *         references in the array can be null
        */
        public CacheStatistics[] getCacheStatistics()
            {
            return m_astat;
            }

        // ----- methods ------------------------------------------------

        /**
        * Add a new mapping between a partition ID and its backing map.
        *
        * @param nPid        the partition ID to add
        * @param mapBacking  the backing map for the partition to add
        *
        * @return a new MapArray object containing the specified mapping
        */
        public MapArray addMap(int nPid, Map mapBacking)
            {
            int            [] aiOld    = m_ai;
            Map            [] amapOld  = m_amap;
            CacheStatistics[] astatOld = m_astat;

            int i = Arrays.binarySearch(aiOld, nPid);
            azzert(i < 0);

            // a negative index indicates the correct insertion point
            i = -(i+1);

            int cOld = aiOld.length;
            int cNew = cOld + 1;

            int            [] aiNew    = new int[cNew];
            Map            [] amapNew  = new Map[cNew];
            CacheStatistics[] astatNew = new CacheStatistics[cNew];

            if (i > 0)
                {
                System.arraycopy(aiOld   , 0, aiNew   , 0, i);
                System.arraycopy(amapOld , 0, amapNew , 0, i);
                System.arraycopy(astatOld, 0, astatNew, 0, i);
                }

            // obtain the CacheStatics object from the backing map, if one
            // is available; otherwise use null
            CacheStatistics stats = null;
            try
                {
                stats = (CacheStatistics) ClassHelper.invoke(
                        mapBacking, "getCacheStatistics", ClassHelper.VOID);
                }
            catch (Throwable e) {}

            aiNew   [i] = nPid;
            amapNew [i] = mapBacking;
            astatNew[i] = stats;

            int cRemain = cOld - i;
            if (cRemain > 0)
                {
                System.arraycopy(aiOld   , i, aiNew   , i+1, cRemain);
                System.arraycopy(amapOld , i, amapNew , i+1, cRemain);
                System.arraycopy(astatOld, i, astatNew, i+1, cRemain);
                }

            PartitionSet partitions = new PartitionSet(m_partitions);
            partitions.add(nPid);

            return new MapArray(partitions, aiNew, amapNew, astatNew);
            }

        /**
        * Remove the mapping for the specified partition ID and its
        * corresponding backing map.
        *
        * @param nPid  the partition ID to remove
        *
        * @return a new MapArray object that does not contain the specified
        *         mapping
        */
        public MapArray removeMap(int nPid)
            {
            int            [] aiOld    = m_ai;
            Map            [] amapOld  = m_amap;
            CacheStatistics[] astatOld = m_astat;

            int i = Arrays.binarySearch(aiOld, nPid);
            azzert(i >= 0);

            int cOld = aiOld.length;
            int cNew = cOld - 1;

            int            [] aiNew    = new int[cNew];
            Map            [] amapNew  = new Map[cNew];
            CacheStatistics[] astatNew = new CacheStatistics[cNew];

            if (i > 0)
                {
                System.arraycopy(aiOld   , 0, aiNew   , 0, i);
                System.arraycopy(amapOld , 0, amapNew , 0, i);
                System.arraycopy(astatOld, 0, astatNew, 0, i);
                }

            int cRemain = cOld - i - 1;
            if (cRemain > 0)
                {
                System.arraycopy(aiOld   , i+1, aiNew   , i, cRemain);
                System.arraycopy(amapOld , i+1, amapNew , i, cRemain);
                System.arraycopy(astatOld, i+1, astatNew, i, cRemain);
                }

            PartitionSet partitions = new PartitionSet(m_partitions);
            partitions.remove(nPid);

            return new MapArray(partitions, aiNew, amapNew, astatNew);
            }

        // ----- data members -------------------------------------------

        /**
        * The PartitionSet containing all partitions for which this backing map
        * manages data.
        */
        private PartitionSet m_partitions;

        /**
        * An array of partition IDs, in ascending order, for which this
        * backing map manages data.
        */
        private int[] m_ai;

        /**
        * An array of backing maps corresponding to the partition IDs in
        * {@link #m_ai}.
        */
        private Map[] m_amap;

        /**
        * An array of CacheStatistics objects corresponding to the partition
        * IDs in {@link #m_ai}.
        */
        private CacheStatistics[] m_astat;
        }


    // ----- inner class: PartitionedIterator -------------------------------

    /**
    * An Iterator designed to ensure that a stable copy of each partition's
    * keys is available for the duration of the iteration over its keys.
    * <p>
    * This is primarily intended for use with the BinaryMap, which does not
    * provide thread-safe iterators if the BinaryMap continues to be modified
    * while the iteration is occurring.
    */
    public class PartitionedIterator
            extends AbstractStableIterator
        {
        /**
        * Construct PartitionedIterator based on the specified array of maps,
        * where each map contains data for one and only one partition.
        *
        * @param amap  an array of underlying maps
        */
        protected PartitionedIterator(Map[] amap)
            {
            m_amap     = amap;
            m_iNextMap = 0;
            m_iNextKey = 0;
            m_aoKey    = NO_OBJECTS;
            }

        // ----- AbstractStableIterator methods -------------------------

        /**
        * {@inheritDoc}
        */
        protected void advance()
            {
            Object[] aoKey = m_aoKey;
            int      iNext = m_iNextKey;
            while (iNext >= aoKey.length)
                {
                // advance to next pid/map
                Map[] amap     = m_amap;
                int   iNextMap = m_iNextMap;
                if (iNextMap >= amap.length)
                    {
                    m_aoKey    = NO_OBJECTS;
                    m_iNextKey = 0;
                    return;
                    }
                else
                    {
                    // the keySet().toArray() is used to avoid any possible
                    // instability from concurrent changes, probably for the
                    //  same reason that it is used by BinaryMapStore.keys()
                    m_aoKey    = aoKey = amap[iNextMap].keySet().toArray();
                    m_iNextKey = iNext = 0;
                    m_iNextMap = iNextMap + 1;
                    }
                }

            setNext(aoKey[iNext]);
            m_iNextKey = iNext + 1;
            }

        /**
        * {@inheritDoc}
        */
        protected void remove(Object oPrev)
            {
            PartitionSplittingBackingMap.this.remove(oPrev);
            }

        // ----- data members -------------------------------------------

        /**
        * The array of backing maps containing the keys to iterate.
        */
        private Map[] m_amap;

        /**
        * The index of the next pid/map pair to get keys from.
        */
        private int m_iNextMap;

        /**
        * Current array of keys.
        */
        private Object[] m_aoKey;

        /**
        * Index of the next key to iterate.
        */
        private int m_iNextKey;
        }


    // ----- inner class: MaskedPartitionMap --------------------------------

    /**
    * A read-only view into a subset of backing maps managed by the underlying
    * PartitionSplittingBackingMap.
    */
    public class MaskedPartitionMap
            extends AbstractKeyBasedMap
            implements Map
        {
        /**
        * Construct MaskedPartitionMap based on the specified PartitionSet.
        *
        * @param partMask  the partition set that indicates which backing maps
        *                  are represented by this MaskedPartitionMap
        */
        protected MaskedPartitionMap(PartitionSet partMask)
            {
            MapArray array = PartitionSplittingBackingMap.this.getMapArray();

            PartitionSet partCurrent = array.getPartitionSet();
            if (!partCurrent.contains(partMask))
                {
                String sMsg = partMask + " not a subset of " + partCurrent;
                if (isStrict())
                    {
                    throw new IllegalArgumentException(sMsg);
                    }
                else
                    {
                    err(sMsg);
                    partMask = new PartitionSet(partMask); // clone
                    partMask.retain(partCurrent);
                    }
                }

            m_partMask = partMask;
            m_amap     = array.getBackingMaps(partMask);
            }

        /**
        * {@inheritDoc}
        */
        public Object get(Object oKey)
            {
            return m_partMask.contains(getContext().getKeyPartition(oKey))
                ? PartitionSplittingBackingMap.this.get(oKey) : null;
            }

        /**
        * {@inheritDoc}
        */
        public boolean containsKey(Object oKey)
            {
            return m_partMask.contains(getContext().getKeyPartition(oKey))
                && PartitionSplittingBackingMap.this.containsKey(oKey);
            }

        /**
        * {@inheritDoc}
        */
        public boolean isEmpty()
            {
            return PartitionSplittingBackingMap.this.isEmpty(m_amap);
            }

        /**
        * {@inheritDoc}
        */
        public int size()
            {
            return PartitionSplittingBackingMap.this.size(m_amap);
            }

        /**
        * {@inheritDoc}
        */
        protected Iterator iterateKeys()
            {
            return PartitionSplittingBackingMap.this.iterateKeys(m_amap);
            }

        // ----- data members -------------------------------------------

        /**
        * The PartitionSet the underlying PartitionSplittingBackingMap is
        * masked by.
        */
        private PartitionSet m_partMask;

        /**
        * The snapshot array of backing maps that are represented by this
        * MaskedPartitionMap.
        */
        private Map[] m_amap;
        }

    // ----- inner class: AggregatingCacheStatistics ------------------------

    /**
    * An implementation of the CacheStatics interface that aggregates across
    * the CacheStatics objects provided by each of the backing maps
    * corresponding to the partitions managed by this
    * PartitionSplittingBackingMap.
    */
    public class AggregatingCacheStatistics
            implements CacheStatistics
        {
        // ----- CacheStatistics interface ------------------------------

        /**
        * {@inheritDoc}
        */
        public long getTotalGets()
            {
            return calculateTotal(PROP_GETS);
            }

        /**
        * {@inheritDoc}
        */
        public long getTotalGetsMillis()
            {
            return calculateTotal(PROP_GETS_MILLIS);
            }

        /**
        * {@inheritDoc}
        */
        public double getAverageGetMillis()
            {
            double cMillis = getTotalGetsMillis();
            double cGets   = getTotalGets();
            return cGets == 0.0 ? 0.0 : cMillis / cGets;
            }

        /**
        * {@inheritDoc}
        */
        public long getTotalPuts()
            {
            return calculateTotal(PROP_PUTS);
            }

        /**
        * {@inheritDoc}
        */
        public long getTotalPutsMillis()
            {
            return calculateTotal(PROP_PUTS_MILLIS);
            }

        /**
        * {@inheritDoc}
        */
        public double getAveragePutMillis()
            {
            double cMillis = getTotalPutsMillis();
            double cPuts   = getTotalPuts();
            return cPuts == 0.0 ? 0.0 : cMillis / cPuts;
            }

        /**
        * {@inheritDoc}
        */
        public long getCacheHits()
            {
            return calculateTotal(PROP_HITS);
            }

        /**
        * {@inheritDoc}
        */
        public long getCacheHitsMillis()
            {
            return calculateTotal(PROP_HITS_MILLIS);
            }

        /**
        * {@inheritDoc}
        */
        public double getAverageHitMillis()
            {
            double cMillis = getCacheHitsMillis();
            double cGets   = getCacheHits();
            return cGets == 0.0 ? 0.0 : cMillis / cGets;
            }

        /**
        * {@inheritDoc}
        */
        public long getCacheMisses()
            {
            return calculateTotal(PROP_MISSES);
            }

        /**
        * {@inheritDoc}
        */
        public long getCacheMissesMillis()
            {
            return calculateTotal(PROP_MISSES_MILLIS);
            }

        /**
        * {@inheritDoc}
        */
        public double getAverageMissMillis()
            {
            double cMillis = getCacheMissesMillis();
            double cGets   = getCacheMisses();
            return cGets == 0.0 ? 0.0 : cMillis / cGets;
            }

        /**
        * {@inheritDoc}
        */
        public double getHitProbability()
            {
            double cHits   = getCacheHits();
            double cTotal  = cHits + getCacheMisses();
            return cTotal == 0.0 ? 0.0 : cHits / cTotal;
            }

        /**
        * {@inheritDoc}
        */
        public long getCachePrunes()
            {
            return calculateTotal(PROP_PRUNES);
            }

        /**
        * {@inheritDoc}
        */
        public long getCachePrunesMillis()
            {
            return calculateTotal(PROP_PRUNES_MILLIS);
            }

        /**
        * {@inheritDoc}
        */
        public void resetHitStatistics()
            {
            CacheStatistics[] astat = getMapArray().getCacheStatistics();
            for (int i = 0, c = astat.length; i < c; ++i)
                {
                CacheStatistics stats = astat[i];
                if (stats != null)
                    {
                    stats.resetHitStatistics();
                    }
                }
            }

        // ----- internal helpers ---------------------------------------

        /**
        * Calculate the statistics total for a given statistics property
        * using the underlying CacheStatics objects corresponding to the
        * backing maps for each partition.
        *
        * @param nProp  a property indicator; one of the PROP_* constants
        *
        * @return an aggregated total value as a long, or <tt>-1L</tt> if
        *         there were no underlying CacheStatics objects or if all of
        *         the underlying CacheStatics objects returned <tt>-1L</tt>
        */
        private long calculateTotal(int nProp)
            {
            long    cTotal = 0L;
            boolean fAny   = false;

            CacheStatistics[] astat = getMapArray().getCacheStatistics();
            for (int i = 0, c = astat.length; i < c; ++i)
                {
                CacheStatistics stats = astat[i];
                if (stats != null)
                    {
                    long n;
                    switch (nProp)
                        {
                        case PROP_GETS:
                            n = stats.getTotalGets();
                            break;
                        case PROP_GETS_MILLIS:
                            n = stats.getTotalGetsMillis();
                            break;
                        case PROP_PUTS:
                            n = stats.getTotalPuts();
                            break;
                        case PROP_PUTS_MILLIS:
                            n = stats.getTotalPutsMillis();
                            break;
                        case PROP_HITS:
                            n = stats.getCacheHits();
                            break;
                        case PROP_HITS_MILLIS:
                            n = stats.getCacheHitsMillis();
                            break;
                        case PROP_MISSES:
                            n = stats.getCacheMisses();
                            break;
                        case PROP_MISSES_MILLIS:
                            n = stats.getCacheMissesMillis();
                            break;
                        case PROP_PRUNES:
                            n = stats.getCachePrunes();
                            break;
                        case PROP_PRUNES_MILLIS:
                            n = stats.getCachePrunesMillis();
                            break;
                        default:
                            throw new IllegalStateException(
                                    "invalid property=" + nProp);
                        }

                    if (n >= 0)
                        {
                        cTotal += n;
                        fAny    = true;
                        }
                    }
                }

            return fAny ? cTotal : -1;
            }

        // ----- Object methods -----------------------------------------

        /**
        * For debugging purposes, format the statistics information into a
        * human-readable format.
        *
        * @return a String representation of this object
        */
        public String toString()
            {
            StringBuffer sb = new StringBuffer();

            sb.append("CacheStatistics {TotalGets=")
              .append(getTotalGets())
              .append(", TotalGetsMillis=")
              .append(getTotalGetsMillis())
              .append(", AverageGetMillis=")
              .append(getAverageGetMillis())
              .append(", TotalPuts=")
              .append(getTotalPuts())
              .append(", TotalPutsMillis=")
              .append(getTotalPutsMillis())
              .append(", AveragePutMillis=")
              .append(getAveragePutMillis())
              .append(", CacheHits=")
              .append(getCacheHits())
              .append(", CacheHitsMillis=")
              .append(getCacheHitsMillis())
              .append(", AverageHitMillis=")
              .append(getAverageHitMillis())
              .append(", CacheMisses=")
              .append(getCacheMisses())
              .append(", CacheMissesMillis=")
              .append(getCacheMissesMillis())
              .append(", AverageMissMillis=")
              .append(getAverageMissMillis())
              .append(", HitProbability=")
              .append(getHitProbability())
              .append(", Prunes=")
              .append(getCachePrunes())
              .append(", PruneMillis=")
              .append(getCachePrunesMillis())
              .append('}');

            return sb.toString();
            }

        // ----- constants ----------------------------------------------

        private static final int PROP_GETS          = 0;
        private static final int PROP_GETS_MILLIS   = 1;
        private static final int PROP_PUTS          = 2;
        private static final int PROP_PUTS_MILLIS   = 3;
        private static final int PROP_HITS          = 4;
        private static final int PROP_HITS_MILLIS   = 5;
        private static final int PROP_MISSES        = 6;
        private static final int PROP_MISSES_MILLIS = 7;
        private static final int PROP_PRUNES        = 8;
        private static final int PROP_PRUNES_MILLIS = 9;
        }

    // ----- inner class: KeySet --------------------------------------------

    /**
    * A KeySet implementation optimized for PartitionSplittingBackingMap.
    * <p>
    * The default implementation of {@link java.util.AbstractSet#removeAll removeAll}
    * determines the smaller collection to walk, which involves a call to
    * {@link #size()}; this is highly inefficient for PartitionSplittingBackingMap.
    */
    protected class KeySet
            extends AbstractKeyBasedMap.KeySet
        {
        /**
        * {@inheritDoc}
        */
        @Override
        public boolean removeAll(Collection coll)
            {
            boolean fRemove = false;
            for (Object oKey : coll)
                {
                fRemove |= PartitionSplittingBackingMap.this.removeBlind(oKey);
                }
            return fRemove;
            }
        }

    // ----- constants ------------------------------------------------------

    /**
    * An array of no items.
    */
    static final Object[] NO_OBJECTS = new Object[0];


    // ----- data members ---------------------------------------------------

    /**
    * The BackingMapManager to use as the factory for the per-partition
    * backing maps.
    * <p>
    * This field is immutable.
    */
    private BackingMapManager m_bmm;

    /**
    * The BackingMapManagerContext.
    */
    private BackingMapManagerContext m_ctx;

    /**
    * The name of the cache for which this serves as a backing map.
    * <p>
    * This field is immutable.
    */
    private String m_sName;

    /**
    * The current partition ID to backing map mapping.
    * <p>
    * This field is volatile; the data structure it references is immutable.
    */
    private volatile MapArray m_maparray;

    /**
    * Cache statistics. (Lazily instantiated.)
    */
    private AggregatingCacheStatistics m_stats;

    /**
    * True to strictly enforce the partition absense.
    */
    private boolean m_fStrict = true;
    }
