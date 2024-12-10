/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import com.tangosol.net.Member;
import com.tangosol.net.PartitionedService;

import java.util.EventObject;


/**
* The PartitionEvent carries information about an event related to one or more
* partitions.
*
* @author cp  2007.05.18
* @since Coherence 3.3
*/
public class PartitionEvent
        extends EventObject
    {
    /**
    * Construct a partition event.
    *
    * @param svc            the service raising this event
    * @param nId            the event ID, one of the PARTITION_* constants
    * @param setPartitions  the set of partitions represented by this event;
    *                       may be null
    * @param memberFrom     the member that held the partitions prior to the
    *                       action represented by this event; may be null
    * @param memberTo       the member that holds the partitions after the
    *                       action represented by this event; may be null
    */
    public PartitionEvent(PartitionedService svc, int nId,
            PartitionSet setPartitions, Member memberFrom, Member memberTo)
        {
        super(svc);

        m_nId           = nId;
        m_setPartitions = setPartitions;
        m_memberFrom    = memberFrom;
        m_memberTo      = memberTo;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the partitioned service that this event originates from.
    *
    * @return the originating service
    */
    public PartitionedService getService()
        {
        return (PartitionedService) getSource();
        }

    /**
    * Return this event's id. The event id is one of the PARTITION_*
    * enumerated constants.
    * <p>
    * It is expected that new event IDs will be added to subsequent versions.
    * Listener implementations that process partition events should only
    * deal with event objects that have specific, known event IDs, and ignore
    * all others.
    *
    * @return the event id
    */
    public int getId()
        {
        return m_nId;
        }

    /**
    * Determine the set of partitions represented by this event.
    *
    * @return the PartitionSet containing the partition identifiers
    *         represented by this event; may be null
    */
    public PartitionSet getPartitionSet()
        {
        return m_setPartitions;
        }

    /**
    * Determine the member that held the partitions prior to the action
    * represented by this event.
    *
    * @return the "from" Member for the partitions; may be null
    */
    public Member getFromMember()
        {
        return m_memberFrom;
        }

    /**
    * Determine the member that holds the partitions after the action
    * represented by this event.
    *
    * @return the "to" Member for the partitions; may be null
    */
    public Member getToMember()
        {
        return m_memberTo;
        }

    /**
    * Get the event's description.
    *
    * @return this event's description
    */
    protected String getDescription()
        {
        Member memberFrom = getFromMember();
        Member memberTo   = getToMember();

        StringBuffer sb         = new StringBuffer();
        PartitionSet partitions = getPartitionSet();

        if (partitions.cardinality() == 1)
            {
            sb.append("partition=")
              .append(partitions.next(0));
            }
        else
            {
            sb.append(getPartitionSet());
            }

        int nEventId = getId();
        switch (nEventId)
            {
            case PARTITION_LOST:
                if (memberFrom != null)
                    {
                    sb.append(" lost by ")
                      .append(memberFrom);
                    }
                if (memberTo != null)
                    {
                    sb.append(" ownership reassigned to ")
                      .append(memberTo);
                    }
                break;

            case PARTITION_ASSIGNED:
                if (memberTo != null)
                    {
                    sb.append(" ownership assigned to ")
                      .append(memberTo);
                    }
                break;

            case PARTITION_RECOVERED:
                if (memberTo != null)
                    {
                    sb.append(" recovered by ")
                      .append(memberTo);
                    }
                break;

            case PARTITION_TRANSMIT_BEGIN:
            case PARTITION_TRANSMIT_COMMIT:
            case PARTITION_TRANSMIT_ROLLBACK:
            case PARTITION_RECEIVE_BEGIN:
            case PARTITION_RECEIVE_COMMIT:
                sb.append(' ')
                  .append(getDescription(nEventId))
                  .append(" from ");
                if (memberFrom == null)
                    {
                    sb.append("backup");
                    }
                else
                    {
                    sb.append(memberFrom);
                    }
                sb.append(" to ")
                  .append(memberTo);
                break;

            default:
                throw new IllegalStateException("unknown event: " + nEventId);
            }

        return sb.toString();
        }

    /**
    * Convert an event ID into a human-readable string.
    *
    * @param nId  an event ID, one of the PARTITION_* enumerated values
    *
    * @return a corresponding human-readable string, for example "lost"
    */
    public static String getDescription(int nId)
        {
        switch (nId)
            {
            case PARTITION_LOST:
                return "lost";

            case PARTITION_ASSIGNED:
                return "assigned";

            case PARTITION_RECOVERED:
                return "recovered";

            case PARTITION_TRANSMIT_BEGIN:
                return "transmission began";

            case PARTITION_TRANSMIT_COMMIT:
                return "transmission committed";

            case PARTITION_TRANSMIT_ROLLBACK:
                return "transmission rolled back";

            case PARTITION_RECEIVE_BEGIN:
                return "receive began";

            case PARTITION_RECEIVE_COMMIT:
                return "receive committed";

            default:
                return "<unknown>";
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a String representation of this PartitionEvent object.
    *
    * @return a human-readable description of the PartitionEvent
    */
    public String toString()
        {
        String sEvt = getClass().getName();

        return sEvt.substring(sEvt.lastIndexOf('.') + 1) + "{Service="
            +  getService().getInfo().getServiceName() + ", "
            +  getDescription() + '}';
        }


    // ----- constants ------------------------------------------------------

    /**
    * This event indicates that one or more partitions have been lost. When a
    * partition is lost, its ownership is re-assigned to an existing member, on
    * which this event is then raised. By default, the partition is assigned to
    * the senior ownership-enabled member running the partitioned service. For the
    * PARTITION_LOST event, the partition set and "to member" will always be
    * non-null.
    * <p>
    * As of Coherence 3.6, a restart of an ownership-enabled member after the
    * loss of all ownership-enabled members is indistinguishable from a fresh
    * service start.  In such a scenario, the PARTITION_ASSIGNED event is
    * raised.
    *
    * @since Coherence 3.3
    */
    public static final int PARTITION_LOST              = 1;

    /**
    * This event indicates that data that belong to one or more partitions are
    * about to be transferred to a different member. When this event is raised,
    * the partitions have been frozen (no changes can occur to data managed
    * within that partitions) and they have been "unowned".
    *
    * @since Coherence 3.5
    */
    public static final int PARTITION_TRANSMIT_BEGIN    = 2;

    /**
    * This event indicates that data that belong to one or more partitions
    * have been successfully transferred to a different member and all the data
    * for that partition have been removed from this node.
    *
    * @since Coherence 3.5
    */
    public static final int PARTITION_TRANSMIT_COMMIT   = 3;

    /**
    * This event indicates that a transfer for one or more partitions has been
    * aborted (e.g. receiver's failure), and the partitions are now "owned" by
    * this node again, and are about to be unfrozen.
    *
    * @since Coherence 3.5
    */
    public static final int PARTITION_TRANSMIT_ROLLBACK = 4;

    /**
    * This event indicates that a transfer for one or more partitions is
    * beginning and the data that belongs to partitions in the specified
    * partition set are about to be inserted into corresponding backing maps.
    * When this is event is raised, this node is not yet an owner for these
    * partitions.
    * <p>
    * Note: if this event is raised as a result of a recovery from the backup
    * during failover, the "member from" property will be null.
    *
    * @since Coherence 3.5
    */
    public static final int PARTITION_RECEIVE_BEGIN     = 5;

    /**
    * This event indicates that a transfer for one or more partitions has
    * completed and the data that belong to these partitions have been inserted
    * into corresponding backing maps. This event is raised immediately before
    * this node becomes the partition owner.
    * <p>
    * Note: if this event is raised as a result of a recovery from the backup
    * during failover, the "member from" property will be null.
    *
    * @since Coherence 3.5
    */
    public static final int PARTITION_RECEIVE_COMMIT    = 6;

    /**
    * This event indicates that the ownership of one or more partitions have
    * been assigned to a service member.  Partitions are initially assigned when
    * the first ownership-enabled member joins the service, at which time this
    * event is raised.  By default, the partition is assigned to the senior
    * ownership-enabled member running the partitioned service. For the
    * PARTITION_ASSIGNED event, the partition set and "to member" will always be
    * non-null.
    *
    * @since Coherence 3.6
    */
    public static final int PARTITION_ASSIGNED          = 7;

    /**
    * This event indicates that one or more partitions have been recovered by a
    * service member.  Partitions are recovered from persistent storage when
    * neither the primary nor any backup owners exist (e.g. during service
    * startup or following the departure of all owners) and a persistent copy is
    * located by the configured PersistenceEnvironment. For the PARTITION_RECOVERED
    * event, the partition set and "to member" will always be non-null.
    *
    * @since Coherence 12.1.3
    */
    public static final int PARTITION_RECOVERED         = 8;


    // ----- data members ---------------------------------------------------

    /**
    * The event's id.
    */
    protected int m_nId;

    /**
    * The set of affected partitions.
    */
    protected PartitionSet m_setPartitions;

    /**
    * The "from" member for events that represent transfer from a member.
    */
    protected Member m_memberFrom;

    /**
    * The "to" member for events that represent transfer to a member.
    */
    protected Member m_memberTo;
    }
