/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package netty.grpc.client.topics;

import com.oracle.coherence.grpc.client.common.topics.GrpcSubscriberConnector;
import com.tangosol.coherence.component.util.safeNamedTopic.SafeSubscriberConnector;
import com.tangosol.internal.net.topic.NamedTopicSubscriber;

import com.tangosol.internal.net.topic.SimpleReceiveResult;
import com.tangosol.internal.net.topic.SimpleSubscriberOption;
import com.tangosol.internal.net.topic.SubscriberConnector;
import com.tangosol.internal.net.topic.impl.paged.model.PageElement;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.Binary;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SimpleSubscriber<V>
        implements Closeable
    {
    @SuppressWarnings("unchecked")
    public SimpleSubscriber(Subscriber<V> subscriber)
        {
        assertThat(subscriber, is(instanceOf(NamedTopicSubscriber.class)));
        SubscriberConnector<?> connector = ((NamedTopicSubscriber<?>) subscriber).getConnector();
        if (connector instanceof SafeSubscriberConnector<?>)
            {
            connector = ((SafeSubscriberConnector<?>) connector).ensureRunningConnector();
            }
        assertThat(connector, is(instanceOf(GrpcSubscriberConnector.class)));
        assertThat(connector.isSimple(), is(true));
        f_subscriber = subscriber;
        f_connector  = (GrpcSubscriberConnector<V>) connector;
        f_converter  = ((NamedTopicSubscriber<V>) subscriber).getValueConverter();
        }

    @Override
    public void close() throws IOException
        {
        f_subscriber.close();
        }

    /**
     * Returns the subscriber's unique identifier.
     *
     * @return the subscriber's unique identifier
     */
    public SubscriberId getSubscriberId()
        {
        return f_subscriber.getSubscriberId();
        }

    /**
     * Returns the current set of channels that this {@link Subscriber} owns.
     * <p>
     * Subscribers that are part of a subscriber group own a sub-set of the available channels.
     * A subscriber in a group should normally be assigned ownership of at least one channel. In the case where there
     * are more subscribers in a group that the number of channels configured for a topic, then some
     * subscribers will obviously own zero channels.
     * Anonymous subscribers that are not part of a group are always owners all the available channels.
     *
     * @return the current set of channels that this {@link Subscriber} is the owner of, or an
     *         empty array if this subscriber has not been assigned ownership any channels
     */
    public int[] getChannels()
        {
        return f_subscriber.getChannels();
        }

    public Subscriber.Element<V> receive()
        {
        SimpleReceiveResult result   = f_connector.receive(1);
        Queue<Binary>       elements = result.getElements();
        if (elements.isEmpty())
            {
            return null;
            }
        assertThat(elements.size(), is(1));
        return elements.stream()
                .map(bin -> PageElement.fromBinary(bin, f_converter::fromBinary))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected to have an element"));
        }

    public List<Subscriber.Element<V>> receive(int cMax)
        {
        SimpleReceiveResult result   = f_connector.receive(1);
        Queue<Binary>       elements = result.getElements();

        List<Subscriber.Element<V>> list = new ArrayList<>();
        if (!elements.isEmpty())
            {
            elements.stream()
                    .map(bin -> PageElement.fromBinary(bin, f_converter::fromBinary))
                    .forEach(list::add);
            }
        return list;
        }

    // ----- helper methods -------------------------------------------------

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static  <V, U> SimpleSubscriber<U> createSubscriber(NamedTopic<V> topic, Subscriber.Option... options)
        {
        Subscriber.Option[] aOpts = new Subscriber.Option[options.length + 1];
        if (options.length > 0)
            {
            System.arraycopy(options, 0, aOpts, 0, options.length);
            }
        aOpts[options.length] = SimpleSubscriberOption.INSTANCE;
        return new SimpleSubscriber<>(topic.createSubscriber(aOpts));
        }


    // ----- data members ---------------------------------------------------

    private final Subscriber<V> f_subscriber;

    private final GrpcSubscriberConnector<V> f_connector;

    private final NamedTopicSubscriber.ValueConverter<V> f_converter;
    }
