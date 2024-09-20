/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy;

import com.oracle.coherence.grpc.proxy.common.GrpcServerController;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight  2020.09.29
 */
public class GrpcServerControllerIT
    {
    @Test
    public void shouldControlServer() throws Exception
        {
        GrpcServerController    controller = GrpcServerController.INSTANCE;
        CompletableFuture<Void> started    = new CompletableFuture<>();

        controller.whenStarted().handle((v, e) -> started.complete(null));

        assertThat(started.isDone(), is(false));
        assertThat(controller.isRunning(), is(false));

        controller.start();
        started.get(1, TimeUnit.MINUTES);

        assertThat(started.isDone(), is(true));
        assertThat(controller.isRunning(), is(true));

        controller.stop();
        assertThat(controller.isRunning(), is(false));
        }
    }
