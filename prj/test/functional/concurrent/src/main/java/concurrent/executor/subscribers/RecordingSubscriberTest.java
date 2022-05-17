/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.executor.subscribers;

import com.oracle.coherence.concurrent.executor.Task;

import com.oracle.coherence.concurrent.executor.subscribers.RecordingSubscriber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Functional tests for {@link RecordingSubscriber}.
 *
 * @author phf
 * @since 21.12
 */
public class RecordingSubscriberTest
    {
    @Test
    public void shouldSubscribe()
        {
        RecordingSubscriber<Object> subscriber = new RecordingSubscriber<>();

        Assertions.assertFalse(subscriber.isSubscribed());

        subscriber.onSubscribe(new Task.Subscription()
            {
            @Override
            public void cancel()
                {
                return;
                }

            @Override
            public Task.Coordinator getCoordinator()
                {
                return null;
                }
            });

        Assertions.assertTrue(subscriber.isSubscribed());
        }

    @Test
    public void shouldNotAllowSubscriberReuse()
        {
        assertThrows(UnsupportedOperationException.class, () ->
            {
            RecordingSubscriber<Object> subscriber = new RecordingSubscriber<>();

            subscriber.onSubscribe(new Task.Subscription()
                {
                @Override
                public void cancel()
                    {
                    return;
                    }

                @Override
                public Task.Coordinator getCoordinator()
                    {
                    return null;
                    }
                });

            subscriber.onSubscribe(new Task.Subscription()
                {
                @Override
                public void cancel()
                    {
                    return;
                    }

                @Override
                public Task.Coordinator getCoordinator()
                    {
                    return null;
                    }
                });
            });
       }
    }
