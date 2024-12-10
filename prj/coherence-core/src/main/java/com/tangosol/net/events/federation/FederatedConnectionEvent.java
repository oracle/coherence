/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.federation;

import com.tangosol.net.events.Event;

/**
 * Represents a change in connection state to a known participant, including
 * connecting, disconnected, error and through-put events.
 * <p>
 * <strong>NOTE:</strong> These events are raised on the same thread
 * that caused the event.  As such {@link com.tangosol.net.events.EventInterceptor}s
 * that handle this event must never perform blocking operations.
 *
 * @author pp  2013.04.02
 *
 * @since 12.2.1
 */
public interface FederatedConnectionEvent
        extends Event<FederatedConnectionEvent.Type>
    {
    /**
     * Obtains the name of the participant on which the event occurred.
     *
     * @return name of the participant
     */
    String getParticipantName();

    // ----- enum Type ------------------------------------------------------

    /**
     * The types of {@link FederatedConnectionEvent}s.
     */
    public static enum Type
        {
        /**
         * Dispatched when a connection is about to be initiated
         * to a participant.
         */
        CONNECTING,

        /**
         * Dispatched when a disconnection from a participant
         * is detected.
         */
        DISCONNECTED,

        /**
         * Dispatched when a participant is backlogged; if
         * the participant is remote it indicates the
         * remote participant has more work than it can handle;
         * if the participant is local it indicates this
         * participant has more work than it can handle.
         */
        BACKLOG_EXCESSIVE,

        /**
         * Dispatched when a participant was previously
         * backlogged but is no longer so.
         */
        BACKLOG_NORMAL,

        /**
         * Dispatched when replicating data to a participant
         * encountered errors while applying the changes on the
         * remote participant or due to exhausting connection retry
         * attempts to the participant.
         */
        ERROR
        }
    }
