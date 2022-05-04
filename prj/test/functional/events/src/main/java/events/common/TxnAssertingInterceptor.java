/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events.common;

import com.tangosol.net.events.EventHelper;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.annotation.TransactionEvents;
import com.tangosol.net.events.annotation.UnsolicitedCommitEvents;
import com.tangosol.net.events.partition.Event;
import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.TransactionEvent.Type;
import com.tangosol.net.events.partition.UnsolicitedCommitEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;

import com.tangosol.util.BinaryEntry;

import java.util.Map;
import java.util.Set;

import static events.common.TxnAssertingInterceptor.IDENTIFIER;

/**
 * TxnAssertingInterceptor is a UEM EventInterceptor implementation
 * that asserts the events received with the expectations provided. These
 * expectations can be set prior to the events being received via
 * {@link ExpectationsInformerInvocable}.
 *
 * @author hr  2012.08.07
 * @since Coherence 12.1.2
 */
@Interceptor(identifier = IDENTIFIER)
@TransactionEvents
@UnsolicitedCommitEvents
public class TxnAssertingInterceptor
        extends AbstractTestInterceptor<Event<? extends Enum>>
    {
    // ----- AbstractTestInterceptor methods --------------------------------

    /**
     * {@inheritDoc}
     */
    @Override public void processEvent(Event<? extends Enum> event)
        {
        if (event instanceof TransactionEvent)
            {
            processEvent((TransactionEvent) event);
            }
        else if (event instanceof UnsolicitedCommitEvent)
            {
            processEvent((UnsolicitedCommitEvent) event);
            }
        }

    protected void processEvent(TransactionEvent event)
        {
        Type                    eventType       = event.getType();
        Map<Enum, Expectations> mapExpectations = getExpectations(IDENTIFIER);
        Expectations            expectations    = mapExpectations.get(eventType);

        if (expectations.isEventTypeCheck())
            {
            processEntries(EventHelper.getEntryEventsMap(event), expectations);
            }
        else
            {
            processEntries(event.getEntrySet(), expectations);
            }

        if (expectations.isEmpty())
            {
            mapExpectations.remove(eventType);
            }
        else
            {
            String sMsg = "Expectations were not fulfilled: " + expectations;
            new AssertionError(sMsg).printStackTrace();
            err(sMsg);
            }
        }

    protected void processEvent(UnsolicitedCommitEvent event)
        {
        UnsolicitedCommitEvent.Type eventType       = event.getType();
        Map<Enum, Expectations>     mapExpectations = getExpectations(IDENTIFIER);
        Expectations                expectations    = mapExpectations.get(eventType);

        if (expectations.isEventTypeCheck())
            {
            processEntries(EventHelper.getEntryEventsMap(event), expectations);
            }
        else
            {
            processEntries(event.getEntrySet(), expectations);
            }

        if (expectations.isEmpty())
            {
            mapExpectations.remove(eventType);
            }
        else
            {
            String sMsg = "Expectations were not fulfilled: " + expectations;
            new AssertionError(sMsg).printStackTrace();
            err(sMsg);
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Process the provided set of {@link BinaryEntry entries} validating
     * against the expectations.
     *
     * @param mapEntryEvents  a map keyed by event type with associated entries
     * @param expectations    expected values
     */
    protected void processEntries(Map<EntryEvent.Type, Set<BinaryEntry>> mapEntryEvents, Expectations expectations)
        {
        for (Map.Entry<EntryEvent.Type, Set<BinaryEntry>> entry : mapEntryEvents.entrySet())
            {
            processEntries(entry.getValue(), expectations, entry.getKey());
            }
        }

    /**
     * An interceptor identifier.
     */
    public static final String IDENTIFIER = "PLOAsserter";
    }
