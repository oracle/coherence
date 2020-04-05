/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.internal;

import com.oracle.coherence.common.base.Continuation;

import com.tangosol.net.Member;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.events.partition.PartitionedServiceDispatcher;
import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.Event;
import com.tangosol.net.events.partition.UnsolicitedCommitEvent;

import com.tangosol.util.BinaryEntry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link PartitionedServiceDispatcher} used by the
 * PartitionedService to dispatch appropriate events.
 *
 * @author rhl/hr  2011.07.20
 * @since Coherence 12.1.2
 */
public class ServiceDispatcher
        extends AbstractEventDispatcher
        implements PartitionedServiceDispatcher
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct a dispatcher for the specified service.
     *
     * @param service  the service associated with this dispatcher
     */
    public ServiceDispatcher(PartitionedService service)
        {
        super(EVENT_TYPES);

        m_service = service;
        }

    // ----- PartitionedServiceDispatcher interface -------------------------

    /**
     * {@inheritDoc}
     */
    public PartitionedService getService()
        {
        return m_service;
        }

    // ----- ServiceDispatcher methods --------------------------------------

    /**
     * Return a continuation whose completion will cause a
     * {@link TransactionEvent} to be dispatched.
     *
     * @param eventType   the {@link TransactionEvent.Type} to raise
     * @param setEntries  a set of {@link BinaryEntry entries}
     *
     * @return a continuation whose completion will post a transaction event
     */
    public Continuation getTransactionEventContinuation(
            TransactionEvent.Type eventType,
            Set<BinaryEntry> setEntries)
        {
        return getDispatchContinuation(new PartitionedServiceTransactionEvent(
                this, eventType, setEntries), null);
        }

    /**
     * Return a continuation whose completion will cause a {@link TransferEvent}
     * to be dispatched.
     *
     * @param eventType     the {@link TransferEvent.Type} to raise
     * @param nPartition    the partition being transferred
     * @param memberLocal   the local member
     * @param memberRemote  the remote member
     * @param mapEntries    a map of entries by cache name
     * @param continuation  the continuation to complete after dispatching
     *
     * @return a continuation whose completion will post a transfer event
     */
    public Continuation getTransferEventContinuation(
            TransferEvent.Type eventType, int nPartition, Member memberLocal,
            Member memberRemote, Map<String, Set<BinaryEntry>> mapEntries,
            Continuation continuation)
        {
        PartitionedServiceTransferEvent event = new PartitionedServiceTransferEvent(
                this, eventType, nPartition, memberLocal, memberRemote, mapEntries);

        return getDispatchContinuation(event, continuation);
        }

    /**
     * Return a continuation whose completion will cause a {@link
     * TransferEvent.RecoveryTransferEvent RecoveryTransferEvent} to be dispatched.
     *
     * @param nPartition     the partition being transferred
     * @param memberLocal    the local member
     * @param memberRemote   the remote member
     * @param mapEntries     a map of entries by cache name
     * @param sSnapshotName  the name of the snapshot that was recovered from
     * @param continuation   the continuation to complete after dispatching
     *
     * @return a continuation whose completion will post a transfer event
     */
    public Continuation getRecoveryTransferEventContinuation(int nPartition, Member memberLocal,
            Member memberRemote, Map<String, Set<BinaryEntry>> mapEntries,
            String sSnapshotName, Continuation continuation)
        {
        PartitionedServiceRecoveryEvent event = new PartitionedServiceRecoveryEvent(
                    this, nPartition, memberLocal, memberRemote, mapEntries, sSnapshotName);

        return getDispatchContinuation(event, continuation);
        }

    /**
     * Return a continuation whose completion will cause a
     * {@link UnsolicitedCommitEvent} to be dispatched.
     *
     * @param eventType   the {@link UnsolicitedCommitEvent.Type} to raise
     * @param setEntries  a set of {@link BinaryEntry entries}
     *
     * @return a continuation whose completion will post an out of band event
     */
    public Continuation getUnsolicitedCommitEventContinuation(
            UnsolicitedCommitEvent.Type eventType,
            Set<BinaryEntry> setEntries)
        {
        return getDispatchContinuation(new PartitionedServiceUnsolicitedCommitEvent(
                this, eventType, setEntries), null);
        }

    // ----- inner class: AbstractPartitionedServiceEvent -------------------

    /**
     * {@link Event} implementation providing access to the
     * {@link PartitionedServiceDispatcher}.
     */
    protected static abstract class AbstractPartitionedServiceEvent<T extends Enum<T>>
            extends AbstractEvent<T>
            implements Event<T>
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct an AbstractPartitionedServiceEvent with the provided
         * dispatcher and event type.
         *
         * @param dispatcher  the dispatcher that raised this event
         * @param eventType   the event type being dispatched
         */
        public AbstractPartitionedServiceEvent(PartitionedServiceDispatcher dispatcher, T eventType)
            {
            super(dispatcher, eventType);
            }

        // ----- AbstractEvent methods --------------------------------------

        /**
         * {@inheritDoc}
         */
        protected String getDescription()
            {
            return super.getDescription() + ", Service=" + getService().getInfo().getServiceName();
            }

        // ----- PartitionEvent methods -------------------------------------

        /**
         * {@inheritDoc}
         */
        public PartitionedServiceDispatcher getDispatcher()
            {
            return (PartitionedServiceDispatcher) m_dispatcher;
            }
        }

    // ----- inner class: PartitionedServiceTransactionEvent ----------------

    /**
     * {@link TransactionEvent} implementation raised by this dispatcher.
     */
    protected static class PartitionedServiceTransactionEvent
            extends AbstractPartitionedServiceEvent<TransactionEvent.Type>
            implements TransactionEvent
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct a transaction event.
         *
         * @param dispatcher  the dispatcher that raised this event
         * @param eventType   the event type
         * @param setEntries  a set of {@link BinaryEntry entries} enlisted
         *                    within this transaction
         */
        protected PartitionedServiceTransactionEvent(
                PartitionedServiceDispatcher dispatcher, TransactionEvent.Type eventType,
                Set<BinaryEntry> setEntries)
            {
            super(dispatcher, eventType);

            m_setEntries = setEntries;
            }

        // ----- AbstractEvent methods --------------------------------------

        /**
         * {@inheritDoc}
         */
        protected boolean isMutableEvent()
            {
            return TransactionEvent.Type.COMMITTING == getType();
            }

        // ----- TransactionEvent interface ---------------------------------

        /**
         * {@inheritDoc}
         */
        public Set<BinaryEntry> getEntrySet()
            {
            return m_setEntries;
            }

        // ----- data members -----------------------------------------------

        /**
         * A set of {@link BinaryEntry entries} modified within this
         * transaction.
         */
        protected final Set<BinaryEntry> m_setEntries;
        }

    // ----- inner class: PartitionedServiceTransferEvent -------------------

    /**
     * {@link TransferEvent} implementation raised by this dispatcher.
     */
    protected static class PartitionedServiceTransferEvent
            extends AbstractPartitionedServiceEvent<TransferEvent.Type>
            implements TransferEvent
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct a transfer event.
         *
         * @param dispatcher    the dispatcher that raised this event
         * @param eventType     the event type
         * @param nPartition    the partition being transferred
         * @param memberLocal   the local member
         * @param memberRemote  the remote member
         * @param mapEntries    a map of entries by cache name
         */
        protected PartitionedServiceTransferEvent(
                PartitionedServiceDispatcher dispatcher, TransferEvent.Type eventType,
                int nPartition, Member memberLocal, Member memberRemote,
                Map<String, Set<BinaryEntry>> mapEntries)
            {
            super(dispatcher, eventType);

            m_nPartition   = nPartition;
            m_mapEntries   = mapEntries;
            m_memberRemote = memberRemote;
            m_memberLocal  = memberLocal;
            }

        // ----- AbstractEvent methods --------------------------------------

        /**
         * {@inheritDoc}
         */
        protected boolean isMutableEvent()
            {
            // for now, all transfer events are immutable
            return false;
            }

        /**
         * {@inheritDoc}
         */
        protected String getDescription()
            {
            return super.getDescription() + ", Partition=" + m_nPartition;
            }

        // ----- TransferEvent interface ------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getPartitionId()
            {
            return m_nPartition;
            }

        /**
         * {@inheritDoc}
         */
        public Member getLocalMember()
            {
            return m_memberLocal;
            }

        /**
         * {@inheritDoc}
         */
        public Member getRemoteMember()
            {
            return m_memberRemote;
            }

        /**
         * {@inheritDoc}
         */
        public Map<String, Set<BinaryEntry>> getEntries()
            {
            return m_mapEntries;
            }

        // ----- data members -----------------------------------------------

        /**
         * The partition id to which the storage transfer event applies.
         */
        protected final int m_nPartition;

        /**
         * The local member.
         */
        protected final Member m_memberLocal;

        /**
         * The remote member.
         */
        protected final Member m_memberRemote;

        /**
         * A map of cache name and set of BinaryEntry's related to this
         * partition and service.
         */
        protected final Map<String, Set<BinaryEntry>> m_mapEntries;
        }

    // ----- inner class: PartitionedServiceRecoveryEvent -------------------

    /**
     * {@link TransferEvent.RecoveryTransferEvent} implementation raised by
     * this dispatcher.
     */
    protected static class PartitionedServiceRecoveryEvent
            extends PartitionedServiceTransferEvent
            implements TransferEvent.RecoveryTransferEvent
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a recovery transfer event.
         *
         * @param dispatcher     the dispatcher that raised this event
         * @param nPartition     the partition being transferred
         * @param memberLocal    the local member
         * @param memberRemote   the remote member
         * @param mapEntries     a map of entries by cache name
         * @param sSnapshotName  the name of the snapshot that was recovered from
         */
        protected PartitionedServiceRecoveryEvent(
                PartitionedServiceDispatcher dispatcher,
                int nPartition, Member memberLocal, Member memberRemote,
                Map<String, Set<BinaryEntry>> mapEntries, String sSnapshotName)
            {
            super(dispatcher, TransferEvent.Type.RECOVERED, nPartition, memberLocal, memberRemote, mapEntries);

            f_sSnapshotName = sSnapshotName;
            }

        // ----- RecoveryTransferEvent interface ----------------------------

        @Override
        public String getSnapshotName()
            {
            return f_sSnapshotName;
            }

        // ----- AbstractEvent methods --------------------------------------

        @Override
        protected String getDescription()
            {
            return super.getDescription() + ", SnapshotName=" + f_sSnapshotName;
            }

        // ----- data members -----------------------------------------------

        /**
         * The name of the snapshot the entries were recovered from.
         */
        protected final String f_sSnapshotName;
        }

    // ----- inner class: PartitionedServiceUnsolicitedCommitEvent ----------

    /**
     * {@link UnsolicitedCommitEvent} implementation raised by this dispatcher.
     */
    protected static class PartitionedServiceUnsolicitedCommitEvent
            extends AbstractPartitionedServiceEvent<UnsolicitedCommitEvent.Type>
            implements UnsolicitedCommitEvent
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct an event for the observed out of band events.
         *
         * @param dispatcher  the dispatcher that raised this event
         * @param eventType   the event type
         * @param setEntries  a set of out of band {@link BinaryEntry entries}
         */
        protected PartitionedServiceUnsolicitedCommitEvent(
                PartitionedServiceDispatcher dispatcher, UnsolicitedCommitEvent.Type eventType,
                Set<BinaryEntry> setEntries)
            {
            super(dispatcher, eventType);

            m_setEntries = setEntries;
            }

        // ----- UnsolicitedCommitEvent interface ---------------------------

        /**
         * {@inheritDoc}
         */
        public Set<BinaryEntry> getEntrySet()
            {
            return m_setEntries;
            }

        // ----- data members -----------------------------------------------

        /**
         * A set of out of band {@link BinaryEntry entries}.
         */
        protected final Set<BinaryEntry> m_setEntries;
        }

    // ----- constants and data members -------------------------------------

    /**
     * The event types raised by this dispatcher.
     */
    protected static final Set<Enum> EVENT_TYPES = new HashSet<Enum>()
        {{
        addAll(Arrays.asList(TransactionEvent.Type.values()));
        addAll(Arrays.asList(TransferEvent.Type.values()));
        addAll(Arrays.asList(UnsolicitedCommitEvent.Type.values()));
        }};

    /**
     * Service context.
     */
    protected final PartitionedService m_service;
    }
