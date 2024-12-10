/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import java.util.EventListener;


/**
* The listener interface for receiving MemberEvents.
*
* @author cp  2002.12.12
*
* @see com.tangosol.net.Service
* @see com.tangosol.net.MemberEvent
*/
public interface MemberListener
        extends EventListener
    {
    /**
    * Invoked when a Member has joined the service.
    * <p>
    * Note: this event could be called during the service restart on the
    * local node ({@link MemberEvent#isLocal() evt.isLocal()}) in which
    * case the listener's code should not attempt to use any clustered
    * cache or service functionality.
    * <p>
    * The most critical situation arises when a number of threads are waiting
    * for a local service restart, being blocked by a Service object
    * synchronization monitor. Since the Joined event should be fired only
    * once, it is called on an event dispatcher thread <b>while holding a
    * synchronization monitor</b>. An attempt to use other clustered service
    * functionality during this local event notification may result in a
    * deadlock.
    *
    * @param evt  the MemberEvent.MEMBER_JOINED event
    */
    public void memberJoined(MemberEvent evt);

    /**
    * Invoked when a Member is leaving the service.
    *
    * @param evt  the MemberEvent.MEMBER_LEAVING event
    */
    public void memberLeaving(MemberEvent evt);

    /**
    * Invoked when a Member has left the service.
    * <p>
    * Note: this event could be called during the service restart on the
    * local node ({@link MemberEvent#isLocal() evt.isLocal()}) in which
    * case the listener's code should not attempt to use any clustered
    * cache or service functionality.
    *
    * @param evt  the MemberEvent.MEMBER_LEFT event
    */
    public void memberLeft(MemberEvent evt);

    /**
     * Invoked when a Member has recovered from persistence.
     *
     * @param evt  the MemberEvent.MEMBER_RECOVERED event
     */
    default public void memberRecovered(MemberEvent evt)
        {
        }
    }