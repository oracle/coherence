/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.SecurityConfig;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.Session;
import com.tangosol.net.ValueTypeAssertion;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.Resources;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.function.Remote;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;

import java.net.URL;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Local cluster topic subscriber install gate tests.
 *
 * @author Aleks Seovic  2026.05.15
 * @since 26.04
 */
public class TopicSubscriberInstallGateClusterTests
    {
    @BeforeClass
    public static void setup()
            throws Exception
        {
        s_sClusterOld      = System.getProperty(PROP_CLUSTER);
        s_sCacheConfigOld  = System.getProperty(PROP_CACHE_CONFIG);
        s_sLocalStorageOld = System.getProperty(PROP_LOCAL_STORAGE);
        s_sModeOld         = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
        s_sWkaOld          = System.getProperty(PROP_WKA);
        s_sLocalhostOld    = System.getProperty(PROP_LOCALHOST);

        String sHost = LocalPlatform.get().getLoopbackAddress().getHostAddress();
        setProperty(PROP_WKA, sHost);
        setProperty(PROP_LOCALHOST, sHost);
        setProperty(PROP_CLUSTER, "CACHE01-C4-14110-topics-" + Long.toString(System.nanoTime(), 36));
        setProperty(PROP_CACHE_CONFIG, "topic-cache-config.xml");
        setProperty(PROP_LOCAL_STORAGE, "true");
        setProperty(CoherenceMode.PROP_COHERENCE_MODE, "prod");
        CoherenceModeHelper.reset();
        resetSecurityConfig();

        ClassLoader loader = Base.getContextClassLoader();
        URL         url    = Resources.findFileOrResource("topic-cache-config.xml", loader);

        if (url == null)
            {
            throw new IllegalStateException("unable to find cache config file: topic-cache-config.xml");
            }

        XmlElement xml = XmlHelper.loadXml(url.openStream());
        CacheFactory.getCacheFactoryBuilder().setCacheConfiguration("topic-cache-config.xml", loader, xml);

        ExtensibleConfigurableCacheFactory.Dependencies deps =
                ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance("topic-cache-config.xml");
        s_eccf = new ExtensibleConfigurableCacheFactory(deps);

        CacheFactory.getCacheFactoryBuilder().setConfigurableCacheFactory(s_eccf, "$Default$", loader, true);
        DefaultCacheServer.start(s_eccf);

        s_session = new ConfigurableCacheFactorySession(s_eccf, loader);
        }

    @AfterClass
    public static void cleanup()
        {
        CacheFactory.shutdown();
        restoreProperty(PROP_CLUSTER, s_sClusterOld);
        restoreProperty(PROP_CACHE_CONFIG, s_sCacheConfigOld);
        restoreProperty(PROP_LOCAL_STORAGE, s_sLocalStorageOld);
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, s_sModeOld);
        restoreProperty(PROP_WKA, s_sWkaOld);
        restoreProperty(PROP_LOCALHOST, s_sLocalhostOld);
        CoherenceModeHelper.reset();
        resetSecurityConfig();
        }

    @Test
    public void rejectsUnannotatedFilterWhenCreatingSubscriber()
        {
        NamedTopic<String> topic = topic("plain-filter");

        assertThrows(SecurityException.class, () ->
                topic.createSubscriber(Subscriber.Filtered.by(new PlainFilter())));
        }

    @Test
    public void rejectsUnannotatedConverterWhenCreatingSubscriber()
        {
        NamedTopic<String> topic = topic("plain-converter");

        assertThrows(SecurityException.class, () ->
                topic.createSubscriber(Subscriber.Convert.using(new PlainConverter())));
        }

    @Test
    public void rejectsNestedExtractorWhenCreatingFilteredSubscriber()
        {
        NamedTopic<String> topic = topic("nested-extractor");

        assertThrows(SecurityException.class, () ->
                topic.createSubscriber(Subscriber.Filtered.by(new EqualsFilter<>(new PlainExtractor(), "value"))));
        }

    @Test
    public void allowsAnnotatedSubscriberFilter()
            throws Exception
        {
        NamedTopic<String> topic = topic("annotated-filter");

        try (Publisher<String> publisher = topic.createPublisher();
             Subscriber<String> subscriber = topic.createSubscriber(Subscriber.CompleteOnEmpty.enabled(),
                     Subscriber.Filtered.by(new AnnotatedFilter())))
            {
            publisher.send("value").get(1, TimeUnit.MINUTES);
            assertEquals("value", subscriber.receive().get(1, TimeUnit.MINUTES).getValue());
            }
        }

    @Test
    public void allowsAnnotatedSubscriberConverter()
            throws Exception
        {
        NamedTopic<String> topic = topic("annotated-converter");

        try (Publisher<String> publisher = topic.createPublisher();
             Subscriber<String> subscriber = topic.createSubscriber(Subscriber.CompleteOnEmpty.enabled(),
                     Subscriber.Convert.using(new AnnotatedConverter())))
            {
            publisher.send("value").get(1, TimeUnit.MINUTES);
            assertEquals("VALUE", subscriber.receive().get(1, TimeUnit.MINUTES).getValue());
            }
        }

    private static NamedTopic<String> topic(String sSuffix)
        {
        return s_session.getTopic("java-c4-14110-" + sSuffix, ValueTypeAssertion.withType(String.class));
        }

    private static void setProperty(String sName, String sValue)
        {
        System.setProperty(sName, sValue);
        }

    private static void restoreProperty(String sName, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sName);
            }
        else
            {
            System.setProperty(sName, sValue);
            }
        }

    private static void resetSecurityConfig()
        {
        try
            {
            Method method = SecurityConfig.class.getDeclaredMethod("resetForTesting");
            method.setAccessible(true);
            method.invoke(null);
            }
        catch (ReflectiveOperationException e)
            {
            throw new AssertionError(e);
            }
        }

    public static class PlainFilter
            implements Filter<String>
        {
        @Override
        public boolean evaluate(String value)
            {
            return true;
            }
        }

    @Remote.Executable
    public static class AnnotatedFilter
            implements Filter<String>
        {
        @Override
        public boolean evaluate(String value)
            {
            return true;
            }
        }

    public static class PlainConverter
            implements Remote.Function<String, String>
        {
        @Override
        public String apply(String value)
            {
            return value;
            }
        }

    @Remote.Executable
    public static class AnnotatedConverter
            implements Remote.Function<String, String>
        {
        @Override
        public String apply(String value)
            {
            return value.toUpperCase();
            }
        }

    public static class PlainExtractor
            implements ValueExtractor<String, String>
        {
        @Override
        public String extract(String target)
            {
            return target;
            }
        }

    private static final String PROP_CLUSTER       = "coherence.cluster";
    private static final String PROP_CACHE_CONFIG  = "coherence.cacheconfig";
    private static final String PROP_LOCAL_STORAGE = "coherence.distributed.localstorage";
    private static final String PROP_WKA           = "coherence.wka";
    private static final String PROP_LOCALHOST     = "coherence.localhost";

    private static ExtensibleConfigurableCacheFactory s_eccf;
    private static Session                            s_session;
    private static String                             s_sClusterOld;
    private static String                             s_sCacheConfigOld;
    private static String                             s_sLocalStorageOld;
    private static String                             s_sModeOld;
    private static String                             s_sWkaOld;
    private static String                             s_sLocalhostOld;
    }
