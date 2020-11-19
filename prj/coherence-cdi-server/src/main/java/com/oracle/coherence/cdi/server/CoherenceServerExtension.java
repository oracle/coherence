/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.oracle.coherence.cdi.CdiEventObserver;
import com.oracle.coherence.cdi.CdiInterceptorSupport;
import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.CdiInterceptorSupport.EntryEventHandler;
import com.oracle.coherence.cdi.CdiInterceptorSupport.EntryProcessorEventHandler;
import com.oracle.coherence.cdi.CdiInterceptorSupport.TransactionEventHandler;
import com.oracle.coherence.cdi.CdiInterceptorSupport.TransferEventHandler;
import com.oracle.coherence.cdi.CdiInterceptorSupport.UnsolicitedCommitEventHandler;

import com.tangosol.net.events.internal.NamedEventInterceptor;
import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.UnsolicitedCommitEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryProcessorEvent;

import java.util.ArrayList;
import java.util.List;

import java.util.stream.Collectors;

import javax.enterprise.event.Observes;

import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessObserverMethod;

/**
 * A Coherence CDI {@link Extension} that should only be used within the
 * cluster members.
 *
 * @author Jonathan Knight  2019.10.24
 * @author Aleks Seovic  2020.03.25
 *
 * @since 20.06
 */
public class CoherenceServerExtension
        implements Extension, CoherenceExtension.InterceptorProvider
    {
    @Override
    public Iterable<NamedEventInterceptor<?>> getInterceptors()
        {
        return m_listInterceptors.stream()
                .map(handler -> new NamedEventInterceptor<>(handler.getId(), handler))
                .collect(Collectors.toList());
        }

    // ---- server-side interceptors support --------------------------------

    /**
     * Process observer methods for {@link EntryEvent}s.
     *
     * @param event  the event to process
     * @param <K>    the type of {@code EntryEvent} keys
     * @param <V>    the type of {@code EntryEvent} values
     */
    private <K, V> void processEntryEventObservers(
            @Observes ProcessObserverMethod<EntryEvent<K, V>, ?> event)
        {
        m_listInterceptors.add(new EntryEventHandler<>(new CdiEventObserver<>(event)));
        }

    /**
     * Process observer methods for {@link EntryProcessorEvent}s.
     *
     * @param event  the event to process
     */
    private void processEntryProcessorEventObservers(
            @Observes ProcessObserverMethod<EntryProcessorEvent, ?> event)
        {
        m_listInterceptors.add(new EntryProcessorEventHandler(new CdiEventObserver<>(event)));
        }

   /**
     * Process observer methods for {@link TransactionEvent}s.
     *
     * @param event  the event to process
     */
    private void processTransactionEventObservers(
            @Observes ProcessObserverMethod<TransactionEvent, ?> event)
        {
        m_listInterceptors.add(new TransactionEventHandler(new CdiEventObserver<>(event)));
        }

   /**
     * Process observer methods for {@link TransferEvent}s.
     *
     * @param event  the event to process
     */
    private void processTransferEventObservers(
            @Observes ProcessObserverMethod<TransferEvent, ?> event)
        {
        m_listInterceptors.add(new TransferEventHandler(new CdiEventObserver<>(event)));
        }

   /**
     * Process observer methods for {@link UnsolicitedCommitEvent}s.
     *
     * @param event  the event to process
     */
    private void processUnsolicitedCommitEventObservers(
            @Observes ProcessObserverMethod<UnsolicitedCommitEvent, ?> event)
        {
        m_listInterceptors.add(new UnsolicitedCommitEventHandler(new CdiEventObserver<>(event)));
        }

    // ---- data members ----------------------------------------------------

    /**
     * A list of event interceptors for all discovered observer methods.
     */
    private final List<CdiInterceptorSupport.EventHandler<?, ?>> m_listInterceptors = new ArrayList<>();
    }
