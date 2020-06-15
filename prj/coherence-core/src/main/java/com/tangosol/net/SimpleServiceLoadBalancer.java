/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import com.tangosol.util.Base;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A simple ServiceLoadBalancer implementation.
 *
 * @author jh  2010.12.07
 */
public class SimpleServiceLoadBalancer<S extends Service, T extends ServiceLoad>
        extends AbstractServiceLoadBalancer<S, T>
    {
    /**
     * Default constructor.
     */
    public SimpleServiceLoadBalancer()
        {
        this(null);
        }

    /**
     * Create a new SimpleServiceLoadBalancer that will order
     * ServiceLoad objects using the specified Comparator. If null, the
     * natural ordering of the ServiceLoad objects will be used.
     *
     * @param comparator  the Comparator used to order ServiceLoad
     *                    objects
     */
    public SimpleServiceLoadBalancer(Comparator<T> comparator)
        {
        m_mapLoad   = new HashMap<>();
        m_mapMember = new TreeMap<>(comparator);
        }

    // ----- ServiceLoadBalancer interface ---------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void update(Member member, T load)
        {
        if (load == null && isLocalMember(member))
            {
            // we have left the cluster
            m_mapLoad.clear();
            m_mapMember.clear();
            return;
            }

        Map<Member, T>       mapLoad   = m_mapLoad;
        Map<T, List<Member>> mapMember = m_mapMember;
        T                    loadOld   = mapLoad.remove(member);

        if (loadOld != null)
            {
            List<Member> listMembersOld = mapMember.get(loadOld);
            if (listMembersOld != null && listMembersOld.remove(member) &&
                    listMembersOld.isEmpty())
                {
                mapMember.remove(loadOld);
                }
            }

        if (load != null)
            {
            mapLoad.put(member, load);
            List<Member> listMembers = mapMember.get(load);
            if (listMembers == null)
                {
                mapMember.put(load, listMembers = new ArrayList<>());
                }
            listMembers.add(member);
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<Member> getMemberList(Member client)
        {
        SortedMap<T, List<Member>> mapMember = m_mapMember;
        if (mapMember.isEmpty())
            {
            return null;
            }

        List<Member> list = new ArrayList<>();
        for (List<Member> listMember : mapMember.values())
            {
            list.addAll(Base.randomize(listMember));
            }

        return list;
        }

    @Override
    public int compare(T load1, T load2)
        {
        Comparator<? super T> comparator = m_mapMember.comparator();
        return comparator == null
            ? load1.compareTo(load2)
            : comparator.compare(load1, load2);
        }

    // ----- data members ---------------------------------------------------

    /**
     * A Map of ServiceLoad objects keyed by their associated Member.
     */
    protected final Map<Member, T> m_mapLoad;

    /**
     * A SortedMap of List of Member objects keyed by their associated
     * ServiceLoad.
     */
    protected final SortedMap<T, List<Member>> m_mapMember;
    }
