/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.topics;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.Coherence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;

/**
 * Integration test for the Topics example.
 *
 * @author Tim Middleton 2020.02.11
 */
public class TopicsChatApplicationTest {
    
    @BeforeAll
    static void boostrapCoherence() {
    System.setProperty("coherence.ttl",       "0");
    System.setProperty("coherence.wka",       "127.0.0.1");
    System.setProperty("coherence.localhost", "127.0.0.1");

        setCacheConfig();
        Coherence coherence = Coherence.clusterMember();
        CompletableFuture<Coherence> future = coherence.start();
        future.join();
    }

    @AfterAll
    static void shutdownCoherence() {
        Coherence coherence = Coherence.getInstance();
        coherence.close();
    }

    @Test
    public void testChatApplication() throws IOException {
        Executor executor = Executors.newFixedThreadPool(5);

        // create PipedOutputStream to write commands to
        PipedOutputStream pos1 = new PipedOutputStream();
        PipedInputStream commands1 = new PipedInputStream(pos1);
        PipedOutputStream pos2 = new PipedOutputStream();
        PipedInputStream commands2 = new PipedInputStream(pos2);
        PipedOutputStream pos3 = new PipedOutputStream();
        PipedInputStream commands3 = new PipedInputStream(pos3);

        ChatApplication user1 = new ChatApplication("user-1", commands1);
        ChatApplication user2 = new ChatApplication("user-2", commands2);

        executor.execute(user1);
        Eventually.assertThat(invoking(user1).isReady(), is(true));

        executor.execute(user2);
        Eventually.assertThat(invoking(user2).isReady(), is(true));
        
        // send some commands to user1
        pos1.write("send hello\n".getBytes());
        Eventually.assertThat(invoking(user1).getMessagesSent(), is(1));

        // user2 will get the join and the message from user 1
        Eventually.assertThat(invoking(user2).getMessagesReceived(), is(2));

        // send command to user2
        pos2.write("send hello\n".getBytes());

        // user2 has now sent 1 message
        Eventually.assertThat(invoking(user2).getMessagesSent(), is(1));

        // user1 had now received one message
        Eventually.assertThat(invoking(user1).getMessagesReceived(), is(2));

        // start another application
        ChatApplication user3 = new ChatApplication("user-3", commands3);

        executor.execute(user3);

        Eventually.assertThat(invoking(user3).isReady(), is(true));

        // user1 and 2 will receive a join message
        Eventually.assertThat(invoking(user1).getMessagesReceived(), is(3));
        Eventually.assertThat(invoking(user2).getMessagesReceived(), is(3));

        // send pm from user-3 to user-1
        pos3.write("sendpm user-1 hello\n".getBytes());

        // user-1 should get message
        Eventually.assertThat(invoking(user1).getMessagesReceived(), is(4));

        // user-2 count should stay the same
        Eventually.assertThat(invoking(user2).getMessagesReceived(), is(3));
    }

    private static void setCacheConfig() {
        System.setProperty("coherence.cacheconfig", "topics-cache-config.xml");
    }
}
