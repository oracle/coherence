/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.util;

import com.oracle.coherence.common.base.Associated;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * Simple thread-safe {@link AssociationPile} implementation based on a
 * LinkedList.
 *
 * @author gg, jh 2014.03.27
 */
public class SimpleAssociationPile<E>
        implements AssociationPile<E>
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public SimpleAssociationPile()
        {
        }

    // ----- AssociationPile interface --------------------------------------

    @Override
    public synchronized boolean add(E element)
        {
        f_list.add(element);

        m_fAvailable = true;

        return true;
        }

    @Override
    public synchronized E poll()
        {
        if (f_list.isEmpty())
            {
            m_fAvailable = false;
            return null;
            }

        E elFirst = f_list.getFirst();

        Contention contention = lockAssociation(elFirst, Contention.NONE);
        if (contention == Contention.NONE)
            {
            f_list.removeFirst();
            if (f_list.isEmpty())
                {
                m_fAvailable = false;
                }
            return elFirst;
            }

        Iterator<E> iter = f_list.iterator();
        iter.next(); // already looked at the first item

        while (iter.hasNext())
            {
            E el = iter.next();

            contention = lockAssociation(el, contention);
            if (contention == Contention.NONE)
                {
                iter.remove();
                return el;
                }
            }

        return null;
        }

    @Override
    public synchronized void release(E element)
        {
        unlockAssociation(element);
        }

    @Override
    public synchronized int size()
        {
        return f_list.size();
        }

    @Override
    public boolean isAvailable()
        {
        return m_fAvailable;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Lock the specified element's association
     *
     * @param element     the element with a potential association to lock
     * @param contention  the maximum contention level that has been previously
     *                    reported by this pile
     *
     * @return {@link Contention#NONE} if the given element does not have an
     *          association or if it's association was successfully locked;
     *          {@link Contention#SINGLE} if the element is associated with
     *          an element that was polled, but hasn't been released;
     *          {@link Contention#ALL} if the element is associated with
     *          {@link AssociationPile#ASSOCIATION_ALL} and there are elements
     *          with a non-null association element that were polled, but
     *          haven't been released;
     */
    private Contention lockAssociation(E element, Contention contention)
        {
        Object oAssoc = element instanceof Associated ?
            ((Associated) element).getAssociatedKey() : null;
        if (oAssoc == null)
            {
            return Contention.NONE;
            }

        switch (contention)
            {
            case ALL:
                return contention;

            default:
                if (oAssoc == ASSOCIATION_ALL)
                    {
                    if (!f_setLocked.isEmpty())
                        {
                        // beyond this point only non-associated elements are eligible
                        return Contention.ALL;
                        }

                    m_fAllLocked = true;
                    return Contention.NONE;
                    }
                else
                    {
                    return !m_fAllLocked && f_setLocked.add(oAssoc) ?
                        Contention.NONE : Contention.SINGLE;
                    }
            }
        }

    /**
     * Unlock the specified element's association
     *
     * @param element  the element with a potential association to unlock
     *
     * @return true if the given element does not have an association or if
     *         it's association was successfully unlocked; false otherwise
     */
    private void unlockAssociation(E element)
        {
        Object oAssoc = element instanceof Associated ?
            ((Associated) element).getAssociatedKey() : null;

        if (oAssoc != null)
            {
            if (oAssoc == ASSOCIATION_ALL)
                {
                if (m_fAllLocked)
                    {
                    m_fAllLocked = false;
                    }
                }
              else
                {
                f_setLocked.remove(oAssoc);
                }
            }
        }


    // ----- data members ---------------------------------------------------

    /**
     * Results of the {@link #lockAssociation} method.
     */
    public enum Contention
        {
        NONE,   // non-contended
        SINGLE, // contended due to a single association
        ALL,    // contended due to ASSOCIATION_ALL
        }

    /**
     * The queue of elements.
     */
    private final LinkedList<E> f_list = new LinkedList();

    /**
     * The set of "locked" associations.
     */
    private final Set f_setLocked = new HashSet();

    /**
     * The flag that indicates that ALL_ASSOCIATION is "locked".
     */
    private volatile boolean m_fAllLocked;

    /**
     * The availability flag.
     */
    private volatile boolean m_fAvailable;
    }
