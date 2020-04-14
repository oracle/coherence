/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;


import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.Base;

import java.util.BitSet;


/**
 * OwnershipConflictResolver is used to resolve potential conflicts in
 * the partition ownership view as reported by different service members.
 * <p>
 * A set of members are "conflict-free" when:
 * <ul>
 *   <li>All members agree exactly on the primary ownership
 *   <li>No two members could both claim backup ownership
 *   <li>No members have assignments containing departed members
 * <ul>
 *
 * @author rhl 2011.03.10
 */
public class OwnershipConflictResolver
    {
    // ----- constructors -------------------------------------------------

    /**
     * Construct an OwnershipConflictResolver with the specified partition and
     * backup counts.
     *
     * @param cPartitions  the partition count
     * @param cBackups     the backup count
     */
    public OwnershipConflictResolver(int cPartitions, int cBackups)
        {
        m_cPartitions = cPartitions;
        m_cBackups    = cBackups;
        m_aView       = new OwnershipView[cPartitions * (cBackups + 1)];
        }


    // ----- accessors ----------------------------------------------------

    /**
     * Return the partition count.
     *
     * @return the partition count
     */
    public int getPartitionCount()
        {
        return m_cPartitions;
        }

    /**
     * Return the backup count.
     *
     * @return the backup count
     */
    public int getBackupCount()
        {
        return m_cBackups;
        }

    /**
     * Return the current state of the conflict resolver.
     *
     * @return the current state (one of the STATE_* constants) of the resolver
     */
    public int getState()
        {
        return m_nState;
        }

    /**
     * Set the current state of the conflict resolver.
     *
     * @param nState  the state of the resolver (one of the STATE_* constants)
     */
    protected void setState(int nState)
        {
        m_nState = nState;
        }

    // ----- OwnershipConflictResolver methods ----------------------------

    /**
     * Register the specified partition assignments, as reported by
     * the specified member.
     *
     * @param nMember    the reporting member
     * @param aaiOwners  the partition assignments
     */
    public void registerOwnership(int nMember, int[][] aaiOwners)
        {
        int             cPartitions = getPartitionCount();
        int             cBackups    = getBackupCount();
        OwnershipView[] aView       = m_aView;

        if (getState() > STATE_OPEN)
            {
            throw new IllegalStateException();
            }

        for (int iStore = 0; iStore <= cBackups; iStore++)
            {
            for (int iPart = 0; iPart < cPartitions; iPart++)
                {
                int           iView  = iPart + iStore * cPartitions;
                OwnershipView view   = aView[iView];
                int           nOwner = aaiOwners[iPart][iStore];

                while (view != null)
                    {
                    if (view.m_nOwner == nOwner)
                        {
                        view.m_bsMembers.set(nMember);
                        break;
                        }

                    view = view.m_viewNext;
                    }

                if (view == null)
                    {
                    // no matching view found
                    view = new OwnershipView(nOwner);
                    view.m_bsMembers.set(nMember);
                    view.m_viewNext = aView[iView];

                    aView[iView] = view;
                    }
                }
            }
        }

    /**
     * Analyze the reported partition assignments and return the set of
     * partitions for which there are ownership conflicts.
     *
     * @return the set of conflicted partitions
     */
    public PartitionSet resolveConflicts()
        {
        if (getState() > STATE_OPEN)
            {
            throw new IllegalStateException();
            }

        // iterate through the views for each {partition, store} and ensure that
        // the owners for each view belong to the same member set.  This ensures
        // that all implicated "owners" agree on the ownership.
        int             cPartitions   = getPartitionCount();
        int             cBackups      = getBackupCount();
        OwnershipView[] aView         = m_aView;
        PartitionSet    partsConflict = new PartitionSet(cPartitions);

        // Check that the primary assignments match exactly.
        // This is a bit stronger than absolutely necessary, but is done to
        // cover the case of disagreement on orphaned ownership.  For example
        // if the transfer (1,3)->(2,3) is completed but 2 dies before
        // publishing, the following views are possible:
        //     member 1: (0, 3)
        //     member 3: (1, 3)
        // An attempt to restore would lead member 1 to incorrectly assign
        // the partition to itself as orphaned.
        for (int iPart = 0; iPart < cPartitions; iPart++)
            {
            OwnershipView view = aView[iPart];
            if (view.m_viewNext != null)
                {
                partsConflict.add(iPart);
                }
            }

        // Check for conflicts on the backup ownership which is not as strict
        // as the primary ownership.
        // We use a 2-pass algorithm to check if there are multiple sets of
        // members, each claiming ownership.
        for (int iStore = 1; iStore <= cBackups; iStore++)
            {
            for (int iPart = 0; iPart < cPartitions; iPart++)
                {
                int           iView     = iPart + iStore * cPartitions;
                OwnershipView viewFirst = aView[iView];

                if (viewFirst.m_viewNext == null)
                    {
                    // common case: complete agreement
                    continue;
                    }

                // Step 1: merge the views, grouped by self-referring owners, or 0
                //
                // For example, if starting with views:
                //   1: {1,2}
                //   2: {4,5}
                //   3: {3,6,7}
                // for the 2nd view, since the owner (2) is not contained in the
                // reporting member-set (member 2 does not consider itself the owner),
                // merge the member set with the view that does contain member 2:
                //   1: {1,2,4,5}
                //   3: {3,6,7}
                OwnershipView view = viewFirst;
                do
                    {
                    int nOwner = view.m_nOwner;
                    if (nOwner == 0 ||                // we think nobody owns it
                        view.m_bsMembers.get(nOwner)) // self-referential
                        {
                        // nothing to do
                        }
                    else
                        {
                        // find the view that contains the current view's owner,
                        // and merge the member sets
                        OwnershipView viewTest = aView[iView];
                        do
                            {
                            if (viewTest.m_bsMembers.get(nOwner))
                                {
                                // merge the members from the current view into
                                // the found view
                                viewTest.m_bsMembers.or(view.m_bsMembers);

                                // remove the current view
                                removeView(iPart, iStore, view);
                                break;
                                }

                            viewTest = viewTest.m_viewNext;
                            }
                        while (viewTest != null);
                        }

                    view = view.m_viewNext;
                    }
                while (view != null);

                // Step 2:
                // If there is only one merged view remaining, there is no
                // conflict.  If there are multiple merged views remaining,
                // the only possibilities that are not a conflict are a single
                // self-referring view, and possibly a view with 0 as the owner.
                // Note that a self-referring view cannot refer to a dead member
                // as it implies that a response was received from that member.
                // Conversely, after merging, a non-self-referring view must
                // refer to a dead member.
                view = aView[iView];
                int cViews = 0;
                do
                    {
                    int nOwner = view.m_nOwner;
                    if (nOwner != 0)
                        {
                        if (++cViews > 1 ||                // multiple conflicting views
                            !view.m_bsMembers.get(nOwner)) // view with a dead owner
                            {
                            partsConflict.add(iPart);
                            break;
                            }
                        }

                    view = view.m_viewNext;
                    }
                while (view != null);
                }
            }

        setState(partsConflict.isEmpty() ? STATE_RESOLVED : STATE_RESOLVED_CONFLICT);

        return partsConflict;
        }

    /**
     * Return the resolved (agreed-on) partition assignments for the
     * specified partition.  The returned array of assignments is indexed
     * by partition-id, backup-index.
     *
     * @return the resolved (agreed-on) partition assignments
     */
    public int[][] getResolvedAssignments()
        {
        int             cPartitions = getPartitionCount();
        int             cBackups    = getBackupCount();
        OwnershipView[] aView       = m_aView;
        int[][]         aaiOwners   = new int[cPartitions][cBackups + 1];

        if (getState() != STATE_RESOLVED)
            {
            throw new IllegalArgumentException();
            }

        for (int iPart = 0; iPart < cPartitions; iPart++)
            {
            int[] aiOwners = aaiOwners[iPart] = new int[cBackups + 1];
            for (int iStore = 0; iStore <= cBackups; iStore++)
                {
                int           iView    = iPart + iStore * cPartitions;
                OwnershipView view     = aView[iView];
                OwnershipView viewNext = view.m_viewNext;
                int           nOwner   = view.m_nOwner;
                if (nOwner == 0 && viewNext != null)
                    {
                    // if this is the "unowned" view, use the actual
                    // (self-referring) second view.  There may be at most one
                    // self-referring view after merging (see #resolveConflicts)
                    Base.azzert(viewNext.m_viewNext == null);

                    nOwner = viewNext.m_nOwner;
                    }

                aiOwners[iStore] = nOwner;
                }
            }

        return aaiOwners;
        }


    // ----- helpers ------------------------------------------------------

    /**
     * Remove the specified OwnershipView (of {partition, store}) from the
     * list of registered views.
     *
     * @param iPartition  the partition id
     * @param iStore      the storage index
     * @param view        the view to remove
     */
    protected void removeView(int iPartition, int iStore, OwnershipView view)
        {
        int             iView   = iPartition + iStore * getPartitionCount();
        OwnershipView[] aView   = m_aView;
        OwnershipView   viewCur = aView[iView];

        if (viewCur == view)
            {
            aView[iView] = view.m_viewNext;
            return;
            }

        do
           {
           OwnershipView viewNext = viewCur.m_viewNext;
           if (viewNext == view)
               {
               viewCur.m_viewNext = view.m_viewNext;
               }

           viewCur = viewNext;
           }
        while (viewCur != null);
        }


    // ----- inner class: OwnershipView -----------------------------------

    /**
     * OwnershipView represents a view of the ownership of a given {partition,
     * store}.  The view consists of the owner of the partition storage, as well
     * as the set of members who share that view.
     * <p>
     * As an implementation optimization, OwnershipView's can also form a
     * linked-list data-structure of differing views that are seen for a given
     * partition storage.
     */
    private static class OwnershipView
        {
        /**
         * Construct a View for the specified owner.
         *
         * @param nOwner  the owner
         */
        public OwnershipView(int nOwner)
            {
            m_nOwner    = nOwner;
            m_bsMembers = new BitSet();
            }

        // ----- data members ---------------------------------------------

        /**
         * The "next" view in the linked list.
         */
        public OwnershipView m_viewNext;

        /**
         * The owner.
         */
        public final int m_nOwner;

        /**
         * A bitset representing the members that share this view.
         */
        public final BitSet m_bsMembers;
        }


    // ----- constants and data members -----------------------------------

    /**
     * The conflict resolver is still open, and accepting ownership views.
     */
    public static final int STATE_OPEN = 0;

    /**
     * The conflict resolver has been closed, and no conflicts were found.
     */
    public static final int STATE_RESOLVED = 1;

    /**
     * The conflict resolver has been closed, and conflicts were found.
     */
    public static final int STATE_RESOLVED_CONFLICT = 2;

    /**
     * The flattened array of views, indexed by iPartition + (iStore * cPartitions).
     */
    protected OwnershipView[] m_aView;

    /**
     * The partition count.
     */
    protected int m_cPartitions;

    /**
     * The backup count.
     */
    protected int m_cBackups;

    /**
     * One of the STATE_* constants.
     */
    protected int m_nState;
    }