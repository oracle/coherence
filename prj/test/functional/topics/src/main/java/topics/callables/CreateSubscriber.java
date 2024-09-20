/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package topics.callables;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.collections.ConcurrentHashMap;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.function.Remote;

import java.util.Map;

import static com.tangosol.net.topic.Subscriber.inGroup;

@SuppressWarnings("unchecked")
public class CreateSubscriber
        implements RemoteCallable<SubscriberId>
    {
    public CreateSubscriber(String sTopicName, String sGroupName)
        {
        f_sTopicName = sTopicName;
        f_sGroupName = sGroupName;
        }

    @Override
    public SubscriberId call() throws Exception
        {
        try
            {
            Session                 session      = Coherence.getInstance().getSession();
            NamedTopic<?>           topic        = session.getTopic(f_sTopicName);
            System.err.println("***** Creating subscriber for topic " + f_sTopicName + " in group " + f_sGroupName);
            PagedTopicSubscriber<?> subscriber   = (PagedTopicSubscriber<?>) topic.createSubscriber(inGroup(f_sGroupName));
            System.err.println("***** Created subscriber for topic " + f_sTopicName + " in group " + f_sGroupName);
            SubscriberId            subscriberId = subscriber.getSubscriberId();
            f_mapSubscriber.put(subscriberId, subscriber);
            System.err.println("***** Returning subscriber for topic " + f_sTopicName + " in group " + f_sGroupName + " id=" + subscriberId);
            return subscriberId;
            }
        catch (Throwable t)
            {
            Logger.err(t);
            throw Exceptions.ensureRuntimeException(t);
            }
        }

    public static <V, R> R apply(CoherenceClusterMember member, SubscriberId id, Remote.Function<Subscriber<V>, R> function)
        {
        return member.invoke(new Action<>(id, function));
        }

    protected static class Action<V, R>
            implements RemoteCallable<R>
        {
        public Action(SubscriberId id, Remote.Function<Subscriber<V>, R> function)
            {
            m_id       = id;
            m_function = function;
            }

        @Override
        public R call() throws Exception
            {
            Subscriber<V> subscriber = (Subscriber<V>) f_mapSubscriber.get(m_id);
            return m_function.apply(subscriber);
            }

        private final SubscriberId m_id;

        private final Remote.Function<Subscriber<V>, R> m_function;
        }

    // ----- data members ---------------------------------------------------

    public static final Map<SubscriberId, Subscriber<?>> f_mapSubscriber = new ConcurrentHashMap<>();

    private final String f_sTopicName;

    private final String f_sGroupName;
    }
