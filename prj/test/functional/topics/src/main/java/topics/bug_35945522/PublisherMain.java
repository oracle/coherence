/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package topics.bug_35945522;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PublisherMain
    {
    @SuppressWarnings("resource")
    public static void main(String[] args) throws Exception
        {
        Coherence coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);
        coherence.whenClosed().join();
        }

    public static Publisher<String> ensurePublisher()
        {
        Publisher<String> publisher = s_publisher;
        if (publisher == null)
            {
            s_lock.lock();
            try
                {
                publisher = s_publisher;
                if (publisher == null)
                    {
                    Session session = Coherence.getInstance().getSession();
                    publisher = s_publisher = session.createPublisher("test", Publisher.OrderBy.roundRobin());
                    }
                }
            finally
                {
                s_lock.unlock();
                }
            }
        return publisher;
        }

    public static class GetChannelCount
        implements RemoteCallable<Integer>
        {
        @Override
        public Integer call() throws Exception
            {
            Session session = Coherence.getInstance().getSession();
            NamedTopic<String> topic = session.getTopic("test");
            return topic.getChannelCount();
            }
        }

    @SuppressWarnings("resource")
    public static class Publish
        implements RemoteCallable<Integer>
        {
        @Override
        public Integer call() throws Exception
            {
            System.err.println("***** In Publish.call()");
            Publisher<String> publisher = ensurePublisher();
            int               cChannel  = publisher.getChannelCount();
            System.err.println("***** In Publish.call() - publishing " + cChannel + " messages");
            for (int i = 0; i < cChannel; i++)
                {
                publisher.publish("message-" + s_cMessage.getAndIncrement());
                }
            System.err.println("***** In Publish.call() - published " + cChannel + " messages");
            return cChannel;
            }
        }

    private static final Lock s_lock = new ReentrantLock();

    private static Publisher<String> s_publisher;

    private static final AtomicInteger s_cMessage = new AtomicInteger();
    }
