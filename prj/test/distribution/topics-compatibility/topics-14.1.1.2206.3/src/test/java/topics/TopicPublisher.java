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
import com.oracle.coherence.common.base.Randoms;
import com.tangosol.net.Coherence;
import com.tangosol.net.topic.Publisher;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * A topic publisher process that can be controlled via Bedrock.
 */
@SuppressWarnings({"unchecked", "resource"})
public class TopicPublisher
    {
    /**
     * The process main method.
     *
     * @param args  the program arguments
     *
     * @throws Exception if there is an error
     */
    public static void main(String[] args) throws Exception
        {
        Coherence.clusterMember()
                .start()
                .get(1, TimeUnit.MINUTES)
                .whenClosed().join();
        }

    /**
     * Create a publisher.
     *
     * @param member      the {@link CoherenceClusterMember} to create the publisher in
     * @param sId         an identifier for the publisher
     * @param sTopicName  the name of the topic to publish to
     * @param options     any additional options
     */
    public static void createPublisher(CoherenceClusterMember member, String sId, String sTopicName, Publisher.Option<String>... options)
        {
        CreatePublisher task = new CreatePublisher(sId, sTopicName, options);
        member.invoke(task);
        }

    /**
     * Publish messages.
     *
     * @param member    the {@link CoherenceClusterMember} to create the publisher in
     * @param sId       an identifier for the publisher
     * @param cMessage  the number of messages to publish
     * @param options   any additional Bedrock options
     */
    public static CompletableFuture<Set<String>> publish(CoherenceClusterMember member, String sId, int cMessage, Option... options)
        {
        PublishTask task = new PublishTask(sId, cMessage);
        return member.submit(task, options);
        }

    /**
     * Publish messages.
     *
     * @param member    the {@link CoherenceClusterMember} to create the publisher in
     * @param sId       an identifier for the publisher
     * @param nStart    the starting number of messages to publish
     * @param nEnd      the end number of messages to publish
     * @param options   any additional Bedrock options
     */
    public static CompletableFuture<Set<String>> publish(CoherenceClusterMember member, String sId, int nStart, int nEnd, Option... options)
        {
        PublishTask task = new PublishTask(sId, nStart, nEnd);
        return member.submit(task, options);
        }

    /**
     * Publish messages.
     *
     * @param member    the {@link CoherenceClusterMember} to create the publisher in
     * @param sId       an identifier for the publisher
     * @param nStart    the starting number of messages to publish
     * @param nEnd      the end number of messages to publish
     * @param sSuffix   the suffix to append to the messages
     * @param options   any additional Bedrock options
     */
    public static CompletableFuture<Set<String>> publish(CoherenceClusterMember member, String sId, int nStart, int nEnd, String sSuffix, Option... options)
        {
        PublishTask task = new PublishTask(sId, nStart, nEnd, sSuffix);
        return member.submit(task, options);
        }

    // ----- inner class: CreatePublisher -----------------------------------

    /**
     * A {@link RemoteCallable} to create a publisher.
     */
    @SuppressWarnings("rawtypes")
    public static class CreatePublisher
            implements RemoteCallable<Void>
        {
        public CreatePublisher(String sId, String sTopicName, Publisher.Option<String>... options)
            {
            m_sId        = sId;
            m_sTopicName = sTopicName;
            m_options    = options;
            }

        @Override
        public Void call()
            {
            Coherence coherence = Coherence.getInstance();
            if (coherence == null)
                {
                throw new IllegalStateException("Coherence is not started");
                }
            Publisher.Option[] opts = m_options.length == 0
                    ? new Publisher.Option[]{Publisher.OrderBy.roundRobin()}
                    : m_options;
            s_mapPublisher.computeIfAbsent(m_sId, s ->
                    coherence.getSession().createPublisher(m_sTopicName, opts));
            return null;
            }

        // ----- data members -----------------------------------------------

        /**
         * The id of the publisher.
         */
        private final String m_sId;

        /**
         * The name of the topic.
         */
        private final String m_sTopicName;

        /**
         * Any publisher options.
         */
        private final Publisher.Option<String>[] m_options;
        }

    // ----- inner class: PublishTask ---------------------------------------

    /**
     * A {@link RemoteCallable} to publish messages.
     */
    public static class PublishTask
            implements RemoteCallable<Set<String>>
        {
        public PublishTask(String sId, int cMessage)
            {
            this(sId, 0, cMessage, createDefaultSuffix());
            }

        public PublishTask(String sId, int nStart, int nEnd)
            {
            this(sId, nStart, nEnd, createDefaultSuffix());
            }

        public PublishTask(String sId, int nStart, int nEnd, String sSuffix)
            {
            m_sId     = sId;
            m_nStart  = nStart;
            m_nEnd    = nEnd;
            m_sSuffix = sSuffix;
            }

        @Override
        public Set<String> call() throws Exception
            {
            Publisher<String> publisher = s_mapPublisher.get(m_sId);
            if (publisher == null)
                {
                throw new IllegalArgumentException("No publisher has been created with id " + m_sId);
                }
            Set<String> set = new HashSet<>();
            for (int i = m_nStart; i < m_nEnd; i++)
                {
                String sMsg = "message-" + m_sId + "-" + i + "-" + m_sSuffix;
                publisher.publish(sMsg).get(1, TimeUnit.MINUTES);
                set.add(sMsg);
                }
            return set;
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Create a default random suffix.
         *
         * @return a default random suffix
         */
        private static String createDefaultSuffix()
            {
            return Randoms.getRandomString(500, 500, true);
            }

        // ----- data members -----------------------------------------------

        /**
         * The id of the publisher.
         */
        private final String m_sId;

        /**
         * The number of the starting message (inclusive).
         */
        private final int m_nStart;

        /**
         * The number of the final message (exclusive).
         */
        private final int m_nEnd;

        /**
         * The suffix to append to the messages.
         */
        private final String m_sSuffix;
        }

    // ----- data members ---------------------------------------------------

    /**
     * A map of publishers, keyed by name.
     */
    private static final Map<String, Publisher<String>> s_mapPublisher = new ConcurrentHashMap<>();
    }
