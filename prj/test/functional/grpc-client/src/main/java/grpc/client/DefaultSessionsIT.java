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
        System.clearProperty(PROP_ENABLED);
        Coherence.closeAll();
        }

    @Test
    public void shouldUseGrpcForDefaultSession() throws Exception
        {
        Coherence coherence = Coherence.client()
                .start()
                .get(5, TimeUnit.MINUTES);

        Session session = coherence.getSession();
        assertThat(session, is(instanceOf(GrpcRemoteSession.class)));
        }

    @Test
    public void shouldNotUseGrpcForDefaultSession() throws Exception
        {
        System.setProperty(PROP_ENABLED, "false");

        Coherence coherence = Coherence.client()
                .start()
                .get(5, TimeUnit.MINUTES);

        Session session = coherence.getSession();
        assertThat(session, is(instanceOf(ConfigurableCacheFactorySession.class)));
        }

    @Test
    public void shouldSpecificallyEnableGrpForDefaultSession() throws Exception
        {
        System.setProperty(PROP_ENABLED, "true");

        Coherence coherence = Coherence.client()
                .start()
                .get(5, TimeUnit.MINUTES);

        Session session = coherence.getSession();
        assertThat(session, is(instanceOf(GrpcRemoteSession.class)));
        }

    public static final String PROP_ENABLED = String.format(GrpcSessionConfiguration.PROP_SESSION_ENABLED, "default");
    }
