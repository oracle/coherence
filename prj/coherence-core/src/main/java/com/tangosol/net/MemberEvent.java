/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import java.util.EventListener;
import java.util.EventObject;

import com.tangosol.util.Listeners;


/**
* An event which indicates that membership has changed:
* <ul>
* <li>a Member has joined
* <li>a Member is leaving
* <li>a Member has left
* </ul>
* A MemberEvent object is sent as an argument to the MemberListener
* interface methods.
*
* @see MemberListener
* @author cp  2002.12.12
*/
public class MemberEvent
        extends EventObject
    {
    /**
    * Constructs a new MemberEvent.
    *
    * @param oSource  the source object that fired the event (a Service)
    * @param nId      this event's id
    * @param member   the Member for which the event applies
    */
    public MemberEvent(Object oSource, int nId, Member member)
        {
        super(oSource);

        m_nId    = nId;
        m_member = member;
        }

    /**
    * Return this event's id.
    *
    * @return the event ID, one of
    */
    public int getId()
        {
        return m_nId;
        }

    /**
    * Return the Member associated with this event.
    *
    * @return the Member
    */
    public Member getMember()
        {
        return m_member;
        }

    /**
    * Return the Service that fired the event
    *
    * @return the Service
    */
    public Service getService()
        {
        return (Service) getSource();
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Dispatch this event to the specified listeners collection.
    *
    * @param listeners the listeners collection
    *
    * @throws ClassCastException if any of the targets is not
    *         an instance of MemberListener interface
    */
    public void dispatch(Listeners listeners)
        {
        if (listeners != null)
            {
            dispatch(listeners.listeners());
            }
        }

    /**
    * Dispatch this event to the specified array of listeners.
    *
    * @param aListeners  the array of listeners
    *
    * @throws ClassCastException if any of the targets is not
    *         an instance of MemberListener interface
    */
    public void dispatch(EventListener[] aListeners)
        {
        for (int i = aListeners.length; --i >= 0; )
            {
            MemberListener target = (MemberListener) aListeners[i];

            switch (getId())
                {
                case MEMBER_JOINED:
                    target.memberJoined(this);
                    break;

                case MEMBER_LEAVING:
                    target.memberLeaving(this);
                    break;

                case MEMBER_LEFT:
                    target.memberLeft(this);
                    break;

                case MEMBER_RECOVERED:
                    target.memberRecovered(this);
                    break;
                 }
            }
        }

    /**
    * Check whether a Member object for this event represents the local
    * member of the cluster.
    *
    * @return true iff the event's Member object represents the local
    *              cluster member
    */
    public boolean isLocal()
        {
        try
            {
            Member member = getMember();
            return member == null || member.equals(
                getService().getCluster().getLocalMember());
            }
        catch (RuntimeException e)
            {
            // the local services are stopped
            // there is no good answer...
            return true;
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Returns a String representation of this MemberEvent object.
    *
    * @return a String representation of this MemberEvent object.
    */
    public String toString()
        {
        Member member      = getMember();
        String sSourceName = getSource() == null
                                ? "<unknown source>"
                                : getSource().getClass().getName();

        return new StringBuffer("MemberEvent{Member=")
          .append(member == null ? "Local" : String.valueOf(member.getId()))
          .append(' ')
          .append(DESCRIPTIONS[getId()])
          .append(' ')
          .append(sSourceName)
          .append('}')
          .toString();
        }


    // ----- constants ------------------------------------------------------

    /**
    * This event indicates that a Member has joined.
    */
    public static final int MEMBER_JOINED = 1;

    /**
    * This event indicates that a Member is leaving.
    */
    public static final int MEMBER_LEAVING = 2;

    /**
    * This event indicates that a Member has left.
    */
    public static final int MEMBER_LEFT = 3;

    /**
     * This event indicates that a Member has performed persistence recovery.
     */
    public static final int MEMBER_RECOVERED = 4;

    /**
    * Descriptions of the various event IDs.
    */
    private static final String[] DESCRIPTIONS = {"<unknown>", "JOINED", "LEAVING", "LEFT", "RECOVERED"};


    // ----- data members ---------------------------------------------------

    /**
    * The event's id.
    */
    private int m_nId;

    /**
    * A Member.
    */
    private Member m_member;
    }
