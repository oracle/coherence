/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package topics;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests that background topic publishers expose their failures to callers.
 *
 * @author phf  2026.10.29
 * @since 26.10
 */
@SuppressWarnings("unchecked")
public class TopicPublisherTests
    {
    @Test
    public void shouldPropagatePublisherCreationFailure() throws Exception
        {
        NamedTopic<String>    topic   = mock(NamedTopic.class);
        IllegalStateException failure = new IllegalStateException("publisher creation failed");
        when(topic.getName()).thenReturn("publisher-failure");
        when(topic.createPublisher(any(Publisher.Option[].class))).thenThrow(failure);

        FutureTask<Void> task = new FutureTask<>(new TopicPublisher(topic, "value-", 1, true), null);
        task.run();

        ExecutionException error = assertThrows(ExecutionException.class, () -> task.get(10, TimeUnit.SECONDS));
        assertSame(failure, error.getCause());
        }

    @Test
    public void shouldPropagateAsynchronousPublishFailure() throws Exception
        {
        NamedTopic<String>                 topic     = mock(NamedTopic.class);
        Publisher<String>                  publisher = mock(Publisher.class);
        IllegalStateException              failure   = new IllegalStateException("publish failed");
        CompletableFuture<Publisher.Status> publish   = new CompletableFuture<>();
        publish.completeExceptionally(failure);
        when(topic.getName()).thenReturn("publisher-failure");
        when(topic.createPublisher(any(Publisher.Option[].class))).thenReturn(publisher);
        when(publisher.publish(any(String.class))).thenReturn(publish);

        FutureTask<Void> task = new FutureTask<>(new TopicPublisher(topic, "value-", 1, false), null);
        task.run();

        ExecutionException error = assertThrows(ExecutionException.class, () -> task.get(10, TimeUnit.SECONDS));
        assertSame(failure, error.getCause().getCause().getCause());
        }

    @Test
    public void shouldPreservePublisherInterrupt() throws Exception
        {
        NamedTopic<String> topic     = mock(NamedTopic.class);
        Publisher<String>  publisher = mock(Publisher.class);
        when(topic.getName()).thenReturn("publisher-interrupt");
        when(topic.createPublisher(any(Publisher.Option[].class))).thenReturn(publisher);
        when(publisher.publish(any(String.class))).thenReturn(new CompletableFuture<>());

        FutureTask<Void> task = new FutureTask<>(new TopicPublisher(topic, "value-", 1, true), null);
        boolean         fWasInterrupted = Thread.currentThread().isInterrupted();
        try
            {
            Thread.currentThread().interrupt();
            task.run();
            assertTrue(Thread.currentThread().isInterrupted());
            Thread.interrupted();

            ExecutionException error = assertThrows(ExecutionException.class, () -> task.get(10, TimeUnit.SECONDS));
            assertTrue(error.getCause().getCause() instanceof InterruptedException);
            }
        finally
            {
            Thread.interrupted();
            if (fWasInterrupted)
                {
                Thread.currentThread().interrupt();
                }
            }
        }
    }
