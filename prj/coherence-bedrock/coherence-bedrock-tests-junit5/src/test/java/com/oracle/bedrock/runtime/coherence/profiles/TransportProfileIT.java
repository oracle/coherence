/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.bedrock.runtime.coherence.profiles;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TransportProfileIT
    {
    @Test
    public void shouldSetTransport()
        {
        TransportProfile profile = new TransportProfile("datagram");

        try (CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class,
                profile,
                ClusterName.of("shouldSetTransport"),
                WellKnownAddress.loopback(),
                LocalHost.only()))
            {
            Eventually.assertDeferred(member::isCoherenceRunning, is(true));
            assertThat(member.invoke(() -> System.getProperty(TransportProfile.PROP_TRANSPORT)), is("datagram"));
            }
        }

    @Test
    public void shouldNotSetTransportIfAlreadySet()
        {
        TransportProfile profile = new TransportProfile("datagram");

        try (CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class,
                profile,
                SystemProperty.of(TransportProfile.PROP_TRANSPORT, "tmb"),
                ClusterName.of("shouldNotSetTransportIfAlreadySet"),
                WellKnownAddress.loopback(),
                LocalHost.only()))
            {
            Eventually.assertDeferred(member::isCoherenceRunning, is(true));
            assertThat(member.invoke(() -> System.getProperty(TransportProfile.PROP_TRANSPORT)), is("tmb"));
            }
        }


    }
