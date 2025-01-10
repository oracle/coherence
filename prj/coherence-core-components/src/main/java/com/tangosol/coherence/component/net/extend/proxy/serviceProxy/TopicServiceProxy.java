/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: Component.net.extend.proxy.serviceProxy.TopicServiceProxy

package com.tangosol.coherence.component.net.extend.proxy.serviceProxy;

import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.Component;

import com.tangosol.coherence.component.net.extend.message.request.TopicServiceRequest;
import com.tangosol.coherence.component.net.extend.protocol.TopicServiceProtocol;
import com.tangosol.coherence.component.net.extend.proxy.ServiceProxy;

import com.tangosol.coherence.component.util.Converter;
import com.tangosol.coherence.component.util.SafeNamedTopic;

import com.tangosol.coherence.config.ResolvableParameterList;

import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.config.expression.Parameter;

import com.tangosol.internal.net.service.extend.proxy.DefaultProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.DefaultTopicServiceProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.LegacyXmlTopicServiceProxyHelper;
import com.tangosol.internal.net.service.extend.proxy.ProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.TopicServiceProxyDependencies;

import com.tangosol.internal.net.topic.ConverterConnectedSubscriber;
import com.tangosol.internal.net.topic.ConverterNamedTopic;
import com.tangosol.internal.net.topic.ConverterPublisher;
import com.tangosol.internal.net.topic.ConverterPublisherConnector;
import com.tangosol.internal.net.topic.NamedTopicPublisher;
import com.tangosol.internal.net.topic.NamedTopicSubscriber;
import com.tangosol.internal.net.topic.PublisherConnector;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;

import com.tangosol.io.Serializer;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.TopicService;

import com.tangosol.net.messaging.Channel;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.TopicBackingMapManager;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SafeHashSet;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A server side proxy for a {@link TopicService}.
 *
 * @author Jonathan Knight  2024.11.26
 */
@SuppressWarnings({"rawtypes", "PatternVariableCanBeUsed"})
public class TopicServiceProxy
        extends ServiceProxy
        implements TopicService, Channel.Receiver
    {
    /**
     * The set of topics created by this proxy.
     */
    private Set<String> __m_NamedTopicSet;

    /**
     * The cache factory that created this service.
     */
    private ConfigurableCacheFactory __m_CacheFactory;

    /**
     * Property TopicService
     */
    private TopicService __m_TopicService;

    /**
     * Property TransferThreshold
     */
    private long __m_TransferThreshold;

    /**
     * True iff binary pass-through optimizations are enabled.
     */
    private boolean __m_PassThroughEnabled;

    /**
     * @see TopicService#getTopicBackingMapManager()
     */
    private TopicBackingMapManager __m_BackingMapManager;

    /**
     * Child ConverterFromBinary instance.
     */
    private ConverterFromBinary __m_ConverterFromBinary;

    /**
     * Child ConverterToBinary instance.
     */
    private ConverterToBinary __m_ConverterToBinary;

    /**
     * A Map of topic names created by this TopicServiceProxy.
     * <p>
     * This is used by the ensureCache() method to determine if a warning should be
     * logged if cache does not support pass-through optimizations.
     */
    @SuppressWarnings("unchecked")
    private final Set<String> __m_NamedCacheSet = new SafeHashSet();

    /**
     * The channel being used.
     */
    private Channel m_channel;

    public TopicServiceProxy()
        {
        this(null, null, true);
        }

    public TopicServiceProxy(String sName, Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        if (fInit)
            {
            __init();
            }
        }

    @Override
    @SuppressWarnings("unchecked")
    public void __init()
        {
        // private initialization
        __initPrivate();

        // state initialization: public and protected properties
        try
            {
            setEnabled(true);
            setNamedTopicSet(new SafeHashSet());
            setPassThroughEnabled(true);
            setServiceVersion("14");
            setTransferThreshold(524288L);
            }
        catch (Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }

        // signal the end of the initialization
        set_Constructed(true);
        }

    @Override
    protected void __initPrivate()
        {
        super.__initPrivate();
        }

    @Override
    public void onInit()
        {
        setConverterFromBinary(new ConverterFromBinary());
        setConverterToBinary(new ConverterToBinary());
        super.onInit();
        }

    /**
     * Create a new Default dependencies object by cloning the input
     * dependencies.  Each class or component that uses dependencies implements
     * a Default dependencies class which provides the clone functionality.
     * The dependency injection design pattern requires every component in the
     * component hierarchy to implement clone.
     *
     * @return DefaultProxyDependencies  the cloned dependencies
     */
    @Override
    protected DefaultProxyDependencies cloneDependencies(ProxyDependencies deps)
        {
        return new DefaultTopicServiceProxyDependencies((TopicServiceProxyDependencies) deps);
        }

    @Override
    public void destroyTopic(NamedTopic<?> topic)
        {
        releaseTopic(topic, /*fDestroy*/ true);
        }

    @Override
    public int ensureChannelCount(String sTopic, int cChannel)
        {
        return ensureChannelCount(sTopic, cChannel, cChannel);
        }

    @Override
    public int ensureChannelCount(String sTopic, int cRequired, int cChannel)
        {
        //noinspection resource
        return ensureTopic(sTopic, null).getTopicService().ensureChannelCount(sTopic, cRequired, cChannel);
        }

    @SuppressWarnings("unchecked")
    @Override
    public <T> NamedTopic<T> ensureTopic(String sTopic, ClassLoader loader)
        {
        ConfigurableCacheFactory ccf = getCacheFactory();

        if (!isPassThroughEnabled())
            {
            return ccf.ensureTopic(sTopic, loader);
            }

        ClassLoader loaderInternal = NullImplementation.getClassLoader();
        NamedTopic  topic          = ccf.ensureTopic(sTopic, loaderInternal);

        // check to see if the Serializer associated with the "backdoor" NamedTopic is
        // compatible with the Serializer associated with this TopicServiceProxy; if
        // they are not, replace the "backdoor" NamedTopic with the "front door"
        // NamedTopic and wrap it with a ConverterNamedTopic that uses this
        // TopicServiceProxy's Converters (see ConverterFromBinary and ConverterToBinary)
        Serializer serializerThis = getSerializer();
        Serializer serializerThat = getSerializer(topic);
        if (!ExternalizableHelper.isSerializerCompatible(serializerThis, serializerThat))
            {
            // COH-8758
            // We cannot release the cache obtained with the NullImplementation loader
            // as this will clear local caches (or caches backed by a local cache, such
            // as a wrapper or converter cache of a local cache). The downside of this
            // change is that we will (at worst) have one unused cache reference per
            // configured cache service. The upside is that it will make subsequent
            // calls of this method (with the same cache name) more efficient.
            // ccf.releaseCache(cache);
            NamedTopic<Binary>  underlying           = ccf.ensureTopic(sTopic, loader);
            ConverterToBinary   converterToBinary    = getConverterToBinary();    // object -> bin-this
            ConverterFromBinary converterFromBinary  = getConverterFromBinary();  // bin-this -> object

            topic = new ConverterTopic(underlying, converterToBinary, converterFromBinary, serializerThat);

            if (getNamedTopicSet().add(sTopic))
                {
                if (serializerThat == null)
                    {
                    Logger.info("The topic \"" + sTopic + "\" does not support"
                         + " pass-through optimization for objects in"
                         + " internal format. If possible, consider using"
                         + " a different topic topology.");
                    }
                else
                    {
                    ExternalizableHelper.reportIncompatibleSerializers(topic, "topic", getServiceName(), serializerThis);
                    }
                }
            }

        return topic;
        }

    @Override
    public int getChannelCount(String sTopic)
        {
        //noinspection resource
        return ensureTopic(sTopic, null).getTopicService().getChannelCount(sTopic);
        }

    @Override
    public String getName()
        {
        return "TopicServiceProxy";
        }

    @Override
    public TopicServiceProtocol getProtocol()
        {
        return TopicServiceProtocol.getInstance();
        }

    @Override
    public String getServiceType()
        {
        return TopicService.TYPE_REMOTE;
        }

    @Override
    public Set<SubscriberGroupId> getSubscriberGroups(String sTopic)
        {
        //noinspection resource
        return ensureTopic(sTopic, null).getTopicService().getSubscriberGroups(sTopic);
        }

    @Override
    public TopicBackingMapManager getTopicBackingMapManager()
        {
        return __m_BackingMapManager;
        }

    @Override
    public void setTopicBackingMapManager(TopicBackingMapManager manager)
        {
        __m_BackingMapManager = manager;
        }

    @Override
    public Set<String> getTopicNames()
        {
        Set<String> set = __m_NamedCacheSet;
        synchronized (set)
            {
            return new HashSet<>(set);
            }
        }

    @Override
    protected void onDependencies(ProxyDependencies deps)
        {
        super.onDependencies(deps);

        TopicServiceProxyDependencies proxyDeps = (TopicServiceProxyDependencies) deps;

        // For ECCF based config, a custom service builder may be injected by CODI.
        // For DCCF, we are still using the XML for custom services.  
        ParameterizedBuilder bldrService = proxyDeps.getServiceBuilder();
        if (bldrService == null)
            {
            // DCCF style
            XmlElement xml = proxyDeps.getServiceClassConfig();
            if (xml != null)
                {
                try
                    {
                    setTopicService((TopicService) XmlHelper.createInstance(xml,
                            Classes.getContextClassLoader(), /*resolver*/ this, TopicService.class));
                    }
                catch (Exception e)
                    {
                    throw ensureRuntimeException(e);
                    }
                }
            }
        else
            {
            // ECCF style - only an InstanceBuilder is supported
            ResolvableParameterList listParams = new ResolvableParameterList();
            listParams.add(new Parameter("topic-service", this));

            if (bldrService instanceof InstanceBuilder)
                {
                // Add any remaining params, skip the first param which is the service
                Iterator iterParams = ((InstanceBuilder) bldrService).getConstructorParameterList().iterator();
                if (iterParams.hasNext())
                    {
                    iterParams.next();
                    }
                while (iterParams.hasNext())
                    {
                    listParams.add((Parameter) iterParams.next());
                    }
                }
            setTopicService((TopicService) bldrService.realize(new NullParameterResolver(),
                    Classes.getContextClassLoader(), listParams));
            }

        setTransferThreshold(proxyDeps.getTransferThreshold());
        }

    @Override
    public void onMessage(com.tangosol.net.messaging.Message message)
        {
        if (message instanceof TopicServiceRequest)
            {
            TopicServiceRequest request = (TopicServiceRequest) message;
            request.setTopicService(getTopicService());
            request.setTopicServiceProxy(this);
            request.setTransferThreshold(getTransferThreshold());
            }
        message.run();
        }

    @Override
    public void releaseTopic(NamedTopic<?> topic)
        {
        releaseTopic(topic, /*fDestroy*/ false);
        }

    protected void releaseTopic(NamedTopic<?> topic, boolean fDestroy)
        {
        if (topic instanceof ConverterNamedTopic<?,?>)
            {
            topic = ((ConverterNamedTopic) topic).getNamedTopic();
            }
        ConfigurableCacheFactory ccf = getCacheFactory();
        if (fDestroy)
            {
            ccf.destroyTopic(topic);
            }
        else
            {
            ccf.releaseTopic(topic);
            }
        }

    @Override
    public void setConfig(XmlElement xml)
        {
        //noinspection deprecation
        setDependencies(LegacyXmlTopicServiceProxyHelper.fromXml(xml, new DefaultTopicServiceProxyDependencies()));
        }

    /**
     * Getter for property NamedTopicSet.<p>
     */
    public Set<String> getNamedTopicSet()
        {
        return __m_NamedTopicSet;
        }

    /**
     * Setter for property NamedTopicSet.<p>
     */
    public void setNamedTopicSet(Set<String> setTopic)
        {
        __m_NamedTopicSet = setTopic;
        }

    /**
     * Return the Serializer associated with the given NamedTopic or null if the
     * NamedTopic is an in-process cache.
     */
    public static Serializer getSerializer(NamedTopic<?> topic)
        {
        Serializer serializer = null;
        if (topic instanceof SafeNamedTopic<?>)
            {
            TopicService service = topic.getTopicService();
            serializer = service.getSerializer();
            }
        return serializer;
        }

    /**
     * The cache factory that created this service.
     */
    public ConfigurableCacheFactory getCacheFactory()
        {
        return __m_CacheFactory;
        }

    /**
     * Set the {@link ConfigurableCacheFactory}.
     *
     * @param ccf  the {@link ConfigurableCacheFactory}
     */
    public void setCacheFactory(ConfigurableCacheFactory ccf)
        {
        __m_CacheFactory = ccf;
        }

    /**
     * Getter for property TopicService.<p>
     */
    public TopicService getTopicService()
        {
        TopicService service = __m_TopicService;
        return service == null ? this : service;
        }

    /**
     * Setter for property TopicService.<p>
     */
    public void setTopicService(TopicService sProperty)
        {
        __m_TopicService = sProperty;
        }

    @Override
    public void setSerializer(com.tangosol.io.Serializer serializer)
        {
        super.setSerializer(serializer);

        getConverterFromBinary().setSerializer(serializer);
        getConverterToBinary().setSerializer(serializer);
        }

    @Override
    public void registerChannel(Channel channel)
        {
        super.registerChannel(channel);
        setChannel(channel);
        }

    @Override
    public void unregisterChannel(Channel channel)
        {
        super.unregisterChannel(channel);
        setChannel(null);
        }

    public Channel getChannel()
        {
        return m_channel;
        }

    public void setChannel(Channel channel)
        {
        m_channel = channel;
        }

    /**
     * Getter for property TransferThreshold.<p>
     */
    public long getTransferThreshold()
        {
        return __m_TransferThreshold;
        }

    /**
     * Setter for property TransferThreshold.<p>
     */
    public void setTransferThreshold(long lThreshold)
        {
        __m_TransferThreshold = lThreshold;
        }

    /**
     * True iff binary pass-through optimizations are enabled.
     */
    public void setPassThroughEnabled(boolean fEnabled)
        {
        __m_PassThroughEnabled = fEnabled;
        }

    /**
     * True iff binary pass-through optimizations are enabled.
     */
    public boolean isPassThroughEnabled()
        {
        return __m_PassThroughEnabled;
        }

    /**
     * Child ConverterFromBinary instance.
     */
    public ConverterFromBinary getConverterFromBinary()
        {
        return __m_ConverterFromBinary;
        }

    /**
     * Child ConverterFromBinary instance.
     */
    protected void setConverterFromBinary(ConverterFromBinary conv)
        {
        __m_ConverterFromBinary = conv;
        }

    /**
     * Child ConverterToBinary instance.
     */
    public ConverterToBinary getConverterToBinary()
        {
        return __m_ConverterToBinary;
        }

    /**
     * Child ConverterToBinary instance.
     */
    protected void setConverterToBinary(ConverterToBinary conv)
        {
        __m_ConverterToBinary = conv;
        }

    // ----- inner class: ConverterFromBinary -------------------------------

    /**
     * Converter implementation that converts Objects from a Binary
     * representation via the CacheServiceProxy's Serializer.
     */
    public static class ConverterFromBinary
            extends Converter
        {
        public ConverterFromBinary()
            {
            this(null, null, true);
            }

        public ConverterFromBinary(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            if (fInit)
                {
                __init();
                }
            }

        @Override
        public void __init()
            {
            // private initialization
            __initPrivate();
            // signal the end of the initialization
            set_Constructed(true);
            }

        @Override
        protected void __initPrivate()
            {
            super.__initPrivate();
            }

        @Override
        public Object convert(Object o)
            {
            return o == null ? null :
                    ExternalizableHelper.fromBinary((Binary) o, getSerializer());
            }
        }

    // ----- inner class: ConverterToBinary ---------------------------------

    /**
     * Converter implementation that converts Objects to a Binary
     * representation via the CacheServiceProxy's Serializer.
     */
    public static class ConverterToBinary
            extends Converter
        {
        public ConverterToBinary()
            {
            this(null, null, true);
            }

        public ConverterToBinary(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);

            if (fInit)
                {
                __init();
                }
            }

        @Override
        public void __init()
            {
            // private initialization
            __initPrivate();
            // signal the end of the initialization
            set_Constructed(true);
            }

        @Override
        protected void __initPrivate()
            {
            super.__initPrivate();
            }

        // Declared at the super level
        public Object convert(Object o)
            {
            return ExternalizableHelper.toBinary(o, getSerializer());
            }
        }

    // ----- inner class: ConverterTopic ------------------------------------

    @SuppressWarnings("unchecked")
    protected static class ConverterTopic
            extends ConverterNamedTopic<Binary, Binary>
            implements PublisherConnector.Factory<Binary>
        {
        public ConverterTopic(NamedTopic<Binary> topic, ConverterToBinary convUp, ConverterFromBinary convDown, Serializer serializerDown)
            {
            super(topic, convUp, bin -> ExternalizableHelper.toBinary(convDown.convert(bin), serializerDown),
                    convDown, bin -> (Binary) convUp.convert(ExternalizableHelper.fromBinary(bin, serializerDown)));
            }

        @Override
        public Publisher<Binary> createPublisher(Publisher.Option<? super Binary>... options)
            {
            NamedTopicPublisher<Binary> publisher = (NamedTopicPublisher<Binary>) f_topic.createPublisher(options);
            return new ConverterPublisher<>(publisher, this);
            }

        @Override
        public PublisherConnector<Binary> createPublisherConnector(Publisher.Option<? super Binary>[] options)
            {
            PublisherConnector<Binary> connector = ((PublisherConnector.Factory<Binary>) f_topic).createPublisherConnector(options);
            return new ConverterPublisherConnector<>(connector, f_convBinaryUp, f_convBinaryDown);
            }

        @Override
        public <U> Subscriber<U> createSubscriber(Subscriber.Option<? super Binary, U>... options)
            {
            NamedTopicSubscriber<Binary> subscriber = (NamedTopicSubscriber<Binary>) f_topic.createSubscriber(options);
            return (Subscriber<U>) new ConverterConnectedSubscriber<>(subscriber, this);
            }
        }
    }
