/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package tcmp;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.util.Base;
import com.tangosol.util.UID;
import com.oracle.coherence.testing.AbstractFunctionalTest;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;

import java.util.Properties;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.core.Is.is;

/**
 * Tests of the witness protocol.
 */
public class WitnessProtocolTests
        extends AbstractFunctionalTest
    {
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.override","witness-test-override.xml");

        AbstractFunctionalTest._startup();
        }

    /**
     * Test that client will be killed rather then server when the only comm trouble is between those two members.
     */
    @Test
    public void testKillClientBeforeServer()
        {
        int cServers = 3;
        Properties propsClient = new Properties();
        propsClient.put("coherence.distributed.localstorage", "false");
        propsClient.put("coherence.cluster", "testKillClientBeforeServer");
        Properties propsServer = new Properties();
        propsServer.put("coherence.cluster", "testKillClientBeforeServer");
        CoherenceClusterMember[] servers = ClusteringTests.startServers("server-witness", "witness", propsServer, cServers);
        CoherenceClusterMember   client  = startCacheServer("client-witness", "witness", null, propsClient);

        Eventually.assertThat(invoking(servers[cServers-1]).getClusterSize(), is(4));

        UID uidClient = client.getLocalMemberUID();
        client.invoke(new ConnectionDestroyer(2));

        Eventually.assertThat(invoking(servers[cServers-1]).getClusterMemberUIDs().contains(uidClient), is(false));

        for (int i = 0; i < cServers; i++)
            {
            servers[i].close();
            }
        client.close();
        ClusteringTests.stopServers("server-witness", cServers);
        AbstractFunctionalTest.stopCacheServer("client-witness");
        Base.sleep(10000);
        }

    /**
     * Test that a bad server will be killed
     */
    @Test
    public void testKillBadServer()
        throws Exception
        {
        int cServers = 3;
        Properties propsClient = new Properties();
        propsClient.put("coherence.distributed.localstorage", "false");
        propsClient.put("coherence.cluster", "testKillBadServer");
        Properties propsServer = new Properties();
        propsServer.put("coherence.cluster", "testKillBadServer");
        CoherenceClusterMember[] servers = ClusteringTests.startServers("server-witness2", "witness2", propsServer, cServers);
        Eventually.assertThat(invoking(servers[cServers-1]).getClusterSize(), is(3));

        CoherenceClusterMember badServer = startCacheServer("bad-server2", "witness2", null, propsServer);
        Eventually.assertThat(invoking(badServer).getClusterSize(), is(4));

        CoherenceClusterMember client = startCacheServer("client-witness2", "witness2", null, propsClient);
        Eventually.assertThat(invoking(client).getClusterSize(), is(5));

        UID uidServer = badServer.getLocalMemberUID();
        client.invoke(new ConnectionDestroyer(badServer.getLocalMemberId()));
        Thread.sleep(2000); // encourage either failure on this connection
        badServer.invoke(new ConnectionDestroyer(0)); // ensure witnesses will agree

        Eventually.assertThat(invoking(client).getClusterMemberUIDs().contains(uidServer), is(false));

        badServer.close();
        for (int i = 0; i < cServers; i++)
            {
            servers[i].close();
            }
        client.close();
        ClusteringTests.stopServers("server-witness2", cServers);
        AbstractFunctionalTest.stopCacheServer("client-witness2");
        }

    public static class ConnectionDestroyer
        implements RemoteCallable<Object>
        {
        ConnectionDestroyer(int nMember)
            {
            m_nMember = nMember;
            }

        @Override
        public Object call()
                throws Exception
            {
            Class  clzLibrary = Class.forName("com.tangosol.coherence.component.application.console.Coherence");
            Method metRun     = clzLibrary.getMethod("get_Instance", new Class[0]);
            Object oCoherence = metRun.invoke(null);
            Method metProc    = clzLibrary.getDeclaredMethod("processCommand", String.class);

            int nMember = m_nMember;
            return metProc.invoke(oCoherence, nMember == 0 ? "connector publisher unicast off" : "connector publisher member " + nMember + " off");
            }

        protected int m_nMember;
        }
    }
