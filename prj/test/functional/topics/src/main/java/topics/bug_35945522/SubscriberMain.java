/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package topics.bug_35945522;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.coherence.config.Config;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.Element;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SubscriberMain
        implements Constants
    {
    public static void main(String[] args) throws Exception
        {
        try (Coherence coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES))
            {
            Session session = coherence.getSession();

            CompletableFuture<Element<String>> future = null;

            long cWaitSeconds = Config.getLong("coherence.subscriber.wait.seconds", 15);

            try (Subscriber<String> subscriber = session.createSubscriber(TOPIC_NAME, Subscriber.inGroup(GROUP_NAME)))
                {
                int cChannel = subscriber.getChannelCount();
                s_subscriber    = subscriber;
                s_nSubscriberId = ((PagedTopicSubscriber<String>) subscriber).getSubscriberId().getId();
                Logger.info("Created subscriber: " + s_subscriber);

                while (true)
                    {
                    try
                        {
                        Logger.info("Calling receive...");
                        future = subscriber.receive();
                        Logger.info("Called receive: " + future + " subscriber " + subscriber);
                        for (int i = 0; i < cChannel; i++)
                            {
                            Logger.info("**** Channel (" + i + ") " + ((PagedTopicSubscriber<String>) subscriber).getChannel(i));
                            }
                        Element<String> element = future.get(cWaitSeconds, TimeUnit.SECONDS);
                        Logger.info("Received message: " + element);
                        s_cReceived++;
                        }
                    catch (Throwable t)
                        {
                        Logger.info("Exception occurred waiting on receive: "
                                + t.getClass().getSimpleName() + " " + t.getMessage() + "\n" + subscriber);
                        if (future != null && !future.isDone())
                            {
                            try
                                {
                                Logger.info("Cancelling future...");
                                future.cancel(true);
                                if (future.isDone() && !future.isCancelled())
                                    {
                                    try
                                        {
                                        Logger.info("Future completed before cancellation");
                                        }
                                    catch (Throwable e)
                                        {
                                        Logger.err(e);
                                        }
                                    }
                                }
                            catch (Throwable tt)
                                {
                                // ignored
                                }
                            if (!(t instanceof TimeoutException))
                                {
                                Logger.err(t);
                                }
                            }
                        else
                            {
                            Logger.info("Future completed after catching exception");
                            }

                        if (future != null && future.isDone() && !future.isCancelled() && !future.isCompletedExceptionally())
                            {
                            Element<String> element = future.get(cWaitSeconds, TimeUnit.SECONDS);
                            Logger.info("Received message: " + element);
                            s_cReceived++;
                            }
                        }
                    }
                }
            }
        }

    public static class GetSubscriberId
            implements RemoteCallable<Long>
        {
        @Override
        public Long call() throws Exception
            {
            return s_nSubscriberId;
            }
        }

    public static class GetChannels
            implements RemoteCallable<Integer>
        {
        @Override
        public Integer call() throws Exception
            {
            return s_subscriber.getChannels().length;
            }
        }

    public static class GetReceivedCount
            implements RemoteCallable<Integer>
        {
        @Override
        public Integer call() throws Exception
            {
            return s_cReceived;
            }
        }

    public static final GetSubscriberId GET_SUBSCRIBER_ID = new GetSubscriberId();

    public static final GetChannels GET_CHANNEL_COUNT = new GetChannels();

    public static final GetReceivedCount GET_RECEIVED_COUNT = new GetReceivedCount();

    private static Subscriber<String> s_subscriber;

    public static Long s_nSubscriberId = null;

    public static int s_cReceived;
    }
