/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.http.netty;

import com.tangosol.coherence.http.HttpServer;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jk  2019.05.13
 */
public class NettyHttpServerTest
    {
    @Test
    public void shouldDiscoverServer()
        {
        HttpServer server = HttpServer.create();

        assertThat(server, is(instanceOf(NettyHttpServer.class)));
        }
    }
