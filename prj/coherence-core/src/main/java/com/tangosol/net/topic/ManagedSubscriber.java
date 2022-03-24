/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.topic;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.io.Serializer;
import com.tangosol.net.management.OpenMBeanHelper;
import com.tangosol.net.topic.Subscriber.Channel;
import com.tangosol.util.Filter;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanConstructorInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfo;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import java.util.Arrays;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A topic subscriber {@link DynamicMBean}.
 *
 * @author Jonathan Knight  2022.03.22
 * @since 21.12.4
 */
public abstract class ManagedSubscriber
        implements DynamicMBean
    {
    // ----- ManagedSubscriber methods --------------------------------------

    /**
     * Returns the subscriber type, typically the simple class name.
     * @return the subscriber type, typically the simple class name
     */
    protected abstract String getSubscriberType();

    /**
     * Return the subscriber identifier.
     * @return the subscriber identifier
     */
    protected abstract long getId();

    /**
     * Return the member the subscriber is running on.
     * @return the member the subscriber is running on
     */
    protected abstract String getMember();

    /**
     * Return the group the subscriber belongs to, if part of a group.
     * @return the group the subscriber belongs to, if part of a group
     */
    protected abstract String getSubscriberGroup();

    /**
     * Return the number of channels the topic has.
     * @return the number of channels the topic has
     */
    protected abstract int getChannelCount();

    /**
     * Return the list of channels owned by this subscriber as a String.
     * @return the list of channels owned by this subscriber as a String
     */
    protected abstract String getChannels();

    /**
     * Return the number of times the subscriber has polled for messages.
     * @return the number of times the subscriber has polled for messages
     */
    protected abstract long getPolls();

    /**
     * Return the number of message elements received.
     * @return the number of message elements received
     */
    protected abstract long getElementsPolled();

    /**
     * Return the number of times the subscriber has had to wait on an empty topic.
     * @return the number of times the subscriber has had to wait on an empty topic
     */
    protected abstract long getWaits();

    /**
     * Return the number of channel populated notifications received.
     * @return the number of channel populated notifications received
     */
    protected abstract long getNotifications();

    /**
     * Return the subscriber's state.
     * @return the subscriber's state
     */
    protected abstract int getState();

    /**
     * Return the subscriber's state, as a String.
     * @return the subscriber's state, as a String
     */
    protected abstract String getStateName();

    /**
     * Return the count of receive requests not yet complete.
     * @return the count of receive requests not yet complete
     */
    protected abstract long getBacklog();

    /**
     * Return the maximum allowed backlog of receive requests not yet complete.
     * @return the maximum allowed backlog of receive requests not yet complete
     */
    protected abstract long getMaxBacklog();

    /**
     * Return {@code true} of the subscriber completes receive futures if empty.
     * @return {@code true} of the subscriber completes receive futures if empty
     */
    protected abstract boolean isCompleteOnEmpty();

    /**
     * Return the optional subscriber {@link Filter}.
     * @return the optional subscriber {@link Filter}
     */
    protected abstract Filter<?> getFilter();

    /**
     * Return the optional subscriber converter.
     * @return the optional subscriber converter
     */
    protected abstract Function<?, ?> getConverter();

    /**
     * Return the subscriber {@link Serializer}.
     * @return the subscriber {@link Serializer}
     */
    protected abstract Serializer getSerializer();

    /**
     * Return the count of receive requests completed.
     * @return the count of receive requests completed
     */
    protected abstract long getReceivedCount();

    /**
     * Return the count of receive requests completed with a {@code null} message.
     * @return the count of receive requests completed with a {@code null} message
     */
    protected abstract long getReceivedEmptyCount();

    /**
     * Return the count of receive requests completed with an error.
     * @return the count of receive requests completed with an error
     */
    protected abstract long getErrorCount();

    /**
     * Return the number of times the subscriber has disconnected.
     * @return the number of times the subscriber has disconnected
     */
    protected abstract long getDisconnectCount();

    /**
     * Force the subscriber to disconnect.
     */
    protected abstract void disconnect();

    /**
     * Ensure the subscriber is connected.
     */
    protected abstract void connect();

    /**
     * Return the current topic heads as seen by this subscriber.
     * @return the current topic heads as seen by this subscriber
     */
    protected abstract Map<Integer, Position> getHeads();

    /**
     * Return the specified channel.
     * @param nChannel  the channel to obtain
     * @return the specified channel
     */
    protected abstract Channel getChannel(int nChannel);

    /**
     * Notify the subscriber that the specified channel has been populated.
     * @param nChannel  the channel identifier
     */
    protected abstract void notifyChannel(int nChannel);

    /**
     * Return the remaining messages in the specified channel, or if the channel is
     * less than zero, return the count of all remaining messages for channels owned
     * by this subscriber.
     *
     * @param nChannel  the channel to count remaining messages in, or -1 for all owned channels
     *
     * @return the remaining messages in the specified channel, or if the channel is
     *         less than zero, return the count of all remaining messages for channels owned
     *         by this subscriber
     */
    protected abstract int getRemainingMessages(int nChannel);

    // ----- DynamicMBean methods -------------------------------------------

    @Override
    public Object getAttribute(String sAttribute) throws AttributeNotFoundException, MBeanException, ReflectionException
        {
        SubscriberAttribute attribute = SubscriberAttribute.valueOf(sAttribute);

        if (SubscriberAttribute.Channels == attribute)
            {
            return getChannelInfo();
            }

        Function<ManagedSubscriber, ?> function = attribute.getFunction();
        if (function == null)
            {
            throw new AttributeNotFoundException(sAttribute);
            }

        return function.apply(this);
        }

    @Override
    public void setAttribute(Attribute sAttribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException
        {
        throw new InvalidAttributeValueException("Attribute " + sAttribute + " is read-only");
        }

    @Override
    public AttributeList getAttributes(String[] asAttribute)
        {
        AttributeList list = new AttributeList();
        for (String sAttribute : asAttribute)
            {
            try
                {
                Object oValue = getAttribute(sAttribute);
                list.add(new Attribute(sAttribute, oValue));
                }
            catch (AttributeNotFoundException | MBeanException | ReflectionException e)
                {
                Logger.err(e);
                }
            }
        return list;
        }

    @Override
    public AttributeList setAttributes(AttributeList attributes)
        {
        AttributeList list = new AttributeList();
        for (Attribute attribute : attributes.asList())
            {
            try
                {
                setAttribute(attribute);
                String sName = attribute.getName();
                list.add(new Attribute(sName, getAttribute(sName)));
                }
            catch (AttributeNotFoundException | InvalidAttributeValueException | MBeanException | ReflectionException e)
                {
                Logger.err(e);
                }
            }
        return list;
        }

    @Override
    public Object invoke(String sOperation, Object[] aoParam, String[] asSignature) throws MBeanException, ReflectionException
        {
        try
            {
            SubscriberOperation operation = SubscriberOperation.valueOf(sOperation);
            switch (operation)
                {
                case Disconnect:
                    disconnect();
                    return null;
                case Connect:
                    connect();
                    return null;
                case Heads:
                    return getHeadsTable();
                case RemainingMessages:
                    return getRemainingMessagesTable();
                case NotifyPopulated:
                    return invokeNotifyChannel(aoParam);
                default:
                    throw new UnsupportedOperationException("MBean operation " + sOperation + " is not supported");
                }
            }
        catch (IllegalArgumentException e)
            {
            throw new UnsupportedOperationException("MBean operation " + sOperation + " is not supported");
            }
        }

    @Override
    public MBeanInfo getMBeanInfo()
        {
        if (m_mBeanInfo == null)
            {
            synchronized (this)
                {
                if (m_mBeanInfo == null)
                    {
                    MBeanAttributeInfo[] aAttributeInfo = Arrays.stream(SubscriberAttribute.values())
                            .map(SubscriberAttribute::getMBeanAttributeInfo)
                            .toArray(MBeanAttributeInfo[]::new);

                    MBeanOperationInfo[] aOperation = Arrays.stream(SubscriberOperation.values())
                            .map(SubscriberOperation::getOperation)
                            .toArray(MBeanOperationInfo[]::new);

                    m_mBeanInfo = new MBeanInfo(this.getClass().getName(),
                                                MBEAN_DESCRIPTION,
                                                aAttributeInfo,
                                                new OpenMBeanConstructorInfoSupport[0],
                                                aOperation,
                                                new MBeanNotificationInfo[0]);
                    }
                }
            }
        return m_mBeanInfo;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtain an attribute for a channel.
     *
     * @param nChannel   the channel identifier
     * @param attribute  the attribute to obtain
     * @return the value of the attribute
     */
    protected Object getChannelAttribute(int nChannel, ChannelAttribute attribute)
        {
        if (nChannel >= 0 && nChannel < getChannelCount())
            {
            if (ChannelAttribute.Channel == attribute)
                {
                return nChannel;
                }

            Function<Channel, Object> function = attribute.getFunction();
            if (function != null)
                {
                Channel channel = getChannel(nChannel);
                return function.apply(channel);
                }
            }
        return null;
        }

    /**
     * Returns a {@link TabularData table} of all the channel attributes for all channels.
     * @return a {@link TabularData table} of all the channel attributes for all channels
     */
    protected TabularData getChannelInfo()
        {
        try
            {
            TabularDataSupport table      = new TabularDataSupport(CHANNEL_TABLE_TYPE);
            int                cChannel   = getChannelCount();
            CompositeData[]    rows       = new CompositeData[cChannel];
            ChannelAttribute[] aAttribute = ChannelAttribute.values();
            int                cAttribute = aAttribute.length;

            for (int nChannel = 0; nChannel < cChannel; nChannel++)
                {
                Object[] aoValue = new Object[cAttribute];
                for (int a = 0; a < cAttribute; a++)
                    {
                    aoValue[a] = getChannelAttribute(nChannel, aAttribute[a]);
                    }
                rows[nChannel] = new CompositeDataSupport(CHANNEL_ROW_TYPE, ALL_CHANNEL_ATTRIBUTES, aoValue);
                }

            table.putAll(rows);

            return table;
            }
        catch (OpenDataException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Returns a {@link TabularData table} of the subscribers heads.
     * @return a {@link TabularData table} of the subscribers heads
     */
    protected TabularData getHeadsTable()
        {
        try
            {
            Map<Integer, Position> mapHead = getHeads();

            TabularDataSupport table = new TabularDataSupport(POSITION_TABLE_TYPE);
            for (Map.Entry<Integer, Position> entry : mapHead.entrySet())
                {
                table.put(new CompositeDataSupport(POSITION_ROW_TYPE, ALL_POSITION_ATTRIBUTES, new Object[]{entry.getKey(), String.valueOf(entry.getValue())}));
                }

            return table;
            }
        catch (Throwable e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Returns a {@link TabularData table} of the remaining messages by channel.
     * @return a {@link TabularData table} of the remaining messages by channel
     */
    protected TabularData getRemainingMessagesTable()
        {
        try
            {
            TabularDataSupport table    = new TabularDataSupport(CHANNEL_COUNT_TABLE_TYPE);
            int                cChannel = getChannelCount();
            for (int nChannel = 0; nChannel < cChannel; nChannel++)
                {
                int cRemaining = getRemainingMessages(nChannel);
                table.put(new CompositeDataSupport(CHANNEL_COUNT_ROW_TYPE, ALL_CHANNEL_COUNT_ATTRIBUTES, new Object[]{nChannel, cRemaining}));
                }

            return table;
            }
        catch (Throwable e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    private static OpenType<?> getChannelTableType()
        {
        return CHANNEL_TABLE_TYPE;
        }

    private Void invokeNotifyChannel(Object[] aoParam)
        {
        if (aoParam == null || aoParam.length != 1 || !(aoParam[0] instanceof Integer))
            {
            throw new IllegalArgumentException("An integer channel identifier must be supplied");
            }

        int nChannel = (Integer) aoParam[0];
        if (nChannel < 0 || nChannel >= getChannelCount())
            {
            throw new IllegalArgumentException("An integer channel identifier must be supplied in the range 0.." + getChannelCount());
            }

        notifyChannel(nChannel);

        return null;
        }

    // ----- inner enum: SubscriberAttribute --------------------------------

    /**
     * An enum representing the MBean attributes.
     */
    public enum SubscriberAttribute
        {
        Id("The subscriber's identifier", SimpleType.LONG, m -> m.getId()),
        Type("The type of this subscriber", SimpleType.STRING, m -> m.getSubscriberType()),
        ChannelAllocations("The subscriber's allocated channels", SimpleType.STRING, m -> m.getChannels()),
        ChannelCount("The number of channels in the topic", SimpleType.INTEGER, m -> m.getChannelCount()),
        SubscriberGroup("The subscriber group the subscriber belongs to", SimpleType.STRING, m -> m.getSubscriberGroup()),
        Polls("The total number of polls for messages", SimpleType.LONG, m -> m.getPolls()),
        Elements("The number of elements received", SimpleType.LONG, m -> m.getElementsPolled()),
        ReceiveCompletions("The number completed receive requests", SimpleType.LONG, m -> m.getReceivedCount()),
        ReceiveErrors("The number exceptionally completed receive requests", SimpleType.LONG, m -> m.getErrorCount()),
        ReceiveEmpty("The number empty receive requests", SimpleType.LONG, m -> m.getReceivedEmptyCount()),
        Waits("The number of waits on an empty channel", SimpleType.LONG, m -> m.getWaits()),
        Notifications("The number of channel notifications received", SimpleType.LONG, m -> m.getNotifications()),
        State("The state of the subscriber", SimpleType.INTEGER, m -> m.getState()),
        StateName("The state of the subscriber as a string", SimpleType.STRING, m -> m.getStateName()),
        Backlog("The number of outstanding receive requests", SimpleType.LONG, m -> m.getBacklog()),
        MaxBacklog("The maximum number of outstanding receive requests allowed before flow control blocks receive calls", SimpleType.LONG, m -> m.getMaxBacklog()),
        Disconnections("The number of times this subscriber has disconnected", SimpleType.LONG, m -> m.getDisconnectCount()),
        Filter("The optional filter being used to filter messages", SimpleType.STRING, m -> String.valueOf(m.getFilter())),
        Converter("The optional converter being used to transform messages", SimpleType.STRING, m -> String.valueOf(m.getConverter())),
        Serializer("The optional converter being used to transform messages", SimpleType.STRING, m -> String.valueOf(m.getSerializer())),
        CompleteOnEmpty("A flag indicating whether the subscriber completes receive requests with a null message when the topic is empty", SimpleType.BOOLEAN, m -> m.isCompleteOnEmpty()),
        Member("The cluster member owning this subscriber", SimpleType.STRING, m -> m.getMember()),
        Channels("The subscriber's channel details", () -> ManagedSubscriber.getChannelTableType(), null),
        ;

        // ----- constructors ---------------------------------------------------

        SubscriberAttribute(String sDescription, OpenType<?> openType, Function<ManagedSubscriber, ?> function)
            {
            this(sDescription, () -> openType, true, false, function);
            }

        SubscriberAttribute(String sDescription, Supplier<OpenType<?>> openType, Function<ManagedSubscriber, ?> function)
            {
            this(sDescription, openType, true, false, function);
            }

        SubscriberAttribute(String sDescription, Supplier<OpenType<?>> openType, boolean fReadable, boolean fWritable, Function<ManagedSubscriber, ?> function)
            {
            f_sDescription = sDescription;
            f_openType     = openType;
            f_fReadable    = fReadable;
            f_fWritable    = fWritable;
            f_function     = function;
            }

        // ----- SubscriberAttribute methods --------------------------------

        public MBeanAttributeInfo getMBeanAttributeInfo()
            {
            if (m_attribute == null)
                {
                synchronized (this)
                    {
                    if (m_attribute == null)
                        {
                        OpenType<?> type = f_openType.get();
                        m_attribute = new OpenMBeanAttributeInfoSupport(name(), f_sDescription, type, f_fReadable, f_fWritable, false);
                        }
                    }
                }
            return m_attribute;
            }

        public Function<ManagedSubscriber, ?> getFunction()
            {
            return f_function;
            }

        public String getDescription()
            {
            return f_sDescription;
            }

        // ----- data members -----------------------------------------------

        /**
         * The lazily created {@link MBeanAttributeInfo} attribute definition.
         */
        private volatile MBeanAttributeInfo m_attribute;

        /**
         * The attribute's description.
         */
        private final String f_sDescription;

        /**
         * The attribute's type
         */
        private final Supplier<OpenType<?>> f_openType;

        /**
         * A flag indicating whether the attribute is readable.
         */
        private final boolean f_fReadable;

        /**
         * A flag indicating whether the attribute is writable.
         */
        private final boolean f_fWritable;

        /**
         * The function to execute to obtain the attribute value.
         */
        private final Function<ManagedSubscriber, ?> f_function;
        }

    // ----- inner enum: ChannelAttribute -----------------------------------

    /**
     * An enum representing the MBean channel attributes.
     */
    public enum ChannelAttribute
        {
        Channel("The channel number", SimpleType.INTEGER, Subscriber.Channel::getId),
        Owned("A flag indicating whether the channel is owned by this subscriber", SimpleType.BOOLEAN, Subscriber.Channel::isOwned),
        Empty("A flag indicating whether the channel is empty", SimpleType.BOOLEAN, Subscriber.Channel::isEmpty),
        LastReceived("The last position received by this subscriber since it was last assigned ownership of this channel", SimpleType.STRING, c -> String.valueOf(c.getLastReceived())),
        LastCommit("The last position successfully committed by this subscriber since it was last assigned ownership of this channel", SimpleType.STRING, c -> String.valueOf(c.getLastCommit())),
        Head("The position this subscriber knows as the head for the channel", SimpleType.STRING, c -> String.valueOf(c.getHead())),
        ;

        // ----- constructors ---------------------------------------------------

        ChannelAttribute(String sDescription, OpenType<?> type, Function<Subscriber.Channel, Object> function)
            {
            f_sDescription = sDescription;
            f_type         = type;
            f_function     = function;
            }

        // ----- ChannelAttribute methods -----------------------------------

        public String getDescription()
            {
            return f_sDescription;
            }

        public OpenType<?> getType()
            {
            return f_type;
            }

        public Function<Subscriber.Channel, Object> getFunction()
            {
            return f_function;
            }

        public static String[] allNames()
            {
            return Arrays.stream(ChannelAttribute.values()).map(ChannelAttribute::name).toArray(String[]::new);
            }

        public static String[] allDescriptions()
            {
            return Arrays.stream(ChannelAttribute.values()).map(ChannelAttribute::getDescription).toArray(String[]::new);
            }

        public static OpenType<?>[] allTypes()
            {
            return Arrays.stream(ChannelAttribute.values()).map(ChannelAttribute::getType).toArray(OpenType[]::new);
            }

        // ----- data members -----------------------------------------------

        /**
         * The attribute's description.
         */
        private final String f_sDescription;

        /**
         * The attribute's type.
         */
        private final OpenType<?> f_type;

        /**
         * A function to obtain the attribute value.
         */
        private final Function<Subscriber.Channel, Object> f_function;
        }

    // ----- inner enum: SubscriberOperation --------------------------------

    /**
     * An enum of MBean operations.
     */
    public enum SubscriberOperation
        {
        Disconnect("Force this subscriber to disconnect and reset itself"),
        Connect("Ensure this subscriber to connected"),
        Heads("Retrieve the current head positions for each channel", new OpenMBeanParameterInfo[0], POSITION_TABLE_TYPE),
        RemainingMessages("Retrieve the count of remaining messages for each channel", new OpenMBeanParameterInfo[0], CHANNEL_COUNT_TABLE_TYPE),
        NotifyPopulated("Send a channel populated notification to this subscriber",
               new OpenMBeanParameterInfo[]{new OpenMBeanParameterInfoSupport("Channel", "The channel identifier", SimpleType.INTEGER)},
               SimpleType.VOID),
        ;

        // ----- constructors ---------------------------------------------------

        SubscriberOperation(String sDescription)
            {
            this(sDescription, new OpenMBeanParameterInfo[0], SimpleType.VOID);
            }

        SubscriberOperation(String sDescription, OpenMBeanParameterInfo[] aParams, OpenType<?> typeReturn)
            {
            f_sDescription = sDescription;
            f_aParams      = aParams;
            f_typeReturn = typeReturn;
            }

        // ----- SubscriberOperation methods --------------------------------

        public MBeanOperationInfo getOperation()
            {
            return new OpenMBeanOperationInfoSupport(name(), f_sDescription, f_aParams, f_typeReturn, OpenMBeanOperationInfoSupport.ACTION);
            }

        // ----- data members ---------------------------------------------------

        /**
         * The operation description.
         */
        private final String f_sDescription;

        /**
         * The operation's parameter types.
         */
        private final OpenMBeanParameterInfo[] f_aParams;

        /**
         * The operation's return type.
         */
        private final OpenType<?> f_typeReturn;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The subscriber MBean ObjectName type.
     */
    public static final String TYPE_SUBSCRIBER = "type=TopicSubscriber";

    /**
     * The string pattern to use to create an MBean name.
     */
    public static final String MBEAN_NAME_PATTERN = TYPE_SUBSCRIBER + ",topic=%s,group=%s,id=%d";

    /**
     * The group name used for anonymous subscribers in the MBean name.
     */
    public static final String ANONYMOUS_GROUP = "<Anonymous>";

    /**
     * The MBean's description.
     */
    protected static final String MBEAN_DESCRIPTION = "A Coherence NamedTopic subscriber";

    /**
     * The name of the Channel attribute used to index channel table data.
     */
    protected static final String CHANNEL_ATTRIBUTE = ChannelAttribute.Channel.name();

    /**
     * All the channel attribute names.
     */
    protected static final String[] ALL_CHANNEL_ATTRIBUTES = ChannelAttribute.allNames();

    /**
     * The definition of a row in a channel table.
     */
    protected static final CompositeType CHANNEL_ROW_TYPE = OpenMBeanHelper.createCompositeType(CHANNEL_ATTRIBUTE,
            ChannelAttribute.Channel.getDescription(),
            ALL_CHANNEL_ATTRIBUTES,
            ChannelAttribute.allDescriptions(),
            ChannelAttribute.allTypes());

    /**
     * The definition of a channel information table.
     */
    protected static final TabularType CHANNEL_TABLE_TYPE = OpenMBeanHelper.createTabularType(CHANNEL_ATTRIBUTE,
                        SubscriberAttribute.Channels.getDescription(),
                        CHANNEL_ROW_TYPE,
                        new String[] {CHANNEL_ATTRIBUTE});

    /**
     * The position information attributes.
     */
    protected static final String[] ALL_POSITION_ATTRIBUTES = new String[]{CHANNEL_ATTRIBUTE, "Position"};

    /**
     * A definition of a row in a position table.
     */
    protected static final CompositeType POSITION_ROW_TYPE = OpenMBeanHelper.createCompositeType(CHANNEL_ATTRIBUTE,
            ChannelAttribute.Channel.getDescription(),
            ALL_POSITION_ATTRIBUTES,
            new String[]{ChannelAttribute.Channel.getDescription(), "The position"},
            new OpenType<?>[]{ChannelAttribute.Channel.getType(), SimpleType.STRING});

    /**
     * A definition of a position table.
     */
    protected static final TabularType POSITION_TABLE_TYPE = OpenMBeanHelper.createTabularType("Positions",
                        "Positions by channel",
                        POSITION_ROW_TYPE,
                        new String[] {CHANNEL_ATTRIBUTE});

    /**
     * The counts for a channel attributes.
     */
    protected static final String[] ALL_CHANNEL_COUNT_ATTRIBUTES = new String[]{CHANNEL_ATTRIBUTE, "Count"};

    /**
     * A definition of a row in a channel count table.
     */
    protected static final CompositeType CHANNEL_COUNT_ROW_TYPE = OpenMBeanHelper.createCompositeType(CHANNEL_ATTRIBUTE,
            ChannelAttribute.Channel.getDescription(),
            ALL_CHANNEL_COUNT_ATTRIBUTES,
            new String[]{ChannelAttribute.Channel.getDescription(), "The count"},
            new OpenType<?>[]{ChannelAttribute.Channel.getType(), SimpleType.INTEGER});

    /**
     * A definition of a channel count table.
     */
    protected static final TabularType CHANNEL_COUNT_TABLE_TYPE = OpenMBeanHelper.createTabularType("Counts",
                        "Counts by channel",
                        CHANNEL_COUNT_ROW_TYPE,
                        new String[] {CHANNEL_ATTRIBUTE});

    // ----- data members ---------------------------------------------------

    /**
     * The lazily created MBean definition.
     */
    private volatile MBeanInfo m_mBeanInfo;
    }
