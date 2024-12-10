/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import org.junit.Assume;
import org.junit.BeforeClass;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import java.net.URI;

import static org.hamcrest.CoreMatchers.is;

/**
 * Tests Management over REST when the http server is running using JAX-RS
 * in the same way that WLS would configure it.
 */
public class WlsManagementInfoResourceTests
        extends BaseManagementInfoResourceTests
    {
    @BeforeClass
    public static void _startup()
        {
        Assume.assumeThat(Boolean.getBoolean("test.security.enabled"), is(false));
        
        startTestCluster(WLSManagementStub.class, CLUSTER_NAME, ClassPath.ofSystem());

        for (CoherenceClusterMember member : s_cluster)
            {
            Eventually.assertDeferred(() -> member.invoke(WLSManagementStub::isRunning), is(true));
            }
        }

    @Override
    public WebTarget getBaseTarget(Client client)
        {
        try
            {
            if (m_baseURI == null)
                {
                CoherenceClusterMember member = s_cluster.getAny();
                int nPort = member.invoke(WLSManagementStub::getHttpPort);
                m_baseURI = URI.create("http://127.0.0.1:" + nPort + URI_ROOT + "/coherence/latest/clusters/" + CLUSTER_NAME);

                Logger.info("Management HTTP Acceptor lookup returned: " + m_baseURI);
                }
            return client.target(m_baseURI);
            }
        catch(Throwable t)
            {
            throw Exceptions.ensureRuntimeException(t);
            }
        }

    // ----- constants ------------------------------------------------------

    public static final String URI_ROOT = "/wls";
    }
