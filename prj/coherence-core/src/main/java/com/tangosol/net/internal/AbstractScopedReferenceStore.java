/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import com.tangosol.net.security.SecurityHelper;

import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.SegmentedConcurrentMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.security.auth.Subject;

/**
 * {@link AbstractScopedReferenceStore} holds scoped references.
 * <p>
 * Cache references are scoped by ClassLoader and, optionally, Subject.
 * Service references are scoped, optionally, by Subject. Subject scoping is
 * handled automatically; ScopedReferenceStore requires no explicit input about
 * Subjects from its clients. Subject scoping is configured in the operational
 * configuration and applies only to remote cache and remote services.
 * <p>
 * AbstractScopedReferenceStore is not thread-safe unless either the lock or lockAll
 * method is used; otherwise, multi-threaded clients must provide their own
 * locking mechanism.
 *
 * @author dag 2010.03.09
 * @author jf 2015.06.23
 *
 * @since Coherence 12.2.1
 *
 * @see ScopedCacheReferenceStore
 * @see ScopedServiceReferenceStore
 */
public class AbstractScopedReferenceStore
    {
    // ----- AbstractScopedReferenceStore methods ---------------------------------------------------------------------

    /**
     * Remove all referenced objects from this store.
     */
    public void clear()
        {
        m_mapByName.clear();
        }

    /**
     * Retrieve the names of all stored references.
     *
     * @return the names of all stored references
     */
    public Set getNames()
        {
        return m_mapByName.keySet();
        }

    // ----- pessimistic methods --------------------------------------------

    /**
     * Attempt to lock the specified item waiting forever.
     * <p>
     * This operation is for the convenience of clients that wish to avoid
     * synchronizing on the entire store.
     *
     * @param sName  the name of the stored item being locked
     *
     * @return true if the referenced item was successfully locked; false
     *         otherwise
     */
    public boolean lock(String sName)
        {
        return lock(sName, -1);
        }

    /**
     * Attempt to lock the specified item waiting forever.
     * <p>
     * This operation is for the convenience of clients that wish to avoid
     * synchronizing on the entire store.
     *
     * @param sName  the name of the stored item being locked
     * @param cWait  the number of milliseconds to continue trying to obtain
     *               a lock; pass zero to return immediately; pass -1 to block
     *               the calling thread until the lock could be obtained
     *
     * @return true if the referenced item was successfully locked; false
     *         otherwise
     */
    public boolean lock(String sName, long cWait)
        {
        return m_mapByName.lock(sName, cWait);
        }

    /**
     * Attempt to lock the entire store, blocking until the lock is obtained.
     *
     * @return true if the store was successfully locked; false otherwise
     *
     */
    public boolean lockAll()
        {
        return m_mapByName.lock(ConcurrentMap.LOCK_ALL, -1);
        }

    /**
     * Unlock the specified item. The item doesn't have to exist to be
     * <i>unlocked</i>. If the item is currently locked, only
     * the <i>holder</i> of the lock can successfully unlock it.
     *
     * @param sName the name of the stored item being unlocked
     *
     * @return true if the item was successfully unlocked; false otherwise
     */
    public boolean unlock(String sName)
        {
        return m_mapByName.unlock(sName);
        }

    /**
     * Unlock the entire store. If the store is currently locked, only
     * the <i>holder</i> of the lock can successfully unlock it.
     *
     * @return true if the item was successfully unlocked; false otherwise
     */
    public boolean unlockAll()
        {
        return m_mapByName.unlock(ConcurrentMap.LOCK_ALL);
        }

    // ----- optimistic methods ---------------------------------------------

    /**
     * Remove all items referenced by this name
     *
     * @param  sName the service or cache name
     */
    public void remove(String sName)
        {
        m_mapByName.remove(sName);
        }

    // ----- inner classes --------------------------------------------------

    /**
     * SubjectScopedReference scopes (associates) an object with a Subject.
     *
     * @author dag 2010.02.25
     */
    protected class SubjectScopedReference
        {
        // ----- SubjectScopedReference methods -----------------------------

        /**
         * Obtain the object referenced by the current subject.
         *
         * @return the referenced object
         */
        protected synchronized Object get()
            {
            Subject subject = SecurityHelper.getCurrentSubject();

            return subject == null ? m_oRef : m_mapSubjectScope.get(subject);
            }

        /**
         * Determine if there are any referenced objects.
         *
         * @return whether there are any referenced objects.
         */
        protected synchronized boolean isEmpty()
            {
            Subject subject = SecurityHelper.getCurrentSubject();

            return subject == null ? m_oRef == null : m_mapSubjectScope.isEmpty();
            }

        /**
         * Add a referenced object based on the current subject.
         *
         * @param oRef  the referenced object
         */
        protected synchronized void set(Object oRef)
            {
            Subject subject = SecurityHelper.getCurrentSubject();

            if (subject == null)
                {
                m_oRef = oRef;
                }
            else
                {
                m_mapSubjectScope.put(subject, oRef);
                }
            }

        /**
         * Add a referenced object based on the current subject only if it does
         * not already exist.
         *
         * @param oRef  the referenced object
         *
         * @return the previous reference associated with the subject or null
         *         if the put succeeded
         */
        protected synchronized Object putIfAbsent(Object oRef)
            {
            Subject subject = SecurityHelper.getCurrentSubject();
            Object  oReturn = null;

            if (subject == null)
                {
                m_oRef = m_oRef == null ? oRef : (oReturn = m_oRef);
                }
            else
                {
                oReturn = m_mapSubjectScope.get(subject);

                if (oReturn == null)
                    {
                    m_mapSubjectScope.put(subject, oRef);
                    }
                }

            return oReturn;
            }

        /**
         * Remove the object referenced by the current subject.
         *
         * @return the previously referenced object
         */
        protected synchronized Object remove()
            {
            Subject subject = SecurityHelper.getCurrentSubject();

            return subject == null ? m_oRef = null : m_mapSubjectScope.remove(subject);
            }

        /**
         * Obtain all referenced objects.
         *
         * @return the Collection of referenced objects.
         */
        protected Collection values()
            {
            Object oRef = m_oRef;

            // return any subject scoped entries
            if (oRef == null)
                {
                return m_mapSubjectScope.values();
                }

            // a single non-subject scoped entry
            if (m_mapSubjectScope.isEmpty())
                {
                return Collections.singleton(oRef);
                }

            // both one or more subject scoped entries and a non-subject scoped entry
            Collection col = new HashSet(m_mapSubjectScope.values());
            col.add(oRef);
            return col;
            }

        // ----- data fields ------------------------------------------------

        /**
         * The map contains referenced objects keyed by Subject.
         */
        private Map m_mapSubjectScope = new WeakHashMap();

        /**
         * Reference stored when the subject is null.
         */
        private Object m_oRef;
        }

    // ----- data fields ----------------------------------------------------

    /**
     * When storing cache references, it is a map keyed by a cache name with
     * a corresponding value being a map keyed by a class loader with a
     * corresponding value being a NamedCache reference or a
     * SubjectScopedReference that contains a NamedCache reference.
     *
     * When storing service references, it is a map keyed by a service name
     * with a corresponding value being a service reference or a
     * SubjectScopedReference that contains a service reference.
     */
    protected SegmentedConcurrentMap m_mapByName = new SegmentedConcurrentMap();
    }
