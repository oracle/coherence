/*
 * Copyright (c) 2020, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.fail;

/**
 * Tests for {@link Reads}.
 */
public class ReadsTest
    {
    /**
     * A server that accepts and then sends nothing must not hold the read open.
     */
    @Test
    public void testHttpReadTimesOutWhenServerStalls()
            throws Exception
        {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")))
            {
            Thread acceptor = new Thread(() ->
                {
                try (Socket socket = server.accept())
                    {
                    Thread.sleep(60000L);
                    }
                catch (Exception e)
                    {
                    }
                });
            acceptor.setDaemon(true);
            acceptor.start();

            URL             url    = URI.create("http://127.0.0.1:" + server.getLocalPort() + "/").toURL();
            ExecutorService pool   = Executors.newSingleThreadExecutor(r ->
                {
                Thread thread = new Thread(r, "reads-timeout");

                thread.setDaemon(true);
                return thread;
                });
            Future<byte[]>  future = pool.submit(() -> Reads.read(url, 500));

            try
                {
                future.get(3, TimeUnit.SECONDS);
                fail("read returned");
                }
            catch (TimeoutException e)
                {
                future.cancel(true);
                fail("read did not time out");
                }
            catch (ExecutionException e)
                {
                assertThat(e.getCause(), instanceOf(SocketTimeoutException.class));
                }
            finally
                {
                pool.shutdownNow();
                }
            }
        }
    }
