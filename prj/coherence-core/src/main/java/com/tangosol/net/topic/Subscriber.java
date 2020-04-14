/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.topic;

import com.tangosol.net.FlowControl;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.function.Remote;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.concurrent.CompletableFuture;

import java.util.function.Function;

/**
 * A {@link Subscriber} subscribes either directly to a {@link NamedTopic} or to a {@link NamedTopic#getSubscriberGroups() subscriber group} of the {@link NamedTopic}.
 * Each value published to a {@link NamedTopic} is delivered to all of its {@link NamedTopic#getSubscriberGroups() subscriber groups}
 * and direct {@link Subscriber}s. Within each subscriber group, each value is only {@link #receive() received} by one of the {@link Subscriber group members},
 * enabling distributed, parallel processing of the values delivered to the subscriber group.
 * Thus each subscriber group in effect behaves like a queue over the topic data.
 * <p>
 * The factory method {@link NamedTopic#createSubscriber(Subscriber.Option[])} allows one to specify
 * one or more {@link Subscriber.Option}s to configure the {@link Subscriber}. The {@link Subscriber.Name#of(String)} option
 * specifies the subscriber group for the {@link Subscriber} to join. If this option is not specified, the
 * {@link Subscriber} is a direct subscriber to the topic. All Subscriber options and defaults are summarized in a table in {@link Option}.
 *
 * @param <V>  the type of the value returned to the subscriber
 *
 * @author jf/jk/mf 2015.06.03
 * @since Coherence 14.1.1
 */
public interface Subscriber<V>
        extends AutoCloseable
    {
    /**
     * Element represents a container for returned values.
     *
     * @param <V>  the type of value stored in the topic
     */
    interface Element<V>
        {
        /**
         * Return the received value
         *
         * @return the received value
         */
        V getValue();
        }

    /**
     * Receive a value from the topic.  If there is no value available then the future will complete according to
     * the {@link CompleteOnEmpty} option.
     * <p>
     * Note: If the returned future is {@link CompletableFuture#cancel(boolean) cancelled} it is possible that a value
     * may still be considered by the topic to have been received by this group, while the group would consider this
     * a lost value. Subscriber implementations will make a best effort to prevent such loss, but it cannot be guaranteed
     * and thus cancellation is not advisable.
     *
     * @return  a future which can be used to access the result of this completed operation
     */
    public CompletableFuture<Element<V>> receive();

    /**
     * Return the {@link FlowControl} object governing this subscriber.
     *
     * @return the FlowControl object.
     */
    public FlowControl getFlowControl();

    /**
     * Close the Subscriber.
     * <p>
     * Closing a subscriber ensures that no new {@link #receive} requests will be accepted and all pending
     * receive requests will be completed or safely cancelled.
     * <p>
     * For a direct topic {@link Subscriber}, {@link #close()}  enables the release of storage resources for
     * unconsumed values.
     * <p>
     * For a {@link Subscriber group member}, {@link #close()} indicates that this member has left its corresponding {@link Name group}.
     * One must actively manage a {@link NamedTopic#getSubscriberGroups() NamedTopic's logical subscriber groups} since their life span
     * is independent of active {@link Subscriber} group membership.
     * {@link NamedTopic#destroySubscriberGroup(String)} releases storage and stops accumulating topic values for a subscriber group.
     */
    @Override
    public void close();

    /**
     * Determine whether this {@link Subscriber} is active.
     *
     * @return  {@code true} if this {@link Subscriber} is active
     */
    public boolean isActive();

    /**
     * Add an action to be executed when this {@link Subscriber} is closed.
     *
     * @param action  the action to execute
     */
    public void onClose(Runnable action);

    // ----- inner interface: Option ------------------------------------

    /**
     * A marker interface to indicate that a class is a valid {@link Option}
     * for a {@link Subscriber}.
     *  <p>
     *  <table>
     *    <caption>Options to use with {@link NamedTopic#createSubscriber(Subscriber.Option...)} </caption>
     *    <tr>
     *       <td valign="top"><b>Subscriber Option</b></td>
     *       <td valign="top"><b>Description</b></td>
     *    </tr>
     *    <tr>
     *       <td valign="top">{@link Name#of(String)}</td>
     *       <td valign="top">Specify subscriber group name.
     *         <br>For each value delivered to a subscriber group, only one member of the group receives the value.</td>
     *     </tr>
     *     <tr>
     *       <td valign="top">{@link Filtered#by(Filter)}</td>
     *       <td valign="top">Only {@link Subscriber#receive() Subscriber.receive()} values matching this <tt>Filter</tt>.
     *         <br>Only one <tt>Filter</tt> per subscription group.</td>
     *     </tr>
     *     <tr>
     *       <td valign="top">{@link Convert#using(Function)}</td>
     *       <td valign="top">Convert topic value using provided {@link Function} (or {@link Remote.Function}) prior to {@link Subscriber#receive()}.
     *         <br>Only one <tt>Converter</tt> per subscription group.</td>
     *      </tr>
     *      <tr>
     *       <td valign="top">{@link CompleteOnEmpty#enabled()}</td>
     *       <td valign="top">When no values left to {@link #receive()}, returned {@link CompletableFuture#get()} returns <tt>null</tt> Element.
     *         <br>By default, returned {@link CompletableFuture#get()} blocks until next topic value is available to return.</td>
     *     </tr>
     *  </table>
     *  <p>
     *
     *  <p>
     *  All members of subscriber group share at most a single {@link Filter} and single {@link Convert Converter}. The last
     *  {@link Subscriber} joining the group and specifying these option(s) are the ones used by all subscriber group members.
     *  Note that when both a {@link Filtered#by(Filter)} and {@link Convert#using(Function)} are specified, the {@link Filter} is
     *  applied first and only {@link NamedTopic} values matching the filter are converted before being
     *  {@link Subscriber#receive() received by the Subscriber(s)}.
     *  <p>
     *
     * @param <V>  the type of the value on the topic
     * @param <U>  the type of the value returned to the subscriber
     */
    public interface Option<V, U>
        {
        }

    // ----- inner interface: Name ------------------------------------------

    /**
     * The Name option is used to specify a subscriber group name.
     * <p>
     * Providing a group name allows multiple subscriber instances to share the
     * responsibility for processing the contents of the group's durable subcription.
     * Each item added to the durable subscription will only be received by one member
     * of the group, whereas each distinct subscriber group for the topic will see
     * every added item.
     * <p>
     * Naming a subscriber also allows it to outlive its subscriber instances.
     * For example a group can be created, all instances can terminate and
     * then later be recreated and pickup exactly where they left off in the
     * topic.  As the groups life is independent of its subscriber instances
     * the group must be explicitly
     * {@link NamedTopic#destroySubscriberGroup(String) destroyed}
     * in order to have the topic stop retaining values for it.
     * <p>
     * If the {@link Name} option is not specified then the subscriber will be
     * part of an anonymous group populated by no other members and will thus
     * be ensured to see the full contents of the topic and automatically
     * destroyed upon being {@link #close closed}, or when the Coherence member
     * terminates.
     */
    public static class Name<V>
            implements Option<V, V>, ExternalizableLite, PortableObject
        {
        /**
         * Default constructor for serialization.
         */
        public Name()
            {
            }

        /**
         * Construct a new group name.
         *
         * @param sName the group name
         */
        protected Name(String sName)
            {
            m_sName = sName;
            }

        /**
         * Obtain a {@link Option} that specifies a group name
         * for a {@link Subscriber}.
         *
         * @param sName  the group name to use for the {Link Subscriber}.
         *
         * @return  a {@link Option} that specifies a group name
         *          for a {@link Subscriber}
         */
        public static <V> Name<V> of(String sName)
            {
            return new Name<>(sName);
            }

        /**
         * Return the group name.
         *
         * @return the group name
         */
        public String getName()
            {
            return m_sName;
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_sName = ExternalizableHelper.readSafeUTF(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeSafeUTF(out, m_sName);
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_sName = in.readString(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeString(0, m_sName);
            }

        /**
         * The group name.
         */
        private String m_sName;
        }

    // ----- inner class: Filtered ------------------------------------------

    /**
     * The Filtered option specifies a filter that will determine which topic values a
     * subscriber is interested in receiving.  Note that all members of a subscriber group
     * share a single filter.  If members join the group using different filters then the last
     * one to join will set the filter for the group.
     *
     * @param <V>  the type of the value on the topic
     */
    public static class Filtered<V>
        implements Option<V, V>, ExternalizableLite, PortableObject
        {
        /**
         * Default constructor for serialization.
         */
        public Filtered()
            {
            }

        protected Filtered(Filter<? super V> filter)
            {
            m_filter = filter;
            }

        /**
         * Return the option's filter.
         *
         * @return the filter
         */
        public Filter<? super V> getFilter()
            {
            return m_filter;
            }

        /**
         * Return a Filtered option with the specified filter.
         *
         * @param filter  the filter
         *
         * @return the Filtered option
         */
        public static <V> Filtered<V> by(Filter<? super V> filter)
            {
            return new Filtered<>(filter);
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_filter = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeObject(out, m_filter);
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_filter = in.readObject(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_filter);
            }

        /**
         * The filter.
         */
        private Filter<? super V> m_filter;
        }

    // ----- inner class: Convert ------------------------------------------

    /**
     * The Convert option specifies a {@link Function} that will convert topic values that
     * a subscriber is interested in receiving prior to sending them to the subscriber.
     * Note that all members of a subscriber group share a single converter.  If members join the group using different
     * converter then the last one to join will set the converter function for the group.
     *
     * @param <V>  the type of the topic value
     * @param <U>  the type of the value returned to the subscriber
     */
    public static class Convert<V,U>
        implements Option<V, U>, ExternalizableLite, PortableObject
        {
        /**
         * Default constructor for serialization.
         */
        public Convert()
            {
            }

        protected Convert(Function<? super V, U> function)
            {
            m_function = function;
            }

        /**
         * Return the option's converter function.
         *
         * @return the converter function
         */
        public Function<? super V, U> getFunction()
            {
            return m_function;
            }

        /**
         * Return a Convert option with the specified function.
         *
         * @param function  the converter function
         *
         * @return the Filtered option
         */
        public static <V, U> Convert<V, U> using(Function<? super V, U> function)
            {
            return new Convert<>(function);
            }

        /**
         * Return a Convert option with the specified function.
         *
         * @param function  the converter function
         *
         * @return the Filtered option
         */
        public static <V, U> Convert<V, U> using(Remote.Function<? super V, U> function)
            {
            return using((Function<? super V, U>) function);
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_function = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeObject(out, m_function);
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_function = in.readObject(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_function);
            }

        /**
         * The filter.
         */
        private Function<? super V, U> m_function;
        }

    // ----- inner class: CompleteOnEmpty -------------------------------

    /**
     * The CompleteOnEmpty option indicates that the {@link CompletableFuture} returned
     * from the {@link #receive} operation should complete with a <tt>null</tt> {@link Element}
     * upon identifying that the topic is or has become empty.  Without this option the
     * future will not complete until a new value becomes available.  It is supported for this
     * option to differ between members of the same subscriber group.
     */
    public static class CompleteOnEmpty<V>
            implements Option<V, V>, ExternalizableLite, PortableObject
        {
        /**
         * Default constructor for serialization.
         */
        public CompleteOnEmpty() {}

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
         * Obtain the Option indicating futures should complete if the topic is empty.
         *
         * @return  the option
         */
        @SuppressWarnings("unchecked")
        public static <V> CompleteOnEmpty<V> enabled()
            {
            return INSTANCE;
            }

        /**
         * The CompleteOnEmpty singleton.
         */
        protected static final CompleteOnEmpty INSTANCE = new CompleteOnEmpty();
        }
    }
