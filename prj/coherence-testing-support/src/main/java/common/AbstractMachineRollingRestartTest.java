/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package common;


import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.Member;

import com.tangosol.util.Base;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;


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
        }
    }