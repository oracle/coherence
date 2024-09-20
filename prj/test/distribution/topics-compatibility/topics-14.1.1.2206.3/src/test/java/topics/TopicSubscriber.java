/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.base.Randoms;
import com.oracle.coherence.common.util.MutableOptions;
import com.tangosol.net.Coherence;
import com.tangosol.net.topic.Subscriber;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"rawtypes", "resource"})
public class TopicSubscriber
    {
    public static void main(String[] args) throws Exception
        {
        Coherence coherence = Coherence.clusterMember()
                .start().get(1, TimeUnit.MINUTES);

        coherence.whenClosed().join();
        }

    public static void createSubscriber(CoherenceClusterMember member, String sId, String sTopic, Subscriber.Option... options)
        {
        member.invoke(new CreateSubscriber(sId, sTopic, options));
        }

    public static void closeSubscriber(CoherenceClusterMember member, String sId)
        {
        member.invoke(new CloseSubscriber(sId));
        }

    public static int getChannelCount(CoherenceClusterMember member, String sId)
        {
        return member.invoke(new GetChannelCount(sId));
        }

    public static CompletableFuture<Set<String>> receive(CoherenceClusterMember member, String sId, Option... opts)
        {
        return receive(member, sId, -1, opts);
        }

    public static CompletableFuture<Set<String>> receive(CoherenceClusterMember member, String sId, int cMessage, Option... opts)
        {
        ReceiveTask task = new ReceiveTask(sId, cMessage);
        return member.submit(task, opts);
        }

    public static class CreateSubscriber
            implements RemoteCallable<Void>
        {
        public CreateSubscriber(String sId, String sTopicName, Subscriber.Option... options)
            {
            m_sId        = sId;
            m_sTopicName = sTopicName;
            m_options    = options;
            }

        @Override
        public Void call()
            {
            Logger.info("Creating subscriber " + m_sId + " for topic " + m_sTopicName
                                + " with options " + Arrays.toString(m_options));

            Coherence coherence = Coherence.getInstance();
            if (coherence == null)
                {
                throw new IllegalStateException("Coherence is not started");
                }
            MutableOptions<Subscriber.Option> options = new MutableOptions<>(Subscriber.Option.class);
            options.addAll(m_options);

            Subscriber<String> subscriber = s_mapSubscriber.computeIfAbsent(m_sId, s ->
                    coherence.getSession().createSubscriber(m_sTopicName, options.asArray()));

            Logger.info("Created subscriber " + m_sId + " for topic " + m_sTopicName
                                + " " + subscriber);
            return null;
            }

        // ----- data members -----------------------------------------------

        private final String m_sId;

        private final String m_sTopicName;

        private final Subscriber.Option[] m_options;
        }

    public static class CloseSubscriber
            implements RemoteCallable<Void>
        {
        public CloseSubscriber(String sId)
            {
            m_sId = sId;
            }

        @Override
        public Void call()
            {
            Subscriber<String> subscriber = s_mapSubscriber.get(m_sId);
            if (subscriber != null)
                {
                if (subscriber.isActive())
                    {
                    Logger.info("Closing subscriber " + m_sId + " " + subscriber);
                    subscriber.close();
                    }
                }
            else
                {
                throw new IllegalArgumentException("Subscriber " + m_sId + " does not exist");
                }
            return null;
            }

        // ----- data members -----------------------------------------------

        private final String m_sId;
        }

    public static class ReceiveTask
            implements RemoteCallable<Set<String>>
        {
        public ReceiveTask(String sId)
            {
            this(sId, -1);
            }

        public ReceiveTask(String sId, int cMessage)
            {
            m_sId      = sId;
            m_cMessage = cMessage;
            }

        @Override
        public Set<String> call() throws Exception
            {
            Subscriber<String> subscriber = s_mapSubscriber.get(m_sId);
            if (subscriber == null)
                {
                throw new IllegalArgumentException("No subscriber has been created with id " + m_sId);
                }

            Set<String> list      = new HashSet<>();
            int         cReceived = m_cMessage <= 0 ? Integer.MAX_VALUE : m_cMessage;
            int         cEmpty    = 0;

            Subscriber.Element<String> element = subscriber.receive().get(1, TimeUnit.MINUTES);
            while(cEmpty < 2)
                {
                if (element == null)
                    {
                    cEmpty++;
                    continue;
                    }
                list.add(element.getValue());
                cReceived--;
                if (cReceived <= 0)
                    {
                    break;
                    }
                element = subscriber.receive().get(1, TimeUnit.MINUTES);
                }
            return list;
            }

        private static String createDefaultSuffix()
            {
            return Randoms.getRandomString(500, 500, true);
            }

        // ----- data members -----------------------------------------------

        private final String m_sId;

        private final int m_cMessage;
        }

    public static class GetChannelCount
            implements RemoteCallable<Integer>
        {
        public GetChannelCount(String sId)
            {
            m_sId = sId;
            }

        @Override
        public Integer call()
            {
            Logger.info("Getting channel count for subscriber " + m_sId);
            Subscriber<String> subscriber = s_mapSubscriber.get(m_sId);
            if (subscriber != null)
                {
                Logger.info("Getting channel count for subscriber " + subscriber);
                int cChannel = subscriber.getChannels().length;
                Logger.info("Getting channel count for subscriber channels= " + cChannel + " subscriber: " + subscriber);
                return cChannel;
                }
            return 0;
            }

        // ----- data members -----------------------------------------------

        private final String m_sId;
        }
    // ----- data members ---------------------------------------------------

    private static final Map<String, Subscriber<String>> s_mapSubscriber = new ConcurrentHashMap<>();
    }
