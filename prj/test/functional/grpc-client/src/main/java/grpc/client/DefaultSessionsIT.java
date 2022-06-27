/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import com.oracle.coherence.client.GrpcRemoteSession;
import com.oracle.coherence.client.GrpcSessionConfiguration;
import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DefaultSessionsIT
    {
    @AfterEach
    public void cleanup()
        {
        Coherence.closeAll();
        }

    @Test
    public void shouldNotUseGrpcForDefaultSession() throws Exception
        {
        Coherence coherence = Coherence.client()
                .start()
                .get(5, TimeUnit.MINUTES);

        Session session = coherence.getSession();
        assertThat(session, is(instanceOf(ConfigurableCacheFactorySession.class)));
        }

    @Test
    public void shouldSpecificallyEnableGrpForDefaultSession() throws Exception
        {
        GrpcSessionConfiguration sessionConfiguration = GrpcSessionConfiguration.builder().build();

        CoherenceConfiguration   configuration        = CoherenceConfiguration.builder()
                .withSession(sessionConfiguration).build();

        Coherence coherence = Coherence.client(configuration)
                .start()
                .get(5, TimeUnit.MINUTES);

        Session session = coherence.getSession();
        assertThat(session, is(instanceOf(GrpcRemoteSession.class)));
        }
    }
