/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.util.GridComponent;

import com.tangosol.net.CacheService.CacheAction;
import com.tangosol.net.Cluster.MemberTimeoutAction;
import com.tangosol.net.ConfigurableQuorumPolicy.MembershipQuorumPolicy.QuorumRule;
import com.tangosol.net.PartitionedService.PartitionRecoveryAction;
import com.tangosol.net.PartitionedService.PartitionedAction;
import com.tangosol.net.ProxyService.ProxyAction;

import com.tangosol.net.internal.QuorumInfo;

import com.tangosol.net.management.Registry;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.persistence.CachePersistenceHelper;
import com.tangosol.persistence.GUIDHelper;
import com.tangosol.persistence.GUIDHelper.GUIDResolver;
import com.tangosol.persistence.PersistenceManagerMBean;

import com.tangosol.util.Base;
import com.tangosol.util.LongArray;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SynchronousListener;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;


/**
 * ConfigurableQuorumPolicy provides a Quorum-based {@link ActionPolicy} for
 * services based on the cluster-configuration.
 *
 * @author rhl 2009.05.07
 * @since  Coherence 3.6
 */
public abstract class ConfigurableQuorumPolicy
        extends Base
        implements ActionPolicy
    {
    // ----- constructors --------------------------------------------------

    /**
     * Create a ConfigurableQuorumPolicy.
     */
    protected ConfigurableQuorumPolicy()
        {
        }

    // ----- accessors ----------------------------------------------------

    /**
     * Return a String that describes the current Quorum state.
     *
     * @return a String describing the allowed actions in the current state
     */
    public abstract String getStatusDescription();


    // ----- factory methods ----------------------------------------------

    /**
     * Instantiate an action policy for a PartitionedCache service.
     *
     * @param aRule                 the quorum rule to be used for this policy
     * @param provider  the address-provider for recovery addresses
     *
     * @return an action policy for a PartitionedCache service
     */
    public static PartitionedCacheQuorumPolicy instantiatePartitionedCachePolicy(
            QuorumRule[] aRule, AddressProvider provider)
        {
        return new PartitionedCacheQuorumPolicy(aRule, provider);
        }

    /**
     * Instantiate an action policy for the a proxy service.
     *
     * @param aRule  the quorum rule to be used for this policy
     *
     * @return an action policy for a Proxy service
     */
    public static ProxyQuorumPolicy instantiateProxyPolicy(QuorumRule[] aRule)
        {
        return new ProxyQuorumPolicy(aRule);
        }

    /**
     * Instantiate an action policy for the a cluster service.
     *
     * @param mapQuorum  the map of quorum count keyed by member role.
     *
     * @return an action policy for the cluster
     */
    public static ClusterQuorumPolicy instantiateClusterPolicy(Map<String, Integer> mapQuorum)
        {
        return new ClusterQuorumPolicy(mapQuorum);
        }

    // ----- inner class: MembershipQuorumPolicy --------------------------

    /**
     * MembershipQuorumPolicy is a quorum policy that is stateless and based
     * solely on service membership sizes.  MembershipQuorumPolicy uses a
     * state-machine that encodes the allowable Actions, and uses MemberEvents to
     * maintain the state-machine as the service membership changes.
     */
    public abstract static class MembershipQuorumPolicy
            extends ConfigurableQuorumPolicy
        {
        // ----- constructors ---------------------------------------------

        /**
         * Create a MembershipQuorumPolicy.
         */
        protected MembershipQuorumPolicy()
            {
            }

        // ----- accessors ------------------------------------------------

        /**
         * Return the Service which this policy applies to.
         *
         * @return the Service which this policy applies to
         */
        public Service getService()
            {
            return m_service;
            }

        /**
         * Set the service that this policy applies to.
         *
         * @param service  the Service that this policy applies to
         */
        protected void setService(Service service)
            {
            m_service = service;
            }

        /**
         * Return the current quorum rule used by the policy.
         *
         * @return the current quorum rule used by the policy
         */
        protected QuorumRule getCurrentRule()
            {
            return m_ruleCurrent;
            }

        /**
         * Return the quorum rule for the specified mask.
         *
         * @param nMask  the mask
         *
         * @return the quorum rule for the specified mask
         */
        protected QuorumRule getRule(int nMask)
            {
            QuorumRule[] aRule = getQuorumRules();

            for (QuorumRule rule : aRule)
                {
                if (rule.contains(nMask))
                    {
                    return rule;
                    }
                }

            return null;
            }

        /**
         * Set the current quorum rule used by the policy.
         *
         * @param ruleCurrent  the current quorum rule used by the policy
         */
        protected void setCurrentRule(QuorumRule ruleCurrent)
            {
            m_ruleCurrent = ruleCurrent;
            }

        /**
         * Set the quorum rules used by this policy.
         *
         * @param aRule  the quorum rules used by this policy
         */
        protected void setQuorumRules(QuorumRule[] aRule)
            {
            m_aRules = aRule;
            }

        /**
         * Return the quorum rules used by this policy.
         *
         * @return the quorum rules used by this policy
         */
        protected QuorumRule[] getQuorumRules()
            {
            return m_aRules;
            }

        /**
         * Return the set of members that are leaving the associated service
         *
         * @return the set of members that are leaving the associated service
         */
        protected Set getLeavingMembers()
            {
            return m_setLeaving;
            }

        /**
         * Calculate and return the current size of the member set that contributes
         * to the quorum for this policy domain.
         *
         * @return the current size
         */
        protected int getPolicyPopulation()
            {
            Set setMembers = getService().getInfo().getServiceMembers();
            setMembers.removeAll(getLeavingMembers());
            return setMembers.size();
            }

        // ----- internal -------------------------------------------------

        /**
         * Configure and initialize this policy with the specified quorum rules.
         *
         * @param aRule  the array of quorum rules to configure for this policy
         */
        protected void configure(QuorumRule[] aRule)
            {
            int cRules = aRule.length;
            if (cRules == 0)
                {
                // degenerate case; configure the ALL_ALLOWED rule
                setQuorumRules(new QuorumRule[] {QuorumRule.ALL_ALLOWED});
                return;
                }

            // sort the rules in the ascending order by their threshold
            Arrays.sort(aRule);

            // compose the "union" rules starting with NONE_ALLOWED
            // and discard redundant rules with the same threshold
            List<QuorumRule> listNewRules = new ArrayList<>(cRules + 2);

            QuorumRule rulePrevious = QuorumRule.NONE_ALLOWED;
            QuorumRule ruleNext     = rulePrevious;

            for (int i = 0; i < cRules; i++)
                {
                ruleNext = aRule[i].union(rulePrevious);

                if (ruleNext.getThreshold() > rulePrevious.getThreshold())
                    {
                    listNewRules.add(rulePrevious);
                    }
                rulePrevious = ruleNext;
                }
             listNewRules.add(ruleNext);

            // set the last rule to be ALL_ALLOWED
            listNewRules.add(QuorumRule.ALL_ALLOWED);

            setQuorumRules(listNewRules.toArray(new QuorumRule[listNewRules.size()]));
            setCurrentRule(QuorumRule.NONE_ALLOWED);
            }

        /**
         * Update the currently applicable quorum rule, possibly changing it to
         * reflect growth or shrinkage of the membership size.
         */
        protected void updateCurrentRule()
            {
            QuorumRule   ruleNew = null;
            int          nSize   = getPolicyPopulation();
            QuorumRule[] aRule   = getQuorumRules();

            for (int i = 0, c = aRule.length; i < c; i++)
                {
                QuorumRule ruleCurrent = aRule[i];
                if (nSize >= ruleCurrent.getThreshold())
                    {
                    ruleNew = ruleCurrent;
                    }
                else
                    {
                    break;
                    }
                }

            if (ruleNew != getCurrentRule())
                {
                setCurrentRule(ruleNew);
                }
            }


        // ----- ActionPolicy interface -----------------------------------

        /**
        * {@inheritDoc}
        */
        public void init(Service service)
            {
            setService(service);
            service.addMemberListener(instantiateMemberListener());

            updateCurrentRule();
            }


        // ----- Object methods -------------------------------------------

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return "{" + getClass().getName() + " " + getStatusDescription() + "}";
            }

        // ----- inner class: QuorumListener ------------------------------

        /**
         * Instantiate a member listener to subscribe to service membership events.
         *
         * @return a member listener
         */
        protected MemberListener instantiateMemberListener()
            {
            return new QuorumListener();
            }

        /**
         * QuorumListener is used to subscribe the quorum policy to receive service
         * membership events.
         */
        protected class QuorumListener
                implements MemberListener, SynchronousListener
            {
            /**
            * {@inheritDoc}
            */
            public void memberJoined(MemberEvent evt)
                {
                updateCurrentRule();
                }

            /**
            * {@inheritDoc}
            */
            public void memberLeaving(MemberEvent evt)
                {
                getLeavingMembers().add(evt.getMember());
                updateCurrentRule();
                }

            /**
            * {@inheritDoc}
            */
            public void memberLeft(MemberEvent evt)
                {
                getLeavingMembers().remove(evt.getMember());
                updateCurrentRule();
                }
            }

        /**
         * A quorum rule defines a set of allowable actions beyond the rule's
         * threshold size.
         */
        public static class QuorumRule
                implements Comparable<QuorumRule>
            {
            /**
             * Construct a state with the specified threshold and numeric
             * representation.
             *
             * @param nRuleMask   numeric representation of the state
             * @param nThreshold  the size threshold of the state
             */
            public QuorumRule(int nRuleMask, int nThreshold)
                {
                this(nRuleMask, nThreshold, 0.0f);
                }

            /**
             * Construct a state with the specified threshold and numeric
             * representation.
             *
             * @param nRuleMask       numeric representation of the state
             * @param nThreshold      the size threshold of the state
             * @param flThresholdPct  the size of threshold of the state in percentage
             */
            public QuorumRule(int nRuleMask, int nThreshold, float flThresholdPct)
                {
                setRuleMask(nRuleMask);
                setThreshold(nThreshold);
                setThresholdFactor(flThresholdPct);
                }

            /**
             * Compare this Rule to another one based on the {@link #getThreshold()
             * threshold}.
             */
            @Override
            public int compareTo(QuorumRule that)
                {
                return this.getThreshold() - that.getThreshold();
                }

            // ----- Object methods ---------------------------------------

            @Override
            public String toString()
                {
                return "QuorumRule {threshold=" + getThreshold() + ", rule mask=" + getRuleMask() + "}";
                }

            // ----- internal ---------------------------------------------

            /**
             * Return true if the current rule contains the specified action mask.
             *
             * @param nMask  the action bitmask to test for
             *
             * @return true if the current rule contains the specified action mask
             */
            protected boolean contains(int nMask)
                {
                return (getRuleMask() & nMask) != 0;
                }

            /**
             * Return a quorum rule composed from this and the specified rule that
             * reflects the "union" of the two rules.  The union of two rules <i>A</i>
             * and <i>B</i> requires a membership threshold that is
             * <i>max(A.getThreshold(), A.getThreshold())</i> and allows all actions
             * allowed by <i>A</i> or <i>B</i>.
             *
             * @param rule  the rule to compute the union of this with
             *
             * @return a quorum rule representing the union with the specified rule
             */
            protected QuorumRule union(QuorumRule rule)
                {
                return new QuorumRule(getRuleMask() | rule.getRuleMask(),
                                      Math.max(getThreshold(), rule.getThreshold()),
                                      Math.max(getThresholdFactor(), rule.m_flThresholdPct));
                }


            // ----- accessors --------------------------------------------

            /**
             * Return the numeric representation of the actions allowed by this rule.
             *
             * @return the numeric representation of this rule
             */
            protected int getRuleMask()
                {
                return m_nRuleMask;
                }

            /**
             * Set the numeric representation of the actions allowed by this rule.
             *
             * @param nRuleMask  the numeric representation of this rule
             */
            protected void setRuleMask(int nRuleMask)
                {
                m_nRuleMask = nRuleMask;
                }

            /**
             * Return the size threshold for this rule.
             *
             * @return the size threshold for this rule
             */
            protected int getThreshold()
                {
                return m_nThreshold;
                }

            /**
             * Set the size threshold for this rule.
             *
             * @param nThreshold  the size threshold for this rule
             */
            protected void setThreshold(int nThreshold)
                {
                m_nThreshold = nThreshold;
                }

            /**
             * Return the percentage threshold for this rule.
             *
             * @return the percentage threshold for this rule
             */
            protected float getThresholdFactor()
                {
                return m_flThresholdPct;
                }

            /**
             * Set the percentage threshold for this rule.
             *
             * @param flThreshold  the percentage threshold for this rule
             */
            protected void setThresholdFactor(float flThreshold)
                {
                m_flThresholdPct = flThreshold;
                }

            // ----- data members -----------------------------------------

            /**
             * The size threshold for this state.
             */
            private int m_nThreshold;

            /**
             * The percentage of the quorum for this state.
             */
            private float m_flThresholdPct;

            /**
             * A numeric representation of the actions allowed by this state.
             */
            private int m_nRuleMask;

            /**
            * A QuorumRule that rejects all actions.
            */
            protected static final QuorumRule NONE_ALLOWED = new QuorumRule(0, 0);

            /**
            * A QuorumRule that permits all actions.
            */
            protected static final QuorumRule ALL_ALLOWED  = new QuorumRule(0xFFFFFFFF, 0);
            }

        // ----- data members ---------------------------------------------

        /**
         * The set of leaving members.
         */
        protected Set m_setLeaving = new HashSet();

        /**
         * The Service that this policy applies to.
         */
        protected Service m_service;

        /**
         * The current state.
         */
        protected QuorumRule m_ruleCurrent;

        /**
         * The array of quorum rules.
         */
        protected QuorumRule[] m_aRules;
        }

    // ----- inner class: PartitionedCacheQuorumPolicy --------------------

    /**
     * PartitionedCacheQuorumPolicy defines a configurable quorum policy that is
     * applicable to a DistributedCacheService.
     */
    public static class PartitionedCacheQuorumPolicy
            extends MembershipQuorumPolicy
        {
        // ----- constructors ---------------------------------------------

        /**
         * Construct a PartitionedCacheQuorumPolicy with the specified rule.
         *
         * @param aRule     the quorum rule
         * @param provider  the recovery address-provider
         */
        public PartitionedCacheQuorumPolicy(QuorumRule[] aRule, AddressProvider provider)
            {
            configure(aRule);

            // all the rules are "unions" now and sorted by the threshold;
            // if the first one doesn't have the RECOVER limitations
            // then none of them do
            m_apRecovery = provider;
            m_fDynamic   = provider == null;
            }

        // ----- helpers --------------------------------------------------

        /**
         * Return the set of ownership-enabled members in the associated
         * PartitionedService.
         *
         * @return the set of ownership-enabled members
         */
        protected Set getOwnershipMemberSet()
            {
            return getService().getOwnershipEnabledMembers();
            }

        /**
         * Return the associated PartitionedService.
         *
         * @return the associated PartitionedService
         */
        @Override
        public PartitionedService getService()
            {
            return (PartitionedService) super.getService();
            }

        /**
         * Check if the recovery is allowed for the current membership.
         * <br>
         * If the address provider is specified, it means that all the recovery
         * addresses are represented by the ownership-enabled members.
         * <br>
         * For dynamic active persistence strategy it means that:
         * <ul>
         *  <li> Global partition (partition 0) is recoverable.
         *  <li> All machines, derived from the global partition, are present.
         *  <li> All other partitions are accessible / recoverable across the service members.
         *  <li> The number of members is larger than the threshold, which is calculated as
         *       a factor of the "last well-formed" member set size
         * </ul>
         *
         * <b>Note: the implementation *MUST NOT* mutate the action's state
         *    (including the resolver's)</b>
         *
         * @return null if the recovery is allowed; otherwise a list of rejection reasons
         */
        protected List<Notification> checkRecoveryMembership(PartitionRecoveryAction action)
            {
            List<Notification> listReasons  = null;
            QuorumInfo         info         = action.getQuorumInfo();
            GUIDResolver       resolver     = action.getResolver();
            PartitionSet       partsRecover = action.getOrphanedPartitions();
            PartitionSet       partsMissing = resolver.getUnresolvedPartitions();

            if (m_fDynamic)
                {
                Set<Member>  setLast = info == null ? null : info.getMembers();

                if (setLast == null)
                    {
                    // there is no info available; this could be due to
                    //  a) completely fresh deployment, or on-demand persistence
                    //  b) missing global partition(s)
                    //  c) an old persistence version
                    //  d) global partition being transferred while experiencing partition loss

                    if (partsMissing.isFull())
                        {
                        // everything is missing; must be the case (a) - allow
                        return null;
                        }

                    if (partsMissing.isEmpty())
                        {
                        // everything is recoverable; must be the case (c) - allow
                        return null;
                        }

                    if (!partsRecover.isFull() && !partsMissing.intersects(partsRecover))
                        {
                        // everything is recoverable; must be case (d) - allow
                        return null;
                        }

                    return addReason(listReasons, "Unreachable quorum info " + partsMissing,
                        "recovery of " + partsRecover + " is disallowed");
                    }

                if (partsMissing.intersects(partsRecover))
                    {
                    partsMissing = new PartitionSet(partsMissing);
                    partsMissing.retain(partsRecover);

                    listReasons = addReason(listReasons, "Unreachable " + partsMissing,
                        reportLastOwnership(partsMissing, info));
                    }

                // check the existing storage versions
                PartitionSet partsStale = getStalePartitions(action, partsRecover, partsMissing);
                if (!partsStale.isEmpty())
                    {
                    listReasons = addReason(listReasons,
                        "Stale storage versions for " + partsStale,
                        reportLastOwnership(partsStale, info));
                    }

                // all the partitions are recoverable; make sure we have
                // enough capacity based on the "last good" membership;
                // (we definitely don't need more nodes than there are partitions)

                int        cLast       = setLast.size();
                int        cParts      = partsRecover.getPartitionCount();
                int        cCurrent    = getOwnershipMemberSet().size();
                QuorumRule ruleRecover = getRule(MASK_RECOVER);
                float      fThreshold  = ruleRecover.getThresholdFactor();
                int        cMinimum    = Math.min(calculateMinThreshold(cLast, fThreshold), cParts);

                int cQuorum = ruleRecover == null ? 0 : ruleRecover.getThreshold();
                if (cQuorum > 0)
                    {
                    cMinimum = cQuorum;
                    }

                if (cCurrent < cMinimum)
                    {
                    listReasons = addReason(listReasons, "Insufficient capacity",
                            cQuorum == 0 ? "the last known ownership size was " + cLast : "" +
                                    ", need at least " + cMinimum + " nodes to recover");
                    }
                else if (!resolver.isSharedStorage())
                    {
                    // to make sure all the machines have enough nodes to recover
                    // we simply calculate the number of nodes on a minimally
                    // loaded machine; it has to follow the same "two-thirds" rule
                    // the total number of nodes

                    // calculate the minimum node count per machine for the
                    // "last good" distribution
                    int cMinLast = calculateMinimumNodeCount(info.getMembers());

                    // calculate the minimum node count per machine now
                    int cMinCurrent = calculateMinimumNodeCount(
                            getService().getOwnershipEnabledMembers());

                    int cMinThreshold = calculateMinThreshold(cMinLast, fThreshold);

                    if (cMinCurrent < cMinThreshold)
                        {
                        listReasons = addReason(listReasons, "Insufficient minimum capacity",
                            "the last known distribution had " + cMinLast +
                            " nodes on the least loaded machine, current minimum is " + cMinCurrent);
                        }
                    }
                }
            else
                {
                AddressProvider provider = m_apRecovery;
                if (provider != null)
                    {
                    Set setAddresses = new HashSet();
                    for (Iterator iter = getOwnershipMemberSet().iterator(); iter.hasNext(); )
                        {
                        Member member = (Member) iter.next();
                        setAddresses.add(member.getAddress().getHostAddress());
                        }

                    for (InetSocketAddress address = provider.getNextAddress();
                          address != null; address = provider.getNextAddress())
                        {
                        String sAddress = address.getAddress().getHostAddress();
                        if (!setAddresses.contains(sAddress))
                            {
                            if (m_fLogged)
                                {
                                listReasons = addReason(listReasons, "Address in <recovery-hosts> is not present: " + sAddress, "");
                                }
                            else
                                {
                                listReasons = addReason(listReasons, "Address in <recovery-hosts> is not present: " + sAddress,
                                        "Persistence recovery will be deferred until a member from the missing host(s) joins the service.\n" +
                                               "To commence recovery regardless of the missing hosts use the forceRecovery operation on the PersistenceManagerMBean.");
                                m_fLogged = true;
                                }
                            }
                        }
                    }

                if (listReasons == null)
                    {
                    // all partitions must be accessible before recover
                    if (!partsMissing.isFull() && !partsMissing.isEmpty() &&
                            (partsMissing.intersects(partsRecover)))
                        {
                        partsMissing = new PartitionSet(partsMissing);
                        partsMissing.retain(partsRecover);

                        listReasons = addReason(listReasons, "Unreachable " + partsMissing,
                                        reportLastOwnership(partsMissing, info));
                        }

                    // check the existing storage versions
                    PartitionSet partsStale = getStalePartitions(action, partsRecover, partsMissing);
                    if (!partsStale.isEmpty())
                        {
                        listReasons = addReason(listReasons,
                                                "Stale storage versions for " + partsStale,
                                        reportLastOwnership(partsStale, info));
                        }
                    }
                }

            return listReasons;
            }

        /**
         * Return staled partition set.
         *
         * @param action        the recovery action
         * @param partsRecover  the partitions to be recovered
         * @param partsMissing  the partitions that are missing
         *
         * @return the staled partitions
         */
        protected PartitionSet getStalePartitions(PartitionRecoveryAction action, PartitionSet partsRecover, PartitionSet partsMissing)
            {
            QuorumInfo   info       = action.getQuorumInfo();
            GUIDResolver resolver   = action.getResolver();
            PartitionSet partsStale = new PartitionSet(partsRecover);
            partsStale.remove(partsMissing);

            for (int iPart = partsStale.next(0); iPart >= 0;
                    iPart = partsStale.next(iPart + 1))
                {
                int nVersionLast = info == null ? 0 : info.getVersions()[iPart];
                if (nVersionLast > 0)
                    {
                    int nVersionPresent = (int) GUIDHelper.getVersion(
                            resolver.getNewestGUID(iPart));

                    if (nVersionLast > nVersionPresent)
                        {
                        // the existing version is stale
                        continue;
                        }
                    }

                // the partition is recoverable
                partsStale.remove(iPart);
                }

            return partsStale;
            }

        /**
         * Calculate the minimum number of storage enabled nodes necessary to
         * proceed with the service recovery.
         *
         * @param cLast  the "last well-formed" member set size
         * @param flPct  the percentage of "last well-formed" member set size
         *
         * @return the number of nodes necessary for the recovery to commence;
         *         the default implementation calculates it as "2/3" of the last
         *         "well-formed" member set size.
         */
        protected int calculateMinThreshold(int cLast, float flPct)
            {
            return (int) ( flPct > 0.0f
                    ? cLast * flPct
                    : cLast * 2/3);
            }

        /**
         * Add a reason to the provided list (could be null).
         */
        private static List<Notification> addReason(
                List<Notification> list, String sMessage, String sData)
            {
            if (list == null)
                {
                list = new LinkedList<>();
                }
            list.add(new Notification(sMessage, sData));

            return list;
            }

        /**
         * Report the machine names in the "last good" membership that owned
         * the specified partitions.
         *
         * @param parts  the partitions to report
         * @param info   the QuorumInfo containing the "last good" membership data
         *
         * @return the human readable string with the machine names
         */
        protected static String reportLastOwnership(PartitionSet parts, QuorumInfo info)
            {
            if (info == null)
                {
                return "";
                }

            LongArray<Member> laMembers   = info.getMemberArray();
            int[]             anOwner     = info.getOwners();
            Set<Member>       setMembers  = new HashSet<>();

            for (int iPart = parts.next(0); iPart >= 0; iPart = parts.next(iPart + 1))
                {
                int nOwner = anOwner[iPart];
                if (nOwner > 0)
                    {
                    setMembers.add(laMembers.get(nOwner));
                    }
                }

            Set<String> setReported = new HashSet<>(); // reported machines

            StringBuilder sb = new StringBuilder("last known locations:");
            for (Member member : setMembers)
                {
                String sMachine = member.getMachineName();
                if (sMachine == null)
                    {
                    sMachine = member.getAddress().toString();
                    }

                if (setReported.add(sMachine))
                    {
                    sb.append(' ');

                    String sAddress = member.getAddress().toString();
                    if (sAddress.contains(sMachine))
                        {
                        sb.append(sAddress);
                        }
                    else
                        {
                        sb.append(sMachine).append(" at ")
                          .append(sAddress);
                        }
                    sb.append(',');
                    }
                }
            return setReported.isEmpty() ? "" : sb.substring(0, sb.length() - 1);
            }

        /**
         * Given a set of Member objects, calculate the minimum number of nodes
         * on a single machine.
         *
         * @param setMembers  the member set
         *
         * @return the minimum number of nodes for a machine
         */
        protected static int calculateMinimumNodeCount(Set<Member> setMembers)
            {
            // it's best to use the "machine-name" attribute to split members
            // across different machines, but we have to rely on the manually
            // configured or automatic generation of that attribute.
            // If any of the machine-name attributes are missing, we'll revert
            // to using the "address" instead.

            Map<Object, Integer> mapCount = new HashMap<>();
            for (Member member : setMembers)
                {
                String sName = member.getMachineName();
                if (sName == null || sName.isEmpty())
                    {
                    // the machine-name attribute is missing; go to the plan B
                    mapCount.clear();
                    break;
                    }

                Integer ICount = mapCount.get(sName);
                mapCount.put(sName, ICount == null ? 1 : ICount + 1);
                }

            if (mapCount.isEmpty())
                {
                for (Member member : setMembers)
                    {
                    InetAddress address = member.getAddress();

                    Integer ICount = mapCount.get(address);
                    mapCount.put(address, ICount == null ? 1 : ICount + 1);
                    }
                }

            int cMin = Integer.MAX_VALUE;
            for (Integer C : mapCount.values())
                {
                cMin = Math.min(cMin, C.intValue());
                }

            return cMin;
            }

        // ----- ConfigurableQuorumPolicy methods -------------------------

        /**
        * {@inheritDoc}
        */
        public String getStatusDescription()
            {
            return "allowed-actions=" +
                getActionName(getCurrentRule().getRuleMask());
            }

        /**
        * {@inheritDoc}
        * <p>
        * Note: The quorum for PartitionedService is determined by the
        *       ownership-enabled members only.
        */
        public int getPolicyPopulation()
            {
            Set setOwners = getOwnershipMemberSet();
            setOwners.removeAll(getLeavingMembers());
            return setOwners.size();
            }

        /**
        * {@inheritDoc}
        */
        public boolean isAllowed(Service service, Action action)
            {
            int                nMask       = 0;
            List<Notification> listReasons = null;
            QuorumRule         ruleCurrent = getCurrentRule();

            if (action == CacheAction.READ)
                {
                nMask = MASK_READ;
                }
            else if (action == CacheAction.WRITE)
                {
                nMask = MASK_WRITE;
                }
            else if (action == PartitionedAction.DISTRIBUTE)
                {
                nMask = MASK_DISTRIBUTION;
                }
            else if (action == PartitionedAction.RESTORE)
                {
                nMask = MASK_RESTORE;
                }
            else if (action instanceof PartitionRecoveryAction)
                {
                nMask       = MASK_RECOVER;
                listReasons = checkRecoveryMembership((PartitionRecoveryAction) action);

                if (listReasons != null)
                    {
                    ruleCurrent = QuorumRule.NONE_ALLOWED;
                    }
                }

            if (nMask > 0 && !ruleCurrent.contains(nMask))
                {
                reportDisallowedAction(nMask, listReasons);
                return false;
                }

            // either action is allowed based on the current rule or an
            // unrecognized action type; we are supposed to allow it
            return true;
            }

        @Override
        protected void updateCurrentRule()
            {
            super.updateCurrentRule();

            m_ldtLastReport = 0L;
            }

        /**
         * Report all disallowed actions if the quorum rule has changed.
         *
         * @param nMaskDisallowed  a mask of the disallowed action
         * @param listReasons      (optional) a list of rejection reasons
         */
        protected void reportDisallowedAction(int nMaskDisallowed, List<Notification> listReasons)
            {
            long ldtNow = getLastSafeTimeMillis();

            if (m_ldtLastReport > ldtNow - 60_000)
                {
                return;
                }

            QuorumRule ruleCurrent = getCurrentRule();

            StringBuilder sb = new StringBuilder("Action \"")
                .append(getActionName(nMaskDisallowed))
                .append("\" disallowed");

            if (ruleCurrent.compareTo(QuorumRule.ALL_ALLOWED) != 0)
                {
                sb.append("; all-disallowed-actions: ")
                    .append(getActionName(~ruleCurrent.getRuleMask()));
                }

            if (listReasons != null)
                {
                GridComponent _service = (GridComponent) getService();
                Registry      registry = _service.getCluster().getManagement();

                String sMBean = CachePersistenceHelper.getMBeanName(
                        _service.getInfo().getServiceName());

                for (Notification note : listReasons)
                    {
                    sb.append(":\n")
                        .append(note.Message)
                        .append(" - ")
                        .append(note.UserData);

                    if (registry != null)
                        {
                        _service.dispatchNotification(registry.ensureGlobalName(sMBean),
                            PersistenceManagerMBean.RECOVER_DISALLOWED,
                            note.Message, note.UserData);
                        }
                    }
                }

            Logger.warn(sb.toString());

            m_ldtLastReport = ldtNow;
            }

        /**
         * Return the string description of the given rule mask.
         *
         * @param nMask  the bitmask
         *
         * @return the string description of the given rule mask
         */
        protected String getActionName(int nMask)
            {
            StringBuilder sb = new StringBuilder();

            for (int nBit = 1; nBit <= MASK_LAST; nBit <<= 1)
                {
                if ((nMask & nBit) != 0)
                    {
                    switch (nBit)
                        {
                        case MASK_DISTRIBUTION:
                            sb.append(", distribution");
                            break;
                        case MASK_RESTORE:
                            sb.append(", restore");
                            break;
                        case MASK_RECOVER:
                            sb.append(", recover");
                            break;
                        case MASK_READ:
                            sb.append(", read");
                            break;
                        case MASK_WRITE:
                            sb.append(", write");
                            break;
                        }
                    }
                }
            return sb.length() == 0 ? sb.toString() : sb.substring(2);
            }


        // ----- inner classes --------------------------------------------

        /**
         * Notification is a simple struct carrying the notification info.
         */
        public static class Notification
            {
            public Notification(String sMessage, String sUserData)
                {
                Message  = sMessage;
                UserData = sUserData;
                }
            public final String Message;
            public final String UserData;
            }

        // ----- constants and data members -------------------------------

        /**
         * Action Rules for {@link PartitionedCacheQuorumPolicy}
         */
        public enum ActionRule
            {
            DISTRIBUTION(MASK_DISTRIBUTION),
            RESTORE(MASK_RESTORE),
            RECOVER(MASK_RECOVER),
            READ(MASK_READ),
            WRITE(MASK_WRITE);

            ActionRule(int nMask)
                {
                m_nMask        = nMask;
                m_sElementName = this.name().toLowerCase() + "-quorum";
                }

            /**
             * Get the configuration element name for this {@link ActionRule}.
             *
             * @return the action-element-name for this action.
             */
            public String getElementName()
                {
                return m_sElementName;
                }

            /**
             * Get the mask for this {@link ActionRule}.
             *
             * @return the mask for this action.
             */
            public int getMask()
                {
                return m_nMask;
                }

            private final int    m_nMask;
            private final String m_sElementName;
            }

        /**
         * Bitmask used to encode a distribution action.
         */
        public static final int MASK_DISTRIBUTION = 0x1;

        /**
         * Bitmask used to encode a partition restore action.
         */
        public static final int MASK_RESTORE = 0x2;

        /**
         * Bitmask used to encode a read action.
         */
        public static final int MASK_READ = 0x4;

        /**
         * Bitmask used to encode a write action.
         */
        public static final int MASK_WRITE = 0x8;

        /**
         * Bitmask used to encode a recover action.
         */
        public static final int MASK_RECOVER = 0x10;

        /**
         * The highest used bitmask value.
         */
        protected static final int MASK_LAST = MASK_RECOVER;

        /**
         * The recovery address-provider.
         */
        protected AddressProvider m_apRecovery;

        /**
         * The last time disallowed actions were reported.
         */
        protected long m_ldtLastReport;

        /**
         * If set to true, indicates a dynamic active recovery strategy.
         */
        protected boolean m_fDynamic;

        /**
         * Only log once.
         */
        private boolean m_fLogged = false;
        }


    // ----- inner class: ProxyQuorumPolicy -------------------------------

    /**
     * ProxyQuorumPolicy defines a configurable quorum policy that is applicable
     * to a proxy service.
     */
    public static class ProxyQuorumPolicy
            extends MembershipQuorumPolicy
        {
        // ----- constructors ---------------------------------------------

        /**
         * Construct a ProxyQuorumPolicy with the specified rule.
         *
         * @param aRule the quorum rule
         */
        protected ProxyQuorumPolicy(QuorumRule[] aRule)
            {
            configure(aRule);
            }


        // ----- internal -------------------------------------------------

        /**
        * {@inheritDoc}
        */
        public String getStatusDescription()
            {
            int           nMask    = getCurrentRule().getRuleMask();
            StringBuilder sbStatus = new StringBuilder();

            sbStatus.append("allowed-actions=");
            if ((nMask & MASK_CONNECT) != 0)
                {
                sbStatus.append("connect");
                }

            return sbStatus.toString();
            }

        // ----- ConfigurableQuorumPolicy methods -------------------------

        /**
        * {@inheritDoc}
        */
        public boolean isAllowed(Service service, Action action)
            {
            QuorumRule ruleCurrent = getCurrentRule();
            if (ruleCurrent == QuorumRule.ALL_ALLOWED)
                {
                // short-circuit the common case
                return true;
                }

            if (action == ProxyAction.CONNECT)
                {
                return ruleCurrent.contains(MASK_CONNECT);
                }

            // unrecognized action type; we are supposed to allow it
            return true;
            }


        // ----- constants ------------------------------------------------

        /**
         * Bitmask used to encode a client connection action.
         */
        public static final int MASK_CONNECT = 0x1;
        }


    // ----- inner class: ClusterQuorumPolicy -----------------------------

    /**
    * ClusterQuorumPolicy defines an action policy that is applicable to the
    * cluster.
     */
    public static class ClusterQuorumPolicy
            extends ConfigurableQuorumPolicy
            implements ActionPolicy
        {
        /**
        * Construct a ClusterQuorumPolicy with the specified quorum map.
        *
        * @param mapQuorum cluster quorum map
        */
        protected ClusterQuorumPolicy(Map<String, Integer>mapQuorum)
            {
            setClusterQuorumMap(mapQuorum);
            }

        /**
        * Return the cluster quorum map, keyed by role name.
         *
        * @return the cluster quorum map
         */
        protected Map<String, Integer> getClusterQuorumMap()
            {
            return m_mapQuorumByRole;
            }

        /**
        * Set the cluster quorum map, keyed by role name.
        *
        * @param mapQuorumByRole  the cluster quorum map
        */
        protected void setClusterQuorumMap(Map mapQuorumByRole)
            {
            m_mapQuorumByRole = mapQuorumByRole;
            }

        /**
        * Return the Cluster service.
        *
        * @return the Cluster service
        */
        public Service getService()
            {
            return m_service;
            }

        /**
        * Set the Cluster service.
         *
        * @param service  the Cluster service
         */
        public void setService(Service service)
            {
            m_service = service;
            }

        /**
        * {@inheritDoc}
        */
        public String getStatusDescription()
            {
            StringBuilder sb = new StringBuilder();
            sb.append("thresholds: {");
            for (Iterator iter = getClusterQuorumMap().entrySet().iterator(); iter.hasNext(); )
                {
                Entry   entry   = (Entry) iter.next();
                String  sRole   = (String) entry.getKey();
                Integer IQuorum = (Integer) entry.getValue();

                sb.append(equals(sRole, ROLE_ALL) ? "*" : "\"" + sRole + "\"");
                sb.append("=").append(IQuorum);
                if (iter.hasNext())
                    {
                    sb.append(",");
                    }
                }
            sb.append("}");
            return sb.toString();
            }


        // ----- internal -------------------------------------------------

        /**
        * Return the specified set, or an empty set if null.
        *
        * @param set the set to ensure, or null
        *
        * @return the specified set, or an empty set if null
        */
        protected Set ensureSet(Set set)
            {
            return set == null ? NullImplementation.getSet() : set;
            }

        /**
        * Group the specified set of Members by their roles.
        *
        * @param setMembers the set of Members
        *
        * @return a map, keyed by role name, of the subsets of Members in that role
        */
        protected Map<String, Set<Member>> groupMembersByRole(Set<Member> setMembers)
            {
            Map<String, Set<Member>> mapByRole = new HashMap();

            mapByRole.put(ROLE_ALL, setMembers);
            for (Member member : setMembers)
                {
                for (String sRole : member.getRoles())
                    {
                    Set<Member> setRoleMember = mapByRole.get(sRole);
                    if (setRoleMember == null)
                        {
                        setRoleMember = new HashSet<>();
                        mapByRole.put(sRole, setRoleMember);
                        }
                    setRoleMember.add(member);
                    }
                }

            return mapByRole;
            }

        /**
        * Check whether the site quorum for the specified role will be
        * satisfied if the set of suspect members is disconnected from the
        * cluster, given the sets of "healthy" and "announcing" members.
        *
        * @param setMembers     the set of cluster members in the specified role
        * @param setTimedOut    the subset of cluster members in the specified
        *                       role that are timed-out.  A member is considered
        *                       timed-out if it has not responded to some
        *                       network communications within the configured
        *                       timeout, and it has been selected for
        *                       termination
        * @param setHealthy     the subset of cluster members in the specified
        *                       role that are known to be "healthy".  A member
        *                       is "healthy" if it has been "recently" heard from
        * @param setAnnouncing  the set of potential new cluster members in the
        *                       specified role that are announcing their
        *                       presence and waiting to join the cluster
        *
        * @return true iff the site quorum for the specified role would be
        *         preserved if the specified timed-out members were disconnected
        */
        protected boolean checkSiteQuorum(int cQuorum, Set<Member> setMembers,
                Set<Member> setTimedOut, Set<Member> setHealthy, Set<Member> setAnnouncing)
            {
            return setHealthy.stream().map(MemberIdentity::getSiteName).
                filter(Objects::nonNull).distinct().count() >= cQuorum;
            }

        /**
        * Check whether the machine quorum for the specified role will be
        * satisfied if the set of suspect members is disconnected from the
        * cluster, given the sets of "healthy" and "announcing" members.
        *
        * @param setMembers     the set of cluster members in the specified role
        * @param setTimedOut    the subset of cluster members in the specified
        *                       role that are timed-out.  A member is considered
        *                       timed-out if it has not responded to some
        *                       network communications within the configured
        *                       timeout, and it has been selected for
        *                       termination
        * @param setHealthy     the subset of cluster members in the specified
        *                       role that are known to be "healthy".  A member
        *                       is "healthy" if it has been "recently" heard from
        * @param setAnnouncing  the set of potential new cluster members in the
        *                       specified role that are announcing their
        *                       presence and waiting to join the cluster
        *
        * @return true iff the site quorum for the specified role would be
        *         preserved if the specified timed-out members were disconnected
        */
        protected boolean checkMachineQuorum(int cQuorum, Set<Member> setMembers,
                Set<Member> setTimedOut, Set<Member> setHealthy, Set<Member> setAnnouncing)
            {
            return setHealthy.stream().map(MemberIdentity::getMachineName).
                filter(Objects::nonNull).distinct().count() >= cQuorum;
            }

        /**
        * Check whether the member quorum for the specified role will be
        * satisfied if the set of suspect members is disconnected from the
        * cluster, given the sets of "healthy" and "announcing" members.
        *
        * @param setMembers     the set of cluster members in the specified role
        * @param setTimedOut    the subset of cluster members in the specified
        *                       role that are timed-out.  A member is considered
        *                       timed-out if it has not responded to some
        *                       network communications within the configured
        *                       timeout, and it has been selected for
        *                       termination
        * @param setHealthy     the subset of cluster members in the specified
        *                       role that are known to be "healthy".  A member
        *                       is "healthy" if it has been "recently" heard from
        * @param setAnnouncing  the set of potential new cluster members in the
        *                       specified role that are announcing their
        *                       presence and waiting to join the cluster
        *
        * @return true iff the member quorum for the specified role would be
        *         preserved if the specified timed-out members were disconnected
        */
        protected boolean checkRoleQuorum(int cQuorum, Set setMembers,
                Set setTimedOut, Set setHealthy, Set setAnnouncing)
            {
            if (setTimedOut.contains(getService().getCluster().getLocalMember()))
                {
                // suicide is a special case.  Here, if we have any quorum rule
                // set, disallow suicide.
                return cQuorum == 0;
                }

            return setHealthy.size() + setAnnouncing.size() >= cQuorum;
            }


        // ----- ActionPolicy interface -----------------------------------

        /**
        * {@inheritDoc}
        */
        public void init(Service service)
            {
            setService(service);
            }

        /**
        * {@inheritDoc}
        */
        public boolean isAllowed(Service service, Action action)
            {
            if (action instanceof MemberTimeoutAction)
                {
                MemberTimeoutAction timeoutAction = (MemberTimeoutAction) action;

                // Evaluate whether or not the configured cluster quorum will
                // be preserved if the disconnect action is performed.  From
                // this member's point of view, other members are either
                // "timed-out", "healthy", or "unknown".
                //
                // The "worst-case" scenario is that any members that are not
                // known to be healthy are also in the process of being kicked
                // from the cluster (or will be in the very near future).
                Set<Member> setMembers    = service.getInfo().getServiceMembers();
                Set<Member> setTimedOut   = timeoutAction.getTimedOutMemberSet();
                Set<Member> setHealthy    = timeoutAction.getResponsiveMemberSet();
                Set<Member> setAnnouncing = timeoutAction.getAnnouncingMemberSet();

                Map<String, Set<Member>> mapMembersByRole    = groupMembersByRole(setMembers);
                Map<String, Set<Member>> mapTimedOutByRole   = groupMembersByRole(setTimedOut);
                Map<String, Set<Member>> mapHealthyByRole    = groupMembersByRole(setHealthy);
                Map<String, Set<Member>> mapAnnouncingByRole = groupMembersByRole(setAnnouncing);

                for (Entry<String, Integer> entry : getClusterQuorumMap().entrySet())
                    {
                    String sRole   = entry.getKey();
                    int    cQuorum = entry.getValue();

                    if (sRole.startsWith(SITES))
                        {
                        // see LegacyXmlConfigurableQuorumPolicy
                        sRole = sRole.substring(SITES.length());

                        if (!checkSiteQuorum(cQuorum,
                            ensureSet(mapMembersByRole.get(sRole)),
                            ensureSet(mapTimedOutByRole.get(sRole)),
                            ensureSet(mapHealthyByRole.get(sRole)),
                            ensureSet(mapAnnouncingByRole.get(sRole))))
                            {
                            return false;
                            }
                        }
                    else if (sRole.startsWith(MACHINES))
                        {
                        // see LegacyXmlConfigurableQuorumPolicy
                        sRole = sRole.substring(MACHINES.length());
                        if (!checkMachineQuorum(cQuorum,
                            ensureSet(mapMembersByRole.get(sRole)),
                            ensureSet(mapTimedOutByRole.get(sRole)),
                            ensureSet(mapHealthyByRole.get(sRole)),
                            ensureSet(mapAnnouncingByRole.get(sRole))))
                            {
                            return false;
                            }
                        }
                    else
                        {
                        if (!checkRoleQuorum(cQuorum,
                            ensureSet(mapMembersByRole.get(sRole)),
                            ensureSet(mapTimedOutByRole.get(sRole)),
                            ensureSet(mapHealthyByRole.get(sRole)),
                            ensureSet(mapAnnouncingByRole.get(sRole))))
                            {
                            return false;
                            }
                        }
                    }
                }

            return true;
            }


        // ----- constants and data members -------------------------------

        /**
        * The role identifier to use for matching any cluster members.
        */
        public static final String ROLE_ALL = "*role-any*";

        /**
        * The role prefix to use for the "timeout-site-quorum".
        */
        public static final String SITES = "*sites*";

        /**
        * The role prefix to use for the "timeout-machine-quorum".
        */
        public static final String MACHINES = "*machines*";

        /**
        * The cluster service.
        */
        protected Service m_service;

        /**
        * The cluster membership quorum map, keyed by role name.
         */
        protected Map<String, Integer> m_mapQuorumByRole;
        }
    }
