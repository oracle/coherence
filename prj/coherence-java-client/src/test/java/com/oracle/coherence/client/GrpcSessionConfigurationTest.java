/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.tangosol.net.SessionConfiguration;
import io.grpc.Channel;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.mockito.Mockito.mock;

public class GrpcSessionConfigurationTest
    {
    @Test
    public void shouldBeOrderedAfterPlainSessionConfiguration() 
        {
        Channel                  channel    = mock(Channel.class);
        GrpcSessionConfiguration grpcConfig = GrpcSessionConfiguration.builder(channel).build();
        SessionConfiguration     config     = SessionConfiguration.builder().build();
        assertThat(config.getPriority(), is(greaterThan(grpcConfig.getPriority())));
        }
    }
