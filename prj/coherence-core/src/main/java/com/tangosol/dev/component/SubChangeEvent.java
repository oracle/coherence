/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import java.util.EventObject;


/**
* SubChangeEvent provides information about a modification to a contained
* trait.
*
* @see com.tangosol.dev.component.SubChangeListener
*
* @version 0.10, 11/16/97
* @version 0.50, 07/24/98  renamed from TraitChangeEvent, added MODIFIED, etc
* @author  Cameron Purdy
*/
public class SubChangeEvent
        extends    EventObject
        implements Constants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a SubChangeEvent.
    *
    * @param traitSource  the trait which is the source of the event
    * @param traitSub     the sub trait (may not be of the trait class)
    * @param nAction      one of 
    * @param nContext     one of CTX_VETO, CTX_UNDO, or CTX_DONE
    * @param event        the event object (if any) that this sub change is
    *                     reporting
    */
    public SubChangeEvent(Trait traitSource, Trait traitSub, int nAction, int nContext, EventObject event)
        {
        super(traitSource);

        m_traitSub = traitSub;
        m_nAction  = nAction;
        m_nContext = nContext;
        m_event    = event;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the trait which was changed and created this event object.
    *
    * @return the trait that raised the event
    */
    public Trait getTrait()
        {
        return (Trait) getSource();
        }

    /**
    * Determine the sub-trait that caused this event.
    *
    * @return the sub-trait that is being modified, added, removed, or
    *         un-removed
    */
    public Trait getSubTrait()
        {
        return m_traitSub;
        }

    /**
    * Determine the cause of this event if the event is related to a
    * sub-trait.
    *
    * @return an enumerated value specifying the action type
    *
    * @see Constants#SUB_CHANGE
    * @see Constants#SUB_ADD
    * @see Constants#SUB_REMOVE
    * @see Constants#SUB_UNREMOVE
    */
    public int getAction()
        {
        return m_nAction;
        }

    /**
    * Determine the context of the event.  An event can be vetoable, vetoed
    * (an "undo"), or a notification of a change already made ("done").
    *
    * @return true if this event is just undoing a previously vetoed event
    */
    public int getContext()
        {
        return m_nContext;
        }

    /**
    * Determine if this event is veto-able.
    *
    * @return true if this event is veto-able
    */
    public boolean isVetoable()
        {
        return m_nContext == CTX_VETO;
        }

    /**
    * Determine if this event is just undoing a previous event that was
    * vetoed.
    *
    * @return true if this event is just undoing a previously vetoed event
    */
    public boolean isUndo()
        {
        return m_nContext == CTX_UNDO;
        }

    /**
    * Get the event that occurred on the sub-trait.
    *
    * @return tThe event that occurred on the sub-trait (if applicable)
    */
    public EventObject getEvent()
        {
        return m_event;
        }

    /**
    * Format the event information as a string.
    *
    * @return the event information as a string
    */
    public String toString()
        {
        final String[] ACTIONS  = new String[] {"Change", "Add", "Remove", "Un-Remove"};
        final String[] CONTEXTS = new String[] {"Vetoable", "Undo (Vetoed)", "Done (Post)"};

        return "SubChangeEvent:  Source Trait=" + getSource()
                + ", Sub Trait=" + getSubTrait()
                + ", Action=" +  ACTIONS[getAction()]
                + ", Context=" + CONTEXTS[getContext()]
                + ", Event=" + getEvent();
        }

    // ----- data members ---------------------------------------------------

    /**
    * The sub trait.
    */
    private Trait m_traitSub;

    /**
    * The sub trait action.
    *
    * @see Constants#SUB_CHANGE
    * @see Constants#SUB_ADD
    * @see Constants#SUB_REMOVE
    * @see Constants#SUB_UNREMOVE
    */
    private int m_nAction;

    /**
    * The event context (vetoable, vetoed notification, change notification).
    *
    * @see Constants#CTX_VETO
    * @see Constants#CTX_UNDO
    * @see Constants#CTX_DONE
    */
    private int m_nContext;

    /**
    * The event that occurred on the sub-trait (if applicable).
    */
    private EventObject m_event;
    }
