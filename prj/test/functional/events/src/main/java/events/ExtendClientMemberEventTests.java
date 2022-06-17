/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CoherenceSession;
import com.tangosol.net.Member;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;
import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.UUID;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.Serializable;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.CoreMatchers;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static com.tangosol.net.cache.TypeAssertion.withTypes;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;


/**
 * Functional test that MemberAware services are dispatched extend client join/leave member events.
 *
 * @author jf  2022/03/28
 */
public class ExtendClientMemberEventTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ----------------------------------------------------

    public ExtendClientMemberEventTests()
        {
        super(SERVER_CACHE_CONFIG);
        }

    @BeforeClass
    public static void _startup()
        {
        setupProps();
        System.setProperty("coherence.cacheconfig", SERVER_CACHE_CONFIG);
        System.setProperty("coherence.member", "TestFrameworkClient");
        System.setProperty("coherence.role",   "TestFrameworkClient");
        System.setProperty("coherence.distributed.localstorage", "false");

        // test client does not join cluster for now
        Properties props = new Properties();
        props.setProperty("coherence.messaging.debug", "true");
        for (int i = 0; i < CLUSTER_SIZE; i++)
            {
            props.setProperty("coherence.member", "ExtendClientMemberLeftServer-" + i);
            System.setProperty("coherence.role",  "ExtendClientMemberLeftServer-" + i);

            CoherenceClusterMember member = startCacheServer("ExtendClientMemberLeftServer-" + i, "cache", SERVER_CACHE_CONFIG, props);
            s_lstMembers.add(member);
            }

        for (CoherenceClusterMember member : s_lstMembers)
            {
            Eventually.assertThat(invoking(member).isServiceRunning("DistributedCacheJava"), CoreMatchers.is(true), within(2, TimeUnit.MINUTES));
            Eventually.assertThat(invoking(member).isServiceRunning("ExtendTcpProxyService"), CoreMatchers.is(true), within(2, TimeUnit.MINUTES));
            Eventually.assertThat(invoking(member).isOperational(), CoreMatchers.is(true), within(2, TimeUnit.MINUTES));
            }
        }

    @AfterClass
    public static void cleanup()
        {
        stopAllApplications();
        }

    @After
    public void cleanupAfterEachTest()
        {
        NamedCache<UUID, List> mapResults = CacheFactory.getConfigurableCacheFactory().
                ensureTypedCache("ExtendClientMemberListenerResultMap",
                                 null,
                                 withTypes(UUID.class, List.class));
        mapResults.truncate();
        CacheFactory.shutdown();
        }

    // ---- tests ------------------------------------------------------------

    @Test
    public void testExtendMemberEvents() throws InterruptedException
        {
        String PREFIX = "TestExtendClientMember_";

        Properties props = new Properties();
        props.setProperty("coherence.tcmp.enabled", "false");
        props.setProperty("coherence.member", PREFIX + "1");
        props.setProperty("coherence.messaging.debug", "true");

        List<String> lstExtendClientMember = new LinkedList<>();
        int          expectedResult        = 0;
        for (int i=0; i < NUM_CLIENTS; i++)
            {
            String sMemberName = PREFIX + i;

            props.setProperty("coherence.member", sMemberName);
            if (i == NUM_CLIENTS - 1)
                {
                // simulate extend client terminating abnormally
                expectedResult = 25;
                props.setProperty("test.termination.value", Integer.toString(expectedResult));
                }

            CoherenceClusterMember member = startCacheApplication(sMemberName,
                                                                  "events.ExtendClientMemberEventTests$ExtendClient",
                                                                  "events", CLIENT_CACHE_CONFIG, props);
            lstExtendClientMember.add(sMemberName);

            int result = member.waitFor();

            assertThat("validate how extend client application terminated",
                       result, is(expectedResult));
            }

        // join as a test framework client and validate memberevents recorded in resultmap
        NamedCache<UUID, List> mapResults = CacheFactory.getConfigurableCacheFactory().
                ensureTypedCache("ExtendClientMemberListenerResultMap",
                                 null,
                                 withTypes(UUID.class, List.class));
        assertThat(mapResults.size(), is(NUM_CLIENTS));
        for (Map.Entry<UUID, List> entry : mapResults.entrySet())
            {
            UUID uuid = entry.getKey();
            List<MemberEventResult> lstEvent = entry.getValue();

            try
                {
                validateMemberEventsForExtendClientMember(lstEvent);
                }
            catch (Throwable t)
                {
                // assist debugging failures
                Logger.info("Processing extend client member name=" + lstEvent.get(0).m_event.getMember().getMemberName() +
                            " uuid=" + uuid);
                for (MemberEventResult result : lstEvent)
                    {
                    Logger.info("       " + result);
                    }
                throw t;
                }
            }
        }

    @Test
    public void testMemberEvents() throws InterruptedException
        {
        Properties props = new Properties();
        props.setProperty("coherence.role", "client");
        props.setProperty("coherence.member", "TestClientMember_1");
        props.setProperty("coherence.messaging.debug", "true");
        props.setProperty("coherence.distributed.storage.enabled", "false");

        CoherenceClusterMember member = startCacheApplication("TestClientMember_1",
                                                              "events.ExtendClientMemberEventTests$MemberClient",
                                                              "events", SERVER_CACHE_CONFIG, props);
        member.waitFor();

        props.setProperty("coherence.member", "TestClientMember_2");
        member = startCacheApplication("TestClientMember_2",
                                       "events.ExtendClientMemberEventTests$MemberClient",
                                       "events", SERVER_CACHE_CONFIG, props);
        member.waitFor();
        }

    // ----- inner class: ExtendClient ---------------------------------------

    public static class ExtendClient
            implements java.io.Serializable
        {
        public static void main(String[] args)
            {
            int nTerminationValue = Integer.getInteger("test.termination.value", 0);

            try (CoherenceSession session = new CoherenceSession())
                {
                NamedCache nc = session.getCache("dist-extend-direct-java");
                nc.truncate();
                nc.put("key1", "string1");
                nc.get("key1");
                }
            catch (Throwable t)
                {
                System.out.println("Unexpected exception " + t.getMessage());
                Runtime.getRuntime().exit(1);
                }

            if (nTerminationValue != 0)
                {
                System.err.println("non-graceful termination");
                Runtime.getRuntime().halt(nTerminationValue);
                }
            }
        }

    // ----- inner class: MemberClient ---------------------------------------

    public static class MemberClient
            implements java.io.Serializable
        {
        public static void main(String[] args)
            {
            int nTerminationValue = 0;

            try (CoherenceSession session = new CoherenceSession())
                {
                NamedCache nc = session.getCache("dist-java");
                nc.truncate();
                nc.put("key1", "string1");
                nc.get("key1");
                }
            catch (Throwable t)
                {
                System.out.println("Unexpected exception " + t.getMessage());
                Runtime.getRuntime().exit(1);
                }

            if (nTerminationValue != 0)
                {
                System.out.println("non-graceful termination");
                Runtime.getRuntime().halt(nTerminationValue);
                }
            }
        }
    // ----- inner class: ExtendClientMemberListener -------------------------

    public static class ExtendClientMemberListener
            implements MemberListener, Serializable
        {
        public ExtendClientMemberListener()
            {
            if (s_mapResults == null)
                {
                s_mapResults = CacheFactory.getConfigurableCacheFactory().
                        ensureTypedCache("ExtendClientMemberListenerResultMap",
                                         null,
                                         withTypes(UUID.class, List.class));
                }
            }

        public void memberJoined(MemberEvent evt)
            {
            Member member = evt.getMember();

            if (member.isRemoteClient())
                {
                Logger.info("ExtendClientMemberListener joined : svc=" + evt.getService().getInfo().getServiceName() + " evt= " + evt + " uuid=" + evt.getMember().getUid());

                s_mapResults.invoke(member.getUuid(),
                                    new AddMemberEventMapEntryProcessor(new MemberEventResult(evt, CacheFactory.getCluster().getLocalMember().getMemberName())));
                }
            }

        public void memberLeaving(MemberEvent evt)
            {
            // not applicable for remote client
            }

        public void memberLeft(MemberEvent evt)
            {
            Member member = evt.getMember();
            if (member.isRemoteClient())
                {
                String sExecutingMember = CacheFactory.getCluster().getLocalMember().getMemberName();

                Logger.info("ExtendClientMemberListener left " + evt.getService().getInfo().getServiceName() + " executing on member " + sExecutingMember + " : evt= " + evt +  " uuid=" + evt.getMember().getUid());
                s_mapResults.invoke(member.getUuid(),
                                    new AddMemberEventMapEntryProcessor(new MemberEventResult(evt, CacheFactory.getCluster().getLocalMember().getMemberName())));
                }
            }

        static private NamedCache<com.tangosol.util.UUID, List> s_mapResults = null;
        }


    // ----- inner class: MemberListener ------------------------------------

    public static class ClientMemberListener
            implements MemberListener, Serializable
        {
        public ClientMemberListener()
            {
            }

        public void memberJoined(MemberEvent evt)
            {
            Logger.info("ClientMemberListener joined : evt= " + evt + " member=" + evt.getMember());
            }

        public void memberLeaving(MemberEvent evt)
            {
            Logger.info("ClientMemberListener leaving : evt= " + evt + " member=" + evt.getMember());
            }

        public void memberLeft(MemberEvent evt)
            {
            Logger.info("ClientMemberListener left : evt= " + evt + " member=" + evt.getMember());
            }
        }

    // ----- helpers ---------------------------------------------------------

    private void validateMemberEventsForExtendClientMember(List<MemberEventResult> lst)
        {
        String sExtendClientName = lst.get(0).m_event.getMember().getMemberName();

        assertThat("extend client member " + sExtendClientName +
                  " assert number of MemberEvents(JOIN & LEFT) per extend client per service members",
                  lst.size(), is(2 * CLUSTER_SIZE));
        assertThat("validate extend client MemberJoin for " + sExtendClientName,
                   lst.stream().filter(r -> r.m_event.getId() == MemberEvent.MEMBER_JOINED).count(), greaterThanOrEqualTo((long) CLUSTER_SIZE));
        assertThat("validate extend client MemberLeft for " + sExtendClientName,
                   lst.stream().filter(r -> r.m_event.getId() == MemberEvent.MEMBER_LEFT).count(), greaterThanOrEqualTo((long) CLUSTER_SIZE));
        for (CoherenceClusterMember member : s_lstMembers)
            {
            // Bedrock CoherenceClusterMember does not expose getMemberName but it sets MemberName and RoleName the same, so use getRoleName
            assertThat("validate " + sExtendClientName + " MemberListener join/left executed on storage enabled server " + member.getRoleName(),
                       lst.stream().filter(r -> r.m_sExecutingServerName.equals(member.getRoleName())).count(), greaterThanOrEqualTo((2L)));
            }
        }

    // ----- inner class: AddMemberEventMapEntryProcessor --------------------

    public static class AddMemberEventMapEntryProcessor
            extends AbstractProcessor<UUID, List, Boolean>
            implements Serializable
        {
        // ----- constructors ------------------------------------------------

        // for serialization
        public AddMemberEventMapEntryProcessor()
            {
            }

        public AddMemberEventMapEntryProcessor(MemberEventResult result)
            {
            m_result = result;
            }

        // ----- InvocableMap.EntryProcessor methods -------------------------

        public Boolean process(InvocableMap.Entry<UUID, List> entry)
            {
            List<MemberEventResult> list = entry.getValue();
            if (list == null)
                {
                list = new LinkedList<>();
                }

            list.add(m_result);
            entry.setValue(list);
            return true;
            }

        // ----- data members -------------------------------------------------
        private MemberEventResult m_result;
        }

    // ----- inner class: MemberEventResult ----------------------------------

    static public class MemberEventResult implements Serializable
        {
        public MemberEventResult()
            {
            }

        public MemberEventResult(MemberEvent event, String sExecutingServerName)
            {
            m_sServiceName         = event.getService().getInfo().getServiceName();
            m_event                = event;
            m_sExecutingServerName = sExecutingServerName;
            }

        public String getServiceName()
            {
            return m_sServiceName;
            }

        public MemberEvent getEvent()
            {
            return m_event;
            }

        public String getExecutingServiceName()
            {
            return m_sExecutingServerName;
            }

        public String getEventType()
            {
            switch (m_event.getId())
                {
                case MemberEvent.MEMBER_JOINED:
                    return "JOINED";
                case MemberEvent.MEMBER_LEFT:
                    return "LEFT";
                case MemberEvent.MEMBER_LEAVING:
                    return "LEAVING";
                default:
                    return "UNKNOWN_ID:" + m_event.getId();
                }
            }

        public String toString()
            {
            return "MemberEvent " + getEventType() + " from service " + m_sServiceName +
                   " Member: " + m_event.getMember() + " executed on server " + m_sExecutingServerName;
            }

        private String      m_sServiceName;
        private String      m_sExecutingServerName;
        private MemberEvent m_event;
        }

    // ----- constants ------------------------------------------------------

    public static final String CLIENT_CACHE_CONFIG = "client-cache-config.xml";

    public static final String SERVER_CACHE_CONFIG = "server-cache-config-with-extend-client-listener.xml";

    private static final AtomicInteger s_count = new AtomicInteger();

    private static final int CLUSTER_SIZE = 3;

    private static final int NUM_CLIENTS = 4;

    // ----- data members ---------------------------------------------------

    private static List<CoherenceClusterMember> s_lstMembers = new LinkedList<>();

    @ClassRule
    public static TestLogs s_testLogs = new TestLogs(ExtendClientMemberEventTests.class);
    }
