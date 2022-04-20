/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing;


import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.Member;
import com.tangosol.net.MemberEvent;

import com.tangosol.util.Base;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;


public abstract class AbstractMachineRollingRestartTest
        extends AbstractRollingRestartTest
    {
    // ----- constructors -------------------------------------------------

    /**
    * Default constructor.
    */
    public AbstractMachineRollingRestartTest()
        {
        super();
        }

    /**
    * Create a new AbstractMachineRollingRestartTest that will use the cache
    * configuration file with the given path to instantiate NamedCache
    * instances.
    *
    * @param sPath  the configuration resource name or file path
    */
    public AbstractMachineRollingRestartTest(String sPath)
        {
        super(sPath);
        }

    /**
    * Create a new AbstractRollingRestartTest that will use the given
    * factory to instantiate NamedCache instances.
    *
    * @param factory  the ConfigurableCacheFactory used to instantiate
    *                 NamedCache instances
    */
    public AbstractMachineRollingRestartTest(ConfigurableCacheFactory factory)
        {
        super(factory);
        }


    // ----- inner class: MemberHandler ------------------------------------

    /**
     * MemberHandler implementation that is "machine-aware".
     */
    public class MemberHandler
            extends AbstractRollingRestartTest.MemberHandler
        {
        // ----- constructors ----------------------------------------------

        /**
        * Create and register a MemberHandler for the specified cluster.
        *
        * @param cluster  the cluster to register a MemberHandler for
        * @param sPrefix  the string prefix to use for created members
        */
        public MemberHandler(Cluster cluster, String sPrefix)
            {
            super(cluster, sPrefix);
            }

        /**
        * Create and register a MemberHandler for the specified cluster. If
        * fExternal is not specified, an invocable will execute on the member
        * to be killed which will ensure that isRemoteKill() will evaluate true.
        *
        * @param cluster        the cluster to register a MemberHandler for
        * @param sPrefix        the string prefix to use for created members
        * @param fExternalKill  true iff members killed by the handler should
        *                       be killed "externally" (e.g. kill -9)
        * @param fGraceful      true iff the members should be added or killed
        *                       by the handler gracefully
        */
        public MemberHandler(Cluster cluster, String sPrefix,
                             boolean fExternalKill, boolean fGraceful)
            {
            super(cluster, sPrefix, fExternalKill, fGraceful);
            }

        /**
        * Create and register a MemberHandler for the specified cluster. If
        * fExternal is not specified, an invocable will execute on the member
        * to be killed which will ensure that isRemoteKill() will evaluate true.
        *
        * @param cluster        the cluster to register a MemberHandler for
        * @param sPrefix        the string prefix to use for created members
        * @param fExternalKill  true iff members killed by the handler should
        *                       be killed "externally" (e.g. kill -9)
        * @param fGracefulKill  true iff the members should be added or killed
        *                       by the handler gracefully
        * @param props          the default set of properties to pass to
        *                       servers started by this member handler
        */
        public MemberHandler(Cluster cluster, String sPrefix,
                             boolean fExternalKill, boolean fGracefulKill,
                             Properties props)
            {
            super(cluster, sPrefix, fExternalKill, fGracefulKill, props);
            }


        // ----- MemberHandler methods -------------------------------------

        /**
         * Add a cache server on the specified (psuedo) machine.
         *
         * @param sMachine  the machine name
         *
         * @return the new Member
         */
        public Member addServer(String sMachine)
            {
            Properties props = new Properties();
            props.put("tangosol.coherence.machine", sMachine);

            return addServer(props);
            }

        /**
         * Start a cache server.
         *
         * @param props  the properties to start the server with, may be null
         *
         * @return the new Member
         */
        public Member addServer(Properties props)
            {
            String     sServerName = getPrefix() + m_nNonce++;
            Properties propsAll    = ensureProperties();

            if (props != null)
                {
                propsAll.putAll(props);
                }

            Cluster cluster = getCluster();
            int     nSize   = cluster.getMemberSet().size();

            CoherenceClusterMember clusterMember = startCacheServer(sServerName, getProjectName(),
                    getCacheConfigPath(), propsAll, true);

            Eventually.assertDeferred(() -> cluster.getMemberSet().size(), is(nSize + 1));

            Set setMembers = cluster.getMemberSet();
            Member memberNew  = null;
            for (Iterator iterMembers = setMembers.iterator(); iterMembers.hasNext(); )
                {
                Member member = (Member) iterMembers.next();
                if (Base.equals(sServerName, member.getRoleName()))
                    {
                    memberNew = member;
                    break;
                    }
                }

            assertNotNull(memberNew);

            synchronized (m_listServers)
                {
                m_listServers.add(memberNew);
                m_mapClusterMembers.put(memberNew.getId(), clusterMember);
                }
            return memberNew;
            }

        /**
         * Kill all members on the specified (psuedo) machine.
         *
         * @param sMachine  the machine name
         */
        public int killAll(String sMachine)
            {
            // do this in 2 passes to avoid co-mod errors (see #onMemberLeft)
            List<Member> listKill = new LinkedList<Member>();
            for (Iterator iter = m_listServers.iterator(); iter.hasNext(); )
                {
                Member member = (Member) iter.next();
                if (Base.equals(member.getMachineName(), sMachine))
                    {
                    listKill.add(member);
                    }
                }

            for (Member member : listKill)
                {
                killServer(member);
                }

            return listKill.size();
            }

        // ----- AbstractRollingRestartTest methods ------------------------

        /**
         * {@inheritDoc}
         */
        public void bounce()
            {
            // kill all of the servers on one of the machines.  Pick
            // the machine of the first member in the list.
            Member memberFirst = (Member) m_listServers.get(0);
            String sMachine    = memberFirst.getMachineName();

            int cKilled = killAll(sMachine);
            for (int i = 0; i < cKilled; i++)
                {
                addServer(sMachine);
                }
            }

        /**
         * {@inheritDoc}
         */
        public void memberLeft(MemberEvent evt)
            {
            synchronized (m_listServers)
                {
                Member member = evt.getMember();
                m_listServers.remove(member);
                m_mapClusterMembers.remove(member.getId());

                m_listServers.notifyAll();
                }
            }

        // ----- data members ------

        /**
         * List of CoherenceClusterMember.
         */
        protected Map<Integer, CoherenceClusterMember> m_mapClusterMembers = new HashMap<>();
        }
    }