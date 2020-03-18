/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.oracle.coherence.common.collections.AbstractStableIterator;

import com.tangosol.util.comparator.SafeComparator;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;


/**
* SafeSortedMap is an implementation of {@link java.util.SortedMap} based on a
* skip-list that is structurally thread-safe.  SafeSortedMap uses lockless
* (CAS-based) algorithms so all operations are performed without
* synchronization.  In the presence of concurrent updates, SafeSortedMap
* iterators will reflect some state of the map, but will not throw
* ConcurrentModificationException or other unexpected exceptions.
* <p>
* Note that the ordering maintained by a sorted map (whether or not an
* explicit comparator is provided) must be <i>consistent with equals</i> if
* this sorted map is to correctly implement the <tt>Map</tt> interface.  (See
* <tt>Comparable</tt> or <tt>Comparator</tt> for a precise definition of
* <i>consistent with equals</i>.)  This is so because the <tt>Map</tt>
* interface is defined in terms of the equals operation, but a map performs
* all key comparisons using its <tt>compareTo</tt> (or <tt>compare</tt>)
* method, so two keys that are deemed equal by this method are, from the
* standpoint of the sorted map, equal.  The behavior of a sorted map
* <i>is</i> well-defined even if its ordering is inconsistent with equals; it
* just fails to obey the general contract of the <tt>Map</tt> interface.
* <p>
* The structure of the SafeSortedMap is maintained in a 2-dimensional lattice:
* <pre>
*  Top
*   |
*  I3 -------------------------------&gt; S5 -------------&gt;
*   |                                   |
*  I2 -------------&gt; S2 -------------&gt; S5 -------------&gt;
*   |                 |                 |
*  I1 -------&gt; S1 -&gt; S2 -------&gt; S4 -&gt; S5 -------&gt; S7 -&gt;
*   |           |     |           |     |           |
*  E  -&gt; E0 -&gt; E1 -&gt; E2 -&gt; E3 -&gt; E4 -&gt; E5 -&gt; E6 -&gt; E7 -&gt;
* </pre>
* <p>
* Searching for a key involves traversing, starting from the top index node,
* as far to the right at each index level, then down.  For example, the search
* path for the element E4 in the above example would be:
* <ol>
* <li>compare E4 with S5 on index level 3; move down to level 2
* <li>compare E4 with S2 on index level 2; move right
* <li>compare E4 with S5 on index level 2; move down to level 1
* <li>compare E4 with S4 on index level 1; move right
* <li>move down
* </ol>
* The search path for the element E7 in the above example would be:
* <ol>
* <li>compare E7 with S5 on index level 3; move right
* <li>move down to level 2
* <li>move down to level 1
* <li>compare E7 with S7 on index level 1; move right
* <li>move down
* </ol>
* <p>
* Removing a key involves inserting a temporary marker-entry while the entry
* pointers are fixed up.  The marker node allows uncontended read threads to
* traverse the safely.  Additionally, removing a key causes any index nodes
* for that key to be removed as well.
* <p>
* In the above example, the steps to remove entry E3 would be:
*
* <ol>
* <li>find LT search-path of Top-&gt;I3-&gt;S2-&gt;S2-&gt;E2
* <li>create new marker node D3
* <li>set D3 -&gt; E4
* <li>set E3 -&gt; D3
* <li>set E2 -&gt; E3
* </ol>
*
* The steps to remove E4 (indexed to level 1) would be:
*
* <ol>
* <li>find LT search-path of Top-&gt;I3-&gt;S2-&gt;S2-&gt;E3
* <li>create new marker node D4
* <li>set D4 -&gt; E5
* <li>set E4 -&gt; D4
* <li>set E3 -&gt; E5
* <li>move up to level 1
* <li>set D4' -&gt; S5
* <li>set S4  -&gt; D4'
* <li>set S2  -&gt; S5
* </ol>
*
* Inserting a key happens in 'bottom-up' fashion, with the entry node being
* inserted first, (possibly) followed by index nodes of increasing level.
* Inserts are fully synchronized on the map.  When inserting a new entry, an
* index level randomly computed such that, the probability that a given
* insertion will be indexed to level <i>n</i> is approximately given by:
* <i>(1/k)<sup>n</sup></i> where <i>k</i> is the average span of each index
* level.
*
* In the above example, the steps to insert entry E3.5 at index-level 1 would
* be:
* <ol>
* <li>find LT search-path of Top-&gt;I3-&gt;S2-&gt;S2-&gt;E2
* <li>create new entry node E3.5
* <li>set E3.5 -&gt; E4
* <li>set E2 -&gt; E3.5
* <li>move up to level 1
* <li>create new entry node S3.5
* <li>set S3.5 -&gt; S4
* <li>set S2 -&gt; S3.5
* </ol>
* <p>
* @see
* <a href="ftp://ftp.cs.umd.edu/pub/skipLists/skiplists.pdf">Bill Pugh's original paper</a>
*
* @since  Coherence 3.5
* @author rhl 2009.03.31
*/
public class SafeSortedMap
        extends AbstractMap
        implements SortedMap
    {
    /*
    * This implementation is heavily based on the
    * java.util.concurrent.ConcurrentSkipListMap, but with several important
    * differences:
    * - support for null keys and values
    * - support for configurable spans
    * - reduced overhead for non-Comparable key types
    * - reduced cost of size() operation
    *
    * In order to improve performance, this implementation is intentionally
    * "unrefactored", and many logical operations are aggressively inlined.
    */

    // ----- constructors -------------------------------------------------

    /**
    * Construct a new SafeSortedMap using the natural ordering of the
    * Comparable keys in this map.
    */
    public SafeSortedMap()
        {
        this(SafeComparator.INSTANCE);
        }

    /**
    * Construct a new SafeSortedMap with the specified Comparator.
    *
    * @param comparator  the comparator used to sort this map
    */
    public SafeSortedMap(Comparator comparator)
        {
        this(comparator, DEFAULT_SPAN, DEFAULT_MAX_LEVEL);
        }

    /**
    * Construct a new SafeSortedMap with the specified parameters.
    *
    * @param comparator  the comparator used to sort this map, or null for
    *                    the entries' natural ordering
    * @param nSpan       the average span to use for each index-level
    * @param nLevelMax   the maximum index-level to use for this map
    */
    public SafeSortedMap(Comparator comparator, int nSpan, int nLevelMax)
        {
        Base.azzert(nSpan > 1, "Span must be larger than 1");

        m_comparator     = comparator == null ? SafeComparator.INSTANCE : comparator;
        m_nLevelMax      = nLevelMax;
        m_nSpan          = nSpan;
        m_mapView        = new ViewMap(null, null);
        m_aflProbabilityThresholds = computeProbabilityThresholds(nLevelMax, nSpan);

        initialize();
        }


    // ----- Map interface ------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public int size()
        {
        // Note: size counter is kept in the base node to allow for atomic
        //       clear() operation
        return getBaseNode().getSizeCounter().get();
        }

    /**
    * {@inheritDoc}
    */
    public boolean containsKey(Object oKey)
        {
        return findNearest(getTopNode(), oKey, SEARCH_EQ, false) != null;
        }

    /**
    * {@inheritDoc}
    */
    public Set entrySet()
        {
        return getViewMap().entrySet();
        }

    /**
    * {@inheritDoc}
    */
    public Object put(Object oKey, Object oValue)
        {
        Comparator comparator = comparator();
        while (true)
            {
            IndexNode     nodeTop  = getTopNode();
            BaseEntryNode nodeBase = (BaseEntryNode) nodeTop.getEntryNode();
            EntryNode     nodePrev = findPredecessor(nodeTop, oKey);
            EntryNode     nodeCur  = nodePrev.getNext();

            // Note: all operations are preformed relative to the top node, so
            //       in the presence of concurrent clear() operations, all
            //       mutations occur against a consistent (but perhaps
            //       out-of-date) snapshot of state
            while (true)
                {
                if (nodeCur != null)
                    {
                    EntryNode nodeNext = nodeCur.getNext();

                    if (nodeCur != nodePrev.getNext())
                        {
                        // inconsistent read; restart the loop.
                        //
                        // Note: this check is not strictly needed, but avoids
                        // unnecessary CAS contention in race-cases
                        break;
                        }

                    // check to see if the node is deleted
                    Object oValueOld = nodeCur.getValueInternal();
                    if (oValueOld == NO_VALUE)
                        {
                        // concurrent delete; help it along
                        nodeCur.helpDelete(nodePrev, nodeNext);
                        break;
                        }
                    if (nodePrev.isDeleted())
                        {
                        break;
                        }

                    int nCompare = comparator.compare(oKey, nodeCur.getKey());
                    if (nCompare == 0)
                        {
                        // found the correct EntryNode
                        if (!nodeCur.casValue(oValueOld, oValue))
                            {
                            // lost the race to update; try again, as we could
                            // be racing against a delete
                            break;
                            }
                        return oValueOld;
                        }
                    else if (nCompare > 0)
                        {
                        nodePrev = nodeCur;
                        nodeCur  = nodeNext;
                        continue;
                        }
                    // else if (nCompare < 0) ==> fall-through
                    }

                // entry was not found
                EntryNode nodeNew = new EntryNode(oKey, oValue, nodeCur);
                if (nodePrev.casNext(nodeCur, nodeNew))
                    {
                    // successful insert; update the size
                    nodeBase.getSizeCounter().incrementAndGet();
                    }
                else
                    {
                    // lost the race to insert; try again
                    break;
                    }

                int nLevelThis = calculateRandomLevel();
                if (nLevelThis > 0)
                    {
                    insertIndex(nodeTop, nodeNew, nLevelThis);
                    }
                return null;
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public Object get(Object oKey)
        {
        EntryNode node = findNearest(getTopNode(), oKey, SEARCH_EQ, false);
        return node == null ? null : node.getValue();
        }

    /**
    * Locate a Map.Entry in this map based on its key.
    * <p>
    * Note: the behaviour of {#setValue} on the returned Entry is undefined in
    *       the presence of concurrent modifications
    *
    * @param oKey  the key to return an Entry for
    *
    * @return an Entry corresponding to the specified key, or null if none exists
    */
    public Map.Entry getEntry(Object oKey)
        {
        EntryNode nodeEntry = findNearest(getTopNode(), oKey, SEARCH_EQ, false);
        if (nodeEntry != null)
            {
            // COH-3646: Must fetch the Map.Entry (populating the value) field
            // before (re)-checking if the node is deleted.
            Map.Entry entry = nodeEntry.getMapEntry();
            if (!nodeEntry.isDeleted())
                {
                return entry;
                }
            }
        return null;
        }

    /**
    * {@inheritDoc}
    */
    public Object remove(Object oKey)
        {
        Comparator comparator = comparator();
        while (true)
            {
            IndexNode     nodeTop  = getTopNode();
            BaseEntryNode nodeBase = (BaseEntryNode) nodeTop.getEntryNode();
            EntryNode     nodePrev = findPredecessor(nodeTop, oKey);
            EntryNode     nodeCur  = nodePrev.getNext();

            // Note: all operations are preformed relative to the top node, so
            //       in the presence of concurrent clear() operations, all
            //       mutations occur against a consistent (but perhaps
            //       out-of-date) snapshot of state
            while (true)
                {
                if (nodeCur != null)
                    {
                    EntryNode nodeNext = nodeCur.getNext();

                    if (nodeCur != nodePrev.getNext())
                        {
                        // inconsistent read; restart the loop.
                        //
                        // Note: this check is not strictly needed, but avoids
                        // unnecessary CAS contention in race-cases
                        break;
                        }

                    // check to see if the node is deleted
                    Object oValueOld = nodeCur.getValueInternal();
                    if (oValueOld == NO_VALUE)
                        {
                        // concurrent delete; help it along
                        nodeCur.helpDelete(nodePrev, nodeNext);
                        break;
                        }

                    if (nodePrev.isDeleted())
                        {
                        break;
                        }

                    int nCompare = comparator.compare(oKey, nodeCur.getKey());
                    if (nCompare == 0)
                        {
                        // found the correct EntryNode to delete
                        if (nodeCur.casValue(oValueOld, NO_VALUE))
                            {
                            // successful remove; update the size
                            nodeBase.getSizeCounter().decrementAndGet();
                            }
                        else
                            {
                            // lost the race to delete; try again
                            break;
                            }

                        if (!nodeCur.markForDelete(nodeNext) ||
                            !nodePrev.casNext(nodeCur, nodeNext))
                            {
                            // this can only mean a concurrent insert or remove.
                            // Call findNearest() to "force" a structural deletion
                            // here; we've already CAS'd the value to be
                            // deleted, but we need to append a marker and
                            // remove the node.
                            findNearest(nodeTop, oKey, SEARCH_EQ, true);
                            }
                        else
                            {
                            // clean the indices
                            findPredecessor(nodeTop, oKey);
                            }
                        return oValueOld;
                        }
                    else if (nCompare > 0)
                        {
                        // concurrent insert; try again
                        nodePrev = nodeCur;
                        nodeCur  = nodeNext;
                        continue;
                        }
                    // else if (nCompare < 0) ==> fall-through
                    }

                // entry was not found
                return null;
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void clear()
        {
        initialize();
        }


    // ----- SortedMap interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Comparator comparator()
        {
        return m_comparator;
        }

    /**
    * {@inheritDoc}
    */
    public SortedMap subMap(Object fromKey, Object toKey)
        {
        if (comparator().compare(fromKey, toKey) > 0)
            {
            throw new IllegalArgumentException();
            }
        return new ViewMap(fromKey, toKey);
        }

    /**
    * {@inheritDoc}
    */
    public SortedMap headMap(Object toKey)
        {
        return new ViewMap(null, toKey);
        }

    /**
    * {@inheritDoc}
    */
    public SortedMap tailMap(Object fromKey)
        {
        return new ViewMap(fromKey, null);
        }

    /**
    * {@inheritDoc}
    */
    public Object firstKey()
        {
        return getViewMap().firstKey();
        }

    /**
    * {@inheritDoc}
    */
    public Object lastKey()
        {
        return getViewMap().lastKey();
        }


    // ----- accessors ----------------------------------------------------

    /**
    * Return a view of the entire map.
    *
    * @return a view of the entire map
    */
    protected ViewMap getViewMap()
        {
        return m_mapView;
        }

    /**
    * Return the precomputed array of probability thresholds used by this map to
    * determine what index levels to build.
    *
    * @return the precomputed array of probability thresholds
    *
    * @see #calculateRandomLevel
    */
    protected float[] getProbabilityThresholds()
        {
        return m_aflProbabilityThresholds;
        }

    /**
    * Return the random number generator for this map.
    *
    * @return the random number generator for this map
    */
    protected Random getRandom()
        {
        return m_random;
        }

    /**
    * Return the maximum index-level that this map is allowed to reach.
    *
    * @return the maximum index-level that this map is allowed to reach
    */
    protected int getMaxLevel()
        {
        return m_nLevelMax;
        }

    /**
    * Return the span of this map.
    *
    * @return the span of this map
    */
    protected int getSpan()
        {
        return m_nSpan;
        }


    // ----- internal -----------------------------------------------------

    /**
    * Initialize the index nodes.
    */
    protected void initialize()
        {
        // Replace the base node with a new one.
        //
        // The BaseEntryNode carries the size counter, so replacing the base
        // node allows for atomically (re-)initializing the map with an empty
        // entry-list and counter.
        BaseEntryNode nodeBase = new BaseEntryNode();
        nodeBase.setNext(null);

        // start out with just 1 index level
        setTopNode(new IndexNode(1, null, nodeBase));
        }

    /**
    * Return the top index node in the map.
    *
    * @return the top index node in the map
    */
    protected IndexNode getTopNode()
        {
        return m_nodeTop;
        }

    /**
    * Set the top index node in the map.
    *
    * @param nodeTop  the new top index node
    */
    protected void setTopNode(IndexNode nodeTop)
        {
        m_nodeTop = nodeTop;
        }

    /**
    * Atomically set specified IndexNode to the top node iff the current top
    * node is the expected top node.
    *
    * @param nodeTopAssume  the IndexNode assumed to be the current top node
    * @param nodeTopNew     the new top node
    *
    * @return true iff the top node is successfully updated
    */
    protected boolean casTopNode(IndexNode nodeTopAssume, IndexNode nodeTopNew)
        {
        return m_atomicUpdaterTopNode.compareAndSet(this, nodeTopAssume, nodeTopNew);
        }

    /**
    * Return the base entry node.
    *
    * @return the base entry node
    */
    protected BaseEntryNode getBaseNode()
        {
        return (BaseEntryNode) getTopNode().getEntryNode();
        }

    /**
    * Return the first valid EntryNode, or null if there are none.
    *
    * @return the first EntryNode or null if empty
    */
    protected EntryNode firstNode()
        {
        while (true)
            {
            EntryNode nodeBase = getBaseNode();
            EntryNode nodeNext = nodeBase.getNext();

            if (nodeNext != null && nodeNext.isDeleted())
                {
                // first node is being deleted; help it along and
                nodeNext.helpDelete(nodeBase, nodeNext.getNext());
                }
            else
                {
                return nodeNext;
                }
            }
        }

    /**
    * Return the last valid EntryNode, or null if there are none.
    *
    * @return the last valid EntryNode or null if empty
    */
    protected EntryNode lastNode()
        {
        outer_loop: while (true)
            {
            IndexNode nodeTop  = getTopNode();
            SkipNode  nodeCur;
            SkipNode  nodeNext = nodeTop;

            do
                {
                nodeCur  = nodeNext;
                nodeNext = nodeCur.getNext();
                if (nodeNext != null)
                    {
                    if (nodeNext.getEntryNode().isDeleted())
                        {
                        // try to fix the index and start over
                        nodeCur.casNext(nodeNext, nodeNext.getNext());
                        continue outer_loop;
                        }
                    }
                else
                    {
                    nodeNext = nodeCur.getBelow();
                    }
                }
            while (nodeNext != null);

            EntryNode nodeEntry     = nodeCur.getEntryNode();
            EntryNode nodeEntryNext = nodeEntry.getNext();
            while (true)
                {
                if (nodeEntryNext == null)
                    {
                    // reached the end; need to check for the "empty list" case
                    return nodeEntry == nodeTop.getEntryNode() ? null : nodeEntry;
                    }

                EntryNode nodeEntryAfter = nodeEntryNext.getNext();
                if (nodeEntryNext != nodeEntry.getNext())
                    {
                    continue outer_loop;
                    }
                if (nodeEntryNext.isDeleted())
                    {
                    nodeEntryNext.helpDelete(nodeEntry, nodeEntryAfter);
                    continue outer_loop;
                    }
                if (nodeEntry.isDeleted())
                    {
                    continue outer_loop;
                    }
                nodeEntry     = nodeEntryNext;
                nodeEntryNext = nodeEntryAfter;
                }
            }
        }

    /**
    * Insert a index nodes for the specified newly inserted EntryNode, up to a
    * random index-level.  Return true if the key was inserted, or false
    * otherwise (if an intervening insert was made for the same key).
    *
    * @param nodeTop     the top index node
    * @param nodeNew     the newly inserted EntryNode
    * @param nLevelThis  the level at which to index the specified EntryNode
    */
    protected void insertIndex(IndexNode nodeTop, EntryNode nodeNew, int nLevelThis)
        {
        // check to see if we need to add a new index level
        while (true)
            {
            int nLevelMax = nodeTop.getLevel();

            if (nLevelThis > nLevelMax)
                {
                // increase the max level of this map's indices
                IndexNode nodeTopOld = nodeTop;
                EntryNode nodeBase   = nodeTop.getEntryNode();
                while (nLevelMax < nLevelThis)
                    {
                    nodeTop = new IndexNode(++nLevelMax, nodeTop, nodeBase);
                    }

                if (!casTopNode(nodeTopOld, nodeTop))
                    {
                    // racing against another thread to increase the index
                    // levels, or the index node has been replaced (clear())
                    nLevelThis = nodeTop.getLevel();
                    break;
                    }
                }
            else
                {
                // no need to add index levels
                break;
                }
            }

        // build the index chain to insert
        SkipNode nodeIndex = null;
        for (int i = 1; i <= nLevelThis; i++)
            {
            nodeIndex = new SkipNode(nodeIndex, nodeNew);
            }

        // traverse the index structure and link in the new index nodes
        Comparator comparator   = comparator();
        int        nLevelInsert = nLevelThis;
        Object     oKeyNew      = nodeNew.getKey();
        while (true)
            {
            int      nLevel   = nodeTop.getLevel();
            SkipNode nodeCur  = nodeTop;
            SkipNode nodeNext = nodeCur.getNext();
            SkipNode nodeIdx  = nodeIndex;

            while (true)
                {
                if (nodeNext != null)
                    {
                    EntryNode nodeEntry = nodeNext.getEntryNode();
                    int       nCompare = comparator.compare(oKeyNew, nodeEntry.getKey());

                    if (nodeEntry.isDeleted())
                        {
                        if (!nodeCur.casNext(nodeNext, nodeNext.getNext()))
                            {
                            // lost a race; retry from the top
                            break;
                            }
                        nodeNext = nodeCur.getNext();
                        continue;
                        }
                    if (nCompare > 0)
                        {
                        nodeCur  = nodeNext;
                        nodeNext = nodeNext.getNext();
                        continue;
                        }
                    }

                if (nLevel == nLevelInsert)
                    {
                    if (nodeNew.isDeleted())
                        {
                        // concurrent delete; stop index insertion and call
                        // findNearest to force index cleanup
                        findNearest(nodeTop, oKeyNew, SEARCH_EQ, true);
                        return;
                        }

                    // link the new index-node in between nodeCur and nodeNext
                    nodeIdx.setNext(nodeNext);
                    if (!nodeCur.casNext(nodeNext, nodeIdx))
                        {
                        // concurrent index insertion; start over from scratch
                        break;
                        }

                    if (--nLevelInsert == 0)
                        {
                        // one last check before we return to see if the newly
                        // inserted node has been deleted.
                        //
                        // Note: we need to do this check here, as deletion
                        //       relies on index-traversal logic (which could
                        //       complete before this concurrent insert does)
                        //       to remove the indices.
                        if (nodeNew.isDeleted())
                            {
                            // node was deleted; call findNearest() to ensure
                            // that the newly inserted index nodes will be
                            // cleaned
                            findNearest(nodeTop, oKeyNew, SEARCH_EQ, true);
                            }
                        return;
                        }
                    }

                if (--nLevel >= nLevelInsert && nLevel < nLevelThis)
                    {
                    nodeIdx = nodeIdx.getBelow();
                    }
                nodeCur  = nodeCur.getBelow();
                nodeNext = nodeCur.getNext();
                }
            }
        }

    /**
    * Return the SkipNode closest to the specified key, or null.
    *
    * @param nodeTop    the top index node
    * @param oKey       the key to search for
    * @param nMode      one of the SEARCH_* constants
    * @param fFixLinks  true iff deletion links must be fixed
    *
    * @return the SkipNode closest to the specified key, or null
    */
    protected EntryNode findNearest(
            IndexNode nodeTop, Object oKey, int nMode, boolean fFixLinks)
        {
        Comparator comparator = comparator();

        while (true)
            {
            EntryNode nodePrev;
            if (nMode == SEARCH_EQ && !fFixLinks)
                {
                // optimized probe for get() and containsKey() operations that
                // do not require the "previous" entry node for link maintenance
                nodePrev = findNearestIndexedEntry(nodeTop, oKey, SEARCH_LTEQ);
                if (nodePrev != nodeTop.getEntryNode() &&
                    comparator.compare(nodePrev.getKey(), oKey) == 0)
                    {
                    // found the exact entry on index probe
                    return nodePrev;
                    }
                }
            else
                {
                // find some arbitrary predecessor
                nodePrev = findNearestIndexedEntry(nodeTop, oKey, SEARCH_LT);
                }

            EntryNode nodeCur = nodePrev.getNext();

            while (true)
                {
                if (nodeCur == null)
                    {
                    // this means: nodePrev < oKey  where nodePrev is the last
                    return ((nMode & SEARCH_LT) != 0) ? nodePrev : null;
                    }

                EntryNode nodeNext = nodeCur.getNext();
                if (nodeCur != nodePrev.getNext())
                    {
                    // inconsistent read; restart the loop.
                    //
                    // Note: this check is not strictly needed, but avoids
                    // unnecessary CAS contention in race-cases
                    break;
                    }

                if (nodeCur.isDeleted())
                    {
                    nodeCur.helpDelete(nodePrev, nodeNext);
                    break;
                    }
                if (nodePrev.isDeleted())
                    {
                    break;
                    }

                int nCompare = comparator.compare(oKey, nodeCur.getKey());
                if (nCompare < 0)
                    {
                    // this means: nodePrev < oKey < nodeCur
                    if (nMode == SEARCH_EQ)
                        {
                        // looking for an exact match only, and we've walked
                        // beyond the key we are searching for without finding it
                        return null;
                        }
                    else if ((nMode & SEARCH_LT) != 0)
                        {
                        // looking for the node LTEQ than this key
                        return nodePrev;
                        }
                    else /*((nMode & SEARCH_GT) != 0)*/
                        {
                        // looking for the node GTEQ than this key
                        return nodeCur;
                        }
                    }
                else if (nCompare == 0)
                    {
                    // this means: nodePrev < oKey==nodeCur
                    if ((nMode & SEARCH_EQ) != 0)
                        {
                        // looking for LTEQ, EQ, or GTEQ; all of which match
                        return nodeCur;
                        }
                    else if (nMode == SEARCH_LT)
                        {
                        // looking for the node LT than this key
                        return nodePrev;
                        }
                    }
                nodePrev = nodeCur;
                nodeCur  = nodeNext;
                }
            }
        }

    /**
    * Return an EntryNode that compares strictly less than the specified key, or
    * the synthetic base node if there is none.
    * <p>
    * Note: this method is merely guaranteed to return <i>some</i> EntryNode
    *       that is less than the specified node, not necessarily the node that
    *       is immediately less than the specified node
    *
    * @param nodeTop  the top index node
    * @param oKey     the key to find a predecessor for
    *
    * @return an EntryNode that is strictly less than the specified key
    */
    protected EntryNode findPredecessor(IndexNode nodeTop, Object oKey)
        {
        return findNearestIndexedEntry(nodeTop, oKey, SEARCH_LT);
        }

    /**
    * Return the EntryNode nearest to the specified key according to the
    * specified search mode, or the synthetic base node if there is none.
    *
    * @param nodeTop  the top index node
    * @param oKey     the key to find an indexed EntryNode for
    * @param nMode    SEARCH_LT or SEARCH_LTEQ)
    *
    * @return an indexed EntryNode that is nearest to the specified key
    *         according to the specified search mode
    */
    protected EntryNode findNearestIndexedEntry(
            IndexNode nodeTop, Object oKey, int nMode)
        {
        boolean    fEqualsOk  = (nMode & SEARCH_EQ) != 0;
        Comparator comparator = comparator();

        // walk the index structure, moving to the right until a larger (or
        // equal) key is found, then move down to the next index level.
        //
        // Fix any "dangling" indices along the way that reference deleted
        // EntryNodes.
        while (true)
            {
            SkipNode nodeCur  = nodeTop;
            SkipNode nodeNext = nodeCur.getNext();

            while (true)
                {
                if (nodeNext != null)
                    {
                    EntryNode nodeEntry = nodeNext.getEntryNode();
                    if (nodeEntry.isDeleted())
                        {
                        // the EntryNode represented by this index has been
                        // deleted; try to clean up the index node
                        if (!nodeCur.casNext(nodeNext, nodeNext.getNext()))
                            {
                            // lost a race to fix the index; retry from the top
                            break;
                            }
                        nodeNext = nodeCur.getNext();
                        continue;
                        }

                    int nCompare = comparator.compare(oKey, nodeEntry.getKey());
                    if (nCompare > 0)
                        {
                        // move to the right
                        nodeCur  = nodeNext;
                        nodeNext = nodeNext.getNext();
                        continue;
                        }
                    else if (fEqualsOk && nCompare == 0)
                        {
                        // found an exact match
                        return nodeEntry;
                        }
                    }

                // move down
                SkipNode nodeBelow = nodeCur.getBelow();
                if (nodeBelow != null)
                    {
                    nodeCur  = nodeBelow;
                    nodeNext = nodeCur.getNext();
                    }
                else
                    {
                    return nodeCur.getEntryNode();
                    }
                }
            }
        }

    /**
    * compute and return an array, indexed by level, of the probability
    * thresholds used to determine what level an entry should be indexed to.
    * <p>
    * Threshold values are in the range [0, 1).  A randomly chosen value <i>v</i>
    * in that range corresponds to a level <i>l</i> iff the following condition
    * holds:
    * <i>threshold[l-1]</i> &lt;= <i>v</i> &lt; <i>threshold[l-1]</i>
    *
    * @param nLevelMax  the maximum number of levels to compute thresholds for
    * @param nSpan      the span of this map
    *
    * @return an array containing the distribution probabilities
    */
    protected static float[] computeProbabilityThresholds(int nLevelMax, int nSpan)
        {
        float[] aflProbability = new float[nLevelMax + 1];
        float   flProbability  = 1.0F;
        float   flCumulative;

        // The probability of selecting a particular level is given by:
        //   p(l) = (1/span)^(l + 1)
        //   p(0) = 1 - (sum of p(l) over l=1..MAX_LEVEL)
        //
        // For these geometric series, p(0) can be expressed as:
        //   p(0) = 1/span + (span-2)/(span-1)
        //
        if (nSpan == 2)
            {
            // special-case span==2
            //
            // testing shows that 1/2 is just too many indices to create.
            // Instead, span=2 will generate a distribution like:
            //   p(0) - 3/4   (no index)
            //   p(1) - 1/8
            //   p(2) - 1/16
            //
            flCumulative = flProbability = 0.5F;
            }
        else
            {
            flCumulative = (float) (nSpan - 2) / (nSpan - 1);
            }

        for (int i = 0; i <= nLevelMax; i++)
            {
            flProbability /= nSpan;
            flCumulative += flProbability;
            aflProbability[i] = flCumulative;
            }
        return aflProbability;
        }

    /**
    * Randomly generate a level value 0 &lt;= <i>L</i> &lt;= <tt>MAX_LEVEL</tt>, in
    * such a way that the probability <i>p(l)</i> of generating level <i>l</i>
    * is equal to <i>p(l)=(1/span)^(l+1)</i> and
    * <i>p(0)=1-(sum of p(l) over l=1..MAX_LEVEL)</i>.
    * <p>
    * For example, the probabilities (with span=2, and weight of 2) of returning
    * the following levels are:
    * 0 (no-index) - 3/4
    * 1            - 1/8
    * 2            - 1/16
    * 3            - 1/32
    * ...
    *
    * @return a random level number between 0 and <tt>MAX_LEVEL</tt>
    */
    protected int calculateRandomLevel()
        {
        int     nLevel         = 0;
        int     nLevelMax      = getMaxLevel();
        float   flRandom       = getRandom().nextFloat();
        float[] aflProbability = getProbabilityThresholds();

        while (nLevel < nLevelMax)
            {
            if (flRandom < aflProbability[nLevel])
                {
                break;
                }
            ++nLevel;
            }

        return nLevel;
        }

    /**
    * Return a human-readable description of this map's internal state.
    *
    *
    * @return a human-readable description of this map's internal state
    */
    public String dump()
        {
        StringBuilder sb        = new StringBuilder();
        IndexNode     nodeTop   = getTopNode();
        int           nLevelMax = nodeTop.getLevel();
        SkipNode[]    aNodeSkip = new SkipNode[nLevelMax + 1];

        // initialize the search list
        SkipNode nodeSkip = nodeTop;
        for (int i = nLevelMax; i > 0; i--)
            {
            aNodeSkip[i] = nodeSkip.getNext();
            nodeSkip     = nodeSkip.getBelow();
            }

        EntryNode nodeCur = firstNode();
        while ((nodeCur = nodeCur.getNext()) != null)
            {
            Object oKey   = nodeCur.getKey();
            Object oValue = nodeCur.getValue();
            int    nLevel = 1;
            while (nLevel <= nLevelMax)
                {
                SkipNode nodeIndex = aNodeSkip[nLevel];
                if (nodeCur != null && nodeIndex != null &&
                    nodeCur == nodeIndex.getEntryNode())
                    {
                    // this key is indexed
                    aNodeSkip[nLevel++] = nodeIndex.getNext();
                    }
                else
                    {
                    break;
                    }
                }
            sb.append(oKey);
            sb.append("=");
            sb.append(oValue);
            sb.append("  indexed to level (");
            sb.append(nLevel - 1);
            sb.append(")\n");
            }
        return sb.toString();
        }


    // ----- inner class: SkipNode ----------------------------------------

    /**
    * SkipNode is an entry or index node in the lattice for a SafeSortedMap's
    * representation.
    */
    protected static class SkipNode
        {
        // ----- constructors ---------------------------------------------

        /**
        * Construct a SkipNode according to the specified parameters.
        *
        * @param nodeBelow  the node "below" this, or null
        * @param nodeEntry  the EntryNode that this SkipNode serves as an
        *                   index for
        */
        protected SkipNode(SkipNode nodeBelow, EntryNode nodeEntry)
            {
            m_nodeBelow = nodeBelow;
            m_nodeEntry = nodeEntry;
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the next node in this node-list.
        *
        * @return the next node in this node-list
        */
        protected SkipNode getNext()
            {
            return m_nodeNext;
            }

        /**
        * Set the next node in this node-list.
        *
        * @param nodeNext  the next node in this node-list
        */
        protected void setNext(SkipNode nodeNext)
            {
            m_nodeNext = nodeNext;
            }

        /**
        * Atomically set the specified next SkipNode iff the current next node
        * matches the expected node.
        *
        * @param nodeAssume  the assumed "next" SkipNode
        * @param nodeNext    the new "next" SkipNode
        *
        * @return true iff the update is performed
        */
        protected boolean casNext(SkipNode nodeAssume, SkipNode nodeNext)
            {
            return m_atomicUpdaterNext.compareAndSet(this, nodeAssume, nodeNext);
            }

        /**
        * Return the node below this one in the skip-list.
        *
        * @return the next below this one in the skip-list
        */
        protected SkipNode getBelow()
            {
            return m_nodeBelow;
            }

        /**
        * Return the EntryNode below this one in the skip-list.
        *
        * @return the EntryNode below this one in the skip-list
        */
        protected EntryNode getEntryNode()
            {
            return m_nodeEntry;
            }

        // ----- data members ---------------------------------------------

        /**
        * AtomicUpdater for the casNext operation.
        */
        protected static final AtomicReferenceFieldUpdater
            m_atomicUpdaterNext = AtomicReferenceFieldUpdater.newUpdater
            (SkipNode.class, SkipNode.class, "m_nodeNext");

        /**
        * The EntryNode indexed by this node in the lattice.
        */
        protected final    EntryNode m_nodeEntry;

        /**
        * The SkipNode "below" this one in the node lattice.
        */
        protected final    SkipNode  m_nodeBelow;

        /**
        * The SkipNode "below" this one in the node lattice.
        */
        protected volatile SkipNode  m_nodeNext;
        }


    // ----- inner class: IndexNode ---------------------------------------

    /**
    * IndexNode represents the start node in the map's lattice representation
    * at a given index-level.
    */
    protected class IndexNode
            extends SkipNode
        {
        // ----- constructors ---------------------------------------------

        /**
        * Create a new IndexNode for the specified index-level.
        *
        * @param nLevel     the index level for this IndexNode
        * @param nodeBelow  the Node of the level below
        * @param nodeEntry  the base entry node
        */
        protected IndexNode(int nLevel, IndexNode nodeBelow, EntryNode nodeEntry)
            {
            super(nodeBelow, nodeEntry);
            m_nLevel = nLevel;
            }

        // ----- SkipNode methods -----------------------------------------

        /**
        * Return a human-readable description of this SkipNode.
        *
        * @return a human-readable description of this SkipNode
        */
        public String getDescription()
            {
            return "IndexLevel=" + getLevel();
            }

        // ----- internal -------------------------------------------------

        /**
        * Return the index-level that this IndexNode represents.
        *
        * @return the index-level that this IndexNode represents
        */
        protected int getLevel()
            {
            return m_nLevel;
            }

        // ----- data members ---------------------------------------------

        /**
        * The level of this IndexNode
        */
        protected final int m_nLevel;
        }


    // ----- inner class: EntryNode ---------------------------------------

    /**
    * EntryNode represents a key-value mapping in this map.
    */
    protected static class EntryNode
        {
        // ----- constructors ---------------------------------------------

        /**
        * Construct an EntryNode for the specified key and value.
        *
        * @param oKey    the key that this EntryNode represents
        * @param oValue  the value that this EntryNode represents
        */
        protected EntryNode(Object oKey, Object oValue)
            {
            m_oKey   = oKey;
            m_oValue = oValue;
            }

        /**
        * Construct an EntryNode for the specified key, value and next node.
        *
        * @param oKey      the key that this EntryNode represents
        * @param oValue    the value that this EntryNode represents
        * @param nodeNext  the node to set as the "next" node
        */
        protected EntryNode(Object oKey, Object oValue, EntryNode nodeNext)
            {
            m_oKey     = oKey;
            m_oValue   = oValue;
            m_nodeNext = nodeNext;
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return true iff this EntryNode has been deleted.
        *
        * @return true iff this EntryNode has been deleted
        */
        public boolean isDeleted()
            {
            return m_oValue == NO_VALUE;
            }

        /**
        * Attempt to mark this EntryNode for deletion by appending a delete
        * marker to follow this EntryNode, assuming that the expected nex
        * EntryNode is the next.
        *
        * @param nodeNext  the expected "next" EntryNode
        *
        * @return true if a delete marker is successfully appended
        */
        public boolean markForDelete(EntryNode nodeNext)
            {
            return casNext(nodeNext, new EntryNode(getKey(), NO_VALUE, nodeNext));
            }

        /**
        * Called when a deleted EntryNode is encountered during a map traversal
        * to help the deletion process complete.  This method attempts to finish
        * appending delete markers and removing nodes from the linked list as
        * appropriate.
        *
        * @param nodePrev  the assumed predecessor to this EntryNode
        * @param nodeNext  the assumed successor to this EntryNode
        */
        public void helpDelete(EntryNode nodePrev, EntryNode nodeNext)
            {
            if (nodeNext == getNext() && this == nodePrev.getNext())
                {
                if (nodeNext == null || !nodeNext.isDeleted())
                    {
                    // this node is not marked for deletion yet; append a marker
                    markForDelete(nodeNext);
                    }
                else
                    {
                    // nodeNext is a deletion marker; CAS it out of the way
                    nodePrev.casNext(this, nodeNext.getNext());
                    }
                }
            }

        /**
        * Return the key that this node represents.
        *
        * @return the key that this node represents
        */
        public Object getKey()
            {
            return m_oKey;
            }

        /**
        * Return the value that this node represents.
        *
        * @return the value that this node represents
        */
        public Object getValue()
            {
            Object oValue = getValueInternal();
            return oValue == NO_VALUE ? null : oValue;
            }

        /**
        * Atomically set the value of this EntryNode to the specified value iff
        * the current value matches the assumed value.
        *
        * @param oValueAssume  the assumed value
        * @param oValueNew     the new value
        *
        * @return true iff the value is successfully updated
        */
        protected boolean casValue(Object oValueAssume, Object oValueNew)
            {
            return m_atomicUpdaterValue.compareAndSet(this, oValueAssume, oValueNew);
            }

        /**
        * Return a MapEntry view of this EntryNode, suitable for returning to
        * the EntrySet iterator.
        *
        * @return a MapEntry view of this EntryNode
        */
        protected MapEntry getMapEntry()
            {
            return new MapEntry();
            }

        // ----- inner class: MapEntry ------------------------------------

        /**
        * A Map.Entry view of the EntryNode.  This implementation supports
        * updates via {#setValue}, but the behaviour is undefined in the
        * presence of concurrent removes.
        */
        protected class MapEntry
                implements Map.Entry
            {
            // ----- Map.Entry interface ----------------------------------

            /**
            * {@inheritDoc}
            */
            public Object getKey()
                {
                return EntryNode.this.getKey();
                }

            /**
            * {@inheritDoc}
            */
            public Object getValue()
                {
                return m_oValue;
                }

            /**
            * {@inheritDoc}
            */
            public Object setValue(Object oValueNew)
                {
                // results are undefined under concurrent modification.
                // Try our best, but don't do anything if the node has already
                // been deleted.
                EntryNode nodeEntry = EntryNode.this;
                Object    oValueOld;
                do
                    {
                    oValueOld = nodeEntry.getValueInternal();
                    }
                while (oValueOld != NO_VALUE &&
                       !nodeEntry.casValue(oValueOld, oValueNew));

                if (oValueOld == NO_VALUE)
                    {
                    // entry was concurrently removed
                    return null;
                    }
                else
                    {
                    // entry was successfully updated; cache the new value
                    m_oValue = oValueNew;
                    return oValueOld;
                    }
                }

            // ----- Object methods ---------------------------------------

            /**
            * {@inheritDoc}
            */
            public boolean equals(Object o)
                {
                return (o instanceof Map.Entry) &&
                    Base.equals(getKey(), ((Map.Entry) o).getKey()) &&
                    Base.equals(getValue(), ((Map.Entry) o).getValue());
                }

            /**
            * {@inheritDoc}
            */
            public int hashCode()
                {
                return Base.hashCode(getKey()) ^ Base.hashCode(getValue());
                }

            /**
            * {@inheritDoc}
            */
            public String toString()
                {
                StringBuilder sb = new StringBuilder();
                sb.append("Entry{Key=\"")
                    .append(getKey())
                    .append("\", Value=\"")
                    .append(getValue());
                sb.append("\"}");
                return sb.toString();
                }

            // ----- data members -----------------------------------------

            /**
            * The cached entry value.
            */
            protected Object m_oValue = EntryNode.this.getValue();
            }

        // ----- Object methods -------------------------------------------

        /**
        * Return a String representation of the SkipNode.
        *
        * @return a human-readable String value describing this SkipNode
        */
        public String toString()
            {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append(getClass().getName());
            sb.append(" Key=");
            sb.append(getKey());
            sb.append(", Value=");
            sb.append(getValue());
            sb.append("}");
            return sb.toString();
            }

        // ----- internal -------------------------------------------------

        /**
        * Return the value associated with the Map entry (could be NO_VALUE).
        *
        * @return the value associated with this Map entry
        */
        protected Object getValueInternal()
            {
            return m_oValue;
            }

        /**
        * Return the next EntryNode in the entry list.
        *
        * @return the next EntryNode in the entry list
        */
        protected EntryNode getNext()
            {
            return m_nodeNext;
            }

        /**
        * Set the next EntryNode in the entry list.
        *
        * @param nodeNext  the next EntryNode in the entry list
        */
        protected void setNext(EntryNode nodeNext)
            {
            m_nodeNext = nodeNext;
            }

        /**
        * Atomically set the next EntryNode to the specified node iff the current
        * next EntryNode matches the expected node.
        *
        * @param nodeAssume  the assumed "next" EntryNode
        * @param nodeNext    the new "next" EntryNode
        *
        * @return true iff the next node is updated
        */
        protected boolean casNext(Object nodeAssume, Object nodeNext)
            {
            return m_atomicUpdaterNext.compareAndSet(this, nodeAssume, nodeNext);
            }

        // ----- data members ---------------------------------------------

        /**
        * AtomicUpdater for the casNext operation.
        */
        protected static final AtomicReferenceFieldUpdater m_atomicUpdaterNext =
            AtomicReferenceFieldUpdater.newUpdater(
                EntryNode.class, EntryNode.class, "m_nodeNext");

        /**
        * AtomicUpdater for the casValue operation.
        */
        protected static final AtomicReferenceFieldUpdater m_atomicUpdaterValue =
            AtomicReferenceFieldUpdater.newUpdater(
                EntryNode.class, Object.class, "m_oValue");

        /**
        * The key represented by this Node.
        */
        protected final    Object    m_oKey;

        /**
        * The value represented by this EntryNode.  This is declared volatile
        * so read operations can be unsynchronized.
        */
        protected volatile Object    m_oValue;

        /**
        * The next SkipNode in the list.  This is declared volatile so read
        * operations can be unsynchronized.
        */
        protected volatile EntryNode m_nodeNext;
        }


    // ----- inner class: BaseEntryNode -----------------------------------

    /**
    * BaseEntryNode is a synthetic EntryNode that serves as the "head" of the
    * base entry list.  The BaseEntryNode also holds the size counter for the
    * number of entries in the base entry list (the size of the map).
    */
    protected static class BaseEntryNode
            extends EntryNode
        {
        /**
        * Construct a BaseEntryNode.
        */
        protected BaseEntryNode()
            {
            super(BASE_VALUE, BASE_VALUE);
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the AtomicLong used to track the size of the base entry list.
        *
        * @return the size counter
        */
        public AtomicInteger getSizeCounter()
            {
            return m_atomicSize;
            }

        // ----- data members ---------------------------------------------

        /**
        * AtomicCounter used to hold the size of the base entry list.  See #size().
        */
        protected final AtomicInteger m_atomicSize = new AtomicInteger();
        }


    // ----- inner class: ViewMap -----------------------------------------

    /**
    * ViewMap provides a SortedMap view over a subset of the SafeSortedMap.  A
    * ViewMap may have an inclusive lower and/or an exclusive upper bound.
    * <p>
    * For example, a ViewMap defined on an SafeSortedMap of Integer keys with
    * lower bound 3 and upper bound 7 would contain any keys in the underlying
    * SafeSortedMap in the range [3, 7).
    */
    protected class ViewMap
            extends AbstractMap
            implements SortedMap
        {
        // ----- constructors ---------------------------------------------

        /**
        * Create a new ViewMap over the SafeSortedMap bounded by the specified
        * keys, or null for no-bound.
        *
        * @param oKeyLower  the (inclusive) lower-bound key, or null
        * @param oKeyUpper  the (exclusive) upper-bound key, or null
        */
        protected ViewMap(Object oKeyLower, Object oKeyUpper)
            {
            m_oKeyLower = oKeyLower;
            m_oKeyUpper = oKeyUpper;
            }

        // ----- Map interface --------------------------------------------

        /**
        * {@inheritDoc}
        */
        public Set entrySet()
            {
            return new EntrySet();
            }

        /**
        * {@inheritDoc}
        */
        public Object put(Object oKey, Object oValue)
            {
            if (!inRange(oKey))
                {
                throw new IllegalArgumentException();
                }
            return SafeSortedMap.this.put(oKey, oValue);
            }

        /**
        * {@inheritDoc}
        */
        public Object get(Object oKey)
            {
            return inRange(oKey) ? SafeSortedMap.this.get(oKey) : null;
            }

        /**
        * {@inheritDoc}
        */
        public boolean containsKey(Object oKey)
            {
            return inRange(oKey) && SafeSortedMap.this.containsKey(oKey);
            }

        /**
        * {@inheritDoc}
        */
        public Object remove(Object oKey)
            {
            return inRange(oKey) ? SafeSortedMap.this.remove(oKey) : null;
            }

        // ----- SortedMap interface --------------------------------------

        /**
        * {@inheritDoc}
        */
        public Comparator comparator()
            {
            return SafeSortedMap.this.comparator();
            }

        /**
        * {@inheritDoc}
        */
        public SortedMap subMap(Object fromKey, Object toKey)
            {
            if (comparator().compare(fromKey, toKey) > 0 ||
                !inRange(fromKey) || !inRange(toKey))
                {
                throw new IllegalArgumentException();
                }
            return new ViewMap(fromKey, toKey);
            }

        /**
        * {@inheritDoc}
        */
        public SortedMap headMap(Object toKey)
            {
            if (!inRange(toKey))
                {
                throw new IllegalArgumentException();
                }
            return new ViewMap(getLowerBound(), toKey);
            }

        /**
        * {@inheritDoc}
        */
        public SortedMap tailMap(Object fromKey)
            {
            if (!inRange(fromKey))
                {
                throw new IllegalArgumentException();
                }
            return new ViewMap(fromKey, getUpperBound());
            }

        /**
        * {@inheritDoc}
        */
        public Object firstKey()
            {
            // Note: the map may be concurrently cleared
            EntryNode nodeFirst = firstNode();
            if (nodeFirst == null || nodeFirst.isDeleted())
                {
                throw new NoSuchElementException();
                }
            return nodeFirst.getKey();
            }

        /**
        * {@inheritDoc}
        */
        public Object lastKey()
            {
            // Note: the map may be concurrently cleared
            EntryNode nodeLast = lastNode();
            if (nodeLast == null || nodeLast.isDeleted())
                {
                throw new NoSuchElementException();
                }
            return nodeLast.getKey();
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the lower bound of this ViewMap.
        *
        * @return the lower bound of this ViewMap
        */
        protected Object getLowerBound()
            {
            return m_oKeyLower;
            }

        /**
        * Return the upper bound of this ViewMap.
        *
        * @return the upper bound of this ViewMap
        */
        protected Object getUpperBound()
            {
            return m_oKeyUpper;
            }

        // ----- internal -------------------------------------------------

        /**
        * Is the specified key in the range represented by this ViewMap?
        *
        * @param oKey  the key to test for
        *
        * @return true iff the specified key is in the range represented by
        *              this ViewMap
        */
        protected boolean inRange(Object oKey)
            {
            Object     oKeyLower  = getLowerBound();
            Object     oKeyUpper  = getUpperBound();
            Comparator comparator = comparator();

            // check that oKeyLower <= oKey < oKeyUpper
            return ((oKeyLower == null ||
                     comparator.compare(oKeyLower, oKey) <= 0) &&
                    (oKeyUpper == null ||
                     comparator.compare(oKey, oKeyUpper) < 0));
            }

        /**
        * Return the first Node in this ViewMap, or null if there are none.
        *
        * @return the first Node or null if empty, or null
        */
        protected EntryNode firstNode()
            {
            Object oKeyLower = getLowerBound();
            if (oKeyLower == null)
                {
                // no lower bound; delegate to the map
                return SafeSortedMap.this.firstNode();
                }
            else
                {
                // start the search at the lower-bound.
                return findNearest(getTopNode(), oKeyLower, SEARCH_GTEQ, true);
                }
            }

        /**
        * Return the last SkipNode in this ViewMap, or null if there are none.
        *
        * @return the last SkipNode in this ViewMap, or null
        */
        protected EntryNode lastNode()
            {
            Object oKeyUpper = getUpperBound();
            if (oKeyUpper == null)
                {
                // no upper bound; search right first at each level, then down
                return SafeSortedMap.this.lastNode();
                }
            else
                {
                // search for the node that less-than the (exclusive)
                // upper-bound
                return findNearest(getTopNode(), oKeyUpper, SEARCH_LT, true);
                }
            }

        // ----- inner class: EntrySet ----------------------------------------

        /**
        * A Set of Entries backed by this ViewMap.
        */
        protected class EntrySet
                extends AbstractSet
            {
            // ----- Set interface --------------------------------------------

            /**
            * {@inheritDoc}
            */
            public int size()
                {
                if (getLowerBound() == null && getUpperBound() == null)
                    {
                    // unbounded view
                    return SafeSortedMap.this.size();
                    }

                int cSize = 0;
                for (Iterator iter = iterator(); iter.hasNext(); iter.next())
                    {
                    cSize++;
                    }
                return cSize;
                }

            /**
            * {@inheritDoc}
            */
            public Iterator iterator()
                {
                return new EntryIterator();
                }
            }

        // ----- inner class: EntryIterator -------------------------------

        /**
        * An Iterator over the Entries backed by this ViewMap.
        */
        protected class EntryIterator
                extends AbstractStableIterator
            {
            /**
            * {@inheritDoc}
            */
            protected void advance()
                {
                Map.Entry entry    = null;
                EntryNode nodeCur  = null;
                EntryNode nodeNext = null;

                if (m_fInitialized)
                    {
                    nodeNext = m_nodeNext;
                    }
                else
                    {
                    // initialize the node cursor on the first iteration.
                    // This is done lazily (as opposed to at iterator creation
                    // time) to reduce the contention window between the
                    // iterating thread, and (potentially) intervening threads
                    // doing insertion/deletion.
                    nodeNext       = firstNode();
                    m_fInitialized = true;
                    }

                do
                    {
                    nodeCur = nodeNext;
                    if (nodeCur == null || !inRange(nodeCur.getKey()))
                        {
                        // iteration exhausted
                        return;
                        }

                    // COH-3646: Must fetch the next Map.Entry (populating the value)
                    // field before (re)-checking if the node is deleted.
                    entry    = nodeCur.getMapEntry();
                    nodeNext = nodeCur.getNext();
                    }
                while (nodeCur.isDeleted());

                m_nodeNext = nodeNext;
                setNext(entry);
                }

            /**
            * {@inheritDoc}
            */
            protected void remove(Object o)
                {
                SafeSortedMap.this.remove(((Map.Entry) o).getKey());
                }

            // ----- data members -----------------------------------------

            /**
            * The next node in the iteration, or null.
            */
            protected EntryNode m_nodeNext     = null;

            /**
            * Has this iterator been initialized yet?
            */
            protected boolean   m_fInitialized = false;
            }

        // ----- data members ---------------------------------------------

        /**
        * The key that this ViewMap is (inclusively) lower-bounded by, or null
        * for no lower-bound.
        */
        protected final Object m_oKeyLower;

        /**
        * The key that this ViewMap is (exclusively) upper-bounded by, or null
        * for no upper-bound
        */
        protected final Object m_oKeyUpper;
        }


    // ----- inner class: Split ------------------------------------------

    /**
    * Return a {@link Split} of this map at the specified key.
    *
    * @param oKey  the key at which to split the map
    *
    * @return a Split of this map at the specified key
    */
    public Split split(Object oKey)
        {
        return new Split(oKey);
        }

    /**
    * Split encapsulates the headMap and tailMap of the SafeSortedMap at a
    * given split-key and could be used as an optimization facility when
    * both head and tail portions of the SortedMap are needed.
    */
    public class Split
        {
        // ----- constructors ---------------------------------------------

        /**
        * Construct a split at the specified key.
        *
        * @param oKey the key at which to split
        */
        protected Split(Object oKey)
            {
            m_mapHead = headMap(oKey);
            m_mapTail = tailMap(oKey);
            }

        // ----- internal ------------------------------------------------

        /**
        * Calculate and return the floor of <tt>log<sub>nBase</sub>(n)</tt>.
        *
        * @param n      the number to take the logarithm of
        * @param nBase  the base with which to compute the logarithm
        *
        * @return the floor of <tt>log<sub>nBase</sub>(n)</tt>
        */
        protected int quickLog(int n, int nBase)
            {
            int nLog = 0;
            for (int i = 1; i <= n; nLog++)
                {
                i *= nBase;
                }

            return Math.max(0, nLog - 1);
            }

        /**
        * Estimate the weight of the head portion of the Split.  The weight is
        * expressed as a ratio of the size of the headMap to the size of the
        * underlying SafeSortedMap.
        *
        * @return the estimated weight of the head portion of the Split
        */
        protected float calculateHeadWeight()
            {
            // If the total size of the map is N, then we expect that for some
            // level l:
            //   p(l) = getProbabilityThresholds()[l]
            //   size(l) = N * (1 - p(l))
            //
            // To compute heuristics, we want to find a level l that contains
            // a relatively small (S=cSampleSize) number of elements.
            //   S = size(l) = N * (1 - p(l))
            //
            // therefore:
            //   S/N = 1 - p(l)
            // where:
            //   p(l) = (span-2)/(span-1) + (1/span + 1/span^2 ...  1/span^(l+1))
            //        = (span-2)/(span-1) + (1/span)(1-1/span^(l+1))/(1-1/span)
            // so:
            //   S/N = 1 - (span-2)/(span-1) - (1/span)(1-1/span^(l+1))/(1-1/span)
            // which algebraically simplifies to:
            //   l = log_span(1 / (1-span*(1 - (span-2)/(span-1) - S/N)*(1-1/span))) - 1
            //
            // See #computeProbabilityThresholds
            Object oKey        = getSplitKey();
            int    cSampleSize = 100;
            int    cEntries    = size();
            int    nLevel      = 0;
            int    nSpan       = getSpan();

            if (cEntries > cSampleSize)
                {
                nLevel = quickLog((int) (
                        1 /
                        (1 - nSpan *
                         (1 - ((float) nSpan - 2)/((float) nSpan - 1) -
                          ((float) cSampleSize/(float) cEntries)) *
                         (1 - 1/((float) nSpan))) - 1),
                        nSpan) - 1;
                }

            Comparator comparator = comparator();
            if (nLevel <= 0)
                {
                // the map is very small; just count the entries
                int       cHead   = 0;
                EntryNode nodeCur = getBaseNode().getNext();
                while (nodeCur != null)
                    {
                    if (comparator.compare(nodeCur.getKey(), oKey) > 0)
                        {
                        break;
                        }
                    ++cHead;
                    nodeCur = nodeCur.getNext();
                    }
                return (float) cHead / cEntries;
                }
            else
                {
                // count the skip nodes at a given index level
                IndexNode nodeIdx = getTopNode();
                while (nodeIdx.getLevel() > nLevel)
                    {
                    nodeIdx = (IndexNode) nodeIdx.getBelow();
                    }

                int      cHead   = 0;
                int      cTail   = 0;
                SkipNode nodeCur = nodeIdx.getNext();
                while (nodeCur != null)
                    {
                    if (cTail == 0 &&
                        comparator.compare(nodeCur.getEntryNode().getKey(), oKey) <= 0)
                        {
                        ++cHead;
                        }
                    else
                        {
                        ++cTail;
                        }

                    nodeCur = nodeCur.getNext();
                    }

                // return the head-weight as the ratio of the number of entries
                // preceding the split key to the total number of entries, at this level
                return (float) cHead / (cHead + cTail);
                }
            }

        // ----- accessors -----------------------------------------------

        /**
        * Return front of the split.  This is a SortedMap view of
        *
        * @return the front of the split
        */
        public SortedMap getHead()
            {
            return m_mapHead;
            }

        /**
        * Return tail of the split
        *
        * @return the tail of the split
        */
        public SortedMap getTail()
            {
            return m_mapTail;
            }

        /**
        * Return the estimated weight of the head portion of the {@link Split}.
        * The weight is expressed as a ratio of the size of the headMap to the
        * size of the underlying SafeSortedMap.
        * <p>
        * Note: this method is not guaranteed to return an exact answer, but
        *       rather uses a heuristic to make an estimate.  For an exact
        *       answer, the more expensive size() methods should be used.
        *
        * @return the estimated weight of the head portion of the split
        */
        public float getHeadWeight()
            {
            if (m_flHeadWeight < 0F)
                {
                m_flHeadWeight = calculateHeadWeight();
                }
            return m_flHeadWeight;
            }

        /**
        * Return the estimated weight of the tail portion of the {@link Split}.
        * The weight is expressed as a ratio of the size of the tailMap to the
        * size of the underlying SafeSortedMap.
        * <p>
        * Note: this method is not guaranteed to return an exact answer, but
        *       rather uses a heuristic to make an estimate.  For an exact
        *       answer, the more expensive size() methods should be used.
        *
        * @return the estimated weight of the tail portion of the split
        */
        public float getTailWeight()
            {
            return 1.0F - getHeadWeight();
            }

        /**
        * Return true if the estimated weight of the head of the split is
        * larger than the tail.
        *
        * @return true iff the estimated weight of the head of the split is
        *         larger than the tail
        *
        * @see #getHeadWeight
        */
        public boolean isHeadHeavy()
            {
            return getHeadWeight() > 0.5F;
            }

        /**
        * Return the key around which this is {@link Split} is defined.
        *
        * @return the key around which this is {@link Split} is defined
        */
        public Object getSplitKey()
            {
            return ((ViewMap) getHead()).getUpperBound();
            }

        // ----- data members ---------------------------------------------

        /**
        * The headMap.
        */
        protected SortedMap m_mapHead;

        /**
        * The tailMap.
        */
        protected SortedMap m_mapTail;

        /**
        * The estimated weight of the head portion of the split, expressed as
        * a ratio of the size of the headMap to the size of the underlying
        * SafeSortedMap.
        */
        protected float     m_flHeadWeight = -1.0F;
        }


    // ----- constants and data members -----------------------------------

    /**
    * The default span value.
    */
    protected static final int    DEFAULT_SPAN      = 3;

    /**
    * The default limit on the index-level.
    */
    protected static final int    DEFAULT_MAX_LEVEL = 16;

    /**
    * Search mode for equal-to.
    */
    protected static final int    SEARCH_EQ         = 0x1;

    /**
    * Search mode for strictly less-than.
    */
    protected static final int    SEARCH_LT         = 0x2;

    /**
    * Search mode for strictly greater-than.
    */
    protected static final int    SEARCH_GT         = 0x4;

    /**
    * Search mode for less-than or equal.
    */
    protected static final int    SEARCH_LTEQ       = SEARCH_EQ | SEARCH_LT;

    /**
    * Search mode for greater-than or equal.
    */
    protected static final int    SEARCH_GTEQ       = SEARCH_EQ | SEARCH_GT;

    /**
    * Placeholder for a non-existent (deleted) value.
    */
    protected static final Object NO_VALUE          = new Object();

    /**
    * Placeholder for a non-existent (deleted) value.
    */
    protected static final Object BASE_VALUE        = new Object();

    /**
    * AtomicUpdater for the casTopNode operation.
    */
    protected static final AtomicReferenceFieldUpdater m_atomicUpdaterTopNode =
        AtomicReferenceFieldUpdater.newUpdater(
            SafeSortedMap.class, IndexNode.class, "m_nodeTop");

    /**
    * The comparator used to sort this map.
    */
    protected final     Comparator    m_comparator;

    /**
    * The maximum number of index levels to use for this map.
    */
    protected final     int           m_nLevelMax;

    /**
    * The array of pre-computed probability thresholds for each level.
    */
    protected final     float[]       m_aflProbabilityThresholds;

    /**
    * The span of the map.
    */
    protected           int           m_nSpan;

    /**
    * The default ViewMap (over the entire map's range).
    */
    protected final     ViewMap       m_mapView;

    /**
    * The top index node.  This is declared volatile so read operations can be
    * unsynchronized.
    */
    protected volatile  IndexNode     m_nodeTop;

    /**
    * The random-number generator for the map.
    */
    protected          Random         m_random = new Random();
    }
