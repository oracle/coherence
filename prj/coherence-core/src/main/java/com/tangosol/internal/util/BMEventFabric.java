/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;


import com.tangosol.util.MapEvent;


/**
* BMEventFabric is a specialization of QueueFabric used internally by the
* PartitionedCache service for tracking observed backing-map events along 2
* dimensions: the key associated with the event, and the thread that generated
* it.
*
* @since  Coherence 3.7
* @author rhl 2010.08.13
*/
public class BMEventFabric
        extends QueueFabric
    {
    // ----- constructors -------------------------------------------------

    /**
    * Default constructor.
    */
    public BMEventFabric()
        {
        }


    // ----- BMEventFabric methods ----------------------------------------

    /**
    * Create and return an EventHolder for the specified event.
    *
    * @param oStatus   an (opaque) status object corresponding to the key
    * @param event     the event
    * @param lEventId  a uniquely assigned event id
    *
    * @return an EventHolder for the specified status, event and event id
    */
    public static EventHolder createEventHolder(Object oStatus, MapEvent event,
                                                long lEventId)
        {
        return new EventHolder(oStatus, event, lEventId);
        }

    /**
    * Create an EventQueue for holding events observed by a given thread.
    *
    * @return an EventQueue
    */
    public EventQueue createThreadQueue()
        {
        return instantiateRowQueue();
        }

    /**
    * Create an EventQueue for holding events observed for a given key.
    *
    * @return an EventQueue
    */
    public EventQueue createKeyQueue()
        {
        return instantiateColQueue();
        }

    /**
    * {@inheritDoc}
    */
    protected EventQueue instantiateRowQueue()
        {
        return new EventQueue(true);
        }

    /**
    * {@inheritDoc}
    */
    protected EventQueue instantiateColQueue()
        {
        return new EventQueue(false);
        }


    // ----- inner class: EventQueue --------------------------------------

    /**
    * EventQueue represents a queue of observed BM events.
    */
    public class EventQueue
            extends LinkedQueue
        {
        // ----- constructors ---------------------------------------------

        /**
        * Construct an EventQueue.
        *
        * @param fThread  true for a "by-thread" queue, or a "by-key" queue
        *                 otherwise
        */
        public EventQueue(boolean fThread)
            {
            super(fThread);
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the first event in this queue.
        *
        * @return the first event in this queue
        */
        public EventHolder getFirstEvent()
            {
            return (EventHolder) getFirstNode();
            }

        /**
        * Return the last event in this queue.
        *
        * @return the last event in this queue
        */
        public EventHolder getLastEvent()
            {
            return (EventHolder) getLastNode();
            }
        }


    // ----- inner class: EventHolder -------------------------------------

    /**
    * EventHolder represents a node in the BMEventMatrix.
    */
    public static class EventHolder
            extends LinkedNode
        {
        // ----- constructors ---------------------------------------------

        /**
        * Construct an EventHolder for the specified event.
        *
        * @param oStatus   an (opaque) status object corresponding to the key
        * @param event     the event
        * @param lEventId  a uniquely assigned event id
        */
        public EventHolder(Object oStatus, MapEvent event, long lEventId)
            {
            m_oStatus  = oStatus;
            m_event    = event;
            m_lEventId = lEventId;
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the next EventHolder posted by the same thread.
        *
        * @return the next EventHolder posted by the same thread
        */
        public EventHolder getNextByThread()
            {
            return (EventHolder) getNextInRow();
            }

        /**
        * Return the previous EventHolder posted by the same thread.
        *
        * @return the previous EventHolder posted by the same thread
        */
        public EventHolder getPrevByThread()
            {
            return (EventHolder) getPrevInRow();
            }

        /**
        * Return the next EventHolder posted on the same key.
        *
        * @return the next EventHolder posted on the same key
        */
        public EventHolder getNextByKey()
            {
            return (EventHolder) getNextInCol();
            }

        /**
        * Return the previous EventHolder posted on the same key.
        *
        * @return the previous EventHolder posted on the same key
        */
        public EventHolder getPrevByKey()
            {
            return (EventHolder) getPrevInCol();
            }

        /**
        * Return the EventQueue holding events posted by the same thread.
        *
        * @return the EventQueue holding events posted by the same thread
        */
        public EventQueue getThreadQueue()
            {
            return (EventQueue) getRowQueue();
            }

        /**
        * Return the EventQueue holding events posted on the same key.
        *
        * @return the EventQueue holding events posted on the same key
        */
        public EventQueue getKeyQueue()
            {
            return (EventQueue) getColumnQueue();
            }

        /**
        * Return the opaque status object associated with this event holder.
        *
        * @return the opaque status object associated with this event holder
        */
        public Object getStatus()
            {
            return m_oStatus;
            }

        /**
        * Return the MapEvent represented by this event holder.
        *
        * @return the MapEvent represented by this event holder
        */
        public MapEvent getEvent()
            {
            return m_event;
            }

        /**
        * Return the unique event id of the event represented by this event holder.
        *
        * @return the unique event id
        */
        public long getEventId()
            {
            return m_lEventId;
            }

        // ----- object methods -------------------------------------------

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return "{EventHolder Event=" + getEvent() + ", EventId=" + getEventId() + "}";
            }

        // ----- data members ---------------------------------------------

        /**
        * The (opaque) event status associated with the key.
        */
        protected Object   m_oStatus;

        /**
        * The MapEvent that this EventHolder represents.
        */
        protected MapEvent m_event;

        /**
        * Unique id for this event.
        * This id is generated and used during event processing only (and not at
        * the point of event generation).
        */
        protected long     m_lEventId;
        }
    }