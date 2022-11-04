/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.topic;

import com.oracle.coherence.common.util.Options;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.FlowControl;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import com.tangosol.util.function.Remote;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;

/**
 * Publisher provides a means to publish values to the {@link NamedTopic}.
 * <p>
 * The factory method {@link NamedTopic#createPublisher(Publisher.Option[])} or
 * {@link com.tangosol.net.Session#createPublisher(String)} allows one to specify one or more
 * {@link Publisher.Option}s to configure the {@link Publisher}.
 * <p>
 * Since the {@link #publish(Object)} method is asynchronous, there is a {@link #flush()} that allows one to
 * block until all outstanding {@link #publish sent values} for the {@link Publisher} have completed.
 *
 * <h3>Channels</h3>
 * Topics use the concept of channels to improve scalability. This is similar to how Coherence uses partition for caches
 * but to avoid confusion the name channel was chosen. The default is the next prime above the square root of the
 * partition count for the underlying cache service, which for the default partition count of 257 is 17 channels.
 * <p>
 * {@link Publisher Publishers} publish messages to a channel based on their ordering configuration. Subscribers then
 * subscribe from channels by assigning channel ownership to subscribers. An anonymous subscriber has ownership of all
 * channels. A subscriber that is part of a subscriber group has ownership of a sub-set of the available channels.
 * <p>
 * Channel count is configurable, but ideally should not be set too high nor too low. For example setting the channel
 * count to 1, would mean that all publishers contend to publish to a single channel, and that only one subscriber
 * in a subscriber group will be able to receive messages. Setting the channel count too high (above say the number of
 * publishers) may mean that some channels never receive any messages and are wasted. Finding the appropriate value is
 * admittedly non-trivial, however when faced with maxing out throughput from a publishers perspective this is a
 * configuration that can be tweaked.
 *
 * <h3>Positions</h3>
 * Elements in a {@link NamedTopic} are published to a channel have a unique {@link Position} within that channel.
 * A {@link Position} is an opaque representation of the underlying position as theoretically the implementation of
 * the {@link Position} could change for different types of topic. Positions are used in various places in the API,
 * for example, positions can be committed, and they can be used to move the subscriber to backwards or forwards
 * within channels. A {@link Position} is serializable so they can be stored and recovered to later reset a subscriber
 * to a desired position. Positions are {@link Comparable} so positions for elements can be used to determine whether
 * how two elements related to each other within a channel.
 *
 * @param <V>  the value type
 *
 * @author jf/jk/mf 2015.06.03
 * @since Coherence 14.1.1
 */
public interface Publisher<V>
        extends AutoCloseable
    {
    /**
     * Asynchronously publish the specified value to the topic.
     * <p>
     * {@link CompletableFuture#cancel(boolean) Cancellation} of the returned future is best
     * effort and is not guaranteed to stop the corresponding publication of the value, for example
     * example the request may already be on the wire and being processed on a storage member.
     *
     * @param value  the value to add to the topic
     *
     * @return  a {@link CompletableFuture} which can be used to identify when the value has been
     *          delivered to the topic
     *
     * @deprecated Use {@link Publisher#publish(Object)} which returns metadata about the published value
     *
     * @throws IllegalStateException if this Publisher is closed or the parent topic
     *                               has been released or destroyed
     */
    @Deprecated
    public default CompletableFuture<Void> send(V value)
        {
        return publish(value).thenAccept(_void -> {});
        }

    /**
     * Asynchronously publish the specified value to the topic.
     * <p>
     * {@link CompletableFuture#cancel(boolean) Cancellation} of the returned future is best
     * effort and is not guaranteed to stop the corresponding publication of the value, for example
     * example the request may already be on the wire and being processed on a storage member.
     * <p>
     * Published messages will be recieved by subscribers in order determined by the {@link OrderBy} option used to
     * create this {@link Publisher}.
     * Message ordering can also be controlled by publishing values that implement the {@link Orderable} interface,
     * in which case the order identifier provided by the {@link Orderable} value will override any ordering
     * configured for the {@link Publisher}.
     *
     * @param value  the value to add to the topic
     *
     * @return  a {@link CompletableFuture} containing the {@link Status}, which can be
     *          used to identify when the value has been delivered to the topic and the position
     *          it was added to the topic
     *
     * @throws IllegalStateException if this Publisher is closed or the parent topic has been released
     *                               or destroyed
     */
    public CompletableFuture<Status> publish(V value);

    /**
     * Return the {@link FlowControl} object governing this publisher.
     *
     * @return the FlowControl object.
     */
    public FlowControl getFlowControl();

    /**
     * Obtain a {@link CompletableFuture} that will be complete when
     * all of the currently outstanding publish operations complete.
     * <p>
     * The returned {@link CompletableFuture} will always complete
     * normally, even if the outstanding operations complete exceptionally.
     *
     * @return a {@link CompletableFuture} that will be completed when
     *         all of the currently outstanding publish operations are complete
     */
    public CompletableFuture<Void> flush();

    /**
     * Close this {@link Publisher}.
     * <p>
     * This is a blocking method and will wait until all outstanding
     * {@link CompletableFuture}s returned from previous calls
     * to {@link #publish(Object)} have completed before returning.
     */
    @Override
    public void close();

    /**
     * Add an action to be executed when this {@link Publisher} is closed.
     *
     * @param action  the action to execute
     */
    public void onClose(Runnable action);

    /**
     * Returns the number of channels in the underlying {@link NamedTopic} that can be published to.
     *
     * @return the number of channels in the underlying {@link NamedTopic} that can be published to
     */
    public int getChannelCount();

    /**
     * Returns the underlying {@link NamedTopic} that this {@link Publisher} publishes to.
     *
     * @return the underlying {@link NamedTopic} that this {@link Publisher} publishes to
     */
    public NamedTopic<V> getNamedTopic();

    /**
     * Specifies whether or not the {@link Publisher} is active.
     *
     * @return true if the NamedCache is active; false otherwise
     */
    public boolean isActive();

    // ----- inner interface: ElementMetadata -------------------------------

    /**
     * The status for a successfully published element.
     */
    public interface Status
        {
        /**
         * Returns the channel that the element was published to.
         *
         * @return the channel that the element was published to
         */
        public int getChannel();

        /**
         * Returns the {@link Position} in the channel that the element was published to.
         *
         * @return the {@link Position} in the channel that the element was published to
         */
        public Position getPosition();
        }

    // ----- inner interface: Option ----------------------------------------

    /**
     * A marker interface to indicate that a class is a valid {@link Option}
     * for a {@link Publisher}.
     * <p>
     *  <table>
     *    <caption>Options to use with {@link NamedTopic#createPublisher(Publisher.Option...)}</caption>
     *    <tr>
     *       <td valign="top"><b>Publisher Option</b></td>
     *       <td valign="top"><b>Description</b></td>
     *    </tr>
     *    <tr>
     *       <td valign="top">{@link OnFailure#Stop}</td>
     *       <td valign="top">Default. If an individual {@link #publish} invocation fails then stop any further publishing and close the {@link Publisher}.</td>
     *     </tr>
     *     <tr>
     *       <td valign="top">{@link OnFailure#Continue}</td>
     *       <td valign="top">If an individual {@link #publish} invocation fails then skip that value and continue to publish other values.</td>
     *     </tr>
     *     <tr>
     *       <td valign="top">{@link FailOnFull FailOnFull.enabled()}</td>
     *       <td valign="top">When the storage size of the unprocessed values on the topic exceed a configured <tt>high-units</tt>,
     *       the {@link CompletableFuture} returned from the {@link #publish} invocation should complete exceptionally.
     *       <br>Overrides the default to block completing until the operation completes when space becomes available.
     *     </tr>
     *     <tr>
     *       <td valign="top">{@link OrderBy#thread()}</td>
     *       <td valign="top">Default. Ensure that all {@link #publish values sent} from
     *                        the same thread are stored sequentially.</td>
     *      </tr>
     *      <tr>
     *        <td valign="top">{@link OrderBy#none()}</td>
     *        <td valign="top">Enforce no specific ordering between {@link #publish sent values} allowing
     *        for the greatest level of parallelism</td>
     *      </tr>
     *      <tr>
     *       <td valign="top">{@link OrderBy#id(int)}</td>
     *       <td valign="top">Ensure ordering of {@link #publish sent values} across
     *       all threads which share the same id.</td>
     *     </tr>
     *     <tr>
     *     <td valign="top">{@link OrderBy#value(ToIntFunction)}</td>
     *       <td valign="top">Compute the unit-of-order based on the applying this method on {@link #publish sent value}</td>
     *     </tr>
     *     <tr>
     *     <td valign="top">{@link OrderBy#value(Remote.ToIntFunction)}</td>
     *       <td valign="top">Compute the unit-of-order based on the applying this method on {@link #publish sent value}</td>
     *     </tr>
     *  </table>
     */
    public interface Option<V>
        {
        }

    // ----- inner class: OnFailure -----------------------------------------

    /**
     * This option controls how a {@link Publisher} handles a failure of an individual
     * {@link #publish} call.
     */
    public enum OnFailure
            implements Option<Object>
        {
        /**
         * If an individual {@link #publish} fails then stop any further
         * publishing and close the {@link Publisher}.
         * <p>
         * This option will maintain order as when a failure occurs no other
         * values will be sent to the {@link NamedTopic}.
         */
        @Options.Default
        Stop,

        /**
         * If an individual {@link #publish(Object)} fails then skip that value and
         * continue to publish other values.
         * <p>
         * This option will not guarantee to maintain order as when a failure
         * occurs other further values will be still sent to the {@link NamedTopic}.
         */
        Continue
        }

    // ---- inner class: FailOnFull -----------------------------------------

    /**
     * The FailOnFull option indicates that the {@link CompletableFuture} returned
     * from the {@link #publish} operation should complete exceptionally
     * upon identifying that the topic is or has become full.  Without this option the
     * future will not complete until more space becomes available.
     */
    public class FailOnFull
        implements Option<Object>, ExternalizableLite, PortableObject
        {
        /**
         * Default constructor for serialization.
         */
        public FailOnFull()
            {}

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            }

        /**
         * Obtain the Option indicating futures should complete exceptionally if the topic is full.
         *
         * @return  the option
         */
        public static FailOnFull enabled()
            {
            return INSTANCE;
            }

        /**
         * Singleton instance.
         */
        protected static final FailOnFull INSTANCE = new FailOnFull();
        }

    // ---- inner class: Orderable ------------------------------------------

    /**
     * Orderable represents a value published by {@link Publisher} that has a natural
     * ordering.
     * <p>
     * Calling {@link Publisher} publish methods with values that implement {@link Orderable}
     * will use the order identifier returned by {@link Orderable#getOrderId()} to determine
     * ordering instead of any ordering configured for the publisher by its {@link OrderBy}
     * option. This is similar to using the {@link OrderByValue} option but offers more
     * flexibility over a hard coded publisher option.
     */
    public static interface Orderable
        {
        public int getOrderId();
        }

    // ---- inner class: OrderBy --------------------------------------------

    /**
     * The OrderBy option specifies the ordering of async operations with respect
     * to one another.  The default unit-of-order is {@link #thread} which ensures
     * that a sequence of async operations issued by a single thread will complete
     * in order.  This default ordering does not however guarantee ordering with
     * respect to other threads or processes which are also issuing operations
     * against the same topic.  To ensure strict global ordering across threads or
     * processes all parties must {@link #id(int) specify} the same unit-of-order id.
     * Note that restricting the topic to a single unit-of-order naturally limits
     * performance as it disallows full parallelism.
     */
    public abstract class OrderBy<V>
            implements Option<V>, ExternalizableLite, PortableObject
        {
        // ---- constructors ------------------------------------------------

        /**
         * Default constructor for serialization.
         */
        protected OrderBy()
            {
            }

        // ---- accessors ---------------------------------------------------

        /**
         * Return unit-of-order id.
         *
         * @return the unit-of-order id
         */
        public abstract int getOrderId(V value);

        // ---- static factory methods --------------------------------------

        /**
         * Return an OrderBy that will ensure that all {@link #publish values sent} from
         * the same thread are stored sequentially.
         *
         * @return the default, thread-based ordering
         */
        @Options.Default
        public static OrderBy<Object> thread()
            {
            return OrderByThread.INSTANCE;
            }

        /**
         * Return an OrderBy that will enforce no specific ordering between
         * {@link #publish sent values} allowing for the greatest level of parallelism.
         *
         * @return the OrderBy which does not enforce ordering
         */
        public static OrderBy<Object> none()
            {
            return OrderByNone.INSTANCE;
            }

        /**
         * Return an OrderBy that will ensure ordering of {@link #publish sent values} across
         * all threads which share the same id.
         * <p>
         * This option effectively publishes all messages to a single channel, the exact channel
         * used is the specified {@code nOrderId} modulo the number of channels in the topic.
         * Therefore a channel number can be used as the {@code nOrderId} parameter to ensure
         * publishing to a specific channel.
         *
         * @param nOrderId  the unit-of-order
         *
         * @return the order which will use specified {@code nOrderId} for all
         *         operations
         */
        public static OrderBy<Object> id(int nOrderId)
            {
            return new OrderById<>(nOrderId);
            }

        /**
         * Return an OrderBy which will compute the unit-of-order based on the {@link #publish sent value}.
         *
         * @param supplierOrderId  the function that should be used to determine
         *                         order id from the sent value.
         *
         * @return the order which will use specified function to provide
         *         unit-of-order for each async operation
         */
        public static <V> OrderBy<V> value(Remote.ToIntFunction<? super V> supplierOrderId)
            {
            return value((ToIntFunction<? super V>) supplierOrderId);
            }

        /**
         * Return an OrderBy which will compute the unit-of-order based on the {@link #publish sent value}.
         *
         * @param supplierOrderId  the function that should be used to determine
         *                         order id from the sent value.
         *
         * @return the order which will use specified function to provide
         *         unit-of-order for each async operation
         */
        public static <V> OrderBy<V> value(ToIntFunction<? super V> supplierOrderId)
            {
            return new OrderByValue<>(supplierOrderId);
            }

        /**
         * Return an OrderBy which will compute the unit-of-order such that each message
         * is published to the next channel in a round robin order.
         *
         * @return an OrderBy which will compute the unit-of-order such that each message
         *         is published to the next channel in a round robin order.
         */
        public static <V> OrderBy<V> roundRobin()
            {
            return new OrderByRoundRobin<>();
            }
        }

    // ----- inner class: OrderByThread -------------------------------------

    /**
     * {@link OrderBy} option which ensures that a sequence of {@link #publish sent values} issued by a single
     * thread will complete in order.
     *
     * @param <V>  the value type
     */
    public static class OrderByThread<V>
            extends OrderBy<V>
        {
        public OrderByThread()
            {
            }

        @Override
        public int getOrderId(V value)
            {
            return Thread.currentThread().hashCode() ^ Base.getProcessRandom();
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            }

        @Override
        public String toString()
            {
            return "thread";
            }

        /**
        * The thread-order singleton.
        *
        * Note, we XOR with the proc random as thread hash codes tend to not be particularly random across JVM instances.
        * For instance the hash code of main is quite consistent from run to run.
        */
        protected static final OrderBy<Object> INSTANCE = new OrderByThread<>();
        }

    // ----- inner class: OrderByNone ---------------------------------------

    /**
     * {@link OrderBy} option enforces no specific ordering between {@link #publish sent values} allowing
     * for the greatest level of parallelism.
     *
     * @param <V>  the value type
     */
    public static class OrderByNone<V>
            extends OrderBy<V>
        {
        public OrderByNone()
            {
            }

        @Override
        public int getOrderId(V value)
            {
            return ThreadLocalRandom.current().nextInt();
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            }

        @Override
        public String toString()
            {
            return "none";
            }

        /**
        * The none-order singleton.
        */
        protected static final OrderBy<Object> INSTANCE = new OrderByNone<>();
        }

    // ----- inner class: OrderById -----------------------------------------

    /**
     * {@link OrderBy} option ensures ordering of {@link #publish sent values} across
     * all threads which share the same {@link #getOrderId(Object) orderId}.
     *
     * @param <V>  the value type
     */
    public static class OrderById<V>
            extends OrderBy<V>
        {
        public OrderById()
            {
            }

        protected OrderById(int nOrderId)
            {
            m_nOrderId = nOrderId;
            }

        @Override
        public int getOrderId(V value)
            {
            return m_nOrderId;
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_nOrderId = in.readInt();
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            out.writeInt(m_nOrderId);
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_nOrderId = in.readInt(1);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeInt(1, m_nOrderId);
            }

        @Override
        public String toString()
            {
            return "id(" + m_nOrderId + ")";
            }

        private int m_nOrderId;
        }

    // ----- inner class: OrderByValue --------------------------------------

    /**
     * {@link OrderBy} option which computes the unit-of-order based on applying {@link #OrderByValue(ToIntFunction)
     * constructor's} {@link ToIntFunction orderIdFunction parameter} on {@link #publish sent value}.
     *
     * @param <V>  the value type
     */
    public static class OrderByValue<V>
            extends OrderBy<V>
        {
        public OrderByValue()
            {
            }

        protected OrderByValue(ToIntFunction<? super V> orderIdFunction)
            {
            m_orderIdFunction = orderIdFunction;
            }

        @Override
        public int getOrderId(V value)
            {
            return m_orderIdFunction.applyAsInt(value);
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_orderIdFunction = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeObject(out, m_orderIdFunction);
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_orderIdFunction = in.readObject(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_orderIdFunction);
            }

        @Override
        public String toString()
            {
            return "value";
            }

        // ---- data members ------------------------------------------------

        /**
        * A function used to compute the unit-of-order from the value type V.
        */
        protected ToIntFunction<? super V> m_orderIdFunction;
        }

    // ----- inner class: OrderByValue --------------------------------------

    /**
     * {@link OrderBy} option which computes the unit-of-order such that each message is sent
     * to the next channel in a round-robin order.
     *
     * @param <V>  the value type
     */
    public static class OrderByRoundRobin<V>
            extends OrderByValue<V>
        {
        public OrderByRoundRobin()
            {
            m_orderIdFunction = this::getOrder;
            }

        private int getOrder(V ignored)
            {
            return m_nOrder.accumulateAndGet(1, (x, y) -> x == Integer.MAX_VALUE ? 0 : x + 1);
            }

        // ----- data members -----------------------------------------------

        private final AtomicInteger m_nOrder = new AtomicInteger();
        }

    /**
     * The system property to use to configure a default publisher channel count.
     */
    public static final String PROP_CHANNEL_COUNT = "coherence.publisher.channel.count";
    }
