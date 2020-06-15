/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package common;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.Member;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.SafeLinkedList;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


/**
* Abstract base class for rolling-restart functional testing
*
* @author rhl 2010.09.16
*/
public abstract class AbstractRollingRestartTest
        extends AbstractFunctionalTest
    {
    // ----- constructors -------------------------------------------------

    /**
    * Default constructor.
    */
    public AbstractRollingRestartTest()
        {
        super();
        }

    /**
    * Create a new AbstractRollingRestartTest that will use the cache
    * configuration file with the given path to instantiate NamedCache
    * instances.
    *
    * @param sPath  the configuration resource name or file path
    */
    public AbstractRollingRestartTest(String sPath)
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
    public AbstractRollingRestartTest(ConfigurableCacheFactory factory)
        {
        super(factory);
        }


    // ----- AbstractRollingRestartTest methods ---------------------------

    /**
    * Return a remote to be used in remote-killing servers.
    *
    * @return a Remote object
    */
    public Remote instantiateRemote()
        {
        // default implementation returns a singleton immutable impl
        return s_remoteDefault;
        }

    /**
    * Return the path to the cache-config to be used.
    *
    * @return the path to the cache-config to be used
    */
    public abstract String getCacheConfigPath();

    /**
    * Return the path to the Ant build file to be used to start and stop servers.
    *
    * @return the path to the Ant build file to be used
    */
    public abstract String getBuildPath();

    /**
    * Return the name of the test project.
    *
    * @return the name of the test project
    */
    public abstract String getProjectName();

    /**
    * Do the specified number of rolling restarts, subject to the specified
    * parameters.  Invoke the specified runnable before killing each member.
    * <p/>
    * Note: this method does nothing to ensure that running services have
    *       reached any "safe" (e.g. machine-safe, node-safe) state.  Tests
    *       that require that condition (e.g. to ensure no data-loss) are
    *       responsible for ensuring that in the specified runnable.
    *
    * @param memberHandler  the member handler
    * @param cIters         the number of restarts
    * @param runPreKill     the runnable to run prior to killing a server
    *
    * @return the number of restarts performed
    */
    public int doRollingRestart(MemberHandler memberHandler, final int cIters,
                                Runnable runPreKill)
        {
        Filter filter = new Filter()
            {
            public boolean evaluate(Object o)
                {
                return ++m_cIters >= cIters;
                }

            int m_cIters;
            };

        return doRollingRestart(memberHandler, filter, runPreKill);
        }

    /**
    * Do the specified number of rolling restarts, subject to the specified
    * parameters.  Invoke the specified runnable before killing each member.
    * <p/>
    * Note: this method does nothing to ensure that running services have
    *       reached any "safe" (e.g. machine-safe, node-safe) state.  Tests
    *       that require that condition (e.g. to ensure no data-loss) are
    *       responsible for ensuring that in the specified runnable.
    *
    * @param memberHandler  the member handler
    * @param filterDone     the filter which, when evaluated to true, stops
    *                       the rolling restarts
    * @param runPreKill     the runnable to run prior to killing a server
    *
    * @return the number of restarts performed
    */
    public int doRollingRestart(MemberHandler memberHandler, Filter filterDone, Runnable runPreKill)
        {
        int cIters = 0;
        while (!filterDone.evaluate(null))
            {
            if (runPreKill != null)
                {
                runPreKill.run();
                }

            memberHandler.bounce();

            ++cIters;
            }

        return cIters;
        }


    // ----- helpers ------------------------------------------------------

    /**
    * Send the specified {@link RemoteRunnable} to the specified member.
    *
    * @param member     the member to run the invocable on
    * @param invocable  the RemoteRunnable to run
    */
    protected void doInvoke(Member member, RemoteRunnable invocable)
        {
        CoherenceClusterMember clusterMember = findApplication(member.getRoleName());
        if (clusterMember != null)
            {
            clusterMember.submit(invocable);
            }
        }

    // ----- inner class: WaitForMachineSafeRunnable ----------------------

    /**
    * A runnable which will wait up to a specified amount of time for
    * a service to become "machine-safe"
    */
    public static class WaitForMachineSafeRunnable
            implements Runnable
        {
        public WaitForMachineSafeRunnable(CacheService service)
            {
            f_service = service;
            }

        // ----- Runnable methods -----------------------------------------

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            waitForMachineSafe(f_service);
            }

        // ----- data members ---------------------------------------------

        protected final CacheService f_service;
        }

    /**
    * Wait until the specified (partitioned) cache service reaches machine-safety.
    *
    * @param service   the partitioned cache to wait for machine-safety
    */
    public static void waitForMachineSafe(CacheService service)
        {
        SafeService serviceSafe = (SafeService) service;
        PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();

        Eventually.assertDeferred(() -> serviceReal.calculateVulnerable(true), is(0));
        }

    // ----- inner class: WaitForNodeSafeRunnable --------------------------

    /**
    * A runnable which will wait up to a specified amount of time for
    * a service to become "node-safe"
    */
    public static class WaitForNodeSafeRunnable
            implements Runnable
        {
        public WaitForNodeSafeRunnable(CacheService service)
            {
            f_service = service;
            }

        // ----- Runnable methods -----------------------------------------

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            waitForNodeSafe(f_service);
            }

        // ----- data members ---------------------------------------------

        protected final CacheService f_service;
        }

    /**
    * Wait until the specified (partitioned) cache service reaches node-safety.
    *
    * @param service   the partitioned cache to wait for node-safety
    */
    public static void waitForNodeSafe(CacheService service)
        {
        SafeService      serviceSafe = (SafeService) service;
        PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();

        Eventually.assertDeferred(() -> serviceReal.calculateEndangered(), is(0));
        }

    // ----- inner class: WaitForNoOrphansRunnable ------------------------

    /**
    * A runnable which will wait up to a specified amount of time for
    * a service to become "node-safe"
    */
    public static class WaitForNoOrphansRunnable
            implements Runnable
        {
        public WaitForNoOrphansRunnable(CacheService service)
            {
            f_service = service;
            }

        // ----- Runnable methods -----------------------------------------

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            waitForNoOrphans(f_service);
            }

        // ----- data members ---------------------------------------------

        protected final CacheService f_service;
        }

    /**
    * Wait until the specified (partitioned) cache service has no orphaned
    * partitions.
    *
    * @param service   the partitioned cache
    */
    public static void waitForNoOrphans(CacheService service)
        {
        SafeService      serviceSafe = (SafeService) service;
        PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();

        Eventually.assertDeferred(() -> serviceReal.calculateOwnership(null, true), new IntArraySlotMatcher(0, 0));
        }

    /**
    * Wait until the specified (partitioned) cache service has finished
    * partition balancing.
    *
    * @param service   the partitioned cache
    */
    public static void waitForBalanced(CacheService service)
        {
        SafeService      serviceSafe = (SafeService) service;
        PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();

        Eventually.assertDeferred(() -> serviceReal.calculateUnbalanced(), is(0));
        }

    /**
     * Matcher for int array.
     */
    protected static class IntArraySlotMatcher
        extends BaseMatcher<int[]>
        {
        public IntArraySlotMatcher(int nIndex, int nValue)
            {
            f_nIndex = nIndex;
            f_nValue = nValue;
            }

        @Override
        public boolean matches(Object o)
            {
            return ((int[]) o)[f_nIndex] == f_nValue;
            }

        @Override
        public void describeTo(Description description)
            {
            }

        protected final int f_nIndex;
        protected final int f_nValue;
        }

    // ----- inner class: MemberHandler -----------------------------------

    /**
    * MemberHandler is used to manage the creation and destruction of
    * cluster members by the rolling restart test.
    */
    public class MemberHandler
            implements MemberListener
        {
        // ----- constructors ---------------------------------------------

        /**
        * Create and register a MemberHandler for the specified cluster.
        *
        * @param cluster  the cluster to register a MemberHandler for
        * @param sPrefix  the string prefix to use for created members
        */
        public MemberHandler(Cluster cluster, String sPrefix)
            {
            this(cluster, sPrefix, true, true);
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
        * @param fGracefulKill  true iff the members should be killed
        *                       by the handler gracefully
        */
        public MemberHandler(Cluster cluster, String sPrefix,
                             boolean fExternalKill, boolean fGracefulKill)
            {
            this(cluster, sPrefix, fExternalKill, fGracefulKill, new Properties());
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
        * @param fGracefulKill  true iff the members should be killed
        *                       by the handler gracefully
        * @param props          the default set of properties to pass to
        *                       servers started by this member handler
        */
        public MemberHandler(Cluster cluster, String sPrefix,
                             boolean fExternalKill, boolean fGracefulKill,
                             Properties props)
            {
            m_cluster       = cluster;
            m_sPrefix       = sPrefix;
            m_fExternalKill = fExternalKill;
            m_fGracefulKill = fGracefulKill;
            m_props         = props;

            cluster.getService("Cluster").addMemberListener(this);
            }

        // ----- MemberHandler methods ------------------------------------

        /**
         * Return a copy of the default properties to pass to new servers.
         *
         * @return a copy of the default properties
         */
        public Properties ensureProperties()
            {
            Properties props = m_props;

            return props == null ? new Properties() : (Properties) props.clone();
            }

        /**
        * Dispose of this MemberHandler.
        */
        public void dispose()
            {
            RuntimeException exception = null;
            for (int i = 0, cServers = m_listServers.size(); i < cServers; i++)
                {
                try
                    {
                    killOldestServer(/*fExternalKill*/ true, /*fGracefulKill*/ false);
                    }
                catch (RuntimeException e)
                    {
                    if (exception == null)
                        {
                        exception = e;
                        }
                    }
                }

            m_cluster.getService("Cluster").removeMemberListener(this);

            if (exception != null)
                {
                throw exception;
                }
            }

        /**
        * Return the Cluster object.
        *
        * @return the Cluster object
        */
        protected Cluster getCluster()
            {
            return m_cluster;
            }

        /**
        * Return the server name prefix.
        *
        * @return the server name prefix
        */
        public String getPrefix()
            {
            return m_sPrefix;
            }

        /**
        * Perform a "bounce", killing and restarting members.
        */
        public void bounce()
            {
            killOldestServer();
            addServer();
            }

        /**
        * Start a cache server.
        *
        * @return the new Member
        */
        public Member addServer()
            {
            return addServer(null);
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

            startCacheServer(sServerName, getProjectName(),
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
                }
            return memberNew;
            }

        /**
         * Return the oldest member.
         *
         * @return the oldest member, or null
         */
        public Member getOldestMember()
            {
            List listServers = m_listServers;

            return listServers.isEmpty() ? null : (Member) listServers.get(0);
            }

        /**
        * Kill the oldest server.
        */
        public void killOldestServer()
            {
            killOldestServer(m_fExternalKill, m_fGracefulKill);
            }

        /**
        * Kill the oldest server.
        *
        * @param fExternalKill  true iff the server should be killed externally
        *                       (e.g. kill -9)
        * @param fGracefulKill  true iff the server should be killed gracefully
        */
        public void killOldestServer(boolean fExternalKill, boolean fGracefulKill)
            {
            killServer((Member) m_listServers.get(0), fExternalKill, fGracefulKill);
            }

        /**
        * Kill the specified member, and wait for it to be removed from the cluster.
        */
        protected void killServer(Member memberKill)
            {
            killServer(memberKill, m_fExternalKill, m_fGracefulKill);
            }

        /**
        * Kill the specified member, and wait for it to be removed from the cluster.
        *
        * @param fExternalKill  true iff the server should be killed externally
        *                       (e.g. kill -9)
        * @param fGracefulKill  true iff the server should be killed gracefully
        */
        protected void killServer(Member memberKill, boolean fExternalKill, boolean fGracefulKill)
            {
            if (fExternalKill)
                {
                stopCacheServer(memberKill.getRoleName(),
                                fGracefulKill);
                }
            else
                {
                doInvoke(memberKill, new RemoteKillRunnable(instantiateRemote()));
                }

            // wait for server death to be noticed
            Eventually.assertDeferred(() -> m_listServers.contains(memberKill), is(false));
            }

        /**
        * {@inheritDoc}
        */
        public void memberJoined(MemberEvent evt)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void memberLeaving(MemberEvent evt)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void memberLeft(MemberEvent evt)
            {
            synchronized (m_listServers)
                {
                m_listServers.remove(evt.getMember());
                m_listServers.notifyAll();
                }
            }

        // ----- data members ---------------------------------------------

        /**
        * The Cluster object.
        */
        protected Cluster    m_cluster;

        /**
        * The server name prefix.
        */
        protected String     m_sPrefix;

        /**
        * A non-repeating unique index for created cache servers.
        */
        protected int        m_nNonce      = 0;

        /**
        * A list of the active cache servers.
        */
        protected List m_listServers = new SafeLinkedList();

        /**
         * True iff members killed by the handler should be killed externally
         * (e.g. kill -9)
         */
        protected boolean    m_fExternalKill;

        /**
         * True iff members should be stopped by the handler gracefully.
         */
        protected boolean    m_fGracefulKill;

        /**
         * The default properties to pass to new members.
         */
        protected Properties m_props;
        }


    // ----- inner class: AbstractRemoteInvocable -------------------------

    /**
    * Abstract class for invocable
    */
    protected static abstract class AbstractRemoteRunnable
            implements RemoteRunnable
        {
        // ----- constructors ---------------------------------------------

        /**
        * Construct an AbstractRemoteInvocable with the specified remote.
        *
        * @param remote  the Remote object
        */
        protected AbstractRemoteRunnable(Remote remote)
            {
            m_remote = remote;
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the Remote.
        *
        * @return the Remote
        */
        public Remote getRemote()
            {
            return m_remote;
            }

        // ----- data members ---------------------------------------------

        /**
        * The Remote object.
        */
        public Remote m_remote;
        }


    // ----- inner class: RemoteKillInvocable -----------------------------

    /**
    * RemoteKillInvocable is used to signal a cache server to kill itself.  The
    * effect of running this invocable will be to ensure that isRemoteKill()
    * will evaluate true.
    * <p/>
    * Note: it is the responsibility of the test to check for this condition
    */
    protected static class RemoteKillRunnable
            extends AbstractRemoteRunnable
        {
        // ----- constructors ---------------------------------------------

        /**
        * Construct a RemoteKillInvocable with the specified remote.
        *
        * @param remote  the Remote object
        */
        protected RemoteKillRunnable(Remote remote)
            {
            super(remote);
            }

        // ----- Invocable methods ----------------------------------------

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            getRemote().signalRemoteKill();
            }
        }


    // ----- inner class: Remote ------------------------------------------

    /**
    *
    */
    public static class Remote
            implements ExternalizableLite
        {
        /**
        * {@inheritDoc}
        */
        public void readExternal(DataInput in) throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(DataOutput out) throws IOException
            {
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return true iff this server has been remotely-killed.
        *
        * @return true iff this server has been remotely-killed
        */
        public boolean isRemoteKill()
            {
            return Base.equals(
                System.getProperty("test.AbstractRollingRestartTest.kill.signal"), "true");
            }

        /**
        * Signal that a remote kill has been ordered.
        */
        public void signalRemoteKill()
            {
            out("AbstractRollingRestartTest: setting the kill signal");
            System.setProperty("test.AbstractRollingRestartTest.kill.signal", "true");
            }
        }

    // ----- data members -------------------------------------------------

    /**
    * Singleton instance of the default implementation of a remote.
    */
    public static final Remote s_remoteDefault = new Remote();
    }